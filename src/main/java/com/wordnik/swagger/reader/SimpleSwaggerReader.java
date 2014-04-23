package com.wordnik.swagger.reader;

import com.fasterxml.jackson.databind.JsonNode;
import com.wordnik.swagger.models.resourcelisting.Authorization;

final class SimpleSwaggerReader
    implements SwaggerReader
{
    SimpleSwaggerReader()
    {
    }

    @Override
    public JsonNode read(final String url, final Authorization auth)
    {
        return null;
    }
}
