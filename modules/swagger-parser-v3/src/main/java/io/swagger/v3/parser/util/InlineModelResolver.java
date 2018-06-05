package io.swagger.v3.parser.util;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.XML;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.core.util.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InlineModelResolver {
    private OpenAPI openAPI;
    private boolean skipMatches;
    static Logger LOGGER = LoggerFactory.getLogger(InlineModelResolver.class);

    Map<String, Schema> addedModels = new HashMap<>();
    Map<String, String> generatedSignature = new HashMap<>();

    public void flatten(OpenAPI openAPI) {
        this.openAPI = openAPI;

        if (openAPI.getComponents() != null) {

            if (openAPI.getComponents().getSchemas() == null) {
                openAPI.getComponents().setSchemas(new HashMap<>());
            }
        }

        // operations
        Map<String, PathItem> paths = openAPI.getPaths();
        if(openAPI.getComponents()== null){
            openAPI.setComponents(new Components());
        }
        Map<String, Schema> models = openAPI.getComponents().getSchemas();

        if (paths != null) {
            for (String pathname : paths.keySet()) {
                PathItem path = paths.get(pathname);

                for (Operation operation : path.readOperations()) {
                    RequestBody body = operation.getRequestBody();

                    if (body != null) {
                        if (body.getContent() != null) {
                            Map<String,MediaType> content = body.getContent();
                            for(String key: content.keySet()) {
                                if (content.get(key) != null) {
                                    MediaType mediaType = content.get(key);
                                    if(mediaType.getSchema() != null) {
                                        Schema model = mediaType.getSchema();
                                        if (model.getProperties() != null && model.getProperties().size() > 0) {
                                            flattenProperties(model.getProperties(), pathname);
                                            String modelName = resolveModelName(model.getTitle(), "body");
                                            mediaType.setSchema(new Schema().$ref(modelName));
                                            addGenerated(modelName, model);
                                            openAPI.getComponents().addSchemas(modelName, model);

                                        } else if (model instanceof ArraySchema) {
                                            ArraySchema am = (ArraySchema) model;
                                            Schema inner = am.getItems();

                                            if (inner instanceof ObjectSchema) {
                                                ObjectSchema op = (ObjectSchema) inner;
                                                if (op.getProperties() != null && op.getProperties().size() > 0) {
                                                    flattenProperties(op.getProperties(), pathname);
                                                    String modelName = resolveModelName(op.getTitle(), "body");
                                                    Schema innerModel = modelFromProperty(op, modelName);
                                                    String existing = matchGenerated(innerModel);
                                                    if (existing != null) {
                                                        am.setItems(new Schema().$ref(existing));
                                                    } else {
                                                        am.setItems(new Schema().$ref(modelName));
                                                        addGenerated(modelName, innerModel);
                                                        openAPI.getComponents().addSchemas(modelName, innerModel);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    List<Parameter> parameters = operation.getParameters();
                    if (parameters != null){
                        for (Parameter parameter : parameters) {
                            if (parameter.getSchema() != null) {
                                Schema model = parameter.getSchema();
                                if (model.getProperties() != null) {
                                    if (model.getType() == null || "object".equals(model.getType())) {
                                        if (model.getProperties() != null && model.getProperties().size() > 0) {
                                            flattenProperties(model.getProperties(), pathname);
                                            String modelName = resolveModelName(model.getTitle(), parameter.getName());
                                            parameter.setSchema(new Schema().$ref(modelName));
                                            addGenerated(modelName, model);
                                            openAPI.getComponents().addSchemas(modelName, model);
                                        }
                                    }
                                } else if (model instanceof ArraySchema) {
                                    ArraySchema am = (ArraySchema) model;
                                    Schema inner = am.getItems();

                                    if (inner instanceof ObjectSchema) {
                                        ObjectSchema op = (ObjectSchema) inner;
                                        if (op.getProperties() != null && op.getProperties().size() > 0) {
                                            flattenProperties(op.getProperties(), pathname);
                                            String modelName = resolveModelName(op.getTitle(), parameter.getName());
                                            Schema innerModel = modelFromProperty(op, modelName);
                                            String existing = matchGenerated(innerModel);
                                            if (existing != null) {
                                                am.setItems(new Schema().$ref(existing));
                                            } else {
                                                am.setItems(new Schema().$ref(modelName));
                                                addGenerated(modelName, innerModel);
                                                openAPI.getComponents().addSchemas(modelName, innerModel);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Map<String, ApiResponse> responses = operation.getResponses();
                    if (responses != null) {
                        for (String key : responses.keySet()) {
                            ApiResponse response = responses.get(key);
                            if (response.getContent() != null) {
                                Map<String,MediaType> content = response.getContent();
                                for (String name: content.keySet()) {
                                    if (content.get(name) != null) {
                                        MediaType media = content.get(name);
                                        if (media.getSchema() != null) {
                                            Schema property = media.getSchema();
                                            if (property instanceof ObjectSchema) {
                                                ObjectSchema op = (ObjectSchema) property;
                                                if (op.getProperties() != null && op.getProperties().size() > 0) {
                                                    String modelName = resolveModelName(op.getTitle(), "inline_response_" + key);
                                                    Schema model = modelFromProperty(op, modelName);
                                                    String existing = matchGenerated(model);
                                                    if (existing != null) {
                                                        media.setSchema(this.makeRefProperty(existing, property));
                                                    } else {
                                                        media.setSchema(this.makeRefProperty(modelName, property));
                                                        addGenerated(modelName, model);
                                                        openAPI.getComponents().addSchemas(modelName, model);
                                                    }
                                                }
                                            } else if (property instanceof ArraySchema) {
                                                ArraySchema ap = (ArraySchema) property;
                                                Schema inner = ap.getItems();

                                                if (inner instanceof ObjectSchema) {
                                                    ObjectSchema op = (ObjectSchema) inner;
                                                    if (op.getProperties() != null && op.getProperties().size() > 0) {
                                                        flattenProperties(op.getProperties(), pathname);
                                                        String modelName = resolveModelName(op.getTitle(),
                                                                "inline_response_" + key);
                                                        Schema innerModel = modelFromProperty(op, modelName);
                                                        String existing = matchGenerated(innerModel);
                                                        if (existing != null) {
                                                            ap.setItems(this.makeRefProperty(existing, op));
                                                        } else {
                                                            ap.setItems(this.makeRefProperty(modelName, op));
                                                            addGenerated(modelName, innerModel);
                                                            openAPI.getComponents().addSchemas(modelName, innerModel);
                                                        }
                                                    }
                                                }
                                            } else if (property.getAdditionalProperties() != null && property.getAdditionalProperties() instanceof Schema) {

                                                Schema innerProperty = (Schema) property.getAdditionalProperties();
                                                if (innerProperty instanceof ObjectSchema) {
                                                    ObjectSchema op = (ObjectSchema) innerProperty;
                                                    if (op.getProperties() != null && op.getProperties().size() > 0) {
                                                        flattenProperties(op.getProperties(), pathname);
                                                        String modelName = resolveModelName(op.getTitle(),
                                                                "inline_response_" + key);
                                                        Schema innerModel = modelFromProperty(op, modelName);
                                                        String existing = matchGenerated(innerModel);
                                                        if (existing != null) {
                                                            property.setAdditionalProperties(new Schema().$ref(existing));
                                                        } else {
                                                            property.setAdditionalProperties(new Schema().$ref(modelName));
                                                            addGenerated(modelName, innerModel);
                                                            openAPI.getComponents().addSchemas(modelName, innerModel);
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // definitions
        if (models != null) {
            List<String> modelNames = new ArrayList<String>(models.keySet());
            for (String modelName : modelNames) {
                Schema model = models.get(modelName);
                if (model.getProperties() != null) {
                    Map<String, Schema> properties = model.getProperties();
                    flattenProperties(properties, modelName);
                    fixStringModel(model);
                } else if (model instanceof ArraySchema) {
                    ArraySchema m = (ArraySchema) model;
                    Schema inner = m.getItems();
                    if (inner instanceof ObjectSchema) {
                        ObjectSchema op = (ObjectSchema) inner;
                        if (op.getProperties() != null && op.getProperties().size() > 0) {
                            String innerModelName = resolveModelName(op.getTitle(), modelName + "_inner");
                            Schema innerModel = modelFromProperty(op, innerModelName);
                            String existing = matchGenerated(innerModel);
                            if (existing == null) {
                                openAPI.getComponents().addSchemas(innerModelName, innerModel);
                                addGenerated(innerModelName, innerModel);
                                m.setItems(new Schema().$ref(innerModelName));
                            } else {
                                m.setItems(new Schema().$ref(existing));
                            }
                        }
                    }
                } else if (model instanceof ComposedSchema) {
                    ComposedSchema composedSchema = (ComposedSchema) model;
                    List<Schema> list = null;
                    if (composedSchema.getAllOf() != null) {
                      list  = composedSchema.getAllOf();
                    }else if (composedSchema.getAnyOf() != null) {
                        list  = composedSchema.getAnyOf();
                    }else if (composedSchema.getOneOf() != null) {
                        list  = composedSchema.getOneOf();
                    }
                    for(int i= 0; i<list.size();i++){
                        if(list.get(i).getProperties()!= null){
                            flattenProperties(list.get(i).getProperties(), modelName);
                        }
                    }
                }
            }
        }
    }

    /**
     * This function fix models that are string (mostly enum). Before this fix, the example
     * would look something like that in the doc: "\"example from def\""
     * @param m Model implementation
     */
    private void fixStringModel(Schema m) {
        if (m.getType() != null && m.getType().equals("string") && m.getExample() != null) {
            String example = m.getExample().toString();
            if (example.substring(0, 1).equals("\"") &&
                    example.substring(example.length() - 1).equals("\"")) {
                m.setExample(example.substring(1, example.length() - 1));
            }
        }
    }

    private String resolveModelName(String title, String key) {
        if (title == null) {
            return uniqueName(key);
        } else {
            return uniqueName(title);
        }
    }

    public String matchGenerated(Schema model) {
        if (this.skipMatches) {
            return null;
        }
        String json = Json.pretty(model);
        if (generatedSignature.containsKey(json)) {
            return generatedSignature.get(json);
        }
        return null;
    }

    public void addGenerated(String name, Schema model) {
        generatedSignature.put(Json.pretty(model), name);
    }

    public String uniqueName(String key) {
        int count = 0;
        boolean done = false;
        key = key.replaceAll("[^a-z_\\.A-Z0-9 ]", ""); // FIXME: a parameter
        // should not be
        // assigned. Also declare
        // the methods parameters
        // as 'final'.
        while (!done) {
            String name = key;
            if (count > 0) {
                name = key + "_" + count;
            }
            if (openAPI.getComponents().getSchemas() == null) {
                return name;
            } else if (!openAPI.getComponents().getSchemas().containsKey(name)) {
                return name;
            }
            count += 1;
        }
        return key;
    }

    public void flattenProperties(Map<String, Schema> properties, String path) {
        if (properties == null) {
            return;
        }
        Map<String, Schema> propsToUpdate = new HashMap<>();
        Map<String, Schema> modelsToAdd = new HashMap<>();
        for (String key : properties.keySet()) {
            Schema property = properties.get(key);
            if (property instanceof ObjectSchema && ((ObjectSchema) property).getProperties() != null
                    && ((ObjectSchema) property).getProperties().size() > 0) {

                ObjectSchema op = (ObjectSchema) property;

                String modelName = resolveModelName(op.getTitle(), path + "_" + key);
                Schema model = modelFromProperty(op, modelName);

                String existing = matchGenerated(model);

                if (existing != null) {
                    propsToUpdate.put(key, new Schema().$ref(existing));
                } else {
                    propsToUpdate.put(key, new Schema().$ref(modelName));
                    modelsToAdd.put(modelName, model);
                    addGenerated(modelName, model);
                    openAPI.getComponents().addSchemas(modelName, model);
                }
            } else if (property instanceof ArraySchema) {
                ArraySchema ap = (ArraySchema) property;
                Schema inner = ap.getItems();

                if (inner instanceof ObjectSchema) {
                    ObjectSchema op = (ObjectSchema) inner;
                    if (op.getProperties() != null && op.getProperties().size() > 0) {
                        flattenProperties(op.getProperties(), path);
                        String modelName = resolveModelName(op.getTitle(), path + "_" + key);
                        Schema innerModel = modelFromProperty(op, modelName);
                        String existing = matchGenerated(innerModel);
                        if (existing != null) {
                            ap.setItems(new Schema().$ref(existing));
                        } else {
                            ap.setItems(new Schema().$ref(modelName));
                            addGenerated(modelName, innerModel);
                            openAPI.getComponents().addSchemas(modelName, innerModel);
                        }
                    }
                }
            } else if (property.getAdditionalProperties() != null && property.getAdditionalProperties() instanceof Schema) {
                Schema inner = (Schema) property.getAdditionalProperties();

                if (inner instanceof ObjectSchema) {
                    ObjectSchema op = (ObjectSchema) inner;
                    if (op.getProperties() != null && op.getProperties().size() > 0) {
                        flattenProperties(op.getProperties(), path);
                        String modelName = resolveModelName(op.getTitle(), path + "_" + key);
                        Schema innerModel = modelFromProperty(op, modelName);
                        String existing = matchGenerated(innerModel);
                        if (existing != null) {
                            property.setAdditionalProperties(new Schema().$ref(existing));
                        } else {
                            property.setAdditionalProperties(new Schema().$ref(modelName));
                            addGenerated(modelName, innerModel);
                            openAPI.getComponents().addSchemas(modelName, innerModel);
                        }
                    }
                }
            }
        }
        if (propsToUpdate.size() > 0) {
            for (String key : propsToUpdate.keySet()) {
                properties.put(key, propsToUpdate.get(key));
            }
        }
        for (String key : modelsToAdd.keySet()) {
            openAPI.getComponents().addSchemas(key, modelsToAdd.get(key));
            this.addedModels.put(key, modelsToAdd.get(key));
        }
    }

    @SuppressWarnings("static-method")
    public Schema modelFromProperty(ArraySchema object, @SuppressWarnings("unused") String path) {
        String description = object.getDescription();
        String example = null;

        Object obj = object.getExample();
        if (obj != null) {
            example = obj.toString();
        }

        Schema inner = object.getItems();
        if (inner instanceof ObjectSchema) {
            ArraySchema model = new ArraySchema();
            model.setDescription(description);
            model.setExample(example);
            model.setItems(object.getItems());
            return model;
        }

        return null;
    }

    public Schema modelFromProperty(ObjectSchema object, String path) {
        String description = object.getDescription();
        String example = null;

        Object obj = object.getExample();
        if (obj != null) {
            example = obj.toString();
        }
        String name = object.getName();
        XML xml = object.getXml();
        Map<String, Schema> properties = object.getProperties();

        Schema model = new Schema();//TODO Verify this!
        model.setDescription(description);
        model.setExample(example);
        model.setName(name);
        model.setXml(xml);
        model.setType(object.getType());

        if (properties != null) {
            flattenProperties(properties, path);
            model.setProperties(properties);
        }

        return model;
    }

    @SuppressWarnings("static-method")
    public Schema modelFromProperty(Schema object, @SuppressWarnings("unused") String path) {
        String description = object.getDescription();
        String example = null;

        Object obj = object.getExample();
        if (obj != null) {
            example = obj.toString();
        }
        ArraySchema model = new ArraySchema();
        model.setDescription(description);
        model.setExample(example);
        if (object.getAdditionalProperties() != null && object.getAdditionalProperties() instanceof Schema) {
            model.setItems((Schema)  object.getAdditionalProperties());
        }


        return model;
    }

    /**
     * Make a RefProperty
     *
     * @param ref new property name
     * @param property Property
     * @return
     */
    public Schema makeRefProperty(String ref, Schema property) {
        Schema newProperty = new Schema().$ref(ref);

        this.copyVendorExtensions(property, newProperty);
        return newProperty;
    }

    /**
     * Copy vendor extensions from Property to another Property
     *
     * @param source source property
     * @param target target property
     */
    public void copyVendorExtensions(Schema source, Schema target) {
        if(source.getExtensions() != null) {
            Map<String, Object> vendorExtensions = source.getExtensions();
            for (String extName : vendorExtensions.keySet()) {
                target.addExtension(extName, vendorExtensions.get(extName));
            }
        }
    }

    public boolean isSkipMatches() {
        return skipMatches;
    }

    public void setSkipMatches(boolean skipMatches) {
        this.skipMatches = skipMatches;
    }

}