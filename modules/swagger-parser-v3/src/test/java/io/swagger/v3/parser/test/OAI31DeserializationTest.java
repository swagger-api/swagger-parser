package io.swagger.v3.parser.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.testng.Assert.*;

public class OAI31DeserializationTest {

    @Test(description = "Test OAS31 new Schema keys deserialization")
    public void testSchemaKeysOAS31() {
        SwaggerParseResult result = new OpenAPIV3Parser().readLocation( "3.1.0/petstore-3.1_more.yaml", null, null);
        assertNotNull(result.getOpenAPI());
        OpenAPI openAPI = result.getOpenAPI();

        assertTrue(result.getMessages().size() == 0);
        Schema schema2020_12 = openAPI.getComponents().getSchemas().get("Schema2020_12");
        assertEquals(schema2020_12.getConst(), "const text");
        assertEquals(schema2020_12.get$id(), "schemaId");
        assertEquals(schema2020_12.get$comment(), "comment for testing");
        assertNotNull(schema2020_12.getIf().getProperties().get("country"));
        assertNotNull(schema2020_12.getThen().getProperties().get("maple_trees"));
        assertNotNull(schema2020_12.getElse().getProperties().get("accept"));
        assertTrue(schema2020_12.getUnevaluatedProperties() instanceof Schema);
        Schema unevaluatedProperty = (Schema)schema2020_12.getUnevaluatedProperties();
        assertTrue(unevaluatedProperty.getTypes().contains("object"));
        assertEquals(schema2020_12.getContentMediaType(), "text/html");
        assertEquals(schema2020_12.get$anchor(), "anchor text");
        assertEquals(schema2020_12.get$schema(), "https://json-schema.org/draft/2020-12/schema");
        assertNotNull(schema2020_12.getExamples().get(0));
        assertEquals(schema2020_12.getContentEncoding(), "base64");
        assertEquals(schema2020_12.getMinContains(), Integer.valueOf(2));
        assertEquals(schema2020_12.getMaxContains(), Integer.valueOf(4));
        assertTrue(schema2020_12.getPrefixItems().get(0) instanceof Schema);
        Schema prefixItems = (Schema)schema2020_12.getPrefixItems().get(0);
        assertEquals(prefixItems.getDescription(), "Name");
        assertTrue(schema2020_12.getContains().getTypes().contains("integer"));
        assertTrue(schema2020_12.getContentSchema().getTypes().contains("string"));
        assertEquals(schema2020_12.getPropertyNames().getPattern(), "^[A-Za-z_][A-Za-z0-9_]*$");
        assertTrue(schema2020_12.getDependentSchemas().get("credit_card") instanceof Schema);
        Schema dependantSchema = (Schema)schema2020_12.getDependentSchemas().get("credit_card");
        assertEquals(dependantSchema.getRequired().get(0), "billing_address");
        assertTrue(schema2020_12.getDependentRequired().containsKey("credit_card"));
        assertTrue(schema2020_12.getPatternProperties().get("^S_") instanceof Schema);
        Schema patternProperties = (Schema)schema2020_12.getPatternProperties().get("^S_");
        assertTrue(schema2020_12.getUnevaluatedItems().getTypes().contains("object"));
        assertTrue(patternProperties.getTypes().contains("string"));
    }

    @Test(description = "Test OAS31 Schema const deserialization")
    public void testSchemaConstOAS31() {
        SwaggerParseResult result = new OpenAPIV3Parser().readLocation( "3.1.0/issue-1975.yaml", null, null);
        assertNotNull(result.getOpenAPI());
        OpenAPI openAPI = result.getOpenAPI();

        assertTrue(result.getMessages().size() == 0);
        assertEquals(openAPI.getComponents().getSchemas().get("ConstValidation").getConst(), 2);
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode objNode = mapper.createObjectNode();
        objNode.put("foo", "bar");
        objNode.put("baz", "bax");
        assertEquals(openAPI.getComponents().getSchemas().get("ConstWithObject").getConst(), objNode);
        ArrayNode arrayNode = mapper.createArrayNode();
        ObjectNode arrayItem = mapper.createObjectNode();
        arrayItem.put("foo", "bar");
        arrayNode.add(arrayItem);
        assertEquals(openAPI.getComponents().getSchemas().get("ConstWithArray").getConst(), arrayNode);
        assertEquals(openAPI.getComponents().getSchemas().get("ConstWithNull").getConst(), NullNode.getInstance());
        assertEquals(openAPI.getComponents().getSchemas().get("ConstWithFalseDoesNotMatch0").getConst(), false);
        assertEquals(openAPI.getComponents().getSchemas().get("ConstWithTrueDoesNotMatch1").getConst(), true);
        arrayNode = mapper.createArrayNode();
        arrayNode.add(false);
        assertEquals(openAPI.getComponents().getSchemas().get("ConstWithArrayFalseDoesNotMatch0").getConst(), arrayNode);
        arrayNode = mapper.createArrayNode();
        arrayNode.add(true);
        assertEquals(openAPI.getComponents().getSchemas().get("ConstWithArrayTrueDoesNotMatch1").getConst(), arrayNode);
        objNode = mapper.createObjectNode();
        objNode.put("a", false);
        assertEquals(openAPI.getComponents().getSchemas().get("ConstWithAFalseDoesNotMatchA0").getConst(), objNode);
        objNode = mapper.createObjectNode();
        objNode.put("a", true);
        assertEquals(openAPI.getComponents().getSchemas().get("ConstWithATrueDoesNotMatchA1").getConst(), objNode);
        assertEquals(openAPI.getComponents().getSchemas().get("ConstWith0DoesNotMatchOtherZeroLikeTypes").getConst(), 0);
        assertEquals(openAPI.getComponents().getSchemas().get("ConstWith1DoesNotMatchTrue").getConst(), 1);
        assertEquals(openAPI.getComponents().getSchemas().get("ConstWith20MatchesIntegerAndFloatTypes").getConst(), new BigDecimal("-2.0"));
        assertEquals(openAPI.getComponents().getSchemas().get("ConstFloatAndIntegersAreEqualUpTo64BitRepresentationLimits").getConst(), new BigDecimal(9007199254740992L));
        assertEquals(openAPI.getComponents().getSchemas().get("ConstNulCharactersInStrings").getConst(), "hello\0there");
    }

