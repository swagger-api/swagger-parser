package io.swagger.parser;

import com.wordnik.swagger.models.Swagger;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

public interface SwaggerParserExtension {
  Swagger read(String input) throws IOException;
  Swagger read(JsonNode node) throws IOException;
}