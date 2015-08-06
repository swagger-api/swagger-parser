package io.swagger.parser;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.models.Swagger;
import io.swagger.models.auth.AuthorizationValue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

public class SwaggerParser {
    public Swagger read(String location) {
        return read(location, null, true);
    }

    public Swagger read(String location, List<AuthorizationValue> auths, boolean resolve) {
        if (location == null) {
            return null;
        }

        List<SwaggerParserExtension> parserExtensions = getExtensions();
        Swagger output;

        try {
            output = new Swagger20Parser().read(location, auths);
            if (output != null) {
                return new SwaggerResolver(output, auths, location).resolve();
            }
        } catch (IOException e) {
            // continue;
        }
        for (SwaggerParserExtension extension : parserExtensions) {
            try {
                output = extension.read(location, auths);
                if (output != null) {
                    return output;
                }
            } catch (IOException e) {
                if (System.getProperty("debugParser") != null) {
                    e.printStackTrace();
                }
                // continue to next parser
            }
        }
        return null;
    }

    public Swagger parse(String swaggerAsString) {
        return parse(swaggerAsString, null);
    }

    public Swagger parse(String swaggerAsString, List<AuthorizationValue> auths) {
        Swagger output;
        try {
            output = new Swagger20Parser().parse(swaggerAsString);
            if (output != null) {
                return new SwaggerResolver(output, auths, null).resolve();
            }
        } catch (IOException e) {
            // continue;
        }
        return null;
    }

    public Swagger read(JsonNode node) {
        return read(node, false);
    }

    public Swagger read(JsonNode node, boolean resolve) {
        if (node == null) {
            return null;
        }

        List<SwaggerParserExtension> parserExtensions = getExtensions();
        Swagger output = null;

        try {
            output = new Swagger20Parser().read(node);
            if (output != null) {
                if(resolve) {
                    return new SwaggerResolver(output, new ArrayList<AuthorizationValue>()).resolve();
                }
                else {
                    return output;
                }
            }
        } catch (IOException e) {
            // continue;
        }
        for (SwaggerParserExtension extension : parserExtensions) {
            try {
                output = extension.read(node);
                if (output != null) {
                    return output;
                }
            } catch (IOException e) {
                if (System.getProperty("debugParser") != null) {
                    e.printStackTrace();
                }
                // continue to next parser
            }
        }
        return null;
    }

    public List<SwaggerParserExtension> getExtensions() {
        ServiceLoader<SwaggerParserExtension> loader = ServiceLoader.load(SwaggerParserExtension.class);
        List<SwaggerParserExtension> output = new ArrayList<SwaggerParserExtension>();
        Iterator<SwaggerParserExtension> itr = loader.iterator();
        while (itr.hasNext()) {
            output.add(itr.next());
        }
        return output;
    }
}