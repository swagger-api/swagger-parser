package com.github.fge.swagger.migrate;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.jsonpointer.JsonPointer;
import com.github.fge.swagger.util.MutableJsonTree;
import com.github.fge.swagger.util.SwaggerTransformException;

import javax.annotation.Nonnull;

import static com.github.fge.swagger.util.SwaggerMigrators.*;

public final class OperationMigrator
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
