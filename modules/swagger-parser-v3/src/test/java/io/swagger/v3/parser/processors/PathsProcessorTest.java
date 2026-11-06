package io.swagger.v3.parser.processors;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.PathItem.HttpMethod;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Map.Entry;

import static java.lang.String.format;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

public class PathsProcessorTest {

    @DataProvider
    public Object[][] rebasedReferences() {
        return new Object[][]{
                {"./sub-dir/params.json", "./sub-dir2/p.json", "./sub-dir/sub-dir2/p.json"},
                {"./sub-dir/params.json", "sub-dir2/p.json", "./sub-dir/sub-dir2/p.json"},
                {"./sub-dir/params.json", "../parameters/p.json", "./parameters/p.json"},
                {"./sub-dir/params.json", "p.json#/components/parameters/Foo", "./sub-dir/p.json#/components/parameters/Foo"},
                {"paths/users.yaml", "../parameters/page.yaml", "parameters/page.yaml"},
                {"product/product-api.yaml", "product-components.yaml#/components/parameters/id", "product/product-components.yaml#/components/parameters/id"},
                {"./sub-dir/params.json", "https://example.com/p.json", "https://example.com/p.json"},
                {"./sub-dir/params.json", "http://example.com/p.json#/Foo", "http://example.com/p.json#/Foo"},
                {"./sub-dir/params.json", "file:/tmp/p.json#/Foo", "file:/tmp/p.json#/Foo"}
        };
    }

    @DataProvider
    public Object[][] uriIllegalReferences() {
        return new Object[][]{
                {"\\"},
                {"%"},
                {"["},
                {"]"},
                {"{"},
                {"}"},
                {"^"},
                {"`"},
                {"|"},
                {"<"},
                {">"},
                {"\""},
                {" "}
        };
    }

    @DataProvider
    public Object[][] absoluteReferencesWithUriIllegalCharacters() {
        return new Object[][]{
                {"/shared params/p.yaml"},
                {"C:\\shared params\\p.yaml"},
                {"\\\\server\\shared params\\p.yaml"},
                {"https://example .com/p.yaml"}
        };
    }

    @Test(dataProvider = "rebasedReferences")
    public void testComputeRefRebasesAgainstContainingDocument(String base, String ref, String expected) {
        PathsProcessor processor = new PathsProcessor(null, new OpenAPI());

        assertEquals(processor.computeRef(ref, base), expected);
    }

    @Test
    public void testComputeRefRebasesInvalidChildReference() {
        PathsProcessor processor = new PathsProcessor(null, new OpenAPI());

        assertEquals(processor.computeRef("invalid ref.yaml", "./sub-dir/params.json"), "./sub-dir/invalid ref.yaml");
    }

    @Test(dataProvider = "uriIllegalReferences")
    public void testComputeRefRebasesUriIllegalCharacters(String character) {
        PathsProcessor processor = new PathsProcessor(null, new OpenAPI());
        String ref = "../shared" + character + "params/p.yaml#/components/parameters/Foo";

        assertEquals(processor.computeRef(ref, "./sub-dir/params.json"),
                "./shared" + character + "params/p.yaml#/components/parameters/Foo");
    }

    @Test
    public void testComputeRefFallsBackWhenContainingDocumentIsNotAValidUri() {
        PathsProcessor processor = new PathsProcessor(null, new OpenAPI());

        assertEquals(processor.computeRef("../parameters/p.yaml", "./path items/users.yaml"),
                "./parameters/p.yaml");
    }

    @Test
    public void testComputeRefFallsBackWhenContainingDocumentHasNoDirectory() {
        PathsProcessor processor = new PathsProcessor(null, new OpenAPI());

        assertEquals(processor.computeRef("parameters.yaml", "path item.yaml"), "parameters.yaml");
    }

    @Test(dataProvider = "absoluteReferencesWithUriIllegalCharacters")
    public void testComputeRefPreservesAbsoluteReferencesWithUriIllegalCharacters(String ref) {
        PathsProcessor processor = new PathsProcessor(null, new OpenAPI());

        assertEquals(processor.computeRef(ref, "./sub-dir/params.json"), ref);
    }

    @Test
    public void testExternalRefJoinFallsBackForUriIllegalChildReference() {
        assertEquals(ExternalRefProcessor.join("nested/api.yaml#/paths/~1products",
                        "../shared schemas/product.yaml#/components/schemas/Product"),
                "shared schemas/product.yaml#/components/schemas/Product");
    }

    @Test
    public void testExternalRefJoinPreservesQueryOnlyAndFragmentOnlyReferences() {
        assertEquals(ExternalRefProcessor.join("path items/api.yaml?version=1#/paths/~1products", "?version=2"),
                "path items/api.yaml?version=2");
        assertEquals(ExternalRefProcessor.join("path items/api.yaml?version=1#/paths/~1products",
                        "#/components/schemas/Product"),
                "path items/api.yaml?version=1#/components/schemas/Product");
    }

