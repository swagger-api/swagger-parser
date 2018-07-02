package io.swagger.v3.parser.processors;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.parser.ResolverCache;
import io.swagger.v3.parser.models.RefFormat;

import java.util.Map;

import static io.swagger.v3.parser.util.RefUtils.*;

/**
 * Created by gracekarina on 20/06/17.
 */
public class RequestBodyProcessor {
    private final SchemaProcessor schemaProcessor;
    private final ExternalRefProcessor externalRefProcessor;
    private final ResolverCache cache;
    private final OpenAPI openAPI;

    public RequestBodyProcessor(ResolverCache cache, OpenAPI openAPI) {
        schemaProcessor = new SchemaProcessor(cache,openAPI);
        this.externalRefProcessor = new ExternalRefProcessor(cache, openAPI);
        this.cache = cache;
        this.openAPI = openAPI;
    }

    public RequestBody processRequestBody(RequestBody refRequestBody) {
        RequestBody requestBody = refRequestBody.get$ref() != null ? processReferenceRequestBody(refRequestBody) : refRequestBody;
        Schema schema = null;
        if(requestBody.getContent() != null){
            Map<String,MediaType> content = requestBody.getContent();
            for( String mediaName : content.keySet()) {
                MediaType mediaType = content.get(mediaName);
                if(mediaType.getSchema()!= null) {
                    schema = mediaType.getSchema();
                    if (schema != null) {
                        schemaProcessor.processSchema(schema);
                    }
                }
            }
        }
        return requestBody;
    }


    public RequestBody processReferenceRequestBody(RequestBody requestBody){
        RefFormat refFormat = computeRefFormat(requestBody.get$ref());
        String $ref = requestBody.get$ref();
        if (isAnExternalRefFormat(refFormat)){
            final String newRef = externalRefProcessor.processRefToExternalRequestBody($ref, refFormat);

            if (newRef != null) {
                requestBody.set$ref(newRef);
            }
        } else if (openAPI.getComponents() != null) {
            Map<String, RequestBody> requestBodies = openAPI.getComponents().getRequestBodies();
            if (requestBodies != null) {
                String referenceKey = computeDefinitionName($ref);
                return requestBodies.getOrDefault(referenceKey, requestBody);
            }
        }
        return requestBody;
    }

}
