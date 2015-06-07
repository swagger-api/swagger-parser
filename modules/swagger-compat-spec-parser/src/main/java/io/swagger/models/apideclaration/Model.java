package io.swagger.models.apideclaration;

import io.swagger.models.SwaggerBaseModel;

import java.util.List;
import java.util.Map;

public class Model extends SwaggerBaseModel {
    private String id = null;
    private String description = null;
    private List<String> required = null;
    private Map<String, ModelProperty> properties = null;
    private String discriminator = null;
    private List<String> subTypes = null;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getRequired() {
        return required;
    }

    public void setRequired(List<String> required) {
        this.required = required;
    }

    public Map<String, ModelProperty> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, ModelProperty> properties) {
        this.properties = properties;
    }

    public String getDiscriminator() {
        return discriminator;
    }

    public void setDiscriminator(String discriminator) {
        this.discriminator = discriminator;
    }

    public List<String> getSubTypes() {
        return subTypes;
    }

    public void setSubTypes(List<String> subTypes) {
        this.subTypes = subTypes;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Model {\n");
        sb.append("  id: ").append(id).append("\n");
        sb.append("  description: ").append(description).append("\n");
        sb.append("  required: ").append(required).append("\n");
        sb.append("  properties: ").append(properties).append("\n");
        sb.append("  discriminator: ").append(discriminator).append("\n");
        sb.append("  subTypes: ").append(subTypes).append("\n");
        sb.append("  extraFields: ").append(getExtraFields()).append("\n");
        sb.append("}\n");
        return sb.toString();
    }
}

