package io.swagger.v3.parser.reference;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.parser.core.models.AuthorizationValue;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Reference {
    private String uri ;
    private int depth = 0;
    private Object value;
    private Set<String> messages = new HashSet<>();
    private Map<String, Reference> referenceSet;
    private JsonNode jsonNode;

    private List<AuthorizationValue> auths;

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public Set<String> getMessages() {
        return messages;
    }

    public void setMessages(Set<String> messages) {
        this.messages = messages;
    }

    public Map<String, Reference> getReferenceSet() {
        return referenceSet;
    }

    public void setReferenceSet(Map<String, Reference> referenceSet) {
        this.referenceSet = referenceSet;
    }

    public Reference uri(String uri) {
        this.uri = uri;
        return this;
    }

    public Reference depth(int depth) {
        this.depth = depth;
        return this;
    }

    public Reference value(Object value) {
        this.value = value;
        return this;
    }

    public Reference messages(Set<String> messages) {
        this.messages = messages;
        return this;
    }

    public Reference referenceSet(Map<String, Reference> referenceSet) {
        this.referenceSet = referenceSet;
        return this;
    }

    public List<AuthorizationValue> getAuths() {
        return auths;
    }

    public void setAuths(List<AuthorizationValue> auths) {
        this.auths = auths;
    }

    public Reference auths(List<AuthorizationValue> auths) {
        this.auths = auths;
        return this;
    }

    public JsonNode getJsonNode() {
        return jsonNode;
    }

    public void setJsonNode(JsonNode jsonNode) {
        this.jsonNode = jsonNode;
    }

    public Reference jsonNode(JsonNode jsonNode) {
        this.jsonNode = jsonNode;
        return this;
    }
}
