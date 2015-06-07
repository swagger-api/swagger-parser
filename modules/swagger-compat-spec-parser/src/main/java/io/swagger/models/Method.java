package io.swagger.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by ron on 11/04/14.
 */
public enum Method {
    GET, POST, PUT, PATCH, DELETE, OPTIONS, HEAD;

    private static Map<String, Method> names = new HashMap<String, Method>();

    @JsonCreator
    public static Method forValue(String value) {
        return names.get(value.toLowerCase());
    }

    @JsonValue
    public String toValue() {
        for (Map.Entry<String, Method> entry : names.entrySet()) {
            if (entry.getValue() == this) {
                return entry.getKey();
            }
        }

        return null; // or fail
    }

    static {
        names.put("get", GET);
        names.put("put", PUT);
        names.put("post", POST);
        names.put("delete", DELETE);
        names.put("patch", PATCH);
        names.put("options", OPTIONS);
        names.put("head", HEAD);
    }
}