    @Test
    public void testExternalRefJoinPreservesNullCompatibility() {
        assertEquals(ExternalRefProcessor.join("source.yaml", null), "source.yaml");
        assertNull(ExternalRefProcessor.join(null, "child.yaml"));
    }

    @Test
    public void testFragmentOnlyParameterRefUsesExternalPathItemDocument() {
        PathsProcessor processor = new PathsProcessor(null, new OpenAPI());
        Parameter parameter = new Parameter().$ref("#/components/parameters/Foo");

        processor.updateRefs(parameter, "product/product-api.yaml");

        assertEquals(parameter.get$ref(), "product/product-api.yaml#/components/parameters/Foo");
    }

    @Test
    public void testInternalSchemaRefKeepsRootDocumentCompatibility() {
        PathsProcessor processor = new PathsProcessor(null, new OpenAPI());
        Schema schema = new Schema().$ref("#/components/schemas/Foo");

        processor.updateRefs(schema, "product/product-api.yaml");

        assertEquals(schema.get$ref(), "#/components/schemas/Foo");
    }

    @Test
    public void testIssue1948BareOperationParameterRefIsRelativeToExternalPathItem() {
        SwaggerParseResult result = parse("issue-1948/openapi.yaml");
        Parameter parameter = result.getOpenAPI().getPaths().get("/products/{param1}")
                .getGet().getParameters().get(0);

        assertParameter(parameter, "param1", "path", "string");
        Schema responseSchema = result.getOpenAPI().getPaths().get("/products/{param1}")
                .getGet().getResponses().get("200").getContent().get("application/json").getSchema();
        assertNotNull(result.getOpenAPI().getComponents().getSchemas().get("Product"));
        assertEquals(responseSchema.get$ref(), "#/components/schemas/Product");
        assertNoRelativeRefLoadFailure(result);
    }

    @Test
    public void testIssue2066DotSlashPathParameterRefIsRelativeToExternalPathItem() {
        SwaggerParseResult result = parse("issue-2066/openapi.json");
        Parameter parameter = result.getOpenAPI().getPaths().get("/params")
                .getGet().getParameters().get(0);

        assertNotNull(parameter);
        assertEquals(parameter.getName(), "limit");
        assertEquals(parameter.getIn(), "query");
        assertNotNull(parameter.getSchema());
        assertEquals(parameter.getSchema().get$ref(), "#/components/schemas/an-int");
        Schema resolvedSchema = result.getOpenAPI().getComponents().getSchemas().get("an-int");
        assertNotNull(resolvedSchema);
        assertEquals(resolvedSchema.getType(), "integer");
        assertEquals(resolvedSchema.getFormat(), "int32");
        assertNoRelativeRefLoadFailure(result);
    }

    @Test
    public void testUriIllegalReferencesAreRebasedEndToEnd() {
        SwaggerParseResult result = parse("issue-2393-regression/openapi.yaml");

        assertParameter(parameter(result, "/space"), "space", "query", "string");
        assertParameter(parameter(result, "/braces"), "braces", "query", "string");
        assertParameter(parameter(result, "/percent"), "percent", "query", "string");
        assertParameter(parameter(result, "/prefix-space"), "prefix-space", "query", "string");
        assertParameter(parameter(result, "/root-prefix"), "root-prefix", "query", "string");
        assertNotNull(result.getOpenAPI().getComponents().getSchemas().get("product"));
        assertNotNull(result.getOpenAPI().getComponents().getExamples().get("example"));
        assertNotNull(result.getOpenAPI().getComponents().getHeaders().get("header"));
        assertNotNull(result.getOpenAPI().getComponents().getLinks().get("link"));
        assertNoRelativeRefLoadFailure(result);
    }

    @Test
    public void testResolveFalseDoesNotResolveExternalPathItemWithUriIllegalNestedRef() {
        ParseOptions options = new ParseOptions();
        options.setResolve(false);
        SwaggerParseResult result = new OpenAPIV3Parser()
                .readLocation("issue-2393-regression/openapi.yaml", null, options);

        assertNotNull(result.getOpenAPI());
        PathItem pathItem = result.getOpenAPI().getPaths().get("/space");
        assertNotNull(pathItem.get$ref());
        assertNull(pathItem.getGet());
        assertNoRelativeRefLoadFailure(result);
    }

