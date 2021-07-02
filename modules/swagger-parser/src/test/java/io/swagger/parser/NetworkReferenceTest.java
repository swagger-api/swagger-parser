package io.swagger.parser;

import io.swagger.models.ModelImpl;
import io.swagger.models.Path;
import io.swagger.models.RefModel;
import io.swagger.models.Response;
import io.swagger.models.Swagger;
import io.swagger.models.auth.AuthorizationValue;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.RefProperty;
import io.swagger.models.properties.StringProperty;
import io.swagger.parser.util.RemoteUrl;
import io.swagger.parser.util.SwaggerDeserializationResult;
import io.swagger.util.Json;
import mockit.Expectations;
import mockit.Mocked;
import org.junit.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class NetworkReferenceTest {
    @Mocked
    public RemoteUrl remoteUrl = new RemoteUrl();

    private static String issue_323_yaml, issue_323_events_yaml, issue_323_paging_yaml, issue_323_bar_yaml;
    private static String issue_328_yaml, issue_328_events_yaml, issue_328_paging_yaml, issue_328_bar_yaml;
    private static String issue_330_yaml, issue_330_users_yaml, issue_330_paging_yaml, issue_330_entities_yaml;
    private static String issue_335_json, issue_335_bar_json;
    private static String issue_407_json;
    private static String issue_411_server, issue_411_components;

    static {
        try {
            issue_323_yaml          = readFile("src/test/resources/nested-file-references/issue-323.yaml");
            issue_323_events_yaml   = readFile("src/test/resources/nested-file-references/eventsCase9.yaml");
            issue_323_paging_yaml   = readFile("src/test/resources/nested-file-references/common/pagingWithFolderRef.yaml");
            issue_323_bar_yaml      = readFile("src/test/resources/nested-file-references/common/common2/bar.yaml");

            issue_328_yaml          = readFile("src/test/resources/nested-file-references/issue-328.yaml");
            issue_328_events_yaml   = readFile("src/test/resources/nested-file-references/issue-328-events.yaml");
            issue_328_paging_yaml   = readFile("src/test/resources/nested-file-references/common/issue-328-paging.yaml");
            issue_328_bar_yaml      = readFile("src/test/resources/nested-file-references/common/common2/issue-328-bar.yaml");

            issue_330_yaml          = readFile("src/test/resources/nested-network-references/issue-330.yaml");
            issue_330_paging_yaml   = readFile("src/test/resources/nested-network-references/common/issue-330-paging.yaml");
            issue_330_users_yaml    = readFile("src/test/resources/nested-network-references/common/issue-330-users.yaml");
            issue_330_entities_yaml = readFile("src/test/resources/nested-network-references/common/issue-330-entities.yaml");

            issue_335_json          = readFile("src/test/resources/nested-file-references/issue-335.json");
            issue_335_bar_json      = readFile("src/test/resources/nested-file-references/issue-335-bar.json");

            issue_407_json          = readFile("src/test/resources/petstore.json");

            issue_411_server        = readFile("src/test/resources/nested-network-references/issue-411-server.yaml");
            issue_411_components    = readFile("src/test/resources/nested-network-references/issue-411-remote2.yaml");
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testIssue323() throws Exception {
        new Expectations() {{
            remoteUrl.urlToString("http://localhost:8080/nested-file-references/issue-323.yaml", new ArrayList<AuthorizationValue>());
            result = issue_323_yaml;

            remoteUrl.urlToString("http://localhost:8080/nested-file-references/eventsCase9.yaml", new ArrayList<AuthorizationValue>());
            result = issue_323_events_yaml;

            remoteUrl.urlToString("http://localhost:8080/nested-file-references/common/pagingWithFolderRef.yaml", new ArrayList<AuthorizationValue>());
            result = issue_323_paging_yaml;

            remoteUrl.urlToString("http://localhost:8080/nested-file-references/common/common2/bar.yaml", new ArrayList<AuthorizationValue>());
            result = issue_323_bar_yaml;
        }};

        SwaggerDeserializationResult result = new SwaggerParser().readWithInfo("http://localhost:8080/nested-file-references/issue-323.yaml", null, true);
        assertNotNull(result.getSwagger());

        Swagger swagger = result.getSwagger();
        assertNotNull(swagger.getPath("/events"));

        assertNotNull(swagger.getDefinitions().get("StatusResponse"));
        assertNotNull(swagger.getDefinitions().get("Paging"));
        assertNotNull(swagger.getDefinitions().get("Foobar"));
    }

    @Test
    public void testIssue328() throws Exception {
        new Expectations() {{
            remoteUrl.urlToString("http://localhost:8080/resources/swagger/issue-328.yaml", new ArrayList<AuthorizationValue>());
            result = issue_328_yaml;

            remoteUrl.urlToString("http://localhost:8080/resources/swagger/issue-328-events.yaml", new ArrayList<AuthorizationValue>());
            result = issue_328_events_yaml;

            remoteUrl.urlToString("http://localhost:8080/resources/swagger/common/issue-328-paging.yaml", new ArrayList<AuthorizationValue>());
            result = issue_328_paging_yaml;

            remoteUrl.urlToString("http://localhost:8080/resources/swagger/common/common2/issue-328-bar.yaml", new ArrayList<AuthorizationValue>());
            result = issue_328_bar_yaml;
        }};
        SwaggerDeserializationResult result = new SwaggerParser().readWithInfo("http://localhost:8080/resources/swagger/issue-328.yaml", null, true);
        assertNotNull(result.getSwagger());

        Swagger swagger = result.getSwagger();
        assertNotNull(swagger.getPath("/events"));

        assertNotNull(swagger.getDefinitions().get("StatusResponse"));
        assertNotNull(swagger.getDefinitions().get("Paging"));
        assertNotNull(swagger.getDefinitions().get("Foobar"));
    }

    @Test
    public void testIssue330() throws Exception {
        new Expectations() {{
            remoteUrl.urlToString("http://server1/resources/swagger.yaml", new ArrayList<AuthorizationValue>());
            result = issue_330_yaml;

            remoteUrl.urlToString("http://server1/resources/common/paging.yaml", new ArrayList<AuthorizationValue>());
            result = issue_330_paging_yaml;

            remoteUrl.urlToString("http://server1/resources/common/users.yaml", new ArrayList<AuthorizationValue>());
            result = issue_330_users_yaml;

            remoteUrl.urlToString("http://server2/resources/common/entities.yaml", new ArrayList<AuthorizationValue>());
            result = issue_330_entities_yaml;
        }};
        SwaggerDeserializationResult result = new SwaggerParser().readWithInfo("http://server1/resources/swagger.yaml", null, true);

        assertNotNull(result.getSwagger());
        Swagger swagger = result.getSwagger();
        assertNotNull(swagger.getPath("/events"));

        assertNotNull(swagger.getDefinitions().get("Address"));
        assertNotNull(swagger.getDefinitions().get("Paging"));
        assertNotNull(swagger.getDefinitions().get("users"));
        assertNotNull(swagger.getDefinitions().get("Phone"));
    }

    @Test
    public void testIssue335() throws Exception {
        new Expectations() {{
            remoteUrl.urlToString("http://server1/resources/swagger.json", new ArrayList<AuthorizationValue>());
            result = issue_335_json;

            remoteUrl.urlToString("http://server1/resources/Bar.json", new ArrayList<AuthorizationValue>());
            result = issue_335_bar_json;
        }};

        SwaggerDeserializationResult result = new SwaggerParser().readWithInfo("http://server1/resources/swagger.json", null, true);

        Swagger swagger = result.getSwagger();
        assertNotNull(swagger);

        assertNotNull(swagger.getDefinitions());
        assertNotNull(swagger.getDefinitions().get("BarData"));
        assertNotNull(swagger.getDefinitions().get("BarSettingsRequest"));
    }

    @Test
    public void testPathReference() throws Exception {
        new Expectations() {{
            remoteUrl.urlToString("http://petstore.swagger.io/v2/swagger.json", new ArrayList<AuthorizationValue>());
            result = issue_407_json;

        }};

        SwaggerParser parser = new SwaggerParser();
        String yaml =
                "swagger: '2.0'\n" +
                "info:\n" +
                "  description: |\n" +
                "  version: 1.0.0\n" +
                "  title: testing\n" +
                "paths:\n" +
                "   /foo:\n" +
                "     $ref: 'http://petstore.swagger.io/v2/swagger.json#/paths/~1pet'\n" +
                "   /bar:\n" +
                "     $ref: 'http://petstore.swagger.io/v2/swagger.json#/paths/~1pet'\n" +
                "schemes:\n" +
                " - https\n" +
                " - http";
        final SwaggerDeserializationResult result = parser.readWithInfo(yaml);
        Assert.assertNotNull(result.getSwagger());
        assertTrue(result.getMessages().size() == 0);
        assertTrue(result.getSwagger().getDefinitions().size() == 3);
    }

    @Test
    public void testIssue411() throws Exception {
        final List< AuthorizationValue > auths = new ArrayList<>();
        AuthorizationValue auth = new AuthorizationValue("Authorization", "OMG_SO_SEKR3T", "header");
        auths.add(auth);

        new Expectations() {{
            remoteUrl.urlToString("http://remote1/resources/swagger.json", auths);
            result = issue_411_server;

            remoteUrl.urlToString("http://remote2/resources/foo", auths);
            result = issue_411_components;
        }};

        SwaggerParser parser = new SwaggerParser();
        SwaggerDeserializationResult result = parser.readWithInfo("http://remote1/resources/swagger.json", auths, true);
        Json.prettyPrint(result);
        Swagger swagger = result.getSwagger();
        assertNotNull(swagger.getPath("/health"));
        Path health = swagger.getPath("/health");
        assertTrue(health.getGet().getParameters().size() == 0);
        Object responseRef = health.getGet().getResponsesObject().get("200").getResponseSchema();
        assertTrue(responseRef instanceof RefModel);

        RefModel refProperty = (RefModel) responseRef;
        assertEquals(refProperty.get$ref(), "#/definitions/Success");

        assertNotNull(swagger.getDefinitions().get("Success"));

        Parameter param = swagger.getPath("/stuff").getGet().getParameters().get(0);
        assertEquals(param.getIn(), "query");
        assertEquals(param.getName(), "skip");

        Response response = swagger.getPath("/stuff").getGet().getResponsesObject().get("200");
        assertNotNull(response);
        assertTrue(response.getSchema() instanceof StringProperty);

        Response error = swagger.getPath("/stuff").getGet().getResponsesObject().get("400");
        assertNotNull(error);
        Property errorProp = error.getSchema();
        assertNotNull(errorProp);
        assertTrue(errorProp instanceof RefProperty);
        RefProperty errorProperty = (RefProperty) errorProp;
        assertEquals(errorProperty.get$ref(), "#/definitions/Error");

        assertTrue(swagger.getDefinitions().get("Error") instanceof ModelImpl);
    }

    static String readFile(String name) throws Exception {
        return new String(Files.readAllBytes(new File(name).toPath()), Charset.forName("UTF-8"));
    }
}
