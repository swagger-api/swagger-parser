package io.swagger.io;

public class HeaderAuthentication implements Authentication {
    private String name;
    private String value;

    public HeaderAuthentication(String name, String value) {
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
        httpClient.addHeader(name, value);

    }

    @Override
    public String toString() {
        return "HeaderAuthentication{" +
                "name='" + name + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}
