package io.swagger.v3.parser.test;

import io.swagger.v3.oas.models.media.Discriminator;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class Issue2406Test {

    @Test
    public void shouldUpdateDiscriminatorMappingOfOneOfNestedInArrayItemsWithExternalRefs() {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        SwaggerParseResult result = new OpenAPIV3Parser().readLocation("issue-2406/openapi.yaml", null, options);

        assertEquals(result.getMessages().toString(), 0, result.getMessages().size());

        Schema<?> searchRequest = result.getOpenAPI().getComponents().getSchemas().get("SearchRequest");
        Schema<?> items = ((Schema<?>) searchRequest.getProperties().get("query")).getItems();
        List<String> oneOfRefs = items.getOneOf().stream().map(Schema::get$ref).collect(Collectors.toList());
        assertEquals(Arrays.asList("#/components/schemas/TypeA", "#/components/schemas/TypeB"), oneOfRefs);

        Discriminator discriminator = items.getDiscriminator();
        assertEquals("kind", discriminator.getPropertyName());
        // no entry with a null key must be added, and the mapping must follow the rewritten oneOf refs
        assertFalse(discriminator.getMapping().containsKey(null));
        assertEquals(3, discriminator.getMapping().size());
        assertEquals("#/components/schemas/TypeA", discriminator.getMapping().get("a"));
        assertEquals("#/components/schemas/TypeB", discriminator.getMapping().get("b"));
        assertEquals("#/components/schemas/TypeB", discriminator.getMapping().get("bLegacy"));
    }
}
