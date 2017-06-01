package io.swagger.parser.test;


import io.swagger.oas.models.OpenAPI;
import io.swagger.oas.models.PathItem;
import io.swagger.parser.models.SwaggerParseResult;
import io.swagger.parser.v2.SwaggerConverter;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Paths;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class V2ConverterTest {
    @Test
    public void testConvertPetstore() throws Exception {
        SwaggerConverter converter = new SwaggerConverter();
        String swaggerAsString = new String(Files.readAllBytes(Paths.get(getClass().getClassLoader().getResource("petstore.yaml").toURI())));
        SwaggerParseResult result = converter.readContents(swaggerAsString, null, null);

        assertNotNull(result.getOpenAPI());
    }

    @Test
    public void testIssue455() throws Exception {
        SwaggerConverter converter = new SwaggerConverter();
        String swaggerAsString = new String(Files.readAllBytes(Paths.get(getClass().getClassLoader().getResource("issue-455.json").toURI())));

        SwaggerParseResult result = converter.readContents(swaggerAsString, null, null);

        assertNotNull(result);

        OpenAPI oas = result.getOpenAPI();

        assertNotNull(oas);
        assertTrue(oas.getPaths().size() == 1);

        PathItem pathItem = oas.getPaths().get("/api/batch/");
        assertNotNull(pathItem);

        assertTrue(pathItem.getGet().getParameters().size() == 1);
    }
}
