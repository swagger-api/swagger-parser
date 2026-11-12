package io.swagger.v3.parser.test;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.SpecVersion;
import io.swagger.v3.oas.models.media.JsonSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class ResolveCombinatorsOas31Test {

    private ParseOptions resolveCombinatorsOptions() {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setResolveFully(true);
        options.setResolveCombinators(true);
        return options;
    }

    private OpenAPI parse(String yaml) {
        return parse(yaml, resolveCombinatorsOptions());
    }

    private OpenAPI parse(String yaml, ParseOptions options) {
        OpenAPI openAPI = new OpenAPIV3Parser().readContents(yaml, null, options).getOpenAPI();
        assertNotNull(openAPI);
        return openAPI;
    }

    private Schema requestBodySchema(OpenAPI openAPI) {
        return openAPI.getPaths().get("/example").getPost()
                .getRequestBody().getContent().get("application/json").getSchema();
    }

    private Schema responseSchema(OpenAPI openAPI) {
        return openAPI.getPaths().get("/example").getGet()
                .getResponses().get("200").getContent().get("application/json").getSchema();
    }

    @Test
    public void allOfMergesForOas30() {
        String yaml = "openapi: 3.0.0\n" +
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
                "              allOf:\n" +
                "                - type: object\n" +
                "                  properties:\n" +
                "                    a:\n" +
                "                      type: string\n" +
                "                - type: object\n" +
                "                  properties:\n" +
                "                    b:\n" +
                "                      type: string\n" +
                "      responses:\n" +
                "        '200':\n" +
                "          description: OK\n";

        OpenAPI openAPI = parse(yaml);

        Schema combined = requestBodySchema(openAPI);
        assertNull(combined.getAllOf(), "allOf should be merged away for OAS 3.0");
        assertNotNull(combined.getProperties());
        assertTrue(combined.getProperties().containsKey("a"));
        assertTrue(combined.getProperties().containsKey("b"));
    }

    @Test
    public void allOfMergesForOas31() {
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
                "              allOf:\n" +
                "                - type: object\n" +
                "                  properties:\n" +
                "                    a:\n" +
                "                      type: string\n" +
                "                - type: object\n" +
                "                  properties:\n" +
                "                    b:\n" +
                "                      type: string\n" +
                "      responses:\n" +
                "        '200':\n" +
                "          description: OK\n";

        OpenAPI openAPI = parse(yaml);

        Schema combined = requestBodySchema(openAPI);
        assertNull(combined.getAllOf(), "allOf should be merged away for OAS 3.1, same as OAS 3.0");
        assertNotNull(combined.getProperties());
        assertTrue(combined.getProperties().containsKey("a"));
        assertTrue(combined.getProperties().containsKey("b"));
    }

    @Test
    public void responseSchemaAllOfMergesForOas31() {
        String yaml = "openapi: 3.1.0\n" +
                "info:\n" +
                "  title: Test\n" +
                "  version: 1.0.0\n" +
                "paths:\n" +
                "  /example:\n" +
                "    get:\n" +
                "      responses:\n" +
                "        '200':\n" +
                "          description: OK\n" +
                "          content:\n" +
                "            application/json:\n" +
                "              schema:\n" +
                "                type: object\n" +
                "                allOf:\n" +
                "                  - type: object\n" +
                "                    properties:\n" +
                "                      id:\n" +
                "                        type: string\n" +
                "                  - type: object\n" +
                "                    properties:\n" +
                "                      name:\n" +
                "                        type: string\n";

        Schema combined = responseSchema(parse(yaml));
        assertNull(combined.getAllOf(), "response allOf should be merged for OAS 3.1");
        assertNotNull(combined.getProperties());
        assertTrue(combined.getProperties().containsKey("id"));
        assertTrue(combined.getProperties().containsKey("name"));
    }

    // Adjacent combinators are intentionally aggregated here to match existing OAS 3.0 ResolverFully behavior.
    @Test
    public void oneOfMergesForOas31WhenAdjacentToAllOf() {
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
                "              allOf:\n" +
                "                - type: object\n" +
                "                  properties:\n" +
                "                    a:\n" +
                "                      type: string\n" +
                "              oneOf:\n" +
                "                - type: object\n" +
                "                  properties:\n" +
                "                    b:\n" +
                "                      type: string\n" +
                "                - type: object\n" +
                "                  properties:\n" +
                "                    c:\n" +
                "                      type: string\n" +
                "      responses:\n" +
                "        '200':\n" +
                "          description: OK\n";

        OpenAPI openAPI = parse(yaml);

        Schema combined = requestBodySchema(openAPI);
        assertNull(combined.getAllOf(), "allOf adjacent to oneOf should be merged for OAS 3.1");
        assertNull(combined.getOneOf(), "oneOf adjacent to allOf should be merged for OAS 3.1");
        assertNotNull(combined.getProperties());
        assertTrue(combined.getProperties().containsKey("a"));
        assertTrue(combined.getProperties().containsKey("b"));
        assertTrue(combined.getProperties().containsKey("c"));
    }

    @Test
    public void anyOfMergesForOas31WhenAdjacentToAllOf() {
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
                "              allOf:\n" +
                "                - type: object\n" +
                "                  properties:\n" +
                "                    a:\n" +
                "                      type: string\n" +
                "              anyOf:\n" +
                "                - type: object\n" +
                "                  properties:\n" +
                "                    b:\n" +
                "                      type: string\n" +
                "                - type: object\n" +
                "                  properties:\n" +
                "                    c:\n" +
                "                      type: string\n" +
                "      responses:\n" +
                "        '200':\n" +
                "          description: OK\n";

        OpenAPI openAPI = parse(yaml);

        Schema combined = requestBodySchema(openAPI);
        assertNull(combined.getAllOf(), "allOf adjacent to anyOf should be merged for OAS 3.1");
        assertNull(combined.getAnyOf(), "anyOf adjacent to allOf should be merged for OAS 3.1");
        assertNotNull(combined.getProperties());
        assertTrue(combined.getProperties().containsKey("a"));
        assertTrue(combined.getProperties().containsKey("b"));
        assertTrue(combined.getProperties().containsKey("c"));
    }

    @Test
    public void refsInsideOas31AllOfAreDereferencedEvenWhenNotAggregated() {
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
                "              allOf:\n" +
                "                - $ref: '#/components/schemas/SchemaA'\n" +
                "                - $ref: '#/components/schemas/SchemaB'\n" +
                "      responses:\n" +
                "        '200':\n" +
                "          description: OK\n" +
                "components:\n" +
                "  schemas:\n" +
                "    SchemaA:\n" +
                "      type: object\n" +
                "      properties:\n" +
                "        a:\n" +
                "          type: string\n" +
                "    SchemaB:\n" +
                "      type: object\n" +
                "      properties:\n" +
                "        b:\n" +
                "          type: string\n";

        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setResolveFully(true);
        options.setResolveCombinators(false);

        OpenAPI openAPI = parse(yaml, options);

        Schema combined = requestBodySchema(openAPI);
        assertNotNull(combined.getAllOf(), "allOf should be left in place when combinator aggregation is disabled");
        assertEquals(combined.getAllOf().size(), 2);

        Schema branchA = (Schema) combined.getAllOf().get(0);
        Schema branchB = (Schema) combined.getAllOf().get(1);
        assertNull(branchA.get$ref(), "allOf branch should be dereferenced, not left as a $ref");
        assertNull(branchB.get$ref(), "allOf branch should be dereferenced, not left as a $ref");
        assertNotNull(branchA.getProperties());
        assertTrue(branchA.getProperties().containsKey("a"));
        assertNotNull(branchB.getProperties());
        assertTrue(branchB.getProperties().containsKey("b"));
    }

    @Test
    public void oneOfAndAnyOfMergeForOas31WhenAdjacentWithoutAllOf() {
        // "adjacent" combinators trigger merging even without allOf being present at all.
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
                "              oneOf:\n" +
                "                - type: object\n" +
                "                  properties:\n" +
                "                    a:\n" +
                "                      type: string\n" +
                "              anyOf:\n" +
                "                - type: object\n" +
                "                  properties:\n" +
                "                    b:\n" +
                "                      type: string\n" +
                "      responses:\n" +
                "        '200':\n" +
                "          description: OK\n";

        OpenAPI openAPI = parse(yaml);

        Schema combined = requestBodySchema(openAPI);
        assertNull(combined.getOneOf(), "oneOf adjacent to anyOf (no allOf) should be merged for OAS 3.1");
        assertNull(combined.getAnyOf(), "anyOf adjacent to oneOf (no allOf) should be merged for OAS 3.1");
        assertNotNull(combined.getProperties());
        assertTrue(combined.getProperties().containsKey("a"));
        assertTrue(combined.getProperties().containsKey("b"));
    }

    @Test
    public void allThreeCombinatorsMergeForOas31() {
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
                "              allOf:\n" +
                "                - type: object\n" +
                "                  properties:\n" +
                "                    a:\n" +
                "                      type: string\n" +
                "              oneOf:\n" +
                "                - type: object\n" +
                "                  properties:\n" +
                "                    b:\n" +
                "                      type: string\n" +
                "              anyOf:\n" +
                "                - type: object\n" +
                "                  properties:\n" +
                "                    c:\n" +
                "                      type: string\n" +
                "      responses:\n" +
                "        '200':\n" +
                "          description: OK\n";

        OpenAPI openAPI = parse(yaml);

        Schema combined = requestBodySchema(openAPI);
        assertNull(combined.getAllOf());
        assertNull(combined.getOneOf());
        assertNull(combined.getAnyOf());
        assertNotNull(combined.getProperties());
        assertTrue(combined.getProperties().containsKey("a"));
        assertTrue(combined.getProperties().containsKey("b"));
        assertTrue(combined.getProperties().containsKey("c"));
    }

    @Test
    public void standaloneOneOfDoesNotMergeForOas31() {
        // A lone oneOf (no allOf, no anyOf) is not "adjacent" and should be left as-is,
        // matching OAS 3.0 behavior (a discriminated union should not be flattened away).
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
                "              oneOf:\n" +
                "                - type: object\n" +
                "                  properties:\n" +
                "                    a:\n" +
                "                      type: string\n" +
                "                - type: object\n" +
                "                  properties:\n" +
                "                    b:\n" +
                "                      type: string\n" +
                "      responses:\n" +
                "        '200':\n" +
                "          description: OK\n";

        OpenAPI openAPI = parse(yaml);

        Schema combined = requestBodySchema(openAPI);
        assertNotNull(combined.getOneOf(), "standalone oneOf should not be merged for OAS 3.1, same as OAS 3.0");
        assertEquals(combined.getOneOf().size(), 2);
    }

    @Test
    public void standaloneAnyOfDoesNotMergeForOas31() {
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
                "              anyOf:\n" +
                "                - type: object\n" +
                "                  properties:\n" +
                "                    a:\n" +
                "                      type: string\n" +
                "                - type: object\n" +
                "                  properties:\n" +
                "                    b:\n" +
                "                      type: string\n" +
                "      responses:\n" +
                "        '200':\n" +
                "          description: OK\n";

        OpenAPI openAPI = parse(yaml);

        Schema combined = requestBodySchema(openAPI);
        assertNotNull(combined.getAnyOf(), "standalone anyOf should not be merged for OAS 3.1, same as OAS 3.0");
        assertEquals(combined.getAnyOf().size(), 2);
    }

    @Test
    public void nestedAllOfInsideAllOfBranchMergesRecursivelyForOas31() {
        // A branch of the outer allOf itself has a nested allOf - aggregateSchemaCombinators
        // recurses to flatten it too, bottom-up.
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
                "              allOf:\n" +
                "                - type: object\n" +
                "                  properties:\n" +
                "                    a:\n" +
                "                      type: string\n" +
                "                - type: object\n" +
                "                  allOf:\n" +
                "                    - type: object\n" +
                "                      properties:\n" +
                "                        b:\n" +
                "                          type: string\n" +
                "                    - type: object\n" +
                "                      properties:\n" +
                "                        c:\n" +
                "                          type: string\n" +
                "      responses:\n" +
                "        '200':\n" +
                "          description: OK\n";

        OpenAPI openAPI = parse(yaml);

        Schema combined = requestBodySchema(openAPI);
        assertNull(combined.getAllOf(), "nested allOf should be merged away recursively for OAS 3.1");
        assertNotNull(combined.getProperties());
        assertTrue(combined.getProperties().containsKey("a"));
        assertTrue(combined.getProperties().containsKey("b"));
        assertTrue(combined.getProperties().containsKey("c"));
    }

    @Test
    public void allOfMergeForOas31AggregatesRequiredDefaultAndExtensions() {
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
                "              x-outer: outer-value\n" +
                "              allOf:\n" +
                "                - type: object\n" +
                "                  required:\n" +
                "                    - a\n" +
                "                  properties:\n" +
                "                    a:\n" +
                "                      type: string\n" +
                "                  default:\n" +
                "                    a: default-a\n" +
                "                  x-inner: inner-value\n" +
                "                - type: object\n" +
                "                  required:\n" +
                "                    - b\n" +
                "                  properties:\n" +
                "                    b:\n" +
                "                      type: string\n" +
                "      responses:\n" +
                "        '200':\n" +
                "          description: OK\n";

        OpenAPI openAPI = parse(yaml);

        Schema combined = requestBodySchema(openAPI);
        assertNull(combined.getAllOf());
        assertNotNull(combined.getRequired(), "required from both allOf branches should be aggregated");
        assertTrue(combined.getRequired().contains("a"));
        assertTrue(combined.getRequired().contains("b"));
        assertTrue(combined.getDefault() instanceof ObjectNode,
                "the object default should be represented as a JSON object");
        assertEquals(((ObjectNode) combined.getDefault()).get("a").asText(), "default-a");
        assertNotNull(combined.getExtensions(), "extensions from branches and the source schema should be aggregated");
        assertTrue(combined.getExtensions().containsKey("x-inner"));
        assertTrue(combined.getExtensions().containsKey("x-outer"));
    }

    @Test
    public void allOfMergeForOas31ProducesJsonSchemaWithTypesSet() {
        // The merged model construction branches on SpecVersion: OAS 3.1 should produce a
        // JsonSchema with the "types" set populated, mirroring OAS 3.0's singular "type" branch.
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
                "              allOf:\n" +
                "                - type: object\n" +
                "                  properties:\n" +
                "                    a:\n" +
                "                      type: string\n" +
                "                - type: object\n" +
                "                  properties:\n" +
                "                    b:\n" +
                "                      type: string\n" +
                "      responses:\n" +
                "        '200':\n" +
                "          description: OK\n";

        OpenAPI openAPI = parse(yaml);

        Schema combined = requestBodySchema(openAPI);
        assertTrue(combined instanceof JsonSchema, "merged OAS 3.1 schema should be a JsonSchema");
        assertEquals(combined.getSpecVersion(), SpecVersion.V31);
        assertNotNull(combined.getTypes(), "merged OAS 3.1 schema should populate the types set, not the singular type field");
        assertTrue(combined.getTypes().contains("object"));
    }
}
