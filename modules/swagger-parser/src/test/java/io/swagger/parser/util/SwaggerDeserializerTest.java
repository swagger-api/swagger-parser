package io.swagger.parser.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.models.*;
import io.swagger.models.auth.ApiKeyAuthDefinition;
import io.swagger.models.auth.BasicAuthDefinition;
import io.swagger.models.auth.In;
import io.swagger.models.auth.SecuritySchemeDefinition;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.parameters.QueryParameter;
import io.swagger.models.properties.*;
import io.swagger.parser.SwaggerParser;
import io.swagger.util.Json;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
}
