package com.wordnik.swagger.transform.migrate;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.jsonpointer.JsonPointer;
import com.wordnik.swagger.transform.util.MutableJsonTree;
import com.wordnik.swagger.transform.util.SwaggerTransformException;

import javax.annotation.Nonnull;

import static com.wordnik.swagger.transform.util.SwaggerMigrators.*;

/**
 * Migrator for one element of an {@code apis} array
 */
public final class ApiObjectMigrator
    implements SwaggerMigrator
{
    private static final JsonPointer PARAMETERS = JsonPointer.of("parameters");
    private final SwaggerMigrator parametersMigrator
        = new OperationParametersMigrator();

    @Nonnull
    @Override
    public JsonNode migrate(@Nonnull final JsonNode input)
        throws SwaggerTransformException
    {
        final MutableJsonTree tree = new MutableJsonTree(input);

        tree.applyMigrator(renameMember("httpMethod", "method"));

        tree.setPointer(PARAMETERS);
        tree.applyMigratorToElements(parametersMigrator);

        return tree.getBaseNode();
    }
}
