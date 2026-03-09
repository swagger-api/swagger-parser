package io.swagger.v3.parser.test;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * Comprehensive tests for null handling after swagger-core changes.
 * Tests verify the distinction between "field not set" and "field explicitly set to null".
 */
public class NullHandlingTest {

    @Test
    public void testSchemaDefaultExplicitlyNull() {
        String yaml = "openapi: 3.0.0\n" +
                "info:\n" +
                "  title: Test\n" +
                "  version: 1.0.0\n" +
                "paths: {}\n" +
                "components:\n" +
                "  schemas:\n" +
                "    NullableSchema:\n" +
                "      type: string\n" +
                "      nullable: true\n" +
                "      default: null\n";

        OpenAPI openAPI = new OpenAPIV3Parser().readContents(yaml, null, null).getOpenAPI();
        assertNotNull(openAPI);

        Schema schema = openAPI.getComponents().getSchemas().get("NullableSchema");
        assertNotNull(schema);
        assertNull(schema.getDefault(), "Default should be null");
        assertTrue(schema.getDefaultSetFlag(), "Default set flag should be true");
    }

    @Test
    public void testSchemaDefaultNotSet() {
        String yaml = "openapi: 3.0.0\n" +
                "info:\n" +
                "  title: Test\n" +
                "  version: 1.0.0\n" +
                "paths: {}\n" +
                "components:\n" +
                "  schemas:\n" +
                "    NoDefaultSchema:\n" +
                "      type: string\n";

        OpenAPI openAPI = new OpenAPIV3Parser().readContents(yaml, null, null).getOpenAPI();
        assertNotNull(openAPI);
        
        Schema schema = openAPI.getComponents().getSchemas().get("NoDefaultSchema");
        assertNotNull(schema);
        assertNull(schema.getDefault(), "Default should be null");
        assertFalse(schema.getDefaultSetFlag(), "Default set flag should be false");
    }

    @Test
    public void testSchemaExampleExplicitlyNull() {
        String yaml = "openapi: 3.0.0\n" +
                "info:\n" +
                "  title: Test\n" +
                "  version: 1.0.0\n" +
                "paths: {}\n" +
                "components:\n" +
                "  schemas:\n" +
                "    NullExampleSchema:\n" +
                "      type: string\n" +
                "      example: null\n";

        OpenAPI openAPI = new OpenAPIV3Parser().readContents(yaml, null, null).getOpenAPI();
        assertNotNull(openAPI);
        

        Schema schema = openAPI.getComponents().getSchemas().get("NullExampleSchema");
        assertNotNull(schema);
        assertNull(schema.getExample(), "Example should be null");
        assertTrue(schema.getExampleSetFlag(), "Example set flag should be true");
    }

    @Test
    public void testSchemaExampleNotSet() {
        String yaml = "openapi: 3.0.0\n" +
                "info:\n" +
                "  title: Test\n" +
                "  version: 1.0.0\n" +
                "paths: {}\n" +
                "components:\n" +
                "  schemas:\n" +
                "    NoExampleSchema:\n" +
                "      type: string\n";

        OpenAPI openAPI = new OpenAPIV3Parser().readContents(yaml, null, null).getOpenAPI();
        assertNotNull(openAPI);
        

        Schema schema = openAPI.getComponents().getSchemas().get("NoExampleSchema");
        assertNotNull(schema);
        assertNull(schema.getExample(), "Example should be null");
        assertFalse(schema.getExampleSetFlag(), "Example set flag should be false");
    }

    @Test
    public void testExampleValueExplicitlyNull() {
        String yaml = "openapi: 3.0.0\n" +
                "info:\n" +
                "  title: Test\n" +
                "  version: 1.0.0\n" +
                "paths: {}\n" +
                "components:\n" +
                "  examples:\n" +
                "    NullExample:\n" +
                "      value: null\n";

        OpenAPI openAPI = new OpenAPIV3Parser().readContents(yaml, null, null).getOpenAPI();
        assertNotNull(openAPI);
        

        Example example = openAPI.getComponents().getExamples().get("NullExample");
        assertNotNull(example);
        assertNull(example.getValue(), "Value should be null");
        assertTrue(example.getValueSetFlag(), "Value set flag should be true");
    }

