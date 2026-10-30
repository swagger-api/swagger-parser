package io.swagger.v3.parser.util;

import tools.jackson.databind.JsonNode;

/**
 * Text access for tree nodes that keeps the Jackson 2 results.
 * <p>
 * Jackson 2 {@code asText()} never threw: null gave {@code "null"}, missing nodes and containers gave
 * {@code ""}. Jackson 3 {@code asString()} gives {@code ""} for null and throws for containers.
 * See JacksonNodeBehaviourTest for the recorded Jackson 3 results.
 */
public final class NodeTextUtils {

    private NodeTextUtils() {
    }

    /** Same result as Jackson 2 {@code JsonNode.asText()}. */
    public static String asText(JsonNode node) {
        switch (node.getNodeType()) {
            case NULL:
                return "null";
            case MISSING:
            case ARRAY:
            case OBJECT:
                return "";
            default:
                return node.asString();
        }
    }
}
