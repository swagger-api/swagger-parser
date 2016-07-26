package io.swagger.parser.processors;

import io.swagger.models.Model;
import io.swagger.models.RefModel;
import io.swagger.models.Swagger;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.RefProperty;
import io.swagger.models.refs.GenericRef;
import io.swagger.models.refs.RefFormat;
import io.swagger.models.refs.RefType;
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
                    RefProperty subRef = (RefProperty) prop.getValue();

                    if (isAnExternalRefFormat(subRef.getRefFormat())) {
                        subRef.set$ref(processRefToExternalDefinition(subRef.get$ref(), subRef.getRefFormat()));
                    } else {
                        processRefToExternalDefinition(file + subRef.get$ref(), RefFormat.RELATIVE);
                    }
                }
            }
        }
        Map<String, Object> vendorExtensions = model.getVendorExtensions();
        if (vendorExtensions != null) {
            if (vendorExtensions.containsKey("x-collection")) {
                String sub$ref = (String) ((Map<String, Object>) vendorExtensions.get("x-collection")).get("schema");
                GenericRef subRef = new GenericRef(RefType.DEFINITION, sub$ref);
                if (isAnExternalRefFormat(subRef.getFormat())) {
                    processRefToExternalDefinition(subRef.getRef(), subRef.getFormat());
                    // FIXME: needs to change link
                } else {
                    processRefToExternalDefinition(file + subRef.getRef(), RefFormat.RELATIVE);
                }
            }
            if (vendorExtensions.containsKey("x-links")) {
                Map<String, Object> xLinks = (Map<String, Object>) vendorExtensions.get("x-links");
                for (Map.Entry<String, Object> xLink : xLinks.entrySet()) {
                    String sub$ref = (String) ((Map<String, Object>) xLink.getValue()).get("schema");
                    GenericRef subRef = new GenericRef(RefType.DEFINITION, sub$ref);
                    if (isAnExternalRefFormat(subRef.getFormat())) {
                        processRefToExternalDefinition(subRef.getRef(), subRef.getFormat());
                        // FIXME: needs to change link
                    } else {
                        processRefToExternalDefinition(file + subRef.getRef(), RefFormat.RELATIVE);
                    }
                }
            }
        }
        if(existingModel == null) {
            // don't overwrite existing model reference
            if (vendorExtensions != null)
                vendorExtensions.put("x-pointer", $ref);
            swagger.addDefinition(newRef, model);
        }

        return newRef;
    }
}
