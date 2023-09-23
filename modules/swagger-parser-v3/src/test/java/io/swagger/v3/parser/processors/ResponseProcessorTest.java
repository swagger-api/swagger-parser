package io.swagger.v3.parser.processors;



import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.parser.ResolverCache;
import io.swagger.v3.parser.models.RefFormat;
import mockit.FullVerifications;
import mockit.Injectable;
import mockit.Mocked;
import mockit.Expectations;
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

    @Injectable
    boolean openapi31;

    @Injectable
    Schema responseSchema;
    @Injectable
    Header responseHeader;

    //@Test
    public void testProcessResponse() throws Exception {

        new Expectations(){{
            new SchemaProcessor(cache, swagger, openapi31);
            times=1;

            new HeaderProcessor(cache,swagger, openapi31);
            times = 1;

            new LinkProcessor(cache,swagger, openapi31);
            times = 1;

            propertyProcessor.processSchema(responseSchema);
            times=1;
        }};

        ApiResponse response = new ApiResponse();
        response.content(new Content().addMediaType("*/*", new MediaType().schema(responseSchema)));
        response.addHeaderObject("foo", responseHeader);

        new ResponseProcessor(cache, swagger, openapi31).processResponse(response);


        new FullVerifications(){{
            propertyProcessor.processSchema(responseSchema);
            times = 1;
            headerProcessor.processHeader(responseHeader);
            times = 1;
        }};
    }
}
