package io.swagger.models.apideclaration;


public class ModelProperty extends ExtendedTypedObject {
    private String description = null;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class ModelProperty {\n");
        sb.append("  description: ").append(description).append("\n");
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

