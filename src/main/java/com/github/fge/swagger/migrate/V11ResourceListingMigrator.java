package com.github.fge.swagger.migrate;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.swagger.util.MutableJsonTree;
import com.github.fge.swagger.util.SwaggerTransformException;

import javax.annotation.Nonnull;
import java.util.Objects;

import static com.github.fge.swagger.util.SwaggerMigrators.*;

public final class V11ResourceListingMigrator
    implements SwaggerMigrator
{
    @Nonnull
    @Override
    public JsonNode migrate(@Nonnull final JsonNode input)
        throws SwaggerTransformException
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
