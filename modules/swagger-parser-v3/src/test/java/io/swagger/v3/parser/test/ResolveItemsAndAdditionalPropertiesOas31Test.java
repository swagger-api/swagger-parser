package io.swagger.v3.parser.test;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class ResolveItemsAndAdditionalPropertiesOas31Test {

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
    public void arrayItemsWithAllOfMergeForOas31() {
        // The outer schema has no combinator; its nested `items` schema does. OAS 3.1 schemas
        // must therefore recurse into `items` instead of relying only on shape-specific subclasses.
        String yaml = "openapi: 3.1.0\n" +
                "info:\n" +
                "  title: Test\n" +
                "  version: 1.0.0\n" +
                "paths:\n" +
                "  /example:\n" +
                "    post:\n" +
                "      requestBody:\n" +
                "        content:\n" +
                "          application/json:\n" +
                "            schema:\n" +
                "              type: array\n" +
                "              items:\n" +
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
        assertNotNull(openAPI);

        Schema arraySchema = requestBodySchema(openAPI);
        Schema items = arraySchema.getItems();
        assertNotNull(items, "array items should be resolved for OAS 3.1");
        assertNull(items.getAllOf(), "allOf inside array items should be merged for OAS 3.1");
        assertNotNull(items.getProperties());
        assertTrue(items.getProperties().containsKey("a"));
        assertTrue(items.getProperties().containsKey("b"));
    }

    @Test
    public void arrayItemsWithRefResolveForOas31() {
        String yaml = "openapi: 3.1.0\n" +
                "info:\n" +
                "  title: Test\n" +
                "  version: 1.0.0\n" +
                "paths:\n" +
                "  /example:\n" +
                "    post:\n" +
                "      requestBody:\n" +
                "        content:\n" +
                "          application/json:\n" +
                "            schema:\n" +
                "              type: array\n" +
                "              items:\n" +
                "                $ref: '#/components/schemas/Item'\n" +
                "      responses:\n" +
                "        '200':\n" +
                "          description: OK\n" +
                "components:\n" +
                "  schemas:\n" +
                "    Item:\n" +
                "      type: object\n" +
                "      properties:\n" +
                "        a:\n" +
                "          type: string\n";

        OpenAPI openAPI = new OpenAPIV3Parser().readContents(yaml, null, resolveCombinatorsOptions()).getOpenAPI();
        assertNotNull(openAPI);

        Schema arraySchema = requestBodySchema(openAPI);
        Schema items = arraySchema.getItems();
        assertNotNull(items);
        assertNull(items.get$ref(), "array items $ref should be resolved for OAS 3.1");
        assertNotNull(items.getProperties());
        assertTrue(items.getProperties().containsKey("a"));
    }

    @Test
    public void arrayAndObjectKeywordsAreBothResolvedForOas31() {
        // OAS 3.1 permits array- and object-specific keywords on the same schema. With no `type`,
        // this schema can describe either an array or an object. Resolving `items` must not return
        // early and skip `properties`.
        String yaml = "openapi: 3.1.0\n" +
                "info:\n" +
                "  title: Test\n" +
                "  version: 1.0.0\n" +
                "paths:\n" +
                "  /example:\n" +
                "    post:\n" +
                "      requestBody:\n" +
                "        content:\n" +
                "          application/json:\n" +
                "            schema:\n" +
                "              type:\n" +
                "                - array\n" +
                "                - object\n" +
                "              items:\n" +
                "                type: string\n" +
                "              properties:\n" +
                "                a:\n" +
                "                  type: object\n" +
                "                  allOf:\n" +
                "                    - type: object\n" +
                "                      properties:\n" +
                "                        p:\n" +
                "                          type: string\n" +
                "                    - type: object\n" +
                "                      properties:\n" +
                "                        q:\n" +
                "                          type: string\n" +
                "      responses:\n" +
                "        '200':\n" +
                "          description: OK\n";

        OpenAPI openAPI = new OpenAPIV3Parser().readContents(yaml, null, resolveCombinatorsOptions()).getOpenAPI();
        assertNotNull(openAPI);

        Schema schema = requestBodySchema(openAPI);
        assertNotNull(schema.getItems(), "items should still be resolved");
        assertNotNull(schema.getProperties(), "properties should also be resolved, not skipped");

        Schema a = (Schema) schema.getProperties().get("a");
        assertNotNull(a);
        assertNull(a.getAllOf(), "allOf on a property sibling to items should still be merged for OAS 3.1");
        assertNotNull(a.getProperties());
        assertTrue(a.getProperties().containsKey("p"));
        assertTrue(a.getProperties().containsKey("q"));
    }

    @Test
    public void additionalPropertiesWithAllOfMergeForOas31() {
        String yaml = "openapi: 3.1.0\n" +
                "info:\n" +
                "  title: Test\n" +
                "  version: 1.0.0\n" +
                "paths:\n" +
                "  /example:\n" +
                "    post:\n" +
                "      requestBody:\n" +
                "        content:\n" +
                "          application/json:\n" +
                "            schema:\n" +
                "              type: object\n" +
                "              additionalProperties:\n" +
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
        assertNotNull(openAPI);

        Schema mapSchema = requestBodySchema(openAPI);
        Object additionalProperties = mapSchema.getAdditionalProperties();
        assertTrue(additionalProperties instanceof Schema, "additionalProperties should be resolved for OAS 3.1");
        Schema additionalPropertiesSchema = (Schema) additionalProperties;
        assertNull(additionalPropertiesSchema.getAllOf(), "allOf inside additionalProperties should be merged for OAS 3.1");
        assertNotNull(additionalPropertiesSchema.getProperties());
        assertTrue(additionalPropertiesSchema.getProperties().containsKey("a"));
        assertTrue(additionalPropertiesSchema.getProperties().containsKey("b"));
    }

    @Test
    public void additionalPropertiesWithRefResolveForOas31() {
        String yaml = "openapi: 3.1.0\n" +
                "info:\n" +
                "  title: Test\n" +
                "  version: 1.0.0\n" +
                "paths:\n" +
                "  /example:\n" +
                "    post:\n" +
                "      requestBody:\n" +
                "        content:\n" +
                "          application/json:\n" +
                "            schema:\n" +
                "              type: object\n" +
                "              additionalProperties:\n" +
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
        assertNotNull(openAPI);

        Schema mapSchema = requestBodySchema(openAPI);
        Object additionalProperties = mapSchema.getAdditionalProperties();
        assertTrue(additionalProperties instanceof Schema);
        Schema additionalPropertiesSchema = (Schema) additionalProperties;
        assertNull(additionalPropertiesSchema.get$ref(), "additionalProperties $ref should be resolved for OAS 3.1");
        assertNotNull(additionalPropertiesSchema.getProperties());
        assertTrue(additionalPropertiesSchema.getProperties().containsKey("y"));
    }
}
