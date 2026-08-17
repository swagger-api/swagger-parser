package io.swagger.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.models.Model;
import io.swagger.models.ModelImpl;
import io.swagger.models.RefModel;
import io.swagger.models.Response;
import io.swagger.models.Swagger;
import io.swagger.models.auth.AuthorizationValue;
import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.RefProperty;
import io.swagger.models.refs.RefFormat;
import io.swagger.models.refs.RefType;
import io.swagger.parser.util.*;
import io.swagger.parser.urlresolver.PermittedUrlsChecker;
import io.swagger.parser.urlresolver.exceptions.HostDeniedException;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    private final String canonicalRootFile;
    private final ParseOptions parseOptions;
    private Map<String, Object> resolutionCache = new HashMap<>();
    private Map<String, String> externalFileCache = new HashMap<>();
    private Map<String, Object> canonicalResolutionCache = new HashMap<>();
    private Map<String, String> canonicalExternalFileCache = new HashMap<>();
    private Set<String> referencedModelKeys = new HashSet<>();

    /*
    a map that stores original external references, and their associated renamed references
     */
    private Map<String, String> renameCache = new ConcurrentHashMap<>();
    private Map<String, String> canonicalRenameCache = new ConcurrentHashMap<>();

    public ResolverCache(Swagger swagger, List<AuthorizationValue> auths, String parentFileLocation) {
        this(swagger, auths, parentFileLocation, new ParseOptions());
    }

    public ResolverCache(Swagger swagger, List<AuthorizationValue> auths, String parentFileLocation, ParseOptions parseOptions) {
        this.swagger = swagger;
        this.auths = auths;
        this.rootPath = parentFileLocation;
        this.parseOptions = parseOptions;

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
        canonicalRootFile = canonicalizeRootFile(parentFileLocation);
        registerRootReferences();

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
        final String canonicalRef = canonicalize(ref);
        final String canonicalFile = canonicalize(file);

        //we might have already resolved an equivalent ref, so check the canonical cache
        Object previouslyResolvedEntity = canonicalResolutionCache.get(canonicalRef);

        if (previouslyResolvedEntity != null) {
            resolutionCache.putIfAbsent(ref, previouslyResolvedEntity);
            cacheExternalFile(file, canonicalFile, refFormat);
            return expectedType.cast(previouslyResolvedEntity);
        }

        //we have not resolved this particular ref
        //but we may have already loaded the file or url in question
        String contents = canonicalExternalFileCache.get(canonicalFile);

        if (contents == null) {
            contents = readExternalFile(file, refFormat);
            canonicalExternalFileCache.put(canonicalFile, contents);
        }
        externalFileCache.putIfAbsent(file, contents);

        if (definitionPath == null) {
            T result = DeserializationUtils.deserialize(contents, file, expectedType);
            resolutionCache.put(ref, result);
            canonicalResolutionCache.put(canonicalRef, result);
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
        canonicalResolutionCache.put(canonicalRef, result);
        
        if (result instanceof BodyParameter) {
        	loadRef(ref, refFormat, (BodyParameter) result);
        }

        return result;
    }

	private void loadRef(String ref, RefFormat refFormat, final BodyParameter bodyParameter) {
		final Model schema = bodyParameter.getSchema();
		if (schema instanceof RefModel && refFormat != RefFormat.INTERNAL) {
			loadRef(ref, refFormat, (RefModel) schema);
		}
	}

	private void loadRef(String ref, RefFormat refFormat, final RefModel refModel) {
		final String rootRef = ref.substring(0, ref.indexOf('#'));
		final String externalRef = RefUtils.isAnExternalRefFormat(refModel.getRefFormat()) ? refModel.getReference()
				: rootRef + refModel.getReference();
		final Model derefModel = loadRef(externalRef, refFormat, Model.class);
		swagger.addDefinition(refModel.getSimpleRef(), derefModel);
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

    protected void checkUrlIsPermitted(String refSet) {
        try {
            PermittedUrlsChecker permittedUrlsChecker = new PermittedUrlsChecker(parseOptions.getRemoteRefAllowList(),
                    parseOptions.getRemoteRefBlockList());

            permittedUrlsChecker.verify(refSet);
        } catch (HostDeniedException exception) {
            throw new RuntimeException(exception.getMessage());
        }
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
        if (originalRef == null) {
            return null;
        }
        if (originalRef.startsWith("#/")) {
            return renameCache.get(originalRef);
        }
        return canonicalRenameCache.get(canonicalize(originalRef));
    }

    public void putRenamedRef(String originalRef, String newRef) {
        renameCache.put(originalRef, newRef);
        canonicalRenameCache.put(canonicalize(originalRef), newRef);
    }

    private String readExternalFile(String file, RefFormat refFormat) {
        if(parseOptions.isSafelyResolveURL()){
            checkUrlIsPermitted(file);
        }

        if(parentDirectory != null) {
            return RefUtils.readExternalRef(file, refFormat, auths, parentDirectory);
        }
        if(rootPath != null) {
            return RefUtils.readExternalUrlRef(file, refFormat, auths, rootPath);
        }
        return null;
    }

    private void cacheExternalFile(String file, String canonicalFile, RefFormat refFormat) {
        String contents = canonicalExternalFileCache.get(canonicalFile);
        if (contents == null) {
            contents = readExternalFile(file, refFormat);
            canonicalExternalFileCache.put(canonicalFile, contents);
        }
        externalFileCache.putIfAbsent(file, contents);
    }

    private String canonicalize(String ref) {
        if (ref == null || ref.isEmpty()) {
            return ref;
        }
        try {
            URI uri = new URI(ref);
            if (uri.isAbsolute()) {
                return uri.normalize().toString();
            }
            if (rootPath != null && (rootPath.startsWith("http://") || rootPath.startsWith("https://"))) {
                return new URI(rootPath).resolve(uri).normalize().toString();
            }
            if ((uri.getPath() == null || uri.getPath().isEmpty()) && canonicalRootFile != null) {
                return appendQueryAndFragment(canonicalRootFile, uri);
            }
            if (parentDirectory != null && uri.getRawPath() != null) {
                URI resolvedFile = parentDirectory.resolve(uri.getRawPath()).normalize().toUri();
                return appendQueryAndFragment(resolvedFile.toString(), uri);
            }
            return uri.normalize().toString();
        } catch (URISyntaxException ignored) {
            return ref;
        }
    }

    private String appendQueryAndFragment(String resource, URI reference) throws URISyntaxException {
        URI resourceUri = new URI(resource);
        return new URI(
                resourceUri.getScheme(),
                resourceUri.getAuthority(),
                resourceUri.getPath(),
                reference.getQuery(),
                reference.getFragment()).normalize().toString();
    }

    private String canonicalizeRootFile(String parentFileLocation) {
        if (parentFileLocation == null || parentFileLocation.isEmpty()) {
            return null;
        }
        try {
            URI rootUri = new URI(parentFileLocation);
            if (rootUri.isAbsolute() && !"file".equalsIgnoreCase(rootUri.getScheme())) {
                return rootUri.normalize().toString();
            }

            Path rootFile;
            if ("file".equalsIgnoreCase(rootUri.getScheme())) {
                rootFile = Paths.get(rootUri);
            } else {
                rootFile = Paths.get(parentFileLocation.replace('\\', '/'));
                if (!Files.exists(rootFile) && parentDirectory != null && rootFile.getFileName() != null) {
                    rootFile = parentDirectory.resolve(rootFile.getFileName());
                }
            }
            URI rootFileUri = rootFile.toAbsolutePath().normalize().toUri();
            return new URI(
                    rootFileUri.getScheme(),
                    rootFileUri.getAuthority(),
                    rootFileUri.getPath(),
                    null,
                    null).toString();
        } catch (Exception ignored) {
            return parentFileLocation;
        }
    }

    private void registerRootReferences() {
        if (canonicalRootFile == null || swagger == null) {
            return;
        }
        registerRootReferences("definitions", swagger.getDefinitions());
        registerRootReferences("responses", swagger.getResponses());
    }

    private void registerRootReferences(String section, Map<String, ?> entries) {
        if (entries == null) {
            return;
        }
        for (Map.Entry<String, ?> entry : entries.entrySet()) {
            String pointer = "#/" + section + "/" + escapePointer(entry.getKey());
            String canonicalRef = canonicalize(canonicalRootFile + pointer);
            canonicalResolutionCache.put(canonicalRef, entry.getValue());
        }
    }

    private String escapePointer(String value) {
        return value.replace("~", "~0").replace("/", "~1");
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
