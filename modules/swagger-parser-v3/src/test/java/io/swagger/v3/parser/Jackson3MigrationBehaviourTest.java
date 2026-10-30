package io.swagger.v3.parser;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.testng.annotations.Test;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.*;

public class Jackson3MigrationBehaviourTest {

    private static SwaggerParseResult parse(String content) {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        return new OpenAPIV3Parser().readContents(content, null, options);
    }

    private static final String HEAD = "openapi: 3.0.3\ninfo:\n  title: t\n  version: '1'\n";
    private static final String HEAD31 = "openapi: 3.1.0\ninfo:\n  title: t\n  version: '1'\n";

    @Test
    public void duplicateDetectionIsEnabledOnBothMappers() {
        assertTrue(ObjectMapperFactory.createJson().tokenStreamFactory()
                .isEnabled(tools.jackson.core.StreamReadFeature.STRICT_DUPLICATE_DETECTION));
        assertTrue(ObjectMapperFactory.createYaml().tokenStreamFactory()
                .isEnabled(tools.jackson.core.StreamReadFeature.STRICT_DUPLICATE_DETECTION));
        try {
            ObjectMapperFactory.createJson().readTree("{\"200\":{},\"200\":{}}");
            fail("duplicate key must fail");
        } catch (StreamReadException e) {
            assertEquals(e.getOriginalMessage(), "Duplicate Object property \"200\"");
        }
        try {
            ObjectMapperFactory.createYaml().readTree("a: 1\na: 2\n");
            fail("duplicate key must fail");
        } catch (StreamReadException e) {
            assertTrue(e.getOriginalMessage().contains("Duplicate"));
        }
    }

    public static class Bean {
        public Integer c = 3;
        public String a = null;
        public Integer b = 1;
    }

    @Test
    public void nullsAreSkipped() {
        String json = ObjectMapperFactory.createJson().writeValueAsString(new Bean());
        assertTrue(json.contains("\"c\":3") && json.contains("\"b\":1") && !json.contains("\"a\""), json);
    }

    @Test
    public void unknownPropertiesAndEmptyBeansDoNotFail() {
        ObjectMapper mapper = ObjectMapperFactory.createJson();
        assertEquals(mapper.writeValueAsString(new Object()), "{}");
        assertNotNull(mapper.readValue("{\"x\":1}", Map.class));
    }

