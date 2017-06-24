package io.swagger.parser.v3.processors;

import io.swagger.oas.models.OpenAPI;
import io.swagger.oas.models.examples.Example;
import io.swagger.parser.v3.ResolverCache;
import io.swagger.parser.v3.models.RefFormat;


import static io.swagger.parser.v3.util.RefUtils.computeRefFormat;

/**
 * Created by gracekarina on 23/06/17.
 */
public class ExampleProcessor {
    private final ResolverCache cache;
    private final OpenAPI openApi;

    public ExampleProcessor(ResolverCache cache, OpenAPI openApi) {
        this.cache = cache;
        this.openApi = openApi;
    }

    public void processExample(String name, Example example) {

        if (example.get$ref() != null){
            RefFormat refFormat = computeRefFormat(example.get$ref());
            String $ref = example.get$ref();
            Example newExample = cache.loadRef($ref, refFormat, Example.class);
            //TODO what if the example is not in components?
            openApi.getComponents().getExamples().replace(name,example,newExample);
        }
    }
}
