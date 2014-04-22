package com.wordnik.swagger.transform.migrate;

import com.fasterxml.jackson.databind.JsonNode;
import com.wordnik.swagger.transform.util.MutableJsonTree;
import com.wordnik.swagger.transform.util.SwaggerMigrationException;

import javax.annotation.Nonnull;
import java.util.Objects;

import static com.wordnik.swagger.transform.util.SwaggerMigrators.*;

/**
 * Migrator for a complete 1.1 resource listing
 */
public final class V11ResourceListingMigrator
    implements SwaggerMigrator
{
    @Nonnull
    @Override
    public JsonNode migrate(@Nonnull final JsonNode input)
        throws SwaggerMigrationException
    {
        Objects.requireNonNull(input);
        final MutableJsonTree tree = new MutableJsonTree(input);

        SwaggerMigrator migrator;

        migrator = membersToString("swaggerVersion", "apiVersion");
        tree.applyMigrator(migrator);

        migrator = patchFromResource("/patches/v1.1/versionChange.json");
        tree.applyMigrator(migrator);

        return tree.getBaseNode();
    }
}
