package io.swagger.v3.parser.test;

import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.core.util.Yaml31;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.JsonSchema;
import io.swagger.v3.oas.models.media.Schema;
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
        //assertEquals(result.getMessages().size(),1);
        assertNotNull(result.getOpenAPI());
    }

    @Test
    public void testOAS31() {
        SwaggerParseResult result = new OpenAPIV3Parser().readLocation( "3.1.0/oas3.1.yaml", null, null);
        assertNotNull(result.getOpenAPI());
    }

    @Test
    public void testJsonSchemaDialectValid() {
        String jsonSchemaDialect = "openapi: 3.1.0\n" +
                "jsonSchemaDialect: https://json-schema.org/draft/2020-12/schema\n" +
                "info:\n" +
                "  title: Swagger Petstore\n" +
                "  version: 1.0.0\n" +
                "paths: {}";
        SwaggerParseResult result = new OpenAPIV3Parser().readContents( jsonSchemaDialect, null, null);
        assertNotNull(result.getOpenAPI());
        assertNotNull(result.getOpenAPI().getJsonSchemaDialect());
        assertEquals(result.getOpenAPI().getJsonSchemaDialect(), "https://json-schema.org/draft/2020-12/schema");
    }

    @Test
    public void testJsonSchemaDialectInvalid() {
        String jsonSchemaDialect = "openapi: 3.1.0\n" +
                "jsonSchemaDialect: bad URI\n" +
                "info:\n" +
                "  title: Swagger Petstore\n" +
                "  version: 1.0.0\n" +
                "paths: {}";
        SwaggerParseResult result = new OpenAPIV3Parser().readContents( jsonSchemaDialect, null, null);
        assertNotNull(result.getOpenAPI());
        assertTrue(result.getMessages().contains("jsonSchemaDialect. Invalid url: bad URI"));


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
                "  version: 1.0.0\n";
        SwaggerParseResult result = new OpenAPIV3Parser().readContents( infoYaml, null, null);
        assertNotNull(result.getOpenAPI());
        assertFalse(result.getMessages().contains("attribute paths is missing"));
    }

    @Test
    public void testValidOpenAPIDocument() {
        String api = "openapi: 3.1.0\n" +
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
        SwaggerParseResult result = new OpenAPIV3Parser().readContents( api, null, null);
        assertNotNull(result.getOpenAPI());
        assertFalse(result.getMessages().contains("attribute paths is missing"));
        assertTrue(result.getMessages().contains("The OpenAPI document MUST contain at least one paths field, a components field or a webhooks field"));
    }

    @Test
    public void testReservedExtensionsOaiAuthorFalse() {
        SwaggerParseResult result = new OpenAPIV3Parser().readLocation( "3.1.0/basic.yaml", null, null);
        assertNotNull(result.getOpenAPI());
        assertTrue(result.getMessages().contains("attribute x-oas-internal is reserved by The OpenAPI Initiative"));
        assertTrue(result.getMessages().contains("attribute x-oai-extension is reserved by The OpenAPI Initiative"));
    }

    @Test
    public void testReservedExtensionsOaiAuthorTrue() {
        ParseOptions options = new ParseOptions();
        options.setOaiAuthor(true);
        SwaggerParseResult result = new OpenAPIV3Parser().readLocation( "3.1.0/basic.yaml", null, options);
        assertNotNull(result.getOpenAPI());
        assertFalse(result.getMessages().contains("attribute x-oas-internal is reserved by The OpenAPI Initiative"));
        assertFalse(result.getMessages().contains("attribute x-oai-extension is reserved by The OpenAPI Initiative"));
    }

    @Test
    public void testSiblingsReferenceObjects() {
        ParseOptions options = new ParseOptions();
        options.setOaiAuthor(true);
        SwaggerParseResult result = new OpenAPIV3Parser().readLocation( "3.1.0/siblings31.yaml", null, options);
        OpenAPI openAPI = result.getOpenAPI();
        assertNotNull(openAPI);

        //PathItems
        assertTrue(openAPI.getPaths().get("/pets").get$ref() != null
                && openAPI.getPaths().get("/pets").getDescription() != null
                && openAPI.getPaths().get("/pets").getSummary() != null);

        //Parameters
        assertTrue(openAPI.getPaths().get("/pets/{petId}").getGet().getParameters().get(0).get$ref() != null
                && openAPI.getPaths().get("/pets/{petId}").getGet().getParameters().get(0).getDescription() != null);

        //Responses
        assertTrue(openAPI.getPaths().get("/pets/{petId}").getGet().getResponses().get("200").get$ref() != null
                && openAPI.getPaths().get("/pets/{petId}").getGet().getResponses().get("200").getDescription() != null);

        //RequestBody
        assertTrue(openAPI.getPaths().get("/pets/requestBody").getPost().getRequestBody().get$ref() != null
                && openAPI.getPaths().get("/pets/requestBody").getPost().getRequestBody().getDescription() != null);

        //Headers
        assertTrue(openAPI.getPaths().get("/pets/requestBody").getPost().getResponses().get("200").getHeaders().get("X-Rate-Limit").get$ref() != null
                && openAPI.getPaths().get("/pets/requestBody").getPost().getResponses().get("200").getHeaders().get("X-Rate-Limit").getDescription() != null);

        //Links
        assertTrue(openAPI.getPaths().get("/pets/requestBody").getPost().getResponses().get("200").getLinks().get("userRepository").get$ref() != null
                && openAPI.getPaths().get("/pets/requestBody").getPost().getResponses().get("200").getLinks().get("userRepository").getDescription() != null);

        //SecuritySchemes
        assertTrue(openAPI.getComponents().getSecuritySchemes().get("api_key").get$ref() != null
                && openAPI.getComponents().getSecuritySchemes().get("api_key").getDescription() != null);
    }

    @Test(description = "Test siblings with $ref for maxItems, properties, description, required")
    public void testSiblingsReferenceJSONSchema1() {
        ParseOptions options = new ParseOptions();
        String refSibling = "openapi: 3.1.0\n" +
                "info:\n" +
                "  title: siblings JSONSchema\n" +
                "  version: 1.0.0\n" +
                "servers:\n" +
                "  - url: /\n" +
                "paths: { }\n" +
                "components:\n" +
                "  schemas:\n" +
                "    profile:\n" +
                "      description: siblings refs\n" +
                "      required:\n" +
                "        - login\n" +
                "        - password\n" +
                "      maxItems: 2\n" +
                "      $ref: ./ex.json#user-profile\n" +
                "      properties:\n" +
                "        login:\n" +
                "          type: string\n" +
                "        password:\n" +
                "          type: string";
        SwaggerParseResult result = new OpenAPIV3Parser().readContents( refSibling , null, options);
        OpenAPI openAPI = result.getOpenAPI();
        assertNotNull(openAPI);
        Schema profile = openAPI.getComponents().getSchemas().get("profile");
        assertNotNull(profile.get$ref());
        assertTrue(profile.getMaxItems()==2);
        assertEquals(profile.getDescription(),"siblings refs");
        assertTrue(profile.getRequired().size()==2);
        assertTrue(profile.getProperties().containsKey("login"));
        assertTrue(profile.getProperties().containsKey("password"));
    }

    @Test(description = "Test siblings with $ref for patternProperties, pattern, additionalProperties")
    public void testSiblingsReferenceJSONSchema2() {
        ParseOptions options = new ParseOptions();
        String refSibling = "openapi: 3.1.0\n" +
                "info:\n" +
                "  title: siblings JSONschema\n" +
                "  version: 1.0.0\n" +
                "servers:\n" +
                "  - url: /\n" +
                "paths: { }\n" +
                "components:\n" +
                "  schemas:\n" +
                "    profile:\n" +
                "      $ref: ./ex.json#user-profile\n" +
                "      pattern: \\d\\d\\d\\d-\\d\\d-\\d\\d\n" +
                "      patternProperties:\n" +
                "        \"^S_\":\n" +
                "          type: string\n" +
                "        \"^I_\":\n" +
                "          type: integer\n" +
                "      additionalProperties: false";
        SwaggerParseResult result = new OpenAPIV3Parser().readContents( refSibling , null, options);
        OpenAPI openAPI = result.getOpenAPI();
        assertNotNull(openAPI);
        Schema profile = openAPI.getComponents().getSchemas().get("profile");
        assertNotNull(profile.get$ref());
        assertEquals(profile.getPattern(),"\\d\\d\\d\\d-\\d\\d-\\d\\d");
        assertNotNull(profile.getAdditionalProperties());
        assertTrue(profile.getPatternProperties().containsKey("^S_"));
    }

    @Test(description = "Test siblings with $ref for patternProperties, pattern, additionalProperties")
    public void testSiblingsReferenceJSONSchema3() {
        ParseOptions options = new ParseOptions();
        String refSibling = "openapi: 3.1.0\n" +
                "info:\n" +
                "  title: siblings JSONschema\n" +
                "  version: 1.0.0\n" +
                "servers:\n" +
                "  - url: /\n" +
                "paths: { }\n" +
                "components:\n" +
                "  schemas:\n" +
                "    profile:\n" +
                "      $id: profile-id\n" +
                "      $anchor: foo\n" +
                "      $schema: https://json-schema.org/draft/2020-12/schema\n" +
                "      $comment: end user should not see this comment\n" +
                "      type:\n" +
                "        - string\n" +
                "        - integer\n" +
                "      exclusiveMaximum: 12\n" +
                "      exclusiveMinimum: 1\n" +
                "      $ref: ./ex.json#user-profile";
        SwaggerParseResult result = new OpenAPIV3Parser().readContents( refSibling , null, options);
        OpenAPI openAPI = result.getOpenAPI();
        assertNotNull(openAPI);
        Schema profile = openAPI.getComponents().getSchemas().get("profile");
        assertNotNull(profile.get$ref());
        assertEquals(profile.get$anchor(),"foo");
        assertEquals(profile.get$id(),"profile-id");
        assertTrue(profile.getExclusiveMaximumValue().intValue()==12);
        assertTrue(profile.getExclusiveMinimumValue().intValue()==1);
        assertEquals(profile.get$comment(),"end user should not see this comment");
    }
}
