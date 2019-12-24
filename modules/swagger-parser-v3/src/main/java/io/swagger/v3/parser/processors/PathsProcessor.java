package io.swagger.v3.parser.processors;



import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.callbacks.Callback;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.parser.OpenAPIResolver;
import io.swagger.v3.parser.ResolverCache;
import io.swagger.v3.parser.models.RefFormat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.swagger.v3.parser.util.RefUtils.computeRefFormat;

public class PathsProcessor {

    private final OpenAPI openAPI;
    private final ResolverCache cache;
    private final OpenAPIResolver.Settings settings;
    private final ParameterProcessor parameterProcessor;
    private final OperationProcessor operationProcessor;

    public PathsProcessor(ResolverCache cache, OpenAPI openAPI) {
        this(cache, openAPI, new OpenAPIResolver.Settings());
    }
    public PathsProcessor(ResolverCache cache, OpenAPI openAPI, OpenAPIResolver.Settings settings) {
        this.openAPI = openAPI;
        this.cache = cache;
        this.settings = settings;
        parameterProcessor = new ParameterProcessor(cache, openAPI);
        operationProcessor = new OperationProcessor(cache, openAPI);
    }

    public void processPaths() {
        final Map<String, PathItem> pathMap = openAPI.getPaths();

        if (pathMap == null) {
            return;
        }

        for (String pathStr : pathMap.keySet()) {
            PathItem pathItem = pathMap.get(pathStr);

            addParametersToEachOperation(pathItem);

            if (pathItem.get$ref() != null) {
                RefFormat refFormat = computeRefFormat(pathItem.get$ref());
                PathItem resolvedPath = cache.loadRef(pathItem.get$ref(), refFormat, PathItem.class);

                // TODO: update references to the parent location

                String pathRef = pathItem.get$ref().split("#")[0];

                if (resolvedPath != null) {
                    updateLocalRefs(resolvedPath, pathRef);
                    //we need to put the resolved path into swagger object
                    openAPI.path(pathStr, resolvedPath);
                    pathItem = resolvedPath;
                }
            }

            //at this point we can process this path
            final List<Parameter> processedPathParameters = parameterProcessor.processParameters(pathItem.getParameters());
            pathItem.setParameters(processedPathParameters);


            final Map<PathItem.HttpMethod, Operation> operationMap = pathItem.readOperationsMap();

            for (PathItem.HttpMethod httpMethod : operationMap.keySet()) {
                Operation operation = operationMap.get(httpMethod);
                operationProcessor.processOperation(operation);
            }
        }
    }

