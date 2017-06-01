package io.swagger.parser.v3.util;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.oas.models.OpenAPI;
import io.swagger.parser.models.SwaggerParseResult;
import io.swagger.parser.v3.ObjectMapperFactory;

import java.util.Arrays;

public class OpenAPIDeserializer {
    public SwaggerParseResult deserialize(JsonNode rootNode) {
        SwaggerParseResult result = new SwaggerParseResult();
        try {
            // TODO
            OpenAPI api = ObjectMapperFactory.createJson().convertValue(rootNode, OpenAPI.class);
            result.setOpenAPI(api);
        }
        catch (Exception e) {
            result.setMessages(Arrays.asList(e.getMessage()));
        }
        return result;
    }
}