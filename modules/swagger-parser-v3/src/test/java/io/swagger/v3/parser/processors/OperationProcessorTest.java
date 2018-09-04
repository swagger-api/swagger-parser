package io.swagger.v3.parser.processors;


import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.parser.ResolverCache;
import io.swagger.v3.parser.models.RefFormat;
import io.swagger.v3.parser.util.RefUtils;
import mockit.Expectations;
import mockit.FullVerifications;
import mockit.Injectable;
import mockit.Mocked;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;

public class OperationProcessorTest {

    @Injectable
    ResolverCache cache;

    @Injectable
    OpenAPI openAPI;

    @Mocked
    ParameterProcessor parameterProcessor;


    @Mocked
    ResponseProcessor responseProcessor;

    @Test
    public void testProcessOperation(@Injectable final List<Parameter> inputParameterList,
                                     @Injectable final List<Parameter> outputParameterList,
                                     @Injectable final ApiResponse incomingResponse,
                                     @Injectable final ApiResponse resolvedResponse) throws Exception {

        Operation operation = new Operation();
        operation.setParameters(inputParameterList);

        final String ref = "http://my.company.com/path/to/file.json#/foo/bar";
        ApiResponse refResponse = new ApiResponse().$ref(ref);

        operation.responses(new ApiResponses().addApiResponse("200", refResponse));
        operation.getResponses().addApiResponse("400", incomingResponse);


        new Expectations() {{
            new ParameterProcessor(cache, openAPI);
            times = 1;
            result = parameterProcessor;

            new ResponseProcessor(cache, openAPI);
            times = 1;
            result = responseProcessor;

            parameterProcessor.processParameters(inputParameterList);
            times = 1;
            result = outputParameterList;

            responseProcessor.processResponse(refResponse);
            times = 1;

            RefUtils.computeRefFormat(ref);
            times = 1;

            cache.loadRef(ref, RefFormat.URL, ApiResponse.class);
            times = 1;
            result = resolvedResponse;

            RefUtils.computeRefFormat(ref);
            times = 1;

            incomingResponse.get$ref();
            times = 1;

            responseProcessor.processResponse(incomingResponse);
            times = 1;
            responseProcessor.processResponse(resolvedResponse);
            times = 1;

        }};



        new OperationProcessor(cache, openAPI).processOperation(operation);


        new FullVerifications() {{}};

        assertEquals(operation.getResponses().get("200"), resolvedResponse);
        assertEquals(operation.getParameters(), outputParameterList);
    }
}
