package io.swagger.io;

public class QueryParamAuthentication implements Authentication {
    private String name;
    private String value;

    public QueryParamAuthentication(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    @Override
    public void apply(HttpClient httpClient) {
        httpClient.addQueryParam(name, value);
    }

    @Override
    public String toString() {
        return "QueryParamAuthentication{" +
                "name='" + name + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}
