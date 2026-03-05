package io.swagger.v3.parser.test;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static org.testng.Assert.*;

/**
 * Tests for null handling in OpenAPI resolver with references and schema resolution.
 * These tests verify that example: null and default: null are properly handled
 * during the resolution process, including with $ref references.
 */
public class OpenAPIResolverNullHandlingTest {

    @Test
    public void testSchemaWithExplicitNullExample() {
        String yaml = "openapi: 3.0.0\n" +
                "info:\n" +
                "  title: Test\n" +
                "  version: 1.0.0\n" +
                "paths:\n" +
                "  /test:\n" +
                "    get:\n" +
                "      responses:\n" +
                "        '200':\n" +
                "          description: Success\n" +
                "          content:\n" +
                "            application/json:\n" +
                "              schema:\n" +
                "                $ref: '#/components/schemas/TestSchema'\n" +
                "components:\n" +
                "  schemas:\n" +
                "    TestSchema:\n" +
                "      type: object\n" +
                "      example: null\n" +
                "      properties:\n" +
                "        id:\n" +
                "          type: string\n" +
                "        value:\n" +
                "          type: string\n" +
                "          example: null\n";

        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setResolveFully(true);

        SwaggerParseResult result = new OpenAPIV3Parser().readContents(yaml, null, options);
        OpenAPI openAPI = result.getOpenAPI();

        assertNotNull(openAPI);
        Schema testSchema = openAPI.getComponents().getSchemas().get("TestSchema");
        assertNotNull(testSchema);
        assertNull(testSchema.getExample());

        Schema valueProperty = (Schema) testSchema.getProperties().get("value");
        assertNotNull(valueProperty);
        assertNull(valueProperty.getExample());

        Schema responseSchema = openAPI.getPaths().get("/test").getGet()
                .getResponses().get("200").getContent().get("application/json").getSchema();
        assertNull(responseSchema.getExample());
    }

    @Test
    public void testSchemaWithExplicitNullDefault() {
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
                "              $ref: '#/components/schemas/TestSchema'\n" +
                "      responses:\n" +
                "        '200':\n" +
                "          description: Success\n" +
                "components:\n" +
                "  schemas:\n" +
                "    TestSchema:\n" +
                "      type: object\n" +
                "      default: null\n" +
                "      properties:\n" +
                "        status:\n" +
                "          type: string\n" +
                "          default: null\n" +
                "        count:\n" +
                "          type: integer\n" +
                "          default: null\n";

        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setResolveFully(true);

        SwaggerParseResult result = new OpenAPIV3Parser().readContents(yaml, null, options);
        OpenAPI openAPI = result.getOpenAPI();
        

        assertNotNull(openAPI);
        Schema testSchema = openAPI.getComponents().getSchemas().get("TestSchema");
        assertNotNull(testSchema);
        assertNull(testSchema.getDefault());

        Schema statusProperty = (Schema) testSchema.getProperties().get("status");
        assertNotNull(statusProperty);
        assertNull(statusProperty.getDefault());

        Schema countProperty = (Schema) testSchema.getProperties().get("count");
        assertNotNull(countProperty);
        assertNull(countProperty.getDefault());

        Schema requestBodySchema = openAPI.getPaths().get("/test").getPost()
                .getRequestBody().getContent().get("application/json").getSchema();
        assertNull(requestBodySchema.getDefault());
    }

    @Test
    public void testAllOfWithNullExampleAndDefault() {
        String yaml = "openapi: 3.0.0\n" +
                "info:\n" +
                "  title: Test\n" +
                "  version: 1.0.0\n" +
                "paths: {}\n" +
                "components:\n" +
                "  schemas:\n" +
                "    BaseSchema:\n" +
                "      type: object\n" +
                "      example: null\n" +
                "      default: null\n" +
                "      properties:\n" +
                "        id:\n" +
                "          type: string\n" +
                "    ExtendedSchema:\n" +
                "      allOf:\n" +
                "        - $ref: '#/components/schemas/BaseSchema'\n" +
                "        - type: object\n" +
                "          properties:\n" +
                "            name:\n" +
                "              type: string\n" +
                "              example: null\n" +
                "              default: null\n";

        ParseOptions options = new ParseOptions();
        options.setResolveFully(true);
        options.setResolveCombinators(true);

        SwaggerParseResult result = new OpenAPIV3Parser().readContents(yaml, null, options);
        OpenAPI openAPI = result.getOpenAPI();

        assertNotNull(openAPI);
        Schema extendedSchema = openAPI.getComponents().getSchemas().get("ExtendedSchema");
        assertNotNull(extendedSchema);
        
        assertNull(extendedSchema.getExample());
        assertNull(extendedSchema.getDefault());

        if (extendedSchema.getProperties() != null) {
            Schema nameProperty = (Schema) extendedSchema.getProperties().get("name");
            if (nameProperty != null) {
                assertNull(nameProperty.getExample());
                assertNull(nameProperty.getDefault());
            }
        }
    }

