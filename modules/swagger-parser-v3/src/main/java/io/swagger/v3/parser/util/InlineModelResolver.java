package io.swagger.v3.parser.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.core.util.Json;
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
import io.swagger.v3.parser.models.RefType;

@SuppressWarnings("rawtypes")
public class InlineModelResolver {
    private OpenAPI openAPI;
    static final Logger LOGGER = LoggerFactory.getLogger(InlineModelResolver.class);

    Map<String, Schema> addedModels = new HashMap<>();
    Map<String, String> generatedSignature = new HashMap<>();

    private final boolean flattenComposedSchemas;
    private final boolean camelCaseFlattenNaming;
    private final boolean nameInlineModelBasedOnEndpointPath;
    private boolean skipMatches;

    public InlineModelResolver(){this(false, false, false, false);}

    public InlineModelResolver(boolean flattenComposedSchemas, boolean camelCaseFlattenNaming) {
        this(flattenComposedSchemas, camelCaseFlattenNaming, false, false);
    }

    public InlineModelResolver(boolean flattenComposedSchemas, boolean camelCaseFlattenNaming, boolean skipMatches, boolean nameInlineModelBasedOnEndpointPath) {
        this.flattenComposedSchemas = flattenComposedSchemas;
        this.camelCaseFlattenNaming = camelCaseFlattenNaming;
        this.nameInlineModelBasedOnEndpointPath = nameInlineModelBasedOnEndpointPath;
        this.skipMatches = skipMatches;
    }

    public void flatten(OpenAPI openAPI) {
        this.openAPI = openAPI;

        if (openAPI.getComponents() != null) {

            if (openAPI.getComponents().getSchemas() == null) {
                openAPI.getComponents().setSchemas(new HashMap<>());
            }
        }

        // operations
        Map<String, PathItem> paths = openAPI.getPaths();
        if (openAPI.getComponents()== null){
            openAPI.setComponents(new Components());
        }
        Map<String, Schema> models = openAPI.getComponents().getSchemas();
        flattenPaths(paths);
        flattenDefinitions(models);
    }

    private void flattenPaths(Map<String, PathItem> paths)
    {
      if (paths == null) {
        return;
      }
      for (String pathname : paths.keySet()) {
          PathItem path = paths.get(pathname);

          for (Operation operation : path.readOperations()) {
              flattenBody(pathname, operation.getRequestBody());
              flattenParams(pathname, operation.getParameters());
              flattenResponses(pathname, operation.getResponses(), operation.getOperationId());
          }
      }
    }

    private void flattenBody(String pathname, RequestBody body)
    {
      if (body == null) {
        return;
      }
      Map<String,MediaType> content = body.getContent();
      if (content == null)
      {
        return;
      }
      for(String key: content.keySet()) {
          if (content.get(key) != null) {
              MediaType mediaType = content.get(key);
              if (mediaType.getSchema() != null) {
                  Schema model = mediaType.getSchema();
                  String genericName = pathBody(pathname);
                  if (model.getProperties() != null && model.getProperties().size() > 0) {
                      flattenProperties(model.getProperties(), pathname);
                      String modelName = resolveModelName(model.getTitle(), genericName);
                      mediaType.setSchema(new Schema().$ref(modelName));
                      addGenerated(modelName, model);
                      openAPI.getComponents().addSchemas(modelName, model);
                  } else if (model instanceof ComposedSchema) {
                      flattenComposedSchema(model, pathname);
                      if (model.get$ref() == null) {
                          String modelName = resolveModelName(model.getTitle(), genericName);
                          mediaType.setSchema(this.makeRefProperty(modelName, model));
                          addGenerated(modelName, model);
                          openAPI.getComponents().addSchemas(modelName, model);
                      }
                  } else if (model instanceof ArraySchema) {
                      ArraySchema am = (ArraySchema) model;
                      Schema inner = am.getItems();
                      if (isObjectSchema(inner)) {
                          if (inner.getProperties() != null && inner.getProperties().size() > 0) {
                              flattenProperties(inner.getProperties(), pathname);
                              String modelName = resolveModelName(inner.getTitle(), genericName);
                              String existing = matchGenerated(inner);
                              if (existing != null) {
                                  am.setItems(new Schema().$ref(existing));
                              } else {
                                  am.setItems(new Schema().$ref(modelName));
                                  addGenerated(modelName, inner);
                                  openAPI.getComponents().addSchemas(modelName, inner);
                              }
                          }else if (inner instanceof ComposedSchema && this.flattenComposedSchemas){
                              flattenComposedSchema(inner,key);
                              if (inner.get$ref() == null) {
                                  String modelName = resolveModelName(inner.getTitle(), "inline_body_items_" + key + "_" + pathname);
                                  am.setItems(this.makeRefProperty(modelName, inner));
                                  addGenerated(modelName, inner);
                                  openAPI.getComponents().addSchemas(modelName, inner);
                              }
                          }
                      }
                  }
              }
          }
      }
    }

