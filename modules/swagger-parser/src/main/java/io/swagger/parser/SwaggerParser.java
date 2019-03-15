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
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

public class SwaggerParser {

    public SwaggerDeserializationResult readWithInfo(String location, List<AuthorizationValue> auths, boolean resolve) {
        if (location == null) {
            return null;
        }
        location = location.replaceAll("\\\\", "/");
        List<SwaggerParserExtension> parserExtensions = getExtensions();
        SwaggerDeserializationResult output = new SwaggerDeserializationResult();
        try {
            if (auths == null) {
                auths = new ArrayList<AuthorizationValue>();
            }

            output = new Swagger20Parser().readWithInfo(location, auths);
            if (output != null) {
                if (output.getSwagger() != null && "2.0".equals(output.getSwagger().getSwagger())) {
                    if (resolve) {
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
            if (output == null) {
                output = new SwaggerDeserializationResult()
                        .message("The swagger definition could not be read");
            }
        }catch (Exception e) {
                output.setMessages(Arrays.asList(e.getMessage()));
            }
        return output;
    }

    public Swagger read(String location) {
        return read(location, null, true);
    }

    public Swagger read(String location, List<AuthorizationValue> auths, boolean resolve) {
        if (location == null) {
            return null;
        }
        location = location.replaceAll("\\\\", "/");
        Swagger output;

        try {
            output = new Swagger20Parser().read(location, auths);
            if (output != null) {
            	if(resolve){
            		return new SwaggerResolver(output, auths, location).resolve();
            	} else {
            		return output;
            	}
            }
        } catch (IOException e) {
        }

        List<SwaggerParserExtension> parserExtensions = getExtensions();
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

    public SwaggerDeserializationResult readWithInfo(String swaggerAsString) {
        return readWithInfo(swaggerAsString, Boolean.TRUE);
    }

    protected JsonNode deserializeYaml(String data) throws IOException{
        return DeserializationUtils.readYamlTree(data);
    }

    public SwaggerDeserializationResult readWithInfo(String swaggerAsString, boolean resolve) {
        if (swaggerAsString == null || swaggerAsString.trim().isEmpty()) {
            return new SwaggerDeserializationResult().message("empty or null swagger supplied");
        }
        try {
            JsonNode node;
            if (swaggerAsString.trim().startsWith("{")) {
                ObjectMapper mapper = Json.mapper();
                node = mapper.readTree(swaggerAsString);
            } else {
                node = deserializeYaml(swaggerAsString);
            }

            SwaggerDeserializationResult result = new Swagger20Parser().readWithInfo(node);
            if (result != null) {
                if (resolve) {
                    result.setSwagger(new SwaggerResolver(result.getSwagger(), new ArrayList<AuthorizationValue>(), null).resolve());
                }
            } else {
                result = new SwaggerDeserializationResult().message("Definition does not appear to be a valid Swagger format");
            }
            return result;
        } catch (Exception e) {
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
        }
        return null;
    }

    public Swagger read(JsonNode node) {
        return read(node, new ArrayList<AuthorizationValue>(), false);
    }

    public Swagger read(JsonNode node, boolean resolve) {
        return read(node, new ArrayList<AuthorizationValue>(), resolve);
    }

    public Swagger read(JsonNode node, List<AuthorizationValue> authorizationValues, boolean resolve) {
        if (node == null) {
            return null;
        }

        List<SwaggerParserExtension> parserExtensions = getExtensions();
        Swagger output = null;

        try {
            output = new Swagger20Parser().read(node);
            if (output != null) {
                if (resolve) {
                    return new SwaggerResolver(output, authorizationValues).resolve();
                } else {
                    return output;
                }
            }
        } catch (IOException e) {
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