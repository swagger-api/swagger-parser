package io.swagger.v3.parser.test;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.junit.Test;

public class Issue2222Test {

    @Test
    public void testIssue2222() {
        ParseOptions options = new ParseOptions();
        options.setResolveFully(true);
        SwaggerParseResult result = new OpenAPIV3Parser().readLocation("issue-2222/openapi.yaml", null, options);

        System.out.println(Json.pretty(result.getOpenAPI()));
    }
}
