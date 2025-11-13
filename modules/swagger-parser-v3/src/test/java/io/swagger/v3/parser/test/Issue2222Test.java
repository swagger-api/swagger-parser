package io.swagger.v3.parser.test;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.media.Discriminator;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class Issue2222Test {

    @Test
    public void testIssue2222() {
        ParseOptions options = new ParseOptions();
        options.setResolveFully(true);
        SwaggerParseResult result = new OpenAPIV3Parser().readLocation("issue-2222/openapi.yaml", null, options);

        // No error/warning messages should be present, especially Duplicate key ones
        assertEquals(0, result.getMessages().size());
        assertEquals(1, result.getOpenAPI().getPaths().size());
        ApiResponse response = result.getOpenAPI().getPaths().get("/mypath").readOperations().get(0).getResponses().get("200");
        Discriminator discriminator = response.getContent().get("application/json").getSchema().getItems().getDiscriminator();
        assertEquals("discriminatorProperty", discriminator.getPropertyName());
        assertEquals("#/components/schemas/TypeA", discriminator.getMapping().get("ONE"));
        assertEquals("#/components/schemas/TypeB", discriminator.getMapping().get("TWO"));
        assertEquals("#/components/schemas/TypeB", discriminator.getMapping().get("THREE"));
        assertEquals("#/components/schemas/TypeB", discriminator.getMapping().get("FOUR"));
        assertEquals("#/components/schemas/TypeB", discriminator.getMapping().get("FIVE"));
    }
}