    private void flattenParams(String pathname, List<Parameter> parameters)
    {
      if (parameters == null){
        return;
      }
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
              }else if (model instanceof ComposedSchema) {
                  String modelName = resolveModelName(model.getTitle(), parameter.getName());
                  parameter.setSchema(new Schema().$ref(modelName));
                  addGenerated(modelName, model);
                  openAPI.getComponents().addSchemas(modelName, model);
              }else if (model instanceof ArraySchema) {
                  ArraySchema am = (ArraySchema) model;
                  Schema inner = am.getItems();
                  if (isObjectSchema(inner)) {
                      if (inner.getProperties() != null && inner.getProperties().size() > 0) {
                          flattenProperties(inner.getProperties(), pathname);
                          String modelName = resolveModelName(inner.getTitle(), parameter.getName());
                          String existing = matchGenerated(inner);
                          if (existing != null) {
                              am.setItems(new Schema().$ref(existing));
                          } else {
                              am.setItems(new Schema().$ref(modelName));
                              addGenerated(modelName, am);
                              openAPI.getComponents().addSchemas(modelName, am);
                          }
                      }else if (inner instanceof ComposedSchema && this.flattenComposedSchemas){
                          flattenComposedSchema(inner, parameter.getName());
                          if (inner.get$ref() == null) {
                              String modelName = resolveModelName(inner.getTitle(), "inline_parameter_items_" + parameter.getName());
                              am.setItems(this.makeRefProperty(modelName, inner));
                              addGenerated(modelName, inner);
                              openAPI.getComponents().addSchemas(modelName, inner);
                          }
                      }
                  }
              }
          }
      }
    }

    private void flattenResponses(String pathname, Map<String, ApiResponse> responses, String operationId)
    {
      if (responses == null) {
        return;
      }
      for (String key : responses.keySet()) {
          ApiResponse response = responses.get(key);
          if (response.getContent() != null) {
              Map<String,MediaType> content = response.getContent();
              for (String name: content.keySet()) {
                  if (content.get(name) != null) {
                      MediaType media = content.get(name);
                      if (media.getSchema() != null) {
                          Schema mediaSchema = media.getSchema();
                          if (isObjectSchema(mediaSchema)) {
                              if (mediaSchema.getProperties() != null && mediaSchema.getProperties().size() > 0 || mediaSchema instanceof ComposedSchema) {
                                  String title = mediaSchema.getTitle();
                                  if (title == null && this.nameInlineModelBasedOnEndpointPath == true ) {
                                    title = operationId.replaceAll("--", "-") + "ResponseBodyInline";
                                  }
                                  String modelName = resolveModelName(title, "inline_response_" + key);
                                  String existing = matchGenerated(mediaSchema);
                                  if (existing != null) {
                                      media.setSchema(this.makeRefProperty(existing, mediaSchema));
                                  } else {
                                      media.setSchema(this.makeRefProperty(modelName, mediaSchema));
                                      addGenerated(modelName, mediaSchema);
                                      openAPI.getComponents().addSchemas(modelName, mediaSchema);
                                  }
                              }else if (mediaSchema.getAdditionalProperties() != null && !(mediaSchema.getAdditionalProperties() instanceof Boolean)) {
                                  Schema innerProperty = (Schema) mediaSchema.getAdditionalProperties();
                                  if (isObjectSchema(innerProperty)) {
                                      key = "inline_response_map" + key;
                                      flattenMapSchema(innerProperty, key, pathname, mediaSchema);
                                  } else if (innerProperty instanceof ArraySchema) {
                                      ArraySchema arraySchema = (ArraySchema) innerProperty;
                                      Schema inner = arraySchema.getItems();
                                      if (isObjectSchema(inner)) {
                                          key = "inline_response_map_items" + key;
                                          flattenMapSchema(inner,key,pathname,mediaSchema);
                                      }
                                  }
                              }
                          }else if (mediaSchema instanceof ArraySchema) {
                              ArraySchema ap = (ArraySchema) mediaSchema;
                              Schema inner = ap.getItems();
                              if (isObjectSchema(inner)) {
                                  flattenArraySchema(inner, key, pathname, ap);
                              }
                          } else if (mediaSchema.getAdditionalProperties() != null && !(mediaSchema.getAdditionalProperties() instanceof Boolean)) {
                              Schema innerProperty = (Schema) mediaSchema.getAdditionalProperties();
                              if (isObjectSchema(innerProperty)) {
                                  key = "inline_response_map" + key;
                                  flattenMapSchema(innerProperty, key, pathname, mediaSchema);
                              }
                          }
                      }
                  }
              }
          }
      }
    }

    private void flattenDefinitions(Map<String, Schema> models)
    {
      if (models == null) {
        return;
      }
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
              if (isObjectSchema(inner)) {
                  if (inner.getProperties() != null && inner.getProperties().size() > 0) {
                      String innerModelName = resolveModelName(inner.getTitle(), modelName + "_inner");
                      String existing = matchGenerated(inner);
                      if (existing == null) {
                          openAPI.getComponents().addSchemas(innerModelName, inner);
                          addGenerated(innerModelName, inner);
                          m.setItems(new Schema().$ref(innerModelName));
                      } else {
                          m.setItems(new Schema().$ref(existing));
                      }
                  }else if (inner instanceof ComposedSchema && this.flattenComposedSchemas){
                      flattenComposedSchema(inner,modelName);
                      if (inner.get$ref() == null) {
                          modelName = resolveModelName(inner.getTitle(), "inline_array_items_" + modelName);
                          m.setItems(this.makeRefProperty(modelName, inner));
                          addGenerated(modelName, inner);
                          openAPI.getComponents().addSchemas(modelName, inner);
                      }
                  }
              }
          } else if (model instanceof ComposedSchema) {
              ComposedSchema composedSchema = (ComposedSchema) model;
              String inlineModelName = "";
              List<Schema> list = null;
              if (composedSchema.getAllOf() != null) {
                  list  = composedSchema.getAllOf();
                  inlineModelName = "AllOf";
              }else if (composedSchema.getAnyOf() != null) {
                  list  = composedSchema.getAnyOf();
                  inlineModelName = "AnyOf";
              }else if (composedSchema.getOneOf() != null) {
                  list  = composedSchema.getOneOf();
                  inlineModelName = "OneOf";
              }

              for(int i= 0; i<list.size();i++){
                  if (list.get(i).get$ref() == null){
                      Schema inline = list.get(i);
                      if (inline.getProperties()!= null){
                          flattenProperties(inline.getProperties(), modelName);
                      }
                      if (this.flattenComposedSchemas) {
                          int position = i+1;
                          inlineModelName = resolveModelName(inline.getTitle(),  modelName + inlineModelName + "_" + position);
                          list.set(i,new Schema().$ref(inlineModelName));
                          addGenerated(inlineModelName, inline);
                          openAPI.getComponents().addSchemas(inlineModelName, inline);
                      }
                  }
              }
          }
      }
    }

    private void flattenArraySchema(Schema inner, String key, String pathname, ArraySchema ap) {
        if (inner.getProperties() != null && inner.getProperties().size() > 0) {
            flattenProperties(inner.getProperties(), pathname);
            key = "inline_response_" + key;
            String modelName = resolveModelName(inner.getTitle(), key);
            String existing = matchGenerated(inner);
            if (existing != null) {
                ap.setItems(this.makeRefProperty(existing, inner));
            } else {
                ap.setItems(this.makeRefProperty(modelName, inner));
                addGenerated(modelName, inner);
                openAPI.getComponents().addSchemas(modelName, inner);
            }
        }else if (inner instanceof ComposedSchema && this.flattenComposedSchemas){
            flattenComposedSchema(inner,key);
            if (inner.get$ref() == null) {
                key = "inline_response_items" + key;
                String modelName = resolveModelName(inner.getTitle(), key );
                ap.setItems(this.makeRefProperty(modelName, inner));
                addGenerated(modelName, inner);
                openAPI.getComponents().addSchemas(modelName, inner);
            }
        }
    }


    private void flattenMapSchema(Schema innerProperty, String key, String pathname, Schema mediaSchema) {
        if (innerProperty.getProperties() != null && innerProperty.getProperties().size() > 0) {
            flattenProperties(innerProperty.getProperties(), pathname);
            String modelName = resolveModelName(innerProperty.getTitle(), key);
            String existing = matchGenerated(innerProperty);
            if (existing != null) {
                mediaSchema.setAdditionalProperties(new Schema().$ref(existing));
            } else {
                mediaSchema.setAdditionalProperties(new Schema().$ref(modelName));
                addGenerated(modelName, innerProperty);
                openAPI.getComponents().addSchemas(modelName, innerProperty);
            }
        }else if (innerProperty instanceof ComposedSchema && this.flattenComposedSchemas){
            flattenComposedSchema(innerProperty,key);
            if (innerProperty.get$ref() == null) {
                String modelName = resolveModelName(innerProperty.getTitle(), key);
                mediaSchema.setAdditionalProperties(new Schema().$ref(modelName));
                addGenerated(modelName, innerProperty);
                openAPI.getComponents().addSchemas(modelName, innerProperty);
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
    
    private static String pathBody(String pathname)
    {
      String[] parts = pathname.split("/");
      StringBuilder body = new StringBuilder();
      if (parts.length > 2)
      {
        body.append(normalize(parts[parts.length-2])).append('_');
      }
      if (parts.length > 1)
      {
        body.append(normalize(parts[parts.length-1])).append('_');
      }
      body.append("body");
      return body.toString();
    }
    
    private static String normalize(String pathPart)
    {
      return pathPart.replace(".", "_");
    }

    private String resolveModelName(String title, String key) {
        if (title == null) {
            return uniqueName(key);
        } else {
            return uniqueName(title);
        }
    }

    public String matchGenerated(Schema model) {
        if (skipMatches) {
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
        if (camelCaseFlattenNaming) {
            String uniqueKey;
            String concatenated = "";
            for (int i = 0; i < key.split("[-|\\s|_]").length; i++) {
                uniqueKey = key.split("[-|\\s|_]")[i];
                uniqueKey = uniqueKey.substring(0, 1).toUpperCase() + uniqueKey.substring(1);
                concatenated = concatenated.concat(uniqueKey);
            }
            key = concatenated.replaceAll("[^a-z_\\.A-Z0-9 ]", ""); // FIXME: a parameter
        }else {
            key = key.replaceAll("[^a-z_\\.A-Z0-9 ]", ""); // FIXME: a parameter
        }
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
            if (isObjectSchema(property) && property.getProperties() != null && property.getProperties().size() > 0) {
                String modelName = resolveModelName(property.getTitle(), path + "_" + key);
                Schema model = createModelFromProperty(property, modelName);
                String existing = matchGenerated(model);
                if (existing != null) {
                    propsToUpdate.put(key, new Schema().$ref(existing));
                } else {
                    propsToUpdate.put(key, new Schema().$ref(RefType.SCHEMAS.getInternalPrefix()+modelName));
                    modelsToAdd.put(modelName, model);
                    addGenerated(modelName, model);
                    openAPI.getComponents().addSchemas(modelName, model);
                }
            } else if (property instanceof ArraySchema) {
                ArraySchema ap = (ArraySchema) property;
                Schema inner = ap.getItems();
                if (isObjectSchema(inner)) {
                    if (inner.getProperties() != null && inner.getProperties().size() > 0) {
                        flattenProperties(inner.getProperties(), path);
                        String modelName = resolveModelName(inner.getTitle(), path + "_" + key);
                        Schema innerModel = createModelFromProperty(inner, modelName);
                        String existing = matchGenerated(innerModel);
                        if (existing != null) {
                            ap.setItems(new Schema().$ref(existing));
                        } else {
                            ap.setItems(new Schema().$ref(modelName));
                            addGenerated(modelName, innerModel);
                            openAPI.getComponents().addSchemas(modelName, innerModel);
                        }
                    }else if (inner instanceof ComposedSchema && this.flattenComposedSchemas) {
                        flattenComposedSchema(inner,key);
                        String modelName = resolveModelName(inner.getTitle(), path + "_" + key);
                        Schema innerModel = createModelFromProperty(inner, modelName);
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
            } else if (property.getAdditionalProperties() != null && !(property.getAdditionalProperties() instanceof Boolean)) {
                Schema inner = (Schema) property.getAdditionalProperties();
                if (isObjectSchema(inner)) {
                    if (inner.getProperties() != null && inner.getProperties().size() > 0) {
                        flattenProperties(inner.getProperties(), path);
                        String modelName = resolveModelName(inner.getTitle(), path + "_" + key);
                        Schema innerModel = createModelFromProperty(inner, modelName);
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

    private void flattenComposedSchema(Schema inner, String key) {

        ComposedSchema composedSchema = (ComposedSchema) inner;
        String inlineModelName = "";

        List<Schema> list = null;
        if (composedSchema.getAllOf() != null) {
            list  = composedSchema.getAllOf();
            inlineModelName = "AllOf";
        }else if (composedSchema.getAnyOf() != null) {
            list  = composedSchema.getAnyOf();
            inlineModelName = "AnyOf";
        }else if (composedSchema.getOneOf() != null) {
            list  = composedSchema.getOneOf();
            inlineModelName = "OneOf";
        }

        for(int i= 0; i<list.size();i++){
            if (list.get(i).get$ref() == null){
                Schema inline = list.get(i);
                if (inline.getProperties()!= null){
                    flattenProperties(inline.getProperties(), key);
                }
                if (this.flattenComposedSchemas) {
                    int position = i+1;
                    inlineModelName = resolveModelName(inline.getTitle(),  key + inlineModelName + "_" + position);
                    list.set(i,new Schema().$ref(inlineModelName));
                    addGenerated(inlineModelName, inline);
                    openAPI.getComponents().addSchemas(inlineModelName, inline);
                }
            }
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

    public Schema createModelFromProperty(Schema schema, String path) {
        String description = schema.getDescription();
        String example = null;
        List<String> requiredList = schema.getRequired();


        Object obj = schema.getExample();
        if (obj != null) {
            example = obj.toString();
        }
        String name = schema.getName();
        XML xml = schema.getXml();
        Map<String, Schema> properties = schema.getProperties();


        if (schema instanceof ComposedSchema && this.flattenComposedSchemas){
            ComposedSchema composedModel = (ComposedSchema) schema;

            composedModel.setDescription(description);
            composedModel.setExample(example);
            composedModel.setName(name);
            composedModel.setXml(xml);
            composedModel.setType(schema.getType());
            composedModel.setRequired(requiredList);

            return composedModel;


        } else {
			Schema model = new Schema();
			model.setAdditionalProperties(schema.getAdditionalProperties());
            model.setDescription(description);
            model.setDeprecated(schema.getDeprecated());
            model.setDiscriminator(schema.getDiscriminator());
            model.setEnum(schema.getEnum());
            model.setExample(example);
            model.setExclusiveMaximum(schema.getExclusiveMaximum());
            model.setExclusiveMinimum(schema.getExclusiveMinimum());
            model.setFormat(schema.getFormat());
            model.setMinLength(schema.getMinLength());
            model.setMaximum(schema.getMaximum());
            model.setMaxItems(schema.getMaxItems());
            model.setMaxProperties(schema.getMaxProperties());
            model.setMaxLength(schema.getMaxLength());
            model.setMinimum(schema.getMinimum());
            model.setMinItems(schema.getMinItems());
            model.setMinLength(schema.getMinLength());
            model.setMinProperties(schema.getMinProperties());
            model.setMultipleOf(schema.getMultipleOf());
            model.setName(name);
            model.setNullable(schema.getNullable());
            model.setNot(schema.getNot());
            model.setPattern(schema.getPattern());
            model.setReadOnly(schema.getReadOnly());
            model.setRequired(requiredList);
            model.setUniqueItems(schema.getUniqueItems());
            model.setTitle(schema.getTitle());
            model.setType(schema.getType());
            model.setXml(xml);
            model.setWriteOnly(schema.getWriteOnly());

            if (properties != null) {
                flattenProperties(properties, path);
                model.setProperties(properties);
            }

            return model;
        }
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
        if (object.getAdditionalProperties() != null && !(object.getAdditionalProperties() instanceof Boolean)) {
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
        if (source.getExtensions() != null) {
            Map<String, Object> vendorExtensions = source.getExtensions();
            for (String extName : vendorExtensions.keySet()) {
                target.addExtension(extName, vendorExtensions.get(extName));
            }
        }
    }

    private boolean isObjectSchema(Schema schema) {
        return schema instanceof ObjectSchema
                || "object".equalsIgnoreCase(schema.getType())
                || (schema.getType() == null && schema.getProperties() != null && !schema.getProperties().isEmpty()
                || schema instanceof ComposedSchema);
    }

    public boolean isSkipMatches() {
        return skipMatches;
    }

    public void setSkipMatches(boolean skipMatches) {
        this.skipMatches = skipMatches;
    }
}
