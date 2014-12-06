package io.swagger.parser;

import com.wordnik.swagger.models.Swagger;

import java.util.ServiceLoader;
import java.util.*;
import java.io.IOException;

public class SwaggerParser {
  public Swagger read(String location) {
    if(location == null)
      return null;

    List<SwaggerParserExtension> parserExtensions = getExtensions();
    Swagger output = null;

    System.out.println("reading.....");

    try{
      output = new Swagger20Parser().read(location);
      if(output != null)
        return output;
    }
    catch (IOException e) {
      // continue;
    }
    for(SwaggerParserExtension extension : parserExtensions) {
      try{
        output = extension.read(location);
        if(output != null) {
          return output;
        }
      }
      catch (IOException e) {
        if(System.getProperty("debugParser") != null) {
          e.printStackTrace();
        }
        // continue to next parser
      }
    }
    return null;
  }

  public List<SwaggerParserExtension> getExtensions() {
    ServiceLoader<SwaggerParserExtension> loader = ServiceLoader.load(SwaggerParserExtension.class);
    List<SwaggerParserExtension> output = new ArrayList<SwaggerParserExtension>();
    Iterator<SwaggerParserExtension> itr = loader.iterator();
    while(itr.hasNext()) {
      output.add(itr.next());
    }
    return output;
  }
}