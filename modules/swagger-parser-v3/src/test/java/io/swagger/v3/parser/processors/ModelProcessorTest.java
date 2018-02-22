package io.swagger.v3.parser.processors;


import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.ResolverCache;
import io.swagger.v3.parser.models.RefFormat;
import mockit.*;
import org.testng.annotations.Test;

import java.util.Arrays;

import static org.testng.Assert.assertEquals;


public class ModelProcessorTest {

    @Injectable
    ResolverCache cache;

    @Injectable
    OpenAPI openAPI;

    @Mocked
    SchemaProcessor schemaProcessor;

    @Mocked
    ExternalRefProcessor externalRefProcessor;

    @Test
    public void testProcessRefModel_ExternalRef() throws Exception {

        final String ref = "http://my.company.com/path/to/file.json#/foo/bar";
        final String newRef = "bar";

        setupPropertyAndExternalRefProcessors();

        new StrictExpectations() {{
            externalRefProcessor.processRefToExternalSchema(ref, RefFormat.URL);
            times = 1;
            result = newRef;
        }};

        Schema refModel = new Schema().$ref(ref);

        new SchemaProcessor(cache, openAPI).processSchema(refModel);

        assertEquals(refModel.get$ref(), "#/components/schemas/bar");
    }

    @Test
    public void testProcessRefModel_InternalRef() throws Exception {
        final String ref = "#/components/schemas/bar";

        setupPropertyAndExternalRefProcessors();

        Schema refModel = new Schema().$ref(ref);

        new SchemaProcessor(cache, openAPI).processSchema(refModel);

        assertEquals(refModel.get$ref(), ref);
    }

    @Test
    public void testProcessArrayModel(@Injectable final Schema property) throws Exception {
        setupPropertyAndExternalRefProcessors();

        new StrictExpectations() {{
            schemaProcessor.processSchema(property);
            times = 1;
        }};

        ArraySchema model = new ArraySchema();
        model.setItems(property);

        new SchemaProcessor(cache, openAPI).processSchema(model);
    }

    @Test
    public void testProcessComposedModel() throws Exception {
        setupPropertyAndExternalRefProcessors();

        final String ref1 = "http://my.company.com/path/to/file.json#/foo/bar";
        final String ref2 = "http://my.company.com/path/to/file.json#/this/that";
        final String ref3 = "http://my.company.com/path/to/file.json#/hello/world";

        SchemaProcessor modelProcessor = new SchemaProcessor(cache, openAPI);

        new Expectations() {{
            externalRefProcessor.processRefToExternalSchema(ref1, RefFormat.URL);
            times = 1;
            result = "bar";
            externalRefProcessor.processRefToExternalSchema(ref2, RefFormat.URL);
            times = 1;
            result = "that";
            externalRefProcessor.processRefToExternalSchema(ref3, RefFormat.URL);
            times = 1;
            result = "world";
        }};

        ComposedSchema composedModel = new ComposedSchema();
        composedModel.addAllOfItem(new Schema().$ref(ref1));
        composedModel.addAnyOfItem(new Schema().$ref(ref2));
        composedModel.setAllOf(Arrays.asList(new Schema().$ref(ref3)));

        modelProcessor.processSchema(composedModel);

        new FullVerifications() {{
            externalRefProcessor.processRefToExternalSchema(ref1, RefFormat.URL);
            times = 1;
            externalRefProcessor.processRefToExternalSchema(ref2, RefFormat.URL);
            times = 1;
            externalRefProcessor.processRefToExternalSchema(ref3, RefFormat.URL);
            times = 1;
        }};

        assertEquals(composedModel.get$ref(), "#/components/schemas/bar");//child
        assertEquals(composedModel.get$ref(), "#/components/schemas/that"); //parent
        assertEquals(composedModel.getAllOf().get(0).get$ref(), "#/components/schemas/world");
    }

    @Test
    public void testProcessModelImpl(@Injectable final Schema property1,
                                     @Injectable final Schema property2) throws Exception {
        setupPropertyAndExternalRefProcessors();

        Schema model = new Schema();
        model.addProperties("foo", property1);
        model.addProperties("bar", property2);

        new Expectations() {{
            schemaProcessor.processSchema(property1);
            times = 1;
            schemaProcessor.processSchema(property2);
            times = 1;
        }};

        new SchemaProcessor(cache, openAPI).processSchema(model);

        new FullVerifications(){{}};
    }

    private void setupPropertyAndExternalRefProcessors() {
        new StrictExpectations() {{
            new SchemaProcessor(cache, openAPI);
            times = 1;
            result = schemaProcessor;

            new ExternalRefProcessor(cache, openAPI);
            times = 1;
            result = externalRefProcessor;
        }};
    }
}
