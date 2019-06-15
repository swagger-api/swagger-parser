package io.swagger.v3.parser.util;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.callbacks.Callback;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.links.Link;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.MapSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.parser.models.RefFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static io.swagger.v3.parser.util.RefUtils.computeDefinitionName;
import static io.swagger.v3.parser.util.RefUtils.computeRefFormat;
import static io.swagger.v3.parser.util.RefUtils.isAnExternalRefFormat;

public class ResolverFully {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResolverFully.class);

    private boolean aggregateCombinators;



    public ResolverFully() {
        this(true);
    }

    public ResolverFully(boolean aggregateCombinators) {
        this.aggregateCombinators = aggregateCombinators;
    }

    private Map<String, Schema> schemas;
    private Map<String, Schema> resolvedModels = new HashMap<>();
    private Map<String, Example> examples;
    private Map<String, Parameter> parameters;
    private Map<String, RequestBody> requestBodies;
    private Map<String, Header> headers;
    private Map<String, Link> links;
    private Map<String, Schema> resolvedProperties = new IdentityHashMap<>();

    public void resolveFully(OpenAPI openAPI) {
        Components components = openAPI.getComponents();
        if (components != null && components.getRequestBodies() != null) {
            requestBodies = components.getRequestBodies();
            if (requestBodies == null) {
                requestBodies = new HashMap<>();
            }
        }

        if (components != null && components.getSchemas() != null) {
            schemas = components.getSchemas();
            if (schemas == null) {
                schemas = new HashMap<>();
            }
        }

        if (components != null && components.getExamples() != null) {
            examples = components.getExamples();
            if (examples == null) {
                examples = new HashMap<>();
            }
        }

        if (components != null && components.getHeaders() != null) {
            headers = components.getHeaders();
            if (headers == null) {
                headers = new HashMap<>();
            }
        }  

        if (components != null && components.getParameters() != null) {
            parameters = components.getParameters();
            if (parameters == null) {
                parameters = new HashMap<>();
            }
        }
        if (components != null && components.getLinks() != null) {
            links = components.getLinks();
            if (links == null) {
                links = new HashMap<>();
            }
        }
        Paths paths = openAPI.getPaths();
        if(paths != null) {
            for (String pathname : paths.keySet()) {
                PathItem pathItem = paths.get(pathname);
                resolvePath(pathItem);
            }
        }
    }

    public void resolvePath(PathItem pathItem){
        for(Operation op : pathItem.readOperations()) {
            // inputs
            if (op.getParameters() != null) {
                for (Parameter parameter : op.getParameters()) {
                    parameter = parameter.get$ref() != null ? resolveParameter(parameter) : parameter;
                    if (parameter.getSchema() != null) {
                        Schema resolved = resolveSchema(parameter.getSchema());
                        if (resolved != null) {
                            parameter.setSchema(resolved);
                        }
                    }
                    if(parameter.getContent() != null){
                        Map<String,MediaType> content = parameter.getContent();
                        for (String key: content.keySet()){
                            if (content.get(key) != null && content.get(key).getSchema() != null ){
                                Schema resolvedSchema = resolveSchema(content.get(key).getSchema());
                                if (resolvedSchema != null) {
                                    content.get(key).setSchema(resolvedSchema);
                                }
                            }
                        }
                    }
                }
            }

            if (op.getCallbacks() != null){
                Map<String,Callback> callbacks = op.getCallbacks();
                for (String name : callbacks.keySet()) {
                    Callback callback = callbacks.get(name);
                    if (callback != null) {
                        for(String callbackName : callback.keySet()) {
                            PathItem path = callback.get(callbackName);
                            if(path != null){
                                resolvePath(path);
                            }

                        }
                    }
                }
            }

            RequestBody refRequestBody = op.getRequestBody();
            if (refRequestBody != null){
                RequestBody requestBody = refRequestBody.get$ref() != null ? resolveRequestBody(refRequestBody) : refRequestBody;
                op.setRequestBody(requestBody);
                if (requestBody.getContent() != null) {
                    Map<String, MediaType> content = requestBody.getContent();
                    for (String key : content.keySet()) {
                        if (content.get(key) != null && content.get(key).getSchema() != null) {
                            Schema resolved = resolveSchema(content.get(key).getSchema());
                            if (resolved != null) {
                                content.get(key).setSchema(resolved);
                            }
                        }
                    }
                }
            }
            // responses
            ApiResponses responses = op.getResponses();
            if(responses != null) {
                for(String code : responses.keySet()) {
                    ApiResponse response = responses.get(code);
                    if (response.getContent() != null) {
                        Map<String, MediaType> content = response.getContent();
                        for(String mediaType: content.keySet()){
                            if(content.get(mediaType).getSchema() != null) {
                                Schema resolved = resolveSchema(content.get(mediaType).getSchema());
                                response.getContent().get(mediaType).setSchema(resolved);
                            }
                            if(content.get(mediaType).getExamples() != null) {
                                Map<String,Example> resolved = resolveExample(content.get(mediaType).getExamples());
                                response.getContent().get(mediaType).setExamples(resolved);

                            }
                        }
                    }

                    resolveHeaders(response.getHeaders());

                    Map<String, Link> links = response.getLinks();
                    if (links != null) {
                        for (Map.Entry<String, Link> link : links.entrySet()) {
                            Link value = link.getValue();
                            Link resolvedValue = value.get$ref() != null ? resolveLink(value) : value;
                            link.setValue(resolvedValue);
                        }
                    }
                }
            }
        }
    }

    private void resolveHeaders(Map<String, Header> headers) {
        if (headers == null || headers.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Header> header : headers.entrySet()) {
            Header value = header.getValue();
            Header resolvedValue = value.get$ref() != null ? resolveHeader(value) : value;
            Map<String, Example> examples = resolvedValue.getExamples();
            if(examples != null) {
                Map<String,Example> resolved = resolveExample(examples);
                resolvedValue.setExamples(resolved);
            }
            header.setValue(resolvedValue);
        }
    }

    public Header resolveHeader(Header header){
        RefFormat refFormat = computeRefFormat(header.get$ref());
        String $ref = header.get$ref();
        if (!isAnExternalRefFormat(refFormat)){
            if (headers != null && !headers.isEmpty()) {
                String referenceKey = computeDefinitionName($ref);
                return headers.getOrDefault(referenceKey, header);
            }
        }
        return header;
    }
  
    public Link resolveLink(Link link){
        RefFormat refFormat = computeRefFormat(link.get$ref());
        String $ref = link.get$ref();
        if (!isAnExternalRefFormat(refFormat)){
            if (links != null && !links.isEmpty()) {
                String referenceKey = computeDefinitionName($ref);
                Link link1 = links.getOrDefault(referenceKey, link);
                if (link1 == null) {
                    return null;
                }
                resolveHeaders(link1.getHeaders());
                return link1;
            }
        }
        return link;
    }

    public RequestBody resolveRequestBody(RequestBody requestBody){
        RefFormat refFormat = computeRefFormat(requestBody.get$ref());
        String $ref = requestBody.get$ref();
        if (!isAnExternalRefFormat(refFormat)){
            if (requestBodies != null && !requestBodies.isEmpty()) {
                String referenceKey = computeDefinitionName($ref);
                return requestBodies.getOrDefault(referenceKey, requestBody);
            }
        }
        return requestBody;
    }

    public Parameter resolveParameter(Parameter parameter){
        String $ref = parameter.get$ref();
        RefFormat refFormat = computeRefFormat($ref);
        if (!isAnExternalRefFormat(refFormat)){
            if (parameters != null && !parameters.isEmpty()) {
                String referenceKey = computeDefinitionName($ref);
                return parameters.getOrDefault(referenceKey, parameter);
            }
        }
        return parameter;
    }

    public Schema resolveSchema(Schema schema) {
        if(schema.get$ref() != null) {
            String ref= schema.get$ref();
            ref = ref.substring(ref.lastIndexOf("/") + 1);
            Schema resolved = schemas.get(ref);

            if (resolved != null) {

                if (this.resolvedModels.containsKey(ref)) {
                    LOGGER.debug("avoiding infinite loop");
                    return resolvedModels.get(ref);
                }
                resolvedModels.put(ref, schema);
                Schema model = resolveSchema(resolved);

                // if we make it without a resolution loop, we can update the reference
                resolvedModels.put(ref, model);

                return model;

            }else {
                return schema;
            }
        }

        if(schema instanceof ArraySchema) {
            ArraySchema arrayModel = (ArraySchema) schema;
            if(arrayModel.getItems().get$ref() != null) {
                arrayModel.setItems(resolveSchema(arrayModel.getItems()));
            } else {
                arrayModel.setItems(arrayModel.getItems());
            }

            return arrayModel;
        }

        if (schema instanceof MapSchema) {
            MapSchema mapSchema = (MapSchema) schema;
            if (mapSchema.getAdditionalProperties() instanceof Schema) {
                Schema additionalPropertiesSchema = (Schema) mapSchema.getAdditionalProperties();
                mapSchema.setAdditionalProperties(resolveSchema(additionalPropertiesSchema));
            }
        }

        if (schema instanceof ObjectSchema) {
            ObjectSchema obj = (ObjectSchema) schema;
            if(obj.getProperties() != null) {
                Map<String, Schema> updated = new LinkedHashMap<>();
                for(String propertyName : obj.getProperties().keySet()) {
                    Schema innerProperty = obj.getProperties().get(propertyName);
                    // reference check
                    if(schema != innerProperty) {
                        if(resolvedProperties.get(propertyName) == null || resolvedProperties.get(propertyName) != innerProperty) {
                            LOGGER.debug("avoiding infinite loop");
                            Schema resolved = resolveSchema(innerProperty);
                            updated.put(propertyName, resolved);
                            resolvedProperties.put(propertyName, resolved);
                        }else {
                            updated.put(propertyName, resolvedProperties.get(propertyName));
                        }
                    }
                }
                obj.setProperties(updated);
            }
            return obj;
        }


        if(schema instanceof ComposedSchema) {
            ComposedSchema composedSchema = (ComposedSchema) schema;
            boolean adjacent = false;
            if (aggregateCombinators) {
                Schema model = SchemaTypeUtil.createSchema(composedSchema.getType(), composedSchema.getFormat());
                Set<String> requiredProperties = new HashSet<>();
                Set<Object> examples = new HashSet<>();

                if ((composedSchema.getAllOf() != null && composedSchema.getAnyOf() != null && composedSchema.getOneOf() != null)||
                        (composedSchema.getAllOf() != null && composedSchema.getAnyOf() != null) ||
                        (composedSchema.getAllOf() != null && composedSchema.getOneOf() != null)||
                        (composedSchema.getOneOf() != null && composedSchema.getAnyOf() != null)){

                    adjacent = true;

                }

                if (composedSchema.getAllOf() != null) {
                    for (Schema innerModel : composedSchema.getAllOf()) {
                        Schema resolved = resolveSchema(innerModel);
                        Map<String, Schema> properties = resolved.getProperties();
                        if (resolved.getProperties() != null) {
                            for (String key : properties.keySet()) {
                                Schema prop = (Schema) resolved.getProperties().get(key);
                                if(resolvedProperties.get(key) == null || resolvedProperties.get(key) != prop) {
                                    LOGGER.debug("avoiding infinite loop");
                                    Schema resolvedProp = resolveSchema(prop);
                                    model.addProperties(key,resolvedProp );
                                    resolvedProperties.put(key, resolvedProp);
                                }else {
                                    model.addProperties(key,resolvedProperties.get(key));
                                }
                            }
                            if (resolved.getRequired() != null) {
                                for (int i = 0; i < resolved.getRequired().size(); i++) {
                                    if (resolved.getRequired().get(i) != null) {
                                        requiredProperties.add(resolved.getRequired().get(i).toString());
                                    }
                                }
                            }
                        }
                        if (requiredProperties.size() > 0) {
                            model.setRequired(new ArrayList<>(requiredProperties));
                        }
                        if (resolved.getExample() != null) {
                            examples.add(resolved.getExample());
                        }
                        if (composedSchema.getExtensions() != null) {
                            Map<String, Object> extensions = composedSchema.getExtensions();
                            for (String key : extensions.keySet()) {
                                model.addExtension(key, composedSchema.getExtensions().get(key));
                            }
                        }
                    }

                } if (composedSchema.getOneOf() != null) {
                    if(adjacent == false) {
                        Schema resolved;
                        List<Schema> list = new ArrayList<>();
                        for (Schema innerModel : composedSchema.getOneOf()) {
                            resolved = resolveSchema(innerModel);
                            list.add(resolved);
                        }
                        composedSchema.setOneOf(list);
                        return composedSchema;
                    }else {
                        for (Schema innerModel : composedSchema.getOneOf()) {
                            Schema resolved = resolveSchema(innerModel);
                            Map<String, Schema> properties = resolved.getProperties();
                            if (resolved.getProperties() != null) {
                                for (String key : properties.keySet()) {
                                    Schema prop = (Schema) resolved.getProperties().get(key);
                                    if(resolvedProperties.get(key) == null || resolvedProperties.get(key) != prop) {
                                        LOGGER.debug("avoiding infinite loop");
                                        Schema resolvedProp = resolveSchema(prop);
                                        model.addProperties(key,resolvedProp );
                                        resolvedProperties.put(key, resolvedProp);
                                    }else {
                                        model.addProperties(key,resolvedProperties.get(key));
                                    }
                                }
                                if (resolved.getRequired() != null) {
                                    for (int i = 0; i < resolved.getRequired().size(); i++) {
                                        if (resolved.getRequired().get(i) != null) {
                                            requiredProperties.add(resolved.getRequired().get(i).toString());
                                        }
                                    }
                                }
                            }
                            if (requiredProperties.size() > 0) {
                                model.setRequired(new ArrayList<>(requiredProperties));
                            }
                            if (resolved.getExample() != null) {
                                examples.add(resolved.getExample());
                            }
                            if (composedSchema.getExtensions() != null) {
                                Map<String, Object> extensions = composedSchema.getExtensions();
                                for (String key : extensions.keySet()) {
                                    model.addExtension(key, composedSchema.getExtensions().get(key));
                                }
                            }
                        }
                    }

                } if (composedSchema.getAnyOf() != null) {
                    if(adjacent == false) {
                        Schema resolved;
                        List<Schema> list = new ArrayList<>();
                        for (Schema innerModel : composedSchema.getAnyOf()) {
                            resolved = resolveSchema(innerModel);
                            list.add(resolved);
                        }
                        composedSchema.setAnyOf(list);
                        return composedSchema;
                    }else {
                        for (Schema innerModel : composedSchema.getAnyOf()) {
                            Schema resolved = resolveSchema(innerModel);
                            Map<String, Schema> properties = resolved.getProperties();
                            if (resolved.getProperties() != null) {
                                for (String key : properties.keySet()) {
                                    Schema prop = (Schema) resolved.getProperties().get(key);
                                    if(resolvedProperties.get(key) == null || resolvedProperties.get(key) != prop) {
                                        LOGGER.debug("avoiding infinite loop");
                                        Schema resolvedProp = resolveSchema(prop);
                                        model.addProperties(key,resolvedProp );
                                        resolvedProperties.put(key, resolvedProp);
                                    }else {
                                        model.addProperties(key,resolvedProperties.get(key));
                                    }
                                }
                                if (resolved.getRequired() != null) {
                                    for (int i = 0; i < resolved.getRequired().size(); i++) {
                                        if (resolved.getRequired().get(i) != null) {
                                            requiredProperties.add(resolved.getRequired().get(i).toString());
                                        }
                                    }
                                }
                            }
                            if (requiredProperties.size() > 0) {
                                model.setRequired(new ArrayList<>(requiredProperties));
                            }
                            if (resolved.getExample() != null) {
                                examples.add(resolved.getExample());
                            }
                            if (composedSchema.getExtensions() != null) {
                                Map<String, Object> extensions = composedSchema.getExtensions();
                                for (String key : extensions.keySet()) {
                                    model.addExtension(key, composedSchema.getExtensions().get(key));
                                }
                            }
                        }
                    }
                }
                if (schema.getExample() != null) {
                    model.setExample(schema.getExample());
                } else if (!examples.isEmpty()) {
                    model.setExample(examples);
                }
                return model;
            } else {
                // User don't want to aggregate composed schema, we only solve refs
                if (composedSchema.getAllOf() != null)
                    composedSchema.allOf(composedSchema.getAllOf().stream().map(this::resolveSchema).collect(Collectors.toList()));
                if (composedSchema.getOneOf() != null)
                    composedSchema.oneOf(composedSchema.getOneOf().stream().map(this::resolveSchema).collect(Collectors.toList()));
                if (composedSchema.getAnyOf() != null)
                    composedSchema.anyOf(composedSchema.getAnyOf().stream().map(this::resolveSchema).collect(Collectors.toList()));
                return composedSchema;
            }
        }

        if (schema.getProperties() != null) {
            Schema model = schema;
            Map<String, Schema> updated = new LinkedHashMap<>();
            Map<String, Schema> properties = model.getProperties();
            for (String propertyName : properties.keySet()) {
                Schema property = (Schema) model.getProperties().get(propertyName);
                if(resolvedProperties.get(propertyName) == null || resolvedProperties.get(propertyName) != property) {
                    LOGGER.debug("avoiding infinite loop");
                    Schema resolved = resolveSchema(property);
                    updated.put(propertyName, resolved);
                    resolvedProperties.put(propertyName, resolved);
                }else {
                    updated.put(propertyName, resolvedProperties.get(propertyName));
                }
            }

            for (String key : updated.keySet()) {
                Schema property = updated.get(key);

                if(property instanceof ObjectSchema) {
                    ObjectSchema op = (ObjectSchema) property;
                    if (op.getProperties() != model.getProperties()) {
                        if (property.getType() == null) {
                            property.setType("object");
                        }
                        model.addProperties(key, property);
                    } else {
                        LOGGER.debug("not adding recursive properties, using generic object");
                        ObjectSchema newSchema = new ObjectSchema();
                        model.addProperties(key, newSchema);
                    }
                }

            }
            return model;
        }

        return schema;
    }

    public Map<String,Example> resolveExample(Map<String,Example> examples){

        Map<String,Example> resolveExamples = examples;

        if (examples != null) {

            for (String name : examples.keySet()) {
                if (examples.get(name).get$ref() != null) {
                    String ref = examples.get(name).get$ref();
                    ref = ref.substring(ref.lastIndexOf("/") + 1);
                    Example sample = this.examples.get(ref);
                    resolveExamples.replace(name, sample);
                }
            }
        }

        return resolveExamples;

    }
}
