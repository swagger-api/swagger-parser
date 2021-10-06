package io.swagger.v3.parser.test;

import io.swagger.v3.core.util.Yaml31;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.*;

public class OAI31DeserializationTest {
    @Test
    public void testDeserializeSimpleDefinition() throws Exception {
        String json =
                "{\n" +
                        "  \"openapi\": \"3.1.0\",\n" +
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

    @Test
    public void testBasic() {
        SwaggerParseResult result = new OpenAPIV3Parser().readLocation( "3.1.0/basic.yaml", null, null);
        //Yaml31.prettyPrint(result);
        //assertEquals(result.getMessages().size(),1);
        assertNotNull(result.getOpenAPI());
    }

    @Test
    public void testInfo() {
        String infoYaml = "openapi: 3.1.0\n" +
                "info:\n" +
                "  title: Swagger Petstore\n" +
                "  summary: test summary in info object\n" +
                "  description: \"This is a sample server Petstore server. You can find out more about\\\n" +
                "    \\ Swagger at [http://swagger.io](http://swagger.io) or on [irc.freenode.net, #swagger](http://swagger.io/irc/).\\\n" +
                "    \\ For this sample, you can use the api key `special-key` to test the authorization\\\n" +
                "    \\ filters.\"\n" +
                "  termsOfService: http://swagger.io/terms/\n" +
                "  contact:\n" +
                "    email: apiteam@swagger.io\n" +
                "  license:\n" +
                "    name: Apache 2.0\n" +
                "    url: http://www.apache.org/licenses/LICENSE-2.0.html\n" +
                "  version: 1.0.0\n" +
                "servers:\n" +
                "- url: /\n";
        SwaggerParseResult result = new OpenAPIV3Parser().readContents( infoYaml, null, null);
        assertNotNull(result.getOpenAPI());
        assertNotNull(result.getOpenAPI().getInfo().getSummary());
        assertFalse(result.getMessages().contains("attribute info.summary is unexpected"));
        Yaml31.prettyPrint(result.getMessages());
    }

    @Test
    public void testPathsItemsUnderComponents() {
        SwaggerParseResult result = new OpenAPIV3Parser().readLocation( "3.1.0/petstore-3.1_more.yaml", null, null);
        assertNotNull(result.getOpenAPI());
        assertNotNull(result.getOpenAPI().getComponents().getPathItems());
        assertFalse(result.getMessages().contains("attribute components.pathItems is unexpected"));
    }

    @Test
    public void testDiscriminatorExtensions() {
        SwaggerParseResult result = new OpenAPIV3Parser().readLocation( "3.1.0/petstore-3.1_more.yaml", null, null);
        assertNotNull(result.getOpenAPI());
        assertNotNull(result.getOpenAPI().getComponents().getSchemas().get("DiscriminatorExtension").getDiscriminator().getExtensions().get("x-extension"));
    }

    @Test
    public void testSecurityDeserialization() throws Exception {
        String yaml = "openapi: 3.1.0\n" +
                "security:\n" +
                "  - mutualTLS: []\n";

        OpenAPIV3Parser parser = new OpenAPIV3Parser();

        SwaggerParseResult result = parser.readContents(yaml, null, null);
        OpenAPI openAPI = result.getOpenAPI();
        assertNotNull(openAPI);
        List<SecurityRequirement> security = openAPI.getSecurity();
        assertTrue(security.size() == 1);
    }

    @Test
    public void testSecurityDeserialization2() throws Exception {
        OpenAPIV3Parser parser = new OpenAPIV3Parser();
        SwaggerParseResult result = parser.readLocation("3.1.0/securitySchemes31.yaml", null, null);
        OpenAPI openAPI = result.getOpenAPI();
        assertNotNull(openAPI);
        assertNotNull(openAPI.getComponents().getSecuritySchemes().get("mutual_TLS"));
    }

    @Test
    public void testOptionalPathsObject() {
        String infoYaml = "openapi: 3.1.0\n" +
                "info:\n" +
                "  title: Swagger Petstore\n" +
                "  summary: test summary in info object\n" +
                "  description: \"This is a sample server Petstore server. You can find out more about\\\n" +
                "    \\ Swagger at [http://swagger.io](http://swagger.io) or on [irc.freenode.net, #swagger](http://swagger.io/irc/).\\\n" +
                "    \\ For this sample, you can use the api key `special-key` to test the authorization\\\n" +
                "    \\ filters.\"\n" +
                "  termsOfService: http://swagger.io/terms/\n" +
                "  contact:\n" +
                "    email: apiteam@swagger.io\n" +
                "  license:\n" +
                "    name: Apache 2.0\n" +
                "    url: http://www.apache.org/licenses/LICENSE-2.0.html\n" +
                "  version: 1.0.0\n" +
                "servers:\n" +
                "- url: /\n";
        SwaggerParseResult result = new OpenAPIV3Parser().readContents( infoYaml, null, null);
        assertNotNull(result.getOpenAPI());
        assertFalse(result.getMessages().contains("attribute paths is missing"));
    }

    @Test
    public void testValidOpenAPIDocument() {
        String infoYaml = "openapi: 3.1.0\n" +
                "info:\n" +
                "  title: Swagger Petstore\n" +
                "  summary: test summary in info object\n" +
                "  description: \"This is a sample server Petstore server. You can find out more about\\\n" +
                "    \\ Swagger at [http://swagger.io](http://swagger.io) or on [irc.freenode.net, #swagger](http://swagger.io/irc/).\\\n" +
                "    \\ For this sample, you can use the api key `special-key` to test the authorization\\\n" +
                "    \\ filters.\"\n" +
                "  termsOfService: http://swagger.io/terms/\n" +
                "  contact:\n" +
                "    email: apiteam@swagger.io\n" +
                "  license:\n" +
                "    name: Apache 2.0\n" +
                "    url: http://www.apache.org/licenses/LICENSE-2.0.html\n" +
                "  version: 1.0.0\n" +
                "servers:\n" +
                "- url: /\n";
        SwaggerParseResult result = new OpenAPIV3Parser().readContents( infoYaml, null, null);
        assertNotNull(result.getOpenAPI());
        assertFalse(result.getMessages().contains("attribute paths is missing"));
        assertTrue(result.getMessages().contains("The OpenAPI document MUST contain at least one paths field, a components field or a webhooks field"));
    }

    @Test
    public void testReservedExtensions() {
        SwaggerParseResult result = new OpenAPIV3Parser().readLocation( "3.1.0/basic.yaml", null, null);
        assertNotNull(result.getOpenAPI());
        assertTrue(result.getMessages().contains("attribute x-oas-internal is reserved by The OpenAPI Iniciative"));
        assertTrue(result.getMessages().contains("attribute x-oai-extension is reserved by The OpenAPI Iniciative"));
    }
}
