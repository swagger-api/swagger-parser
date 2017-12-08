package io.swagger.v3.parser.test;


import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import io.swagger.v3.parser.OpenAPIV3Parser;
import org.testng.annotations.Test;

import static org.testng.Assert.assertNotNull;

public class OAIDeserializationTest {
    @Test
    public void testDeserializeSimpleDefinition() throws Exception {
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
        SwaggerParseResult result = new OpenAPIV3Parser().readContents(json, null, options);

        assertNotNull(result.getOpenAPI());
    }
}
