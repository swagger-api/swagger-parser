package io.swagger.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.models.Swagger;
import io.swagger.models.auth.AuthorizationValue;
import io.swagger.parser.util.*;
import io.swagger.util.Json;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Swagger20Parser implements SwaggerParserExtension {
    private static final Logger LOGGER = LoggerFactory.getLogger(Swagger20Parser.class);

    @Override
    public SwaggerDeserializationResult parseLocation(String location) throws UnparseableContentException {
        return parseLocation(location, new ArrayList<AuthorizationValue>(), true);
    }

    @Override
    public SwaggerDeserializationResult parseLocation(String location, List<AuthorizationValue> auths, boolean resolve) throws UnparseableContentException {
        String data;

        try {
            location = location.replaceAll("\\\\","/");
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
            JsonNode rootNode;
            if (data.trim().startsWith("{")) {
                ObjectMapper mapper = Json.mapper();
                rootNode = mapper.readTree(data);
            } else {
                rootNode = DeserializationUtils.readYamlTree(data);
            }
            return parseContents(rootNode, auths, resolve);
        }
        catch (Exception e) {
            SwaggerDeserializationResult output = new SwaggerDeserializationResult();
            output.message("unable to read location `" + location + "`");
            return output;
        }
    }

    @Override
    public SwaggerDeserializationResult parseContents(JsonNode node) throws UnparseableContentException {
        return parseContents(node, new ArrayList<AuthorizationValue>(), true);
    }

    @Override
    public SwaggerDeserializationResult parseContents(JsonNode node, List<AuthorizationValue> auth, boolean resolve) throws UnparseableContentException {
        SwaggerDeserializationResult result = new SwaggerDeserializer().deserialize(node);

        if(result != null && result.getSwagger() != null) {
            Swagger resolved = new SwaggerResolver(result.getSwagger(), auth).resolve();
            if(resolved != null) {
                result.setSwagger(resolved);
            }
        }
        return result;
    }
}
