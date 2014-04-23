package com.wordnik.swagger.reader;

import com.fasterxml.jackson.databind.JsonNode;
import com.wordnik.swagger.models.resourcelisting.Authorization;

import javax.annotation.Nonnull;
import java.util.Objects;

final class ProxySwaggerReader
    implements SwaggerReader
{
    private final String proxy;

    ProxySwaggerReader(@Nonnull final String proxy)
    {
        this.proxy = Objects.requireNonNull(proxy);
    }

    @Override
    public JsonNode read(final String url, final Authorization auth)
    {
        return null;
    }
}
