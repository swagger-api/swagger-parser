package io.swagger.v3.parser.test;

import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class Issue2333Test {

    /**
     * Two schemas defined in different files but sharing the same name (both {@code Pet}) must be resolved as
     * distinct components. Previously the resolver reused the first model for both references, so a schema that
     * was still an unresolved external {@code $ref} placeholder was silently overwritten by an unrelated one.
     */
    @Test
    public void referencedModelsWithSameNameFromDifferentFilesAreNotMerged() {
        Map<String, Schema> schemas = parse("main.yaml");

        assertEquals(schemas.size(), 3);
        assertTrue(schemas.keySet().containsAll(Arrays.asList("SomeItem", "Pet", "Pet_1")));
        assertSchemaHasOnlyProperty(resolve(schemas, schemas.get("Pet")), "id");
        assertSchemaHasOnlyProperty(schemas.get("Pet_1"), "name");
        assertSame(schemas.get("SomeItem"), schemas.get("Pet_1"));
        assertSchemaHasOnlyProperty(resolve(schemas, schemas.get("SomeItem")), "name");
    }

    @Test
    public void equivalentRefsWithDifferentSpellingReuseThePlaceholderName() {
        Map<String, Schema> schemas = parse("equivalent-refs.yaml");

        assertEquals(schemas.size(), 2);
        assertTrue(schemas.containsKey("Thing"));
        assertFalse(schemas.containsKey("Thing_1"));
        assertSame(schemas.get("ThingAlias"), schemas.get("Thing"));
        assertSchemaHasOnlyProperty(resolve(schemas, schemas.get("ThingAlias")), "value");
    }

    @Test
    public void reversedTraversalPreservesDistinctTargetsAndCollapsesEquivalentRefs() {
        Map<String, Schema> schemas = parse("reversed.yaml");

        assertEquals(schemas.size(), 4);
        assertTrue(schemas.keySet().containsAll(Arrays.asList("Pet", "SomeItem", "PetAlias", "Pet_1")));
        assertFalse(schemas.containsKey("Pet_2"));
        assertSchemaHasOnlyProperty(resolve(schemas, schemas.get("Pet")), "id");
        assertSchemaHasOnlyProperty(schemas.get("Pet_1"), "name");
        assertSame(schemas.get("SomeItem"), schemas.get("Pet_1"));
        assertSame(schemas.get("PetAlias"), schemas.get("Pet"));
        assertSchemaHasOnlyProperty(resolve(schemas, schemas.get("SomeItem")), "name");
        assertSchemaHasOnlyProperty(resolve(schemas, schemas.get("PetAlias")), "id");
    }

    @Test
    public void caseInsensitiveNameCollisionPreservesDistinctTargets() {
        Map<String, Schema> schemas = parse("case-insensitive.yaml");

        assertEquals(schemas.size(), 3);
        assertTrue(schemas.keySet().containsAll(Arrays.asList("SomeItem", "pet", "Pet_1")));
        assertFalse(schemas.containsKey("Pet"));
        assertSchemaHasOnlyProperty(resolve(schemas, schemas.get("pet")), "id");
        assertSchemaHasOnlyProperty(schemas.get("Pet_1"), "name");
        assertSame(schemas.get("SomeItem"), schemas.get("Pet_1"));
        assertSchemaHasOnlyProperty(resolve(schemas, schemas.get("SomeItem")), "name");
    }

    private Map<String, Schema> parse(String fixture) {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setResolveResponses(true);

        SwaggerParseResult result = new OpenAPIV3Parser()
                .readLocation("./src/test/resources/issue-2333/" + fixture, null, options);

        assertNotNull(result.getOpenAPI());
        assertTrue(result.getMessages().isEmpty(), "Unexpected parser messages: " + result.getMessages());
        assertNotNull(result.getOpenAPI().getComponents());
        assertNotNull(result.getOpenAPI().getComponents().getSchemas());
        return result.getOpenAPI().getComponents().getSchemas();
    }

    private void assertSchemaHasOnlyProperty(Schema schema, String property) {
        assertNotNull(schema);
        assertNotNull(schema.getProperties());
        assertEquals(schema.getProperties().size(), 1);
        assertTrue(schema.getProperties().containsKey(property),
                "Expected property " + property + ", but was " + schema.getProperties().keySet());
    }

    private Schema resolve(Map<String, Schema> schemas, Schema schema) {
        if (schema != null && schema.get$ref() != null) {
            String name = schema.get$ref().substring(schema.get$ref().lastIndexOf('/') + 1);
            return schemas.get(name);
        }
        return schema;
    }
}
