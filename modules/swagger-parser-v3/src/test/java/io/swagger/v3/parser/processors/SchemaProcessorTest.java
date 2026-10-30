package io.swagger.v3.parser.processors;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.ResolverCache;
import io.swagger.v3.parser.models.RefFormat;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.testng.Assert.assertEquals;


public class SchemaProcessorTest {

    ResolverCache cache = mock(ResolverCache.class);

    OpenAPI openAPI = mock(OpenAPI.class);

    // Every ExternalRefProcessor created by a SchemaProcessor is a mock.
    // It answers processRefToExternalSchema(ref, format) from this map (null when not listed).
    private final Map<String, String> externalSchemaNames = new HashMap<>();
    private MockedConstruction<ExternalRefProcessor> externalRefProcessors;

    @BeforeMethod
    public void mockExternalRefProcessor() {
        externalSchemaNames.clear();
        externalRefProcessors = Mockito.mockConstruction(ExternalRefProcessor.class, (mock, context) ->
                Mockito.when(mock.processRefToExternalSchema(Mockito.anyString(), Mockito.any(RefFormat.class)))
                        .thenAnswer(invocation -> externalSchemaNames.get(invocation.<String>getArgument(0))));
    }

    @AfterMethod(alwaysRun = true)
    public void closeExternalRefProcessor() {
        externalRefProcessors.close();
    }

    /** Verifies the number of processRefToExternalSchema(ref, URL) calls over all mocked ExternalRefProcessor instances. */
    private void verifyExternalSchemaCalls(String ref, RefFormat format, int expected) {
        int actual = 0;
        for (ExternalRefProcessor processor : externalRefProcessors.constructed()) {
            actual += Mockito.mockingDetails(processor).getInvocations().stream()
                    .filter(i -> i.getMethod().getName().equals("processRefToExternalSchema")
                            && ref.equals(i.getArguments()[0]) && format == i.getArguments()[1])
                    .count();
        }
        assertEquals(actual, expected);
    }

    /** Equivalent of a full verification: nothing else was called on any mock. */
    private void verifyNothingElseHappened() {
        verifyNoInteractions(cache, openAPI);
        for (ExternalRefProcessor processor : externalRefProcessors.constructed()) {
            Mockito.mockingDetails(processor).getInvocations().forEach(i ->
                    assertEquals(i.getMethod().getName(), "processRefToExternalSchema"));
        }
    }

    @Test
    public void testProcessRefSchema_ExternalRef() throws Exception {

        final String ref = "http://my.company.com/path/to/file.json#/foo/bar";
        final String newRef = "bar";

        externalSchemaNames.put(ref, newRef);

        Schema refSchema = new Schema().$ref(ref);

        new SchemaProcessor(cache, openAPI).processSchema(refSchema);

        verifyExternalSchemaCalls(ref, RefFormat.URL, 1);
        assertEquals(refSchema.get$ref(), "#/components/schemas/bar");
    }

    @Test
    public void testProcessRefSchema_InternalRef() throws Exception {
        final String ref = "#/components/schemas/bar";

        Schema refModel = new Schema().$ref(ref);

        new SchemaProcessor(cache, openAPI).processSchema(refModel);

        assertEquals(refModel.get$ref(), ref);
    }



    @Test
    public void testProcessArraySchema() throws Exception {

        Schema property = new Schema();
        SchemaProcessor propertyProcessor = new SchemaProcessor(cache, openAPI);
        propertyProcessor.processSchema(property);


        ArraySchema model = new ArraySchema();
        model.setItems(property);

        SchemaProcessor schemaProcessor = new SchemaProcessor(cache, openAPI);
        schemaProcessor.processSchema(model);

        assertEquals(property,model.getItems());

    }

