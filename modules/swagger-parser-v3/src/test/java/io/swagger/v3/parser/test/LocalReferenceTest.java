package io.swagger.v3.parser.test;


import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.urlresolver.PermittedUrlsChecker;
import io.swagger.v3.parser.util.RemoteUrl;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class LocalReferenceTest {

    private MockedStatic<RemoteUrl> remoteUrl;

    @BeforeMethod
    public void mockRemoteUrl() {
        remoteUrl = Mockito.mockStatic(RemoteUrl.class);
    }

    @AfterMethod(alwaysRun = true)
    public void closeRemoteUrl() {
        remoteUrl.close();
    }

    private static String issue_454_yaml, issue_454_components_yaml;

    static {
        try {
            issue_454_yaml = readFile("src/test/resources/nested-network-references/issue-454.yaml");
            issue_454_components_yaml = readFile("src/test/resources/nested-network-references/issue-454-components.yaml");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testAuth() throws Exception {
        remoteUrl.when(() -> RemoteUrl.urlToString("https://remote-server.com/issue-454.yaml", new ArrayList<>())).thenReturn(issue_454_yaml);
        remoteUrl.when(() -> RemoteUrl.urlToString(eq("https://remote-components.com/issue-454-components"), eq(new ArrayList<>()), nullable(PermittedUrlsChecker.class))).thenReturn(issue_454_components_yaml);
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        OpenAPI swagger = new OpenAPIV3Parser().read("https://remote-server.com/issue-454.yaml",
                null, options);

        assertNotNull(swagger.getComponents().getSchemas().get("ErrorModel"));
        assertNotNull(swagger.getComponents().getSchemas().get("ModelWithNestedProperties"));

        Schema model = swagger.getComponents().getSchemas().get("ModelWithNestedProperties");
        Schema property = (Schema) model.getProperties().get("remoteProperty");
        assertNotNull(property);
        assertTrue(property.get$ref() != null);
        assertEquals(property.get$ref(), "#/components/schemas/RemoteComponent");
        assertNotNull(swagger.getComponents().getSchemas().get("NestedProperty"));

        Schema nestedModel = swagger.getComponents().getSchemas().get("NestedProperty");
        assertNotNull(nestedModel);
        assertNotNull(nestedModel.getProperties().get("name"));
        assertTrue(nestedModel.getProperties().get("name") instanceof StringSchema);
    }

    private static String readFile(String name) throws Exception {
        return new String(Files.readAllBytes(new File(name).toPath()), StandardCharsets.UTF_8);
    }
}
