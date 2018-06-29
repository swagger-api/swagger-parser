package io.swagger.v3.parser.core.models;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.ResolverCache;

import java.util.List;

public class SwaggerParseResult {
    private List<String> messages = null;
    private OpenAPI openAPI;
    private ResolverCache refsCache;

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

    public OpenAPI getOpenAPI() {
        return openAPI;
    }

    public void setOpenAPI(OpenAPI openAPI) {
        this.openAPI = openAPI;
    }

    public ResolverCache getRefsCache() {
        return refsCache;
    }

    public void setRefsCache(ResolverCache refsCache) {
        this.refsCache = refsCache;
    }
}
