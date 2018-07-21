package io.swagger.v3.parser.processors;


import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.ResolverCache;
import io.swagger.v3.parser.models.RefFormat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.swagger.v3.parser.util.RefUtils.computeRefFormat;
import static io.swagger.v3.parser.util.RefUtils.isAnExternalRefFormat;


public class ParameterProcessor {

    private final ResolverCache cache;
    private final SchemaProcessor schemaProcessor;
    private final ExampleProcessor exampleProcessor;
    private final OpenAPI openAPI;
    private final ExternalRefProcessor externalRefProcessor;


    public ParameterProcessor(ResolverCache cache, OpenAPI openAPI) {
        this.cache = cache;
        this.openAPI = openAPI;
        this.schemaProcessor = new SchemaProcessor(cache,openAPI);
        this.exampleProcessor = new ExampleProcessor(cache,openAPI);
        this.externalRefProcessor = new ExternalRefProcessor(cache, openAPI);
    }

    public void processParameter(Parameter parameter) {
        String $ref = parameter.get$ref();
        if($ref != null){
            RefFormat refFormat = computeRefFormat(parameter.get$ref());
            if (isAnExternalRefFormat(refFormat)){
                final String newRef = externalRefProcessor.processRefToExternalParameter($ref, refFormat);
                if (newRef != null) {
                    parameter.set$ref(newRef);
                }
            }
        }
        if (parameter.getSchema() != null){
            schemaProcessor.processSchema(parameter.getSchema());
        }
        if (parameter.getExamples() != null){
            Map <String, Example> examples = parameter.getExamples();
            for(String exampleName: examples.keySet()){
                final Example example = examples.get(exampleName);
                exampleProcessor.processExample(example);
            }
        }
        Schema schema = null;
        if(parameter.getContent() != null) {
            Map<String,MediaType> content = parameter.getContent();
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

   public List<Parameter> processParameters(List<Parameter> parameters) {

        if (parameters == null) {
            return null;
        }

        final List<Parameter> processedPathLevelParameters = new ArrayList<>();
        final List<Parameter> refParameters = new ArrayList<>();

        for (Parameter parameter : parameters) {
            if (parameter.get$ref() != null) {
                RefFormat refFormat = computeRefFormat(parameter.get$ref());
                final Parameter resolvedParameter = cache.loadRef(parameter.get$ref(), refFormat, Parameter.class);

                if(resolvedParameter == null) {
                    // can't resolve it!
                    processedPathLevelParameters.add(parameter);
                    continue;
                }
                // if the parameter exists, replace it
                boolean matched = false;
                for(Parameter param : processedPathLevelParameters) {
                    if(param.getName().equals(resolvedParameter.getName())) {
                        // ref param wins
                        matched = true;
                        break;
                    }
                }
                for(Parameter param : parameters) {
                    if(param.getName() != null) {
                        if (param.getName().equals(resolvedParameter.getName())) {
                            // ref param wins
                            matched = true;
                            break;
                        }
                    }
                }

                if(matched) {
                    refParameters.add(resolvedParameter);
                }
                else {
                    processedPathLevelParameters.add(resolvedParameter);
                }
            }
            else {
                processedPathLevelParameters.add(parameter);
            }
        }

        for(Parameter resolvedParameter : refParameters) {
            int pos = 0;
            for(Parameter param : processedPathLevelParameters) {
                if(param.getName().equals(resolvedParameter.getName())) {
                    // ref param wins
                    processedPathLevelParameters.set(pos, resolvedParameter);
                    break;
                }
                pos++;
            }

        }

        for (Parameter parameter : processedPathLevelParameters) {
            Schema schema = parameter.getSchema();
            if(schema != null){
                schemaProcessor.processSchema(schema);
            }
        }

        return processedPathLevelParameters;
    }
}