    @Test
    public void testExampleValueNotSet() {
        String yaml = "openapi: 3.0.0\n" +
                "info:\n" +
                "  title: Test\n" +
                "  version: 1.0.0\n" +
                "paths: {}\n" +
                "components:\n" +
                "  examples:\n" +
                "    NoValueExample:\n" +
                "      summary: Example without value\n";

        OpenAPI openAPI = new OpenAPIV3Parser().readContents(yaml, null, null).getOpenAPI();
        assertNotNull(openAPI);
        

        Example example = openAPI.getComponents().getExamples().get("NoValueExample");
        assertNotNull(example);
        assertNull(example.getValue(), "Value should be null");
        assertFalse(example.getValueSetFlag(), "Value set flag should be false");
    }

    @Test(description = "Test allOf with source default preserves it when not resolving combinators")
    public void testAllOfSourceDefaultPreservedWithoutResolvingCombinators() {
        String yaml = "openapi: 3.0.0\n" +
                "info:\n" +
                "  title: Test\n" +
                "  version: 1.0.0\n" +
                "paths: {}\n" +
                "components:\n" +
                "  schemas:\n" +
                "    BaseSchema:\n" +
                "      type: object\n" +
                "      properties:\n" +
                "        name:\n" +
                "          type: string\n" +
                "    ExtendedSchema:\n" +
                "      allOf:\n" +
                "        - $ref: '#/components/schemas/BaseSchema'\n" +
                "      default:\n" +
                "        name: 'default'\n";

        ParseOptions options = new ParseOptions();
        options.setResolveFully(true);
        options.setResolveCombinators(false);

        OpenAPI openAPI = new OpenAPIV3Parser().readContents(yaml, null, options).getOpenAPI();
        assertNotNull(openAPI);

        Schema extended = openAPI.getComponents().getSchemas().get("ExtendedSchema");
        assertNotNull(extended);
        assertNotNull(extended.getDefault(), "Default should be preserved when not resolving combinators");
        assertTrue(extended.getDefaultSetFlag(), "Default should be set");
    }

    @Test(description = "Test anyOf does not propagate null example")
    public void testAnyOfNullExampleNotPropagated() {
        String yaml = "openapi: 3.0.0\n" +
                "info:\n" +
                "  title: Test\n" +
                "  version: 1.0.0\n" +
                "paths: {}\n" +
                "components:\n" +
                "  schemas:\n" +
                "    Schema1:\n" +
                "      type: string\n" +
                "    Schema2:\n" +
                "      type: number\n" +
                "    CombinedSchema:\n" +
                "      anyOf:\n" +
                "        - $ref: '#/components/schemas/Schema1'\n" +
                "        - $ref: '#/components/schemas/Schema2'\n";

        ParseOptions options = new ParseOptions();
        options.setResolveFully(true);
        options.setResolveCombinators(true);
        
        OpenAPI openAPI = new OpenAPIV3Parser().readContents(yaml, null, options).getOpenAPI();
        assertNotNull(openAPI);

        Schema combined = openAPI.getComponents().getSchemas().get("CombinedSchema");
        assertNotNull(combined);
        assertNull(combined.getExample(), "Example should be null");
        assertFalse(combined.getExampleSetFlag(), "Example should not be set");
    }

    @Test(description = "Test oneOf preserves explicit null example from source")
    public void testOneOfExplicitNullExamplePreserved() {
        String yaml = "openapi: 3.0.0\n" +
                "info:\n" +
                "  title: Test\n" +
                "  version: 1.0.0\n" +
                "paths: {}\n" +
                "components:\n" +
                "  schemas:\n" +
                "    Schema1:\n" +
                "      type: string\n" +
                "    Schema2:\n" +
                "      type: number\n" +
                "    CombinedSchema:\n" +
                "      oneOf:\n" +
                "        - $ref: '#/components/schemas/Schema1'\n" +
                "        - $ref: '#/components/schemas/Schema2'\n" +
                "      example: null\n";

        ParseOptions options = new ParseOptions();
        options.setResolveFully(true);
        options.setResolveCombinators(true);
        
        OpenAPI openAPI = new OpenAPIV3Parser().readContents(yaml, null, options).getOpenAPI();
        assertNotNull(openAPI);
        
        Schema combined = openAPI.getComponents().getSchemas().get("CombinedSchema");
        assertNotNull(combined);
        assertNull(combined.getExample(), "Example should be null");
        assertTrue(combined.getExampleSetFlag(), "Example should be explicitly set");
    }

