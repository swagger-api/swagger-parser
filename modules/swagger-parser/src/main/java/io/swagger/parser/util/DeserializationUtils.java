package io.swagger.parser.util;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.util.Json;
import io.swagger.util.Yaml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.Tag;

import java.io.IOException;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class DeserializationUtils {

    public static class Options {
        private Integer maxYamlDepth = System.getProperty("maxYamlDepth") == null ? 2000 : Integer.valueOf(System.getProperty("maxYamlDepth"));
        private Long maxYamlReferences = System.getProperty("maxYamlReferences") == null ? 10000000L : Long.valueOf(System.getProperty("maxYamlReferences"));
        private boolean validateYamlInput = System.getProperty("validateYamlInput") == null ? true : Boolean.valueOf(System.getProperty("validateYamlInput"));
        private boolean supportYamlAnchors = System.getProperty("supportYamlAnchors") == null ? true : Boolean.valueOf(System.getProperty("supportYamlAnchors"));
        private boolean yamlCycleCheck = System.getProperty("yamlCycleCheck") == null ? true : Boolean.valueOf(System.getProperty("yamlCycleCheck"));

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
    }

    private static Options options = new Options();

    private static final Logger LOGGER = LoggerFactory.getLogger(DeserializationUtils.class);

    public static Options getOptions() {
        return options;
    }

    public static JsonNode deserializeIntoTree(String contents, String fileOrHost) {
        JsonNode result;

        try {
            if (isJson(contents)) {
                result = Json.mapper().readTree(contents);
            } else {
                result = readYamlTree(contents);
            }
        } catch (IOException e) {
            throw new RuntimeException("An exception was thrown while trying to deserialize the contents of " + fileOrHost + " into a JsonNode tree", e);
        }

        return result;
    }

    public static <T> T deserialize(Object contents, String fileOrHost, Class<T> expectedType) {
        T result;

        boolean isJson = false;

        if(contents instanceof String && isJson((String)contents)) {
            isJson = true;
        }

        try {
            if (contents instanceof String) {
                if (isJson) {
                    result = Json.mapper().readValue((String) contents, expectedType);
                } else {
                    result = Yaml.mapper().readValue((String) contents, expectedType);
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
        return contents.toString().trim().startsWith("{");
    }

    public static JsonNode readYamlTree(String contents) throws IOException {

        if (!options.isSupportYamlAnchors()) {
            return Yaml.mapper().readTree(contents);
        }
        try {
            org.yaml.snakeyaml.Yaml yaml = null;
            if (options.isValidateYamlInput()) {
                yaml = new org.yaml.snakeyaml.Yaml(new CustomSnakeYamlConstructor());
            } else {
                yaml = new org.yaml.snakeyaml.Yaml(new SafeConstructor());
            }

            Object o = yaml.load(contents);
            if (options.isValidateYamlInput()) {
                boolean res = exceedsLimits(o, null, new Integer(0), new IdentityHashMap<Object, Long>());
                if (res) {
                    LOGGER.warn("Error converting snake-parsed yaml to JsonNode");
                    return Yaml.mapper().readTree(contents);
                }
            }
            JsonNode n =  Json.mapper().convertValue(o, JsonNode.class);
            return n;
        } catch (Throwable e) {
            LOGGER.warn("Error snake-parsing yaml content", e);
            return Yaml.mapper().readTree(contents);
        }
    }

    private static boolean exceedsLimits(Object o, Object parent, Integer depth, Map<Object, Long> visited) {

        if (o == null) return false;
        if (!(o instanceof List) && !(o instanceof Map)) return false;
        if (depth > options.getMaxYamlDepth()) {
            LOGGER.warn("snake-yaml result exceeds max depth {}; threshold can be increased if needed by setting system property `maxYamlDepth` to a higher value.", options.getMaxYamlDepth());
            return true;
        }
        int currentDepth = depth;
        if (visited.containsKey(o)) {
            Object target = parent;
            if (target == null) {
                target = o;
            }
            if (options.isYamlCycleCheck()) {
                boolean res = hasReference(o, target, new Integer(0), new IdentityHashMap<Object, Long>());
                if (res) {
                    return true;
                }
            }
            if (visited.get(o) > options.getMaxYamlReferences()) {
                LOGGER.warn("snake-yaml result exceeds max references {}; threshold can be increased if needed by setting system property `maxYamlReferences` to a higher value.", options.getMaxYamlReferences());
                return true;
            }
            visited.put(o, visited.get(o) + 1);

        } else {
            visited.put(o, 1L);
        }

        if (o instanceof Map) {
            for (Object k : ((Map) o).keySet()) {
                boolean res = exceedsLimits(k, o, currentDepth + 1, visited);
                if (res) {
                    return true;
                }
            }
            for (Object v : ((Map) o).values()) {
                boolean res = exceedsLimits(v, o, currentDepth + 1, visited);
                if (res) {
                    return true;
                }
            }

        } else if (o instanceof List) {
            for (Object v: ((List)o)) {
                boolean res = exceedsLimits(v, o, currentDepth + 1, visited);
                if (res) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasReference(Object o, Object target, Integer depth, Map<Object, Long> visited) {

        if (o == null || target == null) return false;
        if (!(o instanceof List) && !(o instanceof Map)) return false;
        if (!(target instanceof List) && !(target instanceof Map)) return false;
        if (depth > options.getMaxYamlDepth()) {
            LOGGER.warn("snake-yaml result exceeds max depth {}; threshold can be increased if needed by setting  system property `maxYamlDepth` to a higher value.", options.getMaxYamlDepth());
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
                 LOGGER.warn("detected cycle in snake-yaml result; cycle check can be disabled by setting system property `yamlCycleCheck` to false.");
                 return true;
            }
            boolean res = hasReference(v, target, currentDepth + 1, visited);
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
                throw new SnakeException("Exception safe-checking yaml content  (maxDepth " + options.getMaxYamlDepth() + ")", e);
            }
        }
    }
}
