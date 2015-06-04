package io.swagger.converter;

import io.swagger.models.Info;
import io.swagger.models.Scheme;
import io.swagger.models.Swagger;
import io.swagger.parser.SwaggerCompatConverter;
import io.swagger.models.resourcelisting.ResourceListing;
import io.swagger.models.apideclaration.ApiDeclaration;

import java.util.*;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class ResourceListingConverterTest {
  SwaggerCompatConverter converter = new SwaggerCompatConverter();

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

    assertTrue(swagger.getSchemes().size() == 1);

    assertTrue(swagger.getSchemes().get(0).equals(Scheme.HTTPS));
    assertTrue(swagger.getSwagger().equals("2.0"));

    Info info = swagger.getInfo();
    assertNotNull(info);
    assertEquals(info.getVersion(), rl.getApiVersion());
    assertEquals(swagger.getBasePath(), "/baz/bar");
    assertEquals(swagger.getHost(), "foo.com");
  }
}


