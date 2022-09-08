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

    private boolean nameInlineResponsesBasedOnEndpoint;

    public boolean isNameInlineResponsesBasedOnEndpoint() {
        return nameInlineResponsesBasedOnEndpoint;
    }

    public void setNameInlineResponsesBasedOnEndpoint(boolean isNameInlineResponsesBasedOnEndpoint) {
        this.nameInlineResponsesBasedOnEndpoint = isNameInlineResponsesBasedOnEndpoint;
    }
}
