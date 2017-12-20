package io.swagger.parser.processors;

import io.swagger.models.*;
import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.MapProperty;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.RefProperty;
import io.swagger.models.refs.RefFormat;
import io.swagger.parser.ResolverCache;
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

        final String possiblyConflictingDefinitionName = computeDefinitionName($ref, definitions.keySet());

        Model existingModel = definitions.get(possiblyConflictingDefinitionName);

        if (existingModel != null) {
            LOGGER.debug("A model for " + existingModel + " already exists");
            if(existingModel instanceof RefModel) {
                // use the new model
                existingModel = null;
            }
        }
        newRef = possiblyConflictingDefinitionName;
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
                    processRefToExternalDefinition(file + refModel.get$ref(), RefFormat.RELATIVE);
                }

            } else if (model instanceof ComposedModel){
                
                ComposedModel composedModel = (ComposedModel) model;
                List<Model> listOfAllOF = composedModel.getAllOf();

                for (Model allOfModel: listOfAllOF){
                    if (allOfModel instanceof RefModel) {
                        RefModel refModel = (RefModel) allOfModel;
                        if (isAnExternalRefFormat(refModel.getRefFormat())) {
                            refModel.set$ref(processRefToExternalDefinition(refModel.get$ref(), refModel.getRefFormat()));
                        } else {
                            processRefToExternalDefinition(file + refModel.get$ref(), RefFormat.RELATIVE);
                        }
                    }
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

    private void processRefProperty(RefProperty subRef, String externalFile) {
        if (isAnExternalRefFormat(subRef.getRefFormat())) {
            String $ref = constructRef(subRef, externalFile);
            subRef.set$ref($ref);
            if($ref.startsWith("."))
                processRefToExternalDefinition($ref, RefFormat.RELATIVE);
            else {
                processRefToExternalDefinition($ref, RefFormat.URL);
            }

        } else {
            processRefToExternalDefinition(externalFile + subRef.get$ref(), RefFormat.RELATIVE);
        }
    }

    protected String constructRef(RefProperty refProperty, String rootLocation) {
        String ref = refProperty.get$ref();
        return join(rootLocation, ref);
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
