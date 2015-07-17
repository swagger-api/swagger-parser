package io.swagger.parser.processors;


import io.swagger.models.Swagger;
import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.MapProperty;
import io.swagger.models.properties.RefProperty;
import io.swagger.models.refs.RefFormat;
import io.swagger.parser.ResolverCache;
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
    Swagger swagger;

    @Mocked
    ExternalRefProcessor externalRefProcessor;

    @Test
    public void testProcessRefProperty_ExternalRef() throws Exception {
        expectCreationOfExternalRefProcessor();

        final String ref = "http://my.company.com/path/to/file.json#/foo/bar";
        final RefProperty refProperty = new RefProperty(ref);

        expectCallToExternalRefProcessor(ref, RefFormat.URL, "bar");

        new PropertyProcessor(cache, swagger).processProperty(refProperty);

        new FullVerifications() {{
        }};

        assertEquals(refProperty.get$ref(), "#/definitions/bar");
    }

    private void expectCallToExternalRefProcessor(final String ref, final RefFormat refFormat, final String newRef) {
        new StrictExpectations() {{
            externalRefProcessor.processRefToExternalDefinition(ref, refFormat);
            times = 1;
            result = newRef;
        }};
    }

    @Test
    public void testProcessRefProperty_InternalRef() throws Exception {
        expectCreationOfExternalRefProcessor();

        final String expectedRef = "#/definitions/foo";
        final RefProperty property = new RefProperty(expectedRef);
        new PropertyProcessor(cache, swagger).processProperty(property);

        new FullVerifications() {{
        }};

        assertEquals(property.get$ref(), expectedRef);
    }

    @Test
    public void testProcessArrayProperty_ItemsIsRefProperty() throws Exception {
        expectCreationOfExternalRefProcessor();
        final String ref = "http://my.company.com/path/to/file.json#/foo/bar";
        final RefProperty refProperty = new RefProperty(ref);

        ArrayProperty arrayProperty = new ArrayProperty();
        arrayProperty.setItems(refProperty);

        expectCallToExternalRefProcessor(ref, RefFormat.URL, "bar");

        new PropertyProcessor(cache, swagger).processProperty(arrayProperty);

        new FullVerifications() {{
        }};

        assertEquals(((RefProperty)arrayProperty.getItems()).get$ref(), "#/definitions/bar");

    }

    @Test
    public void testProcessMapProperty_AdditionalPropertiesIsRefProperty() throws Exception {
        expectCreationOfExternalRefProcessor();
        final String ref = "http://my.company.com/path/to/file.json#/foo/bar";
        final RefProperty refProperty = new RefProperty(ref);

        MapProperty mapProperty = new MapProperty();
        mapProperty.setAdditionalProperties(refProperty);

        expectCallToExternalRefProcessor(ref, RefFormat.URL, "bar");

        new PropertyProcessor(cache, swagger).processProperty(mapProperty);

        new FullVerifications() {{
        }};

        assertEquals(((RefProperty)mapProperty.getAdditionalProperties()).get$ref(), "#/definitions/bar");
    }

    private void expectCreationOfExternalRefProcessor() {
        new StrictExpectations() {{
            new ExternalRefProcessor(cache, swagger);
            times = 1;
            result = externalRefProcessor;
        }};
    }
}
