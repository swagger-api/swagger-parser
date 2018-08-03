package io.swagger.parser;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import io.swagger.v3.core.util.Json;
import org.junit.Test;
import java.util.Map;

import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class OpenAPIParserTest {
    @Test
    public void testIssue749() {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        SwaggerParseResult result = new OpenAPIParser().readLocation("issue749-main.yaml", null, options);
        assertNotNull(result);

        OpenAPI openAPI = result.getOpenAPI();
        assertNotNull(openAPI);

        Components components = openAPI.getComponents();
        assertNotNull(components);

        PathItem pathItem = openAPI.getPaths().get("/some/ping");
        assertNotNull(pathItem);
        List<Parameter> parameters = pathItem.getGet().getParameters();
        assertNotNull(parameters);
        assertEquals(parameters.size(), 1);
        assertEquals(parameters.get(0).getName(), "i");
        assertNotNull(parameters.get(0).getSchema());
        assertEquals(parameters.get(0).getSchema().get$ref(), "#/components/schemas/SomeId");

        Map<String, Schema> schemas = components.getSchemas();
        assertNotNull(schemas);
        assertEquals(schemas.size(), 1);
        assertNotNull(schemas.get("SomeId"));
    }

    @Test
    public void testSimple() {
        SwaggerParseResult result = new OpenAPIParser().readLocation("petstore.yaml", null, null);

        assertNotNull(result);
        assertNotNull(result.getOpenAPI());
        assertEquals(result.getOpenAPI().getOpenapi(), "3.0.1");
    }

    @Test
    public void testIssue768() {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        SwaggerParseResult result = new OpenAPIParser().readLocation("issue768-main.yaml", null, options);
        assertNotNull(result);

        OpenAPI openAPI = result.getOpenAPI();
        assertNotNull(openAPI);

        Components components = openAPI.getComponents();
        assertNotNull(components);

        Map<String, Schema> schemas = components.getSchemas();
        assertNotNull(schemas);

        assertEquals(schemas.size(), 1);
    }

    @Test
    public void test30Url() {
        String location = "http://petstore.swagger.io/v2/swagger.json";

        SwaggerParseResult result = new OpenAPIParser().readLocation(location, null, null);

        assertNotNull(result);
        assertNotNull(result.getOpenAPI());
        assertEquals(result.getOpenAPI().getOpenapi(), "3.0.1");
    }

    @Test
    public void test30() {
        String json =
            "{\n" +
            "  \"openapi\": \"3.0.1\",\n" +
            "  \"info\": {\n" +
            "    \"title\": \"Swagger Petstore\",\n" +
            "    \"description\": \"This is a sample server Petstore server. You can find out more about Swagger at [http://swagger.io](http://swagger.io) or on [irc.freenode.net, #swagger](http://swagger.io/irc/). For this sample, you can use the api key `special-key` to test the authorization filters.\",\n" +
            "    \"termsOfService\": \"http://swagger.io/terms/\",\n" +
            "    \"contact\": {\n" +
            "      \"email\": \"apiteam@swagger.io\"\n" +
            "    },\n" +
            "    \"license\": {\n" +
            "      \"name\": \"Apache 2.0\",\n" +
            "      \"url\": \"http://www.apache.org/licenses/LICENSE-2.0.html\"\n" +
            "    },\n" +
            "    \"version\": \"1.0.0\"\n" +
            "  }\n" +
            "}";

        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        SwaggerParseResult result = new OpenAPIParser().readContents(json, null, options);

        assertNotNull(result);
        assertNotNull(result.getOpenAPI());
        assertEquals(result.getOpenAPI().getOpenapi(), "3.0.1");
    }

    @Test
    public void testParsingPrettifiedExtensions() throws Exception {
        String json =
                "{\n" +
                        "  \"openapi\": \"3.0.1\",\n" +
                        "  \"x-some-extension\": \"some-value\"\n" +
                        "}";

        SwaggerParseResult result = new OpenAPIParser().readContents(json, null, null);
        assertNotNull(result);
        OpenAPI openAPI = result.getOpenAPI();
        assertNotNull(openAPI);
        assertNotNull(openAPI.getExtensions());
        assertEquals(openAPI.getExtensions().get("x-some-extension"), "some-value");

        String prettyJson = Json.pretty(openAPI);

        SwaggerParseResult prettyResult = new OpenAPIParser().readContents(prettyJson, null, null);
        assertNotNull(prettyResult);
        OpenAPI prettyOpenAPI = prettyResult.getOpenAPI();
        assertNotNull(prettyOpenAPI);
        assertNotNull(prettyOpenAPI.getExtensions());
        assertEquals(prettyOpenAPI.getExtensions().get("x-some-extension"), "some-value");
    }
}
