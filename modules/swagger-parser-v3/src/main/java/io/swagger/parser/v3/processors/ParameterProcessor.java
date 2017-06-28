package io.swagger.parser.v3.processors;


import io.swagger.oas.models.examples.Example;
import io.swagger.oas.models.media.MediaType;
import io.swagger.oas.models.media.Schema;
import io.swagger.oas.models.parameters.Parameter;
import io.swagger.oas.models.OpenAPI;
import io.swagger.parser.v3.ResolverCache;
import io.swagger.parser.v3.models.RefFormat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.swagger.parser.v3.util.RefUtils.computeRefFormat;


public class ParameterProcessor {

    private final ResolverCache cache;
    private final SchemaProcessor schemaProcessor;
    private final ExampleProcessor exampleProcessor;
    private final OpenAPI openApi;


    public ParameterProcessor(ResolverCache cache, OpenAPI openApi) {
        this.cache = cache;
        this.openApi = openApi;
        this.schemaProcessor = new SchemaProcessor(cache);
        this.exampleProcessor = new ExampleProcessor(cache,openApi);
    }

    public Parameter processParameter(Parameter parameter) {
        String $ref = parameter.get$ref();
        if($ref != null){
            RefFormat refFormat = computeRefFormat($ref);
            parameter = cache.loadRef($ref, refFormat, Parameter.class);

        }
        if (parameter.getSchema() != null){
            Schema resolved = schemaProcessor.processSchema(parameter.getSchema());
            parameter.setSchema(resolved);

        }
        if (parameter.getExamples() != null){
            Map <String, Example> examples = parameter.getExamples();
            for(String exampleName: examples.keySet()){
                final Example example = examples.get(exampleName);
                Example resolvedExample = exampleProcessor.processExample(example);
                examples.replace(exampleName,example,resolvedExample);
                parameter.setExamples(examples);
            }
        }
        Schema schema = null;
        MediaType resolvedMedia = null;
        if(parameter.getContent() != null) {
            Map<String,MediaType> content = parameter.getContent();
            for( String mediaName : content.keySet()) {
                MediaType mediaType = content.get(mediaName);
                if(mediaType.getSchema()!= null) {
                    schema = mediaType.getSchema();
                    resolvedMedia = new MediaType();
                    if (schema != null) {
                        if(schema.get$ref() != null) {
                            Schema resolved = schemaProcessor.processReferenceSchema(schema);
                            resolvedMedia.setSchema(resolved);
                            parameter.getContent().replace(mediaName,mediaType,resolvedMedia);
                        }else {
                            Schema resolved = schemaProcessor.processSchema(schema);
                            resolvedMedia.setSchema(resolved);
                            parameter.getContent().replace(mediaName,mediaType,resolvedMedia);
                        }
                    }
                }
            }
        }
        return parameter;
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
                processedPathLevelParameters.add(resolvedParameter);
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

        return processedPathLevelParameters;
    }
}