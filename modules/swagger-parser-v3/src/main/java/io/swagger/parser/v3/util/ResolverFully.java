package io.swagger.parser.v3.util;

import io.swagger.oas.models.OpenAPI;
import io.swagger.oas.models.Operation;
import io.swagger.oas.models.PathItem;
import io.swagger.oas.models.callbacks.Callback;
import io.swagger.oas.models.media.ArraySchema;
import io.swagger.oas.models.media.ComposedSchema;
import io.swagger.oas.models.media.MediaType;
import io.swagger.oas.models.media.ObjectSchema;
import io.swagger.oas.models.media.Schema;
import io.swagger.oas.models.parameters.Parameter;
import io.swagger.oas.models.responses.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class ResolverFully {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResolverFully.class);

    private Map<String, Schema> schemas;
    private Map<String, Schema> resolvedModels = new HashMap<>();




    public void resolveFully(OpenAPI openAPI) {
        if (openAPI.getComponents().getSchemas() != null) {
            schemas = openAPI.getComponents().getSchemas();
            if (schemas == null) {
                schemas = new HashMap<>();
            }
        }

        if(openAPI.getPaths() != null) {
            for (String pathname : openAPI.getPaths().keySet()) {
                PathItem pathItem = openAPI.getPaths().get(pathname);
                resolvePath(pathItem);
            }
        }
    }

    public void resolvePath(PathItem pathItem){
        for(Operation op : pathItem.readOperations()) {
            // inputs
            if (op.getParameters() != null) {
                for (Parameter parameter : op.getParameters()) {
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

            if (op.getRequestBody() != null && op.getRequestBody().getContent() != null){
                Map<String,MediaType> content = op.getRequestBody().getContent();
                for (String key: content.keySet()){
                    if (content.get(key) != null && content.get(key).getSchema() != null ){
                        Schema resolved = resolveSchema(content.get(key).getSchema());
                        if (resolved != null) {
                            content.get(key).setSchema(resolved);
                        }
                    }
                }
            }
            // responses
            if(op.getResponses() != null) {
                for(String code : op.getResponses().keySet()) {
                    ApiResponse response = op.getResponses().get(code);
                    if (response.getContent() != null) {
                        Map<String, MediaType> content = response.getContent();
                        for(String mediaType: content.keySet()){
                            if(content.get(mediaType).getSchema() != null) {
                                Schema resolved = resolveSchema(content.get(mediaType).getSchema());
                                response.getContent().get(mediaType).setSchema(resolved);
                            }
                        }
                    }
                }
            }
        }
    }


    public Schema resolveSchema(Schema schema) {
        if(schema.get$ref() != null) {
            String ref= schema.get$ref();
            ref = ref.substring(ref.lastIndexOf("/") + 1);
            Schema resolved = schemas.get(ref);
            if(resolved == null) {
                LOGGER.error("unresolved model " + ref);
                return schema;
            }
            if(this.resolvedModels.containsKey(ref)) {
                LOGGER.debug("avoiding infinite loop");
                return this.resolvedModels.get(ref);
            }
            this.resolvedModels.put(ref, schema);

            Schema model = resolveSchema(resolved);

            // if we make it without a resolution loop, we can update the reference
            this.resolvedModels.put(ref, model);
            return model;
        }

        if(schema instanceof ArraySchema) {
            ArraySchema arrayModel = (ArraySchema) schema;
            if(arrayModel.getItems().get$ref() != null) {
                arrayModel.setItems(resolveSchema(arrayModel.getItems()));
            }
            return arrayModel;
        }
        if (schema instanceof ObjectSchema) {
            ObjectSchema obj = (ObjectSchema) schema;
            if(obj.getProperties() != null) {
                Map<String, Schema> updated = new LinkedHashMap<>();
                for(String propertyName : obj.getProperties().keySet()) {
                    Schema innerProperty = obj.getProperties().get(propertyName);
                    // reference check
                    if(schema != innerProperty) {
                        Schema resolved = resolveSchema(innerProperty);
                        updated.put(propertyName, resolved);
                    }
                }
                obj.setProperties(updated);
            }
            return obj;
        }


        if(schema instanceof ComposedSchema) {
            ComposedSchema composedSchema = (ComposedSchema) schema;
            Schema model = SchemaTypeUtil.createSchema(composedSchema.getType(),composedSchema.getFormat());
            Set<String> requiredProperties = new HashSet<>();
            if(composedSchema.getAllOf() != null){
                for(Schema innerModel : composedSchema.getAllOf()) {
                    Schema resolved = resolveSchema(innerModel);
                    Map<String, Schema> properties = resolved.getProperties();
                    if (resolved.getProperties() != null) {

                        for (String key : properties.keySet()) {
                            Schema prop = (Schema) resolved.getProperties().get(key);
                            model.addProperties(key, resolveSchema(prop));
                        }
                        if (resolved.getRequired() != null) {
                            for(int i =0;i<resolved.getRequired().size();i++){
                                if (resolved.getRequired().get(i) != null) {
                                    requiredProperties.add(resolved.getRequired().get(i).toString());
                                }
                            }
                        }
                    }
                }
            }else if(composedSchema.getOneOf() != null){
                for(Schema innerModel : composedSchema.getOneOf()) {
                    Schema resolved = resolveSchema(innerModel);
                    Map<String, Schema> properties = resolved.getProperties();
                    if (resolved.getProperties() != null) {

                        for (String key : properties.keySet()) {
                            Schema prop = (Schema) resolved.getProperties().get(key);
                            model.addProperties(key, resolveSchema(prop));
                        }
                        if (resolved.getRequired() != null) {
                            for(int i =0;i<resolved.getRequired().size();i++){
                                if (resolved.getRequired().get(i) != null) {
                                    requiredProperties.add(resolved.getRequired().get(i).toString());
                                }
                            }
                        }
                    }
                }

            }else if(composedSchema.getAnyOf() != null){
                for(Schema innerModel : composedSchema.getAnyOf()) {
                    Schema resolved = resolveSchema(innerModel);
                    Map<String, Schema> properties = resolved.getProperties();
                    if (resolved.getProperties() != null) {

                        for (String key : properties.keySet()) {
                            Schema prop = (Schema) resolved.getProperties().get(key);
                            model.addProperties(key, resolveSchema(prop));
                        }
                        if (resolved.getRequired() != null) {
                            for(int i =0;i<resolved.getRequired().size();i++){
                                if (resolved.getRequired().get(i) != null) {
                                    requiredProperties.add(resolved.getRequired().get(i).toString());
                                }
                            }
                        }
                    }

                }
            }
            if(requiredProperties.size() > 0) {
                model.setRequired(new ArrayList<>(requiredProperties));
            }
            if(composedSchema.getExtensions() != null) {
                Map<String, Object> extensions = composedSchema.getExtensions();
                for(String key : extensions.keySet()) {
                    model.addExtension(key, composedSchema.getExtensions().get(key));
                }
            }
            return model;
        }

        if (schema.getProperties() != null) {
            Schema model = schema;
            Map<String, Schema> updated = new LinkedHashMap<>();
            Map<String, Schema> properties = model.getProperties();
            for (String propertyName : properties.keySet()) {
                Schema property = (Schema) model.getProperties().get(propertyName);
                Schema resolved = resolveSchema(property);
                updated.put(propertyName, resolved);
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


        //LOGGER.error("no type match for " + schema);
        return schema;
    }
}
