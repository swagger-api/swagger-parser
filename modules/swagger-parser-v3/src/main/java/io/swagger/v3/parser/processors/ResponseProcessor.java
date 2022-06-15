package io.swagger.v3.parser.processors;

import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.links.Link;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.ResolverCache;
import io.swagger.v3.parser.models.RefFormat;

import java.util.Map;

import static io.swagger.v3.parser.util.RefUtils.computeRefFormat;
import static io.swagger.v3.parser.util.RefUtils.isAnExternalRefFormat;

public class ResponseProcessor {

    private final SchemaProcessor schemaProcessor;
    private final HeaderProcessor headerProcessor;
    private final LinkProcessor linkProcessor;
    private final ExternalRefProcessor externalRefProcessor;
    private final ExampleProcessor exampleProcessor;
    private final ResolverCache cache;
    private final OpenAPI openAPI;

    public ResponseProcessor(ResolverCache cache, OpenAPI openAPI) {
        this(cache, openAPI, false);
    }

    public ResponseProcessor(ResolverCache cache, OpenAPI openAPI, boolean openapi31) {
        schemaProcessor = new SchemaProcessor(cache,openAPI, openapi31);
        headerProcessor = new HeaderProcessor(cache,openAPI, openapi31);
        linkProcessor = new LinkProcessor(cache,openAPI, openapi31);
        exampleProcessor = new ExampleProcessor(cache,openAPI);
        this.externalRefProcessor = new ExternalRefProcessor(cache, openAPI);
        this.cache = cache;
        this.openAPI = openAPI;
    }

    public void processResponse(ApiResponse response) {

        if (response.get$ref() != null){
            processReferenceResponse(response);
        }

        Schema schema = null;
        if(response.getContent() != null){
            Map<String,MediaType> content = response.getContent();
            for( String mediaName : content.keySet()) {
                MediaType mediaType = content.get(mediaName);
                if(mediaType.getSchema()!= null) {
                    schema = mediaType.getSchema();
                    if (schema != null) {
                        schemaProcessor.processSchema(schema);
                    }
                }
                if(mediaType.getExamples() != null) {
                    for(Example ex: mediaType.getExamples().values()){
                        exampleProcessor.processExample(ex);
                    }
                }
            }
        }
        if (response.getHeaders() != null){
            Map<String,Header> headers = response.getHeaders();
            for(String headerName : headers.keySet()){
                Header header = headers.get(headerName);
                headerProcessor.processHeader(header);
            }


        }
        if (response.getLinks() != null){
            Map<String,Link> links = response.getLinks();
            for(String linkName : links.keySet()){
                Link link = links.get(linkName);
                linkProcessor.processLink(link);
            }
        }
    }

    public void processReferenceResponse(ApiResponse response){
        RefFormat refFormat = computeRefFormat(response.get$ref());
        String $ref = response.get$ref();
        if (isAnExternalRefFormat(refFormat)){
            final String newRef =  externalRefProcessor.processRefToExternalResponse($ref, refFormat);

            if (newRef != null) {
                response.set$ref(newRef);
            }
        }
    }
}
