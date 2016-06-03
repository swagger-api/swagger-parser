package io.swagger.parser;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.models.Swagger;
import io.swagger.models.auth.AuthorizationValue;
import io.swagger.parser.util.*;

import java.util.List;

public class Swagger20Parser implements SwaggerParserExtension {

    @Override
    public boolean supports(final JsonNode node) {
        // Supports swagger v2.x.
        String version = node.path("swagger").asText("0").trim();
        return version.equals("2") || version.startsWith("2.");
    }

    @Override
    public SwaggerDeserializationResult parseContents(final JsonNode node, final List<AuthorizationValue> auth, final String parentLocation, final boolean resolve) throws UnparseableContentException {
        if (!supports(node)) {
            // BUG: somebody is calling me with a spec I don't support.
            throw new UnsupportedOperationException("This is not a swagger 2.0 spec.");
        }

        SwaggerDeserializationResult result = new SwaggerDeserializer().deserialize(node);

        if(result != null && result.getSwagger() != null) {
            Swagger resolved = new SwaggerResolver(result.getSwagger(), auth, parentLocation).resolve();
            if(resolved != null) {
                result.setSwagger(resolved);
            }
        }
        return result;
    }
}