    @Test(description = "Test basic OAS31 deserialization/validation")
    public void testBasicOAS31() {
        SwaggerParseResult result = new OpenAPIV3Parser().readLocation( "3.1.0/test/basicOAS31.yaml", null, null);
        assertNotNull(result.getOpenAPI());
        OpenAPI openAPI = result.getOpenAPI();

        //JsonSchemaDialect
        assertNotNull(openAPI.getJsonSchemaDialect());
        //change to bad uri and retest to show the error message
        assertEquals(openAPI.getJsonSchemaDialect(), "https://json-schema.org/draft/2020-12/schema");
        //info: description and summary
        assertNotNull(openAPI.getInfo().getSummary());
        assertEquals(openAPI.getInfo().getSummary(), "test summary in info object");
        assertNotNull(openAPI.getInfo().getDescription());
        assertEquals(openAPI.getInfo().getDescription(), "description in info object");
        //license: identifier
        assertNotNull(openAPI.getInfo().getLicense().getIdentifier());
        assertEquals(openAPI.getInfo().getLicense().getIdentifier(), "test identifier");
        //pathItems under components
        assertNotNull(openAPI.getComponents().getPathItems());
        assertNotNull(openAPI.getComponents().getPathItems().get("pets"));
        //Type array without items
        assertTrue(openAPI.getComponents().getSchemas().get("ArrayWithoutItems").getTypes().contains("array"));
        assertNull(openAPI.getComponents().getSchemas().get("ArrayWithoutItems").getItems());
        assertFalse(result.getMessages().contains("attribute components.schemas.ArrayWithoutItems.items is missing"));
        //Type object with items
        assertTrue(openAPI.getComponents().getSchemas().get("ItemsWithoutArrayType").getTypes().contains("object"));
        assertNotNull(openAPI.getComponents().getSchemas().get("ItemsWithoutArrayType").getItems());
        assertFalse(result.getMessages().contains("attribute components.schemas.ItemsWithoutArrayType.item1 is unexpected"));
        //Type as array
        assertTrue(openAPI.getComponents().getSchemas().get("Pet").getTypes().size() == 3);
        assertTrue(openAPI.getComponents().getSchemas().get("Pet").getTypes().contains("array"));
        assertTrue(openAPI.getComponents().getSchemas().get("Pet").getTypes().contains("string"));
        assertTrue(openAPI.getComponents().getSchemas().get("Pet").getTypes().contains("object"));
        //JsonSchema
        //arbitrary keywords are now allowed
        assertNotNull(openAPI.getComponents().getSchemas().get("Pet").getExtensions().get("arbitraryKeyword"));
        assertFalse(result.getMessages().contains("attribute components.schemas.Pet.arbitraryKeyword is unexpected"));
        //const
        assertNotNull(((Schema) openAPI.getComponents().getSchemas().get("Pet").getProperties().get("testconst")).getConst());
        assertFalse(result.getMessages().contains("attribute components.schemas.Pet.const is unexpected"));
        //exclusiveMaximum-exclusiveMinimum are numeric in 3.1
        assertTrue(openAPI.getComponents().getSchemas().get("Pets").getExclusiveMaximumValue().intValue()==12);
        assertTrue(openAPI.getComponents().getSchemas().get("Pets").getExclusiveMinimumValue().intValue()==1);
        //Null type
        assertTrue(openAPI.getComponents().getSchemas().get("Pets").getTypes().contains("null"));
        //default value independence
        assertEquals(openAPI.getComponents().getSchemas().get("Pets").getDefault(), "I'm a string");
        //not setting the type by default
        assertNull(openAPI.getComponents().getSchemas().get("MapAnyValue").getTypes());
        //$ref siblings
        assertNotNull(openAPI.getComponents().getSchemas().get("Pet").get$ref());
        assertNotNull(openAPI.getComponents().getSchemas().get("Pet").getProperties());
    }