    @Test
    public void testIssue1948ResolveFullyInlinesExternalPathItemParameter() {
        SwaggerParseResult result = parseFully("issue-1948/openapi.yaml");
        Parameter parameter = result.getOpenAPI().getPaths().get("/products/{param1}")
                .getGet().getParameters().get(0);

        assertParameter(parameter, "param1", "path", "string");
        Schema responseSchema = result.getOpenAPI().getPaths().get("/products/{param1}")
                .getGet().getResponses().get("200").getContent().get("application/json").getSchema();
        assertNotNull(responseSchema);
        assertEquals(responseSchema.getType(), "object");
        assertNoRelativeRefLoadFailure(result);
    }

    @Test
    public void testIssue2066ResolveFullyInlinesNestedExternalPathItemParameter() {
        SwaggerParseResult result = parseFully("issue-2066/openapi.json");
        Parameter parameter = result.getOpenAPI().getPaths().get("/params")
                .getGet().getParameters().get(0);

        assertNotNull(parameter);
        assertEquals(parameter.getName(), "limit");
        assertEquals(parameter.getIn(), "query");
        Schema schema = parameter.getSchema();
        assertNotNull(schema);
        assertEquals(schema.getType(), "integer");
        assertEquals(schema.getFormat(), "int32");
        assertNoRelativeRefLoadFailure(result);
    }

    @DataProvider
    public Object[][] bareRelativeRefsRebasedByComputeRef() {
        return new Object[][]{
                {"schemas/product.yaml", "api/path-item.yaml", "api/schemas/product.yaml"},
                {"examples/product.yaml", "api/path-item.yaml", "api/examples/product.yaml"},
                {"product-components.yaml", "product/product-api.yaml", "product/product-components.yaml"},
        };
    }

    @Test(dataProvider = "bareRelativeRefsRebasedByComputeRef")
    public void testComputeRefRebasesBareRelativeRef(String ref, String base, String expected) {
        PathsProcessor processor = new PathsProcessor(null, new OpenAPI());

        assertEquals(processor.computeRef(ref, base), expected);
    }

    @Test
    public void testUpdateRefsSchemaLeavesExternalPathItemBareRelativeRefUnchanged() {
        PathsProcessor processor = new PathsProcessor(null, new OpenAPI());
        Schema schema = new Schema().$ref("schemas/product.yaml");

        processor.updateRefs(schema, "api/path-item.yaml");

        assertEquals(schema.get$ref(), "schemas/product.yaml");
    }

    @Test
    public void testUpdateRefsExampleLeavesExternalPathItemBareRelativeRefUnchanged() {
        PathsProcessor processor = new PathsProcessor(null, new OpenAPI());
        Example example = new Example().$ref("examples/product.yaml");

        processor.updateRefs(example, "api/path-item.yaml");

        assertEquals(example.get$ref(), "examples/product.yaml");
    }

    @Test
    public void testUpdateRefsSchemaRebasesDotDotRelativeRef() {
        PathsProcessor processor = new PathsProcessor(null, new OpenAPI());
        Schema schema = new Schema().$ref("../shared schemas/product.yaml");

        processor.updateRefs(schema, "nested/space-api.yaml");

        assertEquals(schema.get$ref(), "shared schemas/product.yaml");
    }

    @Test
    public void testUpdateRefsExampleRebasesDotDotRelativeRef() {
        PathsProcessor processor = new PathsProcessor(null, new OpenAPI());
        Example example = new Example().$ref("../shared examples/example.yaml");

        processor.updateRefs(example, "nested/space-api.yaml");

        assertEquals(example.get$ref(), "shared examples/example.yaml");
    }

    @Test
    public void testUpdateRefsComposedSchemaAllOfEntriesRebaseDotDotRef() {
        PathsProcessor processor = new PathsProcessor(null, new OpenAPI());
        ComposedSchema composed = new ComposedSchema();
        Schema allOfEntry = new Schema().$ref("../shared schemas/product.yaml");
        composed.addAllOfItem(allOfEntry);

        processor.updateRefs(composed, "nested/space-api.yaml");

        assertEquals(allOfEntry.get$ref(), "shared schemas/product.yaml");
    }

    @Test
    public void testUpdateRefsComposedSchemaAnyOfEntriesRebaseDotDotRef() {
        PathsProcessor processor = new PathsProcessor(null, new OpenAPI());
        ComposedSchema composed = new ComposedSchema();
        Schema anyOfEntry = new Schema().$ref("../shared schemas/product.yaml");
        composed.addAnyOfItem(anyOfEntry);

        processor.updateRefs(composed, "nested/space-api.yaml");

        assertEquals(anyOfEntry.get$ref(), "shared schemas/product.yaml");
    }

    @Test
    public void testComputeRefRebasesRelativeWindowsPathWithoutNormalizingSeparators() {
        PathsProcessor processor = new PathsProcessor(null, new OpenAPI());

        assertEquals(processor.computeRef("schemas\\product.yaml", "api/path-item.yaml"),
                "api/schemas\\product.yaml");
    }


