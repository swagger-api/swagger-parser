package io.swagger.parser.v3.processors;

import io.swagger.oas.models.OpenAPI;
import io.swagger.oas.models.examples.Example;
import io.swagger.parser.v3.ResolverCache;
import io.swagger.parser.v3.models.RefFormat;


import java.util.ArrayList;
import java.util.List;

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

    public Example processExample(Example example) {

        if (example.get$ref() != null){
            RefFormat refFormat = computeRefFormat(example.get$ref());
            String $ref = example.get$ref();
            Example newExample = cache.loadRef($ref, refFormat, Example.class);
            if(newExample != null) {
                return newExample;
            }
            //openApi.getComponents().getExamples().replace(name,example,newExample);
        }
        return example;
    }

    public List<Example> processExample(List<Example> examples) {
        List<Example> newExampleList = new ArrayList<>();
        for(Example example: examples){
            if (example.get$ref() != null){
                RefFormat refFormat = computeRefFormat(example.get$ref());
                String ref = example.get$ref();
                Example newExample = cache.loadRef(ref, refFormat, Example.class);
                newExampleList.add(newExample);
                //openApi.getComponents().getHeaders().get(name).setExamples(newExampleList);
            }
        }
        return newExampleList;
    }
}
