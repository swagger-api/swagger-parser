package io.swagger.parserExtensions;

import io.swagger.models.ArrayModel;
import io.swagger.models.Swagger;
import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.properties.RefProperty;
import io.swagger.parser.SwaggerCompatConverter;
import io.swagger.util.Json;
import org.testng.annotations.Test;

import java.io.IOException;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class SwaggerCompatConverterTest {
    @Test
    public void loadsSpecFromClasspath() throws IOException {
        SwaggerCompatConverter converter = new SwaggerCompatConverter();
        Swagger result = converter.read("/specs/v1_2/singleFile.json");
        assertTrue(result != null, "Didn't load spec from classpath");
    }

    @Test
    public void failsOnNonExistentSpec() throws IOException {
        SwaggerCompatConverter converter = new SwaggerCompatConverter();
        Swagger result = converter.read("specs/v1_2/not-exists.json");
        assertTrue(result == null);
    }

    @Test
    public void testIssue_641()throws IOException {
        SwaggerCompatConverter converter = new SwaggerCompatConverter();
        Swagger swagger2 = converter.read("specs/v1_2/issue-641.json");
        assertNotNull(swagger2);
        BodyParameter bodyParameter = (BodyParameter) swagger2.getPaths().get("/user/createWithArray").getPost().getParameters().get(0);
        assertEquals(((RefProperty)((ArrayModel)bodyParameter.getSchema()).getItems()).get$ref(),"#/definitions/User");
    }
}
