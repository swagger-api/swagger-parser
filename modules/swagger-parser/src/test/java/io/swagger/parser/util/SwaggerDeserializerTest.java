package io.swagger.parser.util;

import io.swagger.models.Contact;
import io.swagger.models.ModelImpl;
import io.swagger.models.SecurityRequirement;
import io.swagger.models.Swagger;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.RefProperty;
import io.swagger.parser.SwaggerParser;
import io.swagger.util.Json;
import io.swagger.models.properties.IntegerProperty;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SwaggerDeserializerTest {
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

        assertEquals(result.getSwagger().getInfo().getVendorExtensions().get("x-foo").toString(), "\"bar\"");
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

        assertEquals(result.getSwagger().getResponses().get("foo").getVendorExtensions().get("x-foo").toString(), "\"bar\"");
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
}
