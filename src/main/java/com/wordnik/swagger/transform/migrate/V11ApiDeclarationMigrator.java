package com.wordnik.swagger.transform.migrate;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JacksonUtils;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jackson.JsonNumEquals;
import com.github.fge.jackson.jsonpointer.JsonPointer;
import com.wordnik.swagger.transform.util.MutableJsonTree;
import com.wordnik.swagger.transform.util.SwaggerTransformException;

import javax.annotation.Nonnull;
import java.io.IOException;

import static com.wordnik.swagger.transform.util.SwaggerMigrators.*;

/**
 * Migrator for a complete 1.1 API declaration
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
