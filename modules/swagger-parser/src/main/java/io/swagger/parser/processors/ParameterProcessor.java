package io.swagger.parser.processors;

import io.swagger.models.Model;
import io.swagger.models.Swagger;
import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.parameters.RefParameter;
import io.swagger.parser.ResolverCache;

import java.util.ArrayList;
import java.util.List;


public class ParameterProcessor {

    private final ResolverCache cache;
    private final ModelProcessor modelProcessor;


    public ParameterProcessor(ResolverCache cache, Swagger swagger) {
        this.cache = cache;
        this.modelProcessor = new ModelProcessor(cache, swagger);
    }

    public List<Parameter> processParameters(List<Parameter> parameters) {

        if (parameters == null) {
            return null;
        }

        final List<Parameter> processedPathLevelParameters = new ArrayList<>();
        final List<Parameter> refParameters = new ArrayList<>();

        for (Parameter parameter : parameters) {
            if (parameter instanceof RefParameter) {
                RefParameter refParameter = (RefParameter) parameter;
                 Parameter resolvedParameter = cache.loadRef(refParameter.get$ref(), refParameter.getRefFormat(), Parameter.class);

                if(resolvedParameter == null) {
                    // can't resolve it!
                    processedPathLevelParameters.add(refParameter);
                    continue;
                }
                // if the parameter exists, replace it
                boolean matched = false;
                for(Parameter param : processedPathLevelParameters) {
                    if (param.getName() != null) {
                        if (param.getName().equals(resolvedParameter.getName())) {
                            // ref param wins
                            matched = true;
                            break;
                        }
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
                if (resolvedParameter instanceof BodyParameter) {
                    BodyParameter bodyParameter = (BodyParameter) resolvedParameter;
                    final Model schema = bodyParameter.getSchema();
                    modelProcessor.processModel(schema);
                }
                if(matched) {
                    refParameters.add(resolvedParameter);
                }
                else {
                    processedPathLevelParameters.add(resolvedParameter);
                }
            }
            else {
                if (parameter instanceof BodyParameter) {
                    BodyParameter bodyParameter = (BodyParameter) parameter;
                    final Model schema = bodyParameter.getSchema();
                    modelProcessor.processModel(schema);
                }
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
