package io.swagger.v3.parser;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;

public class OpenAPIV3ParserLargeFilesTest {

    @BeforeSuite
    public static void init() {
        System.setProperty("maxYamlCodePoints", "10000000");
    }

    @AfterClass
    public void cleanUpAfterAllTests() {
        System.clearProperty("maxYamlCodePoints");
    }

    @Test
    public void issue2059() {
        OpenAPIV3Parser openApiParser = new OpenAPIV3Parser();
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        SwaggerParseResult parseResult = openApiParser.readLocation("issue2059/largeFile.yaml", null, options);
        OpenAPI openAPI = parseResult.getOpenAPI();

        assertNotNull(openAPI);
        assertFalse(parseResult.getMessages().stream().anyMatch(message -> message.contains("exceeds the limit: 3145728")));
    }
}