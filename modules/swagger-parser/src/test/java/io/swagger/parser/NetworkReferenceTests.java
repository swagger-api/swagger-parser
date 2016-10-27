package io.swagger.parser;

import io.swagger.models.Swagger;
import io.swagger.models.auth.AuthorizationValue;
import io.swagger.parser.util.RemoteUrl;
import io.swagger.parser.util.SwaggerDeserializationResult;
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

    private static String issue_323_yaml, eventsCase9_yaml, pagingWithFolderRef_yaml, bar_yaml;

    static {
        try {
            issue_323_yaml = new String(Files.readAllBytes(new File("src/test/resources/nested-file-references/issue-323.yaml").toPath()), Charset.forName("UTF-8"));
            eventsCase9_yaml = new String(Files.readAllBytes(new File("src/test/resources/nested-file-references/eventsCase9.yaml").toPath()), Charset.forName("UTF-8"));
            pagingWithFolderRef_yaml = new String(Files.readAllBytes(new File("src/test/resources/nested-file-references/common/pagingWithFolderRef.yaml").toPath()), Charset.forName("UTF-8"));
            bar_yaml = new String(Files.readAllBytes(new File("src/test/resources/nested-file-references/common/common2/bar.yaml").toPath()), Charset.forName("UTF-8"));
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
            result = eventsCase9_yaml;

            remoteUrl.urlToString("http://localhost:8080/nested-file-references/common/pagingWithFolderRef.yaml", new ArrayList<AuthorizationValue>());
            result = pagingWithFolderRef_yaml;

            remoteUrl.urlToString("http://localhost:8080/nested-file-references/common/common2/bar.yaml", new ArrayList<AuthorizationValue>());
            result = bar_yaml;
        }};

        String url = remoteUrl.urlToString("http://localhost:8080/nested-file-references/issue-323.yaml", null);

        System.out.println(url);

        SwaggerDeserializationResult result = new SwaggerParser().readWithInfo("http://localhost:8080/nested-file-references/issue-323.yaml", null, true);
        assertNotNull(result.getSwagger());

        Swagger swagger = result.getSwagger();
        assertNotNull(swagger.getPath("/events"));

        assertNotNull(swagger.getDefinitions().get("StatusResponse"));
        assertNotNull(swagger.getDefinitions().get("Paging"));
        assertNotNull(swagger.getDefinitions().get("Foobar"));
    }
}
