package io.swagger.v3.parser.processors;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.PathItem.HttpMethod;
import io.swagger.v3.parser.OpenAPIV3Parser;
import org.testng.annotations.Test;

import java.util.Map.Entry;

import static java.lang.String.format;
import static org.testng.Assert.assertFalse;

public class PathsProcessorTest {

    @Test
    public void testProcessPaths_parameters_internalTopLevelDefinition() {
        OpenAPI openAPI = new OpenAPIV3Parser().read("src/test/resources/issue-XYZ/api.yaml");

        assertOperationsHasParameters(openAPI, "/internal/test/{id}/toplevelparam");
    }

    @Test
    public void testProcessPaths_parameters_internalOperationLevelDefinition() {
        OpenAPI openAPI = new OpenAPIV3Parser().read("src/test/resources/issue-XYZ/api.yaml");

        assertOperationsHasParameters(openAPI, "/internal/test/{id}/operationlevelparam");
    }

    @Test
    public void testProcessPaths_parameters_refTopLevelDefinition() {
        OpenAPI openAPI = new OpenAPIV3Parser().read("src/test/resources/issue-XYZ/api.yaml");

        assertOperationsHasParameters(openAPI, "/ref/test/{id}/toplevelparam");
    }

    @Test
    public void testProcessPaths_parameters_refOperationLevelDefinition() {
        OpenAPI openAPI = new OpenAPIV3Parser().read("src/test/resources/issue-XYZ/api.yaml");

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
}