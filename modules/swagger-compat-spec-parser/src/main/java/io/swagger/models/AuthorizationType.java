package io.swagger.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by ron on 11/04/14.
 */
public enum AuthorizationType {
    BASIC_AUTH, APIKEY, OAUTH2, PATCH, DELETE, OPTIONS;

    private static Map<String, AuthorizationType> names = new HashMap<String, AuthorizationType>();

    @JsonCreator
    public static AuthorizationType forValue(String value) {
        return names.get(value.toLowerCase());
    }

    @JsonValue
    public String toValue() {
        for (Map.Entry<String, AuthorizationType> entry : names.entrySet()) {
            if (entry.getValue() == this) {
                return entry.getKey();
            }
        }

        return null; // or fail
    }

    static {
        names.put("basicAuth", BASIC_AUTH);
        names.put("apiKey", APIKEY);
        names.put("oauth2", OAUTH2);
    }
}
