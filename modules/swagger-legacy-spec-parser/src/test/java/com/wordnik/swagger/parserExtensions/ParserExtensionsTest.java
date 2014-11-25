package com.wordnik.swagger.parserExtensions;

import com.wordnik.swagger.parser.*;

import org.testng.annotations.Test;

import java.io.File;
import java.util.List;

import static org.testng.Assert.*;

public class ParserExtensionsTest {
  @Test
  public void readAllExtensions() throws Exception {
    SwaggerParser parser = new SwaggerParser();

    List<SwaggerParserExtension> extensions = parser.getExtensions();

    assertTrue(extensions.size() == 2, "Didn't find 2 extensions as expected");
    for(SwaggerParserExtension extension : extensions) {
      System.out.println(extension);
    }
  }
}