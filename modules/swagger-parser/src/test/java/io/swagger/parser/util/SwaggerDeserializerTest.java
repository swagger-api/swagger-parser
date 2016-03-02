package io.swagger.parser.util;

import com.google.common.base.Optional;
import io.swagger.models.*;
import io.swagger.models.auth.ApiKeyAuthDefinition;
import io.swagger.models.auth.BasicAuthDefinition;
import io.swagger.models.auth.In;
import io.swagger.models.auth.SecuritySchemeDefinition;
import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.parameters.PathParameter;
import io.swagger.models.parameters.QueryParameter;
import io.swagger.models.properties.*;
import io.swagger.parser.SwaggerParser;
import io.swagger.parser.SwaggerResolver;
import static io.swagger.parser.util.SwaggerDeserializer.allPathParametersAccountForExtractedPathParameters;
import io.swagger.util.Json;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.*;

import static org.junit.Assert.*;

public class SwaggerDeserializerTest {

    @Test
    public void testSecurityDeserialization() throws Exception {
        String json = "{\n" +
                        "  \"swagger\": \"2.0\",\n" +
                        "  \"security\": [\n" +
                        "    {\n" +
                        "      \"api_key1\": [],\n" +
                        "      \"api_key2\": []\n" +
                        "    },\n" +
                        "    {\n" +
                        "      \"api_key3\": []\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}";

        SwaggerParser parser = new SwaggerParser();

        SwaggerDeserializationResult result = parser.readWithInfo(json);
        List<String> messageList = result.getMessages();
        Set<String> messages = new HashSet<String>(messageList);
        Swagger swagger = result.getSwagger();
        assertNotNull(swagger);

        List<SecurityRequirement> security = swagger.getSecurity();
        assertTrue(security.size() == 2);
        
    }

    @Test
    public void testSchema() throws Exception {
        String json = "{\n" +
                "  \"type\":\"object\",\n" +
                "  \"properties\": {\n" +
                "    \"data\": {\n" +
                "      \"properties\": {\n" +
                "        \"name\": {\n" +
                "          \"type\": \"string\",\n" +
                "          \"minLength\": 1\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  },\n" +
                "  \"required\": [\n" +
                "    \"data\"\n" +
                "  ]\n" +
                "}";

        Model m = Json.mapper().readValue(json, Model.class);
        assertNotNull(m);
        Map<String, Property> properties = m.getProperties();

        assertTrue(properties.keySet().size() == 1);
        Property data = properties.get("data");
        assertTrue(data instanceof ObjectProperty);
        ObjectProperty op = (ObjectProperty) data;
        Map<String, Property> innerProperties = ((ObjectProperty) data).getProperties();
        assertTrue(innerProperties.keySet().size() == 1);

        Property name = innerProperties.get("name");
        assertTrue(name instanceof StringProperty);
    }

    @Test
    public void testArraySchema() throws Exception {
        String json = "{\n" +
                "  \"properties\": {\n" +
                "    \"data\": {\n" +
                "      \"properties\": {\n" +
                "        \"description\": \"the array type\",\n" +
                "        \"type\": \"array\",\n" +
                "        \"items\": {\n" +
                "          \"properties\": {\n" +
                "            \"name\": {\n" +
                "              \"description\": \"the inner type\",\n" +
                "              \"type\": \"string\",\n" +
                "              \"minLength\": 1\n" +
                "            }\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  },\n" +
                "  \"required\": [\n" +
                "    \"data\"\n" +
                "  ]\n" +
                "}";

        Model m = Json.mapper().readValue(json, Model.class);

        Property data = m.getProperties().get("data");
        assertTrue(data instanceof ArrayProperty);

        ArrayProperty ap = (ArrayProperty) data;
        assertEquals("the array type", ap.getDescription());

        Property inner = ap.getItems();
        assertNotNull(inner);

        assertTrue(inner instanceof ObjectProperty);
        ObjectProperty op = (ObjectProperty) inner;

        Property name = op.getProperties().get("name");
        assertEquals(name.getDescription(), "the inner type");
        assertTrue(((StringProperty)name).getMinLength() == 1);
    }

    @Test
    public void testEmpty() {
        String json = "{}";

        SwaggerParser parser = new SwaggerParser();

        SwaggerDeserializationResult result = parser.readWithInfo(json);
        List<String> messageList = result.getMessages();
        Set<String> messages = new HashSet<String>(messageList);

        assertTrue(messages.contains("attribute swagger is missing"));
        assertTrue(messages.contains("attribute info is missing"));
        assertTrue(messages.contains("attribute paths is missing"));
    }

    @Test
    public void testSecurity() {
        String json = "{\n" +
                "  \"swagger\": \"2.0\",\n" +
                "  \"security\": [\n" +
                "    {\n" +
                "      \"petstore_auth\": [\n" +
                "        \"write:pets\",\n" +
                "        \"read:pets\"\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}";
        SwaggerParser parser = new SwaggerParser();

        SwaggerDeserializationResult result = parser.readWithInfo(json);
        List<String> messageList = result.getMessages();
        Set<String> messages = new HashSet<String>(messageList);

        Swagger swagger = result.getSwagger();

        assertNotNull(swagger.getSecurity());
        List<SecurityRequirement> security = swagger.getSecurity();
        Assert.assertTrue(security.size() == 1);
        Assert.assertTrue(security.get(0).getRequirements().size() == 1);

        List<String> requirement = security.get(0).getRequirements().get("petstore_auth");
        Assert.assertTrue(requirement.size() == 2);

        Set<String> requirements = new HashSet(requirement);
        Assert.assertTrue(requirements.contains("read:pets"));
        Assert.assertTrue(requirements.contains("write:pets"));
    }

