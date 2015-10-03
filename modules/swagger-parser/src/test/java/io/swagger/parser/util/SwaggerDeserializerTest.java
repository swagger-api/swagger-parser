package io.swagger.parser.util;

import io.swagger.models.Contact;
import io.swagger.models.Swagger;
import io.swagger.parser.SwaggerParser;
import io.swagger.util.Json;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SwaggerDeserializerTest {
//    @Test
    public void testEmpty() {
        String json = "{}";

        SwaggerParser parser = new SwaggerParser();

        SwaggerDeserializationResult result = parser.parseWithInfo(json);
        List<String> messageList = result.getMessages();
        Set<String> messages = new HashSet<String>(messageList);

        assertTrue(messages.contains("attribute swagger is missing"));
        assertTrue(messages.contains("attribute info is missing"));
        assertTrue(messages.contains("attribute paths is missing"));
    }

//    @Test
    public void testRootInfo() {
        String json = "{\n" +
                "\t\"swagger\": \"2.0\",\n" +
                "\t\"foo\": \"bar\",\n" +
                "\t\"info\": \"invalid\"\n" +
                "}";

        SwaggerParser parser = new SwaggerParser();

        SwaggerDeserializationResult result = parser.parseWithInfo(json);
        List<String> messageList = result.getMessages();
        Set<String> messages = new HashSet<String>(messageList);

        assertTrue(messages.contains("attribute foo is unexpected"));
        assertTrue(messages.contains("attribute info is not of type `object`"));
    }

//    @Test
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

        SwaggerDeserializationResult result = parser.parseWithInfo(json);
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

        assertEquals(result.getSwagger().getInfo().getVendorExtensions().get("x-foo"), "\"bar\"");
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

        SwaggerDeserializationResult result = parser.parseWithInfo(json);
        List<String> messageList = result.getMessages();
        Set<String> messages = new HashSet<String>(messageList);

        assertTrue(messages.contains("attribute responses.foo.bar is unexpected"));

        assertEquals(result.getSwagger().getResponses().get("foo").getVendorExtensions().get("x-foo"), "\"bar\"");
    }
}
