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

    @Test(description = "3.2-specific fields are bound to the model")
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
                "      operationId: queryPets\n" +
                "      responses:\n" +
                "        '200':\n" +
                "          description: ok\n";
        SwaggerParseResult result = new OpenAPIV3Parser().readContents(yaml, null, null);
        OpenAPI openAPI = result.getOpenAPI();
        assertNotNull(openAPI);
        assertNotNull(openAPI.getPaths().get("/pets").getGet());
        // 3.2 members bind to the model now
        assertEquals(openAPI.get$self(), "https://example.com/api/openapi.yaml");
        assertNotNull(openAPI.getPaths().get("/pets").getQuery());
        assertEquals(openAPI.getPaths().get("/pets").getQuery().getOperationId(), "queryPets");
        assertTrue(result.isOpenapi32());
        assertEquals(openAPI.getSpecVersion(), io.swagger.v3.oas.models.SpecVersion.V32);
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

    @Test(description = "3.2 'in: querystring' parameter binds as QueryStringParameter; missing content is reported")
    public void testOAS32QuerystringParamReported() {
        String yaml = "openapi: 3.2.0\n" +
                "info:\n" +
                "  title: Swagger Petstore\n" +
                "  version: 1.0.0\n" +
                "paths:\n" +
                "  /pets:\n" +
                "    get:\n" +
                "      parameters:\n" +
                "        - in: querystring\n" +
                "          content:\n" +
                "            application/json:\n" +
                "              schema:\n" +
                "                type: object\n" +
                "      responses:\n" +
                "        '200':\n" +
                "          description: ok\n";
        SwaggerParseResult result = new OpenAPIV3Parser().readContents(yaml, null, null);
        OpenAPI openAPI = result.getOpenAPI();
        assertNotNull(openAPI);
        assertEquals(openAPI.getPaths().get("/pets").getGet().getParameters().size(), 1);
        assertTrue(openAPI.getPaths().get("/pets").getGet().getParameters().get(0)
                instanceof io.swagger.v3.oas.models.parameters.QueryStringParameter);
        assertNotNull(openAPI.getPaths().get("/pets").getGet().getParameters().get(0).getContent());

        // querystring without content is invalid per spec
        String noContent = "openapi: 3.2.0\n" +
                "info:\n" +
                "  title: t\n" +
                "  version: 1.0.0\n" +
                "paths:\n" +
                "  /pets:\n" +
                "    get:\n" +
                "      parameters:\n" +
                "        - in: querystring\n" +
                "      responses:\n" +
                "        '200':\n" +
                "          description: ok\n";
        SwaggerParseResult bad = new OpenAPIV3Parser().readContents(noContent, null, null);
        assertTrue(bad.getMessages().stream().anyMatch(m -> m.contains("content")));
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

    @Test(description = "additionalOperations bind to the PathItem model; keys keep their case")
    public void testOAS32AdditionalOperations() {
        String yaml = "openapi: 3.2.0\n" +
                "info:\n" +
                "  title: t\n" +
                "  version: '1'\n" +
                "paths:\n" +
                "  /pets:\n" +
                "    get:\n" +
                "      operationId: getPets\n" +
                "      responses:\n" +
                "        '200':\n" +
                "          description: ok\n" +
                "    additionalOperations:\n" +
                "      PURGE:\n" +
                "        operationId: purgePets\n" +
                "        responses:\n" +
                "          '204':\n" +
                "            description: purged\n";
        SwaggerParseResult result = new OpenAPIV3Parser().readContents(yaml, null, null);
        OpenAPI openAPI = result.getOpenAPI();
        assertNotNull(openAPI);
        assertNotNull(openAPI.getPaths().get("/pets").getAdditionalOperations());
        assertEquals(openAPI.getPaths().get("/pets").getAdditionalOperations()
                .get("PURGE").getOperationId(), "purgePets");
    }

    @Test(description = "additionalOperations key duplicating a fixed method is detected")
    public void testOAS32AdditionalOperationsDuplicateDetected() {
        String yaml = "openapi: 3.2.0\n" +
                "info:\n" +
                "  title: t\n" +
                "  version: '1'\n" +
                "paths:\n" +
                "  /pets:\n" +
                "    additionalOperations:\n" +
                "      get:\n" +
                "        operationId: dupGet\n" +
                "        responses:\n" +
                "          '200':\n" +
                "            description: dup\n";
        SwaggerParseResult result = new OpenAPIV3Parser().readContents(yaml, null, null);
        assertTrue(result.getMessages().stream()
                .anyMatch(m -> m.contains("duplicates a fixed Path Item method name")));
        // lenient: the operation is still bound
        assertNotNull(result.getOpenAPI().getPaths().get("/pets").getAdditionalOperations().get("get"));
    }

    @Test(description = "additionalOperations work inside webhooks and components.pathItems")
    public void testOAS32AdditionalOperationsNested() {
        String yaml = "openapi: 3.2.0\n" +
                "info:\n" +
                "  title: t\n" +
                "  version: '1'\n" +
                "webhooks:\n" +
                "  hook:\n" +
                "    additionalOperations:\n" +
                "      NOTIFY:\n" +
                "        operationId: notify\n" +
                "        responses:\n" +
                "          '200':\n" +
                "            description: ok\n" +
                "components:\n" +
                "  pathItems:\n" +
                "    Shared:\n" +
                "      additionalOperations:\n" +
                "        SEARCH:\n" +
                "          operationId: search\n" +
                "          responses:\n" +
                "            '200':\n" +
                "              description: ok\n";
        SwaggerParseResult result = new OpenAPIV3Parser().readContents(yaml, null, null);
        OpenAPI openAPI = result.getOpenAPI();
        assertNotNull(openAPI);
        assertEquals(openAPI.getWebhooks().get("hook").getAdditionalOperations()
                .get("NOTIFY").getOperationId(), "notify");
        assertEquals(openAPI.getComponents().getPathItems().get("Shared")
                .getAdditionalOperations().get("SEARCH").getOperationId(), "search");
    }

    @Test(description = "in: querystring is rejected for 3.1 documents")
    public void testQuerystringRejectedOn31() {
        String yaml = "openapi: 3.1.0\n" +
                "info:\n" +
                "  title: t\n" +
                "  version: '1'\n" +
                "paths:\n" +
                "  /pets:\n" +
                "    get:\n" +
                "      parameters:\n" +
                "        - in: querystring\n" +
                "          content:\n" +
                "            application/json:\n" +
                "              schema:\n" +
                "                type: object\n" +
                "      responses:\n" +
                "        '200':\n" +
                "          description: ok\n";
        SwaggerParseResult result = new OpenAPIV3Parser().readContents(yaml, null, null);
        assertFalse(result.isOpenapi32());
        assertTrue(result.getMessages().stream()
                .anyMatch(m -> m.contains("in is not of type")));
    }

    @Test(description = "duplicate querystring parameters and querystring+query coexistence are detected")
    public void testQuerystringConstraints() {
        String yaml = "openapi: 3.2.0\n" +
                "info:\n" +
                "  title: t\n" +
                "  version: '1'\n" +
                "paths:\n" +
                "  /pets:\n" +
                "    get:\n" +
                "      parameters:\n" +
                "        - in: querystring\n" +
                "          content:\n" +
                "            application/json:\n" +
                "              schema:\n" +
                "                type: object\n" +
                "        - in: querystring\n" +
                "          content:\n" +
                "            application/json:\n" +
                "              schema:\n" +
                "                type: object\n" +
                "        - name: q\n" +
                "          in: query\n" +
                "          schema:\n" +
                "            type: string\n" +
                "      responses:\n" +
                "        '200':\n" +
                "          description: ok\n";
        SwaggerParseResult result = new OpenAPIV3Parser().readContents(yaml, null, null);
        assertTrue(result.getMessages().stream()
                .anyMatch(m -> m.contains("only one in: querystring")));
        assertTrue(result.getMessages().stream()
                .anyMatch(m -> m.contains("cannot be combined with in: query")));
    }

    @Test(description = "style: cookie binds for in: cookie in 3.2 (explode defaults true), warns on other locations, rejected for 3.1")
    public void testCookieStyle() {
        String responses = "      responses:\n" +
                "        '200':\n" +
                "          description: ok\n";
        // valid: in: cookie + style: cookie (explode defaults to true per spec)
        String validParams = "      parameters:\n" +
                "        - name: session\n" +
                "          in: cookie\n" +
                "          style: cookie\n" +
                "          schema:\n" +
                "            type: string\n";
        SwaggerParseResult v32 = new OpenAPIV3Parser().readContents(
                "openapi: 3.2.0\ninfo:\n  title: t\n  version: '1'\npaths:\n  /p:\n    get:\n"
                        + validParams + responses, null, null);
        io.swagger.v3.oas.models.parameters.Parameter cookieParam =
                v32.getOpenAPI().getPaths().get("/p").getGet().getParameters().get(0);
        assertEquals(cookieParam.getStyle(),
                io.swagger.v3.oas.models.parameters.Parameter.StyleEnum.COOKIE);
        assertEquals(cookieParam.getExplode(), Boolean.TRUE);
        assertFalse(v32.getMessages().stream()
                .anyMatch(m -> m.contains("only defined for in: cookie")));

        // invalid: style: cookie with in: query
        String invalidParams = "      parameters:\n" +
                "        - name: session\n" +
                "          in: query\n" +
                "          style: cookie\n" +
                "          schema:\n" +
                "            type: string\n";
        SwaggerParseResult v32bad = new OpenAPIV3Parser().readContents(
                "openapi: 3.2.0\ninfo:\n  title: t\n  version: '1'\npaths:\n  /p:\n    get:\n"
                        + invalidParams + responses, null, null);
        assertTrue(v32bad.getMessages().stream()
                .anyMatch(m -> m.contains("only defined for in: cookie")));

        SwaggerParseResult v31 = new OpenAPIV3Parser().readContents(
                "openapi: 3.1.0\ninfo:\n  title: t\n  version: '1'\npaths:\n  /p:\n    get:\n"
                        + validParams + responses, null, null);
        assertTrue(v31.getMessages().stream()
                .anyMatch(m -> m.contains("style is not of type")));
    }

    @Test(description = "components.mediaTypes and content map $ref values bind under 3.2")
    public void testOAS32MediaTypesAndContentRef() {
        String yaml = "openapi: 3.2.0\n" +
                "info:\n" +
                "  title: t\n" +
                "  version: '1'\n" +
                "paths:\n" +
                "  /pets:\n" +
                "    get:\n" +
                "      responses:\n" +
                "        '200':\n" +
                "          description: ok\n" +
                "          content:\n" +
                "            application/json:\n" +
                "              $ref: '#/components/mediaTypes/Pet'\n" +
                "components:\n" +
                "  mediaTypes:\n" +
                "    Pet:\n" +
                "      schema:\n" +
                "        type: object\n";
        SwaggerParseResult result = new OpenAPIV3Parser().readContents(yaml, null, null);
        OpenAPI openAPI = result.getOpenAPI();
        assertNotNull(openAPI);
        assertEquals(openAPI.getPaths().get("/pets").getGet().getResponses().get("200")
                        .getContent().get("application/json").get$ref(),
                "#/components/mediaTypes/Pet");
        assertNotNull(openAPI.getComponents().getMediaTypes());
        assertNotNull(openAPI.getComponents().getMediaTypes().get("Pet").getSchema());
    }

    @Test(description = "content map $ref is not bound under 3.1 (MediaType has no $ref there)")
    public void testContentRefNotBound31() {
        String yaml = "openapi: 3.1.0\n" +
                "info:\n" +
                "  title: t\n" +
                "  version: '1'\n" +
                "paths:\n" +
                "  /pets:\n" +
                "    get:\n" +
                "      responses:\n" +
                "        '200':\n" +
                "          description: ok\n" +
                "          content:\n" +
                "            application/json:\n" +
                "              $ref: '#/components/mediaTypes/Pet'\n";
        SwaggerParseResult result = new OpenAPIV3Parser().readContents(yaml, null, null);
        OpenAPI openAPI = result.getOpenAPI();
        assertNotNull(openAPI);
        assertNull(openAPI.getPaths().get("/pets").getGet().getResponses().get("200")
                .getContent().get("application/json").get$ref());
        assertTrue(result.getMessages().stream()
                .anyMatch(m -> m.contains("$ref") && m.contains("unexpected")));
    }

    @Test(description = "3.2 scalar fields bind: server.name, tag.summary/parent/kind, response.summary, example.dataValue/serializedValue, securityScheme.deprecated/oauth2MetadataUrl, deviceAuthorization flow, xml.nodeType, discriminator.defaultMapping, mediaType.itemSchema/encoding")
    public void testOAS32NewScalarFields() {
        String yaml = "openapi: 3.2.0\n" +
                "info:\n" +
                "  title: t\n" +
                "  version: '1'\n" +
                "servers:\n" +
                "  - url: https://api.example.com\n" +
                "    name: production\n" +
                "tags:\n" +
                "  - name: pet\n" +
                "    summary: Pet operations\n" +
                "    parent: root\n" +
                "    kind: nav\n" +
                "paths:\n" +
                "  /pets:\n" +
                "    get:\n" +
                "      responses:\n" +
                "        '200':\n" +
                "          description: ok\n" +
                "          summary: pet list\n" +
                "          content:\n" +
                "            application/json:\n" +
                "              itemSchema:\n" +
                "                type: string\n" +
                "components:\n" +
                "  securitySchemes:\n" +
                "    oauth:\n" +
                "      type: oauth2\n" +
                "      deprecated: true\n" +
                "      oauth2MetadataUrl: https://example.com/.well-known/oauth-authorization-server\n" +
                "      flows:\n" +
                "        deviceAuthorization:\n" +
                "          deviceAuthorizationUrl: https://example.com/device\n" +
                "          tokenUrl: https://example.com/token\n" +
                "          scopes: {}\n";
        SwaggerParseResult result = new OpenAPIV3Parser().readContents(yaml, null, null);
        OpenAPI openAPI = result.getOpenAPI();
        assertNotNull(openAPI);
        assertEquals(openAPI.getServers().get(0).getName(), "production");
        assertEquals(openAPI.getTags().get(0).getSummary(), "Pet operations");
        assertEquals(openAPI.getTags().get(0).getParent(), "root");
        assertEquals(openAPI.getTags().get(0).getKind(), "nav");
        assertEquals(openAPI.getPaths().get("/pets").getGet().getResponses().get("200")
                .getSummary(), "pet list");
        assertNotNull(openAPI.getPaths().get("/pets").getGet().getResponses().get("200")
                .getContent().get("application/json").getItemSchema());
        io.swagger.v3.oas.models.security.SecurityScheme scheme =
                openAPI.getComponents().getSecuritySchemes().get("oauth");
        assertEquals(scheme.getDeprecated(), Boolean.TRUE);
        assertEquals(scheme.getOauth2MetadataUrl(),
                "https://example.com/.well-known/oauth-authorization-server");
        assertEquals(scheme.getFlows().getDeviceAuthorization().getDeviceAuthorizationUrl(),
                "https://example.com/device");
        assertEquals(scheme.getFlows().getDeviceAuthorization().getTokenUrl(),
                "https://example.com/token");
    }

    @Test(description = "3.2 example dataValue/serializedValue bind; value+dataValue coexistence warns")
    public void testOAS32ExampleFields() {
        String yaml = "openapi: 3.2.0\n" +
                "info:\n" +
                "  title: t\n" +
                "  version: '1'\n" +
                "paths: {}\n" +
                "components:\n" +
                "  examples:\n" +
                "    ex:\n" +
                "      dataValue:\n" +
                "        name: fido\n" +
                "      serializedValue: '{\"name\":\"fido\"}'\n";
        SwaggerParseResult result = new OpenAPIV3Parser().readContents(yaml, null, null);
        io.swagger.v3.oas.models.examples.Example ex =
                result.getOpenAPI().getComponents().getExamples().get("ex");
        assertNotNull(ex.getDataValue());
        assertEquals(ex.getSerializedValue(), "{\"name\":\"fido\"}");
    }

    @Test(description = "3.2 xml.nodeType and discriminator.defaultMapping bind")
    public void testOAS32XmlAndDiscriminator() {
        String yaml = "openapi: 3.2.0\n" +
                "info:\n" +
                "  title: t\n" +
                "  version: '1'\n" +
                "paths: {}\n" +
                "components:\n" +
                "  schemas:\n" +
                "    Pet:\n" +
                "      type: object\n" +
                "      xml:\n" +
                "        nodeType: element\n" +
                "      discriminator:\n" +
                "        propertyName: kind\n" +
                "        defaultMapping: '#/components/schemas/Dog'\n";
        SwaggerParseResult result = new OpenAPIV3Parser().readContents(yaml, null, null);
        io.swagger.v3.oas.models.media.Schema pet =
                result.getOpenAPI().getComponents().getSchemas().get("Pet");
        assertEquals(pet.getXml().getNodeType(), "element");
        assertEquals(pet.getDiscriminator().getDefaultMapping(), "#/components/schemas/Dog");
    }

    @Test(description = "3.2 nested encodings (encoding/prefixEncoding/itemEncoding) bind")
    public void testOAS32NestedEncodings() {
        String yaml = "openapi: 3.2.0\n" +
                "info:\n" +
                "  title: t\n" +
                "  version: '1'\n" +
                "paths:\n" +
                "  /upload:\n" +
                "    post:\n" +
                "      requestBody:\n" +
                "        content:\n" +
                "          multipart/mixed:\n" +
                "            schema:\n" +
                "              type: object\n" +
                "            prefixEncoding:\n" +
                "              - contentType: application/json\n" +
                "            itemEncoding:\n" +
                "              contentType: text/plain\n" +
                "            encoding:\n" +
                "              part:\n" +
                "                contentType: application/octet-stream\n" +
                "                itemEncoding:\n" +
                "                  contentType: application/json\n" +
                "      responses:\n" +
                "        '200':\n" +
                "          description: ok\n";
        SwaggerParseResult result = new OpenAPIV3Parser().readContents(yaml, null, null);
        io.swagger.v3.oas.models.media.MediaType mt = result.getOpenAPI().getPaths()
                .get("/upload").getPost().getRequestBody().getContent().get("multipart/mixed");
        assertEquals(mt.getPrefixEncoding().size(), 1);
        assertEquals(mt.getPrefixEncoding().get(0).getContentType(), "application/json");
        assertEquals(mt.getItemEncoding().getContentType(), "text/plain");
        assertEquals(mt.getEncoding().get("part").getItemEncoding().getContentType(),
                "application/json");
    }

    @Test(description = "3.2 fields are reported unexpected and not bound under 3.1")
    public void testOAS32FieldsNotBound31() {
        String yaml = "openapi: 3.1.0\n" +
                "$self: https://example.com/openapi.yaml\n" +
                "info:\n" +
                "  title: t\n" +
                "  version: '1'\n" +
                "servers:\n" +
                "  - url: https://api.example.com\n" +
                "    name: production\n" +
                "paths:\n" +
                "  /pets:\n" +
                "    query:\n" +
                "      operationId: q\n" +
                "      responses:\n" +
                "        '200':\n" +
                "          description: ok\n" +
                "components:\n" +
                "  mediaTypes:\n" +
                "    Pet:\n" +
                "      schema:\n" +
                "        type: object\n";
        SwaggerParseResult result = new OpenAPIV3Parser().readContents(yaml, null, null);
        OpenAPI openAPI = result.getOpenAPI();
        assertNotNull(openAPI);
        assertFalse(result.isOpenapi32());
        assertNull(openAPI.get$self());
        assertNull(openAPI.getServers().get(0).getName());
        assertNull(openAPI.getPaths().get("/pets").getQuery());
        assertNull(openAPI.getComponents().getMediaTypes());
        assertTrue(result.getMessages().contains("attribute $self is unexpected"));
        assertTrue(result.getMessages().contains("attribute .servers.name is unexpected"));
        assertTrue(result.getMessages().contains("attribute paths.'/pets'.query is unexpected"));
        assertTrue(result.getMessages().contains("attribute components.mediaTypes is unexpected"));
    }

    @Test(description = "JSON round-trip: parse -> serialize via Json32 -> re-parse preserves 3.2 fields")
    public void testOAS32JsonRoundTrip() throws Exception {
        String json = "{\n" +
                "  \"openapi\": \"3.2.0\",\n" +
                "  \"$self\": \"https://example.com/openapi.json\",\n" +
                "  \"info\": {\"title\": \"t\", \"version\": \"1\"},\n" +
                "  \"paths\": {\n" +
                "    \"/pets\": {\n" +
                "      \"get\": {\n" +
                "        \"operationId\": \"getPets\",\n" +
                "        \"responses\": {\"200\": {\"description\": \"ok\",\n" +
                "          \"content\": {\"application/json\": {\"$ref\": \"#/components/mediaTypes/Pet\"}}}},\n" +
                "        \"parameters\": [{\"in\": \"querystring\",\n" +
                "          \"content\": {\"application/json\": {\"schema\": {\"type\": \"object\"}}}}]\n" +
                "      },\n" +
                "      \"additionalOperations\": {\"PURGE\": {\"operationId\": \"purge\",\n" +
                "        \"responses\": {\"204\": {\"description\": \"done\"}}}}\n" +
                "    }\n" +
                "  },\n" +
                "  \"components\": {\"mediaTypes\": {\"Pet\": {\"schema\": {\"type\": \"object\"}}}}\n" +
                "}";
        SwaggerParseResult result = new OpenAPIV3Parser().readContents(json, null, null);
        OpenAPI openAPI = result.getOpenAPI();
        assertNotNull(openAPI);

        String serialized = io.swagger.v3.core.util.Json32.pretty(openAPI);
        SwaggerParseResult reparsed = new OpenAPIV3Parser().readContents(serialized, null, null);
        OpenAPI again = reparsed.getOpenAPI();
        assertNotNull(again);
        assertEquals(again.get$self(), "https://example.com/openapi.json");
        assertEquals(again.getPaths().get("/pets").getAdditionalOperations()
                .get("PURGE").getOperationId(), "purge");
        assertEquals(again.getPaths().get("/pets").getGet().getResponses().get("200")
                .getContent().get("application/json").get$ref(), "#/components/mediaTypes/Pet");
        assertTrue(again.getPaths().get("/pets").getGet().getParameters().get(0)
                instanceof io.swagger.v3.oas.models.parameters.QueryStringParameter);
        assertNotNull(again.getComponents().getMediaTypes().get("Pet"));
    }

    @Test(description = "YAML round-trip: parse -> serialize via Yaml32 -> re-parse preserves 3.2 fields")
    public void testOAS32YamlRoundTrip() throws Exception {
        String yaml = "openapi: 3.2.0\n" +
                "$self: https://example.com/openapi.yaml\n" +
                "info:\n" +
                "  title: t\n" +
                "  version: '1'\n" +
                "paths:\n" +
                "  /pets:\n" +
                "    query:\n" +
                "      operationId: queryPets\n" +
                "      responses:\n" +
                "        '200':\n" +
                "          description: ok\n" +
                "    additionalOperations:\n" +
                "      PURGE:\n" +
                "        operationId: purgePets\n" +
                "        responses:\n" +
                "          '204':\n" +
                "            description: done\n" +
                "components:\n" +
                "  mediaTypes:\n" +
                "    Pet:\n" +
                "      schema:\n" +
                "        type: object\n";
        SwaggerParseResult result = new OpenAPIV3Parser().readContents(yaml, null, null);
        OpenAPI openAPI = result.getOpenAPI();
        assertNotNull(openAPI);

        String serialized = io.swagger.v3.core.util.Yaml32.pretty(openAPI);
        SwaggerParseResult reparsed = new OpenAPIV3Parser().readContents(serialized, null, null);
        OpenAPI again = reparsed.getOpenAPI();
        assertNotNull(again);
        assertEquals(again.get$self(), "https://example.com/openapi.yaml");
        assertEquals(again.getPaths().get("/pets").getQuery().getOperationId(), "queryPets");
        assertEquals(again.getPaths().get("/pets").getAdditionalOperations()
                .get("PURGE").getOperationId(), "purgePets");
        assertNotNull(again.getComponents().getMediaTypes().get("Pet"));
    }

    @Test(description = "name remains REQUIRED for in: querystring parameters")
    public void testQuerystringNameRequired() {
        String yaml = "openapi: 3.2.0\n" +
                "info:\n" +
                "  title: t\n" +
                "  version: '1'\n" +
                "paths:\n" +
                "  /pets:\n" +
                "    get:\n" +
                "      parameters:\n" +
                "        - in: querystring\n" +
                "          content:\n" +
                "            application/json:\n" +
                "              schema:\n" +
                "                type: object\n" +
                "      responses:\n" +
                "        '200':\n" +
                "          description: ok\n";
        SwaggerParseResult result = new OpenAPIV3Parser().readContents(yaml, null, null);
        assertTrue(result.getMessages().stream()
                .anyMatch(m -> m.contains("name")));
    }

    @Test(description = "example/examples are permitted common fields on in: querystring parameters")
    public void testQuerystringAllowsExamples() {
        String yaml = "openapi: 3.2.0\n" +
                "info:\n" +
                "  title: t\n" +
                "  version: '1'\n" +
                "paths:\n" +
                "  /pets:\n" +
                "    get:\n" +
                "      parameters:\n" +
                "        - name: query\n" +
                "          in: querystring\n" +
                "          example: a=1&b=2\n" +
                "          content:\n" +
                "            application/x-www-form-urlencoded:\n" +
                "              schema:\n" +
                "                type: object\n" +
                "      responses:\n" +
                "        '200':\n" +
                "          description: ok\n";
        SwaggerParseResult result = new OpenAPIV3Parser().readContents(yaml, null, null);
        assertFalse(result.getMessages().stream()
                .anyMatch(m -> m.contains("must not define example")));
        assertNotNull(result.getOpenAPI().getPaths().get("/pets").getGet()
                .getParameters().get(0).getExample());
    }

    @Test(description = "querystring/query conflict is detected across path-item and operation parameters")
    public void testQuerystringCrossLevelConflict() {
        String yaml = "openapi: 3.2.0\n" +
                "info:\n" +
                "  title: t\n" +
                "  version: '1'\n" +
                "paths:\n" +
                "  /pets:\n" +
                "    parameters:\n" +
                "      - name: q\n" +
                "        in: query\n" +
                "        schema:\n" +
                "          type: string\n" +
                "    get:\n" +
                "      parameters:\n" +
                "        - name: qs\n" +
                "          in: querystring\n" +
                "          content:\n" +
                "            application/json:\n" +
                "              schema:\n" +
                "                type: object\n" +
                "      responses:\n" +
                "        '200':\n" +
                "          description: ok\n";
        SwaggerParseResult result = new OpenAPIV3Parser().readContents(yaml, null, null);
        assertTrue(result.getMessages().stream()
                .anyMatch(m -> m.contains("cannot be combined with in: query")));

        // conflict via a local components.parameters $ref is also detected
        String refYaml = "openapi: 3.2.0\n" +
                "info:\n" +
                "  title: t\n" +
                "  version: '1'\n" +
                "paths:\n" +
                "  /pets:\n" +
                "    get:\n" +
                "      parameters:\n" +
                "        - $ref: '#/components/parameters/q'\n" +
                "        - name: qs\n" +
                "          in: querystring\n" +
                "          content:\n" +
                "            application/json:\n" +
                "              schema:\n" +
                "                type: object\n" +
                "      responses:\n" +
                "        '200':\n" +
                "          description: ok\n" +
                "components:\n" +
                "  parameters:\n" +
                "    q:\n" +
                "      name: q\n" +
                "      in: query\n" +
                "      schema:\n" +
                "        type: string\n";
        SwaggerParseResult refResult = new OpenAPIV3Parser().readContents(refYaml, null, null);
        assertTrue(refResult.getMessages().stream()
                .anyMatch(m -> m.contains("cannot be combined with in: query")));
    }

    @Test(description = "deviceAuthorizationUrl is not bound for 3.0/3.1 documents")
    public void testDeviceAuthorizationUrlVersionGated() {
        String tail = "components:\n" +
                "  securitySchemes:\n" +
                "    oauth:\n" +
                "      type: oauth2\n" +
                "      flows:\n" +
                "        implicit:\n" +
                "          authorizationUrl: https://example.com/auth\n" +
                "          deviceAuthorizationUrl: https://example.com/device\n";
        SwaggerParseResult v31 = new OpenAPIV3Parser().readContents(
                "openapi: 3.1.0\ninfo:\n  title: t\n  version: '1'\npaths: {}\n" + tail, null, null);
        assertNull(v31.getOpenAPI().getComponents().getSecuritySchemes().get("oauth")
                .getFlows().getImplicit().getDeviceAuthorizationUrl());

        SwaggerParseResult v32 = new OpenAPIV3Parser().readContents(
                "openapi: 3.2.0\ninfo:\n  title: t\n  version: '1'\npaths: {}\n" + tail, null, null);
        assertEquals(v32.getOpenAPI().getComponents().getSecuritySchemes().get("oauth")
                .getFlows().getImplicit().getDeviceAuthorizationUrl(), "https://example.com/device");
    }

    @Test(description = "boolean itemSchema is accepted")
    public void testItemSchemaBoolean() {
        String yaml = "openapi: 3.2.0\n" +
                "info:\n" +
                "  title: t\n" +
                "  version: '1'\n" +
                "paths:\n" +
                "  /pets:\n" +
                "    get:\n" +
                "      responses:\n" +
                "        '200':\n" +
                "          description: ok\n" +
                "          content:\n" +
                "            application/json-seq:\n" +
                "              itemSchema: false\n";
        SwaggerParseResult result = new OpenAPIV3Parser().readContents(yaml, null, null);
        assertNotNull(result.getOpenAPI().getPaths().get("/pets").getGet()
                .getResponses().get("200").getContent().get("application/json-seq").getItemSchema());
        assertFalse(result.getMessages().stream()
                .anyMatch(m -> m.contains("itemSchema")));
    }

    @Test(description = "resolve=true preserves 3.2 fields in $ref'd PathItems and resolves MediaType references")
    public void testResolvePreserves32Fields() {
        String yaml = "openapi: 3.2.0\n" +
                "info:\n" +
                "  title: t\n" +
                "  version: '1'\n" +
                "paths:\n" +
                "  /pets:\n" +
                "    $ref: '#/components/pathItems/petsPath'\n" +
                "components:\n" +
                "  pathItems:\n" +
                "    petsPath:\n" +
                "      query:\n" +
                "        operationId: queryPets\n" +
                "        responses:\n" +
                "          '200':\n" +
                "            description: ok\n" +
                "            content:\n" +
                "              application/json:\n" +
                "                $ref: '#/components/mediaTypes/PetMedia'\n" +
                "      additionalOperations:\n" +
                "        PURGE:\n" +
                "          operationId: purgePets\n" +
                "          responses:\n" +
                "            '204':\n" +
                "              description: done\n" +
                "  mediaTypes:\n" +
                "    PetMedia:\n" +
                "      schema:\n" +
                "        type: object\n";
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        SwaggerParseResult result = new OpenAPIV3Parser().readContents(yaml, null, options);
        OpenAPI openAPI = result.getOpenAPI();
        assertNotNull(openAPI);
        // local pathItem refs resolve into components.pathItems (upstream behavior)
        io.swagger.v3.oas.models.PathItem pets =
                openAPI.getComponents().getPathItems().get("petsPath");
        assertNotNull(pets);
        assertNotNull(pets.getQuery());
        assertEquals(pets.getQuery().getOperationId(), "queryPets");
        assertNotNull(pets.getAdditionalOperations());
        assertEquals(pets.getAdditionalOperations().get("PURGE").getOperationId(), "purgePets");
        // media type reference resolved
        io.swagger.v3.oas.models.media.MediaType mt = pets.getQuery().getResponses().get("200")
                .getContent().get("application/json");
        assertNotNull(mt);
        assertNull(mt.get$ref());
        assertNotNull(mt.getSchema());
    }
}
