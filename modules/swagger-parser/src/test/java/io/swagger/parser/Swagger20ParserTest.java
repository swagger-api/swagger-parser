package io.swagger.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.models.Swagger;
import io.swagger.models.auth.AuthorizationValue;
import io.swagger.parser.util.ClasspathHelper;
import io.swagger.parser.util.RemoteUrl;
import io.swagger.parser.util.SwaggerDeserializationResult;
import io.swagger.util.Json;
import mockit.*;
import org.apache.commons.io.FileUtils;
import org.testng.annotations.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.testng.Assert.*;

public class Swagger20ParserTest {

    @Tested
    Swagger20Parser parser;

    @Mocked
    RemoteUrl remoteUrl;

    @Mocked
    FileUtils fileUtils;

    @Mocked
    ClasspathHelper classpathHelper;

    @Mocked
    Files files;
    /*

    @Mocked
    Json json;

    @Mocked
    ObjectMapper objectMapper;

    @Mocked
    Paths paths;

    @Injectable
    JsonNode jsonNode;
    */

    @Injectable
    Path path;

    @Injectable
    Swagger swagger;

    final String expectedJson = "{ \"swagger\": \"2.0\" }";

    @Test
    public void testSupportsTrue() throws Exception {
        ObjectMapper mapper = Json.mapper();
        JsonNode node = mapper.readTree("{ \"swagger\": \"2.0\" }");
        assertTrue(parser.supports(node));
    }

    @Test
    public void testSupportsFalseOldVersion() throws Exception {
        ObjectMapper mapper = Json.mapper();
        JsonNode node = mapper.readTree("{\"swaggerVersion\" : \"1.2\"}");
        assertFalse(parser.supports(node));
    }

    @Test
    public void testSupportsFalseInvalidVersion() throws Exception {
        ObjectMapper mapper = Json.mapper();
        JsonNode node = mapper.readTree("{ \"swagger\": \"ERROR\" }");
        assertFalse(parser.supports(node));
    }

//    @Test
    public void testRead_UrlLocation(@Injectable final List<AuthorizationValue> auths) throws Exception {

        final String location = "http://foo.com/path/to/bar.json";

        new StrictExpectations(){{
            RemoteUrl.urlToString(location, auths); times=1; result=expectedJson;
        }};

        setupCommonExpectations();

        doTest(location, auths);
    }

//    @Test
    public void testRead_RelativeFile(@Injectable final File file) throws Exception {

        final String location = "./path/to/file.json";

        new StrictExpectations(){{
            Paths.get(location); times=1; result=path;
            Files.exists(path); times=1; result=true;
            path.toFile(); times=1; result=file;
            FileUtils.readFileToString(file, "UTF-8"); times=1; result=expectedJson;
        }};

        setupCommonExpectations();

        doTest(location, null);
    }

    private void doTest(final String location, @Injectable final List<AuthorizationValue> auths) throws Exception {
        final SwaggerDeserializationResult result = parseLocation(location, auths);
        Swagger actualSwagger = result.getSwagger();

        new FullVerifications(){{}};

        assertEquals(actualSwagger, swagger);
    }

    private void setupCommonExpectations() throws Exception {
      /*
        new StrictExpectations(){{
            Json.mapper(); times=1; result=objectMapper;

            objectMapper.readTree(expectedJson); times=1; result = jsonNode;

            jsonNode.get("swagger"); times=1; result=jsonNode;

            objectMapper.convertValue(jsonNode, Swagger.class); times=1; result=swagger;
        }};
        */
    }

    private SwaggerDeserializationResult parseLocation(final String location, final List<AuthorizationValue> auths) {
      SwaggerDeserializationResult result;
      try {
          String data = FileUtils.readFileToString(Paths.get(location).toFile(), "UTF-8");
          ObjectMapper mapper = Json.mapper();
          JsonNode node = mapper.readTree(data);
          result = parser.parseContents(node, auths, location, true);
      } catch (Exception e) {
          throw new RuntimeException("Error parsing " + location, e);
      }
      return result;
  }
}
