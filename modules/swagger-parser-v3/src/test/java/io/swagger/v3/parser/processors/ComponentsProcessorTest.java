package io.swagger.v3.parser.processors;


import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.ResolverCache;
import mockit.*;
import org.testng.annotations.Test;



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

    @Injectable
    Schema model1;

    @Injectable
    Schema model2;

    @Injectable
    ResolverCache cache;

    @Injectable
    boolean openapi31;

    @Injectable  OpenAPI openAPI;



    @Test
    public void testComponentsSchemasProcessor() throws Exception {
        final OpenAPI openAPI = new OpenAPI();
        openAPI.components(new Components().addSchemas("foo", model1));
        openAPI.getComponents().addSchemas("bar", model2);


        new Expectations() {{

            schemaProcessor.processSchema((Schema) any);
            times = 2;
        }};

        new ComponentsProcessor(openAPI,cache, openapi31).processComponents();

        new Verifications() {{
            schemaProcessor.processSchema(model1);
            schemaProcessor.processSchema(model2);
        }};
    }

    @Test
    public void testNoComponentsDefined() throws Exception {

        new Expectations() {{
            new SchemaProcessor(cache, openAPI, openapi31);
            times = 1;

            new ResponseProcessor(cache, openAPI, openapi31);
            times = 1;

            new RequestBodyProcessor(cache, openAPI, openapi31);
            times = 1;

            new ParameterProcessor( cache, openAPI, openapi31);
            times = 1;

            new HeaderProcessor(cache, openAPI, openapi31);
            times = 1;

            new ExampleProcessor(cache, openAPI);
            times = 1;

            new LinkProcessor(cache, openAPI, openapi31);
            times = 1;

            new CallbackProcessor(cache, openAPI, openapi31);
            times = 1;

            new SecuritySchemeProcessor(cache, openAPI);
            times = 1;

            openAPI.getComponents();
            times = 1;
            result = null;


        }};

        new ComponentsProcessor(openAPI,cache, openapi31).processComponents();

        new FullVerifications() {{
        }};
    }
}
