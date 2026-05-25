package io.swagger.v3.parser.test;

import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.testng.annotations.Test;

import static org.testng.Assert.assertFalse;

public class ResolverCacheCircularTest {
    @Test
    public void testIssue1961_DuplicateSchemas_ABA() throws Exception {
        ParseOptions parseOptions = new ParseOptions();
        parseOptions.setResolve(true);
        SwaggerParseResult result = new OpenAPIV3Parser().readLocation("src/test/resources/issue-1961/Foo.yaml", null, parseOptions);
        assertFalse(result.getOpenAPI().getComponents().getSchemas().containsKey("FooBar_1"));
    }
}