package io.swagger.parser;

import io.swagger.models.*;
import io.swagger.models.auth.AuthorizationValue;
import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.parameters.RefParameter;
import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.MapProperty;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.RefProperty;
import io.swagger.models.refs.RefFormat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.swagger.parser.util.RefUtils.*;

/**
 *
 */
public class SwaggerResolver2 {

    private Swagger swagger;
    private ResolverCache cache;


    public Swagger resolve(Swagger swagger, List<AuthorizationValue> auths) {
        if (swagger == null) {
            return null;
        }

        this.cache = new ResolverCache(swagger, auths);
        this.swagger = swagger;

        processPaths();
        processDefinitions();

        return swagger;
    }

    private void processDefinitions() {
        final Map<String, Model> definitions = swagger.getDefinitions();

        if (definitions == null) {
            return;
        }

        for (Map.Entry<String, Model> definitionEntry : definitions.entrySet()) {
            final Model model = definitionEntry.getValue();
            processModel(model);
        }
    }

    private void processPaths() {
        final Map<String, Path> pathMap = swagger.getPaths();

        if (pathMap == null) {
            return;
        }

        for (String pathStr : pathMap.keySet()) {
            Path path = pathMap.get(pathStr);

            if (path instanceof RefPath) {
                RefPath refPath = (RefPath) path;
                PathImpl resolvedPath = cache.loadRef(refPath.get$ref(), refPath.getRefFormat(), PathImpl.class);

                if (resolvedPath != null) {
                    //we need to put the resolved path into swagger object
                    swagger.path(pathStr, resolvedPath);
                    path = resolvedPath;
                }
            }

            //at this point we can process this path
            processPath(path);
        }
    }

    private void processPath(Path path) {
        //first lets iterate over the path's parameters
        final List<Parameter> processedPathParameters = processParameters(path.getParameters());
        path.setParameters(processedPathParameters);

        final Map<HttpMethod, Operation> operationMap = path.getOperationMap();

        for (HttpMethod httpMethod : operationMap.keySet()) {
            Operation operation = operationMap.get(httpMethod);

            processOperation(operation);
        }
    }

    private void processOperation(Operation operation) {
        final List<Parameter> processedOperationParameters = processParameters(operation.getParameters());
        operation.setParameters(processedOperationParameters);

        final Map<String, Response> responses = operation.getResponses();

        for (String responseCode : responses.keySet()) {
            Response response = responses.get(responseCode);

            if (response instanceof RefResponse) {
                RefResponse refResponse = (RefResponse) response;
                ResponseImpl resolvedResponse = cache.loadRef(refResponse.get$ref(), refResponse.getRefFormat(), ResponseImpl.class);

                if (resolvedResponse != null) {
                    responses.put(responseCode, resolvedResponse);
                }
            }
            processResponse(response);
        }
    }

    private void processResponse(Response response) {
        //process the response body
        final Property schema = response.getSchema();

        if (schema != null) {
            processProperty(schema);
        }

        //process the response headers
        final Map<String, Property> headers = response.getHeaders();
        for (Map.Entry<String, Property> responseHdrEntry : headers.entrySet()) {
            final Property responseHeader = responseHdrEntry.getValue();
            processProperty(responseHeader);
        }
    }


    private List<Parameter> processParameters(List<Parameter> parameters) {
        final List<Parameter> incomingParameterList = parameters;
        final List<Parameter> processedPathLevelParameters = new ArrayList<>();

        for (Parameter parameter : incomingParameterList) {
            if (parameter instanceof RefParameter) {
                RefParameter refParameter = (RefParameter) parameter;

                final Parameter referencedParameter = cache.loadRef(refParameter.get$ref(), refParameter.getRefFormat(), Parameter.class);

                if (referencedParameter != null) {
                    processedPathLevelParameters.add(referencedParameter);
                }

            } else {
                if (parameter instanceof BodyParameter) {
                    BodyParameter bodyParameter = (BodyParameter) parameter;
                    final Model schema = bodyParameter.getSchema();
                    processModel(schema);
                }
                processedPathLevelParameters.add(parameter);
            }
        }
        return processedPathLevelParameters;
    }

