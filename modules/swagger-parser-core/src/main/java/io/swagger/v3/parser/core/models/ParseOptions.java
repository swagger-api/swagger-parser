package io.swagger.v3.parser.core.models;

import java.util.List;

public class ParseOptions {
    private boolean resolve;
    private boolean resolveCombinators = true;
    private boolean resolveFully;
    private boolean flatten;
    private boolean flattenComposedSchemas;
    private boolean camelCaseFlattenNaming;
    private boolean skipMatches;
    private boolean allowEmptyStrings = true;
    private boolean validateExternalRefs = false;
    private boolean validateInternalRefs = true;
    private boolean legacyYamlDeserialization = false;
    private boolean resolveRequestBody = false;

    private boolean oaiAuthor;
    private boolean inferSchemaType = true;
    private boolean safelyResolveURL;
    private List<String> allowList;
    private List<String> blockList;


    public boolean isResolve() {
        return resolve;
    }

    public void setResolve(boolean resolve) {
        this.resolve = resolve;
    }

    public boolean isResolveCombinators() {
        return resolveCombinators;
    }

    public void setResolveCombinators(boolean resolveCombinators) {
        this.resolveCombinators = resolveCombinators;
    }

    public boolean isResolveFully() {
        return resolveFully;
    }

    public void setResolveFully(boolean resolveFully) {
        this.resolveFully = resolveFully;
    }

    public boolean isResolveRequestBody() {
        return resolveRequestBody;
    }

    /**
     * If set to true, will help resolving the requestBody as inline, provided
     * resolve is also set to true. Default is false because of the existing
     * behaviour.
     */
    public void setResolveRequestBody(boolean resolveRequestBody) {
        this.resolveRequestBody = resolveRequestBody;
    }

    public boolean isFlatten() { return flatten; }

    public void setFlatten(boolean flatten) { this.flatten = flatten; }

    public boolean isSkipMatches() {
        return skipMatches;
    }

    public void setSkipMatches(boolean skipMatches) {
        this.skipMatches = skipMatches;
    }

    public boolean isFlattenComposedSchemas() {
        return flattenComposedSchemas;
    }

    public void setFlattenComposedSchemas(boolean flattenComposedSchemas) {
        this.flattenComposedSchemas = flattenComposedSchemas;
    }
    public boolean isCamelCaseFlattenNaming() {
        return camelCaseFlattenNaming;
    }

    public void setCamelCaseFlattenNaming(boolean camelCaseFlattenNaming) {
        this.camelCaseFlattenNaming = camelCaseFlattenNaming;
    }

    public boolean isAllowEmptyString() {
        return allowEmptyStrings;
    }

    public void setAllowEmptyString(boolean allowEmptyStrings) {
        this.allowEmptyStrings = allowEmptyStrings;
    }

    public boolean isValidateExternalRefs() {
        return validateExternalRefs;
    }

    public void setValidateExternalRefs(boolean validateExternalRefs) {
        this.validateExternalRefs = validateExternalRefs;
    }

    /**
     * if set to true, triggers YAML deserialization as done up to 2.0.30, not supporting YAML Anchors safe resolution.
     */
    public boolean isLegacyYamlDeserialization() {
        return legacyYamlDeserialization;
    }

    public void setLegacyYamlDeserialization(boolean legacyYamlDeserialization) {
        this.legacyYamlDeserialization = legacyYamlDeserialization;
    }

    public void setOaiAuthor(boolean oaiAuthor) {
        this.oaiAuthor = oaiAuthor;
    }

    public boolean isOaiAuthor() {
        return oaiAuthor;
    }

    public void setValidateInternalRefs(boolean validateInternalRefs) {
        this.validateInternalRefs = validateInternalRefs;
    }

    public boolean isValidateInternalRefs() {
        return validateInternalRefs;
    }

    public boolean isInferSchemaType() {
        return inferSchemaType;
    }

    public void setInferSchemaType(boolean inferSchemaType) {
        this.inferSchemaType = inferSchemaType;
    }

    public boolean isSafelyResolveURL() {
        return safelyResolveURL;
    }

    public void setSafelyResolveURL(boolean safelyResolveURL) {
        this.safelyResolveURL = safelyResolveURL;
    }

    public List<String> getAllowList() {
        return allowList;
    }

    public void setAllowList(List<String> allowList) {
        this.allowList = allowList;
    }

    public List<String> getBlockList() {
        return blockList;
    }

    public void setBlockList(List<String> blockList) {
        this.blockList = blockList;
    }
}
