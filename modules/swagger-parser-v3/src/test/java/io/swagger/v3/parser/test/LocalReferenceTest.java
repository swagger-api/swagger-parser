package io.swagger.v3.parser.test;



import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.util.RemoteUrl;
import mockit.Expectations;
import mockit.Mocked;
import org.testng.annotations.Test;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class LocalReferenceTest {
    @Mocked
    public RemoteUrl remoteUrl = new RemoteUrl();

    static String issue_454_yaml, issue_454_components_yaml;

    static {
        try {
            issue_454_yaml = readFile("src/test/resources/nested-network-references/issue-454.yaml");
            issue_454_components_yaml = readFile("src/test/resources/nested-network-references/issue-454-components.yaml");
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testAuth() throws Exception  {
        new Expectations() {{
            remoteUrl.urlToString("https://remote-server.com/issue-454.yaml", new ArrayList<>());
            result = issue_454_yaml;

            remoteUrl.urlToString("https://remote-components.com/issue-454-components", new ArrayList<>());
            result = issue_454_components_yaml;
        }};
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

    static String readFile(String name) throws Exception {
        return new String(Files.readAllBytes(new File(name).toPath()), Charset.forName("UTF-8"));
    }
}
