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

        new Expectations() {{

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

        final ExternalRefProcessor[] externalRefProcessor1 = {new ExternalRefProcessor(cache, openAPI)};
        new Expectations() {{
            externalRefProcessor1[0] = new ExternalRefProcessor(cache, openAPI);
            times = 1;
        }};

        final String ref = "http://my.company.com/path/to/file.json#/foo/bar";
        final Schema refProperty = new Schema().$ref(ref);

        new Expectations() {{
            externalRefProcessor1[0].processRefToExternalSchema(ref, RefFormat.URL );
            times = 1;
            result = "bar";
        }};

        new SchemaProcessor(cache, openAPI).processSchema(refProperty);

        new FullVerifications() {{
            externalRefProcessor1[0].processRefToExternalSchema(ref, RefFormat.URL);
            times = 1;
        }};

        assertEquals(refProperty.get$ref(), "#/components/schemas/bar");
    }

    @Test
    public void testProcessRefProperty_InternalRef() throws Exception {
        new Expectations() {{
            new ExternalRefProcessor(cache, openAPI);
            times = 1;
        }};

        final String expectedRef = "#/components/schemas/foo";
        final Schema property = new Schema().$ref(expectedRef);
        new SchemaProcessor(cache, openAPI).processSchema(property);

        new FullVerifications() {{
        }};

        assertEquals(property.get$ref(), expectedRef);
    }

    @Test
    public void testProcessArrayProperty_ItemsIsRefProperty() throws Exception {
        final ExternalRefProcessor[] externalRefProcessor1 = {new ExternalRefProcessor(cache, openAPI)};
        new Expectations() {{
            externalRefProcessor1[0] = new ExternalRefProcessor(cache, openAPI);
            times = 1;
        }};
        final String ref = "http://my.company.com/path/to/file.json#/foo/bar";
        final Schema refProperty = new Schema().$ref(ref);

        ArraySchema arrayProperty = new ArraySchema();
        arrayProperty.setItems(refProperty);
        new Expectations() {{
            externalRefProcessor1[0].processRefToExternalSchema(ref, RefFormat.URL );
            times = 1;
            result = "bar";
        }};

        new SchemaProcessor(cache, openAPI).processSchema(arrayProperty);

        new FullVerifications() {{
            externalRefProcessor1[0].processRefToExternalSchema(ref, RefFormat.URL);
            times = 1;
        }};

        assertEquals((arrayProperty.getItems()).get$ref(), "#/components/schemas/bar");

    }

    @Test
    public void testProcessMapProperty_AdditionalPropertiesIsRefProperty() throws Exception {
        final ExternalRefProcessor[] externalRefProcessor1 = {new ExternalRefProcessor(cache, openAPI)};
        new Expectations() {{
            externalRefProcessor1[0] = new ExternalRefProcessor(cache, openAPI);
            times = 1;
        }};
        final String ref = "http://my.company.com/path/to/file.json#/foo/bar";
        final Schema refProperty = new Schema().$ref(ref);

        refProperty.setAdditionalProperties(refProperty);

        new Expectations() {{
            externalRefProcessor1[0].processRefToExternalSchema(ref, RefFormat.URL );
            times = 1;
            result = "bar";
        }};

        new SchemaProcessor(cache, openAPI).processSchema(refProperty);

        new FullVerifications() {{
            externalRefProcessor1[0].processRefToExternalSchema(ref, RefFormat.URL);
            times = 1;
        }};

        assertEquals((((Schema)refProperty.getAdditionalProperties()).get$ref()), "#/components/schemas/bar");
    }
}
