package io.swagger.v3.parser.test;

import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class Issue2270Test {

    @Test
    public void shouldResolveRefInsideAllOfOfUnreferencedComponentSchema() {
        ParseOptions options = new ParseOptions();
        options.setResolveFully(true);
        // Keep the allOf structure so we can assert the inner $ref itself was resolved.
        options.setResolveCombinators(false);

        SwaggerParseResult result = new OpenAPIV3Parser().readLocation("issue-2270/openapi.yaml", null, options);

        assertNotNull(result.getOpenAPI());
        Schema<?> contact = result.getOpenAPI().getComponents().getSchemas().get("Contact");
        assertNotNull(contact);

        List<Schema> allOf = contact.getAllOf();
        assertNotNull(allOf);

        // The first allOf member is a $ref to SObject and, although Contact is not reachable
        // through any path, resolveFully(true) must still resolve it: the $ref must be gone
        // and the referenced schema's properties must be inlined.
        Schema<?> firstMember = allOf.get(0);
        assertNull("$ref inside allOf of an unreferenced component schema was not resolved",
                firstMember.get$ref());
        assertNotNull(firstMember.getProperties());
        assertTrue(firstMember.getProperties().containsKey("id"));
    }
}
