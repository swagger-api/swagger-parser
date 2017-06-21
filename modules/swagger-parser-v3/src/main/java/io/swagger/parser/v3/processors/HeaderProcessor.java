package io.swagger.parser.v3.processors;


import io.swagger.oas.models.headers.Header;
import io.swagger.oas.models.media.Schema;
import io.swagger.oas.models.OpenAPI;
import io.swagger.parser.v3.ResolverCache;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class HeaderProcessor {

    private final ResolverCache cache;
    private final SchemaProcessor schemaProcessor;
    private final OpenAPI openApi;


    public HeaderProcessor(ResolverCache cache, OpenAPI openApi) {
        this.cache = cache;
        this.openApi = openApi;
        this.schemaProcessor = new SchemaProcessor(cache);
    }

    public void processHeader(String name, Header header) {
        /*if(header.get$ref() != null){

        }*/
        if (header.getSchema() != null) {
            Schema resolved = schemaProcessor.processSchema(header.getSchema());
            //TODO what if the header is not in components?
            openApi.getComponents().getHeaders().get(name).setSchema(resolved);
        }
    }

   /* public List<Parameter> processParameters(List<Parameter> parameters) {

        if (parameters == null) {
            return null;
        }



        final List<Parameter> processedPathLevelParameters = new ArrayList<>();
        final List<Parameter> refParameters = new ArrayList<>();

        for (Parameter parameter : parameters) {
            if (parameter instanceof RefParameter) {
                //RefParameter refParameter = (RefParameter) parameter;
                //final Parameter resolvedParameter = cache.loadRef(refParameter.get$ref(), refParameter.getRefFormat(), Parameter.class);

                if(resolvedParameter == null) {
                    // can't resolve it!
                    processedPathLevelParameters.add(refParameter);
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
                if (resolvedParameter instanceof RequestBody) {
                    RequestBody body = (RequestBody) resolvedParameter;
                    final Model schema = body.getSchema();
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
                if (parameter instanceof RequestBody) {
                    RequestBody body = (RequestBody) parameter;
                    final Model schema = body.getSchema();
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

        return null;//processedPathLevelParameters;
    }*/
}