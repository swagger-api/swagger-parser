package io.swagger.v3.parser.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.util.Json31;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Yaml31;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;

/**
 * Created by russellb337 on 7/14/15.
 */
public class DeserializationUtils {
    public static JsonNode deserializeIntoTree(String contents, String fileOrHost) {
        return deserializeIntoTree(contents, fileOrHost, false);
    }

    public static JsonNode deserializeIntoTree(String contents, String fileOrHost, boolean openapi31) {
        JsonNode result;

        try {
            if (isJson(contents)) {
                result = openapi31 ? Json31.mapper().readTree(contents) : Json.mapper().readTree(contents);
            } else {
                result = readYamlTree(contents, openapi31);
            }
        } catch (IOException e) {
            throw new RuntimeException("An exception was thrown while trying to deserialize the contents of " + fileOrHost + " into a JsonNode tree", e);
        }

        return result;
    }

    public static <T> T deserialize(Object contents, String fileOrHost, Class<T> expectedType) {
        return deserialize(contents, fileOrHost, expectedType, false);
    }
    public static <T> T deserialize(Object contents, String fileOrHost, Class<T> expectedType, boolean openapi31) {
        T result;

        boolean isJson = false;

        if (contents instanceof String && isJson((String)contents)) {
            isJson = true;
        }

        try {
            if (contents instanceof String) {
                ObjectMapper mapper = Json.mapper();
                if (isJson) {
                    if (openapi31) {
                        mapper = Json31.mapper();
                    }
                } else {
                    if (openapi31) {
                        mapper = Yaml31.mapper();
                    } else {
                        mapper = Yaml.mapper();
                    }
                }
                result = mapper.readValue((String) contents, expectedType);
            } else {
                ObjectMapper mapper = openapi31 ? Json31.mapper() : Json.mapper();
                result = mapper.convertValue(contents, expectedType);
            }
        } catch (Exception e) {
            throw new RuntimeException("An exception was thrown while trying to deserialize the contents of " + fileOrHost + " into type " + expectedType, e);
        }

        return result;
    }

    private static boolean isJson(String contents) {
        return contents.toString().trim().startsWith("{");
    }

    public static JsonNode readYamlTree(String contents) {
        return readYamlTree(contents, false);
    }

    public static JsonNode readYamlTree(String contents, boolean openapi31) {
        org.yaml.snakeyaml.Yaml yaml = new org.yaml.snakeyaml.Yaml(new SafeConstructor());
        ObjectMapper mapper = openapi31 ? Json31.mapper() : Json.mapper();
        return mapper.convertValue(yaml.load(contents), JsonNode.class);
    }

    public static <T> T readYamlValue(String contents, Class<T> expectedType) {
        return readYamlValue(contents, expectedType, false);
    }

    public static <T> T readYamlValue(String contents, Class<T> expectedType, boolean openapi31) {
        org.yaml.snakeyaml.Yaml yaml = new org.yaml.snakeyaml.Yaml(new SafeConstructor());
        ObjectMapper mapper = openapi31 ? Json31.mapper() : Json.mapper();
        return mapper.convertValue(yaml.load(contents), expectedType);
    }
}