    @Test(description = "Test property example null vs not set")
    public void testPropertyExampleNullVsNotSet() {
        String yaml = "openapi: 3.0.0\n" +
                "info:\n" +
                "  title: Test\n" +
                "  version: 1.0.0\n" +
                "paths: {}\n" +
                "components:\n" +
                "  schemas:\n" +
                "    TestSchema:\n" +
                "      type: object\n" +
                "      properties:\n" +
                "        withNullExample:\n" +
                "          type: string\n" +
                "          example: null\n" +
                "        withoutExample:\n" +
                "          type: string\n" +
                "        withExample:\n" +
                "          type: string\n" +
                "          example: 'test'\n";

        OpenAPI openAPI = new OpenAPIV3Parser().readContents(yaml, null, null).getOpenAPI();
        assertNotNull(openAPI);

        Schema testSchema = openAPI.getComponents().getSchemas().get("TestSchema");
        assertNotNull(testSchema);

        Schema withNull = (Schema) testSchema.getProperties().get("withNullExample");
        assertNull(withNull.getExample());
        assertTrue(withNull.getExampleSetFlag(), "Should be explicitly set to null");

        Schema without = (Schema) testSchema.getProperties().get("withoutExample");
        assertNull(without.getExample());
        assertFalse(without.getExampleSetFlag(), "Should not be set");

        Schema withExample = (Schema) testSchema.getProperties().get("withExample");
        assertEquals(withExample.getExample(), "test");
        assertTrue(withExample.getExampleSetFlag(), "Should be set");
    }

    @Test(description = "Test parameter schema example null vs not set")
    public void testParameterSchemaExampleNullVsNotSet() {
        String yaml = "openapi: 3.0.0\n" +
                "info:\n" +
                "  title: Test\n" +
                "  version: 1.0.0\n" +
                "paths:\n" +
                "  /test:\n" +
                "    get:\n" +
                "      parameters:\n" +
                "        - name: nullExample\n" +
                "          in: query\n" +
                "          schema:\n" +
                "            type: string\n" +
                "            example: null\n" +
                "        - name: noExample\n" +
                "          in: query\n" +
                "          schema:\n" +
                "            type: string\n" +
                "      responses:\n" +
                "        '200':\n" +
                "          description: OK\n";

        OpenAPI openAPI = new OpenAPIV3Parser().readContents(yaml, null, null).getOpenAPI();
        assertNotNull(openAPI);

        java.util.List<io.swagger.v3.oas.models.parameters.Parameter> params = openAPI.getPaths().get("/test").getGet().getParameters();
        assertEquals(params.size(), 2);
        
        Schema nullSchema = params.get(0).getSchema();
        assertNull(nullSchema.getExample());
        assertTrue(nullSchema.getExampleSetFlag(), "Should be explicitly set to null");
        
        Schema noExampleSchema = params.get(1).getSchema();
        assertNull(noExampleSchema.getExample());
        assertFalse(noExampleSchema.getExampleSetFlag(), "Should not be set");
    }

    @Test(description = "Test media type example null vs not set")
    public void testMediaTypeExampleNullVsNotSet() {
        String yaml = "openapi: 3.0.0\n" +
                "info:\n" +
                "  title: Test\n" +
                "  version: 1.0.0\n" +
                "paths:\n" +
                "  /test:\n" +
                "    post:\n" +
                "      requestBody:\n" +
                "        content:\n" +
                "          application/json:\n" +
                "            schema:\n" +
                "              type: object\n" +
                "            example: null\n" +
                "          application/xml:\n" +
                "            schema:\n" +
                "              type: object\n" +
                "      responses:\n" +
                "        '200':\n" +
                "          description: OK\n";

        OpenAPI openAPI = new OpenAPIV3Parser().readContents(yaml, null, null).getOpenAPI();
        assertNotNull(openAPI);
        
        io.swagger.v3.oas.models.media.Content content = openAPI.getPaths().get("/test").getPost().getRequestBody().getContent();
        
        io.swagger.v3.oas.models.media.MediaType jsonMedia = content.get("application/json");
        assertNull(jsonMedia.getExample());
        assertTrue(jsonMedia.getExampleSetFlag(), "Should be explicitly set to null");
        
        io.swagger.v3.oas.models.media.MediaType xmlMedia = content.get("application/xml");
        assertNull(xmlMedia.getExample());
        assertFalse(xmlMedia.getExampleSetFlag(), "Should not be set");
    }

