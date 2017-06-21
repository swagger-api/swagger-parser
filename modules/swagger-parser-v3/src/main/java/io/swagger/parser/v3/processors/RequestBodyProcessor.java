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

    public void processRequestBody(String name,RequestBody requestBody) {

        /*TODO Tony if (requestBody.get$ref() != null){
            ApiResponse apiResponse = processReferenceRequestBody(requestBody);
            openApi.getComponents().getResponses().replace(name,requestBody,apiResponse);
        }*/
        Schema schema = null;
        if(requestBody.getContent() != null){
            Map<String,MediaType> content = requestBody.getContent();
            for( Map.Entry<String, MediaType> map : content.entrySet()) {
                if(map.getValue().getSchema()!= null) {
                    MediaType mediaType = map.getValue();
                    schema = mediaType.getSchema();
                    if (schema != null) {
                        if(schema.get$ref() != null) {
                            Schema resolved = schemaProcessor.processReferenceSchema(schema);
                            mediaType.setSchema(resolved);
                        }else {
                            schemaProcessor.processSchema(schema);
                        }
                    }
                }
            }
        }
    }
    /* TODO Tony public RequestBody processReferenceRequestBody(RequestBody requestBody){
        RefFormat refFormat = computeRefFormat(requestBody.get$ref());
        String $ref = requestBody.get$ref();
        RequestBody newRequestBody = cache.loadRef($ref, refFormat, RequestBody.class);
        return newRequestBody;
    }*/

}
