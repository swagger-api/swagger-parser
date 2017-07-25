package io.swagger.parser.validation;

import io.swagger.models.Swagger;

import java.util.List;

public interface SpecValidator {
    List<String> validate(Swagger spec);
}
