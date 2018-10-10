package io.swagger.v3.parser.processors;



import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.parser.ResolverCache;
import mockit.FullVerifications;
import mockit.Injectable;
import mockit.Mocked;
import mockit.StrictExpectations;
import org.testng.annotations.Test;




public class ResponseProcessorTest {

    @Injectable
    ResolverCache cache;

    @Injectable
    OpenAPI swagger;

    @Mocked
    SchemaProcessor propertyProcessor;

    @Mocked
    HeaderProcessor headerProcessor;

    @Mocked
    LinkProcessor linkProcessor;

    @Test
    public void testProcessResponse(@Injectable final Schema responseSchema,
                                    @Injectable final Header responseHeader) throws Exception {

        new StrictExpectations(){{
            new SchemaProcessor(cache, swagger);
            times=1;
            result = propertyProcessor;

            new HeaderProcessor(cache,swagger);
            times = 1;
            result = headerProcessor;

            new LinkProcessor(cache,swagger);
            times = 1;
            result = linkProcessor;


            propertyProcessor.processSchema(responseSchema);
            times=1;

            headerProcessor.processHeader(responseHeader);
            times = 1;


        }};

        ApiResponse response = new ApiResponse();
        response.content(new Content().addMediaType("*/*", new MediaType().schema(responseSchema)));
        response.addHeaderObject("foo", responseHeader);

        new ResponseProcessor(cache, swagger).processResponse(response);


        new FullVerifications(){{}};
    }
}