    /*
        Model Processing Methods
     */

    private void processModel(Model model) {
        if (model == null) {
            return;
        }

        if (model instanceof RefModel) {
            processRefModel((RefModel) model);
        } else if (model instanceof ArrayModel) {
            processArrayModel((ArrayModel) model);
        } else if (model instanceof ComposedModel) {
            processComposedModel((ComposedModel) model);
        } else if (model instanceof ModelImpl) {
            processModelImpl((ModelImpl) model);
        }
    }

    private void processModelImpl(ModelImpl modelImpl) {

        final Map<String, Property> properties = modelImpl.getProperties();

        if (properties == null) {
            return;
        }

        for (Map.Entry<String, Property> propertyEntry : properties.entrySet()) {
            final Property property = propertyEntry.getValue();
            processProperty(property);
        }

    }

    private void processComposedModel(ComposedModel composedModel) {

        /*
        we only need to process the "allOf" list, because ComposedModel.parent, ComposedModel.child, and ComposedModel.interfaces
        all get populated from values in in "allOf"
         */
        final List<Model> allOf = composedModel.getAllOf();

        for (Model model : allOf) {
            processModel(model);
        }
    }

    private void processArrayModel(ArrayModel arrayModel) {

        final Property items = arrayModel.getItems();

        //TODO ArrayModel has a properties map, but my reading of the swagger spec makes me think it should be ignored

        if (items != null) {
            processProperty(items);
        }
    }


    private void processRefModel(RefModel refModel) {
    /* if this is a URL or relative ref:
        1) we need to load it into memory.
        2) shove it into the #/definitions
        3) update the RefModel to point to its location in #/definitions
     */
        if (isAnExternalRefFormat(refModel.getRefFormat())) {
            final String newRef = processRefToExternalDefinition(refModel.get$ref(), refModel.getRefFormat());

            if (newRef != null) {
                refModel.set$ref(newRef);
            }
        }
    }

    /*
        Property Processing Methods
     */

    private void processProperty(Property property) {
        if (property instanceof RefProperty) {
            processRefProperty((RefProperty) property);
        } else if (property instanceof ArrayProperty) {
            processArrayProperty((ArrayProperty) property);
        } else if (property instanceof MapProperty) {
            processMapProperty((MapProperty) property);
        }
    }

    private void processRefProperty(RefProperty refProperty) {
        if (isAnExternalRefFormat(refProperty.getRefFormat())) {
            final String newRef = processRefToExternalDefinition(refProperty.get$ref(), refProperty.getRefFormat());

            if (newRef != null) {
                refProperty.set$ref(newRef);
            }
        }
    }

    private void processMapProperty(MapProperty property) {
        final Property additionalProperties = property.getAdditionalProperties();
        if (additionalProperties != null) {
            processProperty(additionalProperties);
        }
    }

    private void processArrayProperty(ArrayProperty property) {
        final Property items = property.getItems();
        if (items != null) {
            processProperty(items);
        }
    }

    /*
        Generic Definition Processing
     */

    private String processRefToExternalDefinition(String $ref, RefFormat refFormat) {
        final Model model = cache.loadRef($ref, refFormat, Model.class);

        String newRef = null;

        if (model != null) {
            final Map<String, Model> definitions = swagger.getDefinitions();
            final String possiblyConflictingDefinitionName = computeDefinitionName($ref);

            if (definitions.containsKey(possiblyConflictingDefinitionName)) {
                //so this is either a conflict, or a second reference to the same value
                if (cache.getRenamedRef($ref) != null) {
                    //this is an additional reference to something we have renamed once
                    //it exists in the definitions table, all we have to do is set the new $ref value on the ref
                    newRef = possiblyConflictingDefinitionName;
                } else {
                    //this is a conflict
                    String deconflictedName = deconflictName(possiblyConflictingDefinitionName, definitions);
                    cache.putRenamedRef($ref, deconflictedName);
                    newRef = deconflictedName;
                    swagger.addDefinition(deconflictedName, model);
                }
            } else {
                newRef = possiblyConflictingDefinitionName;
            }
        }

        return newRef;
    }

}
