package io.swagger.v3.parser.test;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Map;

import static org.testng.Assert.*;

/**
 * Tests for promoting local $refs within external files to components.schemas in OpenAPI 3.1.
 * This mirrors the behavior of ExternalRefProcessor.processRefSchema() from the 3.0 code path.
 */
public class OpenAPIV31ParserNestedExternalRefTest {

    private static final String RESOURCE_DIR = "src/test/resources/3.1.0/dereference/external-schema-ref-nested/";

    private OpenAPI parse(String file, boolean resolveFully) {
        ParseOptions parseOptions = new ParseOptions();
        parseOptions.setResolve(true);
        if (resolveFully) {
            parseOptions.setResolveFully(true);
        }
        SwaggerParseResult result = new OpenAPIV3Parser().readLocation(
            new File(RESOURCE_DIR + file).getAbsolutePath(), null, parseOptions
        );
        assertNotNull(result.getOpenAPI(), "OpenAPI should not be null");
        return result.getOpenAPI();
    }

    /**
     * Basic test: external file has ListResponse with internal $ref to Item.
     * Both should be promoted to components.schemas with resolve=true.
     */
    @Test
    public void testNestedLocalRefPromotedToComponents() {
        OpenAPI openAPI = parse("swagger.yaml", false);

        Map<String, Schema> schemas = openAPI.getComponents().getSchemas();
        assertNotNull(schemas, "Schemas should not be null");
        assertTrue(schemas.containsKey("ListResponse"), "Should contain ListResponse");
        assertTrue(schemas.containsKey("Item"), "Should contain Item (promoted from internal ref in external file)");

        // Verify Item has correct properties and types
        Schema itemSchema = schemas.get("Item");
        assertNotNull(itemSchema.getProperties(), "Item properties should not be null");
        assertTrue(itemSchema.getProperties().containsKey("id"), "Item should have 'id' property");
        assertTrue(itemSchema.getProperties().containsKey("name"), "Item should have 'name' property");

        // Verify ListResponse.data.items is a $ref to Item, not inlined
        Schema listResponse = schemas.get("ListResponse");
        Schema dataProp = (Schema) listResponse.getProperties().get("data");
        assertNotNull(dataProp.getItems(), "data.items should not be null");
        assertEquals(dataProp.getItems().get$ref(), "#/components/schemas/Item",
            "data.items should be a $ref to #/components/schemas/Item");
    }

    /**
     * With resolveFully=true, schemas should still be in components but also inlined at usage sites.
     */
    @Test
    public void testNestedLocalRefResolveFullyStillPopulatesComponents() {
        OpenAPI openAPI = parse("swagger.yaml", true);

        Map<String, Schema> schemas = openAPI.getComponents().getSchemas();
        assertNotNull(schemas, "Schemas should not be null");
        assertTrue(schemas.containsKey("ListResponse"), "Should contain ListResponse");
        assertTrue(schemas.containsKey("Item"), "Should contain Item");

        // In resolveFully mode, the response schema should be inlined (no $ref)
        Schema responseSchema = openAPI.getPaths().get("/items").getGet()
            .getResponses().get("200").getContent().get("application/json").getSchema();
        assertNull(responseSchema.get$ref(), "Response schema should be inlined (no $ref) in resolveFully mode");
        assertNotNull(responseSchema.getProperties(), "Response schema should have inlined properties");
    }

    /**
     * Type information must be preserved for all schemas: object, array, string, integer.
     */
    @Test
    public void testTypeInformationPreserved() {
        OpenAPI openAPI = parse("swagger.yaml", false);

        Map<String, Schema> schemas = openAPI.getComponents().getSchemas();

        // ListResponse should be type: object (3.1 uses getTypes() returning a Set)
        Schema listResponse = schemas.get("ListResponse");
        assertTrue(listResponse.getTypes().contains("object"), "ListResponse should be type object");

        // data property should be type: array
        Schema dataProp = (Schema) listResponse.getProperties().get("data");
        assertTrue(dataProp.getTypes().contains("array"), "data should be type array");

        // total property should be type: integer
        Schema totalProp = (Schema) listResponse.getProperties().get("total");
        assertTrue(totalProp.getTypes().contains("integer"), "total should be type integer");

        // Item should be type: object with string properties
        Schema item = schemas.get("Item");
        assertTrue(item.getTypes().contains("object"), "Item should be type object");
        Schema idProp = (Schema) item.getProperties().get("id");
        assertTrue(idProp.getTypes().contains("string"), "id should be type string");
    }

