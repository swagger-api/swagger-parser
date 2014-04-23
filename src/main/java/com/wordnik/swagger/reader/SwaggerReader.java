package com.wordnik.swagger.reader;

import com.fasterxml.jackson.databind.JsonNode;
import com.wordnik.swagger.models.resourcelisting.Authorization;

public interface SwaggerReader
{
    JsonNode read(String url, Authorization auth);
}
