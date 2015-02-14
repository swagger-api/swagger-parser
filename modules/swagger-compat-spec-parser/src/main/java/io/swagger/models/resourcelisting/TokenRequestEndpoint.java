package io.swagger.models.resourcelisting;

import io.swagger.models.SwaggerBaseModel;

public class TokenRequestEndpoint extends SwaggerBaseModel {
    private String url = null;
    private String clientIdName = null;
    private String clientSecretName = null;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getClientIdName() {
        return clientIdName;
    }

    public void setClientIdName(String clientIdName) {
        this.clientIdName = clientIdName;
    }

    public String getClientSecretName() {
        return clientSecretName;
    }

    public void setClientSecretName(String clientSecretName) {
        this.clientSecretName = clientSecretName;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class TokenRequestEndpoint {\n");
        sb.append("  url: ").append(url).append("\n");
        sb.append("  clientIdName: ").append(clientIdName).append("\n");
        sb.append("  clientSecretName: ").append(clientSecretName).append("\n");
        sb.append("  extraFields: ").append(getExtraFields()).append("\n");
        sb.append("}\n");
        return sb.toString();
    }
}

