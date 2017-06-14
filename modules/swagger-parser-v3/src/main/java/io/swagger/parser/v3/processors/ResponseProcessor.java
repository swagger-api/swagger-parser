package io.swagger.parser.v3.processors;

import io.swagger.oas.models.media.MediaType;
import io.swagger.oas.models.media.Schema;
import io.swagger.oas.models.responses.ApiResponse;
import io.swagger.oas.models.OpenAPI;
import io.swagger.parser.ResolverCache;

import java.util.Map;

public class ResponseProcessor {

    private final SchemaProcessor schemaProcessor;

    public ResponseProcessor(ResolverCache cache, OpenAPI openApi) {
        schemaProcessor = new SchemaProcessor(cache, openApi);
    }

    public void processResponse(ApiResponse response) {
        //process the response body
        Schema schema = null;
        Map<String,MediaType> content = response.getContent();
        for( Map.Entry<String, MediaType> map : content.entrySet()) {
            if(map.getValue().getSchema()!= null) {
                schema = map.getValue().getSchema();
            }
        }

        if (schema != null) {
            schemaProcessor.processSchema(schema);
        }

        /* intentionally ignoring the response headers, even those these were modelled as a
         Map<String, Property> they should never have a $ref because what does it mean to have a
         complex object in an HTTP header?
          */

    }
}