package io.swagger.parser.v3.processors;


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
    private final OpenAPI openApi;


    public ParameterProcessor(ResolverCache cache, OpenAPI openApi) {
        this.cache = cache;
        this.openApi = openApi;
        this.schemaProcessor = new SchemaProcessor(cache);
    }

    public void processParameter(String name, Parameter parameter) {
        String $ref = parameter.get$ref();
        if($ref != null){
            RefFormat refFormat = computeRefFormat($ref);
            Parameter refParameter = cache.loadRef($ref, refFormat, Parameter.class);
            //TODO what if the example is not in components?
            openApi.getComponents().getParameters().replace(name,parameter,refParameter);
        }
        if (parameter.getSchema() != null){
         Schema resolved = schemaProcessor.processSchema(parameter.getSchema());
         //TODO what if the parameter is not in components?
         openApi.getComponents().getParameters().get(name).setSchema(resolved);
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