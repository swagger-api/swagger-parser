package io.swagger.v3.parser.processors;

import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.testng.annotations.Test;

import java.nio.file.Paths;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;


public class RegressionTestIssue1948 {

    @Test
    public void testParentRefWithSpaceInDirectoryResolvesEndToEnd() {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        SwaggerParseResult result = new OpenAPIV3Parser()
                .readLocation("regression-parent-ref/openapi.yaml", null, options);

        assertNotNull(result.getOpenAPI());
        for (String message : result.getMessages()) {
            assertFalse(message.contains("Unable to load RELATIVE ref"), message);
        }

        Parameter parameter = result.getOpenAPI().getPaths().get("/products/{param1}")
                .getGet().getParameters().get(0);
        assertNotNull(parameter);
        assertEquals(parameter.getName(), "param1");
        assertEquals(parameter.getIn(), "path");
        assertNotNull(parameter.getSchema());
        assertEquals(parameter.getSchema().getType(), "string");
    }

}
