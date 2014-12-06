package com.wordnik.swagger.converter;

import io.swagger.parser.SwaggerLegacyConverter;

import com.wordnik.swagger.models.*;
import com.wordnik.swagger.models.auth.*;
import com.wordnik.swagger.util.Json;

import java.util.*;
import java.io.*;

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

    assertTrue(swagger.getSecurityDefinitions().size() == 2);
    SecuritySchemeDefinition auth = swagger.getSecurityDefinitions().get("oauth2");
    assertNotNull(auth);
    assertEquals(auth.getClass(), OAuth2Definition.class);
    OAuth2Definition oauth2 = (OAuth2Definition) auth;

    assertEquals(oauth2.getFlow(), "implicit");
    assertEquals(oauth2.getAuthorizationUrl(), "http://petstore.swagger.wordnik.com/oauth/dialog");
    assertTrue(oauth2.getScopes().size() == 2);
    Map<String, String> scopes = oauth2.getScopes();
    assertEquals(scopes.get("email"), "Access to your email address");
    assertEquals(scopes.get("pets"), "Access to your pets");

    auth = swagger.getSecurityDefinitions().get("apiKey");
    assertNotNull(auth);
    assertEquals(auth.getClass(), ApiKeyAuthDefinition.class);
    ApiKeyAuthDefinition apiKey = (ApiKeyAuthDefinition) auth;

    assertEquals(apiKey.getName(), "api_key");
    assertEquals(apiKey.getIn(), In.HEADER);


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

  /**
   * reads a single-file swagger definition
   **/
  @Test
  public void convertMultipleFiles() throws Exception {
    Swagger swagger = converter.read("http://petstore.swagger.wordnik.com/api/api-docs");
  }
}