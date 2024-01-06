package io.swagger.v3.parser.test;


import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.apache.commons.io.IOUtils;
import org.testng.annotations.Test;

import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

/**
 * Test parsing classpath and file URIs.
 * Before the fix, an exception is logged as an error and a message containing "(No such file or directory)"
 * is added to the parse result.
 * This test checks for the absence of the message.
 */
public class OpenAPIV31ParserUriTest {
    @Test
    public void resolveFileInput() throws Exception {
        URI uri = getClass().getResource("/3.1.0/basic.yaml").toURI();
        assertEquals(uri.getScheme(), "file");
        String uriString = uri.toString();
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        SwaggerParseResult result = new OpenAPIV3Parser().readLocation(uriString, null, options);
        validateParseResult(result, "(No such file or directory)");
    }

    @Test
    public void resolveClasspathInput() throws Exception {
        URL url = getClass().getResource("/3.1.0/basic.yaml");
        String content = IOUtils.toString(url, StandardCharsets.UTF_8);
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        SwaggerParseResult result = new OpenAPIV3Parser().readContents(content, null, options, "classpath:/3.1.0/basic.yaml");
        validateParseResult(result, "Could not find classpath:/3.1.0/basic.yaml");
    }

    private static void validateParseResult(SwaggerParseResult result, String checkForMessage) {
        String noSuchFileMessage = result.getMessages().stream()
            .filter(message -> message.contains(checkForMessage))
                .findFirst()
                .orElse(null);
        assertNull(noSuchFileMessage);
    }
}