    @Test
    public void testSecurityDefinition() {
        String json = "{\n" +
                "  \"swagger\": \"2.0\",\n" +
                "  \"securityDefinitions\": {\n" +
                "    \"basic_auth\": {\n" +
                "      \"type\": \"basic\"\n" +
                "    },\n" +
                "    \"api_key\": {\n" +
                "      \"type\": \"apiKey\",\n" +
                "      \"name\": \"api_key\",\n" +
                "      \"in\": \"header\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"paths\": {\n" +
                "    \"/pet\": {\n" +
                "      \"get\": {\n" +
                "        \"security\": [\n" +
                "          {\n" +
                "            \"basic_auth\": [],\n" +
                "            \"api_key\": []\n" +
                "          }\n" +
                "        ]\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}";

        SwaggerParser parser = new SwaggerParser();

        SwaggerDeserializationResult result = parser.readWithInfo(json);
        List<String> messageList = result.getMessages();
        Set<String> messages = new HashSet<String>(messageList);

        Swagger swagger = result.getSwagger();
        assertNotNull(swagger.getSecurityDefinitions());
        assertTrue(swagger.getSecurityDefinitions().keySet().size() == 2);

        // Basic Authentication
        SecuritySchemeDefinition definitionBasic = swagger.getSecurityDefinitions().get("basic_auth");
        assertNotNull(definitionBasic);
        assertTrue(definitionBasic instanceof BasicAuthDefinition);

        // API Key Authentication
        SecuritySchemeDefinition definition = swagger.getSecurityDefinitions().get("api_key");
        assertNotNull(definition);
        assertTrue(definition instanceof ApiKeyAuthDefinition);

        ApiKeyAuthDefinition apiKey = (ApiKeyAuthDefinition) definition;
        assertEquals(apiKey.getName(), "api_key");
        assertEquals(apiKey.getIn(), In.HEADER);
    }

    @Test
    public void testRootInfo() {
        String json = "{\n" +
                "\t\"swagger\": \"2.0\",\n" +
                "\t\"foo\": \"bar\",\n" +
                "\t\"info\": \"invalid\"\n" +
                "}";

        SwaggerParser parser = new SwaggerParser();

        SwaggerDeserializationResult result = parser.readWithInfo(json);
        List<String> messageList = result.getMessages();
        Set<String> messages = new HashSet<String>(messageList);

        assertTrue(messages.contains("attribute foo is unexpected"));
        assertTrue(messages.contains("attribute info is not of type `object`"));
    }

    @Test
    public void testContact() {
        String json = "{\n" +
                "\t\"swagger\": \"2.0\",\n" +
                "\t\"info\": {\n" +
                "\t\t\"title\": \"title\",\n" +
                "\t\t\"bad\": \"bad\",\n" +
                "\t\t\"x-foo\": \"bar\",\n" +
                "\t\t\"description\": \"description\",\n" +
                "\t\t\"termsOfService\": \"tos\",\n" +
                "\t\t\"contact\": {\n" +
                "\t\t\t\"name\": \"tony\",\n" +
                "\t\t\t\"url\": \"url\",\n" +
                "\t\t\t\"email\": \"email\",\n" +
                "\t\t\t\"invalid\": \"invalid\",\n" +
                "\t\t\t\"x-fun\": true\n" +
                "\t\t},\n" +
                "\t\t\"version\": \"version\"\n" +
                "\t}\n" +
                "}";

        SwaggerParser parser = new SwaggerParser();

        SwaggerDeserializationResult result = parser.readWithInfo(json);
        List<String> messageList = result.getMessages();
        Set<String> messages = new HashSet<String>(messageList);

        assertEquals(result.getSwagger().getInfo().getTitle(), "title");
        assertEquals(result.getSwagger().getInfo().getDescription(), "description");
        assertEquals(result.getSwagger().getInfo().getTermsOfService(), "tos");
        assertEquals(result.getSwagger().getInfo().getVersion(), "version");

        Contact contact = result.getSwagger().getInfo().getContact();
        assertEquals(contact.getName(), "tony");
        assertEquals(contact.getUrl(), "url");
        assertEquals(contact.getEmail(), "email");

        assertTrue(messages.contains("attribute info.contact.x-fun is unexpected"));
        assertTrue(messages.contains("attribute info.bad is unexpected"));
        assertTrue(messages.contains("attribute info.contact.invalid is unexpected"));

        assertEquals(result.getSwagger().getInfo().getVendorExtensions().get("x-foo").toString(), "bar");
    }

    @Test
    public void testResponses() {
        String json = "{\n" +
                "\t\"swagger\": \"2.0\",\n" +
                "\t\"responses\": {\n" +
                "\t\t\"foo\": {\n" +
                "\t\t\t\"description\": \"description\",\n" +
                "\t\t\t\"bar\": \"baz\",\n" +
                "\t\t\t\"x-foo\": \"bar\"\n" +
                "\t\t}\n" +
                "\t}\n" +
                "}";

        SwaggerParser parser = new SwaggerParser();

        SwaggerDeserializationResult result = parser.readWithInfo(json);
        List<String> messageList = result.getMessages();
        Set<String> messages = new HashSet<String>(messageList);

        assertTrue(messages.contains("attribute responses.foo.bar is unexpected"));

        assertEquals(result.getSwagger().getResponses().get("foo").getVendorExtensions().get("x-foo").toString(), "bar");
    }

