package io.swagger.parser;

import com.wordnik.swagger.models.Swagger;
import com.wordnik.swagger.models.auth.AuthorizationValue;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.List;

public interface SwaggerParserExtension {
    Swagger read(String location, List<AuthorizationValue> auths) throws IOException;
    Swagger read(JsonNode node) throws IOException;
}