    @Test(description = "Test basic OAS30 deserialization/validation if added the fields of OAS3.1")
    public void testBasicOAS30_With31Fields() {
        SwaggerParseResult result = new OpenAPIV3Parser().readLocation( "3.1.0/test/basicOAS30.yaml", null, null);
        assertNotNull(result.getOpenAPI());
        OpenAPI openAPI = result.getOpenAPI();

        //No JsonSchemaDialect
        assertNull(openAPI.getJsonSchemaDialect());
        assertTrue(result.getMessages().contains("attribute jsonSchemaDialect is unexpected"));
        //info: only description is parsed
        assertNotNull(openAPI.getInfo().getDescription());
        assertEquals(openAPI.getInfo().getDescription(), "description in info object");
        assertTrue(result.getMessages().contains("attribute info.summary is unexpected"));
        //license: No identifier
        assertNull(openAPI.getInfo().getLicense().getIdentifier());
        assertTrue(result.getMessages().contains("attribute info.license.identifier is unexpected"));
        //No pathItems under components
        assertNull(openAPI.getComponents().getPathItems());
        assertTrue(result.getMessages().contains("attribute components.pathItems is unexpected"));
        //Type array without items but with error message
        assertEquals(openAPI.getComponents().getSchemas().get("ArrayWithoutItems").getType(),"array");
        assertNull(openAPI.getComponents().getSchemas().get("ArrayWithoutItems").getItems());
        assertTrue(result.getMessages().contains("attribute components.schemas.ArrayWithoutItems.items is missing"));
        //Type object with items: not allowed, it will internally create an array (New Option setInferSchemaType )
        assertNotEquals(openAPI.getComponents().getSchemas().get("ItemsWithoutArrayType").getType(),"object");
        assertEquals(openAPI.getComponents().getSchemas().get("ItemsWithoutArrayType").getType(),"array");
        assertNotNull(openAPI.getComponents().getSchemas().get("ItemsWithoutArrayType").getItems());
        assertTrue(result.getMessages().contains("attribute components.schemas.ItemsWithoutArrayType.item1 is unexpected"));
        //Type field as array not deserialized
        assertNull(openAPI.getComponents().getSchemas().get("Pet").getTypes());
        //JsonSchema
        //arbitrary keywords are not allowed
        assertNull(openAPI.getComponents().getSchemas().get("Pet").getExtensions());
        assertTrue(result.getMessages().contains("attribute components.schemas.Pet.arbitraryKeyword is unexpected"));
        //const
        assertNull(((Schema) openAPI.getComponents().getSchemas().get("Pet").getProperties().get("testconst")).getConst());
        assertTrue(result.getMessages().contains("attribute components.schemas.Pet.const is unexpected"));
        //exclusiveMaximum-exclusiveMinimum are boolean in 3.0
        assertNull(openAPI.getComponents().getSchemas().get("Pets").getExclusiveMaximum());
        assertNull(openAPI.getComponents().getSchemas().get("Pets").getExclusiveMinimum());
        assertEquals(openAPI.getComponents().getSchemas().get("Pets").getTypes().iterator().next(), "array");
        //default value independence
        assertNull(openAPI.getComponents().getSchemas().get("Pets").getDefault());
        assertEquals(openAPI.getComponents().getSchemas().get("MapAnyValue").getTypes().iterator().next(), "object");
        //Not webhooks
        assertTrue(result.getMessages().contains("attribute webhooks is unexpected"));
    }

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
                "  title: siblings JSONSchema\n" +
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

    @Test(description = "Test siblings with $ref for patternProperties, pattern, additionalProperties,exclusiveMaximum,exclusiveMinimum, $schema")
    public void testSiblingsReferenceJSONSchema3() {
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
        assertEquals(profile.get$schema(), "https://json-schema.org/draft/2020-12/schema");
        assertEquals(profile.get$anchor(),"foo");
        assertEquals(profile.get$id(),"profile-id");
        assertTrue(profile.getExclusiveMaximumValue().intValue()==12);
        assertTrue(profile.getExclusiveMinimumValue().intValue()==1);
        assertEquals(profile.get$comment(),"end user should not see this comment");
        assertTrue(profile.getTypes().contains("string"));
        assertTrue(profile.getTypes().contains("integer"));
    }

