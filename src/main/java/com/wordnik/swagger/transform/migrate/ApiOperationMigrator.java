package com.wordnik.swagger.transform.migrate;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.jsonpointer.JsonPointer;
import com.wordnik.swagger.transform.util.MutableJsonTree;
import com.wordnik.swagger.transform.util.SwaggerTransformException;

import javax.annotation.Nonnull;

/**
 * Migrator for the {@code operations} arrau of an API object
 */
public final class ApiOperationMigrator
    implements SwaggerMigrator
{
    private final SwaggerMigrator migrator = new ApiObjectMigrator();

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
