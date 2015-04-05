package io.swagger.parser;

import io.swagger.parser.util.RemoteUrl;

import com.wordnik.swagger.util.Json;
import com.wordnik.swagger.models.*;
import com.wordnik.swagger.models.parameters.*;
import com.wordnik.swagger.models.properties.*;
import com.wordnik.swagger.models.auth.AuthorizationValue;

import com.fasterxml.jackson.databind.JsonNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ServiceLoader;
import java.util.*;
import java.io.IOException;

public class SwaggerResolver {
  Logger LOGGER = LoggerFactory.getLogger(SwaggerResolver.class);
  protected Swagger swagger;
  protected Map<String, List<ResolutionContext>> resolutionMap = new HashMap<String, List<ResolutionContext>>();

  protected ResolverOptions opts;
  public SwaggerResolver(){}
  public SwaggerResolver(ResolverOptions opts) {
    this.opts = opts;
  }
  public Swagger resolve(Swagger swagger, List<AuthorizationValue> auths) {
    if(swagger == null)
      return null;

    this.swagger = swagger;

    // models
    detectModelRefs();

    // operations
    detectOperationRefs();

    applyResolutions(auths);
    return this.swagger;
  }

  public void applyResolutions(List<AuthorizationValue> auths) {
    // hosts to call
    Map<String, List<Object>> hostToObjectMap = new HashMap<String, List<Object>>();

    for(String path : resolutionMap.keySet()) {
      String[] parts = path.split("#");
      if(parts.length == 2) {
        String host = parts[0];
        String definitionPath = parts[1];
        List<Object> objectList = hostToObjectMap.get(host);
        if(objectList == null) {
          objectList = new ArrayList<Object>();
          hostToObjectMap.put(host, objectList);
        }
        List<ResolutionContext> contexts = resolutionMap.get(path);
        for(ResolutionContext ctx : contexts) {
          Object mapping = ctx.object;
          Object target = ctx.parent;
          try {
            String contents = null;
            if(host.startsWith("http"))
              contents = new RemoteUrl().urlToString(host, auths);
            else
              contents = Json.mapper().writeValueAsString(swagger);
            JsonNode location = null;
            String locationName = null;
            if(contents != null) {
              location = Json.mapper().readTree(contents);
              String[] objectPath = definitionPath.split("/");
              for(String objectPathPart : objectPath) {
                LOGGER.debug("getting part " + objectPathPart);
                if(objectPathPart.length() > 0 && location != null) {
                  location = location.get(objectPathPart);
                  locationName = objectPathPart;
                }
              }
            }
            if(location != null) {
              // convert the node to the proper type
              if(mapping instanceof Property) {
                Model model = Json.mapper().convertValue(location, Model.class);
                if(mapping instanceof RefProperty) {
                  RefProperty ref = (RefProperty) mapping;
                  ref.set$ref(locationName);
                  swagger.addDefinition(locationName, model);
                }
              }
              else if(target instanceof Parameter) {
                if(mapping instanceof RefModel) {
                  Model model = Json.mapper().convertValue(location, Model.class);
                  RefModel ref = (RefModel) mapping;
                  ref.set$ref(locationName);
                  swagger.addDefinition(locationName, model);
                }
              }
              else if(target instanceof Operation) {

                // get the operation position
                Operation operation = (Operation) target;
                int position = 0;
                for(Parameter param : operation.getParameters()) {

                  if(param instanceof RefParameter) {
                    RefParameter ref = (RefParameter) param;
                    if(ref.getSimpleRef().equals(locationName)) {
                      // found a match!
                      Parameter remoteParam = Json.mapper().convertValue(location, Parameter.class);
                      operation.getParameters().set(position, remoteParam);
                    }
                  }
                  position += 1;
                }
              }
            }
          }
          catch(Exception e) {
            // failed to get it
            e.printStackTrace();
          }
        }
      }
    }
  }

  public void detectOperationRefs() {
    Map<String, Path> paths = swagger.getPaths();
    if(paths == null) return;

    for(String pathName : paths.keySet()) {
      Path path = paths.get(pathName);
      List<Operation> operations = path.getOperations();
      for(Operation operation : operations) {
        if(operation.getParameters() != null) {
          for(Parameter parameter : operation.getParameters()) {
            if(parameter instanceof BodyParameter) {
              BodyParameter bp = (BodyParameter) parameter;
              if(bp.getSchema() != null && bp.getSchema() instanceof RefModel) {
                RefModel ref = (RefModel)bp.getSchema();
                String key = ref.get$ref();
                if(key.startsWith("http")) {
                  LOGGER.debug("added reference to " + key);
                  List<ResolutionContext> m = resolutionMap.get(key);
                  if(m == null) {
                    m = new ArrayList<ResolutionContext>();
                  }
                  m.add(new ResolutionContext(ref, bp, "ref"));
                  resolutionMap.put(key, m);
                }
              }
            }
            else if(parameter instanceof RefParameter) {
              RefParameter ref = (RefParameter) parameter;
              String key = ref.get$ref();
              LOGGER.debug("added reference to " + ref);

              List<ResolutionContext> m = resolutionMap.get(key);
              if(m == null) {
                m = new ArrayList<ResolutionContext>();
              }
              m.add(new ResolutionContext(ref, operation, "inline"));
              resolutionMap.put(key, m);
            }
          }
        }
        if(operation.getResponses() != null) {
          for(String responseCode : operation.getResponses().keySet()) {
            Response response = operation.getResponses().get(responseCode);
            if(response.getSchema() != null) {
              Property schema = response.getSchema();
              if(schema instanceof RefProperty) {
                RefProperty ref = (RefProperty) schema;
                String key = ref.get$ref();

                if(key != null && key.startsWith("http")) {
                  List<ResolutionContext> m = resolutionMap.get(key);
                  if(m == null) {
                    m = new ArrayList<ResolutionContext>();
                  }
                  m.add(new ResolutionContext(ref, response, "ref"));
                  resolutionMap.put(key, m);
                }
              }
            }
          }
        }
      }
    }
  }

