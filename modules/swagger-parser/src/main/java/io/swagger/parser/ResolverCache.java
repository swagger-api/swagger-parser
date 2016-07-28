package io.swagger.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.models.Model;
import io.swagger.models.Swagger;
import io.swagger.models.auth.AuthorizationValue;
import io.swagger.models.refs.RefFormat;
import io.swagger.models.refs.RefType;
import io.swagger.parser.util.DeserializationUtils;
import io.swagger.parser.util.PathUtils;
import io.swagger.parser.util.RefUtils;
import io.swagger.parser.util.SwaggerDeserializer;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    private final Swagger swagger;
    private final List<AuthorizationValue> auths;
    private final Path parentDirectory;
    private final String rootPath;
    private Map<String, Object> resolutionCache = new HashMap<>();
    private Map<String, String> externalFileCache = new HashMap<>();

    /*
    a map that stores original external references, and their associated renamed references
     */
    private Map<String, String> renameCache = new HashMap<>();

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

    }

    public <T> T loadRef(String ref, RefFormat refFormat, Class<T> expectedType) {
        if (refFormat == RefFormat.INTERNAL) {
            //we don't need to go get anything for internal refs
            return expectedType.cast(loadInternalRef(ref));
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
        JsonNode tree = DeserializationUtils.deserializeIntoTree(contents, file);

        String[] jsonPathElements = definitionPath.split("/");
        for (String jsonPathElement : jsonPathElements) {
            tree = tree.get(jsonPathElement);
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
        resolutionCache.put(ref, result);

        return result;
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
        if (result == null) {
            result = getFromMap(ref, swagger.getDefinitions(), DEFINITION_PATTERN);
        }

        return result;

    }

    private Object getFromMap(String ref, Map map, Pattern pattern) {
        final Matcher parameterMatcher = pattern.matcher(ref);

        if (parameterMatcher.matches()) {
            final String paramName = parameterMatcher.group("name");

            if (map != null) {
                return map.get(paramName);
            }
        }
        return null;
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
