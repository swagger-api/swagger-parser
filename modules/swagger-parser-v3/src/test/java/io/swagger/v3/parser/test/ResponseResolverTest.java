package io.swagger.v3.parser.test;


import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.testng.annotations.Test;

import static org.testng.Assert.assertNotNull;

public class ResponseResolverTest {
    static String spec =
            "openapi: '3.0.0'\n" +
            "info:\n" +
            "  version: \"0.0.0\"\n" +
            "  title: a test\n" +
            "paths:\n" +
            "  /persons:\n" +
            "    get:\n" +
            "      parameters: []\n" +
            "      responses:\n" +
            "        x-it-hurts: true\n" +
            "        200:\n" +
            "          description: Successful response\n";

    @Test
    public void testIssue211() throws Exception {
        SwaggerParseResult result = new OpenAPIV3Parser().readContents(spec,null,null);

        OpenAPI openAPI = result.getOpenAPI();
        assertNotNull(openAPI);
        assertNotNull(openAPI.getPaths().get("/persons").getGet().getResponses().get("200"));
    }
}
