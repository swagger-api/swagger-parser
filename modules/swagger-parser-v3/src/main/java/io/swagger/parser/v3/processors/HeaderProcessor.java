package io.swagger.parser.v3.processors;


import io.swagger.oas.models.examples.Example;
import io.swagger.oas.models.headers.Header;
import io.swagger.oas.models.media.MediaType;
import io.swagger.oas.models.media.Schema;
import io.swagger.oas.models.OpenAPI;
import io.swagger.parser.v3.ResolverCache;
import io.swagger.parser.v3.models.RefFormat;


import java.util.List;
import java.util.Map;


import static io.swagger.parser.v3.util.RefUtils.computeRefFormat;


public class HeaderProcessor {

    private final ResolverCache cache;
    private final SchemaProcessor schemaProcessor;
    private final ExampleProcessor exampleProcessor;
    private final OpenAPI openApi;


    public HeaderProcessor(ResolverCache cache, OpenAPI openApi) {
        this.cache = cache;
        this.openApi = openApi;
        this.schemaProcessor = new SchemaProcessor(cache);
        this.exampleProcessor = new ExampleProcessor(cache,openApi);
    }

    public Header processHeader(Header header) {

        if(header.get$ref() != null){
            RefFormat refFormat = computeRefFormat(header.get$ref());
            String $ref = header.get$ref();
            header = cache.loadRef($ref, refFormat, Header.class);
            if(header != null){
                return header;
            }

        }
        if (header.getSchema() != null) {
            Schema resolved = schemaProcessor.processSchema(header.getSchema());
            header.setSchema(resolved);

        }
        if (header.getExamples() != null){
            List<Example> resolvedExamples = exampleProcessor.processExample(header.getExamples());
            header.setExamples(resolvedExamples);

        }
        Schema schema = null;
        MediaType resolvedMedia = null;
        if(header.getContent() != null) {
            Map<String,MediaType> content = header.getContent();
            for( String mediaName : content.keySet()) {
                MediaType mediaType = content.get(mediaName);
                if(mediaType.getSchema()!= null) {
                    schema = mediaType.getSchema();
                    resolvedMedia = new MediaType();
                    if (schema != null) {
                        if(schema.get$ref() != null) {
                            Schema resolved = schemaProcessor.processReferenceSchema(schema);
                            resolvedMedia.setSchema(resolved);
                            header.getContent().replace(mediaName,mediaType,resolvedMedia);
                        }else {
                            Schema resolved = schemaProcessor.processSchema(schema);
                            resolvedMedia.setSchema(resolved);
                            header.getContent().replace(mediaName,mediaType,resolvedMedia);
                        }
                    }
                }
            }
        }

        return  header;
    }
}