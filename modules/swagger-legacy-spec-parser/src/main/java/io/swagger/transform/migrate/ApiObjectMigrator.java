package io.swagger.transform.migrate;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.jsonpointer.JsonPointer;
import io.swagger.transform.util.MutableJsonTree;
import io.swagger.transform.util.SwaggerMigrationException;

import javax.annotation.Nonnull;

import static io.swagger.transform.util.SwaggerMigrators.*;

/**
 * Migrator for one element of an {@code apis} array
 */
public final class ApiObjectMigrator
    implements SwaggerMigrator
{
    private final SwaggerMigrator parametersMigrator
        = new OperationParametersMigrator();

    @Nonnull
    @Override
    public JsonNode migrate(@Nonnull final JsonNode input)
        throws SwaggerMigrationException
    {
        final MutableJsonTree tree = new MutableJsonTree(input);

        tree.applyMigrator(renameMember("httpMethod", "method"));
        tree.applyMigrator(renameMember("errorResponses", "responseMessages"));

        /*
         * Migrate response messages, if any
         */
        JsonPointer ptr = JsonPointer.of("responseMessages");

        if (!ptr.path(tree.getBaseNode()).isMissingNode()) {
            tree.setPointer(ptr);
            tree.applyMigratorToElements(renameMember("reason", "message"));
        }

        /*
         * Migrate parameters
         */
        ptr = JsonPointer.of("parameters");
        tree.setPointer(ptr);
        tree.applyMigratorToElements(parametersMigrator);

        return tree.getBaseNode();
    }
}
