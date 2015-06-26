package io.swagger.models.apideclaration;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.models.AuthorizationScope;
import io.swagger.models.SwaggerBaseModel;
import io.swagger.models.resourcelisting.ApiListingReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApiDeclaration extends SwaggerBaseModel {
    private String swaggerVersion = null;
    private String apiVersion = null;
    private String basePath = null;
    private String resourcePath = null;
    private List<Api> apis = new ArrayList<>();
    private Map<String, Model> models = new HashMap<>();
    private List<String> produces = new ArrayList<>();
    private List<String> consumes = new ArrayList<>();
    private Map<String, List<AuthorizationScope>> authorizations = new HashMap<>();
    @JsonIgnore
    private ApiListingReference apiListingRef;

    public String getSwaggerVersion() {
        return swaggerVersion;
    }

    public void setSwaggerVersion(String swaggerVersion) {
        this.swaggerVersion = swaggerVersion;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public String getResourcePath() {
        return resourcePath;
    }

    public void setResourcePath(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    public List<Api> getApis() {
        return apis;
    }

    public void setApis(List<Api> apis) {
        this.apis = apis;
    }

    public Map<String, Model> getModels() {
        return models;
    }

    public void setModels(Map<String, Model> models) {
        this.models = models;
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

    public Map<String, List<AuthorizationScope>> getAuthorizations() {
        return authorizations;
    }

    public void setAuthorizations(Map<String, List<AuthorizationScope>> authorizations) {
        this.authorizations = authorizations;
    }

    public ApiListingReference getApiListingRef() {
        return this.apiListingRef;
    }

    public void setApiListingRef(ApiListingReference ref) {
        this.apiListingRef = ref;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class ApiDescription {\n");
        sb.append("  swaggerVersion: ").append(swaggerVersion).append("\n");
        sb.append("  apiVersion: ").append(apiVersion).append("\n");
        sb.append("  basePath: ").append(basePath).append("\n");
        sb.append("  resourcePath: ").append(resourcePath).append("\n");
        sb.append("  apis: ").append(apis).append("\n");
        sb.append("  models: ").append(models).append("\n");
        sb.append("  produces: ").append(produces).append("\n");
        sb.append("  consumes: ").append(consumes).append("\n");
        sb.append("  authorizations: ").append(authorizations).append("\n");
        sb.append("  extraFields: ").append(getExtraFields()).append("\n");
        sb.append("}\n");
        return sb.toString();
    }
}

