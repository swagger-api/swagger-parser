package com.github.fge.swagger.migrate;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JacksonUtils;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jackson.JsonNumEquals;
import com.github.fge.jackson.jsonpointer.JsonPointer;
import com.github.fge.swagger.util.MutableJsonTree;
import com.github.fge.swagger.util.SwaggerTransformException;

import javax.annotation.Nonnull;
import java.io.IOException;

import static com.github.fge.swagger.util.SwaggerMigrators.*;

/**
 * API declaration, JSON Pointer: ""
 */
public final class V11ApiDeclarationMigrator
    implements SwaggerMigrator
{
    private final SwaggerMigrator migrator = new ApiOperationMigrator();

    @Nonnull
    @Override
    public JsonNode migrate(@Nonnull final JsonNode input)
        throws SwaggerTransformException
    {
        final MutableJsonTree tree = new MutableJsonTree(input);

        tree.applyMigrator(membersToString("swaggerVersion", "apiVersion"));
        tree.applyMigrator(patchFromResource("/patches/v1.1/versionChange" +
            ".json"));

        tree.setPointer(JsonPointer.of("apis"));
        tree.applyMigratorToElements(migrator);

        return tree.getBaseNode();
    }

    public static void main(final String... args)
        throws IOException, SwaggerTransformException
    {
        final JsonNode orig
            = JsonLoader.fromResource("/samples/v1.1/callfire-broadcast.json");
        final JsonNode orig2
            = JsonLoader.fromResource("/samples/v1.1/callfire-broadcast.json");

        final SwaggerMigrator migrator = new V11ApiDeclarationMigrator();

        System.out.println(JacksonUtils.prettyPrint(migrator.migrate(orig)));
        System.out.println(JsonNumEquals.getInstance().equivalent(orig, orig2));
    }
}
