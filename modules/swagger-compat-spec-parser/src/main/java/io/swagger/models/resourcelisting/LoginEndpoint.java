package io.swagger.models.resourcelisting;

import io.swagger.models.SwaggerBaseModel;

public class LoginEndpoint extends SwaggerBaseModel {
    private String url = null;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class LoginEndpoint {\n");
        sb.append("  url: ").append(url).append("\n");
        sb.append("  extraFields: ").append(getExtraFields()).append("\n");
        sb.append("}\n");
        return sb.toString();
    }
}

