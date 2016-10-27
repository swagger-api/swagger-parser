package io.swagger.parser;

import io.swagger.models.Model;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.Swagger;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.RefProperty;
import io.swagger.parser.util.SwaggerDeserializationResult;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class FileReferenceTests {
    @Test
    public void testIssue306() {
        SwaggerDeserializationResult result = new SwaggerParser().readWithInfo("./src/test/resources/nested-file-references/issue-306.yaml", null, true);
        assertNotNull(result.getSwagger());

        Swagger swagger = result.getSwagger();

        assertTrue(swagger.getDefinitions().size() == 3);
        // resolved from `$ref: './book.yaml'`
        assertNotNull(swagger.getDefinitions().get("Inventory"));
        // resolved from `$ref: 'book.yaml'`
        assertNotNull(swagger.getDefinitions().get("Orders"));

        // copied from `./book.yaml`
        assertNotNull(swagger.getDefinitions().get("book"));
    }

    @Test
    public void testIssue308() {
        SwaggerDeserializationResult result = new SwaggerParser().readWithInfo("./src/test/resources/nested-file-references/issue-308.yaml", null, true);
        assertNotNull(result.getSwagger());

        Swagger swagger = result.getSwagger();

        assertTrue(swagger.getDefinitions().size() == 2);
        assertTrue(swagger.getDefinitions().get("Paging").getProperties().size() == 1);
    }

    @Test
    public void testIssue310() {
        SwaggerDeserializationResult result = new SwaggerParser().readWithInfo("./src/test/resources/nested-file-references/issue-310.yaml", null, true);
        assertNotNull(result.getSwagger());

        Swagger swagger = result.getSwagger();

        assertTrue(swagger.getDefinitions().size() == 2);
        assertTrue(swagger.getDefinitions().get("Paging").getProperties().size() == 1);
    }

    @Test
    public void testIssue312() {
        SwaggerDeserializationResult result = new SwaggerParser().readWithInfo("./src/test/resources/nested-file-references/issue-312.yaml", null, true);
        assertNotNull(result.getSwagger());

        Swagger swagger = result.getSwagger();
        assertNotNull(swagger.getPath("/events"));
        Path path = swagger.getPath("/events");
        assertNotNull(path.getGet());

        Operation get = path.getGet();
        assertEquals(get.getOperationId(), "getEvents");
        assertTrue(swagger.getDefinitions().size() == 2);
        assertTrue(swagger.getDefinitions().get("Paging").getProperties().size() == 1);
    }

    @Test
    public void testIssue314() {
        SwaggerDeserializationResult result = new SwaggerParser().readWithInfo("./src/test/resources/nested-file-references/issue-314.yaml", null, true);
        assertNotNull(result.getSwagger());

        Swagger swagger = result.getSwagger();
        assertNotNull(swagger.getPath("/events"));
        Path path = swagger.getPath("/events");
        assertNotNull(path.getGet());

        Operation get = path.getGet();
        assertEquals(get.getOperationId(), "getEvents");
        assertTrue(swagger.getDefinitions().size() == 3);
        assertTrue(swagger.getDefinitions().get("Foobar").getProperties().size() == 1);
        assertTrue(swagger.getDefinitions().get("StatusResponse").getProperties().size() == 1);
        assertTrue(swagger.getDefinitions().get("Paging").getProperties().size() == 1);
    }

    @Test
    public void testIssue316() {
        SwaggerDeserializationResult result = new SwaggerParser().readWithInfo("./src/test/resources/nested-file-references/issue-316.yaml", null, true);
        assertNotNull(result.getSwagger());

        Swagger swagger = result.getSwagger();
        assertNotNull(swagger.getPath("/events"));
        Path path = swagger.getPath("/events");
        assertNotNull(path.getGet());
        Operation get = path.getGet();
        assertEquals(get.getOperationId(), "getEvents");
        assertTrue(swagger.getDefinitions().size() == 3);
        assertTrue(swagger.getDefinitions().get("Foobar").getProperties().size() == 1);
        assertTrue(swagger.getDefinitions().get("StatusResponse").getProperties().size() == 1);
        assertTrue(swagger.getDefinitions().get("Paging2").getProperties().size() == 2);
        Model model = swagger.getDefinitions().get("Paging2");

        Property property = model.getProperties().get("foobar");
        assertTrue(property instanceof RefProperty);
        RefProperty ref = (RefProperty) property;
        assertEquals(ref.get$ref(), "#/definitions/Foobar");
    }
}
