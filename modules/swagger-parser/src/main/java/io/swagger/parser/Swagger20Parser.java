package io.swagger.parser;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.models.Swagger;
import io.swagger.models.auth.AuthorizationValue;
import io.swagger.parser.util.*;

import java.util.List;

public class Swagger20Parser implements SwaggerParserExtension {

    @Override
    public SwaggerDeserializationResult parseContents(final JsonNode node, final List<AuthorizationValue> auth, final String parentLocation, final boolean resolve) throws UnparseableContentException {
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
