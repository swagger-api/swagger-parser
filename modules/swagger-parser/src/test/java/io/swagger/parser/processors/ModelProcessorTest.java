package io.swagger.parser.processors;

import io.swagger.models.*;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.RefProperty;
import io.swagger.models.refs.RefFormat;
import io.swagger.parser.ResolverCache;
import mockit.*;
import org.testng.annotations.Test;

import java.util.Arrays;

import static org.testng.Assert.assertEquals;


public class ModelProcessorTest {

    @Injectable
    ResolverCache cache;

    @Injectable
    Swagger swagger;

    @Mocked
    PropertyProcessor propertyProcessor;

    @Mocked
    ExternalRefProcessor externalRefProcessor;

    @Test
    public void testProcessRefModel_ExternalRef() throws Exception {

        final String ref = "http://my.company.com/path/to/file.json#/foo/bar";
        final String newRef = "bar";

        setupPropertyAndExternalRefProcessors();

        new StrictExpectations() {{
            externalRefProcessor.processRefToExternalDefinition(ref, RefFormat.URL);
            times = 1;
            result = newRef;
        }};

        RefModel refModel = new RefModel(ref);

        new ModelProcessor(cache, swagger).processModel(refModel);

        assertEquals(refModel.get$ref(), "#/definitions/bar");
    }

    @Test
    public void testProcessRefModel_InternalRef() throws Exception {
        final String ref = "#/definitions/bar";

        setupPropertyAndExternalRefProcessors();

        RefModel refModel = new RefModel(ref);

        new ModelProcessor(cache, swagger).processModel(refModel);

        assertEquals(refModel.get$ref(), ref);
    }

    @Test
    public void testProcessArrayModel(@Injectable final Property property) throws Exception {
        setupPropertyAndExternalRefProcessors();

        new StrictExpectations() {{
            propertyProcessor.processProperty(property);
            times = 1;
        }};

        ArrayModel model = new ArrayModel();
        model.setItems(property);

        new ModelProcessor(cache, swagger).processModel(model);
    }

    @Test
    public void testProcessComposedModel() throws Exception {
        setupPropertyAndExternalRefProcessors();

        final String ref1 = "http://my.company.com/path/to/file.json#/foo/bar";
        final String ref2 = "http://my.company.com/path/to/file.json#/this/that";
        final String ref3 = "http://my.company.com/path/to/file.json#/hello/world";

        ModelProcessor modelProcessor = new ModelProcessor(cache, swagger);

        new Expectations() {{
            externalRefProcessor.processRefToExternalDefinition(ref1, RefFormat.URL);
            times = 1;
            result = "bar";
            externalRefProcessor.processRefToExternalDefinition(ref2, RefFormat.URL);
            times = 1;
            result = "that";
            externalRefProcessor.processRefToExternalDefinition(ref3, RefFormat.URL);
            times = 1;
            result = "world";
        }};

        ComposedModel composedModel = new ComposedModel();
        composedModel.child(new RefModel(ref1));
        composedModel.parent(new RefModel(ref2));
        composedModel.interfaces(Arrays.asList(new RefModel(ref3)));

        modelProcessor.processModel(composedModel);

        new FullVerifications() {{
            externalRefProcessor.processRefToExternalDefinition(ref1, RefFormat.URL);
            times = 1;
            externalRefProcessor.processRefToExternalDefinition(ref2, RefFormat.URL);
            times = 1;
            externalRefProcessor.processRefToExternalDefinition(ref3, RefFormat.URL);
            times = 1;
        }};

        assertEquals(((RefModel) composedModel.getChild()).get$ref(), "#/definitions/bar");
        assertEquals(((RefModel) composedModel.getParent()).get$ref(), "#/definitions/that");
        assertEquals((composedModel.getInterfaces().get(0)).get$ref(), "#/definitions/world");
    }

    @Test
    public void testProcessModelImpl(@Injectable final Property property1,
                                     @Injectable final Property property2) throws Exception {
        setupPropertyAndExternalRefProcessors();

        ModelImpl model = new ModelImpl();
        model.addProperty("foo", property1);
        model.addProperty("bar", property2);

        new Expectations() {{
            propertyProcessor.processProperty(property1);
            times = 1;
            propertyProcessor.processProperty(property2);
            times = 1;
        }};

        new ModelProcessor(cache, swagger).processModel(model);

        new FullVerifications(){{}};
    }

    private void setupPropertyAndExternalRefProcessors() {
        new StrictExpectations() {{
            new PropertyProcessor(cache, swagger);
            times = 1;
            result = propertyProcessor;

            new ExternalRefProcessor(cache, swagger);
            times = 1;
            result = externalRefProcessor;
        }};
    }


    @Test
    public void testProcessComposedModelWithProperties(@Injectable final Property property1) throws Exception {
        setupPropertyAndExternalRefProcessors();

        final String ref1 = "http://my.company.com/path/to/file.json#/foo/bar";
        final String ref2 = "http://my.company.com/path/to/file.json#/this/that";
        final String ref3 = "http://my.company.com/path/to/file.json#/hello/world";
        final String ref4 = "http://my.company.com/path/to/file.json#/hello/ref";
        final Property property2 = new RefProperty(ref4);

        ModelProcessor modelProcessor = new ModelProcessor(cache, swagger);

        new Expectations() {{
            externalRefProcessor.processRefToExternalDefinition(ref1, RefFormat.URL);
            times = 1;
            result = "bar";
            externalRefProcessor.processRefToExternalDefinition(ref2, RefFormat.URL);
            times = 1;
            result = "that";
            externalRefProcessor.processRefToExternalDefinition(ref3, RefFormat.URL);
            times = 1;
            result = "world";
            propertyProcessor.processProperty(property1);
            times = 1;
            propertyProcessor.processProperty(property2);
            times = 1;
        }};

        ComposedModel composedModel = new ComposedModel();
        composedModel.child(new RefModel(ref1));
        composedModel.parent(new RefModel(ref2));
        composedModel.interfaces(Arrays.asList(new RefModel(ref3)));
        composedModel.addProperty("foo", property1);
        composedModel.addProperty("bar", property2);

        modelProcessor.processModel(composedModel);

        new FullVerifications() {{
            externalRefProcessor.processRefToExternalDefinition(ref1, RefFormat.URL);
            times = 1;
            externalRefProcessor.processRefToExternalDefinition(ref2, RefFormat.URL);
            times = 1;
            externalRefProcessor.processRefToExternalDefinition(ref3, RefFormat.URL);
            times = 1;
        }};

        assertEquals(((RefModel) composedModel.getChild()).get$ref(), "#/definitions/bar");
        assertEquals(((RefModel) composedModel.getParent()).get$ref(), "#/definitions/that");
        assertEquals((composedModel.getInterfaces().get(0)).get$ref(), "#/definitions/world");
    }
}
