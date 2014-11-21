package com.wordnik.swagger.models;

import com.fasterxml.jackson.annotation.*;

import java.util.Map;
import java.util.HashMap;

/**
 * Created by ron on 11/04/14.
 */
public enum ParamType {
    PATH, QUERY, BODY, HEADER, FORM;

    private static Map<String, ParamType> names = new HashMap<String, ParamType>();

    static {
      names.put("path", PATH);
      names.put("query", QUERY);
      names.put("header", HEADER);
      names.put("body", BODY);
      names.put("form", FORM);
    }

    @JsonCreator
    public static ParamType forValue(String value) {
      return names.get(value.toLowerCase());
    }

    @JsonValue
    public String toValue() {
      for (Map.Entry<String, ParamType> entry : names.entrySet()) {
        if (entry.getValue() == this)
          return entry.getKey();
      }

      return null; // or fail
    }
}
