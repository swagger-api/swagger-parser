package io.swagger.parser;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

import java.util.List;

public class SwaggerParserExtensionTest {
    @Test
    public void verifyDefaultExtension() {
        final SwaggerParser parser = new SwaggerParser();
        final List<SwaggerParserExtension> extensions = parser.getExtensions();
        assertTrue(extensions.size() > 0);
        assertTrue(extensions.get(0) instanceof Swagger20Parser);
        assertEquals(extensions.get(0).getClass(), Swagger20Parser.class);
    }
}
