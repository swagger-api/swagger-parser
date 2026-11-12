package io.swagger.v3.parser.test;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import org.testng.annotations.Test;

import java.util.Map;

import static org.testng.Assert.*;

public class ResolveOas31OnlySchemaFieldsTest {

    private ParseOptions resolveCombinatorsOptions() {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setResolveFully(true);
        options.setResolveCombinators(true);
        return options;
    }

    private Schema requestBodySchema(OpenAPI openAPI) {
        return openAPI.getPaths().get("/example").getPost()
                .getRequestBody().getContent().get("application/json").getSchema();
    }

    @Test
    public void notResolvesAndMergesForOas30() {
        String yaml = "openapi: 3.0.0\n" +
                "info:\n  title: Test\n  version: 1.0.0\n" +
                "paths:\n" +
                "  /example:\n" +
                "    post:\n" +
                "      requestBody:\n" +
                "        content:\n" +
                "          application/json:\n" +
                "            schema:\n" +
                "              not:\n" +
                "                type: object\n" +
                "                allOf:\n" +
                "                  - type: object\n" +
                "                    properties:\n" +
                "                      a:\n" +
                "                        type: string\n" +
                "                  - type: object\n" +
                "                    properties:\n" +
                "                      b:\n" +
                "                        type: string\n" +
                "      responses:\n" +
                "        '200':\n" +
                "          description: OK\n";

        OpenAPI openAPI = new OpenAPIV3Parser().readContents(yaml, null, resolveCombinatorsOptions()).getOpenAPI();
        Schema schema = requestBodySchema(openAPI);
        assertNotNull(schema.getNot(), "not should be resolved (it should not itself disappear)");
        assertNull(schema.getNot().getAllOf());
        assertNotNull(schema.getNot().getProperties());
        assertTrue(schema.getNot().getProperties().containsKey("a"));
        assertTrue(schema.getNot().getProperties().containsKey("b"));
    }

    @Test
    public void notResolvesAndMergesForOas31() {
        String yaml = "openapi: 3.1.0\n" +
                "info:\n  title: Test\n  version: 1.0.0\n" +
                "paths:\n" +
                "  /example:\n" +
                "    post:\n" +
                "      requestBody:\n" +
                "        content:\n" +
                "          application/json:\n" +
                "            schema:\n" +
                "              not:\n" +
                "                type: object\n" +
                "                allOf:\n" +
                "                  - type: object\n" +
                "                    properties:\n" +
                "                      a:\n" +
                "                        type: string\n" +
                "                  - type: object\n" +
                "                    properties:\n" +
                "                      b:\n" +
                "                        type: string\n" +
                "      responses:\n" +
                "        '200':\n" +
                "          description: OK\n";

        OpenAPI openAPI = new OpenAPIV3Parser().readContents(yaml, null, resolveCombinatorsOptions()).getOpenAPI();
        Schema schema = requestBodySchema(openAPI);
        assertNotNull(schema.getNot());
        assertNull(schema.getNot().getAllOf());
        assertNotNull(schema.getNot().getProperties());
        assertTrue(schema.getNot().getProperties().containsKey("a"));
        assertTrue(schema.getNot().getProperties().containsKey("b"));
    }

    @Test
    public void ifThenElseResolveAndMergeForOas31() {
        String yaml = "openapi: 3.1.0\n" +
                "info:\n  title: Test\n  version: 1.0.0\n" +
                "paths:\n" +
                "  /example:\n" +
                "    post:\n" +
                "      requestBody:\n" +
                "        content:\n" +
                "          application/json:\n" +
                "            schema:\n" +
                "              if:\n" +
                "                $ref: '#/components/schemas/Cond'\n" +
                "              then:\n" +
                "                type: object\n" +
                "                allOf:\n" +
                "                  - type: object\n" +
                "                    properties:\n" +
                "                      a:\n" +
                "                        type: string\n" +
                "                  - type: object\n" +
                "                    properties:\n" +
                "                      b:\n" +
                "                        type: string\n" +
                "              else:\n" +
                "                type: object\n" +
                "                allOf:\n" +
                "                  - type: object\n" +
                "                    properties:\n" +
                "                      x:\n" +
                "                        type: string\n" +
                "                  - type: object\n" +
                "                    properties:\n" +
                "                      y:\n" +
                "                        type: string\n" +
                "      responses:\n" +
                "        '200':\n" +
                "          description: OK\n" +
                "components:\n" +
                "  schemas:\n" +
                "    Cond:\n" +
                "      type: object\n" +
                "      properties:\n" +
                "        c:\n" +
                "          type: string\n";

        OpenAPI openAPI = new OpenAPIV3Parser().readContents(yaml, null, resolveCombinatorsOptions()).getOpenAPI();
        Schema schema = requestBodySchema(openAPI);

        assertNotNull(schema.getIf());
        assertNull(schema.getIf().get$ref(), "if's $ref should be resolved");
        assertNotNull(schema.getIf().getProperties());
        assertTrue(schema.getIf().getProperties().containsKey("c"));

        assertNull(schema.getThen().getAllOf());
        assertNotNull(schema.getThen().getProperties());
        assertTrue(schema.getThen().getProperties().containsKey("a"));
        assertTrue(schema.getThen().getProperties().containsKey("b"));
        assertNull(schema.getElse().getAllOf());
        assertNotNull(schema.getElse().getProperties());
        assertTrue(schema.getElse().getProperties().containsKey("x"));
        assertTrue(schema.getElse().getProperties().containsKey("y"));
    }

