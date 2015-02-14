package io.swagger.io;

public class NoAuthentication implements Authentication {
    @Override
    public void apply(HttpClient httpClient) {
        // Do nothing
    }
}
