package io.swagger.parser.processors;

import io.swagger.models.Model;
import io.swagger.models.ModelImpl;
import io.swagger.models.RefModel;
import io.swagger.models.Swagger;
import io.swagger.models.refs.RefFormat;
import io.swagger.parser.ResolverCache;
import mockit.Injectable;
import mockit.StrictExpectations;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;


public class ExternalRefProcessorTest {

    @Injectable
    ResolverCache cache;

    @Injectable
    Swagger swagger;

    @Test
    public void testProcessRefToExternalDefinition_NoNameConflict(
            @Injectable final Model mockedModel) throws Exception {

        final String ref = "http://my.company.com/path/to/file.json#/foo/bar";
        final RefFormat refFormat = RefFormat.URL;

        new StrictExpectations() {{
            cache.loadRef(ref, refFormat, Model.class);
            times = 1;
            result = mockedModel;

            swagger.getDefinitions();
            times = 1;
            result = null;

            cache.putRenamedRef(ref, "bar");
            swagger.addDefinition("bar", mockedModel); times=1;
        }};

        String newRef = new ExternalRefProcessor(cache, swagger).processRefToExternalDefinition(ref, refFormat);
        assertEquals(newRef, "bar");
    }


    @Test
    public void testProcessRefToExternalDefinition_NameConflict_FirstAppearance(
            @Injectable final Model mockedModel) throws Exception {

        final String ref = "http://my.company.com/path/to/file.json#/foo/bar";
        final RefFormat refFormat = RefFormat.URL;

        final Map<String, Model> definitionsMap = new HashMap<>();
        definitionsMap.put("bar", new ModelImpl());

        final String expectedNewRef = "bar1";

        new StrictExpectations() {{
            cache.loadRef(ref, refFormat, Model.class);
            times = 1;
            result = mockedModel;

            swagger.getDefinitions();
            times = 1;
            result = definitionsMap;

            cache.getRenamedRef(ref);
            times = 1;
            result = null;

            cache.putRenamedRef(ref, expectedNewRef); times=1;

            swagger.addDefinition(expectedNewRef, mockedModel);
        }};

        String newRef = new ExternalRefProcessor(cache, swagger).processRefToExternalDefinition(ref, refFormat);
        assertEquals(newRef, expectedNewRef);
    }

    @Test
    public void testProcessRefToExternalDefinition_NameConflict_SecondAppearance(@Injectable final Model mockedModel) throws Exception {
        final String ref = "http://my.company.com/path/to/file.json#/foo/bar";
        final RefFormat refFormat = RefFormat.URL;

        final Map<String, Model> definitionsMap = new HashMap<>();
        definitionsMap.put("bar", new ModelImpl());
        definitionsMap.put("bar1", mockedModel);

        final String expectedNewRef = "bar1";


        new StrictExpectations() {{
            cache.loadRef(ref, refFormat, Model.class);
            times = 1;
            result = mockedModel;

            swagger.getDefinitions();
            times = 1;
            result = definitionsMap;

            cache.getRenamedRef(ref);
            times = 1;
            result = expectedNewRef;
        }};

        String newRef = new ExternalRefProcessor(cache, swagger).processRefToExternalDefinition(ref, refFormat);
        assertEquals(newRef, expectedNewRef);
    }
}
