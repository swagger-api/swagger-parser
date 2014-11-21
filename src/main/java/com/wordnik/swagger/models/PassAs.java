package com.wordnik.swagger.models;

import com.fasterxml.jackson.annotation.*;

import java.util.Map;
import java.util.HashMap;

/**
 * Created by ron on 11/04/14.
 */
public enum PassAs {
    HEADER, QUERY;

    private static Map<String, PassAs> names = new HashMap<String, PassAs>();

    static {
      names.put("header", HEADER);
      names.put("query", QUERY);
    }

    @JsonCreator
    public static PassAs forValue(String value) {
      return names.get(value.toLowerCase());
    }

    @JsonValue
    public String toValue() {
      for (Map.Entry<String, PassAs> entry : names.entrySet()) {
        if (entry.getValue() == this)
          return entry.getKey();
      }

      return null; // or fail
    }
}
