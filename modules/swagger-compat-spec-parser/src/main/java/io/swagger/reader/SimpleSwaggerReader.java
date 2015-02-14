package io.swagger.reader;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.io.Authentication;
import io.swagger.io.HttpClient;
import io.swagger.report.Message;
import io.swagger.report.MessageBuilder;
import io.swagger.report.Severity;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;

final class SimpleSwaggerReader implements SwaggerReader {
    private final ObjectMapper objectMapper;

    SimpleSwaggerReader() {
        objectMapper = new ObjectMapper();
        objectMapper.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING);
    }

    @Override
    public JsonNode read(final String url, final Authentication authentication, MessageBuilder messageBuilder) {
        HttpClient httpClient = new io.swagger.io.HttpClient(url);
        JsonNode jsonNode = null;

        authentication.apply(httpClient);

        try {
            InputStream swaggerJson = httpClient.execute();

            jsonNode = objectMapper.readTree(swaggerJson);

        } catch (URISyntaxException | IOException e) {
            messageBuilder.append(new Message("", e.getMessage(), Severity.ERROR));
        }

        httpClient.close();

        return jsonNode;
    }
}
