package io.swagger.parser.v3.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.swagger.oas.models.OpenAPI;
import io.swagger.oas.models.info.Info;
import io.swagger.oas.models.info.License;
import io.swagger.parser.models.SwaggerParseResult;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Paths;

public class OpenAPIDeserializerTest {

    @Test(dataProvider = "openApiSpecification")
    public void readInfoObject(JsonNode rootNode) throws Exception {
        final OpenAPIDeserializer deserializer = new OpenAPIDeserializer();
        final SwaggerParseResult result = deserializer.deserialize(rootNode);
        Assert.assertNotNull(result);
        final OpenAPI openAPI = result.getOpenAPI();
        Assert.assertNotNull(openAPI);
        final Info info = openAPI.getInfo();
        Assert.assertNotNull(info);
        Assert.assertEquals(info.getTitle(), "Sample Pet Store App");
        Assert.assertEquals(info.getDescription(), "This is a sample server for a pet store.");
        final License license = info.getLicense();
        Assert.assertNotNull(license);
        Assert.assertEquals(license.getName(), "Apache 2.0");
        Assert.assertEquals(license.getUrl(), "http://www.apache.org/licenses/LICENSE-2.0.html");
    }

    @DataProvider(name="openApiSpecification")
    private Object[][] getRootNode() throws Exception {
        final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        final JsonNode rootNode = mapper.readTree(Files.readAllBytes(Paths.get(getClass().getResource("/oas.yaml").toURI())));
        return new Object[][]{new Object[]{rootNode}};
    }
}
