package io.swagger.parser;

import io.swagger.models.Swagger;
import io.swagger.models.auth.AuthorizationValue;
import io.swagger.parser.util.RemoteUrl;
import io.swagger.parser.util.SwaggerDeserializationResult;
import io.swagger.util.Json;
import mockit.Expectations;
import mockit.Mocked;
import org.testng.annotations.Test;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;

import static org.testng.Assert.assertNotNull;

public class NetworkReferenceTests {
    @Mocked
    public RemoteUrl remoteUrl = new RemoteUrl();

    private static String issue_323_yaml, issue_323_events_yaml, issue_323_paging_yaml, issue_323_bar_yaml;
    private static String issue_328_yaml, issue_328_events_yaml, issue_328_paging_yaml, issue_328_bar_yaml;
    private static String issue_330_yaml, issue_330_users_yaml, issue_330_paging_yaml, issue_330_entities_yaml;

    private static String issue_335_json, issue_335_bar_json;

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
        Json.prettyPrint(result);
    }

    static String readFile(String name) {
        try {
            return new String(Files.readAllBytes(new File(name).toPath()), Charset.forName("UTF-8"));
        }
        catch (Exception e) {
            return null;
        }
    }
}
