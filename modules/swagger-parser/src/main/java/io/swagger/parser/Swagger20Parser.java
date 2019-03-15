package io.swagger.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.models.Swagger;
import io.swagger.models.auth.AuthorizationValue;
import io.swagger.parser.util.ClasspathHelper;
import io.swagger.parser.util.DeserializationUtils;
import io.swagger.parser.util.RemoteUrl;
import io.swagger.parser.util.SwaggerDeserializationResult;
import io.swagger.parser.util.SwaggerDeserializer;
import io.swagger.util.Json;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class Swagger20Parser implements SwaggerParserExtension {
    private static final Logger LOGGER = LoggerFactory.getLogger(Swagger20Parser.class);

    @Override
    public SwaggerDeserializationResult readWithInfo(JsonNode node) {
        SwaggerDeserializer ser = new SwaggerDeserializer();
        return ser.deserialize(node);
    }

    @Override
    public SwaggerDeserializationResult readWithInfo(String location, List<AuthorizationValue> auths) {
        String data;

        try {
            location = location.replaceAll("\\\\","/");
            if (location.toLowerCase().startsWith("http")) {
                data = RemoteUrl.urlToString(location, auths);
            } else {
                final String fileScheme = "file:";
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
                rootNode = deserializeYaml(data);
            }
            return readWithInfo(rootNode);
        }
        catch (SSLHandshakeException e) {
            SwaggerDeserializationResult output = new SwaggerDeserializationResult();
            output.message("unable to read location `" + location + "` due to a SSL configuration error.  " +
                    "It is possible that the server SSL certificate is invalid, self-signed, or has an untrusted " +
                    "Certificate Authority.");
            return output;
        }
        catch (Exception e) {
            SwaggerDeserializationResult output = new SwaggerDeserializationResult();
            output.message("unable to read location `" + location + "`");
            return output;
        }
    }

    protected JsonNode deserializeYaml(String data) throws IOException{
        return DeserializationUtils.readYamlTree(data);
    }

    @Override
    public Swagger read(String location, List<AuthorizationValue> auths) throws IOException {
        LOGGER.info("reading from " + location);
        try {
            String data;
            location = location.replaceAll("\\\\","/");
            if (location.toLowerCase().startsWith("http")) {
                data = RemoteUrl.urlToString(location, auths);
            } else {
                final String fileScheme = "file:";
                Path path;
                if (location.toLowerCase().startsWith(fileScheme)) {
                    path = Paths.get(URI.create(location));
                } else {
                    path = Paths.get(location);
                }
                if(Files.exists(path)) {
                    data = FileUtils.readFileToString(path.toFile(), "UTF-8");
                } else {
                    data = ClasspathHelper.loadFileFromClasspath(location);
                }
            }

            return convertToSwagger(data);
        } catch (Exception e) {
            if (System.getProperty("debugParser") != null) {
                e.printStackTrace();
            }
            return null;
        }
    }

    private Swagger convertToSwagger(String data) throws IOException {
        if (data != null) {
            JsonNode rootNode;
            if (data.trim().startsWith("{")) {
                ObjectMapper mapper = Json.mapper();
                rootNode = mapper.readTree(data);
            } else {
                rootNode = deserializeYaml(data);
            }

            if (System.getProperty("debugParser") != null) {
                LOGGER.info("\n\nSwagger Tree: \n"
                    + ReflectionToStringBuilder.toString(rootNode, ToStringStyle.MULTI_LINE_STYLE) + "\n\n");
            }
            if(rootNode == null) {
                return null;
            }
            // must have swagger node set
            JsonNode swaggerNode = rootNode.get("swagger");
            if (swaggerNode == null) {
                return null;
            } else {
                SwaggerDeserializationResult result = new SwaggerDeserializer().deserialize(rootNode);

                Swagger convertValue = result.getSwagger();
                if (System.getProperty("debugParser") != null) {
                    LOGGER.info("\n\nSwagger Tree convertValue : \n"
                        + ReflectionToStringBuilder.toString(convertValue, ToStringStyle.MULTI_LINE_STYLE) + "\n\n");
                }
                return convertValue;
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
