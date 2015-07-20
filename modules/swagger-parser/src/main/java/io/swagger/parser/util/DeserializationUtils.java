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
    public static JsonNode deserializeIntoTree(String contents, String fileOrHost) {
        JsonNode result;

        try {
            if (fileOrHost.endsWith(".yaml")) {
                result = Yaml.mapper().readTree(contents);
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

        if(fileOrHost.endsWith(".yaml")) {
            mapper = Yaml.mapper();
        } else {
            mapper = Json.mapper();
        }

        try {
            if (contents instanceof String) {
                result = mapper.readValue((String) contents, expectedType);
            } else {
                result = mapper.convertValue(contents, expectedType);
            }
        } catch (IOException e) {
            throw new RuntimeException("An exception was thrown while trying to deserialize the contents of " + fileOrHost + " into type " + expectedType, e);
        }

        return result;
    }
}
