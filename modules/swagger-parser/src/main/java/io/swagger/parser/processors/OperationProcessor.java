package io.swagger.parser.processors;

import io.swagger.models.Operation;
import io.swagger.models.RefResponse;
import io.swagger.models.Response;
import io.swagger.models.Swagger;
import io.swagger.models.parameters.Parameter;
import io.swagger.parser.ResolverCache;

import java.util.List;
import java.util.Map;


public class OperationProcessor {
    private final ParameterProcessor parameterProcessor;
    private final ResponseProcessor responseProcessor;
    private final ResolverCache cache;

    public OperationProcessor(ResolverCache cache, Swagger swagger) {
        this.cache = cache;
        parameterProcessor = new ParameterProcessor(cache, swagger);
        responseProcessor = new ResponseProcessor(cache, swagger);
    }

    public void processOperation(Operation operation) {
        final List<Parameter> processedOperationParameters = parameterProcessor.processParameters(operation.getParameters());
        operation.setParameters(processedOperationParameters);

        final Map<String, Response> responses = operation.getResponses();

        if (responses != null) {
            for (String responseCode : responses.keySet()) {
                Response response = responses.get(responseCode);

                if(response != null) {
                    if (response instanceof RefResponse) {
                        RefResponse refResponse = (RefResponse) response;
                        responseProcessor.processResponse(response);

                        Response resolvedResponse = cache.loadRef(refResponse.get$ref(), refResponse.getRefFormat(), Response.class);

                        if (resolvedResponse != null) {
                            response = resolvedResponse;
                            responses.put(responseCode, resolvedResponse);
                        }
                    }
                    responseProcessor.processResponse(response);
                }
            }
        }
    }
}