  public void detectModelRefs() {
    Map<String, Model> models = swagger.getDefinitions();
    if(models != null) {
      for(String modelName : models.keySet()) {
        LOGGER.debug("looking at " + modelName);
        Model model = models.get(modelName);
        if(model instanceof RefModel) {
          RefModel ref = (RefModel) model;
          String key = ref.get$ref();
          if(key != null && key.startsWith("http")) {
            LOGGER.debug("added reference to " + key);
            List<ResolutionContext> m = resolutionMap.get(key);
            if(m == null) {
              m = new ArrayList<ResolutionContext>();
            }
            m.add(new ResolutionContext(ref, swagger.getDefinitions(), "ref"));
            resolutionMap.put(key, m);
          }
        }
        else if(model instanceof ArrayModel) {
          ArrayModel arrayModel = (ArrayModel) model;
          if(arrayModel.getItems() != null && arrayModel.getItems() instanceof RefProperty) {
            RefProperty ref = (RefProperty)arrayModel.getItems();
            String key = ref.get$ref();
            if(key != null && key.startsWith("http")) {
              LOGGER.debug("added reference to " + key);
              List<ResolutionContext> m = resolutionMap.get(key);
              if(m == null) {
                m = new ArrayList<ResolutionContext>();
              }
              m.add(new ResolutionContext(ref, swagger.getDefinitions(), "ref"));
              resolutionMap.put(key, m);
            }
          }
        }
        else if(model instanceof ModelImpl) {
          ModelImpl impl = (ModelImpl) model;
          Map<String, Property> properties = impl.getProperties();
          if(properties != null) {
            for(String propertyName : properties.keySet()) {
              Property property = properties.get(propertyName);
              if(property instanceof RefProperty) {
                RefProperty ref = (RefProperty)property;
                String key = ref.get$ref();
                if(key != null && key.startsWith("http")) {
                  LOGGER.debug("added reference to " + key);
                  List<ResolutionContext> m = resolutionMap.get(key);
                  if(m == null) {
                    m = new ArrayList<ResolutionContext>();
                  }
                  m.add(new ResolutionContext(ref, impl, "ref"));
                  resolutionMap.put(key, m);
                }
              }
              else if(property instanceof ArrayProperty) {
                ArrayProperty arrayProperty = (ArrayProperty) property;
                if(arrayProperty.getItems() != null && arrayProperty.getItems() instanceof RefProperty) {
                  RefProperty ref = (RefProperty)arrayProperty.getItems();
                  String key = ref.get$ref();
                  if(key != null && key.startsWith("http")) {
                    LOGGER.debug("added reference to " + key);
                    List<ResolutionContext> m = resolutionMap.get(key);
                    if(m == null) {
                      m = new ArrayList<ResolutionContext>();
                    }
                    m.add(new ResolutionContext(ref, arrayProperty, "ref"));
                    resolutionMap.put(key, m);
                  }
                }
              }
              else if(property instanceof MapProperty) {
                MapProperty mp = (MapProperty) property;
                if(mp.getAdditionalProperties() != null && mp.getAdditionalProperties() instanceof RefProperty) {
                  RefProperty ref = (RefProperty)mp.getAdditionalProperties();
                  String key = ref.get$ref();
                  if(key != null && key.startsWith("http")) {
                    LOGGER.debug("added reference to " + key);
                    List<ResolutionContext> m = resolutionMap.get(key);
                    if(m == null) {
                      m = new ArrayList<ResolutionContext>();
                    }
                    m.add(new ResolutionContext(ref, mp, "ref"));
                    resolutionMap.put(key, m);
                  }                
                }
              }
            }
          }
        }
      }
    }
  }

  static class ResolutionContext {
    private Object object, parent;
    private String scope;

    public ResolutionContext(Object object, Object parent, String scope) {
      this.object = object;
      this.parent = parent;
      this.scope = scope;
    }
  }
}