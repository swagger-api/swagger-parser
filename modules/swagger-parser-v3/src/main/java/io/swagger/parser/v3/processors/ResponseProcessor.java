package io.swagger.parser.v3.processors;

import io.swagger.oas.models.media.MediaType;
import io.swagger.oas.models.media.Schema;
import io.swagger.oas.models.responses.ApiResponse;
import io.swagger.oas.models.OpenAPI;
import io.swagger.parser.v3.ResolverCache;
import io.swagger.parser.v3.models.RefFormat;

import javax.naming.spi.Resolver;
import java.util.Map;

import static io.swagger.parser.v3.util.RefUtils.computeRefFormat;

public class ResponseProcessor {

    private final SchemaProcessor schemaProcessor;
    private final ResolverCache cache;
    private final OpenAPI openApi;

    public ResponseProcessor(ResolverCache cache, OpenAPI openApi) {
        schemaProcessor = new SchemaProcessor(cache);
        this.cache = cache;
        this.openApi = openApi;
    }

    public void processResponse(String name,ApiResponse response) {

        if (response.get$ref() != null){
            ApiResponse apiResponse = processReferenceResponse(response);
            openApi.getComponents().getResponses().replace(name,response,apiResponse);
        }
        Schema schema = null;
        if(response.getContent() != null){
            Map<String,MediaType> content = response.getContent();
            for( Map.Entry<String, MediaType> map : content.entrySet()) {
                if(map.getValue().getSchema()!= null) {
                    MediaType mediaType = map.getValue();
                    schema = mediaType.getSchema();
                    if (schema != null) {
                        if(schema.get$ref() != null) {
                            Schema resolved = schemaProcessor.processReferenceSchema(schema);
                            mediaType.setSchema(resolved);
                        }
                    }
                }
            }
        }
    }
    public ApiResponse processReferenceResponse(ApiResponse response){
        RefFormat refFormat = computeRefFormat(response.get$ref());
        String $ref = response.get$ref();
        ApiResponse newResponse = cache.loadRef($ref, refFormat, ApiResponse.class);
        return newResponse;
    }
}