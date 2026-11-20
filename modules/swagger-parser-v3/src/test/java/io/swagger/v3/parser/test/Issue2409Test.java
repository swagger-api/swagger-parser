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
    public void arrayItemsRefToAnotherSchemaOfTheSameExternalFileIsResolved() {
        Map<String, Schema> schemas = parse("./src/test/resources/issue-2409/same-file/openapi.yaml", false);
        assertEquals(itemsRef(schemas, "BarEmailModel", "parentList"), "#/components/schemas/BarParentListItem");
        assertTrue(schemas.containsKey("BarParentListItem"));
    }

    @Test
    public void itemsRefOfATopLevelArraySchemaIsResolved() {
        assertEquals(parse().get("ItemList").getItems().get$ref(), "#/components/schemas/Item");
    }

    @Test
    public void arrayItemsRefInsideAllOfIsResolved() {
        Schema part = (Schema) parse().get("Composed").getAllOf().get(0);
        Schema array = (Schema) part.getProperties().get("items");
        assertEquals(array.getItems().get$ref(), "#/components/schemas/Item");
    }

    @Test
    public void notRefInAnExternalSchemaDoesNotBreakTheResolve() {
        SwaggerParseResult result = read("./src/test/resources/issue-2409/openapi.yaml", false);
        assertTrue(result.getMessages().isEmpty(), result.getMessages().toString());
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
        return parse("./src/test/resources/issue-2409/openapi.yaml", resolveFully);
    }

    private Map<String, Schema> parse(String location, boolean resolveFully) {
        SwaggerParseResult result = read(location, resolveFully);
        assertNotNull(result.getOpenAPI());
        return result.getOpenAPI().getComponents().getSchemas();
    }

    private SwaggerParseResult read(String location, boolean resolveFully) {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setResolveFully(resolveFully);
        return new OpenAPIV3Parser().readLocation(location, null, options);
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
