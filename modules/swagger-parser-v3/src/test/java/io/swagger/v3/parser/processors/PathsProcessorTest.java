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
import org.testng.annotations.Test;

import java.net.URL;
import java.util.Map.Entry;
import java.util.Objects;

import static java.lang.String.format;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class PathsProcessorTest {

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

    @Test
    public void testProcessPaths_refsToRenamedSchemasResolved() {
		URL mergeSpecLocation = getClass().getClassLoader().getResource("issue-1955/merged_spec12.yaml");
		Objects.requireNonNull(mergeSpecLocation);
		ParseOptions parseOptions = new ParseOptions();
		parseOptions.setResolve(true);

		SwaggerParseResult swaggerParseResult = new OpenAPIV3Parser().readLocation(mergeSpecLocation.toString(), null, parseOptions);

		swaggerParseResult.getOpenAPI()
				.getPaths().values().stream()
				.flatMap(path -> path.readOperationsMap().values().stream())
				.flatMap(operation -> operation.getParameters().stream())
				.map(Parameter::getSchema)
				.forEach(this::assertSchemaNoExternalRefs);
	}

	private void assertSchemaNoExternalRefs(Schema<?> schema) {
		if (schema.get$ref() != null) {
			assertSchemaRefInternal(schema.get$ref());
		}
		if (schema.getItems() != null && schema.getItems().get$ref() != null) {
			assertSchemaRefInternal(schema.getItems().get$ref());
		}
	}

	private void assertSchemaRefInternal(String ref) {
		assertTrue(ref.startsWith("#"));
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