    @Test(description = "Test siblings with $ref for const, contentEncoding, contentMediaType, contentSchema")
    public void testSiblingsReferenceJSONSchema4() {
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
                "      const: sales\n" +
                "      contentEncoding: base64\n" +
                "      contentMediaType: text/html\n" +
                "      contentSchema:\n" +
                "        type: string\n" +
                "      $ref: ./ex.json#user-profile";
        SwaggerParseResult result = new OpenAPIV3Parser().readContents( refSibling , null, options);
        OpenAPI openAPI = result.getOpenAPI();
        assertNotNull(openAPI);
        Schema profile = openAPI.getComponents().getSchemas().get("profile");
        assertNotNull(profile.get$ref());
        assertEquals(profile.getConst(),"sales");
        assertEquals(profile.getContentEncoding(),"base64");
        assertEquals(profile.getContentMediaType(),"text/html");
        assertNotNull(profile.getContentSchema());
        assertTrue(profile.getContentSchema().getTypes().contains("string"));
    }

    @Test(description = "Test siblings with $ref for contains, maxContains, minContains, prefixItems, uniqueItems, propertyNames, unevaluatedProperties, unevaluatedItems")
    public void testSiblingsReferenceJSONSchema5() {
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
                "    ContainsSchema:\n" +
                "      type: array\n" +
                "      contains:\n" +
                "        type: integer\n" +
                "      minContains: 2\n" +
                "      maxContains: 4\n" +
                "      uniqueItems: true\n" +
                "    Profile:\n" +
                "      propertyNames:\n" +
                "        pattern: ^[A-Za-z_][A-Za-z0-9_]*$\n" +
                "      $ref: ./ex.json#user-profile\n" +
                "      unevaluatedProperties:\n" +
                "         type: object\n"+
                "      unevaluatedItems:\n" +
                "          type: object\n" +
                "    Person:\n" +
                "      type: array\n" +
                "      prefixItems:\n" +
                "        - type: string\n" +
                "          description: Name\n" +
                "        - type: integer\n" +
                "          description: Age\n" +
                "      minItems: 2\n" +
                "      maxItems: 2\n" +
                "    Patient:\n" +
                "      type: object\n" +
                "      properties:\n" +
                "        patientId: { }\n" +
                "      patientDetails:\n" +
                "        type: object\n" +
                "    PatientPerson:\n" +
                "      allOf:\n" +
                "        - $ref: '#/components/schemas/Person'\n" +
                "        - $ref: '#/components/schemas/Patient'\n" +
                "      unevaluatedProperties: false\n" +
                "      $ref: ./ex.json#patient-person";
        SwaggerParseResult result = new OpenAPIV3Parser().readContents( refSibling , null, options);
        OpenAPI openAPI = result.getOpenAPI();
        assertNotNull(openAPI);
        Schema profile = openAPI.getComponents().getSchemas().get("Profile");
        Schema containsSchema = openAPI.getComponents().getSchemas().get("ContainsSchema");
        Schema personSchema = openAPI.getComponents().getSchemas().get("Person");
        Schema patientPersonSchema = openAPI.getComponents().getSchemas().get("PatientPerson");
        assertNotNull(profile.get$ref());

        //contains
        assertNotNull(containsSchema.getContains());
        //maxContains, minContains
        assertEquals(containsSchema.getMaxContains().intValue(), 4);
        assertEquals(containsSchema.getMinContains().intValue(), 2);
        //prefixItems
        assertNotNull(personSchema.getPrefixItems());
        //minItems, maxItems
        assertEquals(personSchema.getMaxItems().intValue(), 2);
        assertEquals(personSchema.getMinItems().intValue(), 2);
        //uniqueItems
        assertNotNull(containsSchema.getUniqueItems().booleanValue());
        //propertyNames
        assertEquals(profile.getPropertyNames().getPattern(),"^[A-Za-z_][A-Za-z0-9_]*$");
        //unevaluatedProperties
        assertNotNull(profile.getUnevaluatedProperties());
        assertTrue(profile.getUnevaluatedProperties() instanceof Schema);
        assertTrue(((Schema)profile.getUnevaluatedProperties()).getTypes().contains("object"));
        assertNotNull(patientPersonSchema.getUnevaluatedProperties());
        assertTrue(patientPersonSchema.getUnevaluatedProperties() instanceof Schema);
        assertFalse(patientPersonSchema.getUnevaluatedProperties().getBooleanSchemaValue());
        //unevaluatedItems
        assertNotNull(profile.getUnevaluatedItems());

    }

