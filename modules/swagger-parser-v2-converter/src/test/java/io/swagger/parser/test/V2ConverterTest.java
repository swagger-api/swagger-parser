package io.swagger.parser.test;


import io.swagger.parser.models.SwaggerParseResult;
import io.swagger.parser.v2.SwaggerConverter;
import org.testng.annotations.Test;
import v2.io.swagger.util.Json;

import java.nio.file.Files;
import java.nio.file.Paths;

public class V2ConverterTest {
    @Test
    public void testConvertPetstore() throws Exception {
        SwaggerConverter converter = new SwaggerConverter();
        String swaggerAsString = new String(Files.readAllBytes(Paths.get("src/test/resources/petstore.yaml")));
        SwaggerParseResult result = converter.readContents(swaggerAsString, null, null);

        Json.prettyPrint(result.getOpenAPI());
    }
}
