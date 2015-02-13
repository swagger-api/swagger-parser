package io.swagger.models.resourcelisting;

import io.swagger.models.SwaggerBaseModel;

public class ImplicitGrant extends SwaggerBaseModel {
    private LoginEndpoint loginEndpoint;
    private String tokenName;

    public LoginEndpoint getLoginEndpoint() {
        return loginEndpoint;
    }

    public void setLoginEndpoint(LoginEndpoint loginEndpoint) {
        this.loginEndpoint = loginEndpoint;
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
        sb.append("class ImplicitGrant {\n");
        sb.append("  loginEndpoint: " + loginEndpoint);
        sb.append("  tokenName: " + tokenName);
        sb.append("  extraFields: ").append(getExtraFields()).append("\n");
        sb.append("}\n");
        return sb.toString();
    }
}

