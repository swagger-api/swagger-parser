package io.swagger.parser;

import io.swagger.parser.util.SwaggerDeserializationResult;
import io.swagger.util.Json;
import org.testng.annotations.Test;

public class ResponseResolverTest {
    static String spec =
            "swagger: '2.0'\n" +
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
        SwaggerDeserializationResult result = new SwaggerParser().readWithInfo(spec);

        Json.prettyPrint(result);
    }
}
