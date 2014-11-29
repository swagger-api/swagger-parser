package com.wordnik.swagger.converter;

import io.swagger.parser.SwaggerLegacyConverter;
import io.swagger.models.ParamType;
import io.swagger.models.Format;
import io.swagger.models.resourcelisting.ResourceListing;
import io.swagger.models.apideclaration.ApiDeclaration;

import com.wordnik.swagger.models.Swagger;
import com.wordnik.swagger.models.Info;
import com.wordnik.swagger.models.parameters.*;
import com.wordnik.swagger.models.properties.*;
import com.wordnik.swagger.util.Json;

import java.util.*;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class ResourceListingConverterTest {
  SwaggerLegacyConverter converter = new SwaggerLegacyConverter();

  @Test
  public void convertResourceListingWithRootPath() throws Exception {
    ResourceListing rl = new ResourceListing();
    rl.setApiVersion("2.11");

    List<ApiDeclaration> apis = new ArrayList<ApiDeclaration>();
    ApiDeclaration api = new ApiDeclaration();
    api.setBasePath("http://foo.com");
    apis.add(api);

    Swagger swagger = converter.convert(rl, apis);

    assertTrue(swagger.getSchemes().size() == 1);
    assertTrue(swagger.getSwagger().equals("2.0"));

    Info info = swagger.getInfo();
    assertNotNull(info);
    assertEquals(info.getVersion(), rl.getApiVersion());
    assertEquals(swagger.getBasePath(), "/");
    assertEquals(swagger.getHost(), "foo.com");
  }

  @Test
  public void convertResourceListingWithSubPath() throws Exception {
    ResourceListing rl = new ResourceListing();
    rl.setApiVersion("2.11");

    List<ApiDeclaration> apis = new ArrayList<ApiDeclaration>();
    ApiDeclaration api = new ApiDeclaration();
    api.setBasePath("https://foo.com/baz/bar");
    apis.add(api);

    Swagger swagger = converter.convert(rl, apis);

    Json.prettyPrint(swagger);

    assertTrue(swagger.getSchemes().size() == 1);
    assertTrue(swagger.getSchemes().get(0).equals("https"));
    assertTrue(swagger.getSwagger().equals("2.0"));

    Info info = swagger.getInfo();
    assertNotNull(info);
    assertEquals(info.getVersion(), rl.getApiVersion());
    assertEquals(swagger.getBasePath(), "/baz/bar");
    assertEquals(swagger.getHost(), "foo.com");
  }
}


