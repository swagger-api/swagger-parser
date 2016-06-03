package io.swagger.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import io.swagger.models.*;
import io.swagger.models.auth.ApiKeyAuthDefinition;
import io.swagger.models.auth.AuthorizationValue;
import io.swagger.models.auth.In;
import io.swagger.models.auth.OAuth2Definition;
import io.swagger.models.auth.SecuritySchemeDefinition;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.parameters.PathParameter;
import io.swagger.models.parameters.QueryParameter;
import io.swagger.parser.SwaggerCompatConverter;
import io.swagger.parser.SwaggerParser;
import io.swagger.parser.util.SwaggerDeserializationResult;
import io.swagger.util.Json;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.*;

public class LegacyConverterTest {
    SwaggerCompatConverter converter = new SwaggerCompatConverter();
    ObjectMapper mapper = Json.mapper();

    @Test
    public void testSupportsTrue() throws JsonProcessingException, IOException {
        JsonNode node = mapper.readTree("{\"swaggerVersion\" : \"1.2\"}");
        assertTrue(converter.supports(node));
    }

    @Test
    public void testSupportsFalse() throws JsonProcessingException, IOException {
        JsonNode node = mapper.readTree("{\"swagger\" : \"2.0\"}");
        assertFalse(converter.supports(node));
    }

    @Test
    public void testIssueFun() throws Exception {
        SwaggerParser parser = new SwaggerParser();
        SwaggerDeserializationResult result = parser.parseLocation("http://localhost:8080/api-docs/resources.json", null, true);

        SwaggerDeserializationResult swagger = parser.parseLocation("http://localhost:8080/api-docs/resources.json");
        Assert.assertNotNull(result);
    }


    @Test
    public void testIssue43() throws Exception {
        SwaggerParser parser = new SwaggerParser();
        SwaggerDeserializationResult result = parser.parseLocation("http://gateway.marvel.com/docs", null, true);

        Assert.assertNotNull(result);
    }

    /**
     * reads a single-file swagger definition
     **/
    @Test
    public void convertSingleFile() throws Exception {
        SwaggerDeserializationResult result = parseLocation("src/test/resources/specs/v1_2/singleFile.json");

        Swagger swagger = result.getSwagger();

        assertTrue(swagger.getSecurityDefinitions().size() == 2);
        SecuritySchemeDefinition auth = swagger.getSecurityDefinitions().get("oauth2");
        assertNotNull(auth);
        assertEquals(auth.getClass(), OAuth2Definition.class);
        OAuth2Definition oauth2 = (OAuth2Definition) auth;

        assertEquals(oauth2.getFlow(), "implicit");
        assertEquals(oauth2.getAuthorizationUrl(), "http://petstore.swagger.io/oauth/dialog");
        assertTrue(oauth2.getScopes().size() == 2);
        Map<String, String> scopes = oauth2.getScopes();
        assertEquals(scopes.get("email"), "Access to your email address");
        assertEquals(scopes.get("pets"), "Access to your pets");

        auth = swagger.getSecurityDefinitions().get("apiKey");
        assertNotNull(auth);
        assertEquals(auth.getClass(), ApiKeyAuthDefinition.class);
        ApiKeyAuthDefinition apiKey = (ApiKeyAuthDefinition) auth;

        assertEquals(apiKey.getName(), "api_key");
        assertEquals(apiKey.getIn(), In.HEADER);


        assertEquals(swagger.getSwagger(), "2.0");
        assertEquals(swagger.getHost(), "petstore.swagger.io");
        assertEquals(swagger.getBasePath(), "/api");
        assertNotNull(swagger.getInfo());

        Info info = swagger.getInfo();
        assertEquals(info.getVersion(), "1.0.0");
        assertEquals(info.getTitle(), "Swagger Sample App");
        assertEquals(info.getTermsOfService(), "http://swagger.io/terms/");

        Contact contact = info.getContact();
        assertEquals(contact.getUrl(), "apiteam@swagger.io");

        License license = info.getLicense();
        assertEquals(license.getName(), "Apache 2.0");
        assertEquals(license.getUrl(), "http://www.apache.org/licenses/LICENSE-2.0.html");

        assertTrue(swagger.getDefinitions().size() == 3);
        assertTrue(swagger.getPaths().size() == 5);

        Operation patchOperation = swagger.getPaths().get("/pet/{petId}").getPatch();
        List<Map<String, List<String>>> security = patchOperation.getSecurity();
        assertTrue(security.size() == 1);
        Map<String, List<String>> securityDetail = security.get(0);
        String key = securityDetail.keySet().iterator().next();
        assertEquals(key, "oauth2");
        List<String> oauth2Scopes = securityDetail.get(key);

        assertEquals(oauth2Scopes.size(), 1);
        assertEquals(oauth2Scopes.get(0), "test:anything");

        Operation fetchOperation = swagger.getPaths().get("/pet/findByStatus").getGet();
        QueryParameter param = (QueryParameter) fetchOperation.getParameters().get(0);
        assertEquals(param.getDefaultValue(), "available");

        List<String> _enum = param.getEnum();
        assertEquals(_enum.get(0), "available");
        assertEquals(_enum.get(1), "pending");
        assertEquals(_enum.get(2), "sold");
    }

    @Test
    public void failConversionTest() throws Exception {
        SwaggerDeserializationResult result = parseLocation("src/test/resources/specs/v1_2/empty.json");

        assertNull(result.getSwagger());
    }

    @Test
    public void testFixedProperties() throws Exception {
        SwaggerDeserializationResult result = parseLocation("src/test/resources/specs/v1_2/singleFile.json");
        final Swagger swagger = result.getSwagger();
        final Path path = swagger.getPath("/pet/{petId}");
        assertEquals(path.getPost().getResponses().size(), 1);
        for (Response item : path.getPost().getResponses().values()) {
            assertNull(item.getSchema());
        }
        assertEquals(path.getDelete().getResponses().size(), 1);
        assertEquals(path.getDelete().getResponses().containsKey("default"), true);
        assertEquals(path.getDelete().getResponses().get("default").getDescription(), "success");

        final PathParameter id = (PathParameter) Iterables.find(path.getPatch().getParameters(),
                new Predicate<Parameter>() {

                    @Override
                    public boolean apply(final Parameter input) {
                        return "petId".equals(input.getName());
                    }
                });

        assertEquals(id.getType(), "string");
        assertNull(id.getFormat());
    }

    /**
     * reads a single-file swagger definition
     **/
    @Test
    public void convertSingle1_1File() throws Exception {
        SwaggerDeserializationResult result = parseLocation("src/test/resources/specs/v1_1/sample.json");
        Swagger swagger = result.getSwagger();
        Parameter param = swagger.getPaths().get("/events").getGet().getParameters().get(0);
    }

    private SwaggerDeserializationResult parseLocation(final String location) {
        SwaggerDeserializationResult result;
        try {
            String data = FileUtils.readFileToString(Paths.get(location).toFile(), "UTF-8");
            JsonNode node = mapper.readTree(data);
            result = converter.parseContents(node, new ArrayList<AuthorizationValue>(), location, true);
        } catch (Exception e) {
            throw new RuntimeException("Error parsing " + location, e);
        }
        return result;
    }
}
