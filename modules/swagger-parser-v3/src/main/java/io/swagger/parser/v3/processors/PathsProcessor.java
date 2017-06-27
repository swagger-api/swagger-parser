package io.swagger.parser.v3.processors;



import io.swagger.oas.models.Operation;
import io.swagger.oas.models.PathItem;
import io.swagger.oas.models.parameters.Parameter;
import io.swagger.oas.models.OpenAPI;
import io.swagger.parser.v3.ResolverCache;
import io.swagger.parser.v3.OpenAPIResolver;
import io.swagger.parser.v3.models.RefFormat;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.swagger.parser.v3.util.RefUtils.computeRefFormat;


public class PathsProcessor {

    private final OpenAPI openApi;
    private final ResolverCache cache;
    private final OpenAPIResolver.Settings settings;
    private final ParameterProcessor parameterProcessor;
    private final OperationProcessor operationProcessor;


    public PathsProcessor(ResolverCache cache, OpenAPI openApi) {
        this(cache, openApi, new OpenAPIResolver.Settings());
    }

    public PathsProcessor(ResolverCache cache, OpenAPI openApi, OpenAPIResolver.Settings settings) {
        this.openApi = openApi;
        this.cache = cache;
        this.settings = settings;
        parameterProcessor = new ParameterProcessor(cache, openApi);
        operationProcessor = new OperationProcessor(cache, openApi);
    }

    public void processPaths() {
        final Map<String, PathItem> pathMap = openApi.getPaths();

        if (pathMap == null) {
            return;
        }

        for (String pathStr : pathMap.keySet()) {
            PathItem pathItem = pathMap.get(pathStr);

            if (settings.addParametersToEachOperation()) {
                List<Parameter> parameters = pathItem.getParameters();

                setParametersToEachOperation(pathItem,parameters);
            }

            if (pathItem.get$ref() != null) {
                RefFormat refFormat = computeRefFormat(pathItem.get$ref());
                PathItem resolvedPathItem = cache.loadRef(pathItem.get$ref(), refFormat, PathItem.class);

                //String pathRef = pathItem.get$ref().split("#")[0];
                resolvePathItem(resolvedPathItem);

                if (resolvedPathItem != null) {
                    //we need to put the resolved path into openApi object
                    openApi.path(pathStr, resolvedPathItem);
                    pathItem = resolvedPathItem;
                }
            }

            //at this point we can process this path
            final List<Parameter> processedPathParameters = parameterProcessor.processParameters(pathItem.getParameters());
            if (processedPathParameters != null) {
                pathItem.setParameters(processedPathParameters);
            }
            final Map<PathItem.HttpMethod, Operation> operationMap = pathItem.readOperationsMap();
            setParametersToEachOperation(pathItem,processedPathParameters);
            for (PathItem.HttpMethod httpMethod : operationMap.keySet()) {
                Operation operation = operationMap.get(httpMethod);
                Operation resolvedOperation = operationProcessor.processOperation(operation);
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
            }
        }
    }

    private void setParametersToEachOperation(PathItem pathItem, List<Parameter> parameters) {
        if (parameters != null) {
            // add parameters to each operation
            List<Operation> operations = pathItem.readOperations();
            if (operations != null) {
                for (Operation operation : operations) {
                    List<Parameter> parametersToAdd = new ArrayList<>();
                    List<Parameter> existingParameters = operation.getParameters();
                    for (Parameter parameterToAdd : parameters) {
                        boolean matched = false;
                        for (Parameter existingParameter : existingParameters) {
                            if (parameterToAdd.getIn() != null && parameterToAdd.getIn().equals(existingParameter.getIn()) &&
                                    parameterToAdd.getName().equals(existingParameter.getName())) {
                                matched = true;
                            }
                        }
                        if (!matched) {
                            parametersToAdd.add(parameterToAdd);
                        }
                    }
                    if (parametersToAdd.size() > 0) {
                        operation.getParameters().addAll(0, parametersToAdd);
                    }
                }
            }
        }
        // remove the shared parameters
        pathItem.setParameters(null);
    }


    protected void resolvePathItem(PathItem pathItem) {
        if (pathItem.getParameters() != null) {
            List<Parameter> resolvedParameters = new ArrayList<>();
            List<Parameter> params = pathItem.getParameters();
            for (Parameter param : params) {
                Parameter resolvedParameter = parameterProcessor.processParameter(param);
                resolvedParameters.add(resolvedParameter);
                pathItem.setParameters(resolvedParameters);
            }
        }
    }
}