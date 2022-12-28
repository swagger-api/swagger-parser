package io.swagger.v3.parser.test;

import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class BrokenRefTest {

    @Test
    public void testBrokenRef() {
        OpenAPIV3Parser openApiParser = new OpenAPIV3Parser();

        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        SwaggerParseResult parseResult = openApiParser.readLocation("broken-ref/openapi30.yaml", null, options);

        assertEquals (parseResult.getMessages ().size (), 0);
    }

}
