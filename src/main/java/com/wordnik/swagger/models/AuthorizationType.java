package com.wordnik.swagger.models;

/**
 * Created by ron on 11/04/14.
 */
public enum AuthorizationType {
    BASIC_AUTH("basicAuth"), APIKEY("apiKey"), OAUTH2("oauth2");

    private final String type;

    AuthorizationType(String type) {
         this.type = type;
    }

    @Override
    public String toString() {
        return type;
    }
}
