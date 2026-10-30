package io.swagger.v3.parser.processors;


import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.parser.ResolverCache;
import io.swagger.v3.parser.models.RefFormat;
import io.swagger.v3.parser.util.RefUtils;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testng.annotations.Test;

import java.util.List;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

public class OperationProcessorTest {

    ResolverCache cache = mock(ResolverCache.class);

    OpenAPI openAPI = mock(OpenAPI.class);

    boolean openapi31 = false;

    @Test (enabled = false)
    // TODO OAS3.1 - reenable failing on operation.getParameters()
    @SuppressWarnings("unchecked")
    public void testProcessOperation() throws Exception {
        final List<Parameter> inputParameterList = mock(List.class);
        final List<Parameter> outputParameterList = mock(List.class);
        final Parameter inputParameter = mock(Parameter.class);
        final ApiResponse incomingResponse = mock(ApiResponse.class);
        final ApiResponse resolvedResponse = mock(ApiResponse.class);
        when(inputParameterList.get(0)).thenReturn(inputParameter);

        Operation operation = new Operation();
        operation.setParameters(inputParameterList);

        final String ref = "http://my.company.com/path/to/file.json#/foo/bar";
        ApiResponse refResponse = new ApiResponse().$ref(ref);

        operation.responses(new ApiResponses().addApiResponse("200", refResponse));
        operation.getResponses().addApiResponse("400", incomingResponse);

        when(cache.loadRef(ref, RefFormat.URL, ApiResponse.class)).thenReturn(resolvedResponse);

        try (MockedConstruction<ParameterProcessor> parameterProcessors = Mockito.mockConstruction(ParameterProcessor.class,
                     (mock, context) -> when(mock.processParameters(inputParameterList)).thenReturn(outputParameterList));
             MockedConstruction<ResponseProcessor> responseProcessors = Mockito.mockConstruction(ResponseProcessor.class);
             MockedStatic<RefUtils> refUtils = Mockito.mockStatic(RefUtils.class, Mockito.CALLS_REAL_METHODS)) {

            new OperationProcessor(cache, openAPI, openapi31).processOperation(operation);

            assertEquals(parameterProcessors.constructed().size(), 1);
            assertEquals(responseProcessors.constructed().size(), 1);
            ParameterProcessor parameterProcessor = parameterProcessors.constructed().get(0);
            ResponseProcessor responseProcessor = responseProcessors.constructed().get(0);

            verify(parameterProcessor, times(1)).processParameter(inputParameter);
            verify(parameterProcessor, times(1)).processParameters(inputParameterList);
            verify(responseProcessor, times(1)).processResponse(refResponse);
            verify(responseProcessor, times(1)).processResponse(incomingResponse);
            verify(responseProcessor, times(1)).processResponse(resolvedResponse);
            refUtils.verify(() -> RefUtils.computeRefFormat(ref), atLeastOnce());
            verify(cache, times(1)).loadRef(ref, RefFormat.URL, ApiResponse.class);
            verify(incomingResponse, times(1)).get$ref();
        }

        assertEquals(operation.getResponses().get("200"), resolvedResponse);
        assertEquals(operation.getParameters(), outputParameterList);
    }
}
