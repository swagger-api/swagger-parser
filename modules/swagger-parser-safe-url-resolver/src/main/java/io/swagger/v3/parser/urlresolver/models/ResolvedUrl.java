package io.swagger.v3.parser.urlresolver.models;

public class ResolvedUrl {

    private String url;
    private String hostHeader;

    public ResolvedUrl(String url, String hostHeader) {
        this.url = url;
        this.hostHeader = hostHeader;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getHostHeader() {
        return hostHeader;
    }

    public void setHostHeader(String hostHeader) {
        this.hostHeader = hostHeader;
    }

    @Override
    public String toString() {
        return "ResolvedUrl{" +
                "url='" + url + '\'' +
                ", hostHeader='" + hostHeader + '\'' +
                '}';
    }
}
