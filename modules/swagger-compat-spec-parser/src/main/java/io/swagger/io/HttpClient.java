package io.swagger.io;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

public class HttpClient {
    static Logger LOGGER = LoggerFactory.getLogger(HttpClient.class);

    private String baseUrl;
    private Map<String, String> queryParams = new HashMap<>();
    private Map<String, String> headers = new HashMap<>();
    private CloseableHttpClient httpClient;
    private CloseableHttpResponse response;


    public HttpClient(String url) {
        baseUrl = url;
    }

    public void addQueryParam(String name, String value) {
        queryParams.put(name, value);
    }

    public void addHeader(String name, String value) {
        headers.put(name, value);
    }

    public InputStream execute() throws URISyntaxException, IOException {
        httpClient = HttpClients.createDefault();

        URIBuilder uriBuilder = new URIBuilder(baseUrl);
        for (Map.Entry<String, String> queryParam : queryParams.entrySet()) {
            uriBuilder.addParameter(queryParam.getKey(), queryParam.getValue());
        }

        HttpGet httpGet = new HttpGet(uriBuilder.build());
        for (Map.Entry<String, String> header : headers.entrySet()) {
            httpGet.addHeader(header.getKey(), header.getValue());
        }

        response = httpClient.execute(httpGet);
        return response.getEntity().getContent();
    }

    public void close() {
        try {
            response.close();
        } catch (IOException | NullPointerException e) {
            LOGGER.error("failed to close", e);
        }

        try {
            httpClient.close();
        } catch (IOException | NullPointerException e) {
            LOGGER.error("failed to close", e);
        }
    }
}
