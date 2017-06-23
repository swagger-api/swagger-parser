package io.swagger.parser.v3.processors;

import io.swagger.oas.models.Operation;
//import io.swagger.models.RefResponse;
import io.swagger.oas.models.parameters.RequestBody;
import io.swagger.oas.models.responses.ApiResponse;
import io.swagger.oas.models.OpenAPI;
import io.swagger.oas.models.parameters.Parameter;
import io.swagger.parser.v3.ResolverCache;
import io.swagger.parser.v3.models.RefFormat;

import java.util.List;
import java.util.Map;

import static io.swagger.parser.v3.util.RefUtils.computeRefFormat;


public class OperationProcessor {
    private final ParameterProcessor parameterProcessor;
    private final ResponseProcessor responseProcessor;
    private final ResolverCache cache;

    public OperationProcessor(ResolverCache cache, OpenAPI openApi) {
        this.cache = cache;
        parameterProcessor = new ParameterProcessor(cache, openApi);
        responseProcessor = new ResponseProcessor(cache,openApi);
    }

    public void processOperation(Operation operation) {
        final List<Parameter> processedOperationParameters = parameterProcessor.processParameters(operation.getParameters());
        operation.setParameters(processedOperationParameters);

        final RequestBody requestBody = operation.getRequestBody();



        final Map<String, ApiResponse> responses = operation.getResponses();

        if (responses != null) {
            for (String responseCode : responses.keySet()) {
                ApiResponse response = responses.get(responseCode);

                if(response != null) {
                    if (response.get$ref() != null) {
                        RefFormat refFormat = computeRefFormat(response.get$ref());
                        ApiResponse resolvedResponse = cache.loadRef(response.get$ref(), refFormat, ApiResponse.class);

                        if (resolvedResponse != null) {
                            response = resolvedResponse;
                            responses.put(responseCode, resolvedResponse);
                        }
                    }
                    responseProcessor.processResponse(responseCode,response);
                }
            }
        }
    }
}