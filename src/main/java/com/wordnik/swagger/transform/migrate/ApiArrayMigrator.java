package com.wordnik.swagger.transform.migrate;

import com.fasterxml.jackson.databind.JsonNode;
import com.wordnik.swagger.transform.util.MutableJsonTree;
import com.wordnik.swagger.transform.util.SwaggerMigrationException;

import javax.annotation.Nonnull;

/**
 * API declaration, JSON Pointer: "/apis"
 */
public final class ApiArrayMigrator
    implements SwaggerMigrator
{
    private final SwaggerMigrator migrator = new ApiOperationMigrator();

    @Nonnull
    @Override
    public JsonNode migrate(@Nonnull final JsonNode input)
        throws SwaggerMigrationException
    {
        final MutableJsonTree tree = new MutableJsonTree(input);

        tree.applyMigratorToElements(migrator);
        return tree.getBaseNode();
    }
}
