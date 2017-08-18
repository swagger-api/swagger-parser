package io.swagger.parser;

import io.swagger.parser.models.ParseOptions;
import io.swagger.parser.models.SwaggerParseResult;
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
        assertEquals(result.getOpenAPI().getOpenapi(), "3.0.0");
    }

    @Test
    public void test30Url() {
        String location = "http://petstore.swagger.io/v2/swagger.json";

        SwaggerParseResult result = new OpenAPIParser().readLocation(location, null, null);

        assertNotNull(result);
        assertNotNull(result.getOpenAPI());
        assertEquals(result.getOpenAPI().getOpenapi(), "3.0.0");
    }

    @Test
    public void test30() {
        String json =
            "{\n" +
            "  \"openapi\": \"3.0.0-rc1\",\n" +
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
        assertEquals(result.getOpenAPI().getOpenapi(), "3.0.0-rc1");
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