    @Test(description = "Test siblings with $ref for if - then - else, dependentRequired, dependentSchemas")
    public void testSiblingsReferenceJSONSchema6() {
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
                "    Payment:\n" +
                "      type: object\n" +
                "      properties:\n" +
                "        name:\n" +
                "          type: string\n" +
                "        credit_card:\n" +
                "          type: number\n" +
                "        billing_address:\n" +
                "          type: string\n" +
                "      required:\n" +
                "        - name\n" +
                "      dependentRequired:\n" +
                "        credit_card:\n" +
                "          - billing_address\n" +
                "    PaymentMethod:\n" +
                "      type: object\n" +
                "      properties:\n" +
                "        name:\n" +
                "          type: string\n" +
                "        credit_card:\n" +
                "          type: number\n" +
                "      required:\n" +
                "        - name\n" +
                "      dependentSchemas:\n" +
                "        credit_card:\n" +
                "          properties:\n" +
                "            billing_address:\n" +
                "              type: string\n" +
                "          required:\n" +
                "            - billing_address\n" +
                "    IfTest:\n" +
                "      title: Person\n" +
                "      type: object\n" +
                "      properties:\n" +
                "        country:\n" +
                "          type: string\n" +
                "          widget: Select\n" +
                "          enum:\n" +
                "            - usa\n" +
                "            - canada\n" +
                "            - eu\n" +
                "          default: eu\n" +
                "      required:\n" +
                "        - country\n" +
                "      if:\n" +
                "        properties:\n" +
                "          country:\n" +
                "            type: string\n" +
                "            const: canada\n" +
                "      then:\n" +
                "        properties:\n" +
                "          maple_trees:\n" +
                "            type: number\n" +
                "      else:\n" +
                "        properties:\n" +
                "          accept:\n" +
                "            type: boolean\n" +
                "            const: true\n" +
                "        required:\n" +
                "          - accept\n";
        SwaggerParseResult result = new OpenAPIV3Parser().readContents( refSibling , null, options);
        OpenAPI openAPI = result.getOpenAPI();
        assertNotNull(openAPI);
        //if - then - else
        assertNotNull(openAPI.getComponents().getSchemas().get("IfTest"));
        Schema itTest = openAPI.getComponents().getSchemas().get("IfTest");
        assertNotNull(itTest.getIf());
        assertTrue(itTest.getIf().getProperties().containsKey("country"));
        assertNotNull(itTest.getThen());
        assertTrue(itTest.getThen().getProperties().containsKey("maple_trees"));
        assertNotNull(itTest.getElse());
        assertTrue(itTest.getElse().getProperties().containsKey("accept"));

        //dependentRequired
        assertNotNull(openAPI.getComponents().getSchemas().get("Payment"));
        Schema payment = openAPI.getComponents().getSchemas().get("Payment");
        assertNotNull(payment.getDependentRequired().get("credit_card"));

        //dependentSchemas
        assertNotNull(openAPI.getComponents().getSchemas().get("PaymentMethod"));
        Schema paymentMethod = openAPI.getComponents().getSchemas().get("PaymentMethod");

    }

    @Test(description = "Test examples in JSONSchema")
    public void testExamplesJSONSchema() {
        ParseOptions options = new ParseOptions();
        String examplesSchema = "openapi: 3.1.0\n" +
                "info:\n" +
                "  title: examples JSONSchema\n" +
                "  version: 1.0.0\n" +
                "servers:\n" +
                "  - url: /\n" +
                "paths: { }\n" +
                "components:\n" +
                "  schemas:\n" +
                "    Fruit:\n" +
                "      type: string\n" +
                "      example: kiwi\n" +
                "      examples:\n" +
                "        - apple\n" +
                "        - orange\n" +
                "    Error:\n" +
                "      type: object\n" +
                "      example: wrong\n" +
                "      properties:\n" +
                "        code:\n" +
                "          type: integer\n" +
                "        message:\n" +
                "          type: string\n" +
                "      examples:\n" +
                "        - code: 123\n" +
                "          message: Oops...\n" +
                "        - code: 456\n" +
                "          message: Feature is not available for your plan\n" +
                "    ExampleSchema:\n" +
                "      type: object\n" +
                "      example: foo\n";
        SwaggerParseResult result = new OpenAPIV3Parser().readContents( examplesSchema , null, options);
        OpenAPI openAPI = result.getOpenAPI();
        assertNotNull(openAPI);
        //examples / example
        assertNotNull(openAPI.getComponents().getSchemas().get("Fruit"));
        Schema fruit = openAPI.getComponents().getSchemas().get("Fruit");
        assertTrue(fruit.getExample().equals("kiwi"));
        assertNotNull(fruit.getExamples());
        assertNotNull(openAPI.getComponents().getSchemas().get("Error"));
        Schema error = openAPI.getComponents().getSchemas().get("Error");
        assertTrue(error.getExample().equals("wrong"));
        assertTrue(error.getExamples().get(0)!= null);
        assertNotNull(openAPI.getComponents().getSchemas().get("ExampleSchema"));
        Schema exampleSchema = openAPI.getComponents().getSchemas().get("ExampleSchema");
        assertTrue(exampleSchema.getExample().equals("foo"));
    }

