package io.swagger.transform.migrate;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.jsonpointer.JsonPointer;
import io.swagger.transform.util.MutableJsonTree;
import io.swagger.transform.util.SwaggerMigrationException;

import javax.annotation.Nonnull;

import static io.swagger.transform.util.SwaggerMigrators.membersToString;
import static io.swagger.transform.util.SwaggerMigrators.patchFromResource;

/**
 * Migrator for a complete 1.1 API declaration
 */
public final class V11ApiDeclarationMigrator
        implements SwaggerMigrator {
    private final SwaggerMigrator migrator = new ApiOperationMigrator();

    @Nonnull
    @Override
    public JsonNode migrate(@Nonnull final JsonNode input)
            throws SwaggerMigrationException {
        final MutableJsonTree tree = new MutableJsonTree(input);

        tree.applyMigrator(membersToString("swaggerVersion", "apiVersion"));
        tree.applyMigrator(patchFromResource("/patches/v1.1/versionChange" +
                ".json"));

        tree.setPointer(JsonPointer.of("apis"));
        tree.applyMigratorToElements(migrator);

        return tree.getBaseNode();
    }
}
