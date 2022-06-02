package io.swagger.v3.parser.core.models;

import io.swagger.v3.oas.models.OpenAPI;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SwaggerParseResult {
    private List<String> messages = null;
    private OpenAPI openAPI;
    private boolean openapi31;

    public SwaggerParseResult messages(List<String> messages) {
        this.messages = messages;
        return this;
    }

    public List<String> getMessages() {
        return messages;
    }

    public void setMessages(List<String> messages) {
        this.messages = messages;
    }

    public SwaggerParseResult message(String message) {
        if (messages == null) {
            messages = new ArrayList<>();
        }
        messages.add(message);
        return this;
    }

    public SwaggerParseResult addMessages(List<String> messages) {
        if (this.messages == null) {
            this.messages = new ArrayList<>();
        }
        this.messages.addAll(messages);
        return this;
    }

    public OpenAPI getOpenAPI() {
        return openAPI;
    }

    public void setOpenAPI(OpenAPI openAPI) {
        this.openAPI = openAPI;
    }

    public static SwaggerParseResult ofError(String message){
        final SwaggerParseResult result = new SwaggerParseResult();
        result.setMessages(Collections.singletonList(message));
        return result;
    }

    public void setOpenapi31(boolean openapi31) {
        this.openapi31 = openapi31;
    }

    public SwaggerParseResult openapi31(boolean openapi31) {
        this.openapi31 = openapi31;
        return this;
    }

    public boolean isOpenapi31() {
        return this.openapi31;
    }
}
