package com.wordnik.swagger.models;

/**
 * Created by ron on 11/04/14.
 */
public enum PassAs {
    HEADEDR("header"), QUERY("query");

    private final String passAs;

    PassAs(String passAs) {
         this.passAs = passAs;
    }
}
