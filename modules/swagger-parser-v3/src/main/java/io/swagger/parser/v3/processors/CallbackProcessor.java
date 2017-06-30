package io.swagger.parser.v3.processors;

import io.swagger.oas.models.OpenAPI;
import io.swagger.oas.models.Operation;
import io.swagger.oas.models.PathItem;
import io.swagger.oas.models.callbacks.Callback;
import io.swagger.oas.models.parameters.Parameter;
import io.swagger.parser.v3.ResolverCache;
import io.swagger.parser.v3.models.RefFormat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


import static io.swagger.parser.v3.util.RefUtils.computeRefFormat;

/**
 * Created by gracekarina on 23/06/17.
 */
public class CallbackProcessor {
    private final ResolverCache cache;
    private final OperationProcessor operationProcessor;
    private final ParameterProcessor parameterProcessor;
    private final OpenAPI openApi;

    public CallbackProcessor(ResolverCache cache, OpenAPI openApi) {
        this.cache = cache;
        this.operationProcessor = new OperationProcessor(cache, openApi);
        this.parameterProcessor = new ParameterProcessor(cache,openApi);
        this.openApi = openApi;
    }

    public Callback processCallback(Callback callback) {
        if (callback.get("$ref") != null){
            callback = processReferenceCallback(callback);
        }
        //Resolver PathItem
        for(String name : callback.keySet()){
            PathItem pathItem = callback.get(name);
            final Map<PathItem.HttpMethod, Operation> operationMap = pathItem.readOperationsMap();

            for (PathItem.HttpMethod httpMethod : operationMap.keySet()) {
                Operation operation = operationMap.get(httpMethod);
                Operation resolvedOperation = operationProcessor.processOperation(operation);

                if(PathItem.HttpMethod.GET.equals(httpMethod)) {
                    pathItem.setGet(resolvedOperation);
                }
                else if(PathItem.HttpMethod.POST.equals(httpMethod)) {
                    pathItem.setPost(resolvedOperation);
                }
                else if(PathItem.HttpMethod.PUT.equals(httpMethod)) {
                    pathItem.setPut(resolvedOperation);
                }
                else if(PathItem.HttpMethod.DELETE.equals(httpMethod)) {
                    pathItem.setDelete(resolvedOperation);
                }
                else if(PathItem.HttpMethod.TRACE.equals(httpMethod)) {
                    pathItem.setTrace(resolvedOperation);
                }
                else if(PathItem.HttpMethod.OPTIONS.equals(httpMethod)) {
                    pathItem.setOptions(resolvedOperation);
                }
                else if(PathItem.HttpMethod.HEAD.equals(httpMethod)) {
                    pathItem.setHead(resolvedOperation);
                }
                else if(PathItem.HttpMethod.PATCH.equals(httpMethod)) {
                    pathItem.setPatch(resolvedOperation);
                }
            }

            List<Parameter> parameters = pathItem.getParameters();
            List<Parameter> resolvedParameters = new ArrayList<>();
            if (parameters != null) {
                for (Parameter parameter: parameters){
                    Parameter resolvedParameter = parameterProcessor.processParameter(parameter);
                    resolvedParameters.add(resolvedParameter);
                    pathItem.setParameters(resolvedParameters);
                }
            }
        }
        return callback;
    }

    public Callback processReferenceCallback(Callback callback ){
        String $ref = callback.get("$ref").get$ref();
        RefFormat refFormat = computeRefFormat($ref);
        Callback resolvedCallback = cache.loadRef($ref, refFormat, Callback.class);
        return resolvedCallback;
    }
}
