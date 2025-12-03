package io.swagger.v3.parser.processors;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.parser.ResolverCache;
import io.swagger.v3.parser.models.RefFormat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.swagger.v3.parser.util.RefUtils.computeRefFormat;
import static io.swagger.v3.parser.util.RefUtils.isAnExternalRefFormat;


public class ParameterProcessor {

    private final ResolverCache cache;
    private final SchemaProcessor schemaProcessor;
    private final ExampleProcessor exampleProcessor;
    private final OpenAPI openAPI;
    private final ExternalRefProcessor externalRefProcessor;

    public ParameterProcessor(ResolverCache cache, OpenAPI openAPI) {
        this(cache, openAPI, false);
    }

    public ParameterProcessor(ResolverCache cache, OpenAPI openAPI, boolean openapi31) {
        this.cache = cache;
        this.openAPI = openAPI;
        this.schemaProcessor = new SchemaProcessor(cache, openAPI, openapi31);
        this.exampleProcessor = new ExampleProcessor(cache, openAPI);
        this.externalRefProcessor = new ExternalRefProcessor(cache, openAPI);
    }

    public void processParameter(Parameter parameter) {
        processRefToExternalParameter(parameter, parameter.get$ref());
        if (parameter.getSchema() != null) {
            schemaProcessor.processSchema(parameter.getSchema());
        }
        processParameterExamples(parameter);
        processParameterMediaTypeSchema(parameter);
    }

    private void processRefToExternalParameter(Parameter parameter, String referencePath) {
        if (referencePath != null) {
            RefFormat refFormat = computeRefFormat(parameter.get$ref());
            if (isAnExternalRefFormat(refFormat)) {
                String newRef = externalRefProcessor.processRefToExternalParameter(referencePath, refFormat);
                if (newRef != null) {
                    newRef = "#/components/parameters/" + newRef;
                    parameter.set$ref(newRef);
                }
            }
        }
    }

    private void processParameterExamples(Parameter parameter) {
        Map<String, Example> examples = parameter.getExamples();
        if (examples != null) {
            examples.values().forEach(exampleProcessor::processExample);
        }
    }

    private void processParameterMediaTypeSchema(Parameter parameter) {
        if (parameter.getContent() != null) {
            Map<String, MediaType> content = parameter.getContent();
            for (String mediaName : content.keySet()) {
                MediaType mediaType = content.get(mediaName);
                if (mediaType.getSchema() != null) {
                    Optional<Schema> schema = Optional.ofNullable(mediaType.getSchema());
                    schema.ifPresent(schemaProcessor::processSchema);
                }
            }
        }
    }

    public List<Parameter> processParameters(List<Parameter> parameters) {
        if (parameters == null) {
            return null;
        }
        final List<Parameter> refParameters = new ArrayList<>();

        final List<Parameter> processedPathLevelParameters = processPathLevelParams(parameters, refParameters);

        for (Parameter resolvedParameter : refParameters) {
            int pos = 0;
            for (Parameter param : processedPathLevelParameters) {
                if (param.getName().equals(resolvedParameter.getName())) {
                    // ref param wins
                    processedPathLevelParameters.set(pos, resolvedParameter);
                    break;
                }
                pos++;
            }

        }
        processedPathLevelParameters.forEach(parameter -> {
            if (parameter.getSchema() != null) {
                schemaProcessor.processSchema(parameter.getSchema());
            } else if (parameter.getContent() != null) {
                Map<String, MediaType> content = parameter.getContent();
                content.values().forEach(mediaType -> {
                    if (mediaType.getSchema() != null) {
                        schemaProcessor.processSchema(mediaType.getSchema());
                    }
                    if (mediaType.getExamples() != null) {
                        mediaType.getExamples().values().forEach(exampleProcessor::processExample);
                    }
                });

            }
        });

        return processedPathLevelParameters;
    }

    private List<Parameter> processPathLevelParams(List<Parameter> parameters, List<Parameter> refParameters) {
        final List<Parameter> processedPathLevelParameters = new ArrayList<>();
        for (Parameter parameter : parameters) {
            if (parameter.get$ref() != null) {
                RefFormat refFormat = computeRefFormat(parameter.get$ref());
                final Parameter resolvedParameter = cache.loadRef(parameter.get$ref(), refFormat, Parameter.class);
                if (parameter.get$ref().startsWith("#") && !parameter.get$ref().contains("#/components/parameters")) {
                    //TODO: Not possible to add warning during resolve doesn't accept result as an input. Hence commented below line.
                    //result.warning(location, "The parameter should use Reference Object to link to parameters that are defined at the OpenAPI Object's components/parameters.");
                    continue;
                }
                if (resolvedParameter == null) {
                    // can't resolve it!
                    processedPathLevelParameters.add(parameter);
                    continue;
                }
                boolean matched = isParameterExist(parameters, processedPathLevelParameters, resolvedParameter);

                if (matched) {
                    refParameters.add(resolvedParameter);
                } else {
                    processedPathLevelParameters.add(resolvedParameter);
                }
            } else {
                processedPathLevelParameters.add(parameter);
            }
        }
        return processedPathLevelParameters;
    }

    private static boolean isParameterExist(List<Parameter> parameters, List<Parameter> processedPathLevelParameters, Parameter resolvedParameter) {
        // verify if the parameter exists, if yes, then replace it when name and location are the same
        boolean matched = processedPathLevelParameters.stream()
                .anyMatch(param -> param != null
                        && param.getName() != null
                        && param.getIn() != null
                        && param.getName().equals(resolvedParameter.getName())
                        && param.getIn().equals(resolvedParameter.getIn()));

        if (!matched) {
            matched = parameters.stream()
                    .anyMatch(param -> param != null
                            && param.getName() != null
                            && param.getIn() != null
                            && param.getName().equals(resolvedParameter.getName())
                            && param.getIn().equals(resolvedParameter.getIn()));
        }
        return matched;
    }
}
