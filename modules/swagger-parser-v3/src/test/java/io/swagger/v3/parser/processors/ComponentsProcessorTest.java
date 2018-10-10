package io.swagger.v3.parser.processors;


import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.ResolverCache;
import mockit.*;
import org.testng.annotations.Test;

import java.util.Map;

import static org.testng.Assert.assertEquals;

public class ComponentsProcessorTest {


    @Mocked
    SchemaProcessor schemaProcessor;

    @Mocked
    ResponseProcessor responseProcessor;

    @Mocked
    RequestBodyProcessor requestBodyProcessor;

    @Mocked
    ParameterProcessor parameterProcessor;

    @Mocked
    HeaderProcessor headerProcessor;

    @Mocked
    ExampleProcessor exampleProcessor;

    @Mocked
    LinkProcessor linkProcessor;

    @Mocked
    CallbackProcessor callbackProcessor;

    @Mocked
    SecuritySchemeProcessor securitySchemeProcessor;


    @Test
    public void testComponentsSchemasProcessor(@Injectable final Schema model1,
                                         @Injectable final Schema model2,
                                         @Injectable final ResolverCache cache) throws Exception {

        final OpenAPI openAPI = new OpenAPI();
        openAPI.components(new Components().addSchemas("foo", model1));
        openAPI.getComponents().addSchemas("bar", model2);



        new Expectations() {{

            new SchemaProcessor(cache, openAPI);
            times = 1;
            result = schemaProcessor;


            schemaProcessor.processSchema((Schema) any);
            times = 2;
        }};

        new ComponentsProcessor(openAPI,cache).processComponents();



        new Verifications() {{
            schemaProcessor.processSchema(model1);
            schemaProcessor.processSchema(model2);
        }};
    }

    @Test
    public void testNoComponentsDefined(@Injectable final OpenAPI openAPI,
                                         @Injectable final ResolverCache cache) throws Exception {


        new StrictExpectations() {{
            new SchemaProcessor(cache, openAPI);
            times = 1;
            result = schemaProcessor;

            new ResponseProcessor(cache, openAPI);
            times = 1;
            result = responseProcessor;

            new RequestBodyProcessor(cache, openAPI);
            times = 1;
            result = requestBodyProcessor;

            new ParameterProcessor( cache, openAPI);
            times = 1;
            result = parameterProcessor;

            new HeaderProcessor(cache, openAPI);
            times = 1;
            result = headerProcessor;

            new ExampleProcessor(cache, openAPI);
            times = 1;
            result = exampleProcessor;

            new LinkProcessor(cache, openAPI);
            times = 1;
            result = linkProcessor;

            new CallbackProcessor(cache, openAPI);
            times = 1;
            result = callbackProcessor;

            new SecuritySchemeProcessor(cache, openAPI);
            times = 1;
            result = securitySchemeProcessor;

            openAPI.getComponents();
            times = 1;
            result = null;


        }};

        new ComponentsProcessor(openAPI,cache).processComponents();

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
            @Mock
            String getRenamedRef(String ref) {
                openAPI.getComponents().getSchemas().put("bar", resolvedModel);
                return "bar";
            }
        };

        final ResolverCache mockResolverCache = mockup.getMockInstance();


        new StrictExpectations() {{

            new SchemaProcessor(mockResolverCache, openAPI);
            times = 1;
            result = schemaProcessor;

            new ResponseProcessor(mockResolverCache, openAPI);
            times = 1;
            result = responseProcessor;

            new RequestBodyProcessor(mockResolverCache, openAPI);
            times = 1;
            result = requestBodyProcessor;

            new ParameterProcessor( mockResolverCache, openAPI);
            times = 1;
            result = parameterProcessor;

            new HeaderProcessor(mockResolverCache, openAPI);
            times = 1;
            result = headerProcessor;

            new ExampleProcessor(mockResolverCache, openAPI);
            times = 1;
            result = exampleProcessor;

            new LinkProcessor(mockResolverCache, openAPI);
            times = 1;
            result = linkProcessor;

            new CallbackProcessor(mockResolverCache, openAPI);
            times = 1;
            result = callbackProcessor;

            new SecuritySchemeProcessor(mockResolverCache, openAPI);
            times = 1;
            result = securitySchemeProcessor;

            schemaProcessor.processSchema(refModel);
            times = 1;
        }};

        new ComponentsProcessor(openAPI, mockResolverCache).processComponents();

        new FullVerifications(){{}};

        final Map<String, Schema> definitions = openAPI.getComponents().getSchemas();
        assertEquals(definitions.size(), 1);

        final Schema foo = definitions.get("foo");
        assertEquals(foo, resolvedModel);
    }
}
