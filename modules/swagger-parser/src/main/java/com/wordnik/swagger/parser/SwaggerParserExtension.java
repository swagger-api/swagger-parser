package com.wordnik.swagger.parser;

import com.wordnik.swagger.models.Swagger;

import java.io.IOException;

public interface SwaggerParserExtension {
  Swagger read(String input) throws IOException;
}