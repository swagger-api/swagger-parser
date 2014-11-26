package io.swagger.models;

import com.fasterxml.jackson.annotation.*;

import java.util.Map;
import java.util.HashMap;

/**
 * Created by ron on 11/04/14.
 */
public enum SwaggerVersion {
    V1_0, V1_1, V1_2;

    private static Map<String, SwaggerVersion> names = new HashMap<String, SwaggerVersion>();

    static {
      names.put("1.0", V1_0);
      names.put("1.1", V1_1);
      names.put("1.2", V1_2);
    }

    @JsonCreator
    public static SwaggerVersion forValue(String value) {
      return names.get(value.toLowerCase());
    }

    @JsonValue
    public String toValue() {
      for (Map.Entry<String, SwaggerVersion> entry : names.entrySet()) {
        if (entry.getValue() == this)
          return entry.getKey();
      }

      return null; // or fail
    }
}