    @Test
    public void testLicense () {
        String json = "{\n" +
                "\t\"swagger\": \"2.0\",\n" +
                "\t\"info\": {\n" +
                "\t\t\"license\": {\n" +
                "\t\t\t\"invalid\": true,\n" +
                "\t\t\t\"x-valid\": {\n" +
                "\t\t\t\t\"isValid\": true\n" +
                "\t\t\t}\n" +
                "\t\t}\n" +
                "\t}\n" +
                "}";

        SwaggerParser parser = new SwaggerParser();

        SwaggerDeserializationResult result = parser.readWithInfo(json);
        List<String> messageList = result.getMessages();
        Set<String> messages = new HashSet<String>(messageList);

        assertTrue(messages.contains("attribute info.license.invalid is unexpected"));
        assertTrue(messages.contains("attribute info.title is missing"));
        assertTrue(messages.contains("attribute paths is missing"));

        assertEquals(result.getSwagger().getInfo().getLicense().getVendorExtensions().get("x-valid").toString(), "{\"isValid\":true}");
    }



    @Test
    public void testDefinitions () {
        String json = "{\n" +
                "  \"swagger\": \"2.0\",\n" +
                "  \"definitions\": {\n" +
                "    \"invalid\": true,\n" +
                "    \"Person\": {\n" +
                "      \"required\": [\n" +
                "        \"id\",\n" +
                "        \"name\"\n" +
                "      ],\n" +
                "      \"properties\": {\n" +
                "        \"id\": {\n" +
                "          \"type\": \"integer\",\n" +
                "          \"format\": \"int64\"\n" +
                "        },\n" +
                "        \"name\": {\n" +
                "          \"type\": \"string\"\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}";

        SwaggerParser parser = new SwaggerParser();

        SwaggerDeserializationResult result = parser.readWithInfo(json);
        List<String> messageList = result.getMessages();
        Set<String> messages = new HashSet<String>(messageList);

        assertTrue(messages.contains("attribute definitions.invalid is not of type `object`"));
        assertTrue(result.getSwagger().getDefinitions().get("Person") instanceof ModelImpl);

        List<String> required = ((ModelImpl)result.getSwagger().getDefinitions().get("Person")).getRequired();
        Set<String> requiredKeys = new HashSet<String>(required);
        assertTrue(requiredKeys.contains("id"));
        assertTrue(requiredKeys.contains("name"));
        assertTrue(requiredKeys.size() == 2);
    }

    @Test
    public void testNestedDefinitions() {
        String json = "{\n" +
                "  \"swagger\": \"2.0\",\n" +
                "  \"definitions\": {\n" +
                "    \"Person\": {\n" +
                "      \"required\": [\n" +
                "        \"id\",\n" +
                "        \"name\"\n" +
                "      ],\n" +
                "      \"properties\": {\n" +
                "        \"id\": {\n" +
                "          \"type\": \"integer\",\n" +
                "          \"format\": \"int64\"\n" +
                "        },\n" +
                "        \"name\": {\n" +
                "          \"type\": \"string\"\n" +
                "        },\n" +
                "        \"address\": {\n" +
                "        \t\"$ref\": \"#/definitions/Address\"\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"Address\": {\n" +
                "    \t\"required\": [\"zip\"],\n" +
                "    \t\"properties\": {\n" +
                "    \t\t\"street\": {\n" +
                "    \t\t\t\"type\": \"string\"\n" +
                "    \t\t},\n" +
                "    \t\t\"zip\": {\n" +
                "    \t\t\t\"type\": \"integer\",\n" +
                "    \t\t\t\"format\": \"int32\",\n" +
                "    \t\t\t\"minimum\": 0,\n" +
                "    \t\t\t\"exclusiveMinimum\": true,\n" +
                "    \t\t\t\"maximum\": 99999,\n" +
                "    \t\t\t\"exclusiveMaximum\": true\n" +
                "    \t\t}\n" +
                "    \t}\n" +
                "    }\n" +
                "  }\n" +
                "}";

        SwaggerParser parser = new SwaggerParser();

        SwaggerDeserializationResult result = parser.readWithInfo(json);
        List<String> messageList = result.getMessages();
        Set<String> messages = new HashSet<String>(messageList);

        assertTrue(result.getSwagger().getDefinitions().get("Person") instanceof ModelImpl);
        assertTrue(result.getSwagger().getDefinitions().get("Address") instanceof ModelImpl);

        ModelImpl person = (ModelImpl) result.getSwagger().getDefinitions().get("Person");
        Property property = person.getProperties().get("address");
        assertTrue(property instanceof RefProperty);

        Property zip = ((ModelImpl)result.getSwagger().getDefinitions().get("Address")).getProperties().get("zip");
        assertTrue(zip instanceof IntegerProperty);

        IntegerProperty zipProperty = (IntegerProperty) zip;
        assertTrue(zipProperty.getMinimum() == 0);
        assertTrue(zipProperty.getExclusiveMinimum());

        assertTrue(zipProperty.getMaximum() == 99999);
        assertTrue(zipProperty.getExclusiveMaximum());
    }

