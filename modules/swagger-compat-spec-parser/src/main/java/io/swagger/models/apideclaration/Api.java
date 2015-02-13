package io.swagger.models.apideclaration;

import io.swagger.models.SwaggerBaseModel;

import java.util.ArrayList;
import java.util.List;


public class Api extends SwaggerBaseModel {
    private String path;
    private String description;
    private List<Operation> operations = new ArrayList<>();

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

    public List<Operation> getOperations() {
        return operations;
    }

    public void setOperations(List<Operation> operations) {
        this.operations = operations;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class ApiDescription {\n");
        sb.append("  path: ").append(path).append("\n");
        sb.append("  description: ").append(description).append("\n");
        sb.append("  operations: ").append(operations).append("\n");
        sb.append("  extraFields: ").append(getExtraFields()).append("\n");
        sb.append("}\n");
        return sb.toString();
    }
}