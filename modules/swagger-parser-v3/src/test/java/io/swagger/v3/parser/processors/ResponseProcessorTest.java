package io.swagger.v3.parser.processors;



import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.parser.ResolverCache;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.testng.Assert.assertEquals;




public class ResponseProcessorTest {

    ResolverCache cache = mock(ResolverCache.class);

    OpenAPI swagger = mock(OpenAPI.class);

    boolean openapi31 = false;

    Schema responseSchema = mock(Schema.class);
    Header responseHeader = mock(Header.class);

    //@Test
    public void testProcessResponse() throws Exception {

        try (MockedConstruction<SchemaProcessor> propertyProcessor = Mockito.mockConstruction(SchemaProcessor.class);
             MockedConstruction<HeaderProcessor> headerProcessor = Mockito.mockConstruction(HeaderProcessor.class);
             MockedConstruction<LinkProcessor> linkProcessor = Mockito.mockConstruction(LinkProcessor.class)) {

            ApiResponse response = new ApiResponse();
            response.content(new Content().addMediaType("*/*", new MediaType().schema(responseSchema)));
            response.addHeaderObject("foo", responseHeader);

            new ResponseProcessor(cache, swagger, openapi31).processResponse(response);

            assertEquals(propertyProcessor.constructed().size(), 1);
            assertEquals(headerProcessor.constructed().size(), 1);
            assertEquals(linkProcessor.constructed().size(), 1);

            verify(propertyProcessor.constructed().get(0), times(1)).processSchema(responseSchema);
            verify(headerProcessor.constructed().get(0), times(1)).processHeader(responseHeader);
            verifyNoMoreInteractions(propertyProcessor.constructed().get(0), headerProcessor.constructed().get(0));
        }
    }
}
