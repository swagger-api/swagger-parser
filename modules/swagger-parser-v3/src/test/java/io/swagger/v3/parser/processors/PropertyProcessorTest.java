package io.swagger.v3.parser.processors;



import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.ResolverCache;
import io.swagger.v3.parser.models.RefFormat;
import mockit.FullVerifications;
import mockit.Injectable;
import mockit.Mocked;
import mockit.StrictExpectations;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class PropertyProcessorTest {

    @Injectable
    ResolverCache cache;

    @Injectable
    OpenAPI swagger;

    @Mocked
    ExternalRefProcessor externalRefProcessor;

    @Test
    public void testProcessRefProperty_ExternalRef() throws Exception {
        expectCreationOfExternalRefProcessor();

        final String ref = "http://my.company.com/path/to/file.json#/foo/bar";
        final Schema refProperty = new Schema().$ref(ref);

        expectCallToExternalRefProcessor(ref, RefFormat.URL, "bar");

        new SchemaProcessor(cache, swagger).processSchema(refProperty);

        new FullVerifications() {{
        }};

        assertEquals(refProperty.get$ref(), "#/components/schemas/bar");
    }

    private void expectCallToExternalRefProcessor(final String ref, final RefFormat refFormat, final String newRef) {
        new StrictExpectations() {{
            externalRefProcessor.processRefToExternalSchema(ref, refFormat);
            times = 1;
            result = newRef;
        }};
    }

    @Test
    public void testProcessRefProperty_InternalRef() throws Exception {
        expectCreationOfExternalRefProcessor();

        final String expectedRef = "#/components/schemas/foo";
        final Schema property = new Schema().$ref(expectedRef);
        new SchemaProcessor(cache, swagger).processSchema(property);

        new FullVerifications() {{
        }};

        assertEquals(property.get$ref(), expectedRef);
    }

    @Test
    public void testProcessArrayProperty_ItemsIsRefProperty() throws Exception {
        expectCreationOfExternalRefProcessor();
        final String ref = "http://my.company.com/path/to/file.json#/foo/bar";
        final Schema refProperty = new Schema().$ref(ref);

        ArraySchema arrayProperty = new ArraySchema();
        arrayProperty.setItems(refProperty);

        expectCallToExternalRefProcessor(ref, RefFormat.URL, "bar");

        new SchemaProcessor(cache, swagger).processSchema(arrayProperty);

        new FullVerifications() {{
        }};

        assertEquals((arrayProperty.getItems()).get$ref(), "#/components/schemas/bar");

    }

    @Test
    public void testProcessMapProperty_AdditionalPropertiesIsRefProperty() throws Exception {
        expectCreationOfExternalRefProcessor();
        final String ref = "http://my.company.com/path/to/file.json#/foo/bar";
        final Schema refProperty = new Schema().$ref(ref);


        refProperty.setAdditionalProperties(refProperty);

        expectCallToExternalRefProcessor(ref, RefFormat.URL, "bar");

        new SchemaProcessor(cache, swagger).processSchema(refProperty);

        new FullVerifications() {{
        }};

        assertEquals((((Schema)refProperty.getAdditionalProperties()).get$ref()), "#/components/schemas/bar");
    }

    private void expectCreationOfExternalRefProcessor() {
        new StrictExpectations() {{
            new ExternalRefProcessor(cache, swagger);
            times = 1;
            result = externalRefProcessor;
        }};
    }
}
