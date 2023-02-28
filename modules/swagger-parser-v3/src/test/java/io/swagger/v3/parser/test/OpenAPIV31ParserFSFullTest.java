package io.swagger.v3.parser.test;

import io.swagger.v3.core.util.Yaml31;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.apache.commons.io.FileUtils;
import org.testng.annotations.Test;

import java.io.File;

public class OpenAPIV31ParserFSFullTest {

    @Test
    public void testFull() throws Exception {
        ParseOptions p = new ParseOptions();
        p.setResolve(true);
        String uri = "3.1.0/dereference/fullFS/root.json";
        SwaggerParseResult swaggerParseResult = new OpenAPIV3Parser().readLocation(uri, null, p);
        org.testng.Assert.assertEquals(Yaml31.pretty(swaggerParseResult.getOpenAPI()), FileUtils.readFileToString(new File("src/test/resources/3.1.0/dereference/fullFS/dereferenced.yaml")));
    }

    @Test
    public void testCustomUrlResolver() throws Exception {
        ParseOptions p = new ParseOptions();
        p.setResolve(true);
        String uri = "3.1.0/dereference/custom/root.json";
        SwaggerParseResult swaggerParseResult = new OpenAPIV3Parser().readLocation(uri, null, p);
        org.testng.Assert.assertEquals(Yaml31.pretty(swaggerParseResult.getOpenAPI()), FileUtils.readFileToString(new File("src/test/resources/3.1.0/dereference/custom/dereferenced.yaml")));
    }
}
