package io.swagger.parser.processors;

import io.swagger.models.Model;
import io.swagger.models.Swagger;
import io.swagger.models.parameters.*;
import io.swagger.models.refs.RefFormat;
import io.swagger.parser.ResolverCache;
import mockit.FullVerifications;
import mockit.Injectable;
import mockit.Mocked;
import mockit.StrictExpectations;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

import static org.testng.Assert.assertEquals;


public class ParameterProcessorTest {


    @Injectable
    ResolverCache cache;

    @Injectable
    Swagger swagger;

    @Mocked
    ModelProcessor modelProcessor;

    @Test
    public void testProcessParameters_TypesThatAreNotRefOrBody(@Injectable final HeaderParameter headerParameter,
                                                               @Injectable final QueryParameter queryParameter,
                                                               @Injectable final CookieParameter cookieParameter,
                                                               @Injectable final PathParameter pathParameter,
                                                               @Injectable final FormParameter formParameter) throws Exception {
        expectedModelProcessorCreation();

        final List<Parameter> processedParameters = new ParameterProcessor(cache, swagger)
                .processParameters(Arrays.<Parameter>asList(headerParameter,
                        queryParameter,
                        cookieParameter,
                        pathParameter,
                        formParameter));

        new FullVerifications() {{
        }};

        assertEquals(processedParameters.size(), 5);
        assertEquals(processedParameters.get(0), headerParameter);
        assertEquals(processedParameters.get(1), queryParameter);
        assertEquals(processedParameters.get(2), cookieParameter);
        assertEquals(processedParameters.get(3), pathParameter);
        assertEquals(processedParameters.get(4), formParameter);
    }

    @Test
    public void testProcessParameters_RefToHeader(
            @Injectable final HeaderParameter resolvedHeaderParam) throws Exception {
        expectedModelProcessorCreation();

        final String ref = "#/parameters/foo";
        RefParameter refParameter = new RefParameter(ref);

        expectLoadingRefFromCache(ref, RefFormat.INTERNAL, resolvedHeaderParam);

        final List<Parameter> processedParameters = new ParameterProcessor(cache, swagger)
                .processParameters(Arrays.<Parameter>asList(refParameter));

        new FullVerifications(){{}};

        assertEquals(processedParameters.size(), 1);
        assertEquals(processedParameters.get(0), resolvedHeaderParam);
    }

    private void expectLoadingRefFromCache(final String ref, final RefFormat refFormat,
                                           final Parameter resolvedParam) {
        new StrictExpectations() {{
            cache.loadRef(ref, refFormat, Parameter.class);
            times = 1;
            result = resolvedParam;
        }};
    }

    @Test
    public void testProcessParameters_BodyParameter(@Injectable final Model bodyParamSchema) throws Exception {

        expectedModelProcessorCreation();

        BodyParameter bodyParameter = new BodyParameter().schema(bodyParamSchema);

        expectModelProcessorInvoked(bodyParamSchema);

        final List<Parameter> processedParameters = new ParameterProcessor(cache, swagger)
                .processParameters(Arrays.<Parameter>asList(bodyParameter));

        new FullVerifications(){{}};

        assertEquals(processedParameters.size(), 1);
        assertEquals(processedParameters.get(0), bodyParameter);
    }

    private void expectModelProcessorInvoked(@Injectable final Model bodyParamSchema) {
        new StrictExpectations(){{
            modelProcessor.processModel(bodyParamSchema); times=1;
        }};
    }

    @Test
    public void testProcessParameters_RefToBodyParam(@Injectable final Model bodyParamSchema) throws Exception {
        expectedModelProcessorCreation();

        final String ref = "#/parameters/foo";
        RefParameter refParameter = new RefParameter(ref);
        final BodyParameter resolvedBodyParam = new BodyParameter().schema(bodyParamSchema);

        expectLoadingRefFromCache(ref, RefFormat.INTERNAL, resolvedBodyParam);
        expectModelProcessorInvoked(bodyParamSchema);

        final List<Parameter> processedParameters = new ParameterProcessor(cache, swagger)
                .processParameters(Arrays.<Parameter>asList(refParameter));

        new FullVerifications(){{}};

        assertEquals(processedParameters.size(), 1);
        assertEquals(processedParameters.get(0), resolvedBodyParam);
    }

    private void expectedModelProcessorCreation() {
        new StrictExpectations() {{
            new ModelProcessor(cache, swagger);
            times = 1;
            result = modelProcessor;
        }};
    }
}
