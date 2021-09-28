package io.swagger.v3.parser.test;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.core.util.Yaml31;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.apache.commons.io.FileUtils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Random;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class OpenAPIV31ParserTest {
    protected int serverPort = getDynamicPort();
    protected WireMockServer wireMockServer;

    private static int getDynamicPort() {
        return new Random().ints(10000, 20000).findFirst().getAsInt();
    }

    @BeforeClass
    private void setUpWireMockServer() throws IOException {
        this.wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        this.wireMockServer.start();
        this.serverPort = wireMockServer.port();
        WireMock.configureFor(this.serverPort);

    }

    @AfterClass
    private void tearDownWireMockServer() {
        this.wireMockServer.stop();
    }


    @Test(enabled = false) // TODO remove
    public void testREf() throws Exception {
        ParseOptions p = new ParseOptions();
        p.setResolve(true);
        SwaggerParseResult swaggerParseResult = new OpenAPIV3Parser().readLocation("http://localhost:8082/root.yaml", null, p);
        Yaml.prettyPrint(swaggerParseResult);
        assertNotNull(swaggerParseResult.getOpenAPI());
        assertTrue(swaggerParseResult.getMessages().isEmpty());

    }

    @Test(enabled = false) // TODO remove
    public void testREf2() throws Exception {
        ParseOptions p = new ParseOptions();
        p.setResolve(true);
        SwaggerParseResult swaggerParseResult = new OpenAPIV3Parser().readLocation("/html2/root.yaml", null, p);
        Yaml.prettyPrint(swaggerParseResult);
        assertNotNull(swaggerParseResult.getOpenAPI());
        assertTrue(swaggerParseResult.getMessages().isEmpty());

    }

    @Test
    public void testREf3() throws Exception {
        ParseOptions p = new ParseOptions();
        p.setResolve(true);
        SwaggerParseResult swaggerParseResult = new OpenAPIV3Parser().readLocation("/dati/dev/progetti/swagger/projects/swagger-parser/modules/swagger-parser-v3/src/test/resources/issue-1292/petstore.yml", null, p);

        Yaml.prettyPrint(swaggerParseResult);
        assertNotNull(swaggerParseResult.getOpenAPI());
        assertTrue(swaggerParseResult.getMessages().isEmpty());

    }

    @Test
    public void testExternalResponsesAndSchemas() throws Exception {
        ParseOptions p = new ParseOptions();
        p.setResolve(true);
        SwaggerParseResult swaggerParseResult = new OpenAPIV3Parser().readLocation("/issue-407/petstore.yml", null, p);

        Yaml.prettyPrint(swaggerParseResult);
        assertNotNull(swaggerParseResult.getOpenAPI());
        assertTrue(swaggerParseResult.getMessages().isEmpty());

    }


    @Test
    public void testBasic() throws IOException {
        String pathFile = FileUtils.readFileToString(new File("src/test/resources/3.1.0/resolve/domain.yaml"), "UTF-8");
        WireMock.stubFor(get(urlPathMatching("/domain"))
                .willReturn(aResponse()
                        .withStatus(HttpURLConnection.HTTP_OK)
                        .withHeader("Content-type", "application/json")
                        .withBody(pathFile
                                .getBytes(StandardCharsets.UTF_8))));

        pathFile = FileUtils.readFileToString(new File("src/test/resources/3.1.0/resolve/basicref.yaml"), "UTF-8");
        pathFile = pathFile.replace("${dynamicPort}", String.valueOf(this.serverPort));

        OpenAPIV3Parser parser = new OpenAPIV3Parser();
        ParseOptions options = new ParseOptions();
        options.setResolve(true);

        final SwaggerParseResult parseResult = parser.readContents(pathFile, null, options);
        Yaml31.prettyPrint(parseResult.getOpenAPI());
        assertEquals(parseResult.getMessages().size(), 0);
        assertTrue(parseResult.getOpenAPI().getComponents().getSchemas().size() == 2);
        assertTrue(parseResult.getOpenAPI().getPaths().get("/parse").getGet().getParameters().get(0).getSchema().get$ref().equals("#/components/schemas/Parse"));
    }

    @Test
    public void testBasicFully() throws IOException {
        String pathFile = FileUtils.readFileToString(new File("src/test/resources/3.1.0/resolve/domain.yaml"), "UTF-8");
        WireMock.stubFor(get(urlPathMatching("/domain"))
                .willReturn(aResponse()
                        .withStatus(HttpURLConnection.HTTP_OK)
                        .withHeader("Content-type", "application/json")
                        .withBody(pathFile
                                .getBytes(StandardCharsets.UTF_8))));

        pathFile = FileUtils.readFileToString(new File("src/test/resources/3.1.0/resolve/basicref.yaml"), "UTF-8");
        pathFile = pathFile.replace("${dynamicPort}", String.valueOf(this.serverPort));

        OpenAPIV3Parser parser = new OpenAPIV3Parser();
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        //options.setFlatten(true);
        options.setResolveFully(true);
        //options.setResolveCombinators(true);

        final SwaggerParseResult parseResult = parser.readContents(pathFile, null, options);
        Yaml31.prettyPrint(parseResult.getOpenAPI());
        assertEquals(parseResult.getMessages().size(), 0);
        assertTrue(parseResult.getOpenAPI().getComponents().getSchemas().size() == 2);
        // assertTrue(parseResult.getOpenAPI().getPaths().get("/parse").getGet().getParameters().get(0).getSchema().get$ref().equals("#/components/schemas/Parse"));
    }

    @Test(enabled = false)
    public void testBasicNested() throws IOException {
        String pathFile = FileUtils.readFileToString(new File("src/test/resources/3.1.0/resolve/domain.yaml"), "UTF-8");
        WireMock.stubFor(get(urlPathMatching("/domain"))
                .willReturn(aResponse()
                        .withStatus(HttpURLConnection.HTTP_OK)
                        .withHeader("Content-type", "application/json")
                        .withBody(pathFile
                                .getBytes(StandardCharsets.UTF_8))));

        pathFile = FileUtils.readFileToString(new File("src/test/resources/3.1.0/resolve/nestedref.yaml"), "UTF-8");
        pathFile = pathFile.replace("${dynamicPort}", String.valueOf(this.serverPort));

        OpenAPIV3Parser parser = new OpenAPIV3Parser();
        ParseOptions options = new ParseOptions();
        options.setResolve(true);

        final SwaggerParseResult parseResult = parser.readContents(pathFile, null, options);
        Yaml31.prettyPrint(parseResult.getOpenAPI());
        assertEquals(parseResult.getMessages().size(), 0);
        assertTrue(parseResult.getOpenAPI().getComponents().getSchemas().size() == 2);
        assertTrue(parseResult.getOpenAPI().getPaths().get("/parse").getGet().getParameters().get(0).getSchema().get$ref().equals("#/components/schemas/Parse"));
    }
}