    @Test
    public void testPaths() {
        String json = "{\n" +
                "  \"swagger\": \"2.0\",\n" +
                "  \"paths\": {\n" +
                "    \"/pet\": {\n" +
                "      \"foo\": \"bar\",\n" +
                "      \"get\": {\n" +
                "        \"security\": [\n" +
                "          {\n" +
                "            \"petstore_auth\": [\n" +
                "              \"write:pets\",\n" +
                "              \"read:pets\"\n" +
                "            ]\n" +
                "          }\n" +
                "        ]\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}";
        SwaggerParser parser = new SwaggerParser();

        SwaggerDeserializationResult result = parser.readWithInfo(json);
        List<String> messageList = result.getMessages();
        Set<String> messages = new HashSet<String>(messageList);
        assertTrue(messages.contains("attribute paths.'/pet'.foo is unexpected"));
        Swagger swagger = result.getSwagger();

        Path path = swagger.getPath("/pet");
        assertNotNull(path);
        Operation operation = path.getGet();
        assertNotNull(operation);
        List<Map<String, List<String>>> security = operation.getSecurity();

        assertTrue(security.size() == 1);
        Map<String, List<String>> requirement = security.get(0);

        assertTrue(requirement.containsKey("petstore_auth"));
        List<String> scopesList = requirement.get("petstore_auth");

        Set<String> scopes = new HashSet<String>(scopesList);
        assertTrue(scopes.contains("read:pets"));
        assertTrue(scopes.contains("write:pets"));
    }
    
    @Test
    public void testPathsWithRefResponse() {
        String json = "{\n" +
                "  \"swagger\": \"2.0\",\n" +
                "  \"paths\": {\n" +
                "    \"/pet\": {\n" +
                "      \"get\": {\n" +
                "        \"responses\": {\n" +
                "          \"200\": {\n" +
                "            \"$ref\": \"#/responses/OK\"" +
                "          }\n" +
                "        }\n" +                
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}";
        SwaggerParser parser = new SwaggerParser();

        SwaggerDeserializationResult result = parser.readWithInfo(json);
        Swagger swagger = result.getSwagger();

