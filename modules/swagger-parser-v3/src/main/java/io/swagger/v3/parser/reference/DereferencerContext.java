package io.swagger.v3.parser.reference;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.core.models.AuthorizationValue;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DereferencerContext {

    protected final OpenAPI openApi;

    protected final List<AuthorizationValue> auths;
    protected final String rootUri;
    protected final ParseOptions parseOptions;
    protected String providedBaseUri;
    protected SwaggerParseResult swaggerParseResult;
    protected boolean addParametersToEachOperation = true;
    protected String currentUri;

    private Map<String, Reference> referenceSet = new LinkedHashMap<>();

    public DereferencerContext(
            SwaggerParseResult swaggerParseResult,
            List<AuthorizationValue> auths,
            String rootUri,
            ParseOptions parseOptions,
            String providedBaseUri,
            Map<String, Reference> referenceSet,
            Boolean addParametersToEachOperation) {
        this.swaggerParseResult = swaggerParseResult;
        this.openApi = swaggerParseResult.getOpenAPI();
        this.auths = auths;
        this.rootUri = rootUri;
        this.currentUri = rootUri;
        this.parseOptions = parseOptions;
        this.providedBaseUri = providedBaseUri;
        this.addParametersToEachOperation = addParametersToEachOperation != null ? addParametersToEachOperation : true;
        this.referenceSet = referenceSet != null ? referenceSet : new LinkedHashMap<>();
    }

    public OpenAPI getOpenApi() {
        return openApi;
    }

    public List<AuthorizationValue> getAuths() {
        return auths;
    }

    public String getRootUri() {
        return rootUri;
    }

    public ParseOptions getParseOptions() {
        return parseOptions;
    }

    public String getProvidedBaseUri() {
        return providedBaseUri;
    }

    public SwaggerParseResult getSwaggerParseResult() {
        return swaggerParseResult;
    }

    public boolean isAddParametersToEachOperation() {
        return addParametersToEachOperation;
    }

    public void setAddParametersToEachOperation(boolean addParametersToEachOperation) {
        this.addParametersToEachOperation = addParametersToEachOperation;
    }

    public String getCurrentUri() {
        return currentUri;
    }

    public void setCurrentUri(String currentUri) {
        this.currentUri = currentUri;
    }

    public DereferencerContext providedBaseUri(String providedBaseUri) {
        this.providedBaseUri = providedBaseUri;
        return this;
    }

    public DereferencerContext swaggerParseResult(SwaggerParseResult swaggerParseResult) {
        this.swaggerParseResult = swaggerParseResult;
        return this;
    }

    public DereferencerContext addParametersToEachOperation(boolean addParametersToEachOperation) {
        this.addParametersToEachOperation = addParametersToEachOperation;
        return this;
    }

    public DereferencerContext currentUri(String currentUri) {
        this.currentUri = currentUri;
        return this;
    }

    public Map<String, Reference> getReferenceSet() {
        return referenceSet;
    }

    public void setReferenceSet(Map<String, Reference> referenceSet) {
        this.referenceSet = referenceSet;
    }

    public DereferencerContext referenceSet(Map<String, Reference> referenceSet) {
        this.referenceSet = referenceSet;
        return this;
    }
}
