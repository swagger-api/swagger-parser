package io.swagger.v3.parser.test;



import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.AuthorizationValue;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import io.swagger.v3.parser.util.RemoteUrl;
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
    private static String issue_742_json;

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

            issue_407_json          = readFile("src/test/resources/petstore.yaml");

            issue_411_server        = readFile("src/test/resources/nested-network-references/issue-411-server.yaml");
            issue_411_components    = readFile("src/test/resources/nested-network-references/issue-411-remote2.yaml");

            issue_742_json          = readFile("src/test/resources/issue-742.json");
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testIssue323() throws Exception {
        new Expectations() {{
            remoteUrl.urlToString("http://localhost:8080/nested-file-references/issue-323.yaml", new ArrayList<>());
            result = issue_323_yaml;

            remoteUrl.urlToString("http://localhost:8080/nested-file-references/eventsCase9.yaml", new ArrayList<>());
            result = issue_323_events_yaml;

            remoteUrl.urlToString("http://localhost:8080/nested-file-references/common/pagingWithFolderRef.yaml", new ArrayList<>());
            result = issue_323_paging_yaml;

            remoteUrl.urlToString("http://localhost:8080/nested-file-references/common/common2/bar.yaml", new ArrayList<>());
            result = issue_323_bar_yaml;
        }};

        ParseOptions options = new ParseOptions();
        options.setResolve(true);

        SwaggerParseResult result = new OpenAPIV3Parser().readLocation("http://localhost:8080/nested-file-references/issue-323.yaml", new ArrayList<>(), options);
        assertNotNull(result.getOpenAPI());

        OpenAPI swagger = result.getOpenAPI();
        assertNotNull(swagger.getPaths().get("/events"));

        assertNotNull(swagger.getComponents().getSchemas().get("StatusResponse"));
        assertNotNull(swagger.getComponents().getSchemas().get("Paging"));
        assertNotNull(swagger.getComponents().getSchemas().get("Foobar"));
    }

    @Test
    public void testIssue328() throws Exception {
        new Expectations() {{
            remoteUrl.urlToString("http://localhost:8080/resources/swagger/issue-328.yaml", new ArrayList<>());
            result = issue_328_yaml;

            remoteUrl.urlToString("http://localhost:8080/resources/swagger/issue-328-events.yaml", new ArrayList<>());
            result = issue_328_events_yaml;

            remoteUrl.urlToString("http://localhost:8080/resources/swagger/common/issue-328-paging.yaml", new ArrayList<>());
            result = issue_328_paging_yaml;

            remoteUrl.urlToString("http://localhost:8080/resources/swagger/common/common2/issue-328-bar.yaml", new ArrayList<>());
            result = issue_328_bar_yaml;
        }};

        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        SwaggerParseResult result = new OpenAPIV3Parser().readLocation("http://localhost:8080/resources/swagger/issue-328.yaml", new ArrayList<>(), options);
        assertNotNull(result.getOpenAPI());

        OpenAPI swagger = result.getOpenAPI();
        assertNotNull(swagger.getPaths().get("/events"));

        assertNotNull(swagger.getComponents().getSchemas().get("StatusResponse"));
        assertNotNull(swagger.getComponents().getSchemas().get("Paging"));
        assertNotNull(swagger.getComponents().getSchemas().get("Foobar"));
    }

    @Test
    public void testIssue330() throws Exception {
        new Expectations() {{
            remoteUrl.urlToString("http://server1/resources/swagger.yaml", new ArrayList<>());
            result = issue_330_yaml;

            remoteUrl.urlToString("http://server1/resources/common/paging.yaml", new ArrayList<>());
            result = issue_330_paging_yaml;

            remoteUrl.urlToString("http://server1/resources/common/users.yaml", new ArrayList<>());
            result = issue_330_users_yaml;

            remoteUrl.urlToString("http://server2/resources/common/entities.yaml", new ArrayList<>());
            result = issue_330_entities_yaml;
        }};
        ParseOptions options = new ParseOptions();
        options.setResolve(true);

        SwaggerParseResult result = new OpenAPIV3Parser().readLocation("http://server1/resources/swagger.yaml", new ArrayList<>(), options);

        assertNotNull(result.getOpenAPI());
        OpenAPI swagger = result.getOpenAPI();
        assertNotNull(swagger.getPaths().get("/events"));

        assertNotNull(swagger.getComponents().getSchemas().get("Address"));
        assertNotNull(swagger.getComponents().getSchemas().get("Paging"));
        assertNotNull(swagger.getComponents().getSchemas().get("users"));
        assertNotNull(swagger.getComponents().getSchemas().get("Phone"));
    }

    @Test
    public void testIssue335() throws Exception {
        new Expectations() {{
            remoteUrl.urlToString("http://server1/resources/swagger.json", new ArrayList<>());
            result = issue_335_json;

            remoteUrl.urlToString("http://server1/resources/Bar.json", new ArrayList<>());
            result = issue_335_bar_json;
        }};


        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        SwaggerParseResult result = new OpenAPIV3Parser().readLocation("http://server1/resources/swagger.json", new ArrayList<>(), options);

        OpenAPI swagger = result.getOpenAPI();
        assertNotNull(swagger);

        assertNotNull(swagger.getComponents().getSchemas());
        assertNotNull(swagger.getComponents().getSchemas().get("BarData"));
        assertNotNull(swagger.getComponents().getSchemas().get("BarSettingsRequest"));
    }

    @Test
    public void testPathReference() throws Exception {
        new Expectations() {{
            remoteUrl.urlToString("http://petstore.swagger.io/v2/swagger.json", new ArrayList<>());
            result = issue_407_json;

        }};

        OpenAPIV3Parser parser = new OpenAPIV3Parser();
        String yaml = "openapi: 3.0.0\n" +
                "info:\n" +
                "  description: ''\n" +
                "  version: 1.0.0\n" +
                "  title: testing\n" +
                "paths:\n" +
                "  /foo:\n" +
                "    $ref: 'http://petstore.swagger.io/v2/swagger.json#/paths/~1pet'\n" +
                "  /bar:\n" +
                "    $ref: 'http://petstore.swagger.io/v2/swagger.json#/paths/~1pet'\n";


        final SwaggerParseResult result = parser.readContents(yaml);
        Assert.assertNotNull(result.getOpenAPI());
        assertTrue(result.getMessages().size() == 0);
        assertTrue(result.getOpenAPI().getComponents().getSchemas().size() == 3);
    }

    @Test
    public void testIssue411() throws Exception {
        final List< AuthorizationValue > auths = new ArrayList<>();
        AuthorizationValue auth = new AuthorizationValue("Authorization", "OMG_SO_SEKR3T", "header");
        auths.add(auth);

        new Expectations() {{
            remoteUrl.urlToString("http://remote1/resources/swagger.yaml", auths);
            result = issue_411_server;

            remoteUrl.urlToString("http://remote2/resources/foo", auths);
            result = issue_411_components;
        }};

        OpenAPIV3Parser parser = new OpenAPIV3Parser();
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        SwaggerParseResult result = parser.readLocation("http://remote1/resources/swagger.yaml", auths, options);

        OpenAPI swagger = result.getOpenAPI();
        assertNotNull(swagger.getPaths().get("/health"));
        PathItem health = swagger.getPaths().get("/health");
        assertTrue(health.getGet().getParameters().size() == 0);
        Schema responseRef = health.getGet().getResponses().get("200").getContent().get("*/*").getSchema();
        assertTrue(responseRef.get$ref() != null);


        assertEquals(responseRef.get$ref(), "#/components/schemas/Success");

        assertNotNull(swagger.getComponents().getSchemas().get("Success"));

        Parameter param = swagger.getPaths().get("/stuff").getGet().getParameters().get(0);
        assertEquals(param.getIn(), "query");
        assertEquals(param.getName(), "skip");

        ApiResponse response = swagger.getPaths().get("/stuff").getGet().getResponses().get("200");
        assertNotNull(response);
        assertTrue(response.getContent().get("*/*").getSchema() instanceof StringSchema);

        ApiResponse error = swagger.getPaths().get("/stuff").getGet().getResponses().get("400");
        assertNotNull(error);
        Schema errorProp = error.getContent().get("*/*").getSchema();
        assertNotNull(errorProp);
        assertTrue(errorProp.get$ref() != null);
        assertEquals(errorProp.get$ref(), "#/components/schemas/Error");

        assertTrue(swagger.getComponents().getSchemas().get("Error") instanceof Schema);
    }
    
    @Test
    public void testIssue742() throws Exception {
        final List< AuthorizationValue > auths = new ArrayList<>();

        new Expectations() {{
            remoteUrl.urlToString("http://www.example.io/one/two/swagger.json", auths);
            result = issue_742_json;
        }};

        OpenAPIV3Parser parser = new OpenAPIV3Parser();
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        SwaggerParseResult result = parser.readLocation("http://www.example.io/one/two/swagger.json", auths, options);

        Assert.assertNotNull(result.getOpenAPI());
        Assert.assertNotNull(result.getOpenAPI().getServers());
        Assert.assertEquals(result.getOpenAPI().getServers().size(), 4);
        Assert.assertEquals(result.getOpenAPI().getServers().get(0).getDescription(), "An absolute path");
        Assert.assertEquals(result.getOpenAPI().getServers().get(0).getUrl(), "https://api.absolute.org/v2");
        Assert.assertEquals(result.getOpenAPI().getServers().get(1).getDescription(), "Server relative to root path");
        Assert.assertEquals(result.getOpenAPI().getServers().get(1).getUrl(), "http://www.example.io/api/v2");
        Assert.assertEquals(result.getOpenAPI().getServers().get(2).getDescription(), "Server relative path 1");
        Assert.assertEquals(result.getOpenAPI().getServers().get(2).getUrl(), "http://www.example.io/one/two/path/v2");
        Assert.assertEquals(result.getOpenAPI().getServers().get(3).getDescription(), "Server relative path 2");
        Assert.assertEquals(result.getOpenAPI().getServers().get(3).getUrl(), "http://www.example.io/one/v2");
    }

    static String readFile(String name) throws Exception {
        return new String(Files.readAllBytes(new File(name).toPath()), Charset.forName("UTF-8"));
    }
}
