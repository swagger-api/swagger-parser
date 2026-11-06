package io.swagger.v3.parser.test;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class Issue1961Test {

    private static final String ROOT_DOCUMENT =
            "src/test/resources/issue-1961/TestCase.yaml";

    @Test(description = "Issue #1961: a reference cycle back to the root file must not duplicate schemas")
    public void externalSchemaReferencingRootSchemaDoesNotCreateDuplicate() {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);

        SwaggerParseResult result = new OpenAPIV3Parser().readLocation(
                ROOT_DOCUMENT, null, options);

        assertNotNull(result.getOpenAPI());
        assertTrue(result.getMessages().isEmpty(), "Unexpected parser messages: " + result.getMessages());

        OpenAPI openAPI = result.getOpenAPI();
        assertNotNull(openAPI.getComponents());
        Map<String, Schema> schemas = openAPI.getComponents().getSchemas();
        assertNotNull(schemas);

        assertTrue(schemas.containsKey("TestCase_TestCase"));
        assertTrue(schemas.containsKey("TestCase_v1_TestCase"));
        assertTrue(schemas.containsKey("TestCase_Foo"));
        assertTrue(schemas.containsKey("TestCase_FooType"));
        assertFalse(schemas.containsKey("TestCase_Foo_1"),
                "The back-reference to the root file must reuse TestCase_Foo");
        assertEquals(schemas.size(), 4);

        ComposedSchema root = (ComposedSchema) schemas.get("TestCase_TestCase");
        assertEquals(root.getAnyOf().get(0).get$ref(),
                "#/components/schemas/TestCase_v1_TestCase");

        Schema versioned = schemas.get("TestCase_v1_TestCase");
        assertEquals(((Schema) versioned.getProperties().get("Foo")).get$ref(),
                "#/components/schemas/TestCase_Foo");

        Schema foo = schemas.get("TestCase_Foo");
        ArraySchema fooTypes = (ArraySchema) foo.getProperties().get("FooTypes");
        assertEquals(fooTypes.getItems().get$ref(),
                "#/components/schemas/TestCase_FooType");
    }

    @Test(description = "Issue #1961 control: duplicate promotion only occurs during resolution")
    public void unresolvedDocumentKeepsOriginalSchemasAndReferences() {
        SwaggerParseResult result = new OpenAPIV3Parser().readLocation(
                ROOT_DOCUMENT, null, new ParseOptions());

        assertNotNull(result.getOpenAPI());
        assertTrue(result.getMessages().isEmpty(), "Unexpected parser messages: " + result.getMessages());

        Map<String, Schema> schemas = result.getOpenAPI().getComponents().getSchemas();
        assertEquals(schemas.size(), 3);
        assertFalse(schemas.containsKey("TestCase_v1_TestCase"));
        assertFalse(schemas.containsKey("TestCase_Foo_1"));

        ComposedSchema root = (ComposedSchema) schemas.get("TestCase_TestCase");
        assertEquals(root.getAnyOf().get(0).get$ref(),
                "./TestCase.v1.yaml#/components/schemas/TestCase_v1_TestCase");

        Schema foo = schemas.get("TestCase_Foo");
        ArraySchema fooTypes = (ArraySchema) foo.getProperties().get("FooTypes");
        assertEquals(fooTypes.getItems().get$ref(),
                "./TestCase.yaml#/components/schemas/TestCase_FooType");
    }

    @Test(description = "Issue #1961: file: URI as root document resolves parent directory and deduplicates schemas")
    public void fileUriRootResolvesExternalRefsAndDeduplicatesSchemas() {
        Path rootFile = Paths.get(ROOT_DOCUMENT).toAbsolutePath().normalize();
        String fileUri = rootFile.toUri().toString();

        ParseOptions options = new ParseOptions();
        options.setResolve(true);

        SwaggerParseResult result = new OpenAPIV3Parser().readLocation(fileUri, null, options);

        assertNotNull(result.getOpenAPI());
        assertTrue(result.getMessages().isEmpty(), "Unexpected parser messages: " + result.getMessages());

        Map<String, Schema> schemas = result.getOpenAPI().getComponents().getSchemas();
        assertNotNull(schemas);
        assertFalse(schemas.containsKey("TestCase_Foo_1"),
                "file: URI root must not duplicate schemas from back-references");
        assertEquals(schemas.size(), 4);
    }
}
