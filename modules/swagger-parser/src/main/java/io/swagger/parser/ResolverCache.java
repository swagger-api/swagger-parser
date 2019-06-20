package io.swagger.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.models.Model;
import io.swagger.models.ModelImpl;
import io.swagger.models.RefModel;
import io.swagger.models.Response;
import io.swagger.models.Swagger;
import io.swagger.models.auth.AuthorizationValue;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.RefProperty;
import io.swagger.models.refs.RefFormat;
import io.swagger.models.refs.RefType;
import io.swagger.parser.util.DeserializationUtils;
import io.swagger.parser.util.PathUtils;
import io.swagger.parser.util.RefUtils;
import io.swagger.parser.util.SwaggerDeserializer;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A class that caches values that have been loaded so we don't have to repeat
 * expensive operations like:
 * 1) reading a remote URL with authorization (e.g. using RemoteURL.java)
 * 2) reading the contents of a file into memory
 * 3) extracting a sub object from a json/yaml tree
 * 4) de-serializing json strings into objects
 */
public class ResolverCache {

    private static final Pattern PARAMETER_PATTERN = Pattern.compile("^" + RefType.PARAMETER.getInternalPrefix() + "(?<name>.+)");
    private static final Pattern DEFINITION_PATTERN = Pattern.compile("^" + RefType.DEFINITION.getInternalPrefix() + "(?<name>.+)");
    private static final Pattern RESPONSE_PATTERN = Pattern.compile("^" + RefType.RESPONSE.getInternalPrefix() + "(?<name>.+)");
    private static final Pattern PATHS_PATTERN = Pattern.compile("^" + RefType.PATH.getInternalPrefix() + "(?<name>.+)");

    private final Swagger swagger;
    private final List<AuthorizationValue> auths;
    private final Path parentDirectory;
    private final String parentUrl;
    private final String rootPath;
    private Map<String, Object> resolutionCache = new HashMap<>();
    private Map<String, String> externalFileCache = new HashMap<>();
    private Set<String> referencedModelKeys = new HashSet<>();

    /*
    a map that stores original external references, and their associated renamed references
     */
    private Map<String, String> renameCache = new ConcurrentHashMap<>();

    public ResolverCache(Swagger swagger, List<AuthorizationValue> auths, String parentFileLocation) {
        this.swagger = swagger;
        this.auths = auths;
        this.rootPath = parentFileLocation;

        if(parentFileLocation != null) {
            if(parentFileLocation.startsWith("http")) {
                parentDirectory = null;
            } else {
                parentDirectory = PathUtils.getParentDirectoryOfFile(parentFileLocation);
            }
        } else {
            File file = new File(".");
            parentDirectory = file.toPath();
        }
        parentUrl = parentFileLocation;

    }

    public <T> T loadRef(String ref, RefFormat refFormat, Class<T> expectedType) {
        if (refFormat == RefFormat.INTERNAL) {
            //we don't need to go get anything for internal refs
            Object loadedRef = loadInternalRef(ref);

            try{
                return expectedType.cast(loadedRef);
            }
            catch (Exception e) {
                return null;
            }
        }

        final String[] refParts = ref.split("#/");

        if (refParts.length > 2) {
            throw new RuntimeException("Invalid ref format: " + ref);
        }

        final String file = refParts[0];
        final String definitionPath = refParts.length == 2 ? refParts[1] : null;

        //we might have already resolved this ref, so check the resolutionCache
        Object previouslyResolvedEntity = resolutionCache.get(ref);

        if (previouslyResolvedEntity != null) {
            return expectedType.cast(previouslyResolvedEntity);
        }

        //we have not resolved this particular ref
        //but we may have already loaded the file or url in question
        String contents = externalFileCache.get(file);

        if (contents == null) {
            if(parentDirectory != null) {
                contents = RefUtils.readExternalRef(file, refFormat, auths, parentDirectory);
            }
            else if(rootPath != null) {
                contents = RefUtils.readExternalUrlRef(file, refFormat, auths, rootPath);
            }
            externalFileCache.put(file, contents);
        }

        if (definitionPath == null) {
            T result = DeserializationUtils.deserialize(contents, file, expectedType);
            resolutionCache.put(ref, result);
            return result;
        }

        //a definition path is defined, meaning we need to "dig down" through the JSON tree and get the desired entity
        JsonNode tree = deserialize(contents, file);

        String[] jsonPathElements = definitionPath.split("/");
        for (String jsonPathElement : jsonPathElements) {
            tree = tree.get(unescapePointer(jsonPathElement));
            //if at any point we do find an element we expect, print and error and abort
            if (tree == null) {
                throw new RuntimeException("Could not find " + definitionPath + " in contents of " + file);
            }
        }

        T result;
        if (expectedType.equals(Model.class)) {
            SwaggerDeserializer ser = new SwaggerDeserializer();
            result = (T) ser.definition((ObjectNode) tree, definitionPath.replace("/", "."), null);
        } else {
            result = DeserializationUtils.deserialize(tree, file, expectedType);
        }

        updateLocalRefs(file, result);

        resolutionCache.put(ref, result);

        return result;
    }

