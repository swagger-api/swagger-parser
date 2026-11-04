package io.swagger.transform.util;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Minimal implementation of <a href="https://tools.ietf.org/html/rfc6902">JSON
 * Patch</a>
 *
 * <p>Only the operations needed by the migrators are supported: {@code add},
 * {@code remove}, {@code replace} and {@code move}. Any other operation is
 * rejected when the patch is read.</p>
 *
 * <p>Instances are immutable. Applying a patch never modifies its input; a
 * modified copy is returned instead.</p>
 */
public final class JsonPatch {
    private final List<Operation> operations;

    private JsonPatch(final List<Operation> operations) {
        this.operations = Collections.unmodifiableList(operations);
    }

    /**
     * Build a patch from its JSON representation
     *
     * <p>This is also used by Jackson, so that patches can be read with {@code
     * ObjectMapper#readValue(..., JsonPatch.class)}.</p>
     *
     * @param node the patch, as a JSON Array of operations
     * @return the patch
     * @throws IllegalArgumentException the patch is invalid, or uses an
     *                                  unsupported operation
     */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static JsonPatch fromJson(final JsonNode node) {
        Objects.requireNonNull(node, "node must not be null");
        if (!node.isArray()) {
            throw new IllegalArgumentException("JSON Patch must be a JSON Array");
        }
        final List<Operation> operations = new ArrayList<>();
        for (final JsonNode element : node) {
            operations.add(Operation.fromJson(element));
        }
        return new JsonPatch(operations);
    }

    /**
     * Apply this patch to a JSON value
     *
     * @param node the value to patch (not modified)
     * @return the patched value
     * @throws SwaggerMigrationException one operation failed to apply
     */
    public JsonNode apply(final JsonNode node)
            throws SwaggerMigrationException {
        Objects.requireNonNull(node, "node must not be null");
        JsonNode result = node.deepCopy();
        for (final Operation operation : operations) {
            result = operation.apply(result);
        }
        return result;
    }

    @Override
    public String toString() {
        return operations.toString();
    }

    private enum Type {
        ADD, REMOVE, REPLACE, MOVE
    }

    private static final class Operation {
        private final Type type;
        private final JsonPointer path;
        private final JsonPointer from;
        private final JsonNode value;

        private Operation(final Type type, final JsonPointer path,
                          final JsonPointer from, final JsonNode value) {
            this.type = type;
            this.path = path;
            this.from = from;
            this.value = value;
        }

        static Operation fromJson(final JsonNode node) {
            if (!node.isObject()) {
                throw new IllegalArgumentException("JSON Patch operation must be a JSON Object");
            }
            final String op = textMember(node, "op");
            final Type type = typeOf(op);
            final JsonPointer path = pointerMember(node, "path");
            JsonPointer from = null;
            JsonNode value = null;
            switch (type) {
                case ADD:
                case REPLACE:
                    if (!node.has("value")) {
                        throw new IllegalArgumentException("\"" + op + "\" operation has no \"value\" member");
                    }
                    value = node.get("value").deepCopy();
                    break;
                case MOVE:
                    from = pointerMember(node, "from");
                    break;
                default:
                    break;
            }
            return new Operation(type, path, from, value);
        }

        JsonNode apply(final JsonNode node)
                throws SwaggerMigrationException {
            switch (type) {
                case ADD:
                    return add(node, path, value.deepCopy());
                case REMOVE:
                    remove(node, path);
                    return node;
                case REPLACE:
                    return replace(node, path, value.deepCopy());
                case MOVE:
                    return move(node);
                default:
                    throw new IllegalStateException("unknown operation " + type);
            }
        }

        private JsonNode move(final JsonNode node)
                throws SwaggerMigrationException {
            final JsonNode moved = node.at(from);
            if (moved.isMissingNode()) {
                throw new SwaggerMigrationException("no such path in target JSON document: " + from);
            }
            if (from.equals(path)) {
                return node;
            }
            if (path.toString().startsWith(from + "/")) {
                throw new SwaggerMigrationException("cannot move a value into one of its children: "
                        + from + " -> " + path);
            }
            remove(node, from);
            return add(node, path, moved);
        }

        private static JsonNode add(final JsonNode node, final JsonPointer path,
                                    final JsonNode value)
                throws SwaggerMigrationException {
            if (path.matches()) {
                return value;
            }
            final JsonNode parent = node.at(path.head());
            final String token = path.last().getMatchingProperty();
            if (parent.isObject()) {
                ((ObjectNode) parent).set(token, value);
            } else if (parent.isArray()) {
                final ArrayNode array = (ArrayNode) parent;
                if ("-".equals(token)) {
                    array.add(value);
                } else {
                    array.insert(arrayIndex(array, path, true), value);
                }
            } else {
                throw new SwaggerMigrationException("parent of path to add to is not a container: " + path);
            }
            return node;
        }

        private static void remove(final JsonNode node, final JsonPointer path)
                throws SwaggerMigrationException {
            if (path.matches()) {
                throw new SwaggerMigrationException("cannot remove the whole document");
            }
            if (node.at(path).isMissingNode()) {
                throw new SwaggerMigrationException("no such path in target JSON document: " + path);
            }
            final JsonNode parent = node.at(path.head());
            if (parent.isObject()) {
                ((ObjectNode) parent).remove(path.last().getMatchingProperty());
            } else {
                final ArrayNode array = (ArrayNode) parent;
                array.remove(arrayIndex(array, path, false));
            }
        }

        private static JsonNode replace(final JsonNode node, final JsonPointer path,
                                        final JsonNode value)
                throws SwaggerMigrationException {
            if (path.matches()) {
                return value;
            }
            if (node.at(path).isMissingNode()) {
                throw new SwaggerMigrationException("no such path in target JSON document: " + path);
            }
            final JsonNode parent = node.at(path.head());
            if (parent.isObject()) {
                ((ObjectNode) parent).set(path.last().getMatchingProperty(), value);
            } else {
                final ArrayNode array = (ArrayNode) parent;
                array.set(arrayIndex(array, path, false), value);
            }
            return node;
        }

        private static int arrayIndex(final ArrayNode array, final JsonPointer path,
                                      final boolean allowEnd)
                throws SwaggerMigrationException {
            final int index = path.last().getMatchingIndex();
            final int max = allowEnd ? array.size() : array.size() - 1;
            if (index < 0 || index > max) {
                throw new SwaggerMigrationException("no such index in target JSON array: " + path);
            }
            return index;
        }

        private static Type typeOf(final String op) {
            for (final Type type : Type.values()) {
                if (type.name().toLowerCase(Locale.ROOT).equals(op)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("unsupported JSON Patch operation \"" + op + '"');
        }

        private static String textMember(final JsonNode node, final String name) {
            final JsonNode member = node.get(name);
            if (member == null || !member.isTextual()) {
                throw new IllegalArgumentException("JSON Patch operation has no valid \"" + name + "\" member");
            }
            return member.textValue();
        }

        private static JsonPointer pointerMember(final JsonNode node, final String name) {
            return JsonPointer.compile(textMember(node, name));
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("{op=")
                    .append(type.name().toLowerCase(Locale.ROOT))
                    .append(", path=").append(path);
            if (from != null) {
                sb.append(", from=").append(from);
            }
            if (value != null) {
                sb.append(", value=").append(value);
            }
            return sb.append('}').toString();
        }
    }
}
