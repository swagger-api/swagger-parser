package io.swagger.parser.processors;


import io.swagger.models.Response;
import io.swagger.models.Swagger;
import io.swagger.models.properties.Property;
import io.swagger.parser.ResolverCache;
import mockit.FullVerifications;
import mockit.Injectable;
import mockit.Mocked;
import mockit.StrictExpectations;
import org.testng.annotations.Test;

public class ResponseProcessorTest {

    @Injectable
    ResolverCache cache;

    @Injectable
    Swagger swagger;

    @Mocked
    PropertyProcessor propertyProcessor;

    @Test
    public void testProcessResponse(@Injectable final Property responseSchema,
                                    @Injectable final Property responseHeader) throws Exception {

        new StrictExpectations(){{
            new PropertyProcessor(cache, swagger); times=1; result = propertyProcessor;

            propertyProcessor.processProperty(responseSchema); times=1;
        }};

        Response response = new Response();
        response.setSchema(responseSchema);
        response.addHeader("foo", responseHeader);

        new ResponseProcessor(cache, swagger).processResponse(response);

        new FullVerifications(){{}};
    }
}
