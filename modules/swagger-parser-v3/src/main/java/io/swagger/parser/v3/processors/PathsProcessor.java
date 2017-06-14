package io.swagger.parser.v3.processors;



import io.swagger.oas.models.Operation;
import io.swagger.oas.models.PathItem;
import io.swagger.oas.models.media.AllOfSchema;
import io.swagger.oas.models.media.ArraySchema;
import io.swagger.oas.models.media.MediaType;
import io.swagger.oas.models.media.Schema;
import io.swagger.oas.models.parameters.Parameter;
import io.swagger.oas.models.OpenAPI;
import io.swagger.oas.models.parameters.RequestBody;
import io.swagger.oas.models.responses.ApiResponse;
import io.swagger.parser.ResolverCache;
import io.swagger.parser.v3.OpenAPIResolver;


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;


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

                if (parameters != null) {
                    // add parameters to each operation
                    List<Operation> operations = null;//pathItem.getOperations();
                    if(pathItem.getGet()!= null) {
                        operations = new ArrayList<>();
                        operations.add(pathItem.getGet());
                    }if(pathItem.getPost()!= null) {
                        operations = new ArrayList<>();
                        operations.add(pathItem.getPost());
                    }if(pathItem.getDelete()!= null) {
                        operations = new ArrayList<>();
                        operations.add(pathItem.getDelete());
                    }if(pathItem.getPatch()!= null) {
                        operations = new ArrayList<>();
                        operations.add(pathItem.getPatch());
                    }if(pathItem.getPut()!= null) {
                        operations = new ArrayList<>();
                        operations.add(pathItem.getPut());
                    }if(pathItem.getPatch()!= null) {
                        operations = new ArrayList<>();
                        operations.add(pathItem.getPatch());
                    }if(pathItem.getHead()!= null) {
                        operations = new ArrayList<>();
                        operations.add(pathItem.getHead());
                    }if(pathItem.getOptions()!= null) {
                        operations = new ArrayList<>();
                        operations.add(pathItem.getOptions());
                    }
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

           /*if (pathItem instanceof RefPath) {
                RefPath refPath = (RefPath) pathItem;
                PathItem resolvedPath = cache.loadRef(refPath.get$ref(), refPath.getRefFormat(), PathItem.class);

                // TODO: update references to the parent location

                String pathRef = refPath.get$ref().split("#")[0];
                updateLocalRefs(resolvedPath, pathRef);

                if (resolvedPath != null) {
                    //we need to put the resolved path into swagger object
                    openApi.path(pathStr, resolvedPath);
                    pathItem = resolvedPath;
                }
            }*/

            //at this point we can process this path
            /*final List<Parameter> processedPathParameters = parameterProcessor.processParameters(pathItem.getParameters());
            pathItem.setParameters(processedPathParameters);

            final Map<HttpMethod, Operation> operationMap = pathItem.getOperationMap();

            for (HttpMethod httpMethod : operationMap.keySet()) {
                Operation operation = operationMap.get(httpMethod);
                operationProcessor.processOperation(operation);
            }*/
        }
    }

    protected void updateLocalRefs(PathItem pathItem, String pathRef) {
        if(pathItem.getParameters() != null) {
            List<Parameter> params = pathItem.getParameters();
            for(Parameter param : params) {
                Map <String,MediaType> content = param.getContent();
                for( Map.Entry<String, MediaType> map : content.entrySet()) {
                    if(map.getValue().getSchema()!= null) {
                        updateLocalRefs(map.getValue().getSchema(), pathRef);
                    }
                }
            }
        }
        //List<Operation> ops = pathItem.getOperations();
        List<Operation> operations = null;//pathItem.getOperations();
        if(pathItem.getGet()!= null) {
            operations = new ArrayList<>();
            operations.add(pathItem.getGet());
        }if(pathItem.getPost()!= null) {
            operations = new ArrayList<>();
            operations.add(pathItem.getPost());
        }if(pathItem.getDelete()!= null) {
            operations = new ArrayList<>();
            operations.add(pathItem.getDelete());
        }if(pathItem.getPatch()!= null) {
            operations = new ArrayList<>();
            operations.add(pathItem.getPatch());
        }if(pathItem.getPut()!= null) {
            operations = new ArrayList<>();
            operations.add(pathItem.getPut());
        }if(pathItem.getPatch()!= null) {
            operations = new ArrayList<>();
            operations.add(pathItem.getPatch());
        }if(pathItem.getHead()!= null) {
            operations = new ArrayList<>();
            operations.add(pathItem.getHead());
        }if(pathItem.getOptions()!= null) {
            operations = new ArrayList<>();
            operations.add(pathItem.getOptions());
        }
        for(Operation op : operations) {
            if(op.getRequestBody() != null) {
                updateLocalRefs(op.getRequestBody(), pathRef);
            }
            if(op.getResponses() != null) {
                for(ApiResponse response : op.getResponses().values()) {
                    updateLocalRefs(response, pathRef);
                }
            }
        }
    }

    protected void updateLocalRefs(ApiResponse response, String pathRef) {
        Map <String,MediaType> content = response.getContent();
        for( Map.Entry<String, MediaType> map : content.entrySet()) {
            if(map.getValue().getSchema()!= null) {
                updateLocalRefs(map.getValue().getSchema(), pathRef);
            }
        }

    }

    /*protected void updateLocalRefs(Parameter param, String pathRef) {
        if(param instanceof BodyParameter) {
            BodyParameter bp = (BodyParameter) param;
            if(bp.getSchema() != null) {
                updateLocalRefs(bp.getSchema(), pathRef);
            }
        }
    }*/

    protected void updateLocalRefs(RequestBody requestBody, String pathRef) {
        Map <String,MediaType> content = requestBody.getContent();;
        for( Map.Entry<String, MediaType> map : content.entrySet()) {
            if(map.getValue().getSchema()!= null) {
                updateLocalRefs(map.getValue().getSchema(), pathRef);
            }
        }
    }

    protected void updateLocalRefs(Schema schema, String pathRef) {
        if(schema.get$ref()!= null){
            //TODO Resolve the $ref
        }
        /*if(schema instanceof RefModel) {
            RefModel refModel = (RefModel) schema;
            if(isLocalRef(refModel.get$ref())) {
                refModel.set$ref(computeLocalRef(refModel.get$ref(), pathRef));
            }
        }*/
        else if(schema instanceof Schema) {
            // process properties
            //Schema schema = (Schema) schema;
            if(schema.getProperties() != null) {
                Collection<Schema> properties = schema.getProperties().values();
                for( Schema property : properties) {
                    updateLocalRefs(property, pathRef);
                }
            }
        }
        else if(schema instanceof AllOfSchema) {
            AllOfSchema allOfSchema = (AllOfSchema) schema;
            for(Schema innerSchema : allOfSchema.getAllOf()) {
                updateLocalRefs(innerSchema, pathRef);
            }
        }
        else if(schema instanceof ArraySchema) {
            ArraySchema arraySchema = (ArraySchema) schema;
            if(arraySchema.getItems() != null) {
                updateLocalRefs(arraySchema.getItems(), pathRef);
            }
        }
    }

    /*protected void updateLocalRefs(Schema schema, String pathRef) {
        if(schema instanceof RefProperty) {
            RefProperty ref = (RefProperty) schema;
            if(isLocalRef(ref.get$ref())) {
                ref.set$ref(computeLocalRef(ref.get$ref(), pathRef));
            }
        }
    }*/

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
