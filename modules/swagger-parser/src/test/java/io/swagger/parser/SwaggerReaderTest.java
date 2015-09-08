package io.swagger.parser;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import io.swagger.matchers.SerializationMatchers;
import io.swagger.models.Model;
import io.swagger.models.Swagger;
import io.swagger.models.parameters.QueryParameter;
import io.swagger.util.ResourceUtils;

import org.testng.annotations.Test;

import java.io.IOException;

public class SwaggerReaderTest {

    @Test(description = "it should read the uber api")
    public void readUberApi() {
        final SwaggerParser parser = new SwaggerParser();
        final Swagger swagger = parser.read("uber.json");
        assertNotNull(swagger);
    }

    @Test(description = "it should read the simple example with minimum values")
    public void readSimpleExampleWithMinimumValues() {
        final SwaggerParser parser = new SwaggerParser();
        final Swagger swagger = parser.read("sampleWithMinimumValues.yaml");
        final QueryParameter qp = (QueryParameter) swagger.getPaths().get("/pets").getGet().getParameters().get(0);
        assertEquals(qp.getMinimum(), 0.0);
    }

    @Test(description = "it should read the simple example with model extensions")
    public void readSimpleExampleWithModelExtensions() {
        final SwaggerParser parser = new SwaggerParser();
        final Swagger swagger = parser.read("sampleWithMinimumValues.yaml");
        final Model model = swagger.getDefinitions().get("Cat");
        assertNotNull(model.getVendorExtensions().get("x-extension-here"));
    }

    @Test(description = "it should detect yaml")
    public void detectYaml() {
        final SwaggerParser parser = new SwaggerParser();
        final Swagger swagger = parser.read("minimal_y");
        assertEquals(swagger.getSwagger(), "2.0");
    }

    @Test(description = "it should detect json")
    public void detectJson() {
        final SwaggerParser parser = new SwaggerParser();
        final Swagger swagger = parser.read("minimal_j");
        assertEquals(swagger.getSwagger(), "2.0");
    }

    @Test(description = "it should read the issue 16 resource")
    public void testIssue16() {
        final SwaggerParser parser = new SwaggerParser();
        final Swagger swagger = parser.read("issue_16.yaml");
        assertNotNull(swagger);
    }

    @Test(description = "it should test https://github.com/swagger-api/swagger-codegen/issues/469")
    public void testIssue469() {
        final SwaggerParser parser = new SwaggerParser();
        final Swagger swagger = parser.read("issue_469.json");
        final String expectedJson = "{" +
                "  \"id\" : 12345," +
                "  \"name\" : \"Gorilla\"" +
                "}";
        SerializationMatchers.assertEqualsToJson(swagger.getDefinitions().get("Pet").getExample(), expectedJson);
    }

    @Test(description = "it should read the issue 59 resource")
    public void testIssue59() throws IOException {
        final SwaggerParser parser = new SwaggerParser();
        final String path = "uber.json";

        final Swagger swaggerFromString = parser.parse(ResourceUtils.loadClassResource(getClass(), path));
        final Swagger swaggerFromFile = parser.read(path);

        assertEquals(swaggerFromFile, swaggerFromString);
    }
}
