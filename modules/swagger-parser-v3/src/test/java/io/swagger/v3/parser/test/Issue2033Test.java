package io.swagger.v3.parser.test;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.links.Link;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class Issue2033Test {

    @Test
    public void resolvesRelativeComponentRefsFromExternalTemplatedPathItem() {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);

        SwaggerParseResult result = new OpenAPIV3Parser().readLocation(
                "src/test/resources/issue-2033/main.yaml", null, options);

        assertNotNull(result.getOpenAPI());
        assertTrue(result.getMessages().isEmpty(), "Unexpected parser messages: " + result.getMessages());

        OpenAPI openAPI = result.getOpenAPI();
        ApiResponse partialResponse = openAPI.getPaths()
                .get("/nodes/{uuid}/rights")
                .getGet()
                .getResponses()
                .get("206");

        Header contentRange = partialResponse.getHeaders().get("Content-Range");

        assertNotNull(contentRange);
        assertEquals(contentRange.get$ref(), "#/components/headers/Content-Range");

        Header resolvedContentRange = openAPI.getComponents().getHeaders().get("Content-Range");
        assertNotNull(resolvedContentRange);
        assertNotNull(resolvedContentRange.getSchema());
        assertEquals(resolvedContentRange.getSchema().getType(), "string");
        assertEquals(resolvedContentRange.getSchema().getPattern(), "\\d+-\\d+/\\d+");

        Link nextPage = partialResponse.getLinks().get("nextPage");
        assertNotNull(nextPage);
        assertEquals(nextPage.get$ref(), "#/components/links/NextPage");

        Link resolvedNextPage = openAPI.getComponents().getLinks().get("NextPage");
        assertNotNull(resolvedNextPage);
        assertEquals(resolvedNextPage.getOperationId(), "listNodeRights");

        Example contentRangeExample = partialResponse.getContent()
                .get("application/json")
                .getExamples()
                .get("contentRange");
        assertNotNull(contentRangeExample);
        assertEquals(contentRangeExample.get$ref(), "#/components/examples/ContentRangeExample");

        Example resolvedExample = openAPI.getComponents().getExamples().get("ContentRangeExample");
        assertNotNull(resolvedExample);
        assertEquals(resolvedExample.getValue(), "items 0-9/42");
    }
}
