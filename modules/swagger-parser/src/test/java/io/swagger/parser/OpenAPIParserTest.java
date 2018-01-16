package io.swagger.parser;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import io.swagger.v3.core.util.Json;
import org.junit.Test;
import java.util.List;
import java.util.stream.Collectors;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class OpenAPIParserTest {
    @Test
    public void testSimple() {
        SwaggerParseResult result = new OpenAPIParser().readLocation("petstore.yaml", null, null);

        assertNotNull(result);
        assertNotNull(result.getOpenAPI());
        assertEquals(result.getOpenAPI().getOpenapi(), "3.0.1");
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

    @Test
    public void allowBooleanAdditionalPropertiesIssue499() {
        SwaggerParseResult result = new OpenAPIParser().readLocation("booleanAdditonalProperties.json", null, null);

        assertNotNull(result);
        assertNotNull(result.getOpenAPI());
        assertEquals(result.getOpenAPI().getOpenapi(), "3.0.0");
        List<String> messages = result.getMessages();
        assertTrue(messages.isEmpty(), messages.stream().collect(Collectors.joining("\n")));
    }
}
