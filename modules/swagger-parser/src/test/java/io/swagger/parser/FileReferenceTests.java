package io.swagger.parser;

import io.swagger.models.Swagger;
import io.swagger.parser.util.SwaggerDeserializationResult;
import org.testng.annotations.Test;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

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
}
