package io.swagger.models.resourcelisting;

import io.swagger.models.AuthorizationScope;
import io.swagger.models.AuthorizationType;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ron on 11/04/14.
 */
public class OAuth2Authorization extends Authorization {
    private List<AuthorizationScope> scopes = new ArrayList<>();
    private GrantTypes grantTypes;

    public OAuth2Authorization() {
        setType(AuthorizationType.OAUTH2);
    }

    public List<AuthorizationScope> getScopes() {
        return scopes;
    }

    public void setScopes(List<AuthorizationScope> scopes) {
        this.scopes = scopes;
    }

    public GrantTypes getGrantTypes() {
        return grantTypes;
    }

    public void setGrantTypes(GrantTypes grantTypes) {
        this.grantTypes = grantTypes;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class OAuth2Authorization {\n");
        sb.append("  type: ").append(getType()).append("\n");
        sb.append("  scopes: ").append(scopes).append("\n");
        sb.append("  grantTypes: ").append(grantTypes).append("\n");
        sb.append("  extraFields: ").append(getExtraFields()).append("\n");
        sb.append("}\n");
        return sb.toString();
    }
}
