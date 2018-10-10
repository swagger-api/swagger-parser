package io.swagger.v3.parser.core.extensions;

import io.swagger.v3.parser.core.models.AuthorizationValue;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;

import java.util.List;

public interface SwaggerParserExtension {
    SwaggerParseResult readLocation(String url, List<AuthorizationValue> auth, ParseOptions options);

    SwaggerParseResult readContents(String swaggerAsString, List<AuthorizationValue> auth, ParseOptions options);
}
