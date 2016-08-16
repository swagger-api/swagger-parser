package io.swagger.parser.processors;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.models.Model;
import io.swagger.models.RefModel;
import io.swagger.models.Swagger;
import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.RefProperty;
import io.swagger.models.refs.GenericRef;
import io.swagger.models.refs.RefFormat;
import io.swagger.models.refs.RefType;
import io.swagger.parser.ResolverCache;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
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
                } else if (prop.getValue() instanceof ArrayProperty) {
                    ArrayProperty arrayProp = (ArrayProperty) prop.getValue();
                    if (arrayProp.getItems() instanceof RefProperty) {
                        RefProperty subRef = (RefProperty) arrayProp.getItems();

                        if (isAnExternalRefFormat(subRef.getRefFormat())) {
                            subRef.set$ref(processRefToExternalDefinition(subRef.get$ref(), subRef.getRefFormat()));
                        } else {
                            processRefToExternalDefinition(file + subRef.get$ref(), RefFormat.RELATIVE);
                        }
                    }
                }
            }
            processRefsFromVendorExtensions(model, file);
            addXPointer(model, $ref);
        }
        return newRef;
    }

    private void addXPointer(Model model, String externalRef) {
        Map<String, Object> vendorExtensions = model.getVendorExtensions();
        if (vendorExtensions != null && !vendorExtensions.getClass().getName().equals("java.util.Collections$EmptyMap")) {
            vendorExtensions.put("x-pointer", externalRef);
        }
    }

    public void processRefsFromVendorExtensions(Model model, String externalFile) {
        Map<String, Object> vendorExtensions = model.getVendorExtensions();
        if (vendorExtensions != null) {
            if (vendorExtensions.containsKey("x-collection")) {
                ObjectNode xCollection = (ObjectNode) vendorExtensions.get("x-collection");
                if (xCollection.has("schema")) {
                    String sub$ref = xCollection.get("schema").asText();
                    GenericRef subRef = new GenericRef(RefType.DEFINITION, sub$ref);
                    if (isAnExternalRefFormat(subRef.getFormat())) {
                        xCollection.put("schema", "#/definitions/" + processRefToExternalDefinition(subRef.getRef(), subRef.getFormat()));
                    } else if (externalFile != null) {
                        processRefToExternalDefinition(externalFile + subRef.getRef(), RefFormat.RELATIVE);
                    }
                }
            }
            if (vendorExtensions.containsKey("x-links")) {
                ObjectNode xLinks = (ObjectNode) vendorExtensions.get("x-links");
                Iterator<String> xLinksNames = xLinks.fieldNames();
                while (xLinksNames.hasNext()) {
                    String linkName = xLinksNames.next();
                    if (xLinks.get(linkName) instanceof ObjectNode) {
                        ObjectNode xLink = (ObjectNode) xLinks.get(linkName);
                        if (xLink.has("schema")) {
                            String sub$ref = xLink.get("schema").asText();
                            GenericRef subRef = new GenericRef(RefType.DEFINITION, sub$ref);
                            if (isAnExternalRefFormat(subRef.getFormat())) {
                                xLink.put("schema", "#/definitions/" + processRefToExternalDefinition(subRef.getRef(), subRef.getFormat()));
                            } else if (externalFile != null) {
                                processRefToExternalDefinition(externalFile + subRef.getRef(), RefFormat.RELATIVE);
                            }
                        }
                    }
                }
            }
        }
    }
}