    @Test
    public void testJoinWithMalformedRemoteBasePreservesDotSegmentsUnresolved() {
        String result = ExternalRefProcessor.join(
                "https://example.com/{version}/api.yaml",
                "../schemas/product.yaml");

        assertEquals(result, "https://example.com/{version}/../schemas/product.yaml");
    }


    @Test
    public void testBareRelativeSchemaRefInExternalPathItemResponseIsResolved() {
        SwaggerParseResult result = parse("issue-1948/openapi.yaml");

        Schema responseSchema = result.getOpenAPI().getPaths().get("/products/{param1}")
                .getGet().getResponses().get("200").getContent().get("application/json").getSchema();
        assertNotNull(responseSchema);
        assertEquals(responseSchema.get$ref(), "#/components/schemas/Product");
        Schema productSchema = result.getOpenAPI().getComponents().getSchemas().get("Product");
        assertNotNull(productSchema);
        assertEquals(productSchema.getType(), "object");
        assertNotNull(productSchema.getProperties().get("productCode"));
        assertNoRelativeRefLoadFailure(result);
    }

    @Test
    public void testDotDotRelativeSchemaAndExampleRefsInExternalPathItemAreResolved() {
        SwaggerParseResult result = parse("issue-2393-regression/openapi.yaml");

        Schema productSchema = result.getOpenAPI().getComponents().getSchemas().get("product");
        assertNotNull(productSchema);
        assertEquals(productSchema.getType(), "object");
        assertNotNull(productSchema.getProperties().get("id"));
        Example example = result.getOpenAPI().getComponents().getExamples().get("example");
        assertNotNull(example);
        assertEquals(example.getSummary(), "Product example");
        assertNoRelativeRefLoadFailure(result);
    }

    @Test
    public void testProcessPaths_parameters_internalTopLevelDefinition() {
        OpenAPI openAPI = new OpenAPIV3Parser().read("src/test/resources/issue-1733/api.yaml");

        assertOperationsHasParameters(openAPI, "/internal/test/{id}/toplevelparam");
    }

    @Test
    public void testProcessPaths_parameters_internalOperationLevelDefinition() {
        OpenAPI openAPI = new OpenAPIV3Parser().read("src/test/resources/issue-1733/api.yaml");

        assertOperationsHasParameters(openAPI, "/internal/test/{id}/operationlevelparam");
    }

    @Test
    public void testProcessPaths_parameters_refTopLevelDefinition() {
        OpenAPI openAPI = new OpenAPIV3Parser().read("src/test/resources/issue-1733/api.yaml");

        assertOperationsHasParameters(openAPI, "/ref/test/{id}/toplevelparam");
    }

    @Test
    public void testProcessPaths_parameters_refOperationLevelDefinition() {
        OpenAPI openAPI = new OpenAPIV3Parser().read("src/test/resources/issue-1733/api.yaml");

        assertOperationsHasParameters(openAPI, "/ref/test/{id}/operationlevelparam");
    }

    private void assertOperationsHasParameters(OpenAPI openAPI, String path) {
        PathItem pathItem = openAPI.getPaths().get(path);

        assertFalse(pathItem.readOperations().isEmpty(), format("Expected operations for %s but found none", path));

        for (Entry<HttpMethod, Operation> operationEntry : pathItem.readOperationsMap().entrySet()) {
            HttpMethod httpMethod = operationEntry.getKey();
            Operation operation = operationEntry.getValue();

            assertFalse(operation.getParameters() == null || operation.getParameters().isEmpty(), format("Expected parameters on %s operation for %s but found none", httpMethod, path));
        }
    }

    private SwaggerParseResult parse(String location) {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        return new OpenAPIV3Parser().readLocation(location, null, options);
    }

    private SwaggerParseResult parseFully(String location) {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setResolveFully(true);
        return new OpenAPIV3Parser().readLocation(location, null, options);
    }

    private Parameter parameter(SwaggerParseResult result, String path) {
        return result.getOpenAPI().getPaths().get(path).getGet().getParameters().get(0);
    }

    private void assertParameter(Parameter parameter, String name, String in, String schemaType) {
        assertNotNull(parameter);
        assertEquals(parameter.getName(), name);
        assertEquals(parameter.getIn(), in);
        Schema schema = parameter.getSchema();
        assertNotNull(schema);
        assertEquals(schema.getType(), schemaType);
    }

    private void assertNoRelativeRefLoadFailure(SwaggerParseResult result) {
        assertNotNull(result.getOpenAPI());
        for (String message : result.getMessages()) {
            assertFalse(message.contains("Unable to load RELATIVE ref"), message);
        }
    }
}
