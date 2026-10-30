package io.swagger.v3.parser;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static org.testng.Assert.*;

/**
 * Shows what a user of the parser sees after the move to Jackson 3.
 * Every test goes through the public parser API and matches a section of MIGRATION.md.
 */
public class MigrationUserImpactTest {

    private static final String HEAD = "openapi: 3.0.3\ninfo:\n  title: t\n  version: '1'\n";
    private static final String JSON_DOC = "{\"openapi\":\"3.0.3\",\"info\":{\"title\":\"t\",\"version\":\"1\"},\"paths\":{}}";

    private static SwaggerParseResult parse(String content) {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        return new OpenAPIV3Parser().readContents(content, null, options);
    }

    @Test
    public void duplicateJsonKeyMessageKeeps2xFormat() {
        SwaggerParseResult r = parse("{\"openapi\":\"3.0.3\",\"info\":{\"title\":\"t\",\"version\":\"1\"},\"paths\":{\"/a\":{},\"/a\":{}}}");
        assertNull(r.getOpenAPI());
        assertEquals(r.getMessages().size(), 1);
        assertTrue(r.getMessages().get(0).startsWith("Duplicate field '/a'"), r.getMessages().toString());
    }

    @Test
    public void duplicateYamlKeyMessageIsNotChanged() {
        SwaggerParseResult r = parse(HEAD + "paths:\n  /a: {}\n  /a: {}\n");
        assertTrue(r.getMessages().stream().anyMatch(m -> m.startsWith("Duplicate field /a")), r.getMessages().toString());
    }

    @Test
    public void matchingOnTheWordDuplicateWorksForJsonAndYaml() {
        SwaggerParseResult json = parse(JSON_DOC.replace("\"paths\":{}", "\"paths\":{},\"paths\":{}"));
        SwaggerParseResult yaml = parse(HEAD + "paths: {}\npaths: {}\n");
        assertTrue(json.getMessages().get(0).startsWith("Duplicate"), json.getMessages().toString());
        assertTrue(yaml.getMessages().get(0).startsWith("Duplicate"), yaml.getMessages().toString());
    }

    // ---- MIGRATION.md: "Maximum nesting depth is 500" ----

    private static String nested(int depth) {
        return "{\"a\":".repeat(depth) + "1" + "}".repeat(depth);
    }

    @Test
    public void jsonNestedBelowTheLimitIsParsed() {
        SwaggerParseResult r = parse(JSON_DOC.replace("\"paths\":{}", "\"paths\":{},\"x-deep\":" + nested(300)));
        assertNotNull(r.getOpenAPI(), String.valueOf(r.getMessages()));
        assertTrue(r.getOpenAPI().getExtensions().containsKey("x-deep"));
    }

    @Test
    public void jsonNestedAboveTheLimitGivesAnErrorMessageNotAnException() {
        // Jackson 2 accepted depth up to 1000.
        SwaggerParseResult r = parse(JSON_DOC.replace("\"paths\":{}", "\"paths\":{},\"x-deep\":" + nested(600)));
        assertNull(r.getOpenAPI());
        assertTrue(r.getMessages().get(0).contains("nesting depth"), r.getMessages().toString());
    }

    @Test
    public void yamlNestedAboveTheLimitGivesAnErrorMessage() {
        String deep = "{a: ".repeat(600) + "1" + "}".repeat(600);
        SwaggerParseResult r = parse(HEAD + "paths: {}\nx-deep: " + deep + "\n");
        assertNull(r.getOpenAPI());
        assertTrue(r.getMessages().get(0).contains("nesting depth"), r.getMessages().toString());
    }

    // ---- Jackson 3 default FAIL_ON_TRAILING_TOKENS (mapper defaults task) ----

    @Test
    public void textAfterTheJsonDocumentIsRejected() {
        // Jackson 2 ignored the extra content. Jackson 3 rejects it by default.
        SwaggerParseResult r = parse(JSON_DOC + "{}");
        assertNull(r.getOpenAPI());
        assertTrue(r.getMessages().get(0).contains("Trailing token"), r.getMessages().toString());
    }

    // ---- MIGRATION.md: "Text of null, object and array values" ----

    @Test
    public void serverVariableEnumEntriesKeepJackson2Text() {
        SwaggerParseResult r = parse(HEAD + "paths: {}\nservers:\n  - url: http://{v}\n    variables:\n      v:\n        default: a\n        enum: [a, 12, true, null]\n");
        List<String> e = r.getOpenAPI().getServers().get(0).getVariables().get("v").getEnum();
        assertEquals(e, List.of("a", "12", "true", "null"));
    }

    @Test
    public void schemaEnumNullEntryStaysNull() {
        // Different from the server variable enum above: the schema enum keeps a real null.
        SwaggerParseResult r = parse(HEAD + "paths: {}\ncomponents:\n  schemas:\n    S:\n      type: string\n      enum: [a, null]\n");
        Schema<?> s = r.getOpenAPI().getComponents().getSchemas().get("S");
        assertEquals(s.getEnum().size(), 2);
        assertNull(s.getEnum().get(1));
    }

    @Test
    public void linkParameterObjectAndArrayGiveEmptyText() {
        String spec = HEAD + "paths:\n  /a:\n    get:\n      responses:\n        '200':\n          description: ok\n"
                + "          links:\n            L:\n              operationId: x\n              parameters:\n"
                + "                n: null\n                o: {a: 1}\n                arr: [1]\n                num: 5\n";
        Map<String, String> p = parse(spec).getOpenAPI().getPaths().get("/a").getGet().getResponses().get("200").getLinks().get("L").getParameters();
        assertEquals(p.get("n"), "null");
        assertEquals(p.get("o"), "");
        assertEquals(p.get("arr"), "");
        assertEquals(p.get("num"), "5");
    }

    @Test
    public void discriminatorMappingKeepsJackson2Text() {
        SwaggerParseResult r = parse(HEAD + "paths: {}\ncomponents:\n  schemas:\n    P:\n      type: object\n      properties:\n        t: {type: string}\n"
                + "      discriminator:\n        propertyName: t\n        mapping:\n          a: null\n          b: {x: 1}\n          c: 7\n");
        Map<String, String> m = r.getOpenAPI().getComponents().getSchemas().get("P").getDiscriminator().getMapping();
        assertEquals(m.get("a"), "null");
        assertEquals(m.get("b"), "");
        assertEquals(m.get("c"), "7");
    }

    @Test
    public void securityRequirementScopesKeepJackson2Text() {
        SwaggerParseResult r = parse(HEAD + "security:\n  - oauth: [read, 5, null, {a: 1}]\npaths: {}\n");
        assertEquals(r.getOpenAPI().getSecurity().get(0).get("oauth"), List.of("read", "5", "null", ""));
    }

    @Test
    public void openApi31TypeListKeepsNullTypeText() {
        SwaggerParseResult r = parse("openapi: 3.1.0\ninfo:\n  title: t\n  version: '1'\npaths: {}\ncomponents:\n  schemas:\n    S:\n      type: [string, null]\n");
        OpenAPI api = r.getOpenAPI();
        assertTrue(api.getComponents().getSchemas().get("S").getTypes().contains("null"));
    }

    @Test
    public void nonStringParameterNameIsReadAsText() {
        SwaggerParseResult r = parse(HEAD + "paths:\n  /a:\n    get:\n      parameters:\n        - name: 12\n          in: query\n          schema: {type: string}\n      responses:\n        '200': {description: ok}\n");
        assertEquals(r.getOpenAPI().getPaths().get("/a").getGet().getParameters().get(0).getName(), "12");
    }
}
