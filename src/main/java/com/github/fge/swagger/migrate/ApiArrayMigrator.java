package com.github.fge.swagger.migrate;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.swagger.util.MutableJsonTree;
import com.github.fge.swagger.util.SwaggerTransformException;

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
        throws SwaggerTransformException
    {
        final MutableJsonTree tree = new MutableJsonTree(input);

        tree.applyMigratorToElements(migrator);
        return tree.getBaseNode();
    }
}
