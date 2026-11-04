package io.swagger.transform.migrate;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.transform.util.MutableJsonTree;
import io.swagger.transform.util.SwaggerMigrationException;

import javax.annotation.Nonnull;

/**
 * Migrator for the {@code operations} arrau of an API object
 */
public final class ApiOperationMigrator
        implements SwaggerMigrator {
    private final SwaggerMigrator migrator = new ApiObjectMigrator();

    @Nonnull
    @Override
    public JsonNode migrate(@Nonnull final JsonNode input)
            throws SwaggerMigrationException {
        final MutableJsonTree tree = new MutableJsonTree(input);
        tree.setPointer(JsonPointer.compile("/operations"));
        tree.applyMigratorToElements(migrator);
        return tree.getBaseNode();
    }
}
