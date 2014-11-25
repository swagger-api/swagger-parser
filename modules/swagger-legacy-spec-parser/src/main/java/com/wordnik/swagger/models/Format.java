package com.wordnik.swagger.models;

import com.fasterxml.jackson.annotation.*;

import java.util.Map;
import java.util.HashMap;

/**
 * Created by ron on 11/04/14.
 */
public enum Format {
    INT32, INT64, FLOAT, DOUBLE, BYTE, DATE, DATE_TIME;

    private static Map<String, Format> names = new HashMap<String, Format>();

    static {
      names.put("int32", INT32);
      names.put("int64", INT64);
      names.put("float", FLOAT);
      names.put("double", DOUBLE);
      names.put("byte", BYTE);
      names.put("date", DATE);
      names.put("date-time", DATE_TIME);
    }

    @JsonCreator
    public static Format forValue(String value) {
      return names.get(value.toLowerCase());
    }

    @JsonValue
    public String toValue() {
      for (Map.Entry<String, Format> entry : names.entrySet()) {
        if (entry.getValue() == this)
          return entry.getKey();
      }

      return null; // or fail
    }

    public String toString() {
      for (Map.Entry<String, Format> entry : names.entrySet()) {
        if (entry.getValue() == this)
          return entry.getKey().toLowerCase();
      }
      return null;
    }
}
