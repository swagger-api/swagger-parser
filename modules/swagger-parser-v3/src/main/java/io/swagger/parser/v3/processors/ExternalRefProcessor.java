package io.swagger.parser.v3.processors;

import io.swagger.oas.models.OpenAPI;
import io.swagger.oas.models.media.ArraySchema;
import io.swagger.oas.models.media.Schema;
import io.swagger.parser.v3.ResolverCache;
import io.swagger.parser.v3.models.RefFormat;
//import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import static io.swagger.parser.v3.util.RefUtils.computeRefFormat;
import static io.swagger.parser.v3.util.RefUtils.computeDefinitionName;
import static io.swagger.parser.v3.util.RefUtils.isAnExternalRefFormat;


public final class ExternalRefProcessor {
    //private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ExternalRefProcessor.class);

    private final ResolverCache cache;
    private final OpenAPI openApi;

    public ExternalRefProcessor(ResolverCache cache, OpenAPI openApi) {
        this.cache = cache;
        this.openApi = openApi;
    }

    public String processRefToExternalDefinition(String $ref, RefFormat refFormat) {
        //Devuelve el objeto completo
        final Schema schema = cache.loadRef($ref, refFormat, Schema.class);

        if(schema == null) {
            // stop!  There's a problem.  retain the original ref
            /*LOGGER.warn("unable to load model reference from `" + $ref + "`.  It may not be available " +
                    "or the reference isn't a valid model schema");*/
            System.out.println("unable to load model reference from `" + $ref + "`.  It may not be available " +
                    "or the reference isn't a valid model schema");
            return $ref;
        }
        String newRef;

        Map<String, Schema> components = openApi.getComponents().getSchemas();

        if (components == null) {
            components = new LinkedHashMap<>();
        }

        final String possiblyConflictingDefinitionName = computeDefinitionName($ref);

        Schema existingSchema = components.get(possiblyConflictingDefinitionName);

        if (existingSchema != null) {
            //LOGGER.debug("A schema for " + existingSchema + " already exists");
            System.out.println("A schema for " + existingSchema + " already exists");
            if(existingSchema.get$ref()!= null) {
                // use the new model
                existingSchema = null;
            }
        }
        newRef = possiblyConflictingDefinitionName;
        cache.putRenamedRef($ref, newRef);

        if(existingSchema == null) {
            // don't overwrite existing model reference
            openApi.getComponents().getSchemas().put(newRef, schema);
            cache.addReferencedKey(newRef);

            String file = $ref.split("#/")[0];
            if (schema.get$ref() != null) {

                if (isAnExternalRefFormat(computeRefFormat(schema.get$ref()))) {
                    schema.set$ref(processRefToExternalDefinition(schema.get$ref(), computeRefFormat(schema.get$ref())));
                } else {
                    processRefToExternalDefinition(file + schema.get$ref(), RefFormat.RELATIVE);
                }
            }
            //Loop the properties and recursively call this method;
            Map<String, Schema> subProps = schema.getProperties();
            if (subProps != null) {
                for (Map.Entry<String, Schema> prop : subProps.entrySet()) {
                    if (prop.getValue().get$ref() != null) {
                        processRefProperty(prop.getValue(), file);
                    } else if (prop.getValue() instanceof ArraySchema) {
                        ArraySchema arrayProp = (ArraySchema) prop.getValue();
                        if (arrayProp.getItems().get$ref() != null) {
                            processRefProperty(arrayProp.getItems(), file);
                        }
                    } else if (prop.getValue() instanceof Schema) {
                        Schema mapProp =  prop.getValue();
                        if (mapProp.getAdditionalProperties().get$ref() != null) {
                            processRefProperty(mapProp.getAdditionalProperties(), file);
                        } else if (mapProp.getAdditionalProperties() instanceof ArraySchema &&
                                ((ArraySchema) mapProp.getAdditionalProperties()).getItems().get$ref() != null) {
                            processRefProperty(((ArraySchema) mapProp.getAdditionalProperties()).getItems(), file);
                        }
                    }
                }
            }
            if (schema instanceof ArraySchema && ((ArraySchema) schema).getItems().get$ref() != null) {
                processRefProperty(((ArraySchema) schema).getItems(), file);
            }
        }

        return newRef;
    }

    public void processRefProperty(Schema schema, String externalFile) {
        if (isAnExternalRefFormat(computeRefFormat(schema.get$ref()))) {
            String $ref = constructRef(schema, externalFile);
            schema.set$ref($ref);
            if($ref.startsWith("."))
                processRefToExternalDefinition($ref, RefFormat.RELATIVE);
            else {
                processRefToExternalDefinition($ref, RefFormat.URL);
            }

        } else {
            processRefToExternalDefinition(externalFile + schema.get$ref(), RefFormat.RELATIVE);
        }
    }

    protected String constructRef(Schema refProperty, String rootLocation) {
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
