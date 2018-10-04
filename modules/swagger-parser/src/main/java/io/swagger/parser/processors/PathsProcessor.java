package io.swagger.parser.processors;

import io.swagger.models.*;
import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.RefProperty;
import io.swagger.parser.ResolverCache;
import io.swagger.parser.SwaggerResolver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class PathsProcessor {

    private final Swagger swagger;
    private final ResolverCache cache;
    private final SwaggerResolver.Settings settings;
    private final ParameterProcessor parameterProcessor;
    private final OperationProcessor operationProcessor;

    public PathsProcessor(ResolverCache cache, Swagger swagger) {
        this(cache, swagger, new SwaggerResolver.Settings());
    }
    public PathsProcessor(ResolverCache cache, Swagger swagger, SwaggerResolver.Settings settings) {
        this.swagger = swagger;
        this.cache = cache;
        this.settings = settings;
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

            if (settings.addParametersToEachOperation()) {
                List<Parameter> parameters = path.getParameters();

                if (parameters != null) {
                    // add parameters to each operation
                    List<Operation> operations = path.getOperations();
                    if (operations != null) {
                        for (Operation operation : operations) {
                            List<Parameter> parametersToAdd = new ArrayList<Parameter>();
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
                path.setParameters(null);
            }

            if (path instanceof RefPath) {
                RefPath refPath = (RefPath) path;
                Path resolvedPath = cache.loadRef(refPath.get$ref(), refPath.getRefFormat(), Path.class);

                // TODO: update references to the parent location

                String pathRef = refPath.get$ref().split("#")[0];


                if (resolvedPath != null) {
                    updateLocalRefs(resolvedPath, pathRef);
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

    protected void updateLocalRefs(Path path, String pathRef) {
        if(path.getParameters() != null) {
            List<Parameter> params = path.getParameters();
            for(Parameter param : params) {
                updateLocalRefs(param, pathRef);
            }
        }
        List<Operation> ops = path.getOperations();
        for(Operation op : ops) {
            if(op.getParameters() != null) {
                for (Parameter param : op.getParameters()) {
                    updateLocalRefs(param, pathRef);
                }
            }
            if(op.getResponses() != null) {
                for(Response response : op.getResponses().values()) {
                    updateLocalRefs(response, pathRef);
                }
            }
        }
    }

    protected void updateLocalRefs(Response response, String pathRef) {
        if(response.getResponseSchema() != null) {
            updateLocalRefs(response.getResponseSchema(), pathRef);
        }
    }

    protected void updateLocalRefs(Parameter param, String pathRef) {
        if(param instanceof BodyParameter) {
            BodyParameter bp = (BodyParameter) param;
            if(bp.getSchema() != null) {
                updateLocalRefs(bp.getSchema(), pathRef);
            }
        }
    }

    protected void updateLocalRefs(Model model, String pathRef) {
        if(model instanceof RefModel) {
            RefModel refModel = (RefModel) model;
            if(isLocalRef(refModel.get$ref())) {
                refModel.set$ref(computeLocalRef(refModel.get$ref(), pathRef));
            }/*else if(isLocalRef(refModel.getOriginalRef())) {
                    refModel.set$ref(computeLocalRef(refModel.getOriginalRef(), pathRef));
            }*/
        }
        else if(model instanceof ModelImpl) {
            // process properties
            ModelImpl impl = (ModelImpl) model;
            if(impl.getProperties() != null) {
                for(Property property : impl.getProperties().values()) {
                    updateLocalRefs(property, pathRef);
                }
            }
        }
        else if(model instanceof ComposedModel) {
            ComposedModel cm = (ComposedModel) model;
            for(Model innerModel : cm.getAllOf()) {
                updateLocalRefs(innerModel, pathRef);
            }
        }
        else if(model instanceof ArrayModel) {
            ArrayModel am = (ArrayModel) model;
            if(am.getItems() != null) {
                updateLocalRefs(am.getItems(), pathRef);
            }
        }
    }

    protected void updateLocalRefs(Property property, String pathRef) {
        if(property instanceof RefProperty) {
            RefProperty ref = (RefProperty) property;
            if(isLocalRef(ref.get$ref())) {
                ref.set$ref(computeLocalRef(ref.get$ref(), pathRef));
            }/*else if(isLocalRef(ref.getOriginalRef())) {
                ref.set$ref(computeLocalRef(ref.getOriginalRef(), pathRef));
            }*/
        }
    }

    protected boolean isLocalRef(String ref) {
        if(ref.startsWith("#")) {
            return true;
        }
        return false;
    }

    protected String computeLocalRef(String ref, String prefix) {
        return prefix + ref;
    }
}
