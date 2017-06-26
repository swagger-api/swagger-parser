package io.swagger.parser.v3.processors;

import io.swagger.oas.models.Operation;

import io.swagger.oas.models.PathItem;
import io.swagger.oas.models.callbacks.Callback;
import io.swagger.oas.models.parameters.RequestBody;
import io.swagger.oas.models.responses.ApiResponse;
import io.swagger.oas.models.OpenAPI;
import io.swagger.oas.models.parameters.Parameter;
import io.swagger.oas.models.responses.ApiResponses;
import io.swagger.parser.v3.ResolverCache;
import io.swagger.parser.v3.models.RefFormat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.swagger.parser.v3.util.RefUtils.computeRefFormat;


public class OperationProcessor {
    private final ParameterProcessor parameterProcessor;
    private final RequestBodyProcessor requestBodyProcessor;
    private final ResponseProcessor responseProcessor;
    private final ResolverCache cache;


    public OperationProcessor(ResolverCache cache, OpenAPI openApi) {
        parameterProcessor = new ParameterProcessor(cache, openApi);
        responseProcessor = new ResponseProcessor(cache,openApi);
        requestBodyProcessor = new RequestBodyProcessor(cache,openApi);
        this.cache = cache;
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
                    if (callback.get("$ref") != null){
                        String $ref = callback.get("$ref").get$ref();
                        RefFormat refFormat = computeRefFormat($ref);
                        Callback resolvedCallback = cache.loadRef($ref, refFormat, Callback.class);
                        if(resolvedCallback != null){
                            callbacks.replace(name,callback,resolvedCallback);
                            operation.setCallbacks(callbacks);
                        }
                    }//resolve callback: operations, parameters
                    for(String callbackName : callback.keySet()) {
                        PathItem pathItem = callback.get(callbackName);
                        final Map<PathItem.HttpMethod, Operation> operationMap = pathItem.readOperationsMap();

                        for (PathItem.HttpMethod httpMethod : operationMap.keySet()) {
                            Operation innerOperation = operationMap.get(httpMethod);
                            Operation resolvedOperation = processOperation(innerOperation);

                            if (PathItem.HttpMethod.GET.equals(httpMethod)) {
                                pathItem.setGet(resolvedOperation);
                            } else if (PathItem.HttpMethod.POST.equals(httpMethod)) {
                                pathItem.setPost(resolvedOperation);
                            } else if (PathItem.HttpMethod.PUT.equals(httpMethod)) {
                                pathItem.setPut(resolvedOperation);
                            } else if (PathItem.HttpMethod.DELETE.equals(httpMethod)) {
                                pathItem.setDelete(resolvedOperation);
                            } else if (PathItem.HttpMethod.TRACE.equals(httpMethod)) {
                                pathItem.setTrace(resolvedOperation);
                            } else if (PathItem.HttpMethod.OPTIONS.equals(httpMethod)) {
                                pathItem.setOptions(resolvedOperation);
                            } else if (PathItem.HttpMethod.HEAD.equals(httpMethod)) {
                                pathItem.setHead(resolvedOperation);
                            } else if (PathItem.HttpMethod.PATCH.equals(httpMethod)) {
                                pathItem.setPatch(resolvedOperation);
                            }
                            //callback.addPathItem();
                        }


                        List<Parameter> parameters = pathItem.getParameters();
                        List<Parameter> resolvedParameters = new ArrayList<>();
                        if (parameters != null) {
                            for (Parameter parameter : parameters) {
                                Parameter resolvedParameter = parameterProcessor.processParameter(parameter);
                                resolvedParameters.add(resolvedParameter);
                                pathItem.setParameters(resolvedParameters);
                            }
                        }
                        operation.setCallbacks(callbacks);
                    }
                }
            }
        }
        return operation;
    }
}