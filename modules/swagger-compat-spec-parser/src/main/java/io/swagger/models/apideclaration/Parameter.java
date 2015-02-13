package io.swagger.models.apideclaration;


import io.swagger.models.ParamType;

public class Parameter extends ExtendedTypedObject {
    private ParamType paramType;
    private String name;
    private String description;
    private Boolean required;
    private Boolean allowMultiple;

    public ParamType getParamType() {
        return paramType;
    }

    public void setParamType(ParamType paramType) {
        this.paramType = paramType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getRequired() {
        return required;
    }

    public void setRequired(Boolean required) {
        this.required = required;
    }

    public Boolean getAllowMultiple() {
        return allowMultiple;
    }

    public void setAllowMultiple(Boolean allowMultiple) {
        this.allowMultiple = allowMultiple;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Parameter {\n");
        sb.append("   name: ").append(name).append("\n");
        sb.append("   description: ").append(description).append("\n");
        sb.append("   required: ").append(required).append("\n");
        sb.append("   paramType: ").append(paramType).append("\n");
        sb.append("   allowMultiple: ").append(allowMultiple).append("\n");
        sb.append("  type: ").append(getType()).append("\n");
        sb.append("  format: ").append(getFormat()).append("\n");
        sb.append("  $ref: ").append(getRef()).append("\n");
        sb.append("  defaultValue: ").append(getDefaultValue()).append("\n");
        sb.append("  enum: ").append(getEnumValues()).append("\n");
        sb.append("  minimum: ").append(getMinimum()).append("\n");
        sb.append("  maximum: ").append(getMaximum()).append("\n");
        sb.append("  items: ").append(getItems()).append("\n");
        sb.append("  uniqueItems: ").append(getUniqueItems()).append("\n");
        sb.append("  extraFields: ").append(getExtraFields()).append("\n");
        sb.append("}\n");
        return sb.toString();
    }
}

