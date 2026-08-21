package io.swagger.v3.parser.test;

import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.testng.annotations.Test;

import java.util.Map;

import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class Issue1961PhantomBackRefTest {

    private static final String ROOT_DOCUMENT =
            "src/test/resources/issue-1961-phantom/root.yaml";

    @Test(description = "A back-reference to a schema absent from the on-disk root document"
            + " must surface an error, not bind to a schema promoted during resolution")
    public void backRefToSchemaMissingFromRootDocumentIsReported() {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);

        SwaggerParseResult result = new OpenAPIV3Parser().readLocation(
                ROOT_DOCUMENT, null, options);

        assertNotNull(result.getOpenAPI());
        assertTrue(result.getMessages().stream().anyMatch(
                        m -> m.contains("Could not find components/schemas/Payload")),
                "The invalid back-reference must be reported; got: " + result.getMessages());

        Map<String, Schema> schemas = result.getOpenAPI().getComponents().getSchemas();
        Schema payload = schemas.get("Payload");
        assertNotNull(payload, "The promoted external Payload schema itself is expected");

        Schema x = (Schema) payload.getProperties().get("x");
        assertNotEquals(x.get$ref(), "#/components/schemas/Payload",
                "The phantom back-reference must not be silently rewritten into a"
                        + " self-reference to the promoted copy");
    }
}
