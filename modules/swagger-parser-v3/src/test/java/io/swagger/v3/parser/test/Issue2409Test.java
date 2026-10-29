package io.swagger.v3.parser.test;

import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.testng.annotations.Test;

import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

public class Issue2409Test {

    @Test
    public void arrayItemsRefIsResolvedWhenItIsTheOnlyRefInTheFile() {
        assertEquals(itemsRef(parse(), "Broken", "items"), "#/components/schemas/Item");
    }

    @Test
    public void arrayItemsRefIsResolvedWhenTheFileHasAnotherPropertyRef() {
        assertEquals(itemsRef(parse(), "Working", "items"), "#/components/schemas/Item");
    }

    @Test
    public void arrayItemsRefIsResolvedWhenThePropertyIsNotNamedItems() {
        assertEquals(itemsRef(parse(), "Renamed", "entries"), "#/components/schemas/Item");
    }

    @Test
    public void arrayItemsRefIsInlinedWithResolveFully() {
        Schema items = itemsSchema(parse(true), "Broken", "items");
        assertNull(items.get$ref());
        assertNotNull(items.getProperties());
        assertTrue(items.getProperties().containsKey("name"));
    }

    private Map<String, Schema> parse() {
        return parse(false);
    }

    private Map<String, Schema> parse(boolean resolveFully) {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setResolveFully(resolveFully);

        SwaggerParseResult result = new OpenAPIV3Parser()
                .readLocation("./src/test/resources/issue-2409/openapi.yaml", null, options);

        assertNotNull(result.getOpenAPI());
        return result.getOpenAPI().getComponents().getSchemas();
    }

    private String itemsRef(Map<String, Schema> schemas, String schemaName, String property) {
        return itemsSchema(schemas, schemaName, property).get$ref();
    }

    private Schema itemsSchema(Map<String, Schema> schemas, String schemaName, String property) {
        Schema array = (Schema) schemas.get(schemaName).getProperties().get(property);
        assertNotNull(array.getItems(), schemaName + "." + property + " has no items");
        return array.getItems();
    }
}
