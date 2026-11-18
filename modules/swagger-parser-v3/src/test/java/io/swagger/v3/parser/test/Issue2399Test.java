package io.swagger.v3.parser.test;

import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class Issue2399Test {

    @Test
    public void resolvingRootDocumentAliasRetainsReferencedSchema() {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);

        SwaggerParseResult result = new OpenAPIV3Parser().readLocation(
                "src/test/resources/issue-2399/root-alias.json", null, options);

        assertNotNull(result.getOpenAPI());
        assertTrue(result.getMessages().isEmpty(), "Unexpected parser messages: " + result.getMessages());

        Map<String, Schema> schemas = result.getOpenAPI().getComponents().getSchemas();
        assertEquals(schemas.size(), 2);
        assertEquals(schemas.get("Target").getType(), "string");
        assertEquals(schemas.get("Alias").getType(), "string");
    }

    @Test
    public void resolveFullyPreservesDeclaredComponentKeyForRootDocumentBackReference() {
        ParseOptions options = new ParseOptions();
        options.setResolveResponses(true);
        options.setValidateExternalRefs(true);
        options.setResolveFully(true);

        SwaggerParseResult result = new OpenAPIV3Parser().readLocation(
                "src/test/resources/issue-2399/api.json", null, options);

        assertNotNull(result.getOpenAPI());
        assertTrue(result.getMessages().isEmpty(), "Unexpected parser messages: " + result.getMessages());

        Map<String, Schema> schemas = result.getOpenAPI().getComponents().getSchemas();
        assertEquals(schemas.size(), 4);
        assertTrue(schemas.keySet().containsAll(Arrays.asList(
                "Order-item", "Order-status", "orderItem", "orderStatus")));
        assertFalse(schemas.containsKey("Order-status_1"));

        Schema<?> responseSchema = result.getOpenAPI().getPaths().get("/orders").getGet()
                .getResponses().get("200").getContent().get("application/json").getSchema();
        Schema<?> orderSchema = responseSchema.getItems();
        Schema<?> statusSchema = (Schema<?>) orderSchema.getProperties().get("status");
        assertEquals(statusSchema.getEnum(), Arrays.asList("PENDING", "SHIPPED"));
    }
}
