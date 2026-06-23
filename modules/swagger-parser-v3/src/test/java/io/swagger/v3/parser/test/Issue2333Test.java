package io.swagger.v3.parser.test;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.testng.annotations.Test;

import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class Issue2333Test {

    /**
     * Two schemas defined in different files but sharing the same name (both {@code Pet}) must be resolved as
     * distinct components. Previously the resolver reused the first model for both references, so a schema that
     * was still an unresolved external {@code $ref} placeholder was silently overwritten by an unrelated one.
     */
    @Test
    public void referencedModelsWithSameNameFromDifferentFilesAreNotMerged() {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setResolveResponses(true);

        SwaggerParseResult result = new OpenAPIV3Parser()
                .readLocation("./src/test/resources/issue-2333/main.yaml", null, options);

        assertNotNull(result.getOpenAPI());
        Map<String, Schema> schemas = result.getOpenAPI().getComponents().getSchemas();

        // SomeItem points at inventory.yaml's Pet (the "name" property)
        Schema someItem = resolve(schemas, schemas.get("SomeItem"));
        assertNotNull(someItem.getProperties());
        assertTrue(someItem.getProperties().containsKey("name"),
                "SomeItem should resolve to inventory.yaml's Pet (name), but was " + someItem.getProperties().keySet());
        assertEquals(someItem.getProperties().size(), 1);

        // Pet points at pets.yaml's Pet (the "id" property) and must not be overwritten by inventory.yaml's Pet
        Schema pet = resolve(schemas, schemas.get("Pet"));
        assertNotNull(pet.getProperties());
        assertTrue(pet.getProperties().containsKey("id"),
                "Pet should resolve to pets.yaml's Pet (id), but was " + pet.getProperties().keySet());
        assertEquals(pet.getProperties().size(), 1);
    }

    private Schema resolve(Map<String, Schema> schemas, Schema schema) {
        if (schema != null && schema.get$ref() != null) {
            String name = schema.get$ref().substring(schema.get$ref().lastIndexOf('/') + 1);
            return schemas.get(name);
        }
        return schema;
    }
}
