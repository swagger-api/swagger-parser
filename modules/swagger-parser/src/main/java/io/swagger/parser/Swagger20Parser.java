package io.swagger.parser;

import com.wordnik.swagger.models.Swagger;
import com.wordnik.swagger.util.*;
import com.wordnik.swagger.models.auth.AuthorizationValue;

import io.swagger.parser.util.RemoteUrl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

public class Swagger20Parser implements SwaggerParserExtension {
  public Swagger read(String location, List<AuthorizationValue> auths) throws IOException {
    System.out.println("reading from " + location);

    try {
      ObjectMapper mapper = null;
      JsonNode rootNode = null;
      String data = null;

      if(location.toLowerCase().startsWith("http"))
        data = RemoteUrl.urlToString(location, auths);
      else
        data = FileUtils.readFileToString(new File(location), "UTF-8");

      if(data != null) {
        if(data.trim().startsWith("{"))
          mapper = Json.mapper();
        else
          mapper = Yaml.mapper();
      }
      else
        return null;

      rootNode = mapper.readTree(data);

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