package io.swagger.converter;

import io.swagger.models.Contact;
import io.swagger.models.Info;
import io.swagger.models.License;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.Response;
import io.swagger.models.Swagger;
import io.swagger.models.auth.ApiKeyAuthDefinition;
import io.swagger.models.auth.In;
import io.swagger.models.auth.OAuth2Definition;
import io.swagger.models.auth.SecuritySchemeDefinition;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.parameters.PathParameter;
import io.swagger.models.parameters.QueryParameter;
import io.swagger.parser.SwaggerCompatConverter;

import org.testng.annotations.Test;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

public class LegacyConverterTest {
    SwaggerCompatConverter converter = new SwaggerCompatConverter();

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
        assertNull(path.getDelete().getResponses());
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
}
