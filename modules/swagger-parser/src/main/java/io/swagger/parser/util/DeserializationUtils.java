package io.swagger.parser.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.swagger.util.Json;
import io.swagger.util.ObjectMapperFactory;
import io.swagger.util.Yaml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.BaseConstructor;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;
import org.yaml.snakeyaml.resolver.Resolver;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class DeserializationUtils {

    public static class Options {
        private Integer maxYamlDepth = System.getProperty("maxYamlDepth") == null ? 2000 : Integer.parseInt(System.getProperty("maxYamlDepth"));
        private Long maxYamlReferences = System.getProperty("maxYamlReferences") == null ? 10000000L : Long.parseLong(System.getProperty("maxYamlReferences"));
        private boolean validateYamlInput = System.getProperty("validateYamlInput") == null || Boolean.parseBoolean(System.getProperty("validateYamlInput"));
        private boolean supportYamlAnchors = System.getProperty("supportYamlAnchors") == null || Boolean.parseBoolean(System.getProperty("supportYamlAnchors"));
        private boolean yamlCycleCheck = System.getProperty("yamlCycleCheck") == null || Boolean.parseBoolean(System.getProperty("yamlCycleCheck"));
        private Integer maxYamlAliasesForCollections = System.getProperty("maxYamlAliasesForCollections") == null ? Integer.MAX_VALUE : Integer.parseInt(System.getProperty("maxYamlAliasesForCollections"));
        private boolean yamlAllowRecursiveKeys = System.getProperty("yamlAllowRecursiveKeys") == null || Boolean.parseBoolean(System.getProperty("yamlAllowRecursiveKeys"));
        private Integer maxYamlCodePoints = System.getProperty("maxYamlCodePoints") == null ? 3 * 1024 * 1024 : Integer.parseInt(System.getProperty("maxYamlCodePoints"));

        public Integer getMaxYamlDepth() {
            return maxYamlDepth;
        }

        public void setMaxYamlDepth(Integer maxYamlDepth) {
            this.maxYamlDepth = maxYamlDepth;
        }

        public Long getMaxYamlReferences() {
            return maxYamlReferences;
        }

        public void setMaxYamlReferences(Long maxYamlReferences) {
            this.maxYamlReferences = maxYamlReferences;
        }

        public boolean isValidateYamlInput() {
            return validateYamlInput;
        }

        public void setValidateYamlInput(boolean validateYamlInput) {
            this.validateYamlInput = validateYamlInput;
        }

        public boolean isSupportYamlAnchors() {
            return supportYamlAnchors;
        }

        public void setSupportYamlAnchors(boolean supportYamlAnchors) {
            this.supportYamlAnchors = supportYamlAnchors;
        }

        public boolean isYamlCycleCheck() {
            return yamlCycleCheck;
        }

        public void setYamlCycleCheck(boolean yamlCycleCheck) {
            this.yamlCycleCheck = yamlCycleCheck;
        }

        /**
         * @since 1.0.52
         */
        public Integer getMaxYamlAliasesForCollections() {
            return maxYamlAliasesForCollections;
        }

        /**
         * @since 1.0.52
         */
        public void setMaxYamlAliasesForCollections(Integer maxYamlAliasesForCollections) {
            this.maxYamlAliasesForCollections = maxYamlAliasesForCollections;
        }

        /**
         * @since 1.0.52
         */
        public boolean isYamlAllowRecursiveKeys() {
            return yamlAllowRecursiveKeys;
        }

        /**
         * @since 1.0.52
         */
        public void setYamlAllowRecursiveKeys(boolean yamlAllowRecursiveKeys) {
            this.yamlAllowRecursiveKeys = yamlAllowRecursiveKeys;
        }

        public Integer getMaxYamlCodePoints() {
            return maxYamlCodePoints;
        }

        public void setMaxYamlCodePoints(Integer maxYamlCodePointsInBytes) {
            this.maxYamlCodePoints = maxYamlCodePointsInBytes;
        }
    }

    private static Options options = new Options();

    private static final Logger LOGGER = LoggerFactory.getLogger(DeserializationUtils.class);

    private static ObjectMapper yamlMapper = Yaml.mapper();

    public static void setYamlMapper(YAMLFactory yamlFactory) {
        yamlMapper = ObjectMapperFactory.createYaml(yamlFactory);
    }

    public static ObjectMapper getYamlMapper() {
        return yamlMapper;
    }

    public static Options getOptions() {
        return options;
    }
    public static class CustomResolver extends Resolver {

        /*
         * do not resolve timestamp
         */
        @Override
        protected void addImplicitResolvers() {
            addImplicitResolver(Tag.BOOL, BOOL, "yYnNtTfFoO");
            addImplicitResolver(Tag.INT, INT, "-+0123456789");
            addImplicitResolver(Tag.FLOAT, FLOAT, "-+0123456789.");
            addImplicitResolver(Tag.MERGE, MERGE, "<");
            addImplicitResolver(Tag.NULL, NULL, "~nN\0");
            addImplicitResolver(Tag.NULL, EMPTY, null);
            // addImplicitResolver(Tag.TIMESTAMP, TIMESTAMP, "0123456789");
        }
    }

    public static JsonNode deserializeIntoTree(String contents, String fileOrHost) {
        return deserializeIntoTree(contents, fileOrHost, null);
    }

    public static JsonNode deserializeIntoTree(String contents, String fileOrHost, SwaggerDeserializationResult errorOutput) {
        JsonNode result;

        try {
            if (isJson(contents)) {
                result = Json.mapper().readTree(contents);
            } else {
                result = readYamlTree(contents, errorOutput);
            }
        } catch (IOException e) {
            throw new RuntimeException("An exception was thrown while trying to deserialize the contents of " + fileOrHost + " into a JsonNode tree", e);
        }

        return result;
    }

    public static <T> T deserialize(Object contents, String fileOrHost, Class<T> expectedType) {
        return deserialize(contents, fileOrHost, expectedType, null);
    }

    public static <T> T deserialize(Object contents, String fileOrHost, Class<T> expectedType, SwaggerDeserializationResult errorOutput) {
        T result;

        boolean isJson = contents instanceof String && isJson((String) contents);

        try {
            if (contents instanceof String) {
                if (isJson) {
                    result = Json.mapper().readValue((String) contents, expectedType);
                } else {
                    result = getYamlMapper().convertValue(readYamlTree((String) contents, errorOutput), expectedType);
                }
            } else {
                result = Json.mapper().convertValue(contents, expectedType);
            }
        } catch (Exception e) {
            throw new RuntimeException("An exception was thrown while trying to deserialize the contents of " + fileOrHost + " into type " + expectedType, e);
        }

        return result;
    }

    private static boolean isJson(String contents) {
        return contents.trim().startsWith("{");
    }


    public static org.yaml.snakeyaml.Yaml buildSnakeYaml(BaseConstructor constructor) {
        try {
            LoaderOptions.class.getMethod("getMaxAliasesForCollections");
        } catch (NoSuchMethodException e) {
            return new org.yaml.snakeyaml.Yaml(constructor);
        }
        try {
            LoaderOptions loaderOptions = buildLoaderOptions();
            org.yaml.snakeyaml.Yaml yaml = new org.yaml.snakeyaml.Yaml(constructor, new Representer(new DumperOptions()), new DumperOptions(), loaderOptions, new CustomResolver());
            return yaml;
        } catch (Exception e) {
            //
            LOGGER.error("error building snakeYaml", e);
        }
        return new org.yaml.snakeyaml.Yaml(constructor);
    }

    public static LoaderOptions buildLoaderOptions() {
        LoaderOptions loaderOptions = new LoaderOptions();
        try {
            Method method = LoaderOptions.class.getMethod("setMaxAliasesForCollections", int.class);
            method.invoke(loaderOptions, options.getMaxYamlAliasesForCollections());
            method = LoaderOptions.class.getMethod("setAllowRecursiveKeys", boolean.class);
            method.invoke(loaderOptions, options.isYamlAllowRecursiveKeys());
            method = LoaderOptions.class.getMethod("setCodePointLimit", int.class);
            method.invoke(loaderOptions, options.getMaxYamlCodePoints());
        } catch (ReflectiveOperationException e) {
            LOGGER.debug("using snakeyaml < 1.25, not setting YAML Billion Laughs Attack snakeyaml level protection");
        }
        return loaderOptions;
    }

    public static JsonNode readYamlTree(String contents, SwaggerDeserializationResult errorOutput) throws IOException {

        if (!options.isSupportYamlAnchors()) {
            return getYamlMapper().readTree(contents);
        }
        try {
            org.yaml.snakeyaml.Yaml yaml = null;
            if (options.isValidateYamlInput()) {
                yaml = buildSnakeYaml(new CustomSnakeYamlConstructor());
            } else {
                yaml = buildSnakeYaml(new SafeConstructor(buildLoaderOptions()));
            }

            Object o = yaml.load(contents);
            if (options.isValidateYamlInput()) {
                boolean res = exceedsLimits(o, null, new Integer(0), new IdentityHashMap<Object, Long>(), errorOutput);
                if (res) {
                    LOGGER.warn("Error converting snake-parsed yaml to JsonNode");
                    return getYamlMapper().readTree(contents);
                }
            }
            JsonNode n =  Json.mapper().convertValue(o, JsonNode.class);
            return n;


        } catch (Throwable e) {
            LOGGER.warn("Error snake-parsing yaml content", e);
            if (errorOutput != null) {
                errorOutput.message(e.getMessage());
            }
            return getYamlMapper().readTree(contents);
        }
    }

    private static boolean exceedsLimits(Object o, Object parent, Integer depth, Map<Object, Long> visited, SwaggerDeserializationResult errorOutput) {

        if (o == null) return false;
        if (!(o instanceof List) && !(o instanceof Map)) return false;
        if (depth > options.getMaxYamlDepth()) {
            String msg = String.format("snake-yaml result exceeds max depth %d; threshold can be increased if needed by setting system property `maxYamlDepth` to a higher value.", options.getMaxYamlDepth());
            LOGGER.warn(msg);
            if (errorOutput != null) {
                errorOutput.message(msg);
            }
            return true;
        }
        int currentDepth = depth;
        if (visited.containsKey(o)) {
            Object target = parent;
            if (target == null) {
                target = o;
            }
            if (options.isYamlCycleCheck()) {
                boolean res = hasReference(o, target, new Integer(0), new IdentityHashMap<Object, Long>(), errorOutput);
                if (res) {
                    return true;
                }
            }
            if (visited.get(o) > options.getMaxYamlReferences()) {
                String msg = String.format("snake-yaml result exceeds max references %d; threshold can be increased if needed by setting system property `maxYamlReferences` to a higher value.", options.getMaxYamlReferences());
                LOGGER.warn(msg);
                if (errorOutput != null) {
                    errorOutput.message(msg);
                }
                return true;
            }
            visited.put(o, visited.get(o) + 1);

        } else {
            visited.put(o, 1L);
        }

        if (o instanceof Map) {
            for (Object k : ((Map) o).keySet()) {
                boolean res = exceedsLimits(k, o, currentDepth + 1, visited, errorOutput);
                if (res) {
                    return true;
                }
            }
            for (Object v : ((Map) o).values()) {
                boolean res = exceedsLimits(v, o, currentDepth + 1, visited, errorOutput);
                if (res) {
                    return true;
                }
            }

        } else if (o instanceof List) {
            for (Object v: ((List)o)) {
                boolean res = exceedsLimits(v, o, currentDepth + 1, visited, errorOutput);
                if (res) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasReference(Object o, Object target, Integer depth, Map<Object, Long> visited, SwaggerDeserializationResult errorOutput) {

        if (o == null || target == null) return false;
        if (!(o instanceof List) && !(o instanceof Map)) return false;
        if (!(target instanceof List) && !(target instanceof Map)) return false;
        if (depth > options.getMaxYamlDepth()) {
            String msg = String.format("snake-yaml result exceeds max depth %d; threshold can be increased if needed by setting system property `maxYamlDepth` to a higher value.", options.getMaxYamlDepth());
            LOGGER.warn(msg);
            if (errorOutput != null) {
                errorOutput.message(msg);
            }
            return true;
        }
        int currentDepth = depth;
        if (visited.containsKey(target)) {
            return false;
        }
        visited.put(o, 1L);
        ArrayList children = new ArrayList();
        if (o instanceof Map) {
            children.addAll(((Map)o).keySet());
            children.addAll(((Map)o).values());

        } else if (o instanceof List) {
            children.addAll((List)o);
        }
        for (Object v : children) {
            if (v == target) {
                String msg = "detected cycle in snake-yaml result; cycle check can be disabled by setting system property `yamlCycleCheck` to false.";
                LOGGER.warn(msg);
                if (errorOutput != null) {
                    errorOutput.message(msg);
                }
                return true;
            }
            boolean res = hasReference(v, target, currentDepth + 1, visited, errorOutput);
            if (res) {
                return true;
            }
        }
        return false;
    }

    static class SnakeException extends RuntimeException {
        public SnakeException() {
            super();
        }
        public SnakeException(String msg) {
            super(msg);
        }

        public SnakeException(String message, Throwable cause) {
            super(message, cause);
        }

    }

    static class CustomSnakeYamlConstructor extends SafeConstructor {

        public CustomSnakeYamlConstructor() {
            super(buildLoaderOptions());
        }
        private boolean checkNode(MappingNode node, Integer depth) {
            if (node.getValue() == null) return true;
            if (depth > options.getMaxYamlDepth()) return false;
            int currentDepth = depth;
            List<NodeTuple> list = node.getValue();
            for (NodeTuple t : list) {
                Node n = t.getKeyNode();
                if (n instanceof MappingNode) {
                    boolean res = checkNode((MappingNode) n, currentDepth + 1);
                    if (!res) {
                        return false;
                    }
                }
            }
            return true;
        }

        @Override
        public Object getSingleData(Class<?> type) {
            try {
                Node node = this.composer.getSingleNode();
                if (node != null) {
                    if (node instanceof MappingNode) {
                        if (!checkNode((MappingNode) node, new Integer(0))) {
                            LOGGER.warn("yaml tree depth exceeds max depth {}; threshold can be increased if needed by setting  system property `maxYamlDepth` to a higher value.", options.getMaxYamlDepth());
                            throw new SnakeException("yaml tree depth exceeds max " + options.getMaxYamlDepth());
                        }
                    }
                    if (Object.class != type) {
                        node.setTag(new Tag(type));
                    } else if (this.rootTag != null) {
                        node.setTag(this.rootTag);
                    }

                    return this.constructDocument(node);
                } else {
                    return null;
                }
            } catch (StackOverflowError e) {
                throw new SnakeException("StackOverflow safe-checking yaml content (maxDepth " + options.getMaxYamlDepth() + ")", e);
            } catch (Throwable e) {
                throw new SnakeException("Exception safe-checking yaml content  (maxDepth " + options.getMaxYamlDepth() + ", maxYamlAliasesForCollections " + options.getMaxYamlAliasesForCollections() + ")", e);
            }
        }
    }
}
