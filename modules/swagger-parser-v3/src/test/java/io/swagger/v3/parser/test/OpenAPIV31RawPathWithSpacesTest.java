package io.swagger.v3.parser.test;

import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

/**
 * Regression test for https://github.com/swagger-api/swagger-parser/issues/2365
 *
 * A raw local filesystem path accepted by {@link OpenAPIV3Parser#readLocation} must not
 * produce an "Illegal character in path" error while resolving same-file OpenAPI 3.1 refs,
 * even when the path contains characters that are illegal in a URI (e.g. spaces).
 */
public class OpenAPIV31RawPathWithSpacesTest {

    @Test
    public void resolveSameFileReferenceFromRawPathWithSpaces() throws Exception {
        Path directory = Files.createTempDirectory("openapi path with spaces");
        Path root = directory.resolve("openapi.yaml");

        Files.write(root, (
                "openapi: 3.1.0\n" +
                "info:\n" +
                "  title: Path With Spaces\n" +
                "  version: 1.0.0\n" +
                "paths:\n" +
                "  /examples:\n" +
                "    get:\n" +
                "      responses:\n" +
                "        '200':\n" +
                "          $ref: '#/components/responses/ExampleResponse'\n" +
                "components:\n" +
                "  responses:\n" +
                "    ExampleResponse:\n" +
                "      description: OK\n" +
                "      content:\n" +
                "        application/json:\n" +
                "          schema:\n" +
                "            $ref: '#/components/schemas/Example'\n" +
                "  schemas:\n" +
                "    Example:\n" +
                "      type: object\n" +
                "      properties:\n" +
                "        id:\n" +
                "          type: string\n").getBytes(StandardCharsets.UTF_8));

        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setResolveResponses(true);

        SwaggerParseResult result = new OpenAPIV3Parser().readLocation(root.toString(), null, options);

        assertNotNull(result.getOpenAPI());
        assertNotNull(result.getOpenAPI().getComponents().getResponses().get("ExampleResponse"));

        String illegalCharMessage = result.getMessages().stream()
                .filter(message -> message.contains("Illegal character in path"))
                .findFirst()
                .orElse(null);
        assertNull(illegalCharMessage, "unexpected parse error: " + illegalCharMessage);
    }
}
