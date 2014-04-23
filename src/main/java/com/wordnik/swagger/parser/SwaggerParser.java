package com.wordnik.swagger.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.wordnik.swagger.io.Authentication;
import com.wordnik.swagger.models.apideclaration.ApiDeclaration;
import com.wordnik.swagger.models.resourcelisting.ResourceListing;
import com.wordnik.swagger.reader.SwaggerReader;
import com.wordnik.swagger.reader.SwaggerReaderConfiguration;
import com.wordnik.swagger.reader.SwaggerReaderFactory;
import com.wordnik.swagger.report.MessageBuilder;
import com.wordnik.swagger.report.Severity;

public class SwaggerParser {
    public ResourceListing read(String url, Authentication authentication) {
        MessageBuilder messageBuilder = new MessageBuilder();

        SwaggerReader swaggerReader = new SwaggerReaderFactory(new SwaggerReaderConfiguration()).newReader();

        JsonNode jsonNode = swaggerReader.read(url, authentication, messageBuilder);

        validateMessageReport(messageBuilder);

        return null;
    }

    public ApiDeclaration read(String url, String resourcePath, Authentication authentication) {
        return null;
    }

    private void validateMessageReport(MessageBuilder messageBuilder) {
        if (messageBuilder.getHighestSeverity() == Severity.ERROR) {
            throw new SwaggerException(messageBuilder.toString());
        }
    }
}
