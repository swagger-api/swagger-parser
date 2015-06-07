package io.swagger.models.resourcelisting;

import io.swagger.models.SwaggerBaseModel;
import io.swagger.models.SwaggerVersion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResourceListing extends SwaggerBaseModel {
    private SwaggerVersion swaggerVersion = null;
    private String apiVersion = null;
    private List<ApiListingReference> apis = new ArrayList<>();
    private Map<String, Authorization> authorizations = new HashMap<>();
    private ApiInfo info = null;

    public SwaggerVersion getSwaggerVersion() {
        return swaggerVersion;
    }

    public void setSwaggerVersion(SwaggerVersion swaggerVersion) {
        this.swaggerVersion = swaggerVersion;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public List<ApiListingReference> getApis() {
        return apis;
    }

    public void setApis(List<ApiListingReference> apis) {
        this.apis = apis;
    }

    public Map<String, Authorization> getAuthorizations() {
        return authorizations;
    }

    public void setAuthorizations(Map<String, Authorization> authorizations) {
        this.authorizations = authorizations;
    }

    public ApiInfo getInfo() {
        return info;
    }

    public void setInfo(ApiInfo info) {
        this.info = info;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class ResourceListing {\n");
        sb.append("  apiVersion: ").append(apiVersion).append("\n");
        sb.append("  swaggerVersion: ").append(swaggerVersion).append("\n");
        sb.append("  apis: ").append(apis).append("\n");
        sb.append("  authorizations: ").append(authorizations).append("\n");
        sb.append("  info: ").append(info).append("\n");
        sb.append("  extraFields: ").append(getExtraFields()).append("\n");
        sb.append("}\n");
        return sb.toString();
    }
}

