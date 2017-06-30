package io.swagger.parser.v3.processors;

import io.swagger.oas.models.OpenAPI;
import io.swagger.oas.models.media.MediaType;
import io.swagger.oas.models.media.Schema;
import io.swagger.oas.models.parameters.RequestBody;
import io.swagger.parser.v3.ResolverCache;
import io.swagger.parser.v3.models.RefFormat;

import java.util.Map;

import static io.swagger.parser.v3.util.RefUtils.computeRefFormat;

/**
 * Created by gracekarina on 20/06/17.
 */
public class RequestBodyProcessor {
    private final SchemaProcessor schemaProcessor;
    private final ResolverCache cache;
    private final OpenAPI openApi;

    public RequestBodyProcessor(ResolverCache cache, OpenAPI openApi) {
        schemaProcessor = new SchemaProcessor(cache);
        this.cache = cache;
        this.openApi = openApi;
    }

    public RequestBody processRequestBody(RequestBody requestBody) {

        if (requestBody.get$ref() != null){
            requestBody = processReferenceRequestBody(requestBody);
        }
        Schema schema = null;
        MediaType resolvedMedia = null;
        if(requestBody.getContent() != null){
            Map<String,MediaType> content = requestBody.getContent();
            for( String mediaName : content.keySet()) {
                MediaType mediaType = content.get(mediaName);
                if(mediaType.getSchema()!= null) {
                    schema = mediaType.getSchema();
                    resolvedMedia = new MediaType();
                    if (schema != null) {
                        if(schema.get$ref() != null) {
                            Schema resolved = schemaProcessor.processReferenceSchema(schema);
                            resolvedMedia.setSchema(resolved);
                            requestBody.getContent().replace(mediaName,mediaType,resolvedMedia);
                        }else {
                            Schema resolved = schemaProcessor.processSchema(schema);
                            resolvedMedia.setSchema(resolved);
                            requestBody.getContent().replace(mediaName,mediaType,resolvedMedia);
                        }
                    }
                }
            }
        }
        return requestBody;
    }


    public RequestBody processReferenceRequestBody(RequestBody requestBody){
        RefFormat refFormat = computeRefFormat(requestBody.get$ref());
        String $ref = requestBody.get$ref();
        RequestBody newRequestBody = cache.loadRef($ref, refFormat, RequestBody.class);
        return newRequestBody;
    }

}
