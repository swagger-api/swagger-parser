package io.swagger.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.models.Swagger;
import io.swagger.models.auth.AuthorizationValue;
import io.swagger.parser.util.ClasspathHelper;
import io.swagger.parser.util.RemoteUrl;
import io.swagger.util.Json;
import io.swagger.util.Yaml;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.Validate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class Swagger20Parser implements SwaggerParserExtension {

    @Override
    public Swagger read(String location, List<AuthorizationValue> auths) throws IOException {
        System.out.println("reading from " + location);

        try {
            String data;

            if (location.toLowerCase().startsWith("http")) {
                data = RemoteUrl.urlToString(location, auths);
            } else {
                final Path path = Paths.get(location);
                if(Files.exists(path)) {
                    data = FileUtils.readFileToString(path.toFile(), "UTF-8");
                } else {
                    data = ClasspathHelper.loadFileFromClasspath(location);
                }
            }

            return convertToSwagger(data);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            if (System.getProperty("debugParser") != null) {
                e.printStackTrace();
            }
            return null;
        }
    }

    private Swagger convertToSwagger(String data) throws IOException {
        if (data != null) {
            ObjectMapper mapper;
            if (data.trim().startsWith("{")) {
                mapper = Json.mapper();
            } else {
                mapper = Yaml.mapper();
            }
            JsonNode rootNode = mapper.readTree(data);
            // must have swagger node set
            JsonNode swaggerNode = rootNode.get("swagger");
            if (swaggerNode == null) {
                return null;
            } else {
                return mapper.convertValue(rootNode, Swagger.class);
            }
        } else {
            return null;
        }
    }

    public Swagger parse(String data) throws IOException {
        Validate.notEmpty(data, "data must not be null!");
        return convertToSwagger(data);
    }

    @Override
    public Swagger read(JsonNode node) throws IOException {
        if (node == null) {
            return null;
        }

        return Json.mapper().convertValue(node, Swagger.class);
    }
}