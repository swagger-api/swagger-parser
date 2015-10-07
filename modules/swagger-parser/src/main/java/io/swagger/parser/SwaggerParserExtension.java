package io.swagger.parser;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.models.Swagger;
import io.swagger.models.auth.AuthorizationValue;
import io.swagger.parser.util.SwaggerDeserializationResult;

import java.io.IOException;
import java.util.List;

public interface SwaggerParserExtension {
    SwaggerDeserializationResult readWithInfo(JsonNode node);
    SwaggerDeserializationResult readWithInfo(String location, List<AuthorizationValue> auths);
    Swagger read(String location, List<AuthorizationValue> auths) throws IOException;
    Swagger read(JsonNode node) throws IOException;
}