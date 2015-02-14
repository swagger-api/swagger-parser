package io.swagger.parser;

import com.wordnik.swagger.models.Swagger;
import com.wordnik.swagger.util.*;
import com.wordnik.swagger.models.auth.AuthorizationValue;

import io.swagger.parser.util.RemoteUrl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

public class Swagger20Parser implements SwaggerParserExtension {
  public Swagger read(String location, List<AuthorizationValue> auths) throws IOException {
    System.out.println("reading from " + location);

    try {
      // TODO make smarter
      ObjectMapper mapper = location.toLowerCase().endsWith(".yaml") ?
        Yaml.mapper() :
        Json.mapper();

      JsonNode rootNode = null;

      if(location.toLowerCase().startsWith("http")) {
        String json = RemoteUrl.urlToString(location, auths);
        rootNode = mapper.readTree(json);
      }
      else {
        rootNode = mapper.readTree(new File(location));
      }

      // must have swagger node set
      JsonNode swaggerNode = rootNode.get("swagger");
      if(swaggerNode == null)
        return null;

      return mapper.convertValue(rootNode, Swagger.class);
    }
    catch (Exception e) {
      System.out.println(e.getMessage());
      if(System.getProperty("debugParser") != null) {
        e.printStackTrace();
      }
      return null;
    }
  }

  public Swagger read(JsonNode node) throws IOException {
    if(node == null)
      return null;

    return Json.mapper().convertValue(node, Swagger.class);
  }
}