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
import mockit.*;
import org.testng.annotations.Test;


import java.util.Arrays;
import java.util.List;

import static org.testng.Assert.assertEquals;


public class ParameterProcessorTest {


    @Injectable
    ResolverCache cache;

    @Injectable
    OpenAPI openAPI;

    @Mocked
    SchemaProcessor modelProcessor;

    @Test
    public void testProcessParameters_TypesThatAreNotRefOrBody(@Injectable final HeaderParameter headerParameter,
                                                               @Injectable final QueryParameter queryParameter,
                                                               @Injectable final CookieParameter cookieParameter,
                                                               @Injectable final PathParameter pathParameter) throws Exception {
        expectedModelProcessorCreation();
        new Expectations() {
            {
                headerParameter.getSchema();
                result = null;
                queryParameter.getSchema();
                result = null;
                cookieParameter.getSchema();
                result = null;
                pathParameter.getSchema();
                result = null;
            }
        };
        final List<Parameter> processedParameters = new ParameterProcessor(cache, openAPI)
                .processParameters(Arrays.<Parameter>asList(headerParameter,
                        queryParameter,
                        cookieParameter,
                        pathParameter));

        new FullVerifications() {{
            headerParameter.get$ref();
            times = 1;
            queryParameter.get$ref();
            times = 1;
            cookieParameter.get$ref();
            times = 1;
            pathParameter.get$ref();
            times = 1;
        }};

        assertEquals(processedParameters.size(), 4);
        assertEquals(processedParameters.get(0), headerParameter);
        assertEquals(processedParameters.get(1), queryParameter);
        assertEquals(processedParameters.get(2), cookieParameter);
        assertEquals(processedParameters.get(3), pathParameter);
    }

    @Test
    public void testProcessParameters_RefToHeader(
            @Injectable final HeaderParameter resolvedHeaderParam) throws Exception {
        expectedModelProcessorCreation();

        final String ref = "#/components/parameters/foo";
        Parameter refParameter = new Parameter().$ref(ref);

        expectLoadingRefFromCache(ref, RefFormat.INTERNAL, resolvedHeaderParam);
        new Expectations() {
            {
                resolvedHeaderParam.getSchema();
                result = null;
            }
        };

        final List<Parameter> processedParameters = new ParameterProcessor(cache, openAPI)
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

    private void expectLoadingRefFromCache(final String ref, final RefFormat refFormat,
                                           final RequestBody resolvedParam) {
        new StrictExpectations() {{
            /*cache.loadRef(ref, refFormat, RequestBody.class);
            times = 1;
            result = resolvedParam;*/
        }};
    }

    @Test
    public void testProcessParameters_BodyParameter(@Injectable final Schema bodyParamSchema) throws Exception {

        expectedModelProcessorCreation();

        RequestBody bodyParameter = new RequestBody().content(new Content().addMediaType("*/*",new MediaType().schema(bodyParamSchema)));

        expectModelProcessorInvoked(bodyParamSchema);

        new RequestBodyProcessor(cache, openAPI).processRequestBody(bodyParameter);

        new FullVerifications(){{}};
    }

    private void expectModelProcessorInvoked(@Injectable final Schema bodyParamSchema) {
        new StrictExpectations(){{
            modelProcessor.processSchema(bodyParamSchema); times=1;
        }};
    }


    private void expectedModelProcessorCreation() {
        new StrictExpectations() {{
            new SchemaProcessor(cache, openAPI);
            times = 1;
            result = modelProcessor;
        }};
    }
}
