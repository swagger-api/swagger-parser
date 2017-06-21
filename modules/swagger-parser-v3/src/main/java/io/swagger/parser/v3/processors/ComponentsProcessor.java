package io.swagger.parser.v3.processors;

import io.swagger.oas.models.OpenAPI;
import io.swagger.oas.models.headers.Header;
import io.swagger.oas.models.media.Schema;
import io.swagger.oas.models.parameters.Parameter;
import io.swagger.oas.models.parameters.RequestBody;
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
    private final RequestBodyProcessor requestBodyProcessor;
    private final ParameterProcessor parameterProcessor;
    private final HeaderProcessor headerProcessor;

    public ComponentsProcessor(OpenAPI openApi,ResolverCache cache){
        this.cache = cache;
        this.openApi = openApi;
        this.schemaProcessor = new SchemaProcessor(cache);
        this.responseProcessor = new ResponseProcessor(cache, openApi);
        this.requestBodyProcessor = new RequestBodyProcessor(cache, openApi);
        this.parameterProcessor = new ParameterProcessor(cache, openApi);
        this.headerProcessor = new HeaderProcessor(cache, openApi);

    }


    public void processComponents() {
        final Map<String, Schema> schemas = openApi.getComponents().getSchemas();
        final Map<String, ApiResponse> responses = openApi.getComponents().getResponses();
        final Map<String, RequestBody> requestBodies = openApi.getComponents().getRequestBodies();
        final Map<String, Parameter> parameters = openApi.getComponents().getParameters();
        final Map<String, Header> headers = openApi.getComponents().getHeaders();

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

        //requestBodies
        if (requestBodies != null) {
            Set<String> keySet = new LinkedHashSet<>();
            while(requestBodies.keySet().size() > keySet.size()) {
                processRequestBodies(keySet, requestBodies);
            }
        }

        //parameters
        if (parameters != null) {
            Set<String> keySet = new LinkedHashSet<>();
            while(parameters.keySet().size() > keySet.size()) {
                processParameters(keySet, parameters);
            }
        }

        //headers
        if (headers != null) {
            Set<String> keySet = new LinkedHashSet<>();
            while(headers.keySet().size() > keySet.size()) {
                processHeaders(keySet, headers);
            }
        }
    }

    private void processHeaders(Set<String> HeaderKey, Map<String, Header> headers) {
        HeaderKey.addAll(headers.keySet());

        for (String headersName : HeaderKey) {
            final Header header = headers.get(headersName);
            headerProcessor.processHeader(headersName,header);
        }
    }

    private void processParameters(Set<String> ParametersKey, Map<String, Parameter> parameters) {
        ParametersKey.addAll(parameters.keySet());

        for (String parametersName : ParametersKey) {
            final Parameter parameter = parameters.get(parametersName);
            parameterProcessor.processParameter(parametersName,parameter);
        }
    }

    private void processRequestBodies(Set<String> requestBodyKey, Map<String, RequestBody> requestBodies) {
        requestBodyKey.addAll(requestBodies.keySet());

        for (String requestBodyName : requestBodyKey) {
            final RequestBody requestBody = requestBodies.get(requestBodyName);
            requestBodyProcessor.processRequestBody(requestBodyName,requestBody);
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
