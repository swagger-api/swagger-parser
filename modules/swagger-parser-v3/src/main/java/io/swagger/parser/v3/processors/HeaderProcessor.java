package io.swagger.parser.v3.processors;


import io.swagger.oas.models.examples.Example;
import io.swagger.oas.models.headers.Header;
import io.swagger.oas.models.media.Schema;
import io.swagger.oas.models.OpenAPI;
import io.swagger.parser.v3.ResolverCache;
import io.swagger.parser.v3.models.RefFormat;


import java.util.List;


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
            Header resolved = cache.loadRef($ref, refFormat, Header.class);
            if (resolved != null) {
                return resolved;
            }

        }
        if (header.getSchema() != null) {
            Schema resolved = schemaProcessor.processSchema(header.getSchema());
            header.setSchema(resolved);
            return header;
        }
        if (header.getExamples() != null){
            List<Example> resolvedExamples = exampleProcessor.processExample(header.getExamples());
            header.setExamples(resolvedExamples);

            return header;
        }

        return  header;
    }
}