    @Test
    public void testProcessComposedSchema() throws Exception {

        final String ref1 = "http://my.company.com/path/to/file.json#/foo/bar";
        final String ref2 = "http://my.company.com/path/to/file.json#/this/that";
        final String ref3 = "http://my.company.com/path/to/file.json#/hello/world";

        SchemaProcessor modelProcessor = new SchemaProcessor(cache, openAPI);

        externalSchemaNames.put(ref1, "bar");
        externalSchemaNames.put(ref2, "that");
        externalSchemaNames.put(ref3, "world");

        ComposedSchema composedModel = new ComposedSchema();


        composedModel.addAllOfItem(new Schema().$ref(ref1));
        composedModel.addAllOfItem(new Schema().$ref(ref2));
        composedModel.addAllOfItem(new Schema().$ref(ref3));

        modelProcessor.processSchema(composedModel);

        verifyExternalSchemaCalls(ref1, RefFormat.URL, 1);
        verifyExternalSchemaCalls(ref2, RefFormat.URL, 1);
        verifyExternalSchemaCalls(ref3, RefFormat.URL, 1);
        verifyNothingElseHappened();

        assertEquals(composedModel.getAllOf().get(0).get$ref(),"#/components/schemas/bar");//child
        assertEquals(composedModel.getAllOf().get(1).get$ref(), "#/components/schemas/that"); //parent
        assertEquals(composedModel.getAllOf().get(2).get$ref(), "#/components/schemas/world");
    }

    @Test
    public void testProcessSchema() throws Exception {


        Schema property1 = new Schema();
        Schema property2 = new Schema();

        SchemaProcessor propertyProcessor = new SchemaProcessor(cache,openAPI);

        Schema model = new Schema();
        model.addProperties("foo", property1);
        model.addProperties("bar", property2);

        propertyProcessor.processSchema(property1);
        propertyProcessor.processSchema(property2);

        new SchemaProcessor(cache, openAPI).processSchema(model);

        assertEquals(model.getProperties().get("foo"), property1);
        assertEquals(model.getProperties().get("bar"), property2);
    }

    @Test
    public void testProcessRefProperty_ExternalRef() throws Exception {

        final String ref = "http://my.company.com/path/to/file.json#/foo/bar";
        final Schema refProperty = new Schema().$ref(ref);

        externalSchemaNames.put(ref, "bar");

        new SchemaProcessor(cache, openAPI).processSchema(refProperty);

        assertEquals(externalRefProcessors.constructed().size(), 1);
        verifyExternalSchemaCalls(ref, RefFormat.URL, 1);
        verifyNothingElseHappened();

        assertEquals(refProperty.get$ref(), "#/components/schemas/bar");
    }

    @Test
    public void testProcessRefProperty_InternalRef() throws Exception {
        final String expectedRef = "#/components/schemas/foo";
        final Schema property = new Schema().$ref(expectedRef);
        new SchemaProcessor(cache, openAPI).processSchema(property);

        assertEquals(externalRefProcessors.constructed().size(), 1);
        verifyNoInteractions(externalRefProcessors.constructed().get(0));
        verifyNothingElseHappened();

        assertEquals(property.get$ref(), expectedRef);
    }

    @Test
    public void testProcessArrayProperty_ItemsIsRefProperty() throws Exception {
        final String ref = "http://my.company.com/path/to/file.json#/foo/bar";
        final Schema refProperty = new Schema().$ref(ref);

        ArraySchema arrayProperty = new ArraySchema();
        arrayProperty.setItems(refProperty);
        externalSchemaNames.put(ref, "bar");

        new SchemaProcessor(cache, openAPI).processSchema(arrayProperty);

        assertEquals(externalRefProcessors.constructed().size(), 1);
        verifyExternalSchemaCalls(ref, RefFormat.URL, 1);
        verifyNothingElseHappened();

        assertEquals((arrayProperty.getItems()).get$ref(), "#/components/schemas/bar");

    }

    @Test
    public void testProcessMapProperty_AdditionalPropertiesIsRefProperty() throws Exception {
        final String ref = "http://my.company.com/path/to/file.json#/foo/bar";
        final Schema refProperty = new Schema().$ref(ref);

        refProperty.setAdditionalProperties(refProperty);
        externalSchemaNames.put(ref, "bar");

        new SchemaProcessor(cache, openAPI).processSchema(refProperty);

        assertEquals(externalRefProcessors.constructed().size(), 1);
        verifyExternalSchemaCalls(ref, RefFormat.URL, 1);
        verifyNothingElseHappened();

        assertEquals((((Schema)refProperty.getAdditionalProperties()).get$ref()), "#/components/schemas/bar");
    }
}
