package io.swagger.parser;

import io.swagger.models.ModelImpl;
import io.swagger.models.Swagger;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.RefProperty;
import io.swagger.models.properties.StringProperty;
import io.swagger.parser.util.RemoteUrl;
import mockit.Expectations;
import mockit.Mocked;
import org.testng.annotations.Test;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;

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
            remoteUrl.urlToString("https://remote-server.com/issue-454.yaml", null);
            result = issue_454_yaml;

            remoteUrl.urlToString("https://remote-components.com/issue-454-components", null);
            result = issue_454_components_yaml;
        }};

        Swagger swagger = new SwaggerParser().read("https://remote-server.com/issue-454.yaml",
            null, true);

        assertNotNull(swagger.getDefinitions().get("ErrorModel"));
        assertNotNull(swagger.getDefinitions().get("ModelWithNestedProperties"));

        ModelImpl model = (ModelImpl)swagger.getDefinitions().get("ModelWithNestedProperties");
        Property property = model.getProperties().get("remoteProperty");
        assertNotNull(property);
        assertTrue(property instanceof RefProperty);
        RefProperty ref = (RefProperty) property;
        assertEquals(ref.get$ref(), "#/definitions/RemoteComponent");
        assertNotNull(swagger.getDefinitions().get("NestedProperty"));

        ModelImpl nestedModel = (ModelImpl)swagger.getDefinitions().get("NestedProperty");
        assertNotNull(nestedModel);
        assertNotNull(nestedModel.getProperties().get("name"));
        assertTrue(nestedModel.getProperties().get("name") instanceof StringProperty);
    }

    static String readFile(String name) throws Exception {
        return new String(Files.readAllBytes(new File(name).toPath()), Charset.forName("UTF-8"));
    }
}
