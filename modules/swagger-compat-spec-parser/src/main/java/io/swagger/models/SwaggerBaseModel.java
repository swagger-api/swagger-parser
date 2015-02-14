package io.swagger.models;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Created by ron on 21/04/14.
 */
public abstract class SwaggerBaseModel {
    @JsonUnwrapped
    ObjectNode extraFields = JsonNodeFactory.instance.objectNode();

    @JsonAnySetter
    public void addExtraField(String fieldName, JsonNode value) {
        extraFields.put(fieldName, value);
    }

    public ObjectNode getExtraFields() {
        return extraFields;
    }
}