    @Test(description = "Test arbitraryKeywords in JSONSchema")
    public void testArbitraryKeywordsJSONSchema() {
        ParseOptions options = new ParseOptions();
        String arbitraryKeyword = "openapi: 3.1.0\n" +
                "info:\n" +
                "  title: arbitrary keywords JSONSchema\n" +
                "  version: 1.0.0\n" +
                "servers:\n" +
                "  - url: /\n" +
                "paths: { }\n" +
                "components:\n" +
                "  schemas:\n" +
                "    Fruit:\n" +
                "      deprecated: true\n"+
                "      type: string\n" +
                "      example: kiwi\n" +
                "      examples:\n" +
                "        - apple\n" +
                "        - orange\n" +
                "      arbitraryKeyword: test\n" +
                "      x-normalExtension: extensionTest\n";
        SwaggerParseResult result = new OpenAPIV3Parser().readContents( arbitraryKeyword , null, options);
        OpenAPI openAPI = result.getOpenAPI();
        assertNotNull(openAPI);
        assertNotNull(openAPI.getComponents().getSchemas().get("Fruit").getExtensions().get("arbitraryKeyword"));
        assertNotNull(openAPI.getComponents().getSchemas().get("Fruit").getExtensions().get("x-normalExtension"));
        assertTrue(openAPI.getComponents().getSchemas().get("Fruit").getDeprecated());
    }

    @Test(description = "Test for Tuple parsing")
    public void testTuplesJSONSchema() {
        ParseOptions options = new ParseOptions();
        String tuple = "openapi: 3.1.0\n" +
                "info:\n" +
                "  title: tuple JSONSchema\n" +
                "  version: 1.0.0\n" +
                "servers:\n" +
                "  - url: /\n" +
                "paths: { }\n" +
                "components:\n" +
                "  schemas:\n" +
                "    Tuple:\n" +
                "      type: array\n" +
                "      prefixItems:\n" +
                "        - type: string\n" +
                "          description: Name\n" +
                "        - type: integer\n" +
                "          description: Age\n" +
                "      minItems: 2\n" +
                "      maxItems: 2\n";
        SwaggerParseResult result = new OpenAPIV3Parser().readContents(tuple, null, options);
        OpenAPI openAPI = result.getOpenAPI();
        assertNotNull(openAPI);
        assertTrue(openAPI.getComponents().getSchemas().get("Tuple").getPrefixItems().get(0) instanceof Schema);
        Schema schema = (Schema) openAPI.getComponents().getSchemas().get("Tuple").getPrefixItems().get(0);
        assertTrue(schema.getTypes().contains("string"));
        assertEquals(schema.getDescription(), "Name");
    }

