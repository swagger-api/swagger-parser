package io.swagger.parser.v3;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.oas.models.OpenAPI;
import io.swagger.parser.extensions.SwaggerParserExtension;
import io.swagger.parser.models.AuthorizationValue;
import io.swagger.parser.models.ParseOptions;
import io.swagger.parser.models.SwaggerParseResult;
import io.swagger.parser.v3.util.OpenAPIDeserializer;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.io.IOException;

public class OpenAPIV3Parser implements SwaggerParserExtension {
    private static ObjectMapper JSON_MAPPER, YAML_MAPPER;

    static {
        JSON_MAPPER = ObjectMapperFactory.createJson();
        YAML_MAPPER = ObjectMapperFactory.createYaml();
    }
    @Override
    public SwaggerParseResult readLocation(String url, List<AuthorizationValue> auth, ParseOptions options) {
        SwaggerParseResult result = new SwaggerParseResult();
        try {
            // TODO
            JsonNode node = JSON_MAPPER.readValue(new URL(url), JsonNode.class);
            if(node != null && node.get("openapi") != null) {
                JsonNode version = node.get("openapi");
                if(version.asText() != null && version.asText().startsWith("3.0")) {
                    result = new OpenAPIDeserializer().deserialize(node);
                }
            }
        }
        catch (Exception e) {
            result.setMessages(Arrays.asList(e.getMessage()));
        }
        return result;
    }

    @Override
    public SwaggerParseResult readContents(String swaggerAsString, List<AuthorizationValue> auth, ParseOptions options) {
        SwaggerParseResult result = new SwaggerParseResult();
        if(swaggerAsString != null && !"".equals(swaggerAsString.trim())) {
            ObjectMapper mapper;
            boolean isYaml = false;
            if(swaggerAsString.trim().startsWith("{")) {
                mapper = JSON_MAPPER;
            }
            else {
                mapper = YAML_MAPPER;
                isYaml = true;
            }
            try {
                if(checkForVersion(swaggerAsString, isYaml)){
                    OpenAPI api = mapper.readValue(swaggerAsString, OpenAPI.class);
                    result.setOpenAPI(api);
                }
            }
            catch (Exception e) {
                result.setMessages(Arrays.asList(e.getMessage()));
            }
        }
        else {
            result.setMessages(Arrays.asList("No swagger supplied"));
        }
        return result;
    }

    private boolean checkForVersion(String swaggerAsString, boolean isYaml) throws IOException {
        JsonParser parser = null;
        if (isYaml) {
            YAMLFactory factory = new YAMLFactory();
            parser = factory.createParser(swaggerAsString);
        } else {
            JsonFactory factory = new JsonFactory();
            parser = factory.createParser(swaggerAsString);
        }
        if (parser != null) {
            JsonToken nextToken = parser.nextToken();
            if (nextToken == JsonToken.START_OBJECT) {
                nextToken = parser.nextToken();
            }
            while (nextToken != JsonToken.END_OBJECT && nextToken != null) {
                if (nextToken == JsonToken.FIELD_NAME) {
                    String currentName = parser.getCurrentName();
                    if (currentName != null && currentName.equals("openapi")) {
                        if (parser.nextToken() == JsonToken.VALUE_STRING) {
                            String version = parser.getValueAsString();
                            if (version.startsWith("3.0.0"))
                                return true;
                        }
                    }
                } else {
                    parser.skipChildren();
                }

                nextToken = parser.nextToken();
            }
            return false;
        }
        return false;
    }
}
