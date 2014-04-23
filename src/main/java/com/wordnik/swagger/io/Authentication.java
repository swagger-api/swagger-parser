package com.wordnik.swagger.io;

public interface Authentication {
    void apply(HttpClient httpClient);
}