    @Test
    public void containsResolvesAndMergesForOas31() {
        String yaml = "openapi: 3.1.0\n" +
                "info:\n  title: Test\n  version: 1.0.0\n" +
                "paths:\n" +
                "  /example:\n" +
                "    post:\n" +
                "      requestBody:\n" +
                "        content:\n" +
                "          application/json:\n" +
                "            schema:\n" +
                "              type: array\n" +
                "              contains:\n" +
                "                type: object\n" +
                "                allOf:\n" +
                "                  - type: object\n" +
                "                    properties:\n" +
                "                      a:\n" +
                "                        type: string\n" +
                "                  - type: object\n" +
                "                    properties:\n" +
                "                      b:\n" +
                "                        type: string\n" +
                "      responses:\n" +
                "        '200':\n" +
                "          description: OK\n";

        OpenAPI openAPI = new OpenAPIV3Parser().readContents(yaml, null, resolveCombinatorsOptions()).getOpenAPI();
        Schema schema = requestBodySchema(openAPI);
        assertNotNull(schema.getContains());
        assertNull(schema.getContains().getAllOf());
        assertNotNull(schema.getContains().getProperties());
        assertTrue(schema.getContains().getProperties().containsKey("a"));
        assertTrue(schema.getContains().getProperties().containsKey("b"));
    }

    @Test
    public void propertyNamesAndContentSchemaResolveForOas31() {
        String yaml = "openapi: 3.1.0\n" +
                "info:\n  title: Test\n  version: 1.0.0\n" +
                "paths:\n" +
                "  /example:\n" +
                "    post:\n" +
                "      requestBody:\n" +
                "        content:\n" +
                "          application/json:\n" +
                "            schema:\n" +
                "              type: object\n" +
                "              propertyNames:\n" +
                "                allOf:\n" +
                "                  - type: string\n" +
                "                    minLength: 1\n" +
                "                  - type: string\n" +
                "                    pattern: '^[a-z]'\n" +
                "              contentSchema:\n" +
                "                $ref: '#/components/schemas/Name'\n" +
                "      responses:\n" +
                "        '200':\n" +
                "          description: OK\n" +
                "components:\n" +
                "  schemas:\n" +
                "    Name:\n" +
                "      type: string\n" +
                "      minLength: 1\n";

        OpenAPI openAPI = new OpenAPIV3Parser().readContents(yaml, null, resolveCombinatorsOptions()).getOpenAPI();
        Schema schema = requestBodySchema(openAPI);

        assertNotNull(schema.getPropertyNames());
        assertNull(schema.getPropertyNames().getAllOf(), "propertyNames allOf should be merged");
        assertEquals(schema.getPropertyNames().getMinLength(), Integer.valueOf(1));
        assertEquals(schema.getPropertyNames().getPattern(), "^[a-z]");

        assertNotNull(schema.getContentSchema());
        assertNull(schema.getContentSchema().get$ref(), "contentSchema $ref should be resolved");
        assertEquals(schema.getContentSchema().getMinLength(), Integer.valueOf(1));
    }

    @Test
    public void unevaluatedPropertiesAndUnevaluatedItemsResolveForOas31() {
        String yaml = "openapi: 3.1.0\n" +
                "info:\n  title: Test\n  version: 1.0.0\n" +
                "paths:\n" +
                "  /example:\n" +
                "    post:\n" +
                "      requestBody:\n" +
                "        content:\n" +
                "          application/json:\n" +
                "            schema:\n" +
                "              type: object\n" +
                "              unevaluatedProperties:\n" +
                "                allOf:\n" +
                "                  - type: object\n" +
                "                    properties:\n" +
                "                      y:\n" +
                "                        type: string\n" +
                "                  - type: object\n" +
                "                    properties:\n" +
                "                      z:\n" +
                "                        type: string\n" +
                "              unevaluatedItems:\n" +
                "                $ref: '#/components/schemas/Extra'\n" +
                "      responses:\n" +
                "        '200':\n" +
                "          description: OK\n" +
                "components:\n" +
                "  schemas:\n" +
                "    Extra:\n" +
                "      type: object\n" +
                "      properties:\n" +
                "        y:\n" +
                "          type: string\n";

        OpenAPI openAPI = new OpenAPIV3Parser().readContents(yaml, null, resolveCombinatorsOptions()).getOpenAPI();
        Schema schema = requestBodySchema(openAPI);

        assertNotNull(schema.getUnevaluatedProperties());
        assertNull(schema.getUnevaluatedProperties().getAllOf());
        assertTrue(schema.getUnevaluatedProperties().getProperties().containsKey("y"));
        assertTrue(schema.getUnevaluatedProperties().getProperties().containsKey("z"));

        assertNotNull(schema.getUnevaluatedItems());
        assertNull(schema.getUnevaluatedItems().get$ref());
        assertTrue(schema.getUnevaluatedItems().getProperties().containsKey("y"));
    }

