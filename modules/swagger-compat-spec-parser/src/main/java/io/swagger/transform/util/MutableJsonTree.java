package io.swagger.transform.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jackson.jsonpointer.JsonPointer;
import com.github.fge.jsonpatch.JsonPatch;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import io.swagger.transform.migrate.SwaggerMigrator;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;

import static io.swagger.transform.util.SwaggerMigrators.fromPatch;

/**
 * A mutable JSON tree with traversal capabilities
 *
 * <p>Navigation through the tree is done using {@link JsonPointer}s.</p>
 *
 * <p>Note that {@link JsonNode} is <em>mutable</em>. This means that at init
 * time we make a copy of the node given as argument (using {@link
 * JsonNode#deepCopy()}, but after that all nodes returned by {@link
 * #getCurrentNode()} are mutable. Changes you make to the returned node
 * therefore <strong>will</strong> be reflected in the result.</p>
 *
 * <p>While you can do that, it is advised that you use {@link SwaggerMigrator}s
 * instead and make use of the {@link #applyMigrator(SwaggerMigrator)} and
 * {@link #applyMigratorToElements(SwaggerMigrator)} methods.</p>
 */
@ParametersAreNonnullByDefault
public final class MutableJsonTree {
    private JsonNode baseNode;
    private JsonNode currentNode;
    private JsonPointer currentPointer = JsonPointer.empty();

    /**
     * Constructor
     *
     * @param node the node to transform (copied)
     */
    public MutableJsonTree(final JsonNode node) {
        Objects.requireNonNull(node, "node must not be null");
        baseNode = node.deepCopy();
        currentNode = baseNode;
    }

    /**
     * Absolute pointer change
     *
     * <p>The new current node will be the node at this pointer starting from
     * the base node.</p>
     *
     * @param pointer the pointer
     * @throws IllegalArgumentException no JSON value at given pointer
     */
    public void setPointer(final JsonPointer pointer) {
        Objects.requireNonNull(pointer, "pointer must not be null");
        doSetPointer(pointer);
    }

    /**
     * Relative pointer change
     *
     * <p>The pointer in argument is appended to the current pointer (using
     * {@link JsonPointer#append(JsonPointer)}) and the current node is set
     * accordingly.</p>
     *
     * @param pointer the pointer to append
     * @throws IllegalArgumentException no JSON value at given pointer
     */
    public void appendPointer(final JsonPointer pointer) {
        Objects.requireNonNull(pointer, "pointer must not be null");
        doSetPointer(currentPointer.append(pointer));
    }

    private void doSetPointer(final JsonPointer pointer) {
        currentPointer = pointer;
        currentNode = currentPointer.path(baseNode);
        Preconditions.checkArgument(!currentNode.isMissingNode(),
                "base node has no element at pointer " + currentPointer);
    }

    /**
     * Return the base node
     *
     * <p>The "base node" here means the (potentially) altered copy of the node
     * supplied as the constructor argument.</p>
     *
     * @return the base node
     */
    public JsonNode getBaseNode() {
        return baseNode;
    }

    /**
     * Get the value at the current pointer (always guaranteed to exist)
     *
     * @return the (mutable) node
     */
    public JsonNode getCurrentNode() {
        return currentNode;
    }

    /**
     * Apply a JSON Patch to the current node
     *
     * <p>This will turn the patch into a {@link SwaggerMigrator} using {@link
     * SwaggerMigrators#fromPatch(JsonPatch)} and call {@link
     * #applyMigrator(SwaggerMigrator)}.</p>
     *
     * @param patch the JSON Patch to apply
     * @throws SwaggerMigrationException same as {@link
     *                                   #applyMigrator(SwaggerMigrator)}
     */
    public void applyPatch(final JsonPatch patch)
            throws SwaggerMigrationException {
        applyMigrator(fromPatch(patch));
    }

    /**
     * Apply a migrator to the node at the current pointer
     *
     * <p>It is assumed here that the current node is a JSON Object.</p>
     *
     * @param migrator the migrator to apply
     * @throws SwaggerMigrationException current node is not an object, or the
     *                                   migrator failed to apply
     */
    public void applyMigrator(final SwaggerMigrator migrator)
            throws SwaggerMigrationException {
        final JsonPointer parent = currentPointer.parent();
        if (!parent.get(baseNode).isObject()) {
            throw new SwaggerMigrationException();
        }
        final ObjectNode parentNode = (ObjectNode) parent.get(baseNode);

        final JsonNode patched = migrator.migrate(currentNode);

        if (currentPointer.isEmpty()) {
            baseNode = currentNode = patched;
            return;
        }

        final String memberName = Iterables.getLast(currentPointer)
                .getToken().getRaw();
        parentNode.put(memberName, patched);

    }

    /**
     * Apply a migrator to all elements of the array at the current pointer
     *
     * <p>Note that if the migrator fails to apply to at least one element, the
     * original array is left untouched; its elements are replaced if and only
     * if the migrator applies successfully to <strong>all</strong> elements.
     * </p>
     *
     * @param migrator the migrator to apply
     * @throws SwaggerMigrationException current node is not a JSON Array, or
     *                                   migrator failed to apply to at least one array element
     */
    public void applyMigratorToElements(final SwaggerMigrator migrator)
            throws SwaggerMigrationException {
        if (!currentNode.isArray()) {
            throw new SwaggerMigrationException();
        }

        final ArrayNode array = (ArrayNode) currentNode;
        final ArrayNode transformed = array.arrayNode();
        for (final JsonNode element : array) {
            transformed.add(migrator.migrate(element));
        }

        array.removeAll().addAll(transformed);
    }

    /**
     * Apply a JSON Patch to all elements of a JSON Array
     *
     * <p>This will wrap the patch into a {@link SwaggerMigrator} using {@link
     * SwaggerMigrators#fromPatch(JsonPatch)} and call {@link
     * #applyMigratorToElements(SwaggerMigrator)}.</p>
     *
     * @param patch the JSON Patch to apply
     * @throws SwaggerMigrationException same as {@link
     *                                   #applyMigratorToElements(SwaggerMigrator)}
     */
    public void applyPatchToElements(final JsonPatch patch)
            throws SwaggerMigrationException {
        applyMigratorToElements(fromPatch(patch));
    }

    @Override
    public String toString() {
        return "current pointer: " + currentPointer;
    }
}
