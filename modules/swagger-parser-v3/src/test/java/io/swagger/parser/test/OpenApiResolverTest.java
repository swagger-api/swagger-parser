package io.swagger.parser.test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.swagger.model.ApiDescription;
import io.swagger.oas.models.OpenAPI;
import io.swagger.oas.models.media.ArraySchema;
import io.swagger.oas.models.media.Schema;
import io.swagger.oas.models.responses.ApiResponse;
import io.swagger.parser.models.AuthorizationValue;
import io.swagger.parser.models.SwaggerParseResult;
import io.swagger.parser.v3.OpenAPIResolver;
import io.swagger.parser.v3.util.OpenAPIDeserializer;
import mockit.Injectable;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;

public class OpenAPIResolverTest {

    @Test
    public void testOpenAPIResolver(@Injectable final List<AuthorizationValue> auths) throws Exception {
        final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        final JsonNode rootNode = mapper.readTree(Files.readAllBytes(java.nio.file.Paths.get(getClass().getResource("/oas3.yaml").toURI())));
        final OpenAPIDeserializer deserializer = new OpenAPIDeserializer();
        final SwaggerParseResult result = deserializer.deserialize(rootNode);

        Assert.assertNotNull(result);
        final OpenAPI openAPI = result.getOpenAPI();
        Assert.assertNotNull(openAPI);
        assertEquals(new OpenAPIResolver(openAPI, auths, null).resolve(), openAPI);
        Map<String, Schema> schemas = openAPI.getComponents().getSchemas();
        //System.out.println(schemas.get("ExtendedErrorModel"));

        ArraySchema tagsProperty = (ArraySchema) schemas.get("Pet").getProperties().get("tags");
        Schema name = (Schema)schemas.get("Pet").getProperties().get("user");
        //System.out.println(schemas);

        Map<String, ApiResponse> responses = openAPI.getComponents().getResponses();
        System.out.println(responses);
    }

}