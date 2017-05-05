package io.swagger.parser.v3;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.models.OpenAPI;
import io.swagger.parser.extensions.SwaggerParserExtension;
import io.swagger.parser.models.AuthorizationValue;
import io.swagger.parser.models.ParseOptions;
import io.swagger.parser.models.SwaggerParseResult;
import io.swagger.parser.v3.util.OpenAPIDeserializer;

import java.net.URL;
import java.util.Arrays;
import java.util.List;

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
            result = new OpenAPIDeserializer().deserialize(node);
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
            if(swaggerAsString.trim().startsWith("{")) {
                mapper = JSON_MAPPER;
            }
            else {
                mapper = YAML_MAPPER;
            }
            try {
                OpenAPI api = mapper.readValue(swaggerAsString, OpenAPI.class);
                result.setOpenAPI(api);
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
}
