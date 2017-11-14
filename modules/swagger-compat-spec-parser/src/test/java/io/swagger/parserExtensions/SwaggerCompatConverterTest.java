package io.swagger.parserExtensions;

import io.swagger.models.Swagger;
import io.swagger.parser.SwaggerCompatConverter;
import org.testng.annotations.Test;

import java.io.IOException;

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
}
