package io.swagger.parser.processors;

import io.swagger.models.Response;
import io.swagger.models.Swagger;
import io.swagger.models.properties.Property;
import io.swagger.parser.ResolverCache;

import java.util.Map;

/**
 * Created by russellb337 on 7/15/15.
 */
public class ResponseProcessor {

    private final Swagger swagger;
    private final ResolverCache cache;
    private final PropertyProcessor propertyProcessor;

    public ResponseProcessor(ResolverCache cache, Swagger swagger) {
        this.swagger = swagger;
        this.cache = cache;
        propertyProcessor = new PropertyProcessor(cache, swagger);
    }

    public void processResponse(Response response) {
        //process the response body
        final Property schema = response.getSchema();

        if (schema != null) {
            propertyProcessor.processProperty(schema);
        }

        //process the response headers
        final Map<String, Property> headers = response.getHeaders();
        for (Map.Entry<String, Property> responseHdrEntry : headers.entrySet()) {
            final Property responseHeader = responseHdrEntry.getValue();
            propertyProcessor.processProperty(responseHeader);
        }
    }
}
