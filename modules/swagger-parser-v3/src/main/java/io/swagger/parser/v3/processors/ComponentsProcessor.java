package io.swagger.parser.v3.processors;

import io.swagger.oas.models.OpenAPI;
import io.swagger.oas.models.media.Schema;
import io.swagger.oas.models.responses.ApiResponse;
import io.swagger.parser.v3.ResolverCache;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;


/**
 * Created by gracekarina on 13/06/17.
 */
public class ComponentsProcessor {
    private final ResolverCache cache;
    private final OpenAPI openApi;
    private final SchemaProcessor schemaProcessor;
    private final ResponseProcessor responseProcessor;

    public ComponentsProcessor(OpenAPI openApi,ResolverCache cache){
        this.cache = cache;
        this.openApi = openApi;
        this.schemaProcessor = new SchemaProcessor(cache);
        this.responseProcessor = new ResponseProcessor(cache, openApi);

    }


    public void processComponents() {
        final Map<String, Schema> schemas = openApi.getComponents().getSchemas();
        final Map<String, ApiResponse> responses = openApi.getComponents().getResponses();

        //schemas
        if (schemas != null) {

            Set<String> keySet = new LinkedHashSet<>();
            while(schemas.keySet().size() > keySet.size()) {
                processSchemas(keySet, schemas);
            }
        }

        //responses
        if (responses != null) {

            Set<String> keySet = new LinkedHashSet<>();
            while(responses.keySet().size() > keySet.size()) {
                processResponses(keySet, responses);
            }
        }

    }

    private void processResponses(Set<String> responseKey, Map<String, ApiResponse> responses) {
        responseKey.addAll(responses.keySet());

        for (String responseName : responseKey) {
            final ApiResponse response = responses.get(responseName);
            responseProcessor.processResponse(responseName,response);
        }
    }

    public void processSchemas(Set<String> schemaKeys, Map<String, Schema> schemas) {
        schemaKeys.addAll(schemas.keySet());

        for (String schemaName : schemaKeys) {
            final Schema schema = schemas.get(schemaName);
            schemaProcessor.processSchema(schema);
        }
    }
}
