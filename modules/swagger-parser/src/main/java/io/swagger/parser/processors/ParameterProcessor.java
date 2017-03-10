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
                final Parameter resolvedParameter = cache.loadRef(refParameter.get$ref(), refParameter.getRefFormat(), Parameter.class);
                parameter = resolvedParameter;
                refParameters.add(parameter);
            }

            else if (parameter instanceof BodyParameter) {
                BodyParameter bodyParameter = (BodyParameter) parameter;
                final Model schema = bodyParameter.getSchema();
                modelProcessor.processModel(schema);
            }
            else {
                processedPathLevelParameters.add(parameter);
            }

        }
        for(Parameter refParam : refParameters) {
            int pos = 0;
            for(Parameter param : processedPathLevelParameters) {
                if(refParam.getName().equals(param.getName())) {
                    // ref param wins
                    processedPathLevelParameters.set(pos, refParam);
                }
                pos++;
            }
        }
        return processedPathLevelParameters;
    }
}