        @Test(description = "Test for not setting the schema type as default")
        public void testNotDefaultSchemaType() {
            ParseOptions options = new ParseOptions();
            options.setInferSchemaType(false);
            String defaultSchemaType = "openapi: 3.1.0\n" +
                    "info:\n" +
                    "  title: ping test\n" +
                    "  version: '1.0'\n" +
                    "servers:\n" +
                    "  - url: 'http://localhost:8000/'\n" +
                    "paths:\n" +
                    "  /ping:\n" +
                    "    get:\n" +
                    "      operationId: pingGet\n" +
                    "      responses:\n" +
                    "        '201':\n" +
                    "          description: OK\n" +
                    "components:\n" +
                    "  schemas:\n" +
                    "    AnyValue: {}\n" +
                    "    AnyValueWithDesc:\n" +
                    "      description: Can be any value - string, number, boolean, array or object.\n" +
                    "    AnyValueNullable:\n" +
                    "      nullable: true\n" +
                    "      description: Can be any value, including `null`.\n" +
                    "    AnyValueModel:\n" +
                    "      description: test any value\n" +
                    "      type: object\n" +
                    "      properties:\n" +
                    "        any_value:\n" +
                    "          $ref: '#/components/schemas/AnyValue'\n" +
                    "        any_value_with_desc:\n" +
                    "          $ref: '#/components/schemas/AnyValueWithDesc'\n" +
                    "        any_value_nullable:\n" +
                    "          $ref: '#/components/schemas/AnyValueNullable'\n" +
                    "    AnyValueModelInline:\n" +
                    "      description: test any value inline\n" +
                    "      type: object\n" +
                    "      properties:\n" +
                    "        any_value: {}\n" +
                    "        any_value_with_desc:\n" +
                    "          description: inline any value\n" +
                    "        any_value_nullable:\n" +
                    "          nullable: true\n" +
                    "          description: inline any value nullable\n" +
                    "        map_any_value:\n" +
                    "          additionalProperties: {}\n" +
                    "        map_any_value_with_desc:\n" +
                    "          additionalProperties: \n" +
                    "            description: inline any value\n" +
                    "        map_any_value_nullable:\n" +
                    "          additionalProperties:\n" +
                    "            nullable: true\n" +
                    "            description: inline any value nullable\n" +
                    "        array_any_value:\n" +
                    "          items: {}\n" +
                    "        array_any_value_with_desc:\n" +
                    "          items: \n" +
                    "            description: inline any value\n" +
                    "        array_any_value_nullable:\n" +
                    "          items:\n" +
                    "            nullable: true\n" +
                    "            description: inline any value nullable";
            SwaggerParseResult result = new OpenAPIV3Parser().readContents(defaultSchemaType, null, options);
            OpenAPI openAPI = result.getOpenAPI();
            assertNotNull(openAPI);

            //map_any_value as object when it should be null
            assertNotNull(openAPI.getComponents().getSchemas().get("AnyValueModelInline").getProperties().get("map_any_value"));
            assertTrue(openAPI.getComponents().getSchemas().get("AnyValueModelInline").getProperties().get("map_any_value") instanceof Schema);
            Schema schema = (Schema)openAPI.getComponents().getSchemas().get("AnyValueModelInline").getProperties().get("map_any_value");
            assertNull(schema.getType());

            //map_any_value_with_desc as object when it should be null
            assertNotNull(openAPI.getComponents().getSchemas().get("AnyValueModelInline").getProperties().get("map_any_value_with_desc"));
            assertTrue(openAPI.getComponents().getSchemas().get("AnyValueModelInline").getProperties().get("map_any_value_with_desc") instanceof Schema);
            schema = (Schema)openAPI.getComponents().getSchemas().get("AnyValueModelInline").getProperties().get("map_any_value_with_desc");
            assertNull(schema.getType());

            //map_any_value_nullable as object when it should be null
            assertNotNull(openAPI.getComponents().getSchemas().get("AnyValueModelInline").getProperties().get("map_any_value_nullable"));
            assertTrue(openAPI.getComponents().getSchemas().get("AnyValueModelInline").getProperties().get("map_any_value_nullable") instanceof Schema);
            schema = (Schema)openAPI.getComponents().getSchemas().get("AnyValueModelInline").getProperties().get("map_any_value_nullable");
            assertNull(schema.getType());

            //array_any_value as array when it should be null
            assertNotNull(openAPI.getComponents().getSchemas().get("AnyValueModelInline").getProperties().get("array_any_value"));
            assertTrue(openAPI.getComponents().getSchemas().get("AnyValueModelInline").getProperties().get("array_any_value") instanceof Schema);
            schema = (Schema)openAPI.getComponents().getSchemas().get("AnyValueModelInline").getProperties().get("array_any_value");
            assertNull(schema.getType());

            //array_any_value_with_desc as array when it should be null
            assertNotNull(openAPI.getComponents().getSchemas().get("AnyValueModelInline").getProperties().get("array_any_value_with_desc"));
            assertTrue(openAPI.getComponents().getSchemas().get("AnyValueModelInline").getProperties().get("array_any_value_with_desc") instanceof Schema);
            schema = (Schema)openAPI.getComponents().getSchemas().get("AnyValueModelInline").getProperties().get("array_any_value_with_desc");
            assertNull(schema.getType());

            //array_any_value_nullable
            assertNotNull(openAPI.getComponents().getSchemas().get("AnyValueModelInline").getProperties().get("array_any_value_nullable"));
            assertTrue(openAPI.getComponents().getSchemas().get("AnyValueModelInline").getProperties().get("array_any_value_nullable") instanceof Schema);
            schema = (Schema)openAPI.getComponents().getSchemas().get("AnyValueModelInline").getProperties().get("array_any_value_nullable");
            assertNull(schema.getType());
            assertNull(schema.getItems().getNullable());
            assertNotNull(schema.getItems().getExtensions().get("nullable"));
        }

    @Test(description = "Test how 3.0 spec deserializes with ref schema and other fields")
    public void test30NonRefSiblingsBehavior() {
        ParseOptions options = new ParseOptions();
        String refSibling = "openapi: 3.0.0\n" +
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
        assertNull(profile.getMaxItems());
        assertNull(profile.getDescription());
        assertNull(profile.getRequired());
        assertNull(profile.getProperties());

    }

    @Test(description = "Test Issue 1801")
    public void test31Issue1801() {
        ParseOptions options = new ParseOptions();
        SwaggerParseResult result = new OpenAPIV3Parser().readLocation( "3.1.0/issue-1801.yaml", null, options);
        assertNotNull(result.getOpenAPI());
        OpenAPI openAPI = result.getOpenAPI();
        Schema firstAllOf = (Schema)openAPI.getComponents().getSchemas().get("AllofWithTheLastEmptySchema").getAllOf().get(0);
        assertEquals(firstAllOf.getTypes().iterator().next(), "number");
        assertEquals(firstAllOf.getType(), null);
        Schema secondAllOf = (Schema)openAPI.getComponents().getSchemas().get("AllofWithTheLastEmptySchema").getAllOf().get(1);
        assertEquals(secondAllOf.getTypes(), null);
        assertEquals(firstAllOf.getType(), null);

        try {
            System.setProperty("bind-type", "true");
            result = new OpenAPIV3Parser().readLocation("3.1.0/issue-1801.yaml", null, options);
            assertNotNull(result.getOpenAPI());
            openAPI = result.getOpenAPI();
            firstAllOf = (Schema) openAPI.getComponents().getSchemas().get("AllofWithTheLastEmptySchema").getAllOf().get(0);
            assertEquals(firstAllOf.getTypes().iterator().next(), "number");
            assertEquals(firstAllOf.getType(), "number");
            secondAllOf = (Schema) openAPI.getComponents().getSchemas().get("AllofWithTheLastEmptySchema").getAllOf().get(1);
            assertEquals(secondAllOf.getTypes(), null);
            assertEquals(secondAllOf.getType(), null);
        } finally {
            System.setProperty("bind-type", "false");
        }
    }

