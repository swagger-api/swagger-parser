package io.swagger.parser.processors;


import io.swagger.models.Operation;
import io.swagger.models.RefResponse;
import io.swagger.models.Response;
import io.swagger.models.Swagger;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.refs.RefFormat;
import io.swagger.parser.ResolverCache;
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
    Swagger swagger;

    @Mocked
    ParameterProcessor parameterProcessor;

    @Mocked
    ResponseProcessor responseProcessor;

    @Test
    public void testProcessOperation(@Injectable final List<Parameter> inputParameterList,
                                     @Injectable final List<Parameter> outputParameterList,
                                     @Injectable final Response incomingResponse,
                                     @Injectable final Response resolvedResponse) throws Exception {

        Operation operation = new Operation();
        operation.setParameters(inputParameterList);

        final String ref = "http://my.company.com/path/to/file.json#/foo/bar";
        final RefResponse refResponse = new RefResponse(ref);

        operation.response(200, refResponse);
        operation.response(400, incomingResponse);

        new Expectations() {{
            new ParameterProcessor(cache, swagger);
            times = 1;
            result = parameterProcessor;
            new ResponseProcessor(cache, swagger);
            times = 1;
            result = responseProcessor;

            parameterProcessor.processParameters(inputParameterList);
            times = 1;
            result = outputParameterList;

            responseProcessor.processResponse(refResponse);
            times = 1;

            cache.loadRef(ref, RefFormat.URL, Response.class);
            times = 1;
            result = resolvedResponse;

            responseProcessor.processResponse(incomingResponse);
            times = 1;
            responseProcessor.processResponse(resolvedResponse);
            times = 1;
        }};

        new OperationProcessor(cache, swagger).processOperation(operation);

        new FullVerifications() {{}};

        assertEquals(operation.getResponses().get("200"), resolvedResponse);
        assertEquals(operation.getParameters(), outputParameterList);
    }
}
