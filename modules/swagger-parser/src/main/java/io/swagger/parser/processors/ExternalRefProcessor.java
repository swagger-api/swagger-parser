package io.swagger.parser.processors;

import io.swagger.models.*;
import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.MapProperty;
import io.swagger.models.properties.ObjectProperty;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.RefProperty;
import io.swagger.models.refs.RefFormat;
import io.swagger.models.refs.RefType;
import io.swagger.parser.ResolverCache;
import io.swagger.parser.util.RefUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
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
        String renamedRef = cache.getRenamedRef($ref);

        if(renamedRef != null) {
            return renamedRef;
        }

        final Model model = cache.loadRef($ref, refFormat, Model.class);

        if(model == null) {
            // stop!  There's a problem.  retain the original ref
            LOGGER.warn("unable to load model reference from `" + $ref + "`.  It may not be available " +
                    "or the reference isn't a valid model schema");
            return $ref;
        }
        String newRef;

        Map<String, Model> definitions = swagger.getDefinitions();

        if (definitions == null) {
            definitions = new LinkedHashMap<>();
        }

        final String possiblyConflictingDefinitionName = computeDefinitionName($ref);

        String tryName = null;
        Model existingModel = definitions.get(possiblyConflictingDefinitionName);

        if (existingModel != null) {
            LOGGER.debug("A model for " + existingModel + " already exists");
            if(existingModel instanceof RefModel) {
                // use the new model
                existingModel = null;
            }else{
                //We add a number at the end of the definition name
                int i = 2;
                for (String name : definitions.keySet()) {
                    if (name.equals(possiblyConflictingDefinitionName)) {
                        tryName = possiblyConflictingDefinitionName + "_" + i;
                        existingModel = definitions.get(tryName);
                        i++;
                    }
                }
            }
        }
        if (StringUtils.isNotBlank(tryName)){
            newRef = tryName;
        }else{
            newRef = possiblyConflictingDefinitionName;
        }
        cache.putRenamedRef($ref, newRef);
        if(existingModel == null) {
            // don't overwrite existing model reference
            swagger.addDefinition(newRef, model);
            cache.addReferencedKey(newRef);

            String file = $ref.split("#/")[0];
            if (model instanceof RefModel) {
                RefModel refModel = (RefModel) model;
                if (isAnExternalRefFormat(refModel.getRefFormat())) {
                    refModel.set$ref(processRefToExternalDefinition(refModel.get$ref(), refModel.getRefFormat()));
                } else {
                    refModel.set$ref(processRefToExternalDefinition(file + refModel.get$ref(), RefFormat.RELATIVE));
                }

            }
            if (model instanceof ComposedModel){
                
                ComposedModel composedModel = (ComposedModel) model;
                List<Model> listOfAllOF = composedModel.getAllOf();

                for (Model allOfModel: listOfAllOF){
                    if (allOfModel instanceof RefModel) {
                        RefModel refModel = (RefModel) allOfModel;
                        if (isAnExternalRefFormat(refModel.getRefFormat())) {
                            String joinedRef = join(file, refModel.get$ref());
                            refModel.set$ref(processRefToExternalDefinition(joinedRef, refModel.getRefFormat()));
                        }/*else if (isAnExternalRefFormat(refModel.getOriginalRefFormat())) {
                            String joinedRef = join(file, refModel.getOriginalRef());
                            refModel.set$ref(processRefToExternalDefinition(joinedRef, refModel.getOriginalRefFormat()));
                        }*/else {
                            processRefToExternalDefinition(file + refModel.get$ref(), RefFormat.RELATIVE);
                        }
                    } else if (allOfModel instanceof ModelImpl) {
                        //Loop through additional properties of allOf and recursively call this method;
                        processProperties(allOfModel.getProperties(), file);
                    }
                }
            }
            //Loop the properties and recursively call this method;
            processProperties(model.getProperties(), file);

            if (model instanceof  ModelImpl) {
                ModelImpl modelImpl = (ModelImpl) model;
                Property additionalProperties = modelImpl.getAdditionalProperties();
                if (additionalProperties != null) {
                    if (additionalProperties instanceof RefProperty) {
                        processRefProperty(((RefProperty) additionalProperties), file);
                    } else if (additionalProperties instanceof ArrayProperty) {
                        ArrayProperty arrayProp = (ArrayProperty) additionalProperties;
                        if (arrayProp.getItems() instanceof RefProperty) {
                            processRefProperty((RefProperty) arrayProp.getItems(), file);
                        }
                    } else if (additionalProperties instanceof MapProperty) {
                        MapProperty mapProp = (MapProperty) additionalProperties;
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


    public String processRefToExternalResponse(String $ref, RefFormat refFormat) {

        String renamedRef = cache.getRenamedRef($ref);
        if(renamedRef != null) {
            return renamedRef;
        }
        final Response response = cache.loadRef($ref, refFormat, Response.class);

        String newRef;


        Map<String, Response> responses = swagger.getResponses();

        if (responses == null) {
            responses = new LinkedHashMap<>();
        }

        final String possiblyConflictingDefinitionName = computeDefinitionName($ref);

        Response existingResponse = responses.get(possiblyConflictingDefinitionName);

        if (existingResponse != null) {
            LOGGER.debug("A model for " + existingResponse + " already exists");
            if(existingResponse instanceof RefResponse) {
                // use the new model
                existingResponse = null;
            }
        }
        newRef = possiblyConflictingDefinitionName;
        cache.putRenamedRef($ref, newRef);

        if(response != null) {

            String file = $ref.split("#/")[0];

            Model model = null;
            if (response.getResponseSchema() != null) {
                model = response.getResponseSchema();
                if (model instanceof RefModel) {
                    RefModel refModel = (RefModel) model;
                    if (RefUtils.isAnExternalRefFormat(refFormat)) {
                        processRefModel(refModel, $ref);
                    } else {
                        processRefToExternalDefinition(file + refModel.get$ref(), RefFormat.RELATIVE);
                    }
                }
            }
        }
        return newRef;
    }

    private void processProperties(final Map<String, Property> subProps, final String file) {
        if (subProps == null || 0 == subProps.entrySet().size() ) {
            return;
        }
        for (Map.Entry<String, Property> prop : subProps.entrySet()) {
            if (prop.getValue() instanceof RefProperty) {
                processRefProperty((RefProperty) prop.getValue(), file);
            } else if (prop.getValue() instanceof ArrayProperty) {
                ArrayProperty arrayProp = (ArrayProperty) prop.getValue();
                if (arrayProp.getItems() instanceof RefProperty) {
                    processRefProperty((RefProperty) arrayProp.getItems(), file);
                }
                if (arrayProp.getItems() != null){
                    if (arrayProp.getItems() instanceof  ObjectProperty) {
                        ObjectProperty objectProperty = (ObjectProperty) arrayProp.getItems();
                        processProperties(objectProperty.getProperties(), file);
                    }
                }
            } else if (prop.getValue() instanceof MapProperty) {
                MapProperty mapProp = (MapProperty) prop.getValue();
                if (mapProp.getAdditionalProperties() instanceof RefProperty) {
                    processRefProperty((RefProperty) mapProp.getAdditionalProperties(), file);
                } else if (mapProp.getAdditionalProperties() instanceof ArrayProperty &&
                        ((ArrayProperty) mapProp.getAdditionalProperties()).getItems() instanceof RefProperty) {
                    processRefProperty((RefProperty) ((ArrayProperty) mapProp.getAdditionalProperties()).getItems(),
                            file);
                }
            }
        }
    }

    private void processRefProperty(RefProperty subRef, String externalFile) {

        if (isAnExternalRefFormat(subRef.getRefFormat())) {
            String joinedRef = join(externalFile, subRef.get$ref());
            subRef.set$ref(processRefToExternalDefinition(joinedRef, subRef.getRefFormat()));
        } else {
            String processRef = processRefToExternalDefinition(externalFile + subRef.get$ref(), RefFormat.RELATIVE);
            subRef.set$ref(RefType.DEFINITION.getInternalPrefix()+processRef);
        }
    }

    private void processRefModel(RefModel subRef, String externalFile) {

        if (isAnExternalRefFormat(subRef.getRefFormat())) {
            String joinedRef = join(externalFile, subRef.get$ref());
            subRef.set$ref(processRefToExternalDefinition(joinedRef, subRef.getRefFormat()));
        } else {
            processRefToExternalDefinition(externalFile + subRef.get$ref(), RefFormat.RELATIVE);
        }
    }
    

    public static String join(String source, String fragment) {
        try {
            boolean isRelative = false;
            if(source.startsWith("/") || source.startsWith(".")) {
                isRelative = true;
            }
            URI uri = new URI(source);

            if(!source.endsWith("/") && (fragment.startsWith("./") && "".equals(uri.getPath()))) {
                uri = new URI(source + "/");
            }
            else if("".equals(uri.getPath()) && !fragment.startsWith("/")) {
                uri = new URI(source + "/");
            }
            URI f = new URI(fragment);

            URI resolved = uri.resolve(f);

            URI normalized = resolved.normalize();

            if(Character.isAlphabetic(normalized.toString().charAt(0)) && isRelative) {
                return "./" + normalized.toString();
            }
            return normalized.toString();
        }
        catch(Exception e) {
            return source;
        }
    }
}
