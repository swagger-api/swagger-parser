package io.swagger.parserExtensions;

import io.swagger.parser.SwaggerParser;
import io.swagger.parser.SwaggerParserExtension;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertTrue;

public class ParserExtensionsTest {
    @Test
    public void readAllExtensions() throws Exception {
        SwaggerParser parser = new SwaggerParser();
        List<SwaggerParserExtension> extensions = parser.getExtensions();
        assertTrue(extensions.size() == 2, "Didn't find 2 extensions as expected");
    }
}