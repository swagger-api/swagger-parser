package io.swagger.parser.v3.processors;

import io.swagger.oas.models.Operation;

import io.swagger.oas.models.callbacks.Callback;
import io.swagger.oas.models.parameters.RequestBody;
import io.swagger.oas.models.responses.ApiResponse;
import io.swagger.oas.models.OpenAPI;
import io.swagger.oas.models.parameters.Parameter;
import io.swagger.oas.models.responses.ApiResponses;
import io.swagger.parser.v3.ResolverCache;
import java.util.List;
import java.util.Map;




public class OperationProcessor {
    private final ParameterProcessor parameterProcessor;
    private final RequestBodyProcessor requestBodyProcessor;
    private final ResponseProcessor responseProcessor;
    //private final CallbackProcessor callbackProcessor;


    public OperationProcessor(ResolverCache cache, OpenAPI openApi) {
        parameterProcessor = new ParameterProcessor(cache, openApi);
        responseProcessor = new ResponseProcessor(cache,openApi);
        requestBodyProcessor = new RequestBodyProcessor(cache,openApi);
        //callbackProcessor = new CallbackProcessor(cache,openApi);
    }

    public Operation processOperation(Operation operation) {
        final List<Parameter> processedOperationParameters = parameterProcessor.processParameters(operation.getParameters());
        operation.setParameters(processedOperationParameters);

        final RequestBody requestBody = operation.getRequestBody();
        if(requestBody != null) {
            RequestBody resolvedBody = requestBodyProcessor.processRequestBody(requestBody);
            if(requestBody != null){
                operation.setRequestBody(resolvedBody);
            }
        }


        final Map<String, ApiResponse> responses = operation.getResponses();
        ApiResponses resolvedResponses = new ApiResponses();
        if (responses != null) {
            for (String responseCode : responses.keySet()) {
                ApiResponse response = responses.get(responseCode);
                if(response != null) {
                    ApiResponse resolvedResponse = responseProcessor.processResponse(response);
                    if(resolvedResponse != null){
                        resolvedResponses.addApiResponse(responseCode,resolvedResponse);
                        operation.setResponses(resolvedResponses);
                    }
                }
            }
        }

        final Map<String, Callback> callbacks = operation.getCallbacks();
        if (callbacks != null) {
            for (String name : callbacks.keySet()) {
                Callback callback = callbacks.get(name);
                if(callback != null) {
                    /*Callback resolvedCallback = callbackProcessor.processCallback(callback);
                    if(resolvedCallback != null){
                        callbacks.replace(name,callback,resolvedCallback);
                        operation.setCallbacks(callbacks);
                    }*/
                }
            }
        }
        return operation;
    }
}