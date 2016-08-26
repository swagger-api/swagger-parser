package io.swagger.parser.processors;

import io.swagger.models.ArrayModel;
import io.swagger.models.Model;
import io.swagger.models.RefModel;
import io.swagger.models.Swagger;
import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.MapProperty;
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

        if(existingModel == null) {
            // don't overwrite existing model reference
            swagger.addDefinition(newRef, model);

            String file = $ref.split("#/")[0];
            if (model instanceof RefModel) {
                RefModel refModel = (RefModel) model;
                if (isAnExternalRefFormat(refModel.getRefFormat())) {
                    refModel.set$ref(processRefToExternalDefinition(refModel.get$ref(), refModel.getRefFormat()));
                } else {
                    processRefToExternalDefinition(file + refModel.get$ref(), RefFormat.RELATIVE);
                }
            }
            //Loop the properties and recursively call this method;
            Map<String, Property> subProps = model.getProperties();
            if (subProps != null) {
                for (Map.Entry<String, Property> prop : subProps.entrySet()) {
                    if (prop.getValue() instanceof RefProperty) {
                        processRefProperty((RefProperty) prop.getValue(), file);
                    } else if (prop.getValue() instanceof ArrayProperty) {
                        ArrayProperty arrayProp = (ArrayProperty) prop.getValue();
                        if (arrayProp.getItems() instanceof RefProperty) {
                            processRefProperty((RefProperty) arrayProp.getItems(), file);
                        }
                    } else if (prop.getValue() instanceof MapProperty) {
                        MapProperty mapProp = (MapProperty) prop.getValue();
                        if (mapProp.getAdditionalProperties() instanceof RefProperty) {
                            processRefProperty((RefProperty) mapProp.getAdditionalProperties(), file);
                        } else if (mapProp.getAdditionalProperties() instanceof ArrayProperty &&
                                ((ArrayProperty) mapProp.getAdditionalProperties()).getItems() instanceof RefProperty) {
                            processRefProperty((RefProperty) ((ArrayProperty) mapProp.getAdditionalProperties()).getItems(), file);
                        }
                    }
                }
            }
            if (model instanceof ArrayModel && ((ArrayModel) model).getItems() instanceof RefProperty) {
                processRefProperty((RefProperty) ((ArrayModel) model).getItems(), file);
            }
        }

        return newRef;
    }

    private void processRefProperty(RefProperty subRef, String externalFile) {
        if (isAnExternalRefFormat(subRef.getRefFormat())) {
            subRef.set$ref(processRefToExternalDefinition(subRef.get$ref(), subRef.getRefFormat()));
        } else {
            processRefToExternalDefinition(externalFile + subRef.get$ref(), RefFormat.RELATIVE);
        }
    }
}
