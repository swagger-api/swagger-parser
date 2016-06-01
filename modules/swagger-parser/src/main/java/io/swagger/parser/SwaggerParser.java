package io.swagger.parser;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.models.auth.AuthorizationValue;
import io.swagger.parser.util.SwaggerDeserializationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

public class SwaggerParser extends AbstractParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(SwaggerParser.class);

    public static List<SwaggerParserExtension> getExtensions() {
        ServiceLoader<SwaggerParserExtension> loader = ServiceLoader.load(SwaggerParserExtension.class);
        List<SwaggerParserExtension> output = new ArrayList<SwaggerParserExtension>();
        Iterator<SwaggerParserExtension> itr = loader.iterator();
        while (itr.hasNext()) {
            output.add(itr.next());
        }
        return output;
    }

    public SwaggerDeserializationResult parseLocation(String location) throws UnparseableContentException {
        return parseLocation(location, null, true);
    }

    public SwaggerDeserializationResult parseLocation(String location, List<AuthorizationValue> auths, boolean resolve) throws UnparseableContentException {
        if (location == null) {
            return null;
        }
        location = location.replaceAll("\\\\","/");
        List<SwaggerParserExtension> parserExtensions = getExtensions();

        if(auths == null) {
            auths = new ArrayList<AuthorizationValue>();
        }

        try {
            return new Swagger20Parser().parseLocation(location, auths, resolve);
        } catch (UnparseableContentException e) {
            LOGGER.debug("can't read as Swagger 2.0 file");
        }

        for (SwaggerParserExtension extension : parserExtensions) {
            try {
                return extension.parseLocation(location, auths, resolve);
            }
            catch (UnparseableContentException e) {
                LOGGER.debug("unable to parse location", e);
            }
        }
        throw new UnparseableContentException();
    }

    public SwaggerDeserializationResult parseContents(String swaggerAsString, List<AuthorizationValue> auths, String parentLocation, boolean resolve) throws UnparseableContentException {
        JsonNode node;
        try {
            node = stringToNode(swaggerAsString);
        } catch (JsonParseException e) {
            SwaggerDeserializationResult result = new SwaggerDeserializationResult();
            result.message(e.getOriginalMessage());
            result.message(e.getLocation().toString());
            return result;
        } catch (Exception e) {
            throw new UnparseableContentException();
        }

        try {
            return new Swagger20Parser().parseContents(node, auths, parentLocation, resolve);
        }
        catch (UnparseableContentException e) {
            LOGGER.debug("unable to parse as swagger 2.0");
        }

        List<SwaggerParserExtension> parserExtensions = getExtensions();
        for (SwaggerParserExtension extension : parserExtensions) {
            try {
                return extension.parseContents(node, auths, parentLocation, resolve);
            } catch (UnparseableContentException e) {
                if (System.getProperty("debugParser") != null) {
                    e.printStackTrace();
                }
                // continue to next parser
            }
        }
        throw new UnparseableContentException();
    }

    public SwaggerDeserializationResult parseContents(String swaggerAsString) throws UnparseableContentException {
        JsonNode node;
        try {
            node = stringToNode(swaggerAsString);
        } catch (JsonParseException e) {
            SwaggerDeserializationResult result = new SwaggerDeserializationResult();
            result.message(e.getOriginalMessage());
            result.message(e.getLocation().toString());
            return result;
        } catch (Exception e) {
            throw new UnparseableContentException();
        }
        return parseContents(node);
    }

    public SwaggerDeserializationResult parseContents(JsonNode node) throws UnparseableContentException {
        return parseContents(node, new ArrayList<AuthorizationValue>(), null, false);
    }

    public SwaggerDeserializationResult parseContents(JsonNode node, List<AuthorizationValue> authorizationValues, String parentLocation, boolean resolve) throws UnparseableContentException {
        if (node == null) {
            return null;
        }

        List<SwaggerParserExtension> parserExtensions = getExtensions();

        try {
            return new Swagger20Parser().parseContents(node, authorizationValues, parentLocation, resolve);
        } catch (UnparseableContentException e) {
        }
        for (SwaggerParserExtension extension : parserExtensions) {
            try {
                return extension.parseContents(node, authorizationValues, parentLocation, resolve);
            } catch (UnparseableContentException e) {
                if (System.getProperty("debugParser") != null) {
                    e.printStackTrace();
                }
                // continue to next parser
            }
        }
        throw new UnparseableContentException();
    }
}