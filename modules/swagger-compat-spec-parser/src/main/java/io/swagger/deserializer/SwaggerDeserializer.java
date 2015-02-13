package io.swagger.deserializer;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.report.MessageBuilder;

public interface SwaggerDeserializer<T> {
    T deserialize(JsonNode jsonNode, MessageBuilder messageBuilder);
}
