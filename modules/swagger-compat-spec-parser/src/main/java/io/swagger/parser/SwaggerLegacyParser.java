package io.swagger.parser;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.deserializer.ApiDeclarationDeserializer;
import io.swagger.deserializer.ResourceListingDeserializer;
import io.swagger.io.Authentication;
import io.swagger.io.NoAuthentication;
import io.swagger.models.apideclaration.ApiDeclaration;
import io.swagger.models.resourcelisting.ResourceListing;
import io.swagger.reader.SwaggerReader;
import io.swagger.reader.SwaggerReaderConfiguration;
import io.swagger.reader.SwaggerReaderFactory;
import io.swagger.report.Message;
import io.swagger.report.MessageBuilder;
import io.swagger.report.Severity;
import io.swagger.transform.migrate.ApiDeclarationMigrator;
import io.swagger.transform.migrate.ResourceListingMigrator;
import io.swagger.validate.ApiDeclarationJsonValidator;
import io.swagger.validate.ResourceListingJsonValidator;

import java.net.URI;
import java.net.URISyntaxException;

public class SwaggerLegacyParser {

    public ResourceListing read(String url) {
        return read(url, new NoAuthentication());
    }

    public ResourceListing read(String url, Authentication authentication) {
        MessageBuilder messageBuilder = new MessageBuilder();
        SwaggerReader swaggerReader = new SwaggerReaderFactory(new SwaggerReaderConfiguration()).newReader();

        JsonNode jsonNode = swaggerReader.read(url, authentication, messageBuilder);

        validateMessageReport(messageBuilder);

        ResourceListingMigrator resourceListingMigrator = new ResourceListingMigrator();
        jsonNode = resourceListingMigrator.migrate(messageBuilder, jsonNode);

        validateMessageReport(messageBuilder);

        ResourceListingJsonValidator resourceListingJsonValidator = new ResourceListingJsonValidator();
        resourceListingJsonValidator.validate(messageBuilder, jsonNode);

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

        ApiDeclarationMigrator apiDeclarationMigrator = new ApiDeclarationMigrator();
        jsonNode = apiDeclarationMigrator.migrate(messageBuilder, jsonNode);

        validateMessageReport(messageBuilder);

        ApiDeclarationJsonValidator apiDeclarationJsonValidator = new ApiDeclarationJsonValidator();
        apiDeclarationJsonValidator.validate(messageBuilder, jsonNode);

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
        System.out.println(messageBuilder.getHighestSeverity());
        if (messageBuilder.getHighestSeverity() == Severity.ERROR) {
            throw new SwaggerException(messageBuilder.toString());
        }
    }
}
