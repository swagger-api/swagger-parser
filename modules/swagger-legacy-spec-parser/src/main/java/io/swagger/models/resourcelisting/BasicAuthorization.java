package io.swagger.models.resourcelisting;

import io.swagger.models.AuthorizationType;

/**
 * Created by ron on 11/04/14.
 */
public class BasicAuthorization extends Authorization {

    public BasicAuthorization() {
        setType(AuthorizationType.BASIC_AUTH);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class BasicAuthorization {\n");
        sb.append("  type: ").append(getType()).append("\n");
        sb.append("  extraFields: ").append(getExtraFields()).append("\n");
        sb.append("}\n");
        return sb.toString();
    }
}
