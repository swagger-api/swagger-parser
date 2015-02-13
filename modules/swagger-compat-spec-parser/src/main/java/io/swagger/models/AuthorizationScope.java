package io.swagger.models;

public class AuthorizationScope extends SwaggerBaseModel {
    private String scope = null;
    private String description = null;

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class AuthorizationScope {\n");
        sb.append("  scope: ").append(scope).append("\n");
        sb.append("  description: ").append(description).append("\n");
        sb.append("  extraFields: ").append(getExtraFields()).append("\n");
        sb.append("}\n");
        return sb.toString();
    }
}