    /**
     * Deep chain: Order -> Customer -> Address, Order -> OrderItem.
     * All schemas in the chain should be promoted to components.
     */
    @Test
    public void testDeepNestedRefChain() {
        OpenAPI openAPI = parse("swagger-deep.yaml", false);

        Map<String, Schema> schemas = openAPI.getComponents().getSchemas();
        assertNotNull(schemas, "Schemas should not be null");
        assertTrue(schemas.containsKey("Order"), "Should contain Order");
        assertTrue(schemas.containsKey("Customer"), "Should contain Customer");
        assertTrue(schemas.containsKey("Address"), "Should contain Address (2 levels deep)");
        assertTrue(schemas.containsKey("OrderItem"), "Should contain OrderItem");

        // Verify Order.customer is a $ref to Customer
        Schema order = schemas.get("Order");
        Schema customerProp = (Schema) order.getProperties().get("customer");
        assertEquals(customerProp.get$ref(), "#/components/schemas/Customer",
            "Order.customer should ref Customer");

        // Verify Customer.address is a $ref to Address
        Schema customer = schemas.get("Customer");
        Schema addressProp = (Schema) customer.getProperties().get("address");
        assertEquals(addressProp.get$ref(), "#/components/schemas/Address",
            "Customer.address should ref Address");

        // Verify Order.items is array with $ref to OrderItem
        Schema itemsProp = (Schema) order.getProperties().get("items");
        assertTrue(itemsProp.getTypes().contains("array"), "items should be type array");
        assertEquals(itemsProp.getItems().get$ref(), "#/components/schemas/OrderItem",
            "Order.items should ref OrderItem");
    }

    /**
     * Deep chain with resolveFully: all schemas in components, everything inlined at usage sites.
     */
    @Test
    public void testDeepNestedRefChainResolveFully() {
        OpenAPI openAPI = parse("swagger-deep.yaml", true);

        Map<String, Schema> schemas = openAPI.getComponents().getSchemas();
        assertNotNull(schemas, "Schemas should not be null");
        assertTrue(schemas.containsKey("Order"), "Should contain Order");
        assertTrue(schemas.containsKey("Customer"), "Should contain Customer");
        assertTrue(schemas.containsKey("Address"), "Should contain Address");
        assertTrue(schemas.containsKey("OrderItem"), "Should contain OrderItem");

        // Response schema should be fully inlined
        Schema responseSchema = openAPI.getPaths().get("/orders").getGet()
            .getResponses().get("200").getContent().get("application/json").getSchema();
        assertNull(responseSchema.get$ref(), "Response should be inlined in resolveFully mode");
        assertNotNull(responseSchema.getProperties().get("customer"),
            "Inlined Order should have customer property");
    }

    /**
     * When the same external file is referenced multiple times, schemas should not be duplicated.
     * Item is referenced both indirectly (via ListResponse.data.items) and directly.
     */
    @Test
    public void testDuplicateRefsNotDuplicated() {
        OpenAPI openAPI = parse("swagger-duplicate-refs.yaml", false);

        Map<String, Schema> schemas = openAPI.getComponents().getSchemas();
        assertNotNull(schemas, "Schemas should not be null");
        assertTrue(schemas.containsKey("ListResponse"), "Should contain ListResponse");
        assertTrue(schemas.containsKey("Item"), "Should contain Item");

        // Item should appear exactly once (no Item_1 duplicate)
        assertFalse(schemas.containsKey("Item_1"), "Item should not be duplicated as Item_1");

        // Both refs should point to the same component
        Schema listResponse = schemas.get("ListResponse");
        Schema dataProp = (Schema) listResponse.getProperties().get("data");
        assertEquals(dataProp.getItems().get$ref(), "#/components/schemas/Item",
            "ListResponse.data.items should ref Item");

        // Direct ref to Item should also point to #/components/schemas/Item
        Schema directItemRef = openAPI.getPaths().get("/items/{id}").getGet()
            .getResponses().get("200").getContent().get("application/json").getSchema();
        assertEquals(directItemRef.get$ref(), "#/components/schemas/Item",
            "Direct ref should also point to #/components/schemas/Item");
    }
}
