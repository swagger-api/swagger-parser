package io.swagger.v3.parser.reference;

import io.swagger.v3.oas.models.OpenAPI;

public interface Traverser {
    OpenAPI traverse(OpenAPI openAPI, Visitor visitor) throws Exception;
}
