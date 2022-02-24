package io.swagger.v3.parser.core.models;

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
    private boolean legacyYamlDeserialization = false;

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
}
