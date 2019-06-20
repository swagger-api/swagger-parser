package io.swagger.parser.processors;

import io.swagger.models.Model;
import io.swagger.models.RefResponse;
import io.swagger.models.Response;
import io.swagger.models.Swagger;
import io.swagger.models.properties.Property;
import io.swagger.models.refs.RefFormat;
import io.swagger.parser.ResolverCache;
import io.swagger.parser.util.RefUtils;

public class ResponseProcessor {

    private final ModelProcessor modelProcessor;
    private final ExternalRefProcessor externalRefProcessor;

    public ResponseProcessor(ResolverCache cache, Swagger swagger) {
        modelProcessor = new ModelProcessor(cache, swagger);
        externalRefProcessor = new ExternalRefProcessor(cache, swagger);

    }

    public void processResponse(Response response) {
        //process the response body
        final Model schema = response.getResponseSchema();

        if (response instanceof RefResponse) {
            RefResponse refResponse = (RefResponse) response;
            processReferenceResponse(refResponse);
        }


        if (schema != null) {
            modelProcessor.processModel(schema);
        }

        /* intentionally ignoring the response headers, even those these were modelled as a
         Map<String, Property> they should never have a $ref because what does it mean to have a
         complex object in an HTTP header?
          */

    }

    public void processReferenceResponse(RefResponse refResponse){
        RefFormat refFormat = refResponse.getRefFormat();
        String $ref = refResponse.get$ref();
        if (RefUtils.isAnExternalRefFormat(refFormat)){
            externalRefProcessor.processRefToExternalResponse($ref, refFormat);
        }
    }
}
