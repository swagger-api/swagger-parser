package io.swagger.parser.test;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.converter.SwaggerConverter;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * Tests for null handling in Swagger V2 to OpenAPI V3 conversion
 */
public class V2ConverterNullHandlingTest {

    @Test
    public void testV2ParameterWithoutDefaultNotSetInV3() {
        String v2Yaml = "swagger: '2.0'\n" +
                "info:\n" +
                "  title: Test\n" +
                "  version: 1.0.0\n" +
                "paths:\n" +
                "  /test:\n" +
                "    get:\n" +
                "      parameters:\n" +
                "        - name: param1\n" +
                "          in: query\n" +
                "          type: string\n" +
                "      responses:\n" +
                "        '200':\n" +
                "          description: OK\n";

        OpenAPI openAPI = new SwaggerConverter().readContents(v2Yaml, null, null).getOpenAPI();
        assertNotNull(openAPI);
        System.out.println(Json.pretty(openAPI));

        java.util.List<io.swagger.v3.oas.models.parameters.Parameter> params = openAPI.getPaths().get("/test").getGet().getParameters();
        assertEquals(params.size(), 1);
        
        Schema schema = params.get(0).getSchema();
        assertNotNull(schema);
        assertNull(schema.getDefault(), "Default should be null");
        assertFalse(schema.getDefaultSetFlag(), "Default should not be set");
    }

    @Test
    public void testV2ParameterWithDefaultPreservedInV3() {
        String v2Yaml = "swagger: '2.0'\n" +
                "info:\n" +
                "  title: Test\n" +
                "  version: 1.0.0\n" +
                "paths:\n" +
                "  /test:\n" +
                "    get:\n" +
                "      parameters:\n" +
                "        - name: param1\n" +
                "          in: query\n" +
                "          type: string\n" +
                "          default: 'test'\n" +
                "      responses:\n" +
                "        '200':\n" +
                "          description: OK\n";

        OpenAPI openAPI = new SwaggerConverter().readContents(v2Yaml, null, null).getOpenAPI();
        assertNotNull(openAPI);
        System.out.println(Json.pretty(openAPI));
        java.util.List<io.swagger.v3.oas.models.parameters.Parameter> params = openAPI.getPaths().get("/test").getGet().getParameters();
        assertEquals(params.size(), 1);
        
        Schema schema = params.get(0).getSchema();
        assertNotNull(schema);
        assertEquals(schema.getDefault(), "test", "Default should be 'test'");
        assertTrue(schema.getDefaultSetFlag(), "Default should be set");
    }

    @Test
    public void testV2BodyParameterWithoutDefaultNotSetInV3() {
        String v2Yaml = "swagger: '2.0'\n" +
                "info:\n" +
                "  title: Test\n" +
                "  version: 1.0.0\n" +
                "paths:\n" +
                "  /test:\n" +
                "    post:\n" +
                "      parameters:\n" +
                "        - name: body\n" +
                "          in: body\n" +
                "          schema:\n" +
                "            type: object\n" +
                "            properties:\n" +
                "              name:\n" +
                "                type: string\n" +
                "      responses:\n" +
                "        '200':\n" +
                "          description: OK\n";

        OpenAPI openAPI = new SwaggerConverter().readContents(v2Yaml, null, null).getOpenAPI();
        assertNotNull(openAPI);
        System.out.println(Json.pretty(openAPI));

        io.swagger.v3.oas.models.parameters.RequestBody requestBody = openAPI.getPaths().get("/test").getPost().getRequestBody();
        assertNotNull(requestBody);
        assertNotNull(requestBody.getContent());
        assertFalse(requestBody.getContent().isEmpty());
        
        String mediaType = requestBody.getContent().containsKey("application/json") ? "application/json" : "*/*";
        Schema schema = requestBody.getContent().get(mediaType).getSchema();
        assertNotNull(schema);
        assertNull(schema.getDefault(), "Default should be null");
        assertFalse(schema.getDefaultSetFlag(), "Default should not be set");
    }

    @Test
    public void testV2DefinitionWithoutDefaultNotSetInV3() {
        String v2Yaml = "swagger: '2.0'\n" +
                "info:\n" +
                "  title: Test\n" +
                "  version: 1.0.0\n" +
                "paths: {}\n" +
                "definitions:\n" +
                "  TestModel:\n" +
                "    type: object\n" +
                "    properties:\n" +
                "      name:\n" +
                "        type: string\n";

        OpenAPI openAPI = new SwaggerConverter().readContents(v2Yaml, null, null).getOpenAPI();
        assertNotNull(openAPI);
        System.out.println(Json.pretty(openAPI));

        Schema schema = openAPI.getComponents().getSchemas().get("TestModel");
        assertNotNull(schema);
        assertNull(schema.getDefault(), "Default should be null");
        assertFalse(schema.getDefaultSetFlag(), "Default should not be set");
    }

    @Test
    public void testV2PropertyWithDefaultPreservedInV3() {
        String v2Yaml = "swagger: '2.0'\n" +
                "info:\n" +
                "  title: Test\n" +
                "  version: 1.0.0\n" +
                "paths: {}\n" +
                "definitions:\n" +
                "  TestModel:\n" +
                "    type: object\n" +
                "    properties:\n" +
                "      status:\n" +
                "        type: string\n" +
                "        default: 'active'\n";

        OpenAPI openAPI = new SwaggerConverter().readContents(v2Yaml, null, null).getOpenAPI();
        assertNotNull(openAPI);
        System.out.println(Json.pretty(openAPI));

        Schema schema = openAPI.getComponents().getSchemas().get("TestModel");
        assertNotNull(schema);
        
        Schema statusProp = (Schema) schema.getProperties().get("status");
        assertNotNull(statusProp);
        assertEquals(statusProp.getDefault(), "active", "Default should be 'active'");
        assertTrue(statusProp.getDefaultSetFlag(), "Default should be set");
    }

