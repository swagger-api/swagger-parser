package com.wordnik.swagger.transform.migrate;

import com.fasterxml.jackson.databind.JsonNode;
import com.wordnik.swagger.transform.util.SwaggerTransformException;

import javax.annotation.Nonnull;

public interface SwaggerMigrator
{
    @Nonnull
    JsonNode migrate(@Nonnull final JsonNode input)
        throws SwaggerTransformException;
}
