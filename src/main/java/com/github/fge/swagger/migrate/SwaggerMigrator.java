package com.github.fge.swagger.migrate;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.swagger.util.SwaggerTransformException;

import javax.annotation.Nonnull;

public interface SwaggerMigrator
{
    @Nonnull
    JsonNode migrate(@Nonnull final JsonNode input)
        throws SwaggerTransformException;
}
