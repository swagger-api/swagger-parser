package io.swagger.v3.parser.test;

import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class OpenAPIV3ParserDynamicRefTest {

    private ParseOptions resolveOptions() {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        return options;
    }

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

        assertEquals("#node", itemsSchema.get$dynamicRef(), "Expected $dynamicRef to be preserved");
    }

    @Test
    public void testIdWithComponentRef() {
        SwaggerParseResult result = new OpenAPIV3Parser()
                .readLocation("dynamicRef/id-with-ref.yaml", null, resolveOptions());

        assertTrue(result.getMessages() == null || result.getMessages().isEmpty(),
                "Expected no parse errors, got: " + result.getMessages());

        Schema<?> concrete = result.getOpenAPI().getComponents().getSchemas().get("Concrete");
        assertNotNull(concrete, "Concrete schema should exist");
        assertNotNull(concrete.get$ref(), "Concrete should still have $ref");
    }

    @Test
    public void testDynamicAnchorPreservedAfterResolution() {
        SwaggerParseResult result = new OpenAPIV3Parser()
                .readLocation("dynamicRef/dynamic-anchor-resolution.yaml", null, resolveOptions());

        assertTrue(result.getMessages() == null || result.getMessages().isEmpty(),
                "Expected no parse errors, got: " + result.getMessages());

        Schema<?> node = result.getOpenAPI().getComponents().getSchemas().get("Node");
        assertNotNull(node, "Node schema should exist");
        assertEquals(node.get$id(), "https://example.com/schemas/Node");

        Schema<?> childrenSchema = (Schema<?>) node.getProperties().get("children");
        Schema<?> itemsSchema = childrenSchema.getItems();
        assertEquals(itemsSchema.get$dynamicRef(), "#node",
                "$dynamicRef should be preserved after resolution");
    }

    @Test
    public void testDefsTraversed() {
        SwaggerParseResult result = new OpenAPIV3Parser()
                .readLocation("dynamicRef/dynamic-anchor-resolution.yaml", null, resolveOptions());

        Schema<?> node = result.getOpenAPI().getComponents().getSchemas().get("Node");
        assertNotNull(node.getExtensions(), "Extensions should not be null");
        assertNotNull(node.getExtensions().get("$defs"), "$defs should be preserved in extensions");

        @SuppressWarnings("unchecked")
        Map<String, Object> defs = (Map<String, Object>) node.getExtensions().get("$defs");
        assertTrue(defs.containsKey("node"), "$defs should contain 'node' key");
    }

    @Test
    public void testDynamicRefRecursiveOverride() {
        SwaggerParseResult result = new OpenAPIV3Parser()
                .readLocation("dynamicRef/dynamicref-recursive.yaml", null, resolveOptions());

        assertTrue(result.getMessages() == null || result.getMessages().isEmpty(),
                "Expected no parse errors, got: " + result.getMessages());

        Schema<?> base = result.getOpenAPI().getComponents().getSchemas().get("BaseCategory");
        assertNotNull(base, "BaseCategory schema should exist");
        assertEquals(base.get$dynamicAnchor(), "category",
                "$dynamicAnchor should be preserved on BaseCategory");

        Schema<?> childrenSchema = (Schema<?>) base.getProperties().get("children");
        Schema<?> itemsSchema = childrenSchema.getItems();
        assertEquals(itemsSchema.get$dynamicRef(), "#category",
                "$dynamicRef should be preserved on BaseCategory children items");

        Schema<?> localized = result.getOpenAPI().getComponents().getSchemas().get("LocalizedCategory");
        assertNotNull(localized, "LocalizedCategory schema should exist");
        assertEquals(localized.get$dynamicAnchor(), "category",
                "$dynamicAnchor should be preserved on LocalizedCategory");
    }

    @Test
    public void testMergeSchemasPreservesDynamicFields() {
        SwaggerParseResult result = new OpenAPIV3Parser()
                .readLocation("dynamicRef/dynamicref-recursive.yaml", null, resolveOptions());

        Schema<?> localized = result.getOpenAPI().getComponents().getSchemas().get("LocalizedCategory");
        assertNotNull(localized, "LocalizedCategory schema should exist");

        assertEquals(localized.get$dynamicAnchor(), "category",
                "mergeSchemas should preserve $dynamicAnchor from source");
    }

    @Test
    public void testIdWithComponentRefNestedDocRef() {
        SwaggerParseResult result = new OpenAPIV3Parser()
                .readLocation("dynamicRef/id-with-ref-nested-doc.yaml", null, resolveOptions());

        assertTrue(result.getMessages() == null || result.getMessages().isEmpty(),
                "Expected no parse errors, got: " + result.getMessages());

        Schema<?> concrete = result.getOpenAPI().getComponents().getSchemas().get("Concrete");
        assertNotNull(concrete, "Concrete schema should exist");

        Schema<?> other = result.getOpenAPI().getComponents().getSchemas().get("Other");
        assertNotNull(other, "Other schema should exist");
    }

    @Test
    public void testIdWithComponentRefNestedFileRef() {
        SwaggerParseResult result = new OpenAPIV3Parser()
                .readLocation("dynamicRef/id-with-ref-nested-file.yaml", null, resolveOptions());

        assertTrue(result.getMessages() == null || result.getMessages().isEmpty(),
                "Expected no parse errors, got: " + result.getMessages());

        Schema<?> concrete = result.getOpenAPI().getComponents().getSchemas().get("Concrete");
        assertNotNull(concrete, "Concrete schema should exist");

        Schema<?> template = result.getOpenAPI().getComponents().getSchemas().get("Template");
        assertNotNull(template, "Template schema should exist");
        assertNotNull(template.getProperties(), "Template should have resolved properties from external file");
        assertNotNull(template.getProperties().get("message"), "Template should have 'message' property");
    }
}
