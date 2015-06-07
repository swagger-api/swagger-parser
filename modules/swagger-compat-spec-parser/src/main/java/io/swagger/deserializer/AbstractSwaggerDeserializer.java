package io.swagger.deserializer;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.report.Message;
import io.swagger.report.MessageBuilder;
import io.swagger.report.Severity;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;

public abstract class AbstractSwaggerDeserializer<T> implements SwaggerDeserializer<T> {
    @SuppressWarnings({"unchecked"})
    protected final Class<T> clazz = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    protected final ObjectMapper objectMapper;

    protected AbstractSwaggerDeserializer() {
        objectMapper = new ObjectMapper();
        objectMapper.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING);
    }

    @Override
    public T deserialize(JsonNode jsonNode, MessageBuilder messageBuilder) {
        T deserializedObject = null;

        try {
            deserializedObject = objectMapper.readValue(jsonNode.traverse(), clazz);
        } catch (JsonMappingException e) {
            messageBuilder.append(new Message(e.getPathReference(), e.getMessage(), Severity.ERROR));
        } catch (IOException e) {
            messageBuilder.append(new Message("", e.getMessage(), Severity.ERROR));
        }

        return deserializedObject;
    }
}
