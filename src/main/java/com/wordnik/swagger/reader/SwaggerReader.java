package com.wordnik.swagger.reader;

import com.fasterxml.jackson.databind.JsonNode;
import com.wordnik.swagger.io.Authentication;
import com.wordnik.swagger.report.MessageBuilder;

public interface SwaggerReader {
    JsonNode read(String url, Authentication authentication, MessageBuilder messageBuilder);
}
