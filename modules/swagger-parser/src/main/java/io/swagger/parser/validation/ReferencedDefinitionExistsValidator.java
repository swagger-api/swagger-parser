package io.swagger.parser.validation;

import com.google.common.collect.Sets;
import io.swagger.models.Model;
import io.swagger.models.ModelImpl;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.Response;
import io.swagger.models.Swagger;
import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.RefProperty;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ReferencedDefinitionExistsValidator implements SpecValidator {
    @Override
    public List<String> validate(Swagger swagger) {
        List<String> errors = new ArrayList<>();
        Set<String> definitionRefs = getDefinitionsReferencedInPaths(swagger);

        Set<String> producedDefinitions = getDefinitionsDefinedInSwagger(swagger);
        Sets.SetView<String> difference = Sets.difference(definitionRefs, producedDefinitions);

        for (String modelWhichWasNotFound : difference) {
            errors.add("Could not find a referenced definition named " + modelWhichWasNotFound);
        }
        return errors;
    }

    private Set<String> getDefinitionsDefinedInSwagger(Swagger swagger) {
        if (swagger.getDefinitions() == null) {
            return new HashSet<>();
        }
        return swagger.getDefinitions().keySet();
    }

    private Set<String> getDefinitionsReferencedInPaths(Swagger swagger) {
        Set<String> definitionRefs = new HashSet<>();
        if (swagger.getPaths() != null) {
            for (Path path : swagger.getPaths().values()) {
                for (Operation operation : path.getOperations()) {
                    List<String> definitions = collectDefinitions(operation);
                    for (String definition : definitions) {
                        definitionRefs.add(definition.replace("#/definitions/", ""));
                    }
                }
            }
        }
        return definitionRefs;
    }

    private List<String> collectDefinitions(Operation operation) {
        List<String> definitions = new ArrayList<>();
        for (BodyParameter bodyParameter : getBodyParameters(operation)) {
            if (bodyParameter.getSchema().getReference() != null) {
                definitions.add(bodyParameter.getSchema().getReference());
            }
        }
        if (operation.getResponses() != null) {
            for (Response response : operation.getResponses().values()) {
                String refFromResponse = getRefFromResponse(response);
                if (refFromResponse != null) {
                    definitions.add(refFromResponse);
                }
            }
        }
        return definitions;
    }

    private String getRefFromResponse(Response response) {
        if (response.getSchema() instanceof RefProperty) {
            return ((RefProperty) response.getSchema()).get$ref();
        } else if (response.getSchema() instanceof ArrayProperty) {
            Property items = ((ArrayProperty) response.getSchema()).getItems();
            return ((RefProperty) items).get$ref();
        }
        return null;
    }

    private List<BodyParameter> getBodyParameters(Operation operation) {
        List<BodyParameter> bodyParameters = new ArrayList<>();
        for (Parameter parameter : operation.getParameters()) {
            if (parameter instanceof BodyParameter) {
                bodyParameters.add((BodyParameter) parameter);
            }
        }
        return bodyParameters;
    }
}
