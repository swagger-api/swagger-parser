package io.swagger.parser.v3.processors;

import io.swagger.oas.models.headers.Header;
import io.swagger.oas.models.links.Link;
import io.swagger.oas.models.media.MediaType;
import io.swagger.oas.models.media.Schema;
import io.swagger.oas.models.responses.ApiResponse;
import io.swagger.oas.models.OpenAPI;
import io.swagger.parser.v3.ResolverCache;
import io.swagger.parser.v3.models.RefFormat;

import java.util.Map;

import static io.swagger.parser.v3.util.RefUtils.computeRefFormat;

public class ResponseProcessor {

    private final SchemaProcessor schemaProcessor;
    private final HeaderProcessor headerProcessor;
    private final LinkProcessor linkProcessor;
    private final ResolverCache cache;
    private final OpenAPI openApi;

    public ResponseProcessor(ResolverCache cache, OpenAPI openApi) {
        schemaProcessor = new SchemaProcessor(cache);
        headerProcessor = new HeaderProcessor(cache,openApi);
        linkProcessor = new LinkProcessor(cache,openApi);
        this.cache = cache;
        this.openApi = openApi;
    }

    public ApiResponse processResponse(ApiResponse response) {

        if (response.get$ref() != null){
            response = processReferenceResponse(response);
        }
        Schema schema = null;
        MediaType resolvedMedia = null;
        if(response.getContent() != null){
            Map<String,MediaType> content = response.getContent();
            for( String mediaName : content.keySet()) {
                MediaType mediaType = content.get(mediaName);
                if(mediaType.getSchema()!= null) {
                    schema = mediaType.getSchema();
                    resolvedMedia = new MediaType();
                    if (schema != null) {
                        if(schema.get$ref() != null) {
                            Schema resolved = schemaProcessor.processReferenceSchema(schema);
                            resolvedMedia.setSchema(resolved);
                            response.getContent().replace(mediaName,mediaType,resolvedMedia);
                        }else {
                            Schema resolved = schemaProcessor.processSchema(schema);
                            resolvedMedia.setSchema(resolved);
                            response.getContent().replace(mediaName,mediaType,resolvedMedia);
                        }
                    }
                }
            }
        }
        if (response.getHeaders() != null){
            Map<String,Header> headers = response.getHeaders();
            for(String headerName : headers.keySet()){
                Header header = headers.get(headerName);
                Header resolvedHeader = headerProcessor.processHeader(header);
                headers.replace(headerName,header,resolvedHeader);
            }
            response.setHeaders(headers);

        }
        if (response.getLinks() != null){
            Map<String,Link> links = response.getLinks();
            for(String linkName : links.keySet()){
                Link link = links.get(linkName);
                Link resolvedLink = linkProcessor.processLink(link);
                links.replace(linkName,link,resolvedLink);
            }
            response.setLinks(links);
        }

        return response;
    }

    public ApiResponse processReferenceResponse(ApiResponse response){
        RefFormat refFormat = computeRefFormat(response.get$ref());
        String $ref = response.get$ref();
        ApiResponse newResponse = cache.loadRef($ref, refFormat, ApiResponse.class);
        return newResponse;
    }
}