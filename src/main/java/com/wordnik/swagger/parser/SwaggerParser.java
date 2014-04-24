package com.wordnik.swagger.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.wordnik.swagger.deserializer.ApiDeclarationDeserializer;
import com.wordnik.swagger.deserializer.ResourceListingDeserializer;
import com.wordnik.swagger.io.Authentication;
import com.wordnik.swagger.io.NoAuthentication;
import com.wordnik.swagger.models.apideclaration.ApiDeclaration;
import com.wordnik.swagger.models.resourcelisting.ResourceListing;
import com.wordnik.swagger.reader.SwaggerReader;
import com.wordnik.swagger.reader.SwaggerReaderConfiguration;
import com.wordnik.swagger.reader.SwaggerReaderFactory;
import com.wordnik.swagger.report.Message;
import com.wordnik.swagger.report.MessageBuilder;
import com.wordnik.swagger.report.Severity;

import java.net.URI;
import java.net.URISyntaxException;

public class SwaggerParser {

    public ResourceListing read(String url) {
        return read(url, new NoAuthentication());
    }

    public ResourceListing read(String url, Authentication authentication) {
        MessageBuilder messageBuilder = new MessageBuilder();
        SwaggerReader swaggerReader = new SwaggerReaderFactory(new SwaggerReaderConfiguration()).newReader();

        JsonNode jsonNode = swaggerReader.read(url, authentication, messageBuilder);

        validateMessageReport(messageBuilder);

        ResourceListingDeserializer resourceListingDeserializer = new ResourceListingDeserializer();
        ResourceListing resourceListing = resourceListingDeserializer.deserialize(jsonNode, messageBuilder);

        validateMessageReport(messageBuilder);

        return resourceListing;
    }

    public ApiDeclaration read(String url, String resourcePath) {
        return read(url, resourcePath, new NoAuthentication());
    }

    public ApiDeclaration read(String url, String resourcePath, Authentication authentication) {
        MessageBuilder messageBuilder = new MessageBuilder();
        SwaggerReader swaggerReader = new SwaggerReaderFactory(new SwaggerReaderConfiguration()).newReader();

        JsonNode jsonNode = null;

        try {
            String resourceListingURL = getResourceListingURL(url, resourcePath);

            jsonNode = swaggerReader.read(resourceListingURL, authentication, messageBuilder);

        } catch (URISyntaxException e) {
            messageBuilder.append(new Message("", e.getMessage(), Severity.ERROR));
        }

        validateMessageReport(messageBuilder);

        ApiDeclarationDeserializer apiDeclarationDeserializer = new ApiDeclarationDeserializer();
        ApiDeclaration apiDeclaration = apiDeclarationDeserializer.deserialize(jsonNode, messageBuilder);

        validateMessageReport(messageBuilder);

        return apiDeclaration;
    }

    private String getResourceListingURL(String url, String resourcePath) throws URISyntaxException {
        String resourceListingUrl;

        URI uri = new URI(resourcePath);
        if (uri.isAbsolute()) {
            resourceListingUrl = resourcePath;
        } else {
            resourceListingUrl = url + resourcePath;
        }

        return resourceListingUrl;
    }

    private void validateMessageReport(MessageBuilder messageBuilder) {
        if (messageBuilder.getHighestSeverity() == Severity.ERROR) {
            throw new SwaggerException(messageBuilder.toString());
        }
    }
}
