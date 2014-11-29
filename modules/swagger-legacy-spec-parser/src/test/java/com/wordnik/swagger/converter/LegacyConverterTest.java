package com.wordnik.swagger.converter;

import io.swagger.parser.SwaggerLegacyConverter;

import com.wordnik.swagger.models.*;
import com.wordnik.swagger.util.Json;

import java.util.*;
import java.io.*;

import org.apache.commons.io.FileUtils;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class LegacyConverterTest {
  SwaggerLegacyConverter converter = new SwaggerLegacyConverter();

  /**
   * reads a single-file swagger definition
   **/
  @Test
  public void convertSingleFile() throws Exception {
    Swagger swagger = converter.read("src/test/resources/specs/v1_2/singleFile.json");
    assertEquals(swagger.getSwagger(), "2.0");
    assertEquals(swagger.getHost(), "petstore.swagger.wordnik.com");
    assertEquals(swagger.getBasePath(), "/api");
    assertNotNull(swagger.getInfo());

    Info info = swagger.getInfo();
    assertEquals(info.getVersion(), "1.0.0");
    assertEquals(info.getTitle(), "Swagger Sample App");
    assertEquals(info.getTermsOfService(), "http://helloreverb.com/terms/");

    Contact contact = info.getContact();
    assertEquals(contact.getUrl(), "apiteam@wordnik.com");

    License license = info.getLicense();
    assertEquals(license.getName(), "Apache 2.0");
    assertEquals(license.getUrl(), "http://www.apache.org/licenses/LICENSE-2.0.html");

    assertTrue(swagger.getDefinitions().size() == 3);
    assertTrue(swagger.getPaths().size() == 5);
  }
}