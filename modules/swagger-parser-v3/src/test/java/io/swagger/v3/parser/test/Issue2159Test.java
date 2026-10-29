package io.swagger.v3.parser.test;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

public class Issue2159Test {

    @Test
    public void resolvesRefsWithPathTemplateInFragment() {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        SwaggerParseResult result = new OpenAPIV3Parser().readLocation("issue-2159/openapi.yaml", null, options);

        assertTrue(result.getMessages().isEmpty(), result.getMessages().toString());
        OpenAPI openAPI = result.getOpenAPI();

        PathItem local = openAPI.getPaths().get("/v2/test/{tid}");
        assertNull(local.get$ref());
        assertEquals(local.getGet().getParameters().get(0).getName(), "tid");

        PathItem external = openAPI.getPaths().get("/v1/items/{id}");
        assertNull(external.get$ref());
        assertEquals(external.getGet().getOperationId(), "getItem");

        Schema test = openAPI.getComponents().getSchemas().get("Test");
        assertNull(test.get$ref());
        assertNotNull(test.getProperties().get("id"));
    }
}
