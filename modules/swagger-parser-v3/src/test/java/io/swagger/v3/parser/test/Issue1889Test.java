package io.swagger.v3.parser.test;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.testng.annotations.Test;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

public class Issue1889Test {

    private static final String SCHEMA_PREFIX = "#/components/schemas/";
    private static final String HEADER_PREFIX = "#/components/headers/";

    @Test
    public void resolvesNestedRelativeArrayItemReference() {
        OpenAPI openAPI = parse("issue-1889/root.yaml");

        Schema items = getProductItems(openAPI);
        String resolvedName = assertInternalReference(items);

        assertNotNull(openAPI.getComponents().getSchemas().get(resolvedName));
    }

    @Test
    public void resolvesNestedRelativeArrayItemReferenceWithoutOverwritingExistingSchema() {
        OpenAPI openAPI = parse("issue-1889/root-with-product.yaml");

        Schema rootProduct = openAPI.getComponents().getSchemas().get("Product");
        assertNotNull(rootProduct);
        assertNotNull(rootProduct.getProperties().get("rootOnly"));

        Schema items = getProductItems(openAPI);
        String resolvedName = assertInternalReference(items);
        Schema resolvedProduct = openAPI.getComponents().getSchemas().get(resolvedName);

        assertNotNull(resolvedProduct);
        assertNotNull(resolvedProduct.getProperties().get("id"));
        assertNull(resolvedProduct.getProperties().get("rootOnly"));
    }

    @Test
    public void resolvesNestedRelativeArrayItemReferenceWithFragment() {
        OpenAPI openAPI = parse("issue-1889/root-fragment-ref.yaml");

        Schema response = openAPI.getComponents().getSchemas().get("GetProductResponse");
        assertNotNull(response);
        Schema products = (Schema) response.getProperties().get("products");
        assertTrue(products instanceof ArraySchema);
        Schema items = ((ArraySchema) products).getItems();

        assertNotNull(items);
        assertNotNull(items.get$ref());
        assertTrue(items.get$ref().startsWith(SCHEMA_PREFIX),
                "items.$ref should be internal ref, was: " + items.get$ref());
        assertFalse(items.get$ref().contains("schemas.yaml"),
                "items.$ref should not contain file path, was: " + items.get$ref());

        String resolvedName = items.get$ref().substring(SCHEMA_PREFIX.length());
        assertNotNull(openAPI.getComponents().getSchemas().get(resolvedName));
    }

    @Test
    public void resolvesThreeLevelChainOfNestedRelativeFileReferences() {
        OpenAPI openAPI = parse("issue-1889/root-chain.yaml");

        Schema response = openAPI.getComponents().getSchemas().get("GetProductResponse");
        assertNotNull(response);
        Schema products = (Schema) response.getProperties().get("products");
        assertTrue(products instanceof ArraySchema);
        Schema productItems = ((ArraySchema) products).getItems();

        assertNotNull(productItems);
        assertNotNull(productItems.get$ref());
        assertTrue(productItems.get$ref().startsWith(SCHEMA_PREFIX),
                "productItems.$ref should be internal ref, was: " + productItems.get$ref());

        String productName = productItems.get$ref().substring(SCHEMA_PREFIX.length());
        Schema productSchema = openAPI.getComponents().getSchemas().get(productName);
        assertNotNull(productSchema, "ProductChain schema should be in components");

        Schema nestedItems = (Schema) productSchema.getProperties().get("items");
        assertNotNull(nestedItems);
        assertTrue(nestedItems instanceof ArraySchema);
        Schema itemRef = ((ArraySchema) nestedItems).getItems();

        assertNotNull(itemRef);
        assertNotNull(itemRef.get$ref());
        assertTrue(itemRef.get$ref().startsWith(SCHEMA_PREFIX),
                "itemRef.$ref should be internal ref, was: " + itemRef.get$ref());
        assertFalse(itemRef.get$ref().contains("Item.yaml"),
                "itemRef.$ref should not contain file path, was: " + itemRef.get$ref());

        String itemName = itemRef.get$ref().substring(SCHEMA_PREFIX.length());
        assertNotNull(openAPI.getComponents().getSchemas().get(itemName), "Item schema should be in components");
    }

    @Test
    public void resolvesNestedRelativeHeaderReferenceFromExternalResponse() {
        OpenAPI openAPI = parse("issue-1889/root-header.yaml");
        ApiResponse response = getProductsResponse(openAPI);
        assertNotNull(response.getHeaders(), "response should have headers");

        io.swagger.v3.oas.models.headers.Header rateLimitHeader = response.getHeaders().get("X-Rate-Limit");
        assertNotNull(rateLimitHeader);
        assertNotNull(rateLimitHeader.get$ref());
        assertTrue(rateLimitHeader.get$ref().startsWith(HEADER_PREFIX),
                "X-Rate-Limit $ref should be internal header ref, was: " + rateLimitHeader.get$ref());
        assertFalse(rateLimitHeader.get$ref().contains("RateLimitHeader.yaml"),
                "X-Rate-Limit $ref should not contain file path, was: " + rateLimitHeader.get$ref());

        String headerName = rateLimitHeader.get$ref().substring(HEADER_PREFIX.length());
        assertNotNull(openAPI.getComponents().getHeaders().get(headerName),
                "RateLimitHeader should be in components/headers");
    }

    private OpenAPI parse(String location) {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);

        SwaggerParseResult result = new OpenAPIV3Parser().readLocation(location, null, options);
        assertNotNull(result);
        assertNotNull(result.getOpenAPI());
        return result.getOpenAPI();
    }

    private Schema getProductItems(OpenAPI openAPI) {
        Schema response = openAPI.getComponents().getSchemas().get("GetProductResponse");
        assertNotNull(response);
        Schema products = (Schema) response.getProperties().get("products");
        assertTrue(products instanceof ArraySchema);
        return ((ArraySchema) products).getItems();
    }

    private ApiResponse getProductsResponse(OpenAPI openAPI) {
        ApiResponse pathResponse = openAPI.getPaths().get("/products").getGet().getResponses().get("200");
        assertNotNull(pathResponse);
        assertNotNull(pathResponse.get$ref());
        String responsePrefix = "#/components/responses/";
        assertTrue(pathResponse.get$ref().startsWith(responsePrefix),
                "path response $ref should be internal, was: " + pathResponse.get$ref());

        String responseName = pathResponse.get$ref().substring(responsePrefix.length());
        ApiResponse response = openAPI.getComponents().getResponses().get(responseName);
        assertNotNull(response, "response should be in components/responses");
        return response;
    }

    private String assertInternalReference(Schema items) {
        assertNotNull(items);
        assertNotNull(items.get$ref());
        assertTrue(items.get$ref().startsWith(SCHEMA_PREFIX));
        assertFalse(items.get$ref().contains("Product.yaml"));
        return items.get$ref().substring(SCHEMA_PREFIX.length());
    }
}