    @Test
    public void testV2ArrayItemsWithoutDefaultNotSetInV3() {
        String v2Yaml = "swagger: '2.0'\n" +
                "info:\n" +
                "  title: Test\n" +
                "  version: 1.0.0\n" +
                "paths:\n" +
                "  /test:\n" +
                "    get:\n" +
                "      parameters:\n" +
                "        - name: items\n" +
                "          in: query\n" +
                "          type: array\n" +
                "          items:\n" +
                "            type: string\n" +
                "      responses:\n" +
                "        '200':\n" +
                "          description: OK\n";

        OpenAPI openAPI = new SwaggerConverter().readContents(v2Yaml, null, null).getOpenAPI();
        assertNotNull(openAPI);
        System.out.println(Json.pretty(openAPI));

        java.util.List<io.swagger.v3.oas.models.parameters.Parameter> params = openAPI.getPaths().get("/test").getGet().getParameters();
        assertEquals(params.size(), 1);
        
        Schema schema = params.get(0).getSchema();
        assertNotNull(schema);
        assertTrue(schema instanceof io.swagger.v3.oas.models.media.ArraySchema);
        
        io.swagger.v3.oas.models.media.ArraySchema arraySchema = 
            (io.swagger.v3.oas.models.media.ArraySchema) schema;
        assertNull(arraySchema.getDefault(), "Default should be null");
        assertFalse(arraySchema.getDefaultSetFlag(), "Default should not be set");
    }

    @Test
    public void testV2ExamplePreservedInV3() {
        String v2Yaml = "swagger: '2.0'\n" +
                "info:\n" +
                "  title: Test\n" +
                "  version: 1.0.0\n" +
                "paths: {}\n" +
                "definitions:\n" +
                "  TestModel:\n" +
                "    type: object\n" +
                "    example:\n" +
                "      name: 'test'\n" +
                "    properties:\n" +
                "      name:\n" +
                "        type: string\n";

        OpenAPI openAPI = new SwaggerConverter().readContents(v2Yaml, null, null).getOpenAPI();
        assertNotNull(openAPI);
        
        Schema schema = openAPI.getComponents().getSchemas().get("TestModel");
        assertNotNull(schema);
        assertNotNull(schema.getExample(), "Example should be set");
    }

    @Test
    public void testV2PropertyWithoutExampleNotSetInV3() {
        String v2Yaml = "swagger: '2.0'\n" +
                "info:\n" +
                "  title: Test\n" +
                "  version: 1.0.0\n" +
                "paths: {}\n" +
                "definitions:\n" +
                "  TestModel:\n" +
                "    type: object\n" +
                "    properties:\n" +
                "      name:\n" +
                "        type: string\n";

        OpenAPI openAPI = new SwaggerConverter().readContents(v2Yaml, null, null).getOpenAPI();
        assertNotNull(openAPI);
        
        Schema schema = openAPI.getComponents().getSchemas().get("TestModel");
        assertNotNull(schema);
        
        Schema nameProp = (Schema) schema.getProperties().get("name");
        assertNotNull(nameProp);
        assertNull(nameProp.getExample(), "Example should be null");
        assertFalse(nameProp.getExampleSetFlag(), "Example should not be set");
    }

    @Test
    public void testV2IntegerParameterWithDefault() {
        String v2Yaml = "swagger: '2.0'\n" +
                "info:\n" +
                "  title: Test\n" +
                "  version: 1.0.0\n" +
                "paths:\n" +
                "  /test:\n" +
                "    get:\n" +
                "      parameters:\n" +
                "        - name: limit\n" +
                "          in: query\n" +
                "          type: integer\n" +
                "          default: 10\n" +
                "      responses:\n" +
                "        '200':\n" +
                "          description: OK\n";

        OpenAPI openAPI = new SwaggerConverter().readContents(v2Yaml, null, null).getOpenAPI();
        assertNotNull(openAPI);
        
        java.util.List<io.swagger.v3.oas.models.parameters.Parameter> params = openAPI.getPaths().get("/test").getGet().getParameters();
        assertEquals(params.size(), 1);
        
        Schema schema = params.get(0).getSchema();
        assertNotNull(schema);
        assertEquals(schema.getDefault(), 10, "Default should be 10");
        assertTrue(schema.getDefaultSetFlag(), "Default should be set");
    }

    @Test
    public void testV2BooleanParameterWithDefault() {
        String v2Yaml = "swagger: '2.0'\n" +
                "info:\n" +
                "  title: Test\n" +
                "  version: 1.0.0\n" +
                "paths:\n" +
                "  /test:\n" +
                "    get:\n" +
                "      parameters:\n" +
                "        - name: verbose\n" +
                "          in: query\n" +
                "          type: boolean\n" +
                "          default: false\n" +
                "      responses:\n" +
                "        '200':\n" +
                "          description: OK\n";

        OpenAPI openAPI = new SwaggerConverter().readContents(v2Yaml, null, null).getOpenAPI();
        assertNotNull(openAPI);
        
        java.util.List<io.swagger.v3.oas.models.parameters.Parameter> params = openAPI.getPaths().get("/test").getGet().getParameters();
        assertEquals(params.size(), 1);
        
        Schema schema = params.get(0).getSchema();
        assertNotNull(schema);
        assertEquals(schema.getDefault(), false, "Default should be false");
        assertTrue(schema.getDefaultSetFlag(), "Default should be set");
    }
}