    protected JsonNode deserialize(String contents, String file) {
        return DeserializationUtils.deserializeIntoTree(contents, file);
    }

    protected <T> void updateLocalRefs(String file, T result) {
        if(result instanceof Response) {
            Response response = (Response) result;
            updateLocalRefs(file, response.getResponseSchema());
        }
        else if(result instanceof RefProperty) {
            RefProperty prop = (RefProperty) result;
            updateLocalRefs(file, prop);
        }
        else if(result instanceof Model) {
            Model model = (Model) result;
            updateLocalRefs(file, model);
        }
    }

    protected <T> void updateLocalRefs(String file, Model schema) {
        if(schema instanceof RefModel) {
            RefModel ref = (RefModel) schema;
            String updatedLocation = merge(file, ref.get$ref());
            ref.set$ref(updatedLocation);
        }
        else if(schema instanceof ModelImpl) {
            ModelImpl impl = (ModelImpl) schema;
            if(impl.getProperties() != null) {
                for(Property property : schema.getProperties().values()) {
                    updateLocalRefs(file, property);
                }
            }
        }
    }

    protected <T> void updateLocalRefs(String file, Property schema) {
        if(schema instanceof RefProperty) {
            RefProperty ref = (RefProperty) schema;
            String updatedLocation = merge(file, ref.get$ref());
            ref.set$ref(updatedLocation);
        }
    }

    protected String merge(String host, String ref) {
        if(StringUtils.isBlank(host)) {
            return ref;
        }
        if(ref.startsWith("http:") || ref.startsWith("https:")) {
            // already an absolute ref
            return ref;
        }
        if(!host.startsWith("http:") && !host.startsWith("https:")) {
            return ref;
        }
        if(ref.startsWith(".")) {
            // relative ref, leave alone
            return ref;
        }
        if(host.endsWith("/") && ref.startsWith("/")) {
            return host + ref.substring(1);
        }
        return host + ref;
    }

    private String unescapePointer(String jsonPathElement) {
        // URL decode the fragment
        try {
            jsonPathElement = URLDecoder.decode(jsonPathElement, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            //
        }
        // Unescape the JSON Pointer segment using the algorithm described in RFC 6901, section 4:
        // https://tools.ietf.org/html/rfc6901#section-4
        // First transform any occurrence of the sequence '~1' to '/'
        jsonPathElement = jsonPathElement.replaceAll("~1", "/");
        // Then transforming any occurrence of the sequence '~0' to '~'.
        return jsonPathElement.replaceAll("~0", "~");
    }

    private Object loadInternalRef(String ref) {
        Object result = null;

        if(ref.startsWith("#/definitions")) {
            result = getFromMap(ref, swagger.getParameters(), PARAMETER_PATTERN);
        }
        else if(ref.startsWith("#/responses")) {
            result = getFromMap(ref, swagger.getResponses(), RESPONSE_PATTERN);
        }
        else if(ref.startsWith("#/parameters")) {
            result = getFromMap(ref, swagger.getParameters(), PARAMETER_PATTERN);
        }
        else if(ref.startsWith("#/paths")) {
            result = getFromMap(ref, swagger.getPaths(), PATHS_PATTERN);
        }
        if (result == null) {
            result = getFromMap(ref, swagger.getDefinitions(), DEFINITION_PATTERN);
        }

        return result;

    }

    private Object getFromMap(String ref, Map map, Pattern pattern) {
        final Matcher parameterMatcher = pattern.matcher(ref);

        if (parameterMatcher.matches()) {
            final String paramName = unescapePointer(parameterMatcher.group("name"));

            if (map != null) {
                return map.get(paramName);
            }
        }
        return null;
    }

    public boolean hasReferencedKey(String modelKey) {
        if(referencedModelKeys == null) {
            return false;
        }
        return referencedModelKeys.contains(modelKey);
    }

    public void addReferencedKey(String modelKey) {
        referencedModelKeys.add(modelKey);
    }

    public String getRenamedRef(String originalRef) {
        return renameCache.get(originalRef);
    }

    public void putRenamedRef(String originalRef, String newRef) {
        renameCache.put(originalRef, newRef);
    }

    public Map<String, Object> getResolutionCache() {
        return Collections.unmodifiableMap(resolutionCache);
    }

    public Map<String, String> getExternalFileCache() {
        return Collections.unmodifiableMap(externalFileCache);
    }

    public Map<String, String> getRenameCache() {
        return Collections.unmodifiableMap(renameCache);
    }
}
