package io.swagger.parser;

import io.swagger.models.Swagger;
import io.swagger.parser.util.SwaggerDeserializationResult;
import org.testng.annotations.Test;

import static org.testng.Assert.assertNotNull;

public class NetworkReferenceTests {
    @Test(enabled = false)
    public void testIssue323() {
        SwaggerDeserializationResult result = new SwaggerParser().readWithInfo("http://localhost:8080/nested-file-references/issue-323.yaml", null, true);
        assertNotNull(result.getSwagger());

        Swagger swagger = result.getSwagger();
        assertNotNull(swagger.getPath("/events"));

        assertNotNull(swagger.getDefinitions().get("StatusResponse"));
        assertNotNull(swagger.getDefinitions().get("Paging"));
        assertNotNull(swagger.getDefinitions().get("Foobar"));
    }
}
