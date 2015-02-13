package io.swagger.models.resourcelisting;

import io.swagger.models.SwaggerBaseModel;

public class GrantTypes extends SwaggerBaseModel {
    private ImplicitGrant implicit = null;
    private AuthorizationCodeGrant authorization_code = null;


    public ImplicitGrant getImplicit() {
        return implicit;
    }

    public void setImplicit(ImplicitGrant implicit) {
        this.implicit = implicit;
    }

    public AuthorizationCodeGrant getAuthorization_code() {
        return authorization_code;
    }

    public void setAuthorization_code(AuthorizationCodeGrant authorization_code) {
        this.authorization_code = authorization_code;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class GrantTypes {\n");
        sb.append("  implicit: ").append(implicit).append("\n");
        sb.append("  authorization_code: ").append(authorization_code).append("\n");
        sb.append("  extraFields: ").append(getExtraFields()).append("\n");
        sb.append("}\n");
        return sb.toString();
    }
}

