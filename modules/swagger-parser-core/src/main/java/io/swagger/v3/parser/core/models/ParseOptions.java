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
    private boolean validateInternalRefs = true;
    private boolean legacyYamlDeserialization = false;
    private boolean resolveRequestBody = false;
    private boolean resolveResponses = true;
    
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
     * If set to true, will help resolving the requestBody as inline, provided resolve is also set to true.
     * Default is false because of the existing behaviour.
     */
	public void setResolveRequestBody(boolean resolveRequestBody) {
		this.resolveRequestBody = resolveRequestBody;
	}
	
	public boolean isResolveResponses() {
		return resolveResponses;
	}

	/**
	 * If set to true, will help resolving the responses as inline, provided resolve is also set to true.
	 * Default is true because of the existing behaviour.
	 */
	public void setResolveResponses(boolean resolveResponses) {
		this.resolveResponses = resolveResponses;
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

    public void setValidateInternalRefs(boolean validateInternalRefs) {
        this.validateInternalRefs = validateInternalRefs;
    }

    public boolean isValidateInternalRefs() {
        return validateInternalRefs;
    }
}
