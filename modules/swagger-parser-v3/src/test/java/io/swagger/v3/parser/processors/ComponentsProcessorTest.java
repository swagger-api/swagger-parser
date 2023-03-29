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

    @Injectable
    boolean openapi31;

    @Test
    public void testComponentsSchemasProcessor(@Injectable final Schema model1,
                                         @Injectable final Schema model2,
                                         @Injectable final ResolverCache cache) throws Exception {

        final OpenAPI openAPI = new OpenAPI();
        openAPI.components(new Components().addSchemas("foo", model1));
        openAPI.getComponents().addSchemas("bar", model2);



        new Expectations() {{

            new SchemaProcessor(cache, openAPI, openapi31);
            times = 1;
            result = schemaProcessor;


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
    public void testNoComponentsDefined(@Injectable final OpenAPI openAPI,
                                         @Injectable final ResolverCache cache) throws Exception {


        new Expectations() {{
            new SchemaProcessor(cache, openAPI, openapi31);
            times = 1;
            result = schemaProcessor;

            new ResponseProcessor(cache, openAPI, openapi31);
            times = 1;
            result = responseProcessor;

            new RequestBodyProcessor(cache, openAPI, openapi31);
            times = 1;
            result = requestBodyProcessor;

            new ParameterProcessor( cache, openAPI, openapi31);
            times = 1;
            result = parameterProcessor;

            new HeaderProcessor(cache, openAPI, openapi31);
            times = 1;
            result = headerProcessor;

            new ExampleProcessor(cache, openAPI);
            times = 1;
            result = exampleProcessor;

            new LinkProcessor(cache, openAPI, openapi31);
            times = 1;
            result = linkProcessor;

            new CallbackProcessor(cache, openAPI, openapi31);
            times = 1;
            result = callbackProcessor;

            new SecuritySchemeProcessor(cache, openAPI);
            times = 1;
            result = securitySchemeProcessor;

            openAPI.getComponents();
            times = 1;
            result = null;


        }};

        new ComponentsProcessor(openAPI,cache, openapi31).processComponents();

        new FullVerifications() {{
        }};
    }
}
