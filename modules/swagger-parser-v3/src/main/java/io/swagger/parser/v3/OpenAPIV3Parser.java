package io.swagger.parser.v3;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.oas.models.OpenAPI;
import io.swagger.parser.extensions.SwaggerParserExtension;
import io.swagger.parser.models.AuthorizationValue;
import io.swagger.parser.models.ParseOptions;
import io.swagger.parser.models.SwaggerParseResult;
import io.swagger.parser.v3.util.OpenAPIDeserializer;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

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
            JsonNode node = YAML_MAPPER.readValue(new URL(url), JsonNode.class);
            if(node != null && node.get("openapi") != null) {
                JsonNode version = node.get("openapi");
                if(version.asText() != null && version.asText().startsWith("3.0")) {
                    result.setOpenAPI(new OpenAPIResolver(new OpenAPIDeserializer().deserialize(node).getOpenAPI(),null,null).resolve());
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
            if(swaggerAsString.trim().startsWith("{")) {
                mapper = JSON_MAPPER;
            }
            else {
                mapper = YAML_MAPPER;
            }
            try {
                OpenAPIDeserializer deserializer = new OpenAPIDeserializer();
                JsonNode rootNode = mapper.readTree(swaggerAsString.getBytes());
                SwaggerParseResult  deserializeOpenAPI = deserializer.deserialize(rootNode);
                OpenAPIResolver resolver = new OpenAPIResolver(deserializeOpenAPI.getOpenAPI(),null,null);
                result.setOpenAPI(resolver.resolve());

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

    protected List<io.swagger.parser.extensions.SwaggerParserExtension> getExtensions() {
        List<io.swagger.parser.extensions.SwaggerParserExtension> extensions = new ArrayList<>();

        ServiceLoader<SwaggerParserExtension> loader = ServiceLoader.load(io.swagger.parser.extensions.SwaggerParserExtension.class);
        Iterator<SwaggerParserExtension> itr = loader.iterator();
        while (itr.hasNext()) {
            extensions.add(itr.next());
        }
        extensions.add(0, new OpenAPIV3Parser());
        return extensions;
    }

    /**
     * Transform the swagger-model version of AuthorizationValue into a parser-specific one, to avoid
     * dependencies across extensions
     *
     * @param input
     * @return
     */
    protected List<io.swagger.parser.models.AuthorizationValue> transform(List<AuthorizationValue> input) {
        if(input == null) {
            return null;
        }

        List<io.swagger.parser.models.AuthorizationValue> output = new ArrayList<>();

        for(AuthorizationValue value : input) {
            io.swagger.parser.models.AuthorizationValue v = new io.swagger.parser.models.AuthorizationValue();

            v.setKeyName(value.getKeyName());
            v.setValue(value.getValue());
            v.setType(value.getType());

            output.add(v);
        }

        return output;
    }
}
