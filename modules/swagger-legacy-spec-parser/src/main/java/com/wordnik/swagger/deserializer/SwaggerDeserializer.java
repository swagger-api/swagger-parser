package com.wordnik.swagger.deserializer;

import com.fasterxml.jackson.databind.JsonNode;
import com.wordnik.swagger.report.MessageBuilder;

public interface SwaggerDeserializer<T> {
    T deserialize(JsonNode jsonNode, MessageBuilder messageBuilder);
}
