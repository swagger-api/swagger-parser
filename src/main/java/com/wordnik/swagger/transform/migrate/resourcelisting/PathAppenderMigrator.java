package com.wordnik.swagger.transform.migrate.resourcelisting;

import com.fasterxml.jackson.databind.JsonNode;
import com.wordnik.swagger.transform.migrate.SwaggerMigrator;
import com.wordnik.swagger.transform.util.SwaggerMigrationException;

import javax.annotation.Nonnull;
import javax.annotation.Untainted;

/**
 * Append a base path to individual API objects
 *
 * <p>See <a href="https://github.com/wordnik/swagger-parser/issues/4"
 * target="_blank">issue #4 on GitHub</a>.</p>
 */
public final class PathAppenderMigrator
    implements SwaggerMigrator
{
    private final String basePath;

    public PathAppenderMigrator(@Untainted @Nonnull final String basePath)
    {
        this.basePath = basePath;
    }

    @Nonnull
    @Override
    public JsonNode migrate(@Nonnull final JsonNode input)
        throws SwaggerMigrationException
    {
        return null;
    }
}
