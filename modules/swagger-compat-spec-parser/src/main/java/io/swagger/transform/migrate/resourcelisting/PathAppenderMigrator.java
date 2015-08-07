package io.swagger.transform.migrate.resourcelisting;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import io.swagger.transform.migrate.SwaggerMigrator;
import io.swagger.transform.util.SwaggerMigrationException;

import javax.annotation.Nonnull;
import javax.annotation.Untainted;

/**
 * Append a base path to individual API objects
 *
 * <p>See <a href="https://github.com/swagger-api/swagger-parser/issues/4"
 * target="_blank">issue #4 on GitHub</a>.</p>
 */
public final class PathAppenderMigrator
        implements SwaggerMigrator {
    private final String basePath;

    public PathAppenderMigrator(@Untainted @Nonnull final String basePath) {
        this.basePath = basePath;
    }

    @Nonnull
    @Override
    public JsonNode migrate(@Nonnull final JsonNode input)
            throws SwaggerMigrationException {
        try {
            Preconditions.checkArgument(input.isObject(),
                    "expected JSON to be a JSON object but it isn't");
            Preconditions.checkArgument(input.path("path").isTextual(),
                    "\"path\" member of API object is not a JSON string");
        } catch (IllegalArgumentException e) {
            throw new SwaggerMigrationException(e.getMessage());
        }

        // We have to do that... JsonNode is not immutable
        final ObjectNode node = input.deepCopy();
        final String oldPath = node.get("path").textValue();
        node.put("path", basePath + oldPath);
        return node;
    }
}
