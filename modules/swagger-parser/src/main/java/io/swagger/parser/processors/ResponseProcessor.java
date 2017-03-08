package io.swagger.parser.processors;

import java.util.Map;

import io.swagger.models.RefResponse;
import io.swagger.models.Response;
import io.swagger.models.Swagger;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.RefProperty;
import io.swagger.parser.ResolverCache;

public class ResponseProcessor {

    private final PropertyProcessor propertyProcessor;
    private final ResolverCache cache;

    public ResponseProcessor(ResolverCache cache, Swagger swagger) {
        this.cache = cache;
        propertyProcessor = new PropertyProcessor(cache, swagger);
    }

    public void processResponse(Response response) {
        // process the response body
        final Property schema = response.getSchema();

        if (schema != null) {
            propertyProcessor.processProperty(schema);
        }

        /*
         * intentionally ignoring the response headers, even those these were
         * modelled as a Map<String, Property> they should never have a $ref
         * because what does it mean to have a complex object in an HTTP header?
         * 
         * @tompahoward: Having $ref in headers doesn't mean they are complex
         * objects, it means that we don't want to repeat the definition for
         * each header in every response. We should be able to define a header
         * once and $ref refer to it everywhere we ant to use it.
         */

        final Map<String, Property> headers = response.getHeaders();
        if (headers != null) {
            for (String headerName : headers.keySet()) {
                Property header = headers.get(headerName);

                if (header != null) {
                    if (header instanceof RefProperty) {
                        RefProperty refHeader = (RefProperty) header;
                        Property resolvedHeader = cache.loadRef(refHeader.get$ref(), refHeader.getRefFormat(),
                                Property.class);

                        if (resolvedHeader != null) {
                            header = resolvedHeader;
                            headers.put(headerName, resolvedHeader);
                        }
                    }
                    propertyProcessor.processProperty(header);
                }
            }
        }
    }
}