    private static String nestedJson(int depth) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"a\":".repeat(Math.max(0, depth)));
        sb.append("1");
        sb.append("}".repeat(Math.max(0, depth)));
        return sb.toString();
    }

    // Jackson 3 lowers the default nesting depth from 1000 to 500. The parser keeps the Jackson 3 limit.
    @Test
    public void jsonNestedUpToJackson3LimitIsAccepted() {
        assertNotNull(ObjectMapperFactory.createJson().readTree(nestedJson(400)));
    }

    @Test(expectedExceptions = tools.jackson.core.JacksonException.class)
    public void jsonNestedDeeperThanJackson3LimitIsRejected() {
        ObjectMapperFactory.createJson().readTree(nestedJson(600));
    }

    // ---- extensions: convertValue(node, Object.class) ----

    private void assertExtensionTypes(Map<String, Object> ext) {
        assertEquals(ext.get("x-int"), 12);
        assertEquals(ext.get("x-long"), 12345678901L);
        assertEquals(ext.get("x-big"), new BigInteger("123456789012345678901234567890"));
        assertEquals(ext.get("x-double"), 1.5);
        assertEquals(ext.get("x-bool"), Boolean.TRUE);
        assertEquals(ext.get("x-str"), "s");
        assertTrue(ext.containsKey("x-null"));
        assertNull(ext.get("x-null"));
        assertEquals(ext.get("x-list"), List.of(1, "a"));
        assertEquals(ext.get("x-obj"), Map.of("k", 1));
    }

    private static final String EXTENSIONS =
            """
                    x-int: 12
                    x-long: 12345678901
                    x-big: 123456789012345678901234567890
                    x-double: 1.5
                    x-bool: true
                    x-str: s
                    x-null: null
                    x-list: [1, a]
                    x-obj: {k: 1}
                    """;

    @Test
    public void extensionsKeepJackson2NumberTypes30() {
        SwaggerParseResult r = parse(HEAD + EXTENSIONS + "paths: {}\n");
        assertNotNull(r.getOpenAPI(), String.valueOf(r.getMessages()));
        assertExtensionTypes(r.getOpenAPI().getExtensions());
    }

    @Test
    public void extensionsKeepJackson2NumberTypes31() {
        SwaggerParseResult r = parse(HEAD31 + EXTENSIONS + "paths: {}\n");
        assertNotNull(r.getOpenAPI(), String.valueOf(r.getMessages()));
        assertExtensionTypes(r.getOpenAPI().getExtensions());
    }

    @Test
    public void extensionsInJsonInputKeepNumberTypes() {
        String json = "{\"openapi\":\"3.0.3\",\"info\":{\"title\":\"t\",\"version\":\"1\"},\"paths\":{},"
                + "\"x-int\":12,\"x-long\":12345678901,\"x-big\":123456789012345678901234567890,\"x-double\":1.5,"
                + "\"x-bool\":true,\"x-str\":\"s\",\"x-null\":null,\"x-list\":[1,\"a\"],\"x-obj\":{\"k\":1}}";
        assertExtensionTypes(parse(json).getOpenAPI().getExtensions());
    }

    // ---- enum entries and NullNode ----

    @Test
    public void schemaEnumKeepsNullEntry30() {
        SwaggerParseResult r = parse(HEAD + "paths: {}\ncomponents:\n  schemas:\n    S:\n      type: string\n      enum: [a, null, b]\n");
        Schema s = r.getOpenAPI().getComponents().getSchemas().get("S");
        assertEquals(s.getEnum().size(), 3);
        assertEquals(s.getEnum().get(0), "a");
        assertNull(s.getEnum().get(1));
        assertEquals(s.getEnum().get(2), "b");
    }

    @Test
    public void schemaEnumKeepsNullEntry31() {
        SwaggerParseResult r = parse(HEAD31 + "paths: {}\ncomponents:\n  schemas:\n    S:\n      type: string\n      enum: [a, null, b]\n");
        Schema s = r.getOpenAPI().getComponents().getSchemas().get("S");
        assertEquals(s.getEnum().size(), 3);
        assertNull(s.getEnum().get(1));
    }

    @Test
    public void schemaEnumMixedTypes30() {
        SwaggerParseResult r = parse(HEAD + "paths: {}\ncomponents:\n  schemas:\n    S:\n      enum: [1, 1.5, true, x, null]\n");
        List<?> e = r.getOpenAPI().getComponents().getSchemas().get("S").getEnum();
        assertEquals(e.size(), 5);
        assertEquals(e.get(0), 1);
        assertEquals(e.get(1), 1.5);
        assertEquals(e.get(2), Boolean.TRUE);
        assertEquals(e.get(3), "x");
        assertNull(e.get(4));
    }

    @Test
    public void serverVariableEnumUsesJackson2Text() {
        // asText() on a NullNode gave "null" in Jackson 2; Jackson 3 asString() gives "".
        SwaggerParseResult r = parse(HEAD + "paths: {}\nservers:\n  - url: http://{v}\n    variables:\n      v:\n        default: a\n        enum: [a, 1, true, null]\n");
        List<String> e = r.getOpenAPI().getServers().get(0).getVariables().get("v").getEnum();
        assertEquals(e, List.of("a", "1", "true", "null"));
    }

    // ---- other NullNode / non-string sites ----

    @Test
    public void linkParametersUseJackson2Text() {
        String spec = HEAD + "paths:\n  /a:\n    get:\n      responses:\n        '200':\n          description: ok\n"
                + "          links:\n            L:\n              operationId: x\n              parameters:\n"
                + "                p1: 12\n                p2: true\n                p3: null\n                p4: {a: 1}\n                p5: [1]\n                p6: abc\n";
        SwaggerParseResult r = parse(spec);
        Map<String, String> p = r.getOpenAPI().getPaths().get("/a").getGet().getResponses().get("200").getLinks().get("L").getParameters();
        assertEquals(p.get("p1"), "12");
        assertEquals(p.get("p2"), "true");
        assertEquals(p.get("p3"), "null");
        // Same as Jackson 2 asText(): "" for object and array. (Swagger Core 3 rejects these values with an error.)
        assertEquals(p.get("p4"), "");
        assertEquals(p.get("p5"), "");
        assertEquals(p.get("p6"), "abc");
    }

    @Test
    public void securityRequirementScopesUseJackson2Text() {
        String spec = HEAD + "security:\n  - oauth: [read, 5, null]\npaths: {}\n";
        List<String> scopes = parse(spec).getOpenAPI().getSecurity().get(0).get("oauth");
        assertEquals(scopes, List.of("read", "5", "null"));
    }

    @Test
    public void requiredWithNullEntryReportsInvalidTypeAndKeepsStrings() {
        SwaggerParseResult r = parse(HEAD + "paths: {}\ncomponents:\n  schemas:\n    S:\n      type: object\n      properties:\n        a: {type: string}\n      required: [a, null, 3]\n");
        Schema s = r.getOpenAPI().getComponents().getSchemas().get("S");
        assertEquals(s.getRequired(), List.of("a"));
        assertTrue(r.getMessages().stream().anyMatch(m -> m.contains("required")), r.getMessages().toString());
    }

    @Test
    public void typeArrayWithNullInOpenApi31() {
        SwaggerParseResult r = parse(HEAD31 + "paths: {}\ncomponents:\n  schemas:\n    S:\n      type: [string, null]\n");
        Schema s = r.getOpenAPI().getComponents().getSchemas().get("S");
        assertEquals(s.getTypes(), new java.util.LinkedHashSet<>(List.of("string", "null")));
        assertTrue(r.getMessages() == null || r.getMessages().isEmpty(), String.valueOf(r.getMessages()));
    }

    @Test
    public void parameterNameAndMappingAcceptNonStringScalars() {
        SwaggerParseResult r = parse(HEAD + "paths:\n  /a:\n    get:\n      parameters:\n        - name: 12\n          in: query\n          schema: {type: string}\n      responses:\n        '200': {description: ok}\n");
        assertNotNull(r.getOpenAPI());
        assertEquals(r.getOpenAPI().getPaths().get("/a").getGet().getParameters().get(0).getName(), "12");
    }

    @Test
    public void tagsAndNonStringEntriesAreSkippedNotFailing() {
        SwaggerParseResult r = parse(HEAD + "paths:\n  /a:\n    get:\n      tags: [a, 1, null]\n      responses:\n        '200': {description: ok}\n");
        assertEquals(r.getOpenAPI().getPaths().get("/a").getGet().getTags(), List.of("a"));
    }

    @Test
    public void nonStringRefDoesNotThrow() {
        SwaggerParseResult r = parse(HEAD + "paths: {}\ncomponents:\n  schemas:\n    S:\n      $ref: 12\n");
        assertNotNull(r.getOpenAPI());
    }

    // ---- asString("null"): used where Jackson 2 asText() could see a null value node ----

    @Test
    public void asStringWithDefaultKeepsTheNullText() {
        ObjectMapper m = ObjectMapperFactory.createJson();
        assertEquals(m.readTree("\"abc\"").asString("null"), "abc");
        assertEquals(m.readTree("12").asString("null"), "12");
        assertEquals(m.readTree("1.5").asString("null"), "1.5");
        assertEquals(m.readTree("true").asString("null"), "true");
        assertEquals(m.readTree("null").asString("null"), "null");
    }

    // ---- InlineModelResolver signature (Json.pretty) ----

    @Test
    public void inlineModelsWithSameShapeShareOneGeneratedModel() {
        String spec = HEAD + "paths:\n"
                + "  /a:\n    get:\n      responses:\n        '200':\n          description: ok\n          content:\n            application/json:\n              schema:\n                type: object\n                properties:\n                  id: {type: integer}\n                  name: {type: string}\n"
                + "  /b:\n    get:\n      responses:\n        '200':\n          description: ok\n          content:\n            application/json:\n              schema:\n                type: object\n                properties:\n                  id: {type: integer}\n                  name: {type: string}\n"
                + "  /c:\n    get:\n      responses:\n        '200':\n          description: ok\n          content:\n            application/json:\n              schema:\n                type: object\n                properties:\n                  other: {type: boolean}\n";
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setFlatten(true);
        OpenAPI api = new OpenAPIV3Parser().readContents(spec, null, options).getOpenAPI();
        Map<String, Schema> schemas = api.getComponents().getSchemas();
        assertEquals(schemas.keySet(), new java.util.LinkedHashSet<>(List.of("inline_response_200", "inline_response_200_1")),
                "names must be the same as with Jackson 2");
        assertEquals(api.getPaths().get("/a").getGet().getResponses().get("200").getContent().get("application/json").getSchema().get$ref(),
                "#/components/schemas/inline_response_200");
        assertEquals(api.getPaths().get("/b").getGet().getResponses().get("200").getContent().get("application/json").getSchema().get$ref(),
                "#/components/schemas/inline_response_200");
        assertEquals(api.getPaths().get("/c").getGet().getResponses().get("200").getContent().get("application/json").getSchema().get$ref(),
                "#/components/schemas/inline_response_200_1");
    }

    @Test
    public void duplicateKeysStillReportedWhenParsingContents() {
        // Guard against "defaults lost": if a mapper is rebuilt without the factory settings, duplicates pass silently.
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        SwaggerParseResult json = new OpenAPIV3Parser().readContents(
                "{\"openapi\":\"3.0.3\",\"info\":{\"title\":\"t\",\"version\":\"1\"},\"paths\":{},\"paths\":{}}", null, options);
        assertTrue(json.getMessages().stream().anyMatch(m -> m.contains("Duplicate field 'paths'")), String.valueOf(json.getMessages()));
        SwaggerParseResult yaml = new OpenAPIV3Parser().readContents(HEAD + "paths: {}\npaths: {}\n", null, options);
        assertTrue(yaml.getMessages().stream().anyMatch(m -> m.contains("Duplicate field")), String.valueOf(yaml.getMessages()));
    }

    enum Color {
        RED;

        @Override
        public String toString() {
            return "red-text";
        }
    }

    @Test
    public void yamlFallbackMapperKeepsHeadSettings() throws Exception {
        // JSON_MAPPER_FOR_YAML had these settings at HEAD: enums via toString, BigDecimal as plain text, dates as text.
        java.lang.reflect.Field f = io.swagger.v3.parser.util.DeserializationUtils.class.getDeclaredField("JSON_MAPPER_FOR_YAML");
        f.setAccessible(true);
        ObjectMapper mapper = (ObjectMapper) f.get(null);
        String json = mapper.writeValueAsString(Map.of("e", Color.RED, "n", new java.math.BigDecimal("1E+3")));
        JsonNode tree = mapper.readTree(json);
        assertEquals(tree.get("e").stringValue(null), "red-text");
        assertTrue(json.contains("\"n\":1000"), json);
        assertTrue(mapper.writeValueAsString(java.time.LocalDate.of(2020, 1, 2)).contains("2020-01-02"));
    }
}