        Path path = swagger.getPath("/pet");
        assertNotNull(path);
        Operation operation = path.getGet();
        assertNotNull(operation);
        assertTrue(operation.getResponses().containsKey("200"));
        assertEquals(RefResponse.class,operation.getResponses().get("200").getClass());
        RefResponse refResponse = (RefResponse)operation.getResponses().get("200");
        assertEquals("#/responses/OK",refResponse.get$ref());
    }

    @Test
    public void testArrayModelDefinition() {
        String json = "{\n" +
                "  \"paths\": {\n" +
                "    \"/store/inventory\": {\n" +
                "      \"get\": {\n" +
                "        \"responses\": {\n" +
                "          \"200\": {\n" +
                "            \"description\": \"successful operation\",\n" +
                "            \"schema\": {\n" +
                "              \"type\": \"object\",\n" +
                "              \"additionalProperties\": {\n" +
                "                \"type\": \"integer\",\n" +
                "                \"format\": \"int32\"\n" +
                "              }\n" +
                "            }\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}";

        SwaggerParser parser = new SwaggerParser();

        SwaggerDeserializationResult result = parser.readWithInfo(json);
        List<String> messageList = result.getMessages();
        Set<String> messages = new HashSet<String>(messageList);
        Swagger swagger = result.getSwagger();

        Property response = swagger.getPath("/store/inventory").getGet().getResponses().get("200").getSchema();
        assertTrue(response instanceof MapProperty);
    }

    @Test
    public void testArrayQueryParam() throws Exception {
        String json = "{\n" +
                "  \"swagger\": \"2.0\",\n" +
                "  \"paths\": {\n" +
                "    \"/pet/findByStatus\": {\n" +
                "      \"get\": {\n" +
                "        \"parameters\": [\n" +
                "          {\n" +
                "            \"name\": \"status\",\n" +
                "            \"in\": \"query\",\n" +
                "            \"description\": \"Status values that need to be considered for filter\",\n" +
                "            \"required\": false,\n" +
                "            \"type\": \"array\",\n" +
                "            \"items\": {\n" +
                "              \"type\": \"string\"\n" +
                "            },\n" +
                "            \"collectionFormat\": \"pipes\",\n" +
                "            \"default\": \"available\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"responses\": {\n" +
                "          \"200\": {\n" +
                "            \"description\": \"successful operation\",\n" +
                "            \"schema\": {\n" +
                "              \"$ref\": \"#/definitions/PetArray\"\n" +
                "            }\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}";

        SwaggerParser parser = new SwaggerParser();

        SwaggerDeserializationResult result = parser.readWithInfo(json);

        Swagger swagger = result.getSwagger();
        Parameter param = swagger.getPath("/pet/findByStatus").getGet().getParameters().get(0);

        assertTrue(param instanceof QueryParameter);
        QueryParameter qp = (QueryParameter) param;
        Property p = qp.getItems();

        assertEquals(qp.getType(), "array");
        assertTrue(p instanceof StringProperty);
    }


    @Test(description = "it should read a top-level extension per https://github.com/swagger-api/validator-badge/issues/59")
    public void testToplevelExtension() throws Exception {
        String json = "\n" +
                "{\n" +
                "    \"swagger\": \"2.0\",\n" +
                "\t\"x-foo\" : \"woof\",\n" +
                "    \"info\": {\n" +
                "        \"version\": \"0.0.0\",\n" +
                "        \"title\": \"Simple API\"\n" +
                "    },\n" +
                "    \"paths\": {\n" +
                "        \"/\": {\n" +
                "            \"get\": {\n" +
                "                \"responses\": {\n" +
                "                    \"200\": {\n" +
                "                        \"description\": \"OK\"\n" +
                "                    }\n" +
                "                }\n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "}";

        SwaggerParser parser = new SwaggerParser();

        SwaggerDeserializationResult result = parser.readWithInfo(json);
    }

    @Test
    public void testDeserializeBinaryString() {
        String json = "{\n" +
                "  \"swagger\": \"2.0\",\n" +
                "  \"info\": {\n" +
                "    \"title\": \"foo\"\n" +
                "  },\n" +
                "  \"paths\": {\n" +
                "    \"/test\": {\n" +
                "      \"post\": {\n" +
                "        \"parameters\": [\n" +
                "          {\n" +
                "            \"name\": \"AnyName\",\n" +
                "            \"in\": \"body\",\n" +
                "            \"schema\": {\n" +
                "              \"type\": \"string\",\n" +
                "              \"format\": \"binary\"\n" +
                "            }\n" +
                "          }\n" +
                "        ],\n" +
                "        \"responses\": {\n" +
                "          \"200\": {\n" +
                "            \"description\": \"ok\"\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}";

        SwaggerParser parser = new SwaggerParser();
        SwaggerDeserializationResult result = parser.readWithInfo(json);

        final Swagger resolved = new SwaggerResolver(result.getSwagger(), null).resolve();
    }

    @Test
    public void testDeserializeEnum() {
        String yaml = "swagger: '2.0'\n" +
                "info:\n" +
                "  version: 0.0.0\n" +
                "  title: your title\n" +
                "paths:\n" +
                "  /persons:\n" +
                "    get:\n" +
                "      description: a test\n" +
                "      responses:\n" +
                "        '200':\n" +
                "          description: Successful response\n" +
                "          schema:\n" +
                "            $ref: '#/definitions/ExampleEnum'\n" +
                "definitions:\n" +
                "  ExampleEnum:\n" +
                "    type: string\n" +
                "    default: foo\n" +
                "    enum:\n" +
                "      - First\n" +
                "      - Second";
        SwaggerParser parser = new SwaggerParser();
        SwaggerDeserializationResult result = parser.readWithInfo(yaml);

        final Swagger resolved = new SwaggerResolver(result.getSwagger(), null).resolve();

        Model model = resolved.getDefinitions().get("ExampleEnum");
        assertTrue(model instanceof ModelImpl);
        ModelImpl impl = (ModelImpl) model;
        List<String> enumValues = impl.getEnum();
        assertTrue(enumValues.size() == 2);
        assertEquals(enumValues.get(0), "First");
        assertEquals(enumValues.get(1), "Second");
    }

    @Test
    public void testDeserializeWithMessages() {
        String yaml = "swagger: '2.0'\n" +
                "info:\n" +
                "  version: 0.0.0\n" +
                "  title:\n" +
                "    - bar";
        SwaggerParser parser = new SwaggerParser();
        SwaggerDeserializationResult result = parser.readWithInfo(yaml);

        Set<String> messages = new HashSet<String>(result.getMessages());
        assertTrue(messages.size() == 2);

        assertTrue(messages.contains("attribute info.title is not of type `string`"));
        assertTrue(messages.contains("attribute paths is missing"));
    }

    @Test
    public void testDeserializeWithDiscriminator() {
        String yaml =
                "swagger: '2.0'\n" +
                "definitions: \n" +
                "  Animal:\n" +
                "    type: object\n" +
                "    discriminator: petType\n" +
                "    description: |\n" +
                "      A basic `Animal` object which can extend to other animal types.\n" +
                "    required:\n" +
                "      - commonName\n" +
                "      - petType\n" +
                "    properties:\n" +
                "      commonName:\n" +
                "        description: the household name of the animal\n" +
                "        type: string\n" +
                "      petType:\n" +
                "        description: |\n" +
                "          The discriminator for the animal type.  It _must_\n" +
                "          match one of the concrete schemas by name (i.e. `Cat`)\n" +
                "          for proper deserialization\n" +
                "        type: string";

        SwaggerParser parser = new SwaggerParser();
        SwaggerDeserializationResult result = parser.readWithInfo(yaml);

        Set<String> messages = new HashSet<String>(result.getMessages());
        assertFalse(messages.contains("attribute definitions.Animal.discriminator is unexpected"));
    }

    @Test
    public void testIssue161() {
        String yaml =
                "swagger: '2.0'\n" +
                "paths:\n" +
                "  /users:\n" +
                "    get:\n" +
                "      parameters:\n" +
                "        - in: query\n" +
                "          name: name\n" +
                "          type: string\n" +
                "          minLength: 10\n" +
                "          maxLength: 100\n" +
                "          required: false\n" +
                "      responses:\n" +
                "        default:\n" +
                "          description: ok";
        SwaggerParser parser = new SwaggerParser();
        SwaggerDeserializationResult result = parser.readWithInfo(yaml);

        Set<String> messages = new HashSet<String>(result.getMessages());
        assertFalse(messages.contains("attribute paths.'/users'(get).[name].maxLength is unexpected"));
    }

    @Test
    public void testValidatorIssue50() {
        String json = "{\n" +
                "  \"swagger\": \"2.0\",\n" +
                "  \"info\": {\n" +
                "    \"version\": \"2.0.0\",\n" +
                "    \"title\": \"Beanhunter API\",\n" +
                "    \"description\": \"Description of the api goes here.\"\n" +
                "  },\n" +
                "  \"host\": \"local.xxx.com\",\n" +
                "  \"schemes\": [\n" +
                "    \"http\"\n" +
                "  ],\n" +
                "  \"consumes\": [\n" +
                "    \"application/json\"\n" +
                "  ],\n" +
                "  \"produces\": [\n" +
                "    \"application/json\"\n" +
                "  ],\n" +
                "  \"paths\": {\n" +
                "    \"/city\": {\n" +
                "      \"get\": {\n" +
                "        \"description\": \"test description\",\n" +
                "        \"responses\": {}\n" +
                "      }\n" +
                "    }\n" +
                "  },\n" +
                "  \"definitions\": {\n" +
                "    \"Endpoints\": {\n" +
                "      \"title\": \"Endpoints object\",\n" +
                "      \"properties\": {\n" +
                "        \"links\": {}\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}";

        SwaggerParser parser = new SwaggerParser();
        SwaggerDeserializationResult result = parser.readWithInfo(json);

        assertTrue(result.getMessages().size() == 1);
    }

    public void testIssue151() throws Exception {
        String json =
                "{\n" +
                "    \"swagger\": \"2.0\",\n" +
                "    \"paths\": {\n" +
                "        \"/\": {\n" +
                "            \"get\": {\n" +
                "                \"responses\": {\n" +
                "                    \"200\": {\n" +
                "                        \"description\": \"OK\"\n" +
                "                    }\n" +
                "                }\n" +
                "            }\n" +
                "        }\n" +
                "    },\n" +
                "    \"definitions\": {\n" +
                "        \"Pet\": {\n" +
                "            \"type\": \"object\",\n" +
                "            \"required\": [\n" +
                "                \"id\"\n" +
                "            ],\n" +
                "            \"properties\": {\n" +
                "                \"id\": {\n" +
                "                    \"type\": \"integer\",\n" +
                "                    \"format\": \"int64\"\n" +
                "                }\n" +
                "            }\n" +
                "        },\n" +
                "        \"Dog\": {\n" +
                "            \"type\": \"object\",\n" +
                "            \"allOf\": [\n" +
                "                {\n" +
                "                    \"$ref\": \"#/definitions/Pet\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"required\": [\n" +
                "                        \"name\"\n" +
                "                    ],\n" +
                "                    \"properties\": {\n" +
                "                        \"name\": {\n" +
                "                            \"type\": \"string\"\n" +
                "                        }\n" +
                "                    }\n" +
                "                }\n" +
                "            ],\n" +
                "            \"x-vendor-ext\": \"some data\"\n" +
                "        }\n" +
                "    }\n" +
                "}";

        SwaggerParser parser = new SwaggerParser();

        SwaggerDeserializationResult result = parser.readWithInfo(json);
        Swagger swagger = result.getSwagger();

        Map<String, Model> definitions = swagger.getDefinitions();
        assertTrue(definitions.size() == 2);
        Model allOfModel = definitions.get("Dog");
        assertTrue(allOfModel instanceof ComposedModel);
        assertFalse(allOfModel.getVendorExtensions().isEmpty());
        assertEquals("some data", allOfModel.getVendorExtensions().get("x-vendor-ext"));
    }

    @Test
    public void testExtractParametersOne() {
        SwaggerDeserializer deserializer = new SwaggerDeserializer();

        final String path = "http://foo.bar.com/{id}";
        final List<String> expectedParams = new ArrayList<>();
        expectedParams.add("id");

        final Optional<List<String>> extractedParams = deserializer.extractPathParameters(path);

        assertEquals(expectedParams, extractedParams.get());
    }

    @Test
    public void testExtractParametersTwo() {
        SwaggerDeserializer deserializer = new SwaggerDeserializer();

        final String path = "http://foo.bar.com/{id}/{foo}";
        final List<String> expectedParams = new ArrayList<>();
        expectedParams.add("id");
        expectedParams.add("foo");

        final Optional<List<String>> extractedParams = deserializer.extractPathParameters(path);

        assertEquals(expectedParams, extractedParams.get());
    }

    @Test
    public void testExtractParametersThree() {
        SwaggerDeserializer deserializer = new SwaggerDeserializer();

        final String path = "http://foo.bar.com/{id}/{foo}/zzzz/bbb/{bar}";
        final List<String> expectedParams = new ArrayList<>();
        expectedParams.add("id");
        expectedParams.add("foo");
        expectedParams.add("bar");

        final Optional<List<String>> extractedParams = deserializer.extractPathParameters(path);

        assertEquals(expectedParams, extractedParams.get());
    }

    @Test
    public void testExtractParametersNoClosingBrace() {
        SwaggerDeserializer deserializer = new SwaggerDeserializer();

        final String path = "http://foo.bar.com/{id";
        final Optional<List<String>> extractedParams = deserializer.extractPathParameters(path);
        assertFalse(extractedParams.isPresent());
    }

    @Test
    public void testExtractParametersNoParams() {
        SwaggerDeserializer deserializer = new SwaggerDeserializer();

        final String path = "http://foo.bar.com";
        final Optional<List<String>> extractedParams = deserializer.extractPathParameters(path);
        assertTrue(extractedParams.get().isEmpty());
    }

    @Test
    public void testExtractParametersEmptyParam() {
        SwaggerDeserializer deserializer = new SwaggerDeserializer();

        final String path = "http://foo.bar.com/{}";
        final Optional<List<String>> extractedParams = deserializer.extractPathParameters(path);
        assertFalse(extractedParams.isPresent());
    }

    @Test
    public void testExtractParametersWhitespaceParam() {
        SwaggerDeserializer deserializer = new SwaggerDeserializer();

        final String path = "http://foo.bar.com/{    }";
        final Optional<List<String>> extractedParams = deserializer.extractPathParameters(path);
        assertFalse(extractedParams.isPresent());
    }

    @Test
    public void testExtractParametersClosingThenOpeningBraces() {
        SwaggerDeserializer deserializer = new SwaggerDeserializer();

        final String path = "http://foo.bar.com/}{";
        final Optional<List<String>> extractedParams = deserializer.extractPathParameters(path);
        assertFalse(extractedParams.isPresent());
    }

    @Test
    public void testExtractParametersClosingThenOpeningBracesWithWhitespace() {
        SwaggerDeserializer deserializer = new SwaggerDeserializer();

        final String path = "http://foo.bar.com/}   {";
        final Optional<List<String>> extractedParams = deserializer.extractPathParameters(path);
        assertFalse(extractedParams.isPresent());
    }

    @Test
    public void testExtractParameters3Params() {
        SwaggerDeserializer deserializer = new SwaggerDeserializer();

        final String path = "http://foo.bar.com/{id}/{foo}/{b}";
        final List<String> expectedParams = new ArrayList<>();
        expectedParams.add("id");
        expectedParams.add("foo");
        expectedParams.add("b");

        final Optional<List<String>> actualParams = deserializer.extractPathParameters(path);
        assertEquals(actualParams.get(), expectedParams);
    }
    
    final Collection<Parameter> emptyPathSpecificParameters = Collections.emptyList();

    @Test
    public void testParametersMatchPathParameters() {
        SwaggerDeserializer deserializer = new SwaggerDeserializer();

        final String path = "http://foo.bar.com/{id}";
        final List<Parameter> operationSpecificParameters = new ArrayList<>();
        final PathParameter p = new PathParameter();
        p.setName("id");
        operationSpecificParameters.add(p);

        final Optional<Boolean> matches = deserializer.pathParametersMatchParameters(path, emptyPathSpecificParameters, operationSpecificParameters);

        assertTrue(matches.get());
    }

    @Test
    public void testParametersOnePathParameterMatching() {
        SwaggerDeserializer deserializer = new SwaggerDeserializer();

        final String path = "http://foo.bar.com/{id}";
        final List<Parameter> operationSpecificParameters = new ArrayList<>();
        final PathParameter p = new PathParameter();
        p.setName("id");

        final BodyParameter bp = new BodyParameter();
        bp.setName("foo");

        operationSpecificParameters.add(p);
        operationSpecificParameters.add(bp);

        final Optional<Boolean> matches =
                deserializer.pathParametersMatchParameters(path, emptyPathSpecificParameters, operationSpecificParameters);

        assertTrue(matches.get());
    }

    @Test
    public void testParametersNoPathParameters() {
        SwaggerDeserializer deserializer = new SwaggerDeserializer();

        final String path = "http://www.google.com";
        final List<Parameter> operationSpecificParameters = new ArrayList<>();

        final BodyParameter bp = new BodyParameter();
        bp.setName("foo");

        operationSpecificParameters.add(bp);

        final Optional<Boolean> matches = deserializer.pathParametersMatchParameters(path, emptyPathSpecificParameters, operationSpecificParameters);

        assertTrue(matches.get());
    }

    @Test
    public void testParametersNoMatchDifferentValues() {
        SwaggerDeserializer deserializer = new SwaggerDeserializer();

        final String path = "http://foo.bar.com/{id}";
        final List<Parameter> operationSpecificParameters = new ArrayList<>();
        final PathParameter p = new PathParameter();
        p.setName("foobar");
        operationSpecificParameters.add(p);

        final Optional<Boolean> matches = deserializer.pathParametersMatchParameters(path, emptyPathSpecificParameters, operationSpecificParameters);

        assertFalse(matches.get());
    }

    @Test
    public void testParametersMatchNoParams() {
        SwaggerDeserializer deserializer = new SwaggerDeserializer();

        final Collection<Parameter> emptyOperationSpecificParameters = Collections.emptyList();

        final String path = "http://foo.bar.com";
        final Optional<Boolean> matches = deserializer.pathParametersMatchParameters(path, emptyPathSpecificParameters,emptyOperationSpecificParameters);

        assertTrue(matches.get());
    }

    @Test
    public void testNoPathParametersInPathButOneOperationSpecificParameter() {
        SwaggerDeserializer deserializer = new SwaggerDeserializer();

        final String path = "http://foo.bar.com";
        final List<Parameter> operationSpecificParameters = new ArrayList<>();
        final PathParameter p = new PathParameter();
        p.setName("id");
        operationSpecificParameters.add(p);

        final Optional<Boolean> matches = deserializer.pathParametersMatchParameters(path, emptyPathSpecificParameters, operationSpecificParameters);

        assertTrue(matches.get());
    }

    @Test
    public void validateSwaggerPathsNoPathParams() {
        final SwaggerParser parser = new SwaggerParser();
        final Swagger swagger = parser.read("minimal_j");

        final Optional<Boolean> actualResult = allPathParametersAccountForExtractedPathParameters(swagger);
        final Optional<Boolean> expectedResult = Optional.of(Boolean.TRUE);

        assertEquals(expectedResult, actualResult);
    }

    @Test
    public void validateSwaggerPathsPathParamsOnlyInPath() {
        final SwaggerParser parser = new SwaggerParser();
        final Swagger swagger = parser.read("path_param_test_path_parameter_path_only");

        assertNull(swagger);
    }

    @Test
    public void validateSwaggerPathsPathParamsOnlyInParametersKeyValues() {
        final SwaggerParser parser = new SwaggerParser();
        final Swagger swagger = parser.read("path_param_test_parameters_in_path_body_only");

        final Optional<Boolean> actualResult = allPathParametersAccountForExtractedPathParameters(swagger);
        final Optional<Boolean> expectedResult = Optional.of(Boolean.TRUE);

        assertEquals(expectedResult, actualResult);
    }

    @Test
    public void validateSwaggerPathsPathOperationParamsMatch() {
        final SwaggerParser parser = new SwaggerParser();
        final Swagger swagger = parser.read("path_param_test_params_match");

        final Optional<Boolean> actualResult = allPathParametersAccountForExtractedPathParameters(swagger);
        final Optional<Boolean> expectedResult = Optional.of(Boolean.TRUE);

        assertEquals(expectedResult, actualResult);
    }

    @Test
    public void validateSwaggerPathsGlobalPathParamMatches() {
        final SwaggerParser parser = new SwaggerParser();
        final Swagger swagger = parser.read("path_param_test_path_global_params_match");

        assertNull(swagger);
    }

    @Test
    public void validateSwaggerPathsPathSpecificPathParamMatches() {
        final SwaggerParser parser = new SwaggerParser();
        final Swagger swagger      = parser.read("path_param_test_path_path_specific_params_match");

        final Optional<Boolean> actualResult   = allPathParametersAccountForExtractedPathParameters(swagger);
        final Optional<Boolean> expectedResult = Optional.of(Boolean.TRUE);

        assertEquals(expectedResult, actualResult);
    }

    @Test
    public void validateSwaggerPathsPathParamsMatchTwoPathParams() {
        final SwaggerParser parser = new SwaggerParser();
        final Swagger swagger = parser.read("path_param_test_path_2_params_match");

        final Optional<Boolean> actualResult = allPathParametersAccountForExtractedPathParameters(swagger);
        final Optional<Boolean> expectedResult = Optional.of(Boolean.TRUE);

        assertEquals(expectedResult, actualResult);
    }

    @Test
    public void validateSwaggerPathsInvalidPath() {
        final SwaggerParser parser = new SwaggerParser();
        final Swagger swagger = parser.read("minimal_invalid_path");

        assertNull(swagger);
    }

    @Test
    public void validateSwaggerPathsTwoOperations() {
        final SwaggerParser parser = new SwaggerParser();
        final Swagger swagger = parser.read("path_param_test_path_2_operations_matching_params");

        final Optional<Boolean> actualResult = allPathParametersAccountForExtractedPathParameters(swagger);
        final Optional<Boolean> expectedResult = Optional.of(Boolean.TRUE);

        assertEquals(expectedResult, actualResult);
    }

    @Test
    public void validateSwaggerDocPathsTwoOperations() {
        final SwaggerParser parser = new SwaggerParser();
        final Swagger swagger = parser.read("path_param_test_path_2_operations_matching_params");

        assertNotNull(swagger);
    }

    @Test
    public void validateSwaggerPathsThreePathParamsParamFromEach() {
        final SwaggerParser parser = new SwaggerParser();
        final Swagger swagger = parser.read("path_param_test_path_3_path_params_each_type");

        assertNull(swagger);
    }

    @Test
    public void validateSwaggerDocThreePathParamsParamFromEach() {
        final SwaggerParser parser = new SwaggerParser();
        final Swagger swagger = parser.read("path_param_test_path_3_path_params_each_type");

        assertNull(swagger);
    }

    @Test
    public void singlePathHasNoParamsButDocHasEachTypeOfPathParam() {
        final SwaggerParser parser = new SwaggerParser();
        final Swagger swagger = parser.read("path_param_test_path_no_params_with_each_param");

        final Optional<Boolean> actualResult = allPathParametersAccountForExtractedPathParameters(swagger);
        final Optional<Boolean> expectedResult = Optional.of(Boolean.TRUE);

        assertEquals(expectedResult, actualResult);
    }

    @Test
    public void validateSwaggerDocsinglePathHasNoParamsButDocHasEachTypeOfPathParam() {
        final SwaggerParser parser = new SwaggerParser();
        final Swagger swagger = parser.read("path_param_test_path_no_params_with_each_param");

        assertNotNull(swagger);
    }

    @Test
    public void validateSwaggerPathsThreePathParamsMissingOperationSpecificParam() {
        final SwaggerParser parser = new SwaggerParser();
        final Swagger swagger = parser.read("path_param_test_path_3_path_params_missing_operation_specific");
        assertNull(swagger);
    }

    @Test
    public void validateSwaggerDocThreePathParamsMissingOperationSpecificParam() {
        final SwaggerParser parser = new SwaggerParser();
        final Swagger swagger = parser.read("path_param_test_path_3_path_params_missing_operation_specific");

        assertNull(swagger);
    }

    @Test
    public void validateSwaggerPathsTwoOperationsFirstOperationWrongPathParams() {
        final SwaggerParser parser = new SwaggerParser();
        final Swagger swagger = parser.read("path_param_test_path_2_operations_non_matching_params");

        assertNull(swagger);
    }

}
