package io.swagger.parser;

import io.swagger.models.Swagger;
import io.swagger.models.auth.AuthorizationValue;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.List;

public interface SwaggerParserExtension {
    Swagger read(String location, List<AuthorizationValue> auths) throws IOException;
    Swagger read(JsonNode node) throws IOException;
}