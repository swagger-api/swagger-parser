package io.swagger.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.models.Swagger;
import io.swagger.models.auth.AuthorizationValue;
import io.swagger.parser.util.DeserializationUtils;
import io.swagger.parser.util.SwaggerDeserializationResult;
import io.swagger.util.Json;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SwaggerParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(SwaggerParser.class);
    private static final String I_O_EXCEPTION_HAS_OCCURRED = "I/O Exception of some sort has occurred";

    public SwaggerDeserializationResult readWithInfo(String location, List<AuthorizationValue> auths, boolean resolve) {
        if (location == null) {
            return null;
        }
        location = location.replaceAll("\\\\","/");
        List<SwaggerParserExtension> parserExtensions = getExtensions();
        SwaggerDeserializationResult output;

        if(auths == null) {
            auths = new ArrayList<AuthorizationValue>();
        }

        output = new Swagger20Parser().readWithInfo(location, auths);
        if (output != null) {
            if(output.getSwagger() != null && "2.0".equals(output.getSwagger().getSwagger())) {
                if(resolve) {
                    output.setSwagger(new SwaggerResolver(output.getSwagger(), auths, location).resolve());
                }
                return output;
            }
        }
        for (SwaggerParserExtension extension : parserExtensions) {
            output = extension.readWithInfo(location, auths);
            if (output != null && output.getSwagger() != null && "2.0".equals(output.getSwagger().getSwagger())) {
                return output;
            }
        }
        output = new SwaggerDeserializationResult();
        output.message("The swagger definition could not be read");
        return output;
    }

    public Swagger read(String location) {
        return read(location, null, true);
    }

    public Swagger read(String location, List<AuthorizationValue> auths, boolean resolve) {
        if (location == null) {
            return null;
        }
        location = location.replaceAll("\\\\","/");
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
                    LOGGER.error(I_O_EXCEPTION_HAS_OCCURRED, e);
                }
                // continue to next parser
            }
        }
        return null;
    }

    public SwaggerDeserializationResult readWithInfo(String swaggerAsString) {
        if(swaggerAsString == null) {
            return new SwaggerDeserializationResult().message("empty or null swagger supplied");
        }
        try {
            JsonNode node;
            if (swaggerAsString.trim().startsWith("{")) {
                ObjectMapper mapper = Json.mapper();
                node = mapper.readTree(swaggerAsString);
            } else {
                node = DeserializationUtils.readYamlTree(swaggerAsString);
            }

            SwaggerDeserializationResult result = new Swagger20Parser().readWithInfo(node);
            if (result != null) {
                result.setSwagger(new SwaggerResolver(result.getSwagger(), new ArrayList<AuthorizationValue>(), null).resolve());
            }
            return new Swagger20Parser().readWithInfo(node);
        }
        catch (Exception e) {
            return new SwaggerDeserializationResult().message("malformed or unreadable swagger supplied");
        }
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
                    LOGGER.error(I_O_EXCEPTION_HAS_OCCURRED, e);
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