    @Test(description = "Test Issue 1821")
    public void test31Issue1821() {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setResolveFully(true);
        SwaggerParseResult result = new OpenAPIV3Parser().readLocation( "3.1.0/issue-1821.yaml", null, options);
        assertNotNull(result.getOpenAPI());
        Schema id = (Schema)result.getOpenAPI().getComponents().getSchemas().get("Rule").getProperties().get("id");
        assertEquals(id.getTypes().iterator().next(), "string");
    }

    @Test(description = "Test safe resolving")
    public void test31SafeURLResolving() {
        ParseOptions parseOptions = new ParseOptions();
        parseOptions.setResolveFully(true);
        parseOptions.setSafelyResolveURL(true);
        List<String> allowList = Collections.emptyList();
        List<String> blockList = Collections.emptyList();
        parseOptions.setRemoteRefAllowList(allowList);
        parseOptions.setRemoteRefBlockList(blockList);

        SwaggerParseResult result = new OpenAPIV3Parser().readLocation("safeResolving/oas31SafeUrlResolvingWithPetstore.yaml", null, parseOptions);
        if (result.getMessages() != null) {
            for (String message : result.getMessages()) {
                assertTrue(message.contains("Server returned HTTP response code: 403"));
            }
        }
    }

    @Test(description = "Test safe resolving with blocked URL")
    public void test31SafeURLResolvingWithBlockedURL() {
        ParseOptions parseOptions = new ParseOptions();
        parseOptions.setResolveFully(true);
        parseOptions.setSafelyResolveURL(true);
        List<String> allowList = Collections.emptyList();
        List<String> blockList = Arrays.asList("petstore3.swagger.io");
        parseOptions.setRemoteRefAllowList(allowList);
        parseOptions.setRemoteRefBlockList(blockList);

        SwaggerParseResult result = new OpenAPIV3Parser().readLocation("safeResolving/oas31SafeUrlResolvingWithPetstore.yaml", null, parseOptions);

        if (result.getMessages() != null) {
            for (String message : result.getMessages()) {
                assertTrue(
                        message.contains("Server returned HTTP response code: 403") ||
                        message.contains("URL is part of the explicit denylist. URL [https://petstore3.swagger.io/api/v3/openapi.json]"));
            }
        }
    }

    @Test(description = "Test safe resolving with turned off safelyResolveURL option")
    public void test31SafeURLResolvingWithTurnedOffSafeResolving() {
        ParseOptions parseOptions = new ParseOptions();
        parseOptions.setResolveFully(true);
        parseOptions.setSafelyResolveURL(false);
        List<String> allowList = Collections.emptyList();
        List<String> blockList = Arrays.asList("petstore3.swagger.io");
        parseOptions.setRemoteRefAllowList(allowList);
        parseOptions.setRemoteRefBlockList(blockList);

        SwaggerParseResult result = new OpenAPIV3Parser().readLocation("safeResolving/oas31SafeUrlResolvingWithPetstore.yaml", null, parseOptions);
        if (result.getMessages() != null) {
            for (String message : result.getMessages()) {
                assertTrue(message.contains("Server returned HTTP response code: 403"));
            }
        }
    }

    @Test(description = "Test safe resolving with localhost and blocked url")
    public void test31SafeURLResolvingWithLocalhostAndBlockedURL() {
        ParseOptions parseOptions = new ParseOptions();
        parseOptions.setResolveFully(true);
        parseOptions.setSafelyResolveURL(true);

        SwaggerParseResult result = new OpenAPIV3Parser().readLocation("safeResolving/oas31SafeUrlResolvingWithLocalhost.yaml", null, parseOptions);
        if (result.getMessages() != null) {
            for (String message : result.getMessages()) {
                assertTrue(
                        message.contains("Server returned HTTP response code: 403") ||
                                message.contains("IP is restricted"));
            }
        }
    }

    @Test(description = "Test safe resolving with localhost url")
    public void test31SafeURLResolvingWithLocalhost() {
        ParseOptions parseOptions = new ParseOptions();
        parseOptions.setResolveFully(true);
        parseOptions.setSafelyResolveURL(true);
        List<String> blockList = Arrays.asList("petstore.swagger.io");
        parseOptions.setRemoteRefBlockList(blockList);

        String error = "URL is part of the explicit denylist. URL [https://petstore.swagger.io/v2/swagger.json]";
        SwaggerParseResult result = new OpenAPIV3Parser().readLocation("safeResolving/oas31SafeUrlResolvingWithLocalhost.yaml", null, parseOptions);

        if (result.getMessages() != null) {
            for (String message : result.getMessages()) {
                assertTrue(
                        message.contains("Server returned HTTP response code: 403") ||
                                message.contains("IP is restricted") ||
                                message.contains(error)
                        );
            }
        }
    }
}
