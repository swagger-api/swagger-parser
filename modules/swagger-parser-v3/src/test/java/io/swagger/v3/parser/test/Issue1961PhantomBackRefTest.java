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

/**
 * Guards the error signal for back-references to the root document that name
 * a schema the root does not define on disk.
 *
 * <p>During resolution, external schemas are promoted into the root's
 * components map. A root-document back-reference must be resolved against the
 * original root document, not against that mutated in-memory state — otherwise
 * an invalid reference (here {@code ./root.yaml#/components/schemas/Payload},
 * where the on-disk root has no {@code Payload}) silently binds to whatever
 * schema happens to have been promoted under that name first, making the
 * result dependent on resolution order.</p>
 */
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