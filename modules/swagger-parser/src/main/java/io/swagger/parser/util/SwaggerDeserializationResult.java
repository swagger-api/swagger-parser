package io.swagger.parser.util;

import io.swagger.models.Swagger;

import java.util.ArrayList;
import java.util.List;

public class SwaggerDeserializationResult {
    private Swagger swagger;
    private List<String> messages;

    public Swagger getSwagger() {
        return swagger;
    }

    public List<String> getMessages() {
        return messages;
    }

    public void setMessages(List<String> messages) {
        this.messages = messages;
    }

    public void setSwagger(Swagger swagger) {
        this.swagger = swagger;
    }

    public SwaggerDeserializationResult message(String message) {
        if(messages == null) {
            messages = new ArrayList<String>();
        }
        messages.add(message);
        return this;
    }
}
