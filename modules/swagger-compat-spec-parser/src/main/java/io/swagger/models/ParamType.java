package io.swagger.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by ron on 11/04/14.
 */
public enum ParamType {
    PATH, QUERY, BODY, HEADER, FORM;

    private static Map<String, ParamType> names = new HashMap<String, ParamType>();

    @JsonCreator
    public static ParamType forValue(String value) {
        return names.get(value.toLowerCase());
    }

    @JsonValue
    public String toValue() {
        for (Map.Entry<String, ParamType> entry : names.entrySet()) {
            if (entry.getValue() == this) {
                return entry.getKey();
            }
        }

        return null; // or fail
    }

    static {
        names.put("path", PATH);
        names.put("query", QUERY);
        names.put("header", HEADER);
        names.put("body", BODY);
        names.put("form", FORM);
    }
}
