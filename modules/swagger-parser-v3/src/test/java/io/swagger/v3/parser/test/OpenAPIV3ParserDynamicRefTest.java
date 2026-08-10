package io.swagger.v3.parser.test;

import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class OpenAPIV3ParserDynamicRefTest {
    @Test
    public void testDynamicRefParsing() {
        SwaggerParseResult result = new OpenAPIV3Parser()
                .readLocation("dynamicRef/dynamicref-example.yaml", null, null);

        assertNotNull(result.getOpenAPI(), "Parsed OpenAPI object should not be null");

        Schema<?> rootSchema = result.getOpenAPI()
                .getPaths().get("/tree").getGet()
                .getResponses().get("200").getContent()
                .get("application/json").getSchema();

        assertEquals(rootSchema.get$ref(), "#/components/schemas/Node",
                "Expected root schema to be a $ref to Node");

        Schema<?> nodeSchema = result.getOpenAPI().getComponents()
                .getSchemas().get("Node");

        assertNotNull(nodeSchema, "Node schema should be parsed");

        Schema<?> childrenSchema = (Schema<?>) nodeSchema.getProperties().get("children");
        Schema<?> itemsSchema = childrenSchema.getItems();

        // THIS is the actual test: you should have a get$dynamicRef() field
        assertEquals("#node", itemsSchema.get$dynamicRef(), "Expected $dynamicRef to be preserved");
    }
}
