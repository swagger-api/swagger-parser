package io.swagger.converter;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import io.swagger.models.Contact;
import io.swagger.models.Info;
import io.swagger.models.License;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.Response;
import io.swagger.models.Swagger;
import io.swagger.models.auth.ApiKeyAuthDefinition;
import io.swagger.models.auth.AuthorizationValue;
import io.swagger.models.auth.In;
import io.swagger.models.auth.OAuth2Definition;
import io.swagger.models.auth.SecuritySchemeDefinition;
import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.parameters.PathParameter;
import io.swagger.models.parameters.QueryParameter;
import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.RefProperty;
import io.swagger.models.properties.StringProperty;
import io.swagger.parser.SwaggerCompatConverter;
import io.swagger.parser.SwaggerParser;
import io.swagger.parser.util.RemoteUrl;
import io.swagger.parser.util.SwaggerDeserializationResult;
import io.swagger.util.Json;
import mockit.Expectations;
import mockit.Mocked;
import org.junit.Assert;
import org.testng.annotations.Test;

import javax.validation.constraints.AssertFalse;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

public class LegacyConverterTest {
    SwaggerCompatConverter converter = new SwaggerCompatConverter();

    private static String resources_json, pet_json, store_json, user_json, marvel_json, public_json;

    @Mocked
    public RemoteUrl remoteUrl = new RemoteUrl();

    static {
        try {
            resources_json = readFile("src/test/resources/specs/v1_2/petstore/api-docs");
            pet_json = readFile("src/test/resources/specs/v1_2/petstore/pet");
            user_json = readFile("src/test/resources/specs/v1_2/petstore/user");
            store_json = readFile("src/test/resources/specs/v1_2/petstore/store");

            marvel_json = readFile("src/test/resources/specs/v1_2/marvel.json");
            public_json = readFile("src/test/resources/specs/v1_2/public.json");
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testIssueFun() throws Exception {
        new Expectations() {{
            remoteUrl.urlToString("http://localhost:8080/api-docs", new ArrayList<AuthorizationValue>());
            result = resources_json;

            remoteUrl.urlToString("http://localhost:8080/api-docs/pet", new ArrayList<AuthorizationValue>());
            result = pet_json;

            remoteUrl.urlToString("http://localhost:8080/api-docs/store", new ArrayList<AuthorizationValue>());
            result = store_json;

            remoteUrl.urlToString("http://localhost:8080/api-docs/user", new ArrayList<AuthorizationValue>());
            result = user_json;

            remoteUrl.urlToString("http://localhost:8080/api-docs", null);
            result = resources_json;

            remoteUrl.urlToString("http://localhost:8080/api-docs/pet", null);
            result = pet_json;

            remoteUrl.urlToString("http://localhost:8080/api-docs/store", null);
            result = store_json;

            remoteUrl.urlToString("http://localhost:8080/api-docs/user", null);
            result = user_json;

        }};

        SwaggerParser parser = new SwaggerParser();
        SwaggerDeserializationResult result = parser.readWithInfo("http://localhost:8080/api-docs", null, true);

        Swagger swagger = parser.read("http://localhost:8080/api-docs");
        Assert.assertNotNull(swagger);
    }


    @Test
    public void testIssue43() throws Exception {
        new Expectations() {{
            remoteUrl.urlToString("http://gateway.marvel.com/docs", new ArrayList<AuthorizationValue>());
            result = marvel_json;

            remoteUrl.urlToString("http://gateway.marvel.com/docs/public", new ArrayList<AuthorizationValue>());
            result = public_json;
        }};

        SwaggerParser parser = new SwaggerParser();
        SwaggerDeserializationResult result = parser.readWithInfo("http://gateway.marvel.com/docs", null, true);

        Assert.assertNotNull(result.getSwagger());
    }

    /**
     * reads a single-file swagger definition
     **/
    @Test
    public void convertSingleFile() throws Exception {
        Swagger swagger = converter.read("src/test/resources/specs/v1_2/singleFile.json");

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
        Swagger swagger = converter.read("src/test/resources/specs/v1_2/empty.json");

        assertNull(swagger);
    }

    @Test
    public void testFixedProperties() throws IOException {
        final Swagger swagger = converter.read("src/test/resources/specs/v1_2/singleFile.json");
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
                    public boolean apply(Parameter input) {
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
        Swagger swagger = converter.read("src/test/resources/specs/v1_1/sample.json");
        Parameter param = swagger.getPaths().get("/events").getGet().getParameters().get(0);
    }

    static String readFile(String name) {
        try {
            return new String(Files.readAllBytes(new File(name).toPath()), Charset.forName("UTF-8"));
        }
        catch (Exception e) {
            return null;
        }
    }

    @Test
    public void testIssue104() throws Exception {
        Swagger swagger = converter.read("src/test/resources/specs/v1_2/issue-104.json");
        Json.prettyPrint(swagger);
        Property p = swagger.getDefinitions().get("Issue").getProperties().get("availableTransitions");
        assertTrue(p instanceof ArrayProperty);
        ArrayProperty ap = (ArrayProperty) p;
        Property items = ap.getItems();
        assertTrue(items instanceof StringProperty);
        StringProperty sp = (StringProperty) items;

        Set<String> expected = new HashSet<String>(Arrays.asList("startProgress", "stopProgress", "resolve", "reopen", "close"));
        Set<String> actual = new HashSet<>(sp.getEnum());

        assertEquals(actual, expected);
    }

    @Test
    public void testIssue799() throws Exception {

        Swagger swagger = converter.read("src/test/resources/specs/v1_2/issue799.json");
        Assert.assertEquals( swagger.getPaths().get("/api/v1beta3/namespaces/{namespaces}/bindings").getPost().getResponses().get("200").getResponseSchema().getReference(), "#/definitions/v1beta3.Binding");
        Parameter bodyParameter = swagger.getPaths().get("/api/v1beta3/namespaces/{namespaces}/bindings").getPost().getParameters().get(1);
        Assert.assertEquals( bodyParameter.getName(), "body");
        Assert.assertTrue( bodyParameter instanceof BodyParameter);
        Assert.assertEquals( ((BodyParameter)bodyParameter).getSchema().getReference(), "#/definitions/v1beta3.Binding");
        Assert.assertEquals( swagger.getPaths().get("/api/v1beta3/namespaces/{namespaces}/componentstatuses/{name}").getGet().getResponses().get("200").getResponseSchema().getReference(), "#/definitions/v1beta3.ComponentStatus");
        Assert.assertEquals( swagger.getPaths().get("/api/v1beta3/namespaces/{namespaces}/componentstatuses").getGet().getResponses().get("200").getResponseSchema().getReference(), "#/definitions/v1beta3.ComponentStatusList");
        Property conditionsProperty = swagger.getDefinitions().get("v1beta3.ComponentStatus").getProperties().get("conditions");
        Assert.assertTrue( conditionsProperty instanceof ArrayProperty);
        Property items = ((ArrayProperty)conditionsProperty).getItems();
        Assert.assertTrue( items instanceof RefProperty);
        Assert.assertEquals( ((RefProperty)items).get$ref(), "#/definitions/v1beta3.ObjectReference");
        Property metadataProperty = swagger.getDefinitions().get("v1beta3.ComponentStatus").getProperties().get("metadata");
        Assert.assertTrue( metadataProperty instanceof RefProperty);
        Assert.assertEquals( ((RefProperty)metadataProperty).get$ref(), "#/definitions/v1beta3.ObjectMeta");
    }
}
