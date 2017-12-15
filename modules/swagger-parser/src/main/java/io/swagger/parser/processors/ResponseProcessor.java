package io.swagger.parser.processors;

import io.swagger.models.Model;
import io.swagger.models.Response;
import io.swagger.models.Swagger;
import io.swagger.models.properties.Property;
import io.swagger.parser.ResolverCache;

public class ResponseProcessor {

    private final ModelProcessor modelProcessor;

    public ResponseProcessor(ResolverCache cache, Swagger swagger) {
        modelProcessor = new ModelProcessor(cache, swagger);

    }

    public void processResponse(Response response) {
        //process the response body
        final Model schema = response.getResponseSchema();

        if (schema != null) {
            modelProcessor.processModel(schema);
        }

        /* intentionally ignoring the response headers, even those these were modelled as a
         Map<String, Property> they should never have a $ref because what does it mean to have a
         complex object in an HTTP header?
          */

    }
}
