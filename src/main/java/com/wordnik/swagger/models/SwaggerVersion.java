package com.wordnik.swagger.models;

/**
 * Created by ron on 11/04/14.
 */
public enum SwaggerVersion {
    V1_0("1.0"), V1_1("1.1"), V1_2("1.2");

    private final String version;

    SwaggerVersion(String version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return version;
    }
}
