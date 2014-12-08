package io.swagger.parser;

import com.wordnik.swagger.models.Swagger;
import com.wordnik.swagger.util.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.net.URL;

public class Swagger20Parser implements SwaggerParserExtension {
  public Swagger read(String location) throws IOException {
    System.out.println("reading from " + location);

    try {
      // TODO make smarter
      ObjectMapper mapper = location.toLowerCase().endsWith(".yaml") ?
        Yaml.mapper() :
        Json.mapper();

      JsonNode rootNode = location.toLowerCase().startsWith("http") ?
        mapper.readTree(new URL(location)) :
        mapper.readTree(new File(location));

      // must have swagger node set
      JsonNode swaggerNode = rootNode.get("swagger");
      if(swaggerNode == null)
        return null;

      return mapper.convertValue(rootNode, Swagger.class);
    }
    catch (Exception e) {
      System.out.println(e.getMessage());
      return null;
    }
  }
}