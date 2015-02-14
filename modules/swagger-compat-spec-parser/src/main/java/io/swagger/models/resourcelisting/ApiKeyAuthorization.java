package io.swagger.models.resourcelisting;

import io.swagger.models.AuthorizationType;
import io.swagger.models.PassAs;

/**
 * Created by ron on 11/04/14.
 */
public class ApiKeyAuthorization extends Authorization {
    private PassAs passAs = null;
    private String keyname = null;

    public ApiKeyAuthorization() {
        setType(AuthorizationType.APIKEY);
    }

    public PassAs getPassAs() {
        return passAs;
    }

    public void setPassAs(PassAs passAs) {
        this.passAs = passAs;
    }

    public String getKeyname() {
        return keyname;
    }

    public void setKeyname(String keyname) {
        this.keyname = keyname;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class ApiKeyAuthorization {\n");
        sb.append("  type: ").append(getType()).append("\n");
        sb.append("  passAs: ").append(passAs).append("\n");
        sb.append("  keyname: ").append(keyname).append("\n");
        sb.append("  extraFields: ").append(getExtraFields()).append("\n");
        sb.append("}\n");
        return sb.toString();
    }
}