    private void addParametersToEachOperation(PathItem pathItem) {
        if (settings.addParametersToEachOperation()) {
            List<Parameter> parameters = pathItem.getParameters();

            if (parameters != null) {
                // add parameters to each operation
                List<Operation> operations = pathItem.readOperations();
                if (operations != null) {
                    for (Operation operation : operations) {
                        List<Parameter> parametersToAdd = new ArrayList<>();
                        List<Parameter> existingParameters = operation.getParameters();
                        if (existingParameters == null) {
                            existingParameters = new ArrayList<>();
                            operation.setParameters(existingParameters);
                        }
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
    }

    protected void updateLocalRefs(PathItem path, String pathRef) {
        if(path.getParameters() != null) {
            List<Parameter> params = path.getParameters();
            for(Parameter param : params) {
                updateLocalRefs(param, pathRef);
            }
        }
        List<Operation> ops = path.readOperations();
        for(Operation op : ops) {
            if(op.getParameters() != null) {
                for (Parameter param : op.getParameters()) {
                    updateLocalRefs(param, pathRef);
                }
            }
            if(op.getResponses() != null) {
                for(ApiResponse response : op.getResponses().values()) {
                    updateLocalRefs(response, pathRef);
                }
            }
            if (op.getRequestBody() != null){
                updateLocalRefs(op.getRequestBody(),pathRef);
            }
            if (op.getCallbacks() != null){
                Map<String,Callback> callbacks = op.getCallbacks();
                for (String name : callbacks.keySet()) {
                    Callback callback = callbacks.get(name);
                    if (callback != null) {
                        for(String callbackName : callback.keySet()) {
                            PathItem pathItem = callback.get(callbackName);
                            updateLocalRefs(pathItem,pathRef);
                        }
                    }
                }
            }
        }
    }

    protected void updateLocalRefs(ApiResponse response, String pathRef) {
        if (response.get$ref() != null){
            if(isLocalRef(response.get$ref())) {
                response.set$ref(computeLocalRef(response.get$ref(), pathRef));
            }
        }
        if(response.getContent() != null) {
            Map<String, MediaType> content = response.getContent();
            for (String key: content.keySet()) {
                MediaType mediaType = content.get(key);
                if (mediaType.getSchema() != null) {
                    updateLocalRefs(mediaType.getSchema(), pathRef);
                }
                Map<String, Example> examples = content.get(key).getExamples();
                if (examples != null) {
                    for( Example example:examples.values()){
                        updateLocalRefs(example, pathRef);
                    }
                }
            }
        }
    }

    protected void updateLocalRefs(Example example, String pathRef) {
        if(example.get$ref() != null) {
            if(isLocalRef(example.get$ref())) {
                example.set$ref(computeLocalRef(example.get$ref(), pathRef));
            }
        }
    }

    protected void updateLocalRefs(Parameter param, String pathRef) {
        if (param.get$ref() != null){
            if(isLocalRef(param.get$ref())) {
                param.set$ref(computeLocalRef(param.get$ref(), pathRef));
            }
        }
        if(param.getSchema() != null) {
            updateLocalRefs(param.getSchema(), pathRef);
        }
        if(param.getContent() != null) {
            Map<String, MediaType> content = param.getContent();
            for (String key: content.keySet()) {
                MediaType mediaType = content.get(key);
                if (mediaType.getSchema() != null) {
                    updateLocalRefs(mediaType.getSchema(), pathRef);
                }
            }
        }

    }

    protected void updateLocalRefs(RequestBody body, String pathRef) {
        if (body.get$ref() != null){
            if(isLocalRef(body.get$ref())) {
                body.set$ref(computeLocalRef(body.get$ref(), pathRef));
            }
        }
        if(body.getContent() != null) {
            Map<String, MediaType> content = body.getContent();
            for (String key: content.keySet()) {
                MediaType mediaType = content.get(key);
                if (mediaType.getSchema() != null) {
                    updateLocalRefs(mediaType.getSchema(), pathRef);
                }
                Map<String, Example> examples = content.get(key).getExamples();
                if (examples != null) {
                    for (Example example : examples.values()) {
                        updateLocalRefs(example, pathRef);
                    }
                }
            }
        }else if(body.get$ref() != null){

        }
    }

    protected void updateLocalRefs(Schema model, String pathRef) {
        if(model.get$ref() != null) {
            if(isLocalRef(model.get$ref())) {
                model.set$ref(computeLocalRef(model.get$ref(), pathRef));
            }
        }
        else if(model.getProperties() != null) {
            // process properties
            if(model.getProperties() != null) {
                Map<String,Schema> properties = model.getProperties();
                for(String key : properties.keySet()) {
                    Schema property = properties.get(key);
                    if (property != null) {
                        updateLocalRefs(property, pathRef);
                    }
                }
            }
        }
        else if(model instanceof ComposedSchema) {
            ComposedSchema composedSchema = (ComposedSchema) model;
            if (composedSchema.getAllOf() != null) {
                for (Schema innerModel : composedSchema.getAllOf()) {
                    updateLocalRefs(innerModel, pathRef);
                }
            }if (composedSchema.getAnyOf() != null) {
                for(Schema innerModel : composedSchema.getAnyOf()) {
                    updateLocalRefs(innerModel, pathRef);
                }
            }if (composedSchema.getOneOf() != null) {
                for (Schema innerModel : composedSchema.getOneOf()) {
                    updateLocalRefs(innerModel, pathRef);
                }
            }
        }
        else if(model instanceof ArraySchema) {
            ArraySchema arraySchema = (ArraySchema) model;
            if(arraySchema.getItems() != null) {
                updateLocalRefs(arraySchema.getItems(), pathRef);
            }
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
