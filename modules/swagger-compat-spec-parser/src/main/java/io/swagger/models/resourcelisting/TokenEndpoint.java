package io.swagger.models.resourcelisting;

import io.swagger.models.SwaggerBaseModel;

public class TokenEndpoint extends SwaggerBaseModel {
    private String url = null;
    private String tokenName = null;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTokenName() {
        return tokenName;
    }

    public void setTokenName(String tokenName) {
        this.tokenName = tokenName;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class TokenEndpoint {\n");
        sb.append("  url: ").append(url).append("\n");
        sb.append("  tokenName: ").append(tokenName).append("\n");
        sb.append("  extraFields: ").append(getExtraFields()).append("\n");
        sb.append("}\n");
        return sb.toString();
    }
}

