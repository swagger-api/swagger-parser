package com.wordnik.swagger.transform.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jackson.jsonpointer.JsonPointer;
import com.github.fge.jsonpatch.JsonPatch;
import com.wordnik.swagger.transform.migrate.SwaggerMigrator;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;

import static com.wordnik.swagger.transform.util.SwaggerMigrators.*;

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
 */
@ParametersAreNonnullByDefault
public final class MutableJsonTree
{
    private JsonNode baseNode;
    private JsonNode currentNode;
    private JsonPointer currentPointer = JsonPointer.empty();

    /**
     * Constructor
     *
     * @param node the node to transform (copied)
     */
    public MutableJsonTree(final JsonNode node)
    {
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
    public void setPointer(final JsonPointer pointer)
    {
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
    public void appendPointer(final JsonPointer pointer)
    {
        Objects.requireNonNull(pointer, "pointer must not be null");
        doSetPointer(currentPointer.append(pointer));
    }

    private void doSetPointer(final JsonPointer pointer)
    {
        currentPointer = pointer;
        currentNode = currentPointer.path(baseNode);
        Preconditions.checkArgument(!currentNode.isMissingNode(),
            "base node has no element at pointer " + currentPointer);
    }

    public JsonNode getBaseNode()
    {
        return baseNode;
    }

    /**
     * Get the value at the current pointer (always guaranteed to exist)
     *
     * @return the (mutable) node
     */
    public JsonNode getCurrentNode()
    {
        return currentNode;
    }

    public void applyPatch(final JsonPatch patch)
        throws SwaggerTransformException
    {
        applyMigrator(fromPatch(patch));
    }

    public void applyMigrator(final SwaggerMigrator migrator)
        throws SwaggerTransformException
    {
        final JsonPointer parent = currentPointer.parent();
        if (!parent.get(baseNode).isObject())
            throw new SwaggerTransformException();
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

    public void applyMigratorToElements(final SwaggerMigrator migrator)
        throws SwaggerTransformException
    {
        if (!currentNode.isArray())
            throw new SwaggerTransformException();

        final ArrayNode array = (ArrayNode) currentNode;
        final ArrayNode transformed = array.arrayNode();
        for (final JsonNode element: array)
            transformed.add(migrator.migrate(element));

        array.removeAll().addAll(transformed);
    }

    public void applyPatchToElements(final JsonPatch patch)
        throws SwaggerTransformException
    {
        applyMigratorToElements(fromPatch(patch));
    }

    @Override
    public String toString()
    {
        return "current pointer: " + currentPointer;
    }
}
