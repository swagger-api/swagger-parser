package io.swagger.v3.parser.processors;


import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.ResolverCache;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.testng.Assert.assertEquals;


public class ComponentsProcessorTest {

    // Every processor created inside ComponentsProcessor is a mock.
    private final List<MockedConstruction<?>> constructions = new ArrayList<>();
    private MockedConstruction<SchemaProcessor> schemaProcessor;
    private MockedConstruction<ResponseProcessor> responseProcessor;
    private MockedConstruction<RequestBodyProcessor> requestBodyProcessor;
    private MockedConstruction<ParameterProcessor> parameterProcessor;
    private MockedConstruction<HeaderProcessor> headerProcessor;
    private MockedConstruction<ExampleProcessor> exampleProcessor;
    private MockedConstruction<LinkProcessor> linkProcessor;
    private MockedConstruction<CallbackProcessor> callbackProcessor;
    private MockedConstruction<SecuritySchemeProcessor> securitySchemeProcessor;

    Schema model1 = mock(Schema.class);

    Schema model2 = mock(Schema.class);

    ResolverCache cache = mock(ResolverCache.class);

    boolean openapi31 = false;

    OpenAPI openAPI = mock(OpenAPI.class);

    private <T> MockedConstruction<T> track(MockedConstruction<T> construction) {
        constructions.add(construction);
        return construction;
    }

    @BeforeMethod
    public void mockProcessors() {
        schemaProcessor = track(Mockito.mockConstruction(SchemaProcessor.class));
        responseProcessor = track(Mockito.mockConstruction(ResponseProcessor.class));
        requestBodyProcessor = track(Mockito.mockConstruction(RequestBodyProcessor.class));
        parameterProcessor = track(Mockito.mockConstruction(ParameterProcessor.class));
        headerProcessor = track(Mockito.mockConstruction(HeaderProcessor.class));
        exampleProcessor = track(Mockito.mockConstruction(ExampleProcessor.class));
        linkProcessor = track(Mockito.mockConstruction(LinkProcessor.class));
        callbackProcessor = track(Mockito.mockConstruction(CallbackProcessor.class));
        securitySchemeProcessor = track(Mockito.mockConstruction(SecuritySchemeProcessor.class));
    }

    @AfterMethod(alwaysRun = true)
    public void closeMocks() {
        constructions.forEach(MockedConstruction::close);
        constructions.clear();
    }

    @Test
    public void testComponentsSchemasProcessor() throws Exception {
        final OpenAPI openAPI = new OpenAPI();
        openAPI.components(new Components().addSchemas("foo", model1));
        openAPI.getComponents().addSchemas("bar", model2);

        new ComponentsProcessor(openAPI,cache, openapi31).processComponents();

        assertEquals(schemaProcessor.constructed().size(), 1);
        SchemaProcessor processor = schemaProcessor.constructed().get(0);
        verify(processor, times(2)).processSchema(any(Schema.class));
        verify(processor).processSchema(model1);
        verify(processor).processSchema(model2);
    }

    @Test
    public void testNoComponentsDefined() throws Exception {

        new ComponentsProcessor(openAPI,cache, openapi31).processComponents();

        // each processor is created exactly once
        assertEquals(schemaProcessor.constructed().size(), 1);
        assertEquals(responseProcessor.constructed().size(), 1);
        assertEquals(requestBodyProcessor.constructed().size(), 1);
        assertEquals(parameterProcessor.constructed().size(), 1);
        assertEquals(headerProcessor.constructed().size(), 1);
        assertEquals(exampleProcessor.constructed().size(), 1);
        assertEquals(linkProcessor.constructed().size(), 1);
        assertEquals(callbackProcessor.constructed().size(), 1);
        assertEquals(securitySchemeProcessor.constructed().size(), 1);

        // full verification: only openAPI.getComponents() was called, once, and nothing else
        verify(openAPI, times(1)).getComponents();
        verifyNoMoreInteractions(openAPI);
        verifyNoInteractions(cache);
        for (MockedConstruction<?> construction : constructions) {
            construction.constructed().forEach(processor -> verifyNoInteractions(processor));
        }
    }
}
