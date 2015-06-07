package io.swagger.models.resourcelisting;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.models.AuthorizationType;
import io.swagger.models.SwaggerBaseModel;


@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(name = "apiKey", value = ApiKeyAuthorization.class),
        @JsonSubTypes.Type(name = "basicAuth", value = BasicAuthorization.class),
        @JsonSubTypes.Type(name = "oauth2", value = OAuth2Authorization.class)
})
public abstract class Authorization extends SwaggerBaseModel {
    private AuthorizationType type = null;

    @JsonIgnore
    public AuthorizationType getType() {
        return type;
    }

    public void setType(AuthorizationType type) {
        this.type = type;
    }
}

