package io.swagger.v3.parser.processors;


import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.callbacks.Callback;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.links.Link;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.parser.ResolverCache;
import io.swagger.v3.parser.models.RefFormat;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

import static io.swagger.v3.parser.util.RefUtils.computeDefinitionName;
import static io.swagger.v3.parser.util.RefUtils.computeRefFormat;
import static io.swagger.v3.parser.util.RefUtils.isAnExternalRefFormat;

public final class ExternalRefProcessor {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ExternalRefProcessor.class);

    private final ResolverCache cache;
    private final OpenAPI openAPI;

    public ExternalRefProcessor(ResolverCache cache, OpenAPI openAPI) {
        this.cache = cache;
        this.openAPI = openAPI;
    }

    public String processRefToExternalSchema(String $ref, RefFormat refFormat) {
        String renamedRef = cache.getRenamedRef($ref);
        if(renamedRef != null) {
            return renamedRef;
        }

        final Schema schema = cache.loadRef($ref, refFormat, Schema.class);

        if(schema == null) {
            // stop!  There's a problem.  retain the original ref
            LOGGER.warn("unable to load model reference from `" + $ref + "`.  It may not be available " +
                    "or the reference isn't a valid model schema");
            return $ref;
        }
        String newRef;

        if (openAPI.getComponents() == null) {
            openAPI.setComponents(new Components());
        }
        Map<String, Schema> schemas = openAPI.getComponents().getSchemas();

        if (schemas == null) {
            schemas = new LinkedHashMap<>();
        }

        final String possiblyConflictingDefinitionName = computeDefinitionName($ref);
        String tryName = null;
        Schema existingModel = schemas.get(possiblyConflictingDefinitionName);

        if (existingModel != null) {
            LOGGER.debug("A model for " + existingModel + " already exists");
            if(existingModel.get$ref() != null) {
                // use the new model
                existingModel = null;
            }else{
                //We add a number at the end of the definition name
                int i = 2;
                for (String name : schemas.keySet()) {
                    if (name.equals(possiblyConflictingDefinitionName)) {
                        tryName = possiblyConflictingDefinitionName + "_" + i;
                        existingModel = schemas.get(tryName);
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
            openAPI.getComponents().addSchemas(newRef, schema);
            cache.addReferencedKey(newRef);

            String file = $ref.split("#/")[0];
            if (schema.get$ref() != null) {
                RefFormat ref = computeRefFormat(schema.get$ref());
                if (isAnExternalRefFormat(ref)) {
                    schema.set$ref(processRefToExternalSchema(schema.get$ref(), ref));
                } else {
                    processRefToExternalSchema(file + schema.get$ref(), RefFormat.RELATIVE);
                }
            }

            if(schema instanceof ComposedSchema){
                ComposedSchema composedSchema = (ComposedSchema) schema;
                if (composedSchema.getAllOf() != null){
                    for(Schema item : composedSchema.getAllOf()){
                        if (item.get$ref() != null){
                            processRefSchema(item,file);
                        } else if (item.getProperties() != null) {
                            processProperties(item.getProperties(), file);
                        }
                    }

                }if (composedSchema.getOneOf() != null){
                    for(Schema item : composedSchema.getOneOf()){
                        if (item.get$ref() != null){
                            if (item.get$ref() != null){
                                processRefSchema(item,file);
                            }
                        }
                    }
                }if (composedSchema.getAnyOf() != null){
                    for(Schema item : composedSchema.getAnyOf()){
                        if (item.get$ref() != null){
                            if (item.get$ref() != null){
                                processRefSchema(item,file);
                            }
                        }
                    }

                }
            }
            //Loop the properties and recursively call this method;
            Map<String, Schema> subProps = schema.getProperties();

            processProperties(subProps,file);

            if(schema.getAdditionalProperties() != null && schema.getAdditionalProperties() instanceof Schema){
                Schema additionalProperty = (Schema) schema.getAdditionalProperties();
                if (additionalProperty.get$ref() != null) {
                    processRefSchema(additionalProperty, file);
                } else if (additionalProperty instanceof ArraySchema) {
                    ArraySchema arrayProp = (ArraySchema) additionalProperty;
                    if (arrayProp.getItems() != null && arrayProp.getItems().get$ref() != null &&
                            StringUtils.isNotBlank(arrayProp.get$ref())) {
                        processRefSchema(arrayProp.getItems(), file);
                    }
                } else if (additionalProperty.getAdditionalProperties() != null && additionalProperty.getAdditionalProperties() instanceof Schema) {
                    Schema mapProp =  (Schema) additionalProperty.getAdditionalProperties();
                    if (mapProp.get$ref() != null) {
                        processRefSchema(mapProp, file);
                    } else if (mapProp.getAdditionalProperties() instanceof ArraySchema &&
                                ((ArraySchema) mapProp).getItems() != null &&
                                    ((ArraySchema) mapProp).getItems().get$ref() != null
                                    && StringUtils.isNotBlank(((ArraySchema) mapProp).getItems().get$ref()))  {
                        processRefSchema(((ArraySchema) mapProp).getItems(), file);
                    }
                }

            }
            if (schema instanceof ArraySchema && ((ArraySchema) schema).getItems() != null && ((ArraySchema) schema).getItems().get$ref() != null
                    && StringUtils.isNotBlank(((ArraySchema) schema).getItems().get$ref())) {
                processRefSchema(((ArraySchema) schema).getItems(), file);
            }
        }

        return newRef;
    }

    private void processProperties(Map<String,Schema> subProps, String file) {
        if (subProps != null) {
            for (Map.Entry<String, Schema> prop : subProps.entrySet()) {
                if (prop.getValue().get$ref() != null) {
                    processRefSchema(prop.getValue(), file);
                } else if (prop.getValue() instanceof ArraySchema) {
                    ArraySchema arrayProp = (ArraySchema) prop.getValue();
                    if (arrayProp.getItems() != null && arrayProp.getItems().get$ref() != null &&
                            StringUtils.isNotBlank(arrayProp.getItems().get$ref())) {
                        processRefSchema(arrayProp.getItems(), file);
                    }
                } else if (prop.getValue().getAdditionalProperties() != null && prop.getValue().getAdditionalProperties() instanceof Schema) {
                    Schema mapProp =  (Schema) prop.getValue().getAdditionalProperties();
                    if (mapProp.get$ref() != null) {
                        processRefSchema(mapProp, file);
                    } else if (mapProp.getAdditionalProperties() instanceof ArraySchema &&
                            ((ArraySchema) mapProp.getAdditionalProperties()).getItems()!= null &&
                            ((ArraySchema) mapProp.getAdditionalProperties()).getItems().get$ref() != null
                            && StringUtils.isNotBlank(((ArraySchema) mapProp.getAdditionalProperties()).getItems().get$ref())) {
                        processRefSchema(((ArraySchema) mapProp.getAdditionalProperties()).getItems(), file);
                    }
                }
            }
        }
    }

    public void processRefToExternalResponse(String $ref, RefFormat refFormat) {

        final ApiResponse response = cache.loadRef($ref, refFormat, ApiResponse.class);

        if(response != null) {

        String file = $ref.split("#/")[0];

            Schema schema = null;
            if(response.getContent() != null){
                Map<String, MediaType> content = response.getContent();
                for( String mediaName : content.keySet()) {
                    MediaType mediaType = content.get(mediaName);
                    if(mediaType.getSchema()!= null) {
                        schema = mediaType.getSchema();
                        if (schema.get$ref() != null) {
                            RefFormat ref = computeRefFormat(schema.get$ref());
                            if (isAnExternalRefFormat(ref)) {
                               processRefSchema(schema, $ref);
                            } else {
                                processRefToExternalSchema(file + schema.get$ref(), RefFormat.RELATIVE);
                            }
                        }
                    }
                }
            }
        }
    }

    public String processRefToExternalRequestBody(String $ref, RefFormat refFormat) {
        String renamedRef = cache.getRenamedRef($ref);
        if(renamedRef != null) {
            return renamedRef;
        }

        final RequestBody body = cache.loadRef($ref, refFormat, RequestBody.class);

        if(body == null) {
            // stop!  There's a problem.  retain the original ref
            LOGGER.warn("unable to load model reference from `" + $ref + "`.  It may not be available " +
                    "or the reference isn't a valid model schema");
            return $ref;
        }
        String newRef;

        if (openAPI.getComponents() == null) {
            openAPI.setComponents(new Components());
        }
        Map<String, RequestBody> bodies = openAPI.getComponents().getRequestBodies();

        if (bodies == null) {
            bodies = new LinkedHashMap<>();
        }

        final String possiblyConflictingDefinitionName = computeDefinitionName($ref);

        RequestBody existingBody= bodies.get(possiblyConflictingDefinitionName);

        if (existingBody != null) {
            LOGGER.debug("A model for " + existingBody + " already exists");
            if(existingBody.get$ref() != null) {
                // use the new model
                existingBody = null;
            }
        }
        newRef = possiblyConflictingDefinitionName;
        cache.putRenamedRef($ref, newRef);

        if(existingBody == null) {
            // don't overwrite existing model reference
            openAPI.getComponents().addRequestBodies(newRef, body);
            cache.addReferencedKey(newRef);

            String file = $ref.split("#/")[0];
            if (body.get$ref() != null) {
                RefFormat format = computeRefFormat(body.get$ref());
                if (isAnExternalRefFormat(format)) {
                    body.set$ref(processRefToExternalRequestBody(body.get$ref(), format));
                } else {
                    processRefToExternalRequestBody(file + body.get$ref(), RefFormat.RELATIVE);
                }
            }
        }

        return newRef;
    }

    public String processRefToExternalHeader(String $ref, RefFormat refFormat) {
        String renamedRef = cache.getRenamedRef($ref);
        if(renamedRef != null) {
            return renamedRef;
        }

        final Header header = cache.loadRef($ref, refFormat, Header.class);

        if(header == null) {
            // stop!  There's a problem.  retain the original ref
            LOGGER.warn("unable to load model reference from `" + $ref + "`.  It may not be available " +
                    "or the reference isn't a valid model schema");
            return $ref;
        }
        String newRef;

        if (openAPI.getComponents() == null) {
            openAPI.setComponents(new Components());
        }
        Map<String, Header> headers = openAPI.getComponents().getHeaders();

        if (headers == null) {
            headers = new LinkedHashMap<>();
        }

        final String possiblyConflictingDefinitionName = computeDefinitionName($ref);

        Header existingHeader = headers.get(possiblyConflictingDefinitionName);

        if (existingHeader != null) {
            LOGGER.debug("A model for " + existingHeader + " already exists");
            if(existingHeader.get$ref() != null) {
                // use the new model
                existingHeader = null;
            }
        }
        newRef = possiblyConflictingDefinitionName;
        cache.putRenamedRef($ref, newRef);

        if(existingHeader == null) {
            // don't overwrite existing model reference
            openAPI.getComponents().addHeaders(newRef, header);
            cache.addReferencedKey(newRef);

            String file = $ref.split("#/")[0];
            if (header.get$ref() != null) {
                RefFormat format = computeRefFormat(header.get$ref());
                if (isAnExternalRefFormat(format)) {
                    header.set$ref(processRefToExternalHeader(header.get$ref(), format));
                } else {
                    processRefToExternalHeader(file + header.get$ref(), RefFormat.RELATIVE);
                }
            }
        }

        return newRef;
    }

    public String processRefToExternalSecurityScheme(String $ref, RefFormat refFormat) {
        String renamedRef = cache.getRenamedRef($ref);
        if(renamedRef != null) {
            return renamedRef;
        }

        final SecurityScheme securityScheme = cache.loadRef($ref, refFormat, SecurityScheme.class);

        if(securityScheme == null) {
            // stop!  There's a problem.  retain the original ref
            LOGGER.warn("unable to load model reference from `" + $ref + "`.  It may not be available " +
                    "or the reference isn't a valid model schema");
            return $ref;
        }
        String newRef;

        if (openAPI.getComponents() == null) {
            openAPI.setComponents(new Components());
        }
        Map<String, SecurityScheme> securitySchemeMap = openAPI.getComponents().getSecuritySchemes();

        if (securitySchemeMap == null) {
            securitySchemeMap = new LinkedHashMap<>();
        }

        final String possiblyConflictingDefinitionName = computeDefinitionName($ref);

        SecurityScheme existingSecurityScheme = securitySchemeMap.get(possiblyConflictingDefinitionName);

        if (existingSecurityScheme != null) {
            LOGGER.debug("A model for " + existingSecurityScheme + " already exists");
            if(existingSecurityScheme.get$ref() != null) {
                // use the new model
                existingSecurityScheme = null;
            }
        }
        newRef = possiblyConflictingDefinitionName;
        cache.putRenamedRef($ref, newRef);

        if(existingSecurityScheme == null) {
            // don't overwrite existing model reference
            openAPI.getComponents().addSecuritySchemes(newRef, securityScheme);
            cache.addReferencedKey(newRef);

            String file = $ref.split("#/")[0];
            if (securityScheme.get$ref() != null) {
                RefFormat format = computeRefFormat(securityScheme.get$ref());
                if (isAnExternalRefFormat(format)) {
                    securityScheme.set$ref(processRefToExternalSecurityScheme(securityScheme.get$ref(), format));
                } else {
                    processRefToExternalSecurityScheme(file + securityScheme.get$ref(), RefFormat.RELATIVE);
                }
            }
        }

        return newRef;
    }

    public String processRefToExternalLink(String $ref, RefFormat refFormat) {
        String renamedRef = cache.getRenamedRef($ref);
        if(renamedRef != null) {
            return renamedRef;
        }

        final Link link = cache.loadRef($ref, refFormat, Link.class);

        if(link == null) {
            // stop!  There's a problem.  retain the original ref
            LOGGER.warn("unable to load model reference from `" + $ref + "`.  It may not be available " +
                    "or the reference isn't a valid model schema");
            return $ref;
        }
        String newRef;

        if (openAPI.getComponents() == null) {
            openAPI.setComponents(new Components());
        }
        Map<String, Link> links = openAPI.getComponents().getLinks();

        if (links == null) {
            links = new LinkedHashMap<>();
        }

        final String possiblyConflictingDefinitionName = computeDefinitionName($ref);

        Link existingLink = links.get(possiblyConflictingDefinitionName);

        if (existingLink != null) {
            LOGGER.debug("A model for " + existingLink + " already exists");
            if(existingLink.get$ref() != null) {
                // use the new model
                existingLink = null;
            }
        }
        newRef = possiblyConflictingDefinitionName;
        cache.putRenamedRef($ref, newRef);

        if(existingLink == null) {
            // don't overwrite existing model reference
            openAPI.getComponents().addLinks(newRef, link);
            cache.addReferencedKey(newRef);

            String file = $ref.split("#/")[0];
            if (link.get$ref() != null) {
                RefFormat format = computeRefFormat(link.get$ref());
                if (isAnExternalRefFormat(format)) {
                    link.set$ref(processRefToExternalLink(link.get$ref(), format));
                } else {
                    processRefToExternalLink(file + link.get$ref(), RefFormat.RELATIVE);
                }
            }
        }

        return newRef;
    }

    public String processRefToExternalExample(String $ref, RefFormat refFormat) {
        String renamedRef = cache.getRenamedRef($ref);
        if(renamedRef != null) {
            return renamedRef;
        }

        final Example example = cache.loadRef($ref, refFormat, Example.class);

        if(example == null) {
            // stop!  There's a problem.  retain the original ref
            LOGGER.warn("unable to load model reference from `" + $ref + "`.  It may not be available " +
                    "or the reference isn't a valid model schema");
            return $ref;
        }
        String newRef;

        if (openAPI.getComponents() == null) {
            openAPI.setComponents(new Components());
        }
        Map<String, Example> examples = openAPI.getComponents().getExamples();

        if (examples == null) {
            examples = new LinkedHashMap<>();
        }

        final String possiblyConflictingDefinitionName = computeDefinitionName($ref);

        Example existingExample = examples.get(possiblyConflictingDefinitionName);

        if (existingExample != null) {
            LOGGER.debug("A model for " + existingExample + " already exists");
            if(existingExample.get$ref() != null) {
                // use the new model
                existingExample = null;
            }
        }
        newRef = possiblyConflictingDefinitionName;
        cache.putRenamedRef($ref, newRef);

        if(existingExample == null) {
            // don't overwrite existing model reference
            openAPI.getComponents().addExamples(newRef, example);
            cache.addReferencedKey(newRef);

            String file = $ref.split("#/")[0];
            if (example.get$ref() != null) {
                RefFormat format = computeRefFormat(example.get$ref());
                if (isAnExternalRefFormat(format)) {
                    example.set$ref(processRefToExternalExample(example.get$ref(), format));
                } else {
                    processRefToExternalExample(file + example.get$ref(), RefFormat.RELATIVE);
                }
            }
        }

        return newRef;
    }

    public String processRefToExternalParameter(String $ref, RefFormat refFormat) {
        String renamedRef = cache.getRenamedRef($ref);
        if(renamedRef != null) {
            return renamedRef;
        }

        final Parameter parameter = cache.loadRef($ref, refFormat, Parameter.class);

        if(parameter == null) {
            // stop!  There's a problem.  retain the original ref
            LOGGER.warn("unable to load model reference from `" + $ref + "`.  It may not be available " +
                    "or the reference isn't a valid model schema");
            return $ref;
        }
        String newRef;

        if (openAPI.getComponents() == null) {
            openAPI.setComponents(new Components());
        }
        Map<String, Parameter> parameters = openAPI.getComponents().getParameters();

        if (parameters == null) {
            parameters = new LinkedHashMap<>();
        }

        final String possiblyConflictingDefinitionName = computeDefinitionName($ref);

        Parameter existingParameters = parameters.get(possiblyConflictingDefinitionName);

        if (existingParameters != null) {
            LOGGER.debug("A model for " + existingParameters + " already exists");
            if(existingParameters.get$ref() != null) {
                // use the new model
                existingParameters = null;
            }
        }
        newRef = possiblyConflictingDefinitionName;
        cache.putRenamedRef($ref, newRef);

        if(existingParameters == null) {
            // don't overwrite existing model reference
            openAPI.getComponents().addParameters(newRef, parameter);
            cache.addReferencedKey(newRef);

            String file = $ref.split("#/")[0];
            if (parameter.get$ref() != null) {
                RefFormat format = computeRefFormat(parameter.get$ref());
                if (isAnExternalRefFormat(format)) {
                    parameter.set$ref(processRefToExternalParameter(parameter.get$ref(), format));
                } else {
                    processRefToExternalParameter(file + parameter.get$ref(), RefFormat.RELATIVE);
                }
            }
        }

        return newRef;
    }

    public String processRefToExternalCallback(String $ref, RefFormat refFormat) {
        String renamedRef = cache.getRenamedRef($ref);
        if(renamedRef != null) {
            return renamedRef;
        }

        final Callback callback = cache.loadRef($ref, refFormat, Callback.class);

        if(callback == null) {
            // stop!  There's a problem.  retain the original ref
            LOGGER.warn("unable to load model reference from `" + $ref + "`.  It may not be available " +
                    "or the reference isn't a valid model schema");
            return $ref;
        }
        String newRef;

        if (openAPI.getComponents() == null) {
            openAPI.setComponents(new Components());
        }
        Map<String, Callback> callbacks = openAPI.getComponents().getCallbacks();

        if (callbacks == null) {
            callbacks = new LinkedHashMap<>();
        }

        final String possiblyConflictingDefinitionName = computeDefinitionName($ref);

        Callback existingCallback = callbacks.get(possiblyConflictingDefinitionName);

        if (existingCallback != null) {
            LOGGER.debug("A model for " + existingCallback + " already exists");
            if(existingCallback.get("$ref").get$ref() != null) {
                // use the new model
                existingCallback = null;
            }
        }
        newRef = possiblyConflictingDefinitionName;
        cache.putRenamedRef($ref, newRef);

        if(existingCallback == null) {
            // don't overwrite existing model reference
            openAPI.getComponents().addCallbacks(newRef, callback);
            cache.addReferencedKey(newRef);

            String file = $ref.split("#/")[0];
            if(callback.get("$ref") != null){
                if (callback.get("$ref").get$ref() != null) {
                    RefFormat format = computeRefFormat(callback.get("$ref").get$ref());
                    if (isAnExternalRefFormat(format)) {
                        callback.get("$ref").set$ref(processRefToExternalCallback(callback.get("$ref").get$ref(), format));
                    } else {
                        processRefToExternalCallback(file + callback.get("$ref").get$ref(), RefFormat.RELATIVE);
                    }
                }
            }
        }

        return newRef;
    }

    private void processRefSchema(Schema subRef, String externalFile) {
        RefFormat format = computeRefFormat(subRef.get$ref());

        if (!isAnExternalRefFormat(format)) {
            processRefToExternalSchema(externalFile + subRef.get$ref(), RefFormat.RELATIVE);
            return;
        }
        String $ref = subRef.get$ref();
        if (format.equals(RefFormat.RELATIVE)) {
            $ref = constructRef(subRef, externalFile);
            subRef.set$ref($ref);
        }
        processRefToExternalSchema($ref, computeRefFormat(subRef.get$ref()));
    }


    protected String constructRef(Schema refProperty, String rootLocation) {
        String ref = refProperty.get$ref();
        return join(rootLocation, ref);
    }

    // visible for testing
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