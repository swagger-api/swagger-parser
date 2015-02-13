package io.swagger.models.reader;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.swagger.models.AuthorizationScope;
import io.swagger.report.Message;
import io.swagger.report.MessageBuilder;
import io.swagger.report.Severity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public abstract class SwaggerParser {
    ObjectMapper mapper = new ObjectMapper();


    protected SwaggerParser() {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    protected String readString(Object object) {
        String value = null;

        if (object != null) {
            value = object.toString();
        }

        return value;
    }

    protected Boolean readBoolean(Object object) {
        Boolean value = null;

        if (object != null) {
            // TODO: Handle parsing error
            value = Boolean.parseBoolean(object.toString());
        }

        return value;
    }

    protected Integer readInteger(Object object) {
        Integer value = null;

        if (object != null) {
            // TODO: Handle parsing error
            value = Integer.parseInt(object.toString());
        }

        return value;
    }

    protected List<AuthorizationScope> readAuthorizationScopes(List<Map<String, Object>> map, MessageBuilder messages) {
        List<AuthorizationScope> authorizationScopes = new ArrayList<>();

        for (Map<String, Object> object : map) {
            AuthorizationScope authorizationScope = new AuthorizationScope();

            Object scope = object.get("scope");
            if (scope != null) {
                authorizationScope.setScope(scope.toString());
            } else {
                messages.append(new Message("AuthorizationScopes.scopes.scope", "missing required scope", Severity.ERROR));
            }

            Object description = object.get("description");
            if (description != null) {
                authorizationScope.setDescription(description.toString());
            } else {
                messages.append(new Message("AuthorizationScopes.scopes.description", "missing description", Severity.RECOMMENDED));
            }

            authorizationScopes.add(authorizationScope);
        }

        return authorizationScopes;
    }
}