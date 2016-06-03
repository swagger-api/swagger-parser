package io.swagger.parser;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.models.auth.AuthorizationValue;
import io.swagger.parser.util.SwaggerDeserializationResult;

import java.util.List;

public interface SwaggerParserExtension {
    boolean supports (JsonNode node);
    SwaggerDeserializationResult parseContents(JsonNode node, List<AuthorizationValue> auth, String parentLocation, boolean resolve) throws UnparseableContentException;
}
