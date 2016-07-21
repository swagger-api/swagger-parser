package io.swagger.parser.processors;

import io.swagger.models.ArrayModel;
import io.swagger.models.Model;
import io.swagger.models.Swagger;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.RefProperty;
import io.swagger.models.refs.RefFormat;
import io.swagger.parser.ResolverCache;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static io.swagger.parser.util.RefUtils.computeDefinitionName;
import static io.swagger.parser.util.RefUtils.isAnExternalRefFormat;

public final class ExternalRefProcessor {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ExternalRefProcessor.class);

    private final ResolverCache cache;
    private final Swagger swagger;

    public ExternalRefProcessor(ResolverCache cache, Swagger swagger) {
        this.cache = cache;
        this.swagger = swagger;
    }

    public String processRefToExternalDefinition(String $ref, RefFormat refFormat) {
        final Model model = cache.loadRef($ref, refFormat, Model.class);

        String newRef;

        Map<String, Model> definitions = swagger.getDefinitions();

        if (definitions == null) {
            definitions = new HashMap<>();
        }

        final String possiblyConflictingDefinitionName = computeDefinitionName($ref);

        final Model existingModel = definitions.get(possiblyConflictingDefinitionName);

        if (existingModel != null) {
            LOGGER.debug("A model for " + existingModel + " already exists");
        }
        newRef = possiblyConflictingDefinitionName;
        cache.putRenamedRef($ref, newRef);


        //If this is a new model, then check it for other sub references
        //Loop the properties and recursively call this method;
        Map<String, Property> subProps = model.getProperties();
        if(subProps != null) {
            for (Map.Entry<String, Property> prop : subProps.entrySet()) {
                if (prop.getValue() instanceof RefProperty) {
                    RefProperty subRef = (RefProperty) prop.getValue();

                    if(isAnExternalRefFormat(subRef.getRefFormat()))
                        subRef.set$ref(processRefToExternalDefinition(subRef.get$ref(), subRef.getRefFormat()));
                }
            }
        } else if (model instanceof ArrayModel && ((ArrayModel)model).getItems() instanceof RefProperty) {
            RefProperty subRef = (RefProperty) ((ArrayModel)model).getItems();
            if(isAnExternalRefFormat(subRef.getRefFormat()))
                subRef.set$ref(processRefToExternalDefinition(subRef.get$ref(), subRef.getRefFormat()));
        }
        if(existingModel == null) {
            // don't overwrite existing model reference
            swagger.addDefinition(newRef, model);
        }

        return newRef;
    }
}
