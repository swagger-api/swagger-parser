package io.swagger.models.resourcelisting;

import io.swagger.models.SwaggerBaseModel;

public class AuthorizationCodeGrant extends SwaggerBaseModel {
    private TokenRequestEndpoint tokenRequestEndpoint = null;
    private TokenEndpoint tokenEndpoint = null;

    public TokenRequestEndpoint getTokenRequestEndpoint() {
        return tokenRequestEndpoint;
    }

    public void setTokenRequestEndpoint(TokenRequestEndpoint tokenRequestEndpoint) {
        this.tokenRequestEndpoint = tokenRequestEndpoint;
    }

    public TokenEndpoint getTokenEndpoint() {
        return tokenEndpoint;
    }

    public void setTokenEndpoint(TokenEndpoint tokenEndpoint) {
        this.tokenEndpoint = tokenEndpoint;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class AuthorizationCodeGrant {\n");
        sb.append("  tokenRequestEndpoint: ").append(tokenRequestEndpoint).append("\n");
        sb.append("  tokenEndpoint: ").append(tokenEndpoint).append("\n");
        sb.append("  extraFields: ").append(getExtraFields()).append("\n");
        sb.append("}\n");
        return sb.toString();
    }
}

