package io.swagger.parser;

import io.swagger.parser.models.SwaggerParseResult;
import org.junit.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class OpenAPIParserTest {
    @Test
    public void testSimple() {
        SwaggerParseResult result = new OpenAPIParser().readLocation("petstore.yaml", null, null);

        assertNotNull(result);
        assertNotNull(result.getOpenAPI());
        assertEquals(result.getOpenAPI().getOpenapi(), "3.0.0-rc0");
    }

    @Test
    public void test30() {
        String yaml =
            "{\n" +
            "  \"openapi\": \"3.0.0-rc0\",\n" +
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

        SwaggerParseResult result = new OpenAPIParser().readContents(yaml, null, null);

        assertNotNull(result);
        assertNotNull(result.getOpenAPI());
        assertEquals(result.getOpenAPI().getOpenapi(), "3.0.0-rc0");
    }
}
