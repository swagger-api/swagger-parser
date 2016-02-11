package io.swagger.parser.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.util.Json;
import io.swagger.util.Yaml;

import java.io.IOException;

/**
 * Created by russellb337 on 7/14/15.
 */
public class DeserializationUtils {

    private DeserializationUtils() {
        throw new InstantiationError("Must not instantiate this class");
    }

    public static JsonNode deserializeIntoTree(String contents, String fileOrHost) {
        JsonNode result;

        try {
            if (fileOrHost.endsWith(".yaml")) {
                result = readYamlTree(contents);
            } else {
                result = Json.mapper().readTree(contents);
            }
        } catch (IOException e) {
            throw new RuntimeException("An exception was thrown while trying to deserialize the contents of " + fileOrHost + " into a JsonNode tree", e);
        }

        return result;
    }

    public static <T> T deserialize(Object contents, String fileOrHost, Class<T> expectedType) {
        T result;

        ObjectMapper mapper;
        boolean isYaml = false;

        if(fileOrHost.endsWith(".yaml")) {
            mapper = Yaml.mapper();
            isYaml = true;
        } else {
            mapper = Json.mapper();
        }

        try {
            if (contents instanceof String) {
                if (isYaml) {
                    result = readYamlValue((String) contents, expectedType);
                } else {
                    result = mapper.readValue((String) contents, expectedType);
                }
            } else {
                result = mapper.convertValue(contents, expectedType);
            }
        } catch (IOException e) {
            throw new RuntimeException("An exception was thrown while trying to deserialize the contents of " + fileOrHost + " into type " + expectedType, e);
        }

        return result;
    }

    public static JsonNode readYamlTree(String contents) {
        org.yaml.snakeyaml.Yaml yaml = new org.yaml.snakeyaml.Yaml();
        return Json.mapper().convertValue(yaml.load(contents), JsonNode.class);
    }

    public static <T> T readYamlValue(String contents, Class<T> expectedType) {
        org.yaml.snakeyaml.Yaml yaml = new org.yaml.snakeyaml.Yaml();
        return Json.mapper().convertValue(yaml.load(contents), expectedType);
    }
}
