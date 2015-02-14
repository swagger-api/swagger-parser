package io.swagger.models.apideclaration;


import io.swagger.models.AuthorizationScope;
import io.swagger.models.Method;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Operation extends ExtendedTypedObject {
    private Method method = null;
    private String summary = null;
    private String notes = null;
    private String nickname = null;
    private Map<String, List<AuthorizationScope>> authorizations = new HashMap<>();
    private List<Parameter> parameters = new ArrayList<>();
    private List<ResponseMessage> responseMessages = new ArrayList<>();
    private List<String> produces = new ArrayList<>();
    private List<String> consumes = new ArrayList<>();
    private Boolean deprecated = null;

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public Map<String, List<AuthorizationScope>> getAuthorizations() {
        return authorizations;
    }

    public void setAuthorizations(Map<String, List<AuthorizationScope>> authorizations) {
        this.authorizations = authorizations;
    }

    public List<Parameter> getParameters() {
        return parameters;
    }

    public void setParameters(List<Parameter> parameters) {
        this.parameters = parameters;
    }

    public List<ResponseMessage> getResponseMessages() {
        return responseMessages;
    }

    public void setResponseMessages(List<ResponseMessage> responseMessages) {
        this.responseMessages = responseMessages;
    }

    public List<String> getProduces() {
        return produces;
    }

    public void setProduces(List<String> produces) {
        this.produces = produces;
    }

    public List<String> getConsumes() {
        return consumes;
    }

    public void setConsumes(List<String> consumes) {
        this.consumes = consumes;
    }

    public Boolean getDeprecated() {
        return deprecated;
    }

    public void setDeprecated(Boolean deprecated) {
        this.deprecated = deprecated;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Operation {\n");
        sb.append("  method: ").append(method).append("\n");
        sb.append("  summary: ").append(summary).append("\n");
        sb.append("  notes: ").append(notes).append("\n");
        sb.append("  nickname: ").append(nickname).append("\n");
        sb.append("  produces: ").append(produces).append("\n");
        sb.append("  consumes: ").append(consumes).append("\n");
        sb.append("  authorizations: ").append(authorizations).append("\n");
        sb.append("  parameters: ").append(parameters).append("\n");
        sb.append("  responseMessages: ").append(responseMessages).append("\n");
        sb.append("  deprecated: ").append(deprecated).append("\n");
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

