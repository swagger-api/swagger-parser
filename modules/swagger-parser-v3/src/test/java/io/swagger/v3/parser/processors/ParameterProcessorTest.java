package io.swagger.v3.parser.processors;


import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.CookieParameter;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.PathParameter;
import io.swagger.v3.oas.models.parameters.QueryParameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.parser.ResolverCache;
import io.swagger.v3.parser.models.RefFormat;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;


import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;


public class ParameterProcessorTest {


    ResolverCache cache = mock(ResolverCache.class);

    OpenAPI openAPI = mock(OpenAPI.class);

    // Every SchemaProcessor created inside ParameterProcessor / RequestBodyProcessor is a mock.
    private MockedConstruction<SchemaProcessor> modelProcessor;

    boolean openapi31 = false;

    HeaderParameter headerParameter = mock(HeaderParameter.class);

    QueryParameter queryParameter = mock(QueryParameter.class);

    CookieParameter cookieParameter = mock(CookieParameter.class);

    PathParameter pathParameter = mock(PathParameter.class);

    HeaderParameter resolvedHeaderParam = mock(HeaderParameter.class);

    Schema bodyParamSchema = mock(Schema.class);

    @BeforeMethod
    public void mockSchemaProcessor() {
        modelProcessor = Mockito.mockConstruction(SchemaProcessor.class);
    }

    @AfterMethod(alwaysRun = true)
    public void closeSchemaProcessor() {
        modelProcessor.close();
    }

    @Test
    public void testProcessParameters_TypesThatAreNotRefOrBody() throws Exception {
        when(headerParameter.getSchema()).thenReturn(null);
        when(headerParameter.getContent()).thenReturn(null);
        when(queryParameter.getSchema()).thenReturn(null);
        when(queryParameter.getContent()).thenReturn(null);
        when(cookieParameter.getSchema()).thenReturn(null);
        when(cookieParameter.getContent()).thenReturn(null);
        when(pathParameter.getSchema()).thenReturn(null);
        when(pathParameter.getContent()).thenReturn(null);

        final List<Parameter> processedParameters = new ParameterProcessor(cache, openAPI, openapi31)
                .processParameters(Arrays.asList(headerParameter,
                        queryParameter,
                        cookieParameter,
                        pathParameter));

        expectedModelProcessorCreation();

        // full verification: only these calls happened on the parameters
        for (Parameter parameter : Arrays.asList(headerParameter, queryParameter, cookieParameter, pathParameter)) {
            verify(parameter, times(1)).get$ref();
            verify(parameter, Mockito.atLeastOnce()).getSchema();
            verify(parameter, Mockito.atLeastOnce()).getContent();
            verifyNoMoreInteractions(parameter);
        }

        assertEquals(processedParameters.size(), 4);
        assertEquals(processedParameters.get(0), headerParameter);
        assertEquals(processedParameters.get(1), queryParameter);
        assertEquals(processedParameters.get(2), cookieParameter);
        assertEquals(processedParameters.get(3), pathParameter);
    }

    @Test
    public void testProcessParameters_RefToHeader() throws Exception {
        final String ref = "#/components/parameters/foo";
        Parameter refParameter = new Parameter().$ref(ref);

        expectLoadingRefFromCache(ref, RefFormat.INTERNAL, resolvedHeaderParam);
        when(resolvedHeaderParam.getSchema()).thenReturn(null);
        when(resolvedHeaderParam.getContent()).thenReturn(null);

        final List<Parameter> processedParameters = new ParameterProcessor(cache, openAPI, openapi31).processParameters(Arrays.asList(refParameter));

        expectedModelProcessorCreation();
        verify(cache, times(1)).loadRef(ref, RefFormat.INTERNAL, Parameter.class);
        verifyNoMoreInteractions(cache);
        verifyNoInteractions(openAPI);
        verify(resolvedHeaderParam, Mockito.atLeastOnce()).getSchema();
        verify(resolvedHeaderParam, Mockito.atLeastOnce()).getContent();
        verifyNoMoreInteractions(resolvedHeaderParam);

        assertEquals(processedParameters.size(), 1);
        assertEquals(processedParameters.get(0), resolvedHeaderParam);
    }

    private void expectLoadingRefFromCache(final String ref, final RefFormat refFormat,
                                           final Parameter resolvedParam) {
        when(cache.loadRef(ref, refFormat, Parameter.class)).thenReturn(resolvedParam);
    }

    @Test
    public void testProcessParameters_BodyParameter() throws Exception {
        RequestBody bodyParameter = new RequestBody().content(new Content().addMediaType("*/*",new MediaType().schema(bodyParamSchema)));

        new RequestBodyProcessor(cache, openAPI, openapi31).processRequestBody(bodyParameter);

        // RequestBodyProcessor creates exactly one SchemaProcessor, and it processes the body schema once
        expectedModelProcessorCreation();
        SchemaProcessor schemaProcessor = modelProcessor.constructed().get(0);
        verify(schemaProcessor, times(1)).processSchema(bodyParamSchema);
        verifyNoMoreInteractions(schemaProcessor);
        verifyNoInteractions(cache, openAPI);
    }

    private void expectedModelProcessorCreation() {
        assertEquals(modelProcessor.constructed().size(), 1);
    }
}
