package io.swagger.v3.parser.processors;


import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.ResolverCache;
import mockit.*;
import org.testng.annotations.Test;

import java.util.Map;

import static org.testng.Assert.assertEquals;

public class DefinitionsProcessorTest {


    @Mocked
    SchemaProcessor modelProcessor;
    @Mocked
    ComponentsProcessor componentsProcessor;

    @Test
    public void testComponentsSchemasProcessor(@Injectable final Schema model1,
                                         @Injectable final Schema model2,
                                         @Injectable final ResolverCache cache) throws Exception {

        final OpenAPI openAPI = new OpenAPI();
        openAPI.components(new Components().addSchemas("foo", model1));
        openAPI.components(new Components().addSchemas("bar", model2));


        new Expectations() {{
            new ComponentsProcessor( openAPI, cache);
            times = 1;
            result = componentsProcessor;

            componentsProcessor.processComponents();


            componentsProcessor.processSchemas(openAPI.getComponents().getSchemas().keySet(), openAPI.getComponents().getSchemas());

            new SchemaProcessor(cache, openAPI);
            times = 1;
            result = modelProcessor;
            modelProcessor.processSchema((Schema) any);
            times = 2;


        }};



        new Verifications() {{
            modelProcessor.processSchema(model1);
            modelProcessor.processSchema(model2);
        }};
    }

    @Test
    public void testNoDefinitionsDefined(@Injectable final OpenAPI openAPI,
                                         @Injectable final ResolverCache cache) throws Exception {


        new StrictExpectations() {{
            new SchemaProcessor(cache, openAPI);
            times = 1;
            result = modelProcessor;
            openAPI.getComponents().getSchemas();
            times = 1;
            result = null;

            componentsProcessor.processSchemas(null,null);

            new SchemaProcessor(cache,openAPI);
        }};




        new FullVerifications() {{
        }};
    }

    @Test
    public void testDefinitionsProcessor_RefModelInDefinitionsMap(@Injectable final Schema resolvedModel) throws Exception {
        final OpenAPI openAPI = new OpenAPI();
        final String ref = "http://my.company.com/path/to/file.json#/foo/bar";
        final Schema refModel = new Schema().$ref(ref);
        openAPI.components(new Components().addSchemas("foo", refModel));

        final MockUp<ResolverCache> mockup = new MockUp<ResolverCache>() {
            @Mock(invocations = 1)
            String getRenamedRef(String ref) {
                openAPI.getComponents().getSchemas().put("bar", resolvedModel);
                return "bar";
            }
        };

        final ResolverCache mockResolverCache = mockup.getMockInstance();


        new StrictExpectations() {{
            new SchemaProcessor(mockResolverCache, openAPI);
            times = 1;
            result = modelProcessor;

            modelProcessor.processSchema(refModel);
            times = 1;
        }};

        new ComponentsProcessor(openAPI, mockResolverCache);

        new FullVerifications(){{}};

        final Map<String, Schema> definitions = openAPI.getComponents().getSchemas();
        assertEquals(definitions.size(), 1);

        final Schema foo = definitions.get("foo");
        assertEquals(foo, resolvedModel);
    }
}
