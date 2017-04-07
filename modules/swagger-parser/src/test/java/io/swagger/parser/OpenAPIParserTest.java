package io.swagger.parser;

import v2.io.swagger.util.Json;
import org.junit.Test;

public class OpenAPIParserTest {
    @Test
    public void testSimple() {
        Json.prettyPrint(new OpenAPIParser().readLocation("http://petstore.swagger.io/v2/swagger.json", null, null));
    }
}
