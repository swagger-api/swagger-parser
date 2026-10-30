package io.swagger.v3.parser;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import io.swagger.v3.parser.util.NodeTextUtils;
import org.testng.annotations.Test;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.*;

/**
 * Small tests for each decision taken while moving from Jackson 2 to Jackson 3.
 * The expected values are the results Jackson 2 gave before the move.
 */
public class Jackson3MigrationBehaviourTest {

    private static SwaggerParseResult parse(String content) {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        return new OpenAPIV3Parser().readContents(content, null, options);
    }

    private static final String HEAD = "openapi: 3.0.3\ninfo:\n  title: t\n  version: '1'\n";
    private static final String HEAD31 = "openapi: 3.1.0\ninfo:\n  title: t\n  version: '1'\n";

    // ---- ObjectMapperFactory settings ----

    @Test
    public void jsonMapperAllowsTrailingTokens() {
        // Jackson 3 fails on trailing text by default. Jackson 2 did not.
        JsonNode node = ObjectMapperFactory.createJson().readTree("{\"a\":1} trailing");
        assertEquals(node.get("a").intValue(), 1);
    }

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
            assertEquals(OpenAPIV3Parser.toLegacyMessage(e.getOriginalMessage()), "Duplicate field '200'");
        }
        try {
            ObjectMapperFactory.createYaml().readTree("a: 1\na: 2\n");
            fail("duplicate key must fail");
        } catch (StreamReadException e) {
            assertTrue(e.getOriginalMessage().contains("Duplicate"));
        }
    }

    @Test
    public void duplicateMessageIsRewrittenToJackson2Text() {
        assertEquals(OpenAPIV3Parser.toLegacyMessage("Duplicate Object property \"200\""), "Duplicate field '200'");
        assertEquals(OpenAPIV3Parser.toLegacyMessage("Other message"), "Other message");
        assertNull(OpenAPIV3Parser.toLegacyMessage(null));
    }

    public static class Bean {
        public Integer c = 3;
        public String a = null;
        public Integer b = 1;
    }

    @Test
    public void beansAreNotSortedAndNullsAreSkipped() {
        // Jackson 3 sorts bean properties by default; Jackson 2 used declaration order.
        assertEquals(ObjectMapperFactory.createJson().writeValueAsString(new Bean()), "{\"c\":3,\"b\":1}");
    }

    @Test
    public void unknownPropertiesAndEmptyBeansDoNotFail() {
        ObjectMapper mapper = ObjectMapperFactory.createJson();
        assertEquals(mapper.writeValueAsString(new Object()), "{}");
        assertNotNull(mapper.readValue("{\"x\":1}", Map.class));
    }

    @Test
    public void deepJsonUpToJackson2LimitIsAccepted() {
        int depth = 900;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < depth; i++) sb.append("{\"a\":");
        sb.append("1");
        for (int i = 0; i < depth; i++) sb.append("}");
        assertNotNull(ObjectMapperFactory.createJson().readTree(sb.toString()));
    }

    // ---- extensions: convertValue(node, Object.class) ----

    private void assertExtensionTypes(Map<String, Object> ext) {
        assertEquals(ext.get("x-int"), Integer.valueOf(12));
        assertEquals(ext.get("x-long"), Long.valueOf(12345678901L));
        assertEquals(ext.get("x-big"), new BigInteger("123456789012345678901234567890"));
        assertEquals(ext.get("x-double"), Double.valueOf(1.5));
        assertEquals(ext.get("x-bool"), Boolean.TRUE);
        assertEquals(ext.get("x-str"), "s");
        assertTrue(ext.containsKey("x-null"));
        assertNull(ext.get("x-null"));
        assertEquals(ext.get("x-list"), List.of(1, "a"));
        assertEquals(ext.get("x-obj"), Map.of("k", 1));
    }

    private static final String EXTENSIONS =
            "x-int: 12\nx-long: 12345678901\nx-big: 123456789012345678901234567890\nx-double: 1.5\n"
                    + "x-bool: true\nx-str: s\nx-null: null\nx-list: [1, a]\nx-obj: {k: 1}\n";

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
        assertEquals(e.get(1), Double.valueOf(1.5));
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

    // ---- NodeTextUtils (used where Jackson 2 asText() could see any node type) ----

    @Test
    public void nodeTextUtilsMatchesJackson2AsText() {
        ObjectMapper m = ObjectMapperFactory.createJson();
        assertEquals(NodeTextUtils.asText(m.readTree("\"abc\"")), "abc");
        assertEquals(NodeTextUtils.asText(m.readTree("12")), "12");
        assertEquals(NodeTextUtils.asText(m.readTree("1.5")), "1.5");
        assertEquals(NodeTextUtils.asText(m.readTree("true")), "true");
        assertEquals(NodeTextUtils.asText(m.readTree("null")), "null");
        assertEquals(NodeTextUtils.asText(m.readTree("{\"a\":1}")), "");
        assertEquals(NodeTextUtils.asText(m.readTree("[1]")), "");
        assertEquals(NodeTextUtils.asText(m.readTree("{}").path("x")), "");
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

    // ---- mapper immutability and lost defaults ----

    @Test
    public void mappersAreImmutableAndFactoryGivesFreshInstances() {
        ObjectMapper json = ObjectMapperFactory.createJson();
        assertNotSame(json, ObjectMapperFactory.createJson());
        // Jackson 3 mappers have no configure(...). A rebuilt mapper is a copy: the original keeps its settings.
        ObjectMapper copy = json.rebuild()
                .configure(tools.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS, true).build();
        assertNotSame(json, copy);
        assertFalse(json.isEnabled(tools.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS));
        assertTrue(copy.isEnabled(tools.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS));
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
