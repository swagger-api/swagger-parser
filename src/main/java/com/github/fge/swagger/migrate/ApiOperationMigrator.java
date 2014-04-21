package com.github.fge.swagger.migrate;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.jsonpointer.JsonPointer;
import com.github.fge.swagger.util.MutableJsonTree;
import com.github.fge.swagger.util.SwaggerTransformException;

import javax.annotation.Nonnull;

/**
 * API declaration, JSON Path: "/apis", all elements
 */
public final class ApiOperationMigrator
    implements SwaggerMigrator
{
    private final SwaggerMigrator migrator = new OperationMigrator();

    @Nonnull
    @Override
    public JsonNode migrate(@Nonnull final JsonNode input)
        throws SwaggerTransformException
    {
        final MutableJsonTree tree = new MutableJsonTree(input);
        tree.setPointer(JsonPointer.of("operations"));
        tree.applyMigratorToElements(migrator);
        return tree.getBaseNode();
    }
}