    @Test(description = "Test header schema example null vs not set")
    public void testHeaderSchemaExampleNullVsNotSet() {
        String yaml = "openapi: 3.0.0\n" +
                "info:\n" +
                "  title: Test\n" +
                "  version: 1.0.0\n" +
                "paths:\n" +
                "  /test:\n" +
                "    get:\n" +
                "      responses:\n" +
                "        '200':\n" +
                "          description: OK\n" +
                "          headers:\n" +
                "            X-Null-Example:\n" +
                "              schema:\n" +
                "                type: string\n" +
                "                example: null\n" +
                "            X-No-Example:\n" +
                "              schema:\n" +
                "                type: string\n";

        OpenAPI openAPI = new OpenAPIV3Parser().readContents(yaml, null, null).getOpenAPI();
        assertNotNull(openAPI);
        
        java.util.Map<String, io.swagger.v3.oas.models.headers.Header> headers = openAPI.getPaths().get("/test").getGet().getResponses().get("200").getHeaders();
        
        Schema nullSchema = headers.get("X-Null-Example").getSchema();
        assertNull(nullSchema.getExample());
        assertTrue(nullSchema.getExampleSetFlag(), "Should be explicitly set to null");
        
        Schema noExampleSchema = headers.get("X-No-Example").getSchema();
        assertNull(noExampleSchema.getExample());
        assertFalse(noExampleSchema.getExampleSetFlag(), "Should not be set");
    }

    @Test(description = "Test multiple defaults in allOf - should not set any if different")
    public void testAllOfMultipleDifferentDefaults() {
        String yaml = "openapi: 3.0.0\n" +
                "info:\n" +
                "  title: Test\n" +
                "  version: 1.0.0\n" +
                "paths: {}\n" +
                "components:\n" +
                "  schemas:\n" +
                "    Schema1:\n" +
                "      type: object\n" +
                "      default:\n" +
                "        value: 1\n" +
                "      properties:\n" +
                "        value:\n" +
                "          type: integer\n" +
                "    Schema2:\n" +
                "      type: object\n" +
                "      default:\n" +
                "        value: 2\n" +
                "      properties:\n" +
                "        value:\n" +
                "          type: integer\n" +
                "    CombinedSchema:\n" +
                "      allOf:\n" +
                "        - $ref: '#/components/schemas/Schema1'\n" +
                "        - $ref: '#/components/schemas/Schema2'\n";

        ParseOptions options = new ParseOptions();
        options.setResolveFully(true);
        options.setResolveCombinators(true);
        
        OpenAPI openAPI = new OpenAPIV3Parser().readContents(yaml, null, options).getOpenAPI();
        assertNotNull(openAPI);

        Schema combined = openAPI.getComponents().getSchemas().get("CombinedSchema");
        assertNotNull(combined);
        // When multiple different defaults exist, none should be set
        assertNull(combined.getDefault(), "Default should not be set when multiple different defaults exist");
    }

    @Test(description = "Test resolveFully without resolveCombinators preserves defaults")
    public void testResolveFullyWithoutCombinators() {
        String yaml = "openapi: 3.0.0\n" +
                "info:\n" +
                "  title: Test\n" +
                "  version: 1.0.0\n" +
                "paths: {}\n" +
                "components:\n" +
                "  schemas:\n" +
                "    BaseSchema:\n" +
                "      type: object\n" +
                "      default:\n" +
                "        name: 'test'\n" +
                "      properties:\n" +
                "        name:\n" +
                "          type: string\n" +
                "    ExtendedSchema:\n" +
                "      allOf:\n" +
                "        - $ref: '#/components/schemas/BaseSchema'\n" +
                "      default:\n" +
                "        name: 'extended'\n";

        ParseOptions options = new ParseOptions();
        options.setResolveFully(true);
        options.setResolveCombinators(false);
        
        OpenAPI openAPI = new OpenAPIV3Parser().readContents(yaml, null, options).getOpenAPI();
        assertNotNull(openAPI);
        

        Schema extended = openAPI.getComponents().getSchemas().get("ExtendedSchema");
        assertNotNull(extended);
        assertNotNull(extended.getDefault(), "Default should be preserved");
        // Default is stored as a JsonNode internally
        assertTrue(extended.getDefault() instanceof com.fasterxml.jackson.databind.node.ObjectNode);
    }
}
