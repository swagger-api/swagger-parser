package io.swagger.transform.migrate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jackson.jsonpointer.JsonPointer;
import io.swagger.transform.util.MutableJsonTree;
import io.swagger.transform.util.SwaggerMigrationException;
import io.swagger.util.Json;

import javax.annotation.Nonnull;
import java.util.Iterator;

import static io.swagger.transform.util.SwaggerMigrators.renameMember;

/**
 * Migrator for one element of an {@code apis} array
 */
public final class ApiObjectMigrator
        implements SwaggerMigrator {
    private final SwaggerMigrator parametersMigrator
            = new OperationParametersMigrator();

    @Nonnull
    @Override
    public JsonNode migrate(@Nonnull final JsonNode input)
            throws SwaggerMigrationException {
        ObjectNode on = (ObjectNode) input;
        if (on.get("type") == null) {
            JsonNode responseMessages = on.get("responseMessages");
            JsonNode type = null;
            if (responseMessages != null && responseMessages instanceof ArrayNode) {
                // look for a 200 response
                ArrayNode arrayNode = (ArrayNode) responseMessages;
                Iterator<JsonNode> itr = arrayNode.elements();
                while (itr.hasNext()) {
                    JsonNode rm = itr.next();
                    JsonNode code = rm.get("code");
                    if (code != null) {
                        if ("200".equals(code.toString())) {
                            type = rm;
                        }
                    }
                }
            }
            if (type != null) {
                if (type.get("type") == null) {
                    on.put("type", "void");
                } else {
                    on.put("type", type.get("type"));
                }
            } else {
                on.put("type", "void");
            }
        }

        // if there are no parameters, we can insert an empty array
        if (on.get("parameters") == null) {
            on.put("parameters", Json.mapper().createArrayNode());
        }

        // see if there's a response

        final MutableJsonTree tree = new MutableJsonTree(input);

        tree.applyMigrator(renameMember("httpMethod", "method"));
        tree.applyMigrator(renameMember("errorResponses", "responseMessages"));

        /*
         * Migrate response messages, if any
         */
        JsonPointer ptr = JsonPointer.of("responseMessages");

        if (!ptr.path(tree.getBaseNode()).isMissingNode()) {
            tree.setPointer(ptr);
            tree.applyMigratorToElements(renameMember("reason", "message"));
        }

        /*
         * Migrate parameters
         */
        ptr = JsonPointer.of("parameters");
        tree.setPointer(ptr);
        tree.applyMigratorToElements(parametersMigrator);

        return tree.getBaseNode();
    }
}
