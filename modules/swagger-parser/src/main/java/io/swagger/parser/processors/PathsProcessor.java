package io.swagger.parser.processors;

import io.swagger.models.HttpMethod;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.RefPath;
import io.swagger.models.Swagger;
import io.swagger.models.parameters.Parameter;
import io.swagger.parser.ResolverCache;

import java.util.List;
import java.util.Map;


public class PathsProcessor {

    private final Swagger swagger;
    private final ResolverCache cache;
    private final ParameterProcessor parameterProcessor;
    private final OperationProcessor operationProcessor;

    public PathsProcessor(ResolverCache cache, Swagger swagger) {
        this.swagger = swagger;
        this.cache = cache;
        parameterProcessor = new ParameterProcessor(cache, swagger);
        operationProcessor = new OperationProcessor(cache, swagger);
    }

    public void processPaths() {
        final Map<String, Path> pathMap = swagger.getPaths();

        if (pathMap == null) {
            return;
        }

        for (String pathStr : pathMap.keySet()) {
            Path path = pathMap.get(pathStr);

            List<Parameter> parameters = path.getParameters();

            if(parameters != null) {
                // add parameters to each operation
                List<Operation> operations = path.getOperations();
                if(operations != null) {
                    for(Operation operation : operations) {
                        operation.getParameters().addAll(0, parameters);
                    }
                }
            }
            // remove the shared parameters
            path.setParameters(null);

            if (path instanceof RefPath) {
                RefPath refPath = (RefPath) path;
                Path resolvedPath = cache.loadRef(refPath.get$ref(), refPath.getRefFormat(), Path.class);

                if (resolvedPath != null) {
                    //we need to put the resolved path into swagger object
                    swagger.path(pathStr, resolvedPath);
                    path = resolvedPath;
                }
            }

            //at this point we can process this path
            final List<Parameter> processedPathParameters = parameterProcessor.processParameters(path.getParameters());
            path.setParameters(processedPathParameters);

            final Map<HttpMethod, Operation> operationMap = path.getOperationMap();

            for (HttpMethod httpMethod : operationMap.keySet()) {
                Operation operation = operationMap.get(httpMethod);
                operationProcessor.processOperation(operation);
            }
        }
    }
}
