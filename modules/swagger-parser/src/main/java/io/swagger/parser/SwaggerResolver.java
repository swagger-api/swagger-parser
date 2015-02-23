package io.swagger.parser;

import io.swagger.parser.util.RemoteUrl;

import com.wordnik.swagger.util.Json;
import com.wordnik.swagger.models.*;
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

  protected ResolverOptions opts;
  public SwaggerResolver(){}
  public SwaggerResolver(ResolverOptions opts) {
    this.opts = opts;
  }
  public Swagger resolve(Swagger swagger, List<AuthorizationValue> auths) {
    if(swagger == null)
      return null;

    Map<String, Object> toResolve = new HashMap<String, Object>();

    detectModelRefs(swagger, toResolve);

    applyResolutions(swagger, toResolve, auths);
    // JsonNode node = Json.mapper().convertValue(swagger, JsonNode.class);

    // // models
    // JsonNode definitions = node.get("definitions");
    // if(definitions != null && definitions instanceof ObjectNode) {
    //   ObjectNode on = (ObjectNode)definitions;

    // }

    // operations

    return swagger;
  }

  public void applyResolutions(Swagger swagger, Map<String, Object> toResolve, List<AuthorizationValue> auths) {
    // hosts to call
    Map<String, List<Object>> hostToObjectMap = new HashMap<String, List<Object>>();

    for(String path : toResolve.keySet()) {
      String[] parts = path.split("#");
      if(parts.length == 2) {
        String host = parts[0];
        String definitionPath = parts[1];
        List<Object> objectList = hostToObjectMap.get(host);
        if(objectList == null) {
          objectList = new ArrayList<Object>();
          hostToObjectMap.put(host, objectList);
        }
        Object mapping = toResolve.get(path);
        
        try {
          String contents = new RemoteUrl().urlToString(host, auths);
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
          }
        }
        catch(Exception e) {
          // failed to get it
          e.printStackTrace();
        }
      }
    }
    // Json.prettyPrint(toResolve);
  }

  public void detectModelRefs(Swagger swagger, Map<String, Object> toResolve) {
    Map<String, Model> models = swagger.getDefinitions();
    if(models != null) {
      for(String modelName : models.keySet()) {
        LOGGER.debug("looking at " + modelName);
        Model model = models.get(modelName);
        if(model instanceof RefModel) {
          RefModel ref = (RefModel) model;
          if(ref.get$ref() != null && ref.get$ref().startsWith("http")) {
            LOGGER.debug("added reference to " + ref.get$ref());
            toResolve.put(ref.get$ref(), ref);
          }
        }
        else if(model instanceof ArrayModel) {
          ArrayModel arrayModel = (ArrayModel) model;
          if(arrayModel.getItems() != null && arrayModel.getItems() instanceof RefProperty) {
            RefProperty ref = (RefProperty)arrayModel.getItems();
            if(ref.get$ref() != null && ref.get$ref().startsWith("http")) {
              LOGGER.debug("added reference to " + ref.get$ref());
              toResolve.put(ref.get$ref(), ref);
            }
          }
        }
        else if(model instanceof ModelImpl) {
          ModelImpl impl = (ModelImpl) model;
          Map<String, Property> properties = impl.getProperties();
          for(String propertyName : properties.keySet()) {
            Property property = properties.get(propertyName);
            if(property instanceof RefProperty) {
              RefProperty ref = (RefProperty)property;
              if(ref.get$ref() != null && ref.get$ref().startsWith("http")) {
                LOGGER.debug("added reference to " + ref.get$ref());
                toResolve.put(ref.get$ref(), ref);
              }
            }
            else if(property instanceof ArrayProperty) {
              ArrayProperty arrayProperty = (ArrayProperty) property;
              if(arrayProperty.getItems() != null && arrayProperty.getItems() instanceof RefProperty) {
                RefProperty ref = (RefProperty)arrayProperty.getItems();
                if(ref.get$ref() != null && ref.get$ref().startsWith("http")) {
                  LOGGER.debug("added reference to " + ref.get$ref());
                  toResolve.put(ref.get$ref(), ref);
                }
              }
            }
            else if(property instanceof MapProperty) {
              MapProperty mp = (MapProperty) property;
              if(mp.getAdditionalProperties() != null && mp.getAdditionalProperties() instanceof RefProperty) {
                RefProperty ref = (RefProperty)mp.getAdditionalProperties();
                if(ref.get$ref() != null && ref.get$ref().startsWith("http")) {
                  LOGGER.debug("added reference to " + ref.get$ref());
                  toResolve.put(ref.get$ref(), ref);
                }                
              }
            }
          }
        }
      }
    }
  }
}