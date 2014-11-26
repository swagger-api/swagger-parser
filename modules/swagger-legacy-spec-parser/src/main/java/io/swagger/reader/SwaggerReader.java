package io.swagger.reader;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.io.Authentication;
import io.swagger.report.MessageBuilder;

public interface SwaggerReader {
    JsonNode read(String url, Authentication authentication, MessageBuilder messageBuilder);
}
