package com.wordnik.swagger.models;

/**
 * Created by ron on 11/04/14.
 */
public enum ParamType {
    PATH("path"), QUERY("query"), BODY("body"), HEADER("header"), FORM("form");

    private final String paramType;

    ParamType(String paramType) {
         this.paramType = paramType;
    }

    @Override
    public String toString() {
        return paramType;
    }
}
