package io.swagger.v3.parser.test;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class OAI32DeserializationTest {

    @Test(description = "OpenAPI 3.2.0 version string is recognized and parsed")
    public void testBasicOAS320() {
        String yaml = "openapi: 3.2.0\n" +
                "info:\n" +
                "  title: Swagger Petstore\n" +
                "  version: 1.0.0\n" +
                "paths:\n" +
                "  /pets:\n" +
                "    get:\n" +
                "      responses:\n" +
                "        '200':\n" +
                "          description: ok\n";
        SwaggerParseResult result = new OpenAPIV3Parser().readContents(yaml, null, null);
        assertNotNull(result.getOpenAPI());
        assertEquals(result.getOpenAPI().getOpenapi(), "3.2.0");
        assertNotNull(result.getOpenAPI().getPaths().get("/pets").getGet());
        // 3.2 shares the 3.1 data model code path
        assertTrue(result.isOpenapi31());
    }

    @Test(description = "OpenAPI 3.2.1 version string is recognized and parsed")
    public void testBasicOAS321() {
        String json = "{\n" +
                "  \"openapi\": \"3.2.1\",\n" +
                "  \"info\": {\n" +
                "    \"title\": \"Swagger Petstore\",\n" +
                "    \"version\": \"1.0.0\"\n" +
                "  },\n" +
                "  \"paths\": {}\n" +
                "}";
        SwaggerParseResult result = new OpenAPIV3Parser().readContents(json, null, null);
        assertNotNull(result.getOpenAPI());
        assertEquals(result.getOpenAPI().getOpenapi(), "3.2.1");
        assertTrue(result.isOpenapi31());
    }

    @Test(description = "3.2-specific fields do not crash parsing and are reported as unexpected attributes")
    public void testOAS32SpecificFieldsRecorded() {
        String yaml = "openapi: 3.2.0\n" +
                "$self: https://example.com/api/openapi.yaml\n" +
                "jsonSchemaDialect: https://json-schema.org/draft/2020-12/schema\n" +
                "info:\n" +
                "  title: Swagger Petstore\n" +
                "  version: 1.0.0\n" +
                "paths:\n" +
                "  /pets:\n" +
                "    get:\n" +
                "      responses:\n" +
                "        '200':\n" +
                "          description: ok\n" +
                "    query:\n" +
                "      responses:\n" +
                "        '200':\n" +
                "          description: ok\n";
        SwaggerParseResult result = new OpenAPIV3Parser().readContents(yaml, null, null);
        OpenAPI openAPI = result.getOpenAPI();
        assertNotNull(openAPI);
        assertNotNull(openAPI.getPaths().get("/pets").getGet());
        // 3.2-only members are not modeled yet: they surface as validation messages
        assertTrue(result.getMessages().contains("attribute $self is unexpected"));
        assertTrue(result.getMessages().contains("attribute paths.'/pets'.query is unexpected"));
        // 3.1-family fields continue to work
        assertEquals(openAPI.getJsonSchemaDialect(), "https://json-schema.org/draft/2020-12/schema");
    }

    @Test(description = "3.2 document parses with resolveFully option without crashing")
    public void testOAS32ResolveFully() {
        String yaml = "openapi: 3.2.0\n" +
                "info:\n" +
                "  title: Swagger Petstore\n" +
                "  version: 1.0.0\n" +
                "paths:\n" +
                "  /pets:\n" +
                "    get:\n" +
                "      responses:\n" +
                "        '200':\n" +
                "          description: ok\n" +
                "          content:\n" +
                "            application/json:\n" +
                "              schema:\n" +
                "                $ref: '#/components/schemas/Pet'\n" +
                "components:\n" +
                "  schemas:\n" +
                "    Pet:\n" +
                "      type: object\n" +
                "      properties:\n" +
                "        name:\n" +
                "          type: string\n";
        ParseOptions options = new ParseOptions();
        options.setResolveFully(true);
        SwaggerParseResult result = new OpenAPIV3Parser().readContents(yaml, null, options);
        OpenAPI openAPI = result.getOpenAPI();
        assertNotNull(openAPI);
        assertEquals(openAPI.getOpenapi(), "3.2.0");
        // the $ref is actually resolved through the 3.1-family dereference path
        Schema schema = openAPI.getPaths().get("/pets").getGet().getResponses().get("200")
                .getContent().get("application/json").getSchema();
        assertNull(schema.get$ref());
        assertNotNull(schema.getProperties().get("name"));
    }

    @Test(description = "3.2 resolve routes through the 3.1-family dereferencer: components.pathItems refs resolve")
    public void testOAS32ResolveRoutesTo31Dereferencer() {
        // components.pathItems is a 3.1+ feature the legacy (3.0) resolver cannot resolve:
        // ResolverCache has no pattern for #/components/pathItems, so under the wrong
        // routing this assertion fails (the pathItem stays an unresolved $ref with no ops)
        String yaml = "openapi: 3.2.0\n" +
                "info:\n" +
                "  title: Swagger Petstore\n" +
                "  version: 1.0.0\n" +
                "paths:\n" +
                "  /pets:\n" +
                "    $ref: '#/components/pathItems/PetOps'\n" +
                "components:\n" +
                "  pathItems:\n" +
                "    PetOps:\n" +
                "      get:\n" +
                "        operationId: listPets\n" +
                "        responses:\n" +
                "          '200':\n" +
                "            description: ok\n";
        ParseOptions options = new ParseOptions();
        options.setResolveFully(true);
        SwaggerParseResult result = new OpenAPIV3Parser().readContents(yaml, null, options);
        OpenAPI openAPI = result.getOpenAPI();
        assertNotNull(openAPI);
        assertNotNull(openAPI.getPaths().get("/pets").getGet());
        assertEquals(openAPI.getPaths().get("/pets").getGet().getOperationId(), "listPets");
    }

    @Test(description = "3.2 'in: querystring' parameter is dropped with a validation message (current best-effort)")
    public void testOAS32QuerystringParamReported() {
        String yaml = "openapi: 3.2.0\n" +
                "info:\n" +
                "  title: Swagger Petstore\n" +
                "  version: 1.0.0\n" +
                "paths:\n" +
                "  /pets:\n" +
                "    get:\n" +
                "      parameters:\n" +
                "        - name: q\n" +
                "          in: querystring\n" +
                "      responses:\n" +
                "        '200':\n" +
                "          description: ok\n";
        SwaggerParseResult result = new OpenAPIV3Parser().readContents(yaml, null, null);
        OpenAPI openAPI = result.getOpenAPI();
        assertNotNull(openAPI);
        // the parameter is not representable yet: it is dropped and reported
        assertTrue(openAPI.getPaths().get("/pets").getGet().getParameters() == null
                || openAPI.getPaths().get("/pets").getGet().getParameters().isEmpty());
        assertTrue(result.getMessages().stream()
                .anyMatch(m -> m.contains("in is not of type `[query|header|path|cookie]`")));
    }

    @Test(description = "bare 3.2 (no patch version) is accepted per upstream's loose convention, with a warning")
    public void testBareVersionString() {
        // mirrors upstream's handling of bare "3.0"/"3.1" (see OpenAPIV3ParserTest#testIssue1780):
        // accepted and routed to the 3.1-family model, but reported as not a valid version field
        String yaml = "openapi: '3.2'\n" +
                "info:\n" +
                "  title: Swagger Petstore\n" +
                "  version: 1.0.0\n" +
                "paths: {}\n";
        SwaggerParseResult result = new OpenAPIV3Parser().readContents(yaml, null, null);
        assertNotNull(result.getOpenAPI());
        assertEquals(result.getOpenAPI().getOpenapi(), "3.2");
        assertTrue(result.isOpenapi31());
        assertTrue(result.getMessages().contains("The provided definition does not specify a valid version field"));
    }

    @Test(description = "loose prefix: 3.20.x is accepted because it starts with \"3.2\" (prefix match, not true 3.x support -- 3.3.0 is rejected), and warned since it has no 3.2. dotted form")
    public void testVersionPrefixBoundary() {
        String yaml = "openapi: 3.20.0\n" +
                "info:\n" +
                "  title: Swagger Petstore\n" +
                "  version: 1.0.0\n" +
                "paths: {}\n";
        SwaggerParseResult result = new OpenAPIV3Parser().readContents(yaml, null, null);
        assertNotNull(result.getOpenAPI());
        assertTrue(result.isOpenapi31());
        assertTrue(result.getMessages().contains("The provided definition does not specify a valid version field"));
    }

    @Test(description = "malformed version 3.2. (trailing dot, no patch) still parses; no warning fires, matching the pre-existing 3.0./3.1. quirk this change extends rather than fixes")
    public void testMalformedVersionString() {
        // "3.2." satisfies startsWith("3.2."), so the dotted-form warning check does not
        // fire for it, the same way "3.0." and "3.1." alone would not warn either. This is
        // an inherited quirk of the pre-existing convention, not something new to 3.2 --
        // asserted explicitly here so a future change to the 3.0/3.1 behavior doesn't
        // silently leave 3.2 inconsistent with it.
        String yaml = "openapi: '3.2.'\n" +
                "info:\n" +
                "  title: Swagger Petstore\n" +
                "  version: 1.0.0\n" +
                "paths: {}\n";
        SwaggerParseResult result = new OpenAPIV3Parser().readContents(yaml, null, null);
        assertNotNull(result.getOpenAPI());
        assertEquals(result.getOpenAPI().getOpenapi(), "3.2.");
        assertTrue(result.isOpenapi31());
        assertFalse(result.getMessages().contains("The provided definition does not specify a valid version field"));
    }

    @Test(description = "3.3.0 is genuinely rejected (unlike 3.20.0, which is only accepted by prefix-match accident)")
    public void testUnrecognizedMinorVersionRejected() {
        String yaml = "openapi: 3.3.0\n" +
                "info:\n" +
                "  title: Swagger Petstore\n" +
                "  version: 1.0.0\n" +
                "paths: {}\n";
        SwaggerParseResult result = new OpenAPIV3Parser().readContents(yaml, null, null);
        assertNull(result.getOpenAPI());
    }
}
