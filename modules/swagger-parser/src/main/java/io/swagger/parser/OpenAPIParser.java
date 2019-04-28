package io.swagger.parser;

import io.swagger.v3.parser.core.extensions.SwaggerParserExtension;
import io.swagger.v3.parser.core.models.AuthorizationValue;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import io.swagger.v3.parser.OpenAPIV3Parser;

import java.util.List;

public class OpenAPIParser {
    public SwaggerParseResult readLocation(String url, List<AuthorizationValue> auth, ParseOptions options) {
        SwaggerParseResult output = null;

        for(SwaggerParserExtension extension : OpenAPIV3Parser.getExtensions()) {
            output = extension.readLocation(url, auth, options);
            if(output != null && output.getOpenAPI() != null) {
                return output;
            }
        }

        return output;
    }

    public SwaggerParseResult readContents(String swaggerAsString, List<AuthorizationValue> auth, ParseOptions options) {
        SwaggerParseResult output = null;

        for(SwaggerParserExtension extension : OpenAPIV3Parser.getExtensions()) {
            output = extension.readContents(swaggerAsString, auth, options);
            if(output != null && output.getOpenAPI() != null) {
                return output;
            }
        }

        return output;
    }

}
