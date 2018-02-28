package io.swagger.v3.parser.processors;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.ResolverCache;
import io.swagger.v3.parser.models.RefFormat;
import mockit.*;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;

import static org.testng.Assert.assertEquals;


public class SchemaProcessorTest {

    @Injectable
    ResolverCache cache;

    @Injectable
    OpenAPI openAPI;

    @Mocked
    ExternalRefProcessor externalRefProcessor;

    @Test
    public void testProcessRefSchema_ExternalRef() throws Exception {

        final String ref = "http://my.company.com/path/to/file.json#/foo/bar";
        final String newRef = "bar";

        setupPropertyAndExternalRefProcessors();

        new StrictExpectations() {{

            externalRefProcessor.processRefToExternalSchema(ref, RefFormat.URL);
            times = 1;
            result = newRef;

        }};

        Schema refSchema = new Schema().$ref(ref);

        new SchemaProcessor(cache, openAPI).processSchema(refSchema);

        assertEquals(refSchema.get$ref(), "#/components/schemas/bar");
    }

    @Test
    public void testProcessRefSchema_InternalRef() throws Exception {
        final String ref = "#/components/schemas/bar";

        setupPropertyAndExternalRefProcessors();

        Schema refModel = new Schema().$ref(ref);

        new SchemaProcessor(cache, openAPI).processSchema(refModel);

        assertEquals(refModel.get$ref(), ref);
    }



    @Test
    public void testProcessArraySchema(@Injectable final Schema property,
                                       @Mocked SchemaProcessor propertyProcessor) throws Exception {

        //setupPropertyAndExternalRefProcessors();

        new StrictExpectations() {{
            new SchemaProcessor(cache,openAPI);
            times = 1;
            result = propertyProcessor;


            propertyProcessor.processSchema(property);
            times = 1;

        }};

        ArraySchema model = new ArraySchema();
        model.setItems(property);

        SchemaProcessor schemaProcessor = new SchemaProcessor(cache, openAPI);
        schemaProcessor.processSchema(model);
    }

    @Test
    public void testProcessComposedSchema() throws Exception {
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
        composedModel.addAllOfItem(new Schema().$ref(ref2));
        composedModel.addAllOfItem(new Schema().$ref(ref3));

        modelProcessor.processSchema(composedModel);

        new FullVerifications() {{
            externalRefProcessor.processRefToExternalSchema(ref1, RefFormat.URL);
            times = 1;
            externalRefProcessor.processRefToExternalSchema(ref2, RefFormat.URL);
            times = 1;
            externalRefProcessor.processRefToExternalSchema(ref3, RefFormat.URL);
            times = 1;
        }};

        assertEquals(composedModel.getAllOf().get(0).get$ref(),"#/components/schemas/bar");//child
        assertEquals(composedModel.getAllOf().get(1).get$ref(), "#/components/schemas/that"); //parent
        assertEquals(composedModel.getAllOf().get(2).get$ref(), "#/components/schemas/world");
    }

    @Test
    public void testProcessSchema(@Injectable final Schema property1,
                                     @Injectable final Schema property2, @Mocked
                                              SchemaProcessor propertyProcessor) throws Exception {
        setupPropertyAndExternalRefProcessors();

        Schema model = new Schema();
        model.addProperties("foo", property1);
        model.addProperties("bar", property2);

        new Expectations() {{
            new SchemaProcessor(cache,openAPI);
            times = 1;
            result = propertyProcessor;

            propertyProcessor.processSchema(property1);
            times = 1;
            propertyProcessor.processSchema(property2);
            times = 1;
        }};

        new SchemaProcessor(cache, openAPI).processSchema(model);

        new FullVerifications(){{}};
    }

    private void setupPropertyAndExternalRefProcessors() {
        new StrictExpectations() {{
            new ExternalRefProcessor(cache, openAPI);
            times = 1;
            result = externalRefProcessor;
        }};
    }
}
