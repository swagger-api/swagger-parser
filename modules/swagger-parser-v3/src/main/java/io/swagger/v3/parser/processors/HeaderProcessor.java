package io.swagger.v3.parser.processors;


import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.ResolverCache;
import io.swagger.v3.parser.models.RefFormat;

import java.util.Map;


import static io.swagger.v3.parser.util.RefUtils.computeRefFormat;
import static io.swagger.v3.parser.util.RefUtils.isAnExternalRefFormat;


public class HeaderProcessor {

    private final ResolverCache cache;
    private final SchemaProcessor schemaProcessor;
    private final ExampleProcessor exampleProcessor;
    private final ExternalRefProcessor externalRefProcessor;
    private final OpenAPI openAPI;


    public HeaderProcessor(ResolverCache cache, OpenAPI openAPI) {
        this.cache = cache;
        this.openAPI = openAPI;
        this.schemaProcessor = new SchemaProcessor(cache,openAPI);
        this.exampleProcessor = new ExampleProcessor(cache,openAPI);
        this.externalRefProcessor = new ExternalRefProcessor(cache, openAPI);
    }

    public void processHeader(Header header) {

        if(header.get$ref() != null){
            RefFormat refFormat = computeRefFormat(header.get$ref());
            String $ref = header.get$ref();
            if (isAnExternalRefFormat(refFormat)){
                final String newRef = externalRefProcessor.processRefToExternalHeader($ref, refFormat);
                if (newRef != null) {
                    header.set$ref(newRef);
                }
            }
        }
        if (header.getSchema() != null) {
            schemaProcessor.processSchema(header.getSchema());

        }
        if (header.getExamples() != null){
            if (header.getExamples() != null) {
                Map<String,Example> examples = header.getExamples();
                for (String key : examples.keySet()){
                    exampleProcessor.processExample(header.getExamples().get(key));
                }

            }
        }
        Schema schema = null;
        if(header.getContent() != null) {
            Map<String,MediaType> content = header.getContent();
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
    }
}