package io.swagger.parser;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.models.auth.AuthorizationValue;
import io.swagger.parser.util.ClasspathHelper;
import io.swagger.parser.util.DeserializationUtils;
import io.swagger.parser.util.RemoteUrl;
import io.swagger.parser.util.SwaggerDeserializationResult;
import io.swagger.util.Json;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

public class SwaggerParser {
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

    public SwaggerDeserializationResult parseLocation(final String location) throws UnparseableContentException {
        return parseLocation(location, new ArrayList<AuthorizationValue>(), true);
    }

    public SwaggerDeserializationResult parseLocation(String location, final List<AuthorizationValue> auths, final boolean resolve) throws UnparseableContentException {
        if (location == null) {
            return null;
        }
        location = location.replaceAll("\\\\","/");
        String data;

        try {
          if (location.toLowerCase().startsWith("http")) {
              data = RemoteUrl.urlToString(location, auths);
          } else {
              final String fileScheme = "file://";
              Path path;
              if (location.toLowerCase().startsWith(fileScheme)) {
                  path = Paths.get(URI.create(location));
              } else {
                  path = Paths.get(location);
              }
              if (Files.exists(path)) {
                  data = FileUtils.readFileToString(path.toFile(), "UTF-8");
              } else {
                  data = ClasspathHelper.loadFileFromClasspath(location);
              }
          }
        } catch (Exception e) {
            return convertToDeserializationResult(location, e);
        }

        return parseContents(data, auths, location, resolve);
    }

    public SwaggerDeserializationResult parseContents(final String swaggerAsString, final List<AuthorizationValue> auths, final String parentLocation, final boolean resolve) throws UnparseableContentException {

        if(swaggerAsString == null) {
            return null;
        }

        JsonNode swaggerAsJson;
        try {
            if (swaggerAsString.trim().startsWith("{")) {
                ObjectMapper mapper = Json.mapper();
                swaggerAsJson = mapper.readTree(swaggerAsString);
            } else {
              swaggerAsJson = DeserializationUtils.readYamlTree(swaggerAsString);
            }
        } catch (Exception e) {
            return convertToDeserializationResult(parentLocation, e);
        }

        return parseContents(swaggerAsJson, auths, parentLocation, resolve);
    }

    public SwaggerDeserializationResult parseContents(final String swaggerAsString) throws UnparseableContentException {
        return parseContents(swaggerAsString, new ArrayList<AuthorizationValue>(), null, false);
    }

    public SwaggerDeserializationResult parseContents(final JsonNode node) throws UnparseableContentException {
        return parseContents(node, new ArrayList<AuthorizationValue>(), null, false);
    }

    public SwaggerDeserializationResult parseContents(final JsonNode node, final List<AuthorizationValue> authorizationValues, final String parentLocation, final boolean resolve) throws UnparseableContentException {
        if (node == null) {
            return null;
        }
        SwaggerParserExtension currentExtension = null;
        List<SwaggerParserExtension> parserExtensions = getExtensions();
        for (SwaggerParserExtension extension : parserExtensions) {
            if (extension.supports(node)) {
                if (currentExtension != null) {
                  // BUG: there are two extensions that support the spec.
                  throw new UnparseableContentException();
                }
                currentExtension = extension;
            }
        }

        if (currentExtension == null) {
            // Error: no extension supports the spec.
            throw new UnparseableContentException();
        }

        return currentExtension.parseContents(node, authorizationValues, parentLocation, resolve);
    }

    private SwaggerDeserializationResult convertToDeserializationResult(final String location, final Exception ex) {
        SwaggerDeserializationResult result = new SwaggerDeserializationResult();
        if (ex instanceof JsonParseException) {
            JsonParseException e = (JsonParseException) ex;
            result.message(e.getOriginalMessage());
            result.message(e.getLocation().toString());
            return result;
        } else {
            String message = "unable to read spec";
            if (location != null) {
                message += " from location `" + location + "`";
            }
            message += ": " + ex.getMessage();
            result.message(message);
            return result;
        }
    }
}
