package io.swagger.v3.parser.processors;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.PathItem.HttpMethod;
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

    @Test(dataProvider = "rebasedReferences")
    public void testComputeRefRebasesAgainstContainingDocument(String base, String ref, String expected) {
        PathsProcessor processor = new PathsProcessor(null, new OpenAPI());

        assertEquals(processor.computeRef(ref, base), expected);
    }

    @Test
    public void testComputeRefKeepsInvalidChildReference() {
        PathsProcessor processor = new PathsProcessor(null, new OpenAPI());

        assertEquals(processor.computeRef("invalid ref.yaml", "./sub-dir/params.json"), "invalid ref.yaml");
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
