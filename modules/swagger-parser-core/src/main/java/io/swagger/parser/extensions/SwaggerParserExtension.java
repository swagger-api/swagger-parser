package io.swagger.parser.extensions;

import io.swagger.parser.models.AuthorizationValue;
import io.swagger.parser.models.ParseOptions;
import io.swagger.parser.models.SwaggerParseResult;

import java.util.List;

public interface SwaggerParserExtension {
    SwaggerParseResult readLocation(String url, List<AuthorizationValue> auth, ParseOptions options);

    SwaggerParseResult readContents(String swaggerAsString, List<AuthorizationValue> auth, ParseOptions options);
}
