package io.swagger.models.resourcelisting;

import io.swagger.models.SwaggerBaseModel;

public class ApiListingReference extends SwaggerBaseModel {
    private String path = null;
    private String description = null;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
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
        sb.append("class ApiListingReference {\n");
        sb.append("  path: ").append(path).append("\n");
        sb.append("  description: ").append(description).append("\n");
        sb.append("  extraFields: ").append(getExtraFields()).append("\n");
        sb.append("}\n");
        return sb.toString();
    }
}

