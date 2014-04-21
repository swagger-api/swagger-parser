package com.wordnik.swagger.models;

/**
 * Created by ron on 11/04/14.
 */
public enum Method {
    GET("GET"), POST("POST"), PUT("PUT"), PATCH("PATCH"), DELETE("DELETE"), OPTIONS("OPTIONS");

    private final String method;

    Method(String method) {
         this.method = method;
    }
}