    @Test
    public void testParameterWithNullExampleAndDefault() {
        String yaml = "openapi: 3.0.0\n" +
                "info:\n" +
                "  title: Test\n" +
                "  version: 1.0.0\n" +
                "paths:\n" +
                "  /test:\n" +
                "    get:\n" +
                "      parameters:\n" +
                "        - name: testParam\n" +
                "          in: query\n" +
                "          schema:\n" +
                "            type: string\n" +
                "            example: null\n" +
                "            default: null\n" +
                "        - $ref: '#/components/parameters/RefParam'\n" +
                "      responses:\n" +
                "        '200':\n" +
                "          description: Success\n" +
                "components:\n" +
                "  parameters:\n" +
                "    RefParam:\n" +
                "      name: refParam\n" +
                "      in: query\n" +
                "      schema:\n" +
                "        type: integer\n" +
                "        example: null\n" +
                "        default: null\n";

        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setResolveFully(true);

        SwaggerParseResult result = new OpenAPIV3Parser().readContents(yaml, null, options);
        OpenAPI openAPI = result.getOpenAPI();

        assertNotNull(openAPI);
        List<Parameter> parameters = openAPI.getPaths().get("/test").getGet().getParameters();
        assertNotNull(parameters);
        assertEquals(2, parameters.size());

        Parameter testParam = parameters.get(0);
        assertNotNull(testParam.getSchema());
        assertNull(testParam.getSchema().getExample());
        assertNull(testParam.getSchema().getDefault());

        Parameter refParam = parameters.get(1);
        assertNotNull(refParam.getSchema());
        assertNull(refParam.getSchema().getExample());
        assertNull(refParam.getSchema().getDefault());
    }


    @Test
    public void testNullDefaultNotPropagatedInAllOf() {
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
                "        id:\n" +
                "          type: string\n" +
                "    ExtendedSchema:\n" +
                "      allOf:\n" +
                "        - $ref: '#/components/schemas/BaseSchema'\n" +
                "        - type: object\n" +
                "          properties:\n" +
                "            name:\n" +
                "              type: string\n";

        ParseOptions options = new ParseOptions();
        options.setResolveFully(true);
        options.setResolveCombinators(true);

        SwaggerParseResult result = new OpenAPIV3Parser().readContents(yaml, null, options);
        OpenAPI openAPI = result.getOpenAPI();

        assertNotNull(openAPI);
        Schema extendedSchema = openAPI.getComponents().getSchemas().get("ExtendedSchema");
        assertNotNull(extendedSchema);

        assertNull(extendedSchema.getDefault());


        if (extendedSchema.getProperties() != null) {
            Schema idProperty = (Schema) extendedSchema.getProperties().get("id");
            Schema nameProperty = (Schema) extendedSchema.getProperties().get("name");
            if (idProperty != null) {
                assertNull(idProperty.getDefault());
            }
            if (nameProperty != null) {
                assertNull(nameProperty.getDefault());
            }
        }

    }


    @Test
    public void testSourceDefaultWhenResolvedIsNull() {
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
                "        count:\n" +
                "          type: integer\n" +
                "    ExtendedSchema:\n" +
                "      allOf:\n" +
                "        - $ref: '#/components/schemas/BaseSchema'\n" +
                "      default:\n" +
                "        count: 10\n";

        ParseOptions options = new ParseOptions();
        options.setResolveFully(true);
        options.setResolveCombinators(true);

        SwaggerParseResult result = new OpenAPIV3Parser().readContents(yaml, null, options);
        OpenAPI openAPI = result.getOpenAPI();

        assertNotNull(openAPI);
        Schema extendedSchema = openAPI.getComponents().getSchemas().get("ExtendedSchema");
        assertNotNull(extendedSchema);

        assertNotNull(extendedSchema.getDefault());
    }

    @Test
    public void testOneOfNullDefaultNotPropagated() {
        String yaml = "openapi: 3.0.0\n" +
                "info:\n" +
                "  title: Test\n" +
                "  version: 1.0.0\n" +
                "paths: {}\n" +
                "components:\n" +
                "  schemas:\n" +
                "    StringSchema:\n" +
                "      type: string\n" +
                "    NumberSchema:\n" +
                "      type: number\n" +
                "    UnionSchema:\n" +
                "      oneOf:\n" +
                "        - $ref: '#/components/schemas/StringSchema'\n" +
                "        - $ref: '#/components/schemas/NumberSchema'\n";

        ParseOptions options = new ParseOptions();
        options.setResolveFully(true);
        options.setResolveCombinators(false);

        SwaggerParseResult result = new OpenAPIV3Parser().readContents(yaml, null, options);
        OpenAPI openAPI = result.getOpenAPI();

        assertNotNull(openAPI);
        Schema unionSchema = openAPI.getComponents().getSchemas().get("UnionSchema");
        assertNotNull(unionSchema);

        assertNull(unionSchema.getDefault());
    }

    @Test
    public void testAnyOfNonNullDefaultPreserved() {
        String yaml = "openapi: 3.0.0\n" +
                "info:\n" +
                "  title: Test\n" +
                "  version: 1.0.0\n" +
                "paths: {}\n" +
                "components:\n" +
                "  schemas:\n" +
                "    Schema1:\n" +
                "      type: object\n" +
                "      properties:\n" +
                "        prop1:\n" +
                "          type: string\n" +
                "    Schema2:\n" +
                "      type: object\n" +
                "      properties:\n" +
                "        prop2:\n" +
                "          type: integer\n" +
                "    CombinedSchema:\n" +
                "      anyOf:\n" +
                "        - $ref: '#/components/schemas/Schema1'\n" +
                "        - $ref: '#/components/schemas/Schema2'\n" +
                "      default:\n" +
                "        prop1: 'test'\n";

        ParseOptions options = new ParseOptions();
        options.setResolveFully(true);
        options.setResolveCombinators(false);

        SwaggerParseResult result = new OpenAPIV3Parser().readContents(yaml, null, options);
        OpenAPI openAPI = result.getOpenAPI();

        assertNotNull(openAPI);
        Schema combinedSchema = openAPI.getComponents().getSchemas().get("CombinedSchema");
        assertNotNull(combinedSchema);

        assertNotNull(combinedSchema.getDefault());
    }
}
