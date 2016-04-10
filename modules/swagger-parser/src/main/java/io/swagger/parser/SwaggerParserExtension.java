package io.swagger.parser;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.models.auth.AuthorizationValue;
import io.swagger.parser.util.SwaggerDeserializationResult;

import java.util.List;

public interface SwaggerParserExtension {
    SwaggerDeserializationResult parseContents(JsonNode node) throws UnparseableContentException;
    SwaggerDeserializationResult parseContents(JsonNode node, List<AuthorizationValue> auth, boolean resolve) throws UnparseableContentException;
    SwaggerDeserializationResult parseLocation(String location) throws UnparseableContentException;
    SwaggerDeserializationResult parseLocation(String location, List<AuthorizationValue> auths, boolean resolve) throws UnparseableContentException;
}