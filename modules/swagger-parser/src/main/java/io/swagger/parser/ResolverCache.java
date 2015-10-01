package io.swagger.parser;

import com.google.common.collect.ImmutableList;
import com.fasterxml.jackson.databind.JsonNode;

import io.swagger.models.Model;
import io.swagger.models.Response;
import io.swagger.models.Swagger;
import io.swagger.models.auth.AuthorizationValue;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.refs.RefFormat;
import io.swagger.models.refs.RefType;
import io.swagger.parser.util.DeserializationUtils;
import io.swagger.parser.util.PathUtils;
import io.swagger.parser.util.RefUtils;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
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

    private final Swagger swagger;
    private final List<AuthorizationValue> auths;
    private final Path parentDirectory;
    private Map<String, Object> resolutionCache = new HashMap<>();
    private Map<String, String> externalFileCache = new HashMap<>();

    /*
    a map that stores original external references, and their associated renamed references
     */
    private Map<String, String> renameCache = new HashMap<>();

    public ResolverCache(Swagger swagger, List<AuthorizationValue> auths, String parentFileLocation) {
        this.swagger = swagger;
        this.auths = auths;

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
            return expectedType.cast(loadInternalRef(ref, expectedType));
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
            T result = expectedType.cast(previouslyResolvedEntity);
            return result;
        }

        //we have not resolved this particular ref
        //but we may have already loaded the file or url in question
        String contents = externalFileCache.get(file);

        if (contents == null) {
            contents = RefUtils.readExternalRef(file, refFormat, auths, parentDirectory);
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

        T result = DeserializationUtils.deserialize(tree, file, expectedType);
        resolutionCache.put(ref, result);

        return result;
    }

    private abstract static class InternalRefAccessor<T> {
        private final Class<T> resultType;
        private final Pattern pattern;

        private InternalRefAccessor(final Class<T> resultType, final RefType refType) {
            this.resultType = resultType;
            this.pattern = Pattern.compile("^" + refType.getInternalPrefix() + "(?<name>\\S+)");
        }

        abstract Map<String, T> getMap(Swagger s);

        static final List<InternalRefAccessor<?>> INSTANCES = ImmutableList.of(new InternalRefAccessor<Model>(
                    Model.class, RefType.DEFINITION) {
                    @Override
                    Map<String, Model> getMap(final Swagger swagger) {
                        return swagger.getDefinitions();
                    }
                }, new InternalRefAccessor<Parameter>(Parameter.class, RefType.PARAMETER) {
                    @Override
                    Map<String, Parameter> getMap(final Swagger swagger) {
                        return swagger.getParameters();
                    }
                }, new InternalRefAccessor<io.swagger.models.Path>(io.swagger.models.Path.class, RefType.PATH) {
                    @Override
                    Map<String, io.swagger.models.Path> getMap(final Swagger swagger) {
                        return swagger.getPaths();
                    }
                }, new InternalRefAccessor<Response>(Response.class, RefType.RESPONSE) {
                    @Override
                    Map<String, Response> getMap(final Swagger swagger) {

                        // TODO implement this
                        throw new UnsupportedOperationException("swagger.getResponses() is missing!");
                    }
                });

        @SuppressWarnings("unchecked")
        static <T> InternalRefAccessor<T> getByClass(final Class<T> clazz) {
            for (final InternalRefAccessor<?> internalRefAccessor : INSTANCES) {
                if (internalRefAccessor.resultType == clazz) {
                    return (InternalRefAccessor<T>) internalRefAccessor;
                }
            }

            throw new NoSuchElementException("no accessor for " + clazz);
        }
    }

    private <T> T loadInternalRef(final String ref, final Class<T> expectedType) {
        final InternalRefAccessor<T> accessor = InternalRefAccessor.getByClass(expectedType);
        return getFromMap(ref, accessor.getMap(swagger), accessor.pattern);
    }

    private <T> T getFromMap(final String ref, final Map<String, T> map, final Pattern pattern) {
        final Matcher refMatcher = pattern.matcher(ref);

        if (refMatcher.matches()) {
            final String paramName = refMatcher.group("name");

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
