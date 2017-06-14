package io.swagger.parser.v3.processors;

import io.swagger.oas.models.Operation;
//import io.swagger.models.RefResponse;
import io.swagger.oas.models.responses.ApiResponse;
import io.swagger.oas.models.OpenAPI;
import io.swagger.oas.models.parameters.Parameter;
import io.swagger.parser.ResolverCache;

import java.util.List;
import java.util.Map;


public class OperationProcessor {
    private final ParameterProcessor parameterProcessor;
    private final ResponseProcessor responseProcessor;
    private final ResolverCache cache;

    public OperationProcessor(ResolverCache cache, OpenAPI openApi) {
        this.cache = cache;
        parameterProcessor = new ParameterProcessor(cache, openApi);
        responseProcessor = new ResponseProcessor(cache, openApi);
    }

    public void processOperation(Operation operation) {
        final List<Parameter> processedOperationParameters = parameterProcessor.processParameters(operation.getParameters());
        operation.setParameters(processedOperationParameters);

        final Map<String, ApiResponse> responses = operation.getResponses();

        if (responses != null) {
            for (String responseCode : responses.keySet()) {
                ApiResponse response = responses.get(responseCode);

                if(response != null) {
                    /*if (response instanceof RefResponse) {
                        RefResponse refResponse = (RefResponse) response;
                        ApiResponse resolvedResponse = cache.loadRef(refResponse.get$ref(), refResponse.getRefFormat(), Response.class);

                        if (resolvedResponse != null) {
                            response = resolvedResponse;
                            responses.put(responseCode, resolvedResponse);
                        }
                    }*/
                    responseProcessor.processResponse(response);
                }
            }
        }
    }
}