package io.swagger.parser.processors;

import io.swagger.models.Response;
import io.swagger.models.Swagger;
import io.swagger.models.properties.Property;
import io.swagger.parser.ResolverCache;

public class ResponseProcessor {

    private final PropertyProcessor propertyProcessor;

    public ResponseProcessor(ResolverCache cache, Swagger swagger) {
        propertyProcessor = new PropertyProcessor(cache, swagger);
    }

    public void processResponse(Response response) {
        //process the response body
        final Property schema = response.getSchema();

        if (schema != null) {
            propertyProcessor.processProperty(schema);
        }

        /* intentionally ignoring the response headers, even those these were modelled as a
         Map<String, Property> they should never have a $ref because what does it mean to have a
         complex object in an HTTP header?
          */

    }
}
