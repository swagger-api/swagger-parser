package io.swagger.parser;

import io.swagger.parser.models.AuthorizationValue;
import io.swagger.parser.models.ParseOptions;
import io.swagger.parser.models.SwaggerParseResult;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

public class OpenAPIParser {
    public SwaggerParseResult readLocation(String url, List<AuthorizationValue> auth, ParseOptions options) {
        SwaggerParseResult output = null;

        for(io.swagger.parser.extensions.SwaggerParserExtension extension : getExtensions()) {
            output = extension.readLocation(url, transform(auth), options);
            if(output != null) {
                return output;
            }
        }

        return output;
    }

    public SwaggerParseResult readContents(String swaggerAsString, List<AuthorizationValue> auth, ParseOptions options) {
        SwaggerParseResult output = null;

        for(io.swagger.parser.extensions.SwaggerParserExtension extension : getExtensions()) {
            output = extension.readContents(swaggerAsString, transform(auth), options);
            if(output != null) {
                return output;
            }
        }

        return output;
    }

    protected List<io.swagger.parser.extensions.SwaggerParserExtension> getExtensions() {
        List<io.swagger.parser.extensions.SwaggerParserExtension> extensions = new ArrayList<>();

        ServiceLoader<io.swagger.parser.extensions.SwaggerParserExtension> loader = ServiceLoader.load(io.swagger.parser.extensions.SwaggerParserExtension.class);
        Iterator<io.swagger.parser.extensions.SwaggerParserExtension> itr = loader.iterator();
        while (itr.hasNext()) {
            extensions.add(itr.next());
        }
        return extensions;
    }

    /**
     * Transform the swagger-model version of AuthorizationValue into a parser-specific one, to avoid
     * dependencies across extensions
     *
     * @param input
     * @return
     */
    protected List<io.swagger.parser.models.AuthorizationValue> transform(List<AuthorizationValue> input) {
        if(input == null) {
            return null;
        }

        List<io.swagger.parser.models.AuthorizationValue> output = new ArrayList<>();

        for(AuthorizationValue value : input) {
            io.swagger.parser.models.AuthorizationValue v = new io.swagger.parser.models.AuthorizationValue();

            v.setKeyName(value.getKeyName());
            v.setValue(value.getValue());
            v.setType(value.getType());

            output.add(v);
        }

        return output;
    }
}