    // No parsed-input test for additionalItems: OpenAPI 3.1 uses JSON Schema 2020-12,
    // where additionalItems is not a recognized keyword. OpenAPIDeserializer therefore
    // stores it as an extension instead of Schema.additionalItems. Programmatically
    // populated additionalItems is handled separately.

    @Test
    public void prefixItemsResolveAndMergeForOas31() {
        String yaml = "openapi: 3.1.0\n" +
                "info:\n  title: Test\n  version: 1.0.0\n" +
                "paths:\n" +
                "  /example:\n" +
                "    post:\n" +
                "      requestBody:\n" +
                "        content:\n" +
                "          application/json:\n" +
                "            schema:\n" +
                "              type: array\n" +
                "              prefixItems:\n" +
                "                - $ref: '#/components/schemas/First'\n" +
                "                - type: object\n" +
                "                  allOf:\n" +
                "                    - type: object\n" +
                "                      properties:\n" +
                "                        a:\n" +
                "                          type: string\n" +
                "                    - type: object\n" +
                "                      properties:\n" +
                "                        b:\n" +
                "                          type: string\n" +
                "      responses:\n" +
                "        '200':\n" +
                "          description: OK\n" +
                "components:\n" +
                "  schemas:\n" +
                "    First:\n" +
                "      type: string\n";

        OpenAPI openAPI = new OpenAPIV3Parser().readContents(yaml, null, resolveCombinatorsOptions()).getOpenAPI();
        Schema schema = requestBodySchema(openAPI);

        assertNotNull(schema.getPrefixItems());
        assertEquals(schema.getPrefixItems().size(), 2);
        Schema first = (Schema) schema.getPrefixItems().get(0);
        assertNull(first.get$ref(), "prefixItems[0] $ref should be resolved");

        Schema second = (Schema) schema.getPrefixItems().get(1);
        assertNull(second.getAllOf());
        assertNotNull(second.getProperties());
        assertTrue(second.getProperties().containsKey("a"));
        assertTrue(second.getProperties().containsKey("b"));
    }

    @Test
    public void patternPropertiesAndDependentSchemasResolveAndMergeForOas31() {
        String yaml = "openapi: 3.1.0\n" +
                "info:\n  title: Test\n  version: 1.0.0\n" +
                "paths:\n" +
                "  /example:\n" +
                "    post:\n" +
                "      requestBody:\n" +
                "        content:\n" +
                "          application/json:\n" +
                "            schema:\n" +
                "              type: object\n" +
                "              patternProperties:\n" +
                "                '^x-':\n" +
                "                  type: object\n" +
                "                  allOf:\n" +
                "                    - type: object\n" +
                "                      properties:\n" +
                "                        a:\n" +
                "                          type: string\n" +
                "                    - type: object\n" +
                "                      properties:\n" +
                "                        b:\n" +
                "                          type: string\n" +
                "              dependentSchemas:\n" +
                "                foo:\n" +
                "                  allOf:\n" +
                "                    - type: object\n" +
                "                      properties:\n" +
                "                        y:\n" +
                "                          type: string\n" +
                "                    - type: object\n" +
                "                      properties:\n" +
                "                        z:\n" +
                "                          type: string\n" +
                "      responses:\n" +
                "        '200':\n" +
                "          description: OK\n";

        OpenAPI openAPI = new OpenAPIV3Parser().readContents(yaml, null, resolveCombinatorsOptions()).getOpenAPI();
        Schema schema = requestBodySchema(openAPI);

        assertNotNull(schema.getPatternProperties());
        Map<String, Schema> patternProperties = schema.getPatternProperties();
        Schema patternProp = patternProperties.get("^x-");
        assertNull(patternProp.getAllOf());
        assertNotNull(patternProp.getProperties());
        assertTrue(patternProp.getProperties().containsKey("a"));
        assertTrue(patternProp.getProperties().containsKey("b"));

        assertNotNull(schema.getDependentSchemas());
        Map<String, Schema> dependentSchemas = schema.getDependentSchemas();
        Schema fooDependent = dependentSchemas.get("foo");
        assertNotNull(fooDependent);
        assertNull(fooDependent.getAllOf(), "dependentSchemas allOf should be merged");
        assertTrue(fooDependent.getProperties().containsKey("y"));
        assertTrue(fooDependent.getProperties().containsKey("z"));
    }
}
