package io.swagger.v3.parser.test;

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.swagger.v3.core.util.Json31;
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

public class OpenAPIV31ParserSchemaTest {
    protected int serverPort = getDynamicPort();
    protected WireMockServer wireMockServer;

    private static SwaggerParseResult readLocation(String pathname, ParseOptions p) {
        return new OpenAPIV3Parser().readLocation(new File(pathname).getAbsoluteFile().toURI().toString(), null, p);
    }

    private static int getDynamicPort() {
        return new Random().ints(10000, 20000).findFirst().getAsInt();
    }

    @BeforeClass
    private void setUpWireMockServer() throws IOException {
        this.wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        this.wireMockServer.start();
        this.serverPort = wireMockServer.port();
        WireMock.configureFor(this.serverPort);


        String pathFile = FileUtils.readFileToString(new File("src/test/resources/3.1.0/dereference/full/root.json"));
        pathFile = pathFile.replace("${dynamicPort}", String.valueOf(this.serverPort));

        WireMock.stubFor(get(urlPathMatching("/full/root.json"))
                .willReturn(aResponse()
                        .withStatus(HttpURLConnection.HTTP_OK)
                        .withHeader("Content-type", "application/json")
                        .withBody(pathFile
                                .getBytes(StandardCharsets.UTF_8))));

        pathFile = FileUtils.readFileToString(new File("src/test/resources/3.1.0/dereference/full/nested/domain.yaml"));
        pathFile = pathFile.replace("${dynamicPort}", String.valueOf(this.serverPort));

        WireMock.stubFor(get(urlPathMatching("/full/nested/domain.yaml"))
                .willReturn(aResponse()
                        .withStatus(HttpURLConnection.HTTP_OK)
                        .withHeader("Content-type", "application/yaml")
                        .withBody(pathFile
                                .getBytes(StandardCharsets.UTF_8))));


        pathFile = FileUtils.readFileToString(new File("src/test/resources/3.1.0/dereference/full/ex.json"));
        pathFile = pathFile.replace("${dynamicPort}", String.valueOf(this.serverPort));

        WireMock.stubFor(get(urlPathMatching("/full/ex.json"))
                .willReturn(aResponse()
                        .withStatus(HttpURLConnection.HTTP_OK)
                        .withHeader("Content-type", "application/json")
                        .withBody(pathFile
                                .getBytes(StandardCharsets.UTF_8))));

        pathFile = FileUtils.readFileToString(new File("src/test/resources/3.1.0/dereference/full/ex1.json"));
        pathFile = pathFile.replace("${dynamicPort}", String.valueOf(this.serverPort));

        WireMock.stubFor(get(urlPathMatching("/full/ex1.json"))
                .willReturn(aResponse()
                        .withStatus(HttpURLConnection.HTTP_OK)
                        .withHeader("Content-type", "application/json")
                        .withBody(pathFile
                                .getBytes(StandardCharsets.UTF_8))));

        pathFile = FileUtils.readFileToString(new File("src/test/resources/3.1.0/dereference/full/ex2.json"));
        pathFile = pathFile.replace("${dynamicPort}", String.valueOf(this.serverPort));

        WireMock.stubFor(get(urlPathMatching("/full/ex2.json"))
                .willReturn(aResponse()
                        .withStatus(HttpURLConnection.HTTP_OK)
                        .withHeader("Content-type", "application/json")
                        .withBody(pathFile
                                .getBytes(StandardCharsets.UTF_8))));

        pathFile = FileUtils.readFileToString(new File("src/test/resources/3.1.0/dereference/full/ex1a.json"));
        pathFile = pathFile.replace("${dynamicPort}", String.valueOf(this.serverPort));

        WireMock.stubFor(get(urlPathMatching("/full/ex1a.json"))
                .willReturn(aResponse()
                        .withStatus(HttpURLConnection.HTTP_OK)
                        .withHeader("Content-type", "application/json")
                        .withBody(pathFile
                                .getBytes(StandardCharsets.UTF_8))));

        pathFile = FileUtils.readFileToString(new File("src/test/resources/3.1.0/dereference/full/nested/ex2a.json"));
        pathFile = pathFile.replace("${dynamicPort}", String.valueOf(this.serverPort));

        WireMock.stubFor(get(urlPathMatching("/full/nested/ex2a.json"))
                .willReturn(aResponse()
                        .withStatus(HttpURLConnection.HTTP_OK)
                        .withHeader("Content-type", "application/json")
                        .withBody(pathFile
                                .getBytes(StandardCharsets.UTF_8))));

        pathFile = FileUtils.readFileToString(new File("src/test/resources/3.1.0/dereference/full/nested/ex3a.json"));
        pathFile = pathFile.replace("${dynamicPort}", String.valueOf(this.serverPort));

        WireMock.stubFor(get(urlPathMatching("/full/nested/ex3a.json"))
                .willReturn(aResponse()
                        .withStatus(HttpURLConnection.HTTP_OK)
                        .withHeader("Content-type", "application/json")
                        .withBody(pathFile
                                .getBytes(StandardCharsets.UTF_8))));

        pathFile = FileUtils.readFileToString(new File("src/test/resources/3.1.0/dereference/full/ex1schema.json"));
        pathFile = pathFile.replace("${dynamicPort}", String.valueOf(this.serverPort));

        WireMock.stubFor(get(urlPathMatching("/full/ex1schema.json"))
                .willReturn(aResponse()
                        .withStatus(HttpURLConnection.HTTP_OK)
                        .withHeader("Content-type", "application/json")
                        .withBody(pathFile
                                .getBytes(StandardCharsets.UTF_8))));

        pathFile = FileUtils.readFileToString(new File("src/test/resources/3.1.0/dereference/full/ex2schema.json"));
        pathFile = pathFile.replace("${dynamicPort}", String.valueOf(this.serverPort));

        WireMock.stubFor(get(urlPathMatching("/full/ex2schema.json"))
                .willReturn(aResponse()
                        .withStatus(HttpURLConnection.HTTP_OK)
                        .withHeader("Content-type", "application/json")
                        .withBody(pathFile
                                .getBytes(StandardCharsets.UTF_8))));

        pathFile = FileUtils.readFileToString(new File("src/test/resources/3.1.0/dereference/full/ex3schema.json"));
        pathFile = pathFile.replace("${dynamicPort}", String.valueOf(this.serverPort));

        WireMock.stubFor(get(urlPathMatching("/full/ex3schema.json"))
                .willReturn(aResponse()
                        .withStatus(HttpURLConnection.HTTP_OK)
                        .withHeader("Content-type", "application/json")
                        .withBody(pathFile
                                .getBytes(StandardCharsets.UTF_8))));

    }

    @AfterClass
    private void tearDownWireMockServer() {
        this.wireMockServer.stop();
    }


    @Test
    public void test$idUrlExternal() throws Exception {
        ParseOptions p = new ParseOptions();
        p.setResolve(true);
        SwaggerParseResult swaggerParseResult = readLocation("src/test/resources/3.1.0/dereference/schema/$id-uri-external/root.json", p);
        compare("$id-uri-external", swaggerParseResult);
    }

    @Test
    public void test$idUrlEnclosing() throws Exception {
        ParseOptions p = new ParseOptions();
        p.setResolve(true);
        SwaggerParseResult swaggerParseResult = readLocation("src/test/resources/3.1.0/dereference/schema/$id-uri-enclosing/root.json", p);
        compare("$id-uri-enclosing", swaggerParseResult);
    }

    @Test
    public void test$idUrlDirect() throws Exception {
        ParseOptions p = new ParseOptions();
        p.setResolve(true);
        SwaggerParseResult swaggerParseResult = readLocation("src/test/resources/3.1.0/dereference/schema/$id-uri-direct/root.json", p);
        compare("$id-uri-direct", swaggerParseResult);
    }
    @Test
    public void test$idUrlUnresolvable() throws Exception {
        ParseOptions p = new ParseOptions();
        p.setResolve(true);
        SwaggerParseResult swaggerParseResult = readLocation("src/test/resources/3.1.0/dereference/schema/$id-unresolvable/root.json", p);
        compare("$id-unresolvable", swaggerParseResult);
    }

    @Test
    public void testAnchorExt() throws Exception {
        ParseOptions p = new ParseOptions();
        p.setResolve(true);
        SwaggerParseResult swaggerParseResult = readLocation("src/test/resources/3.1.0/dereference/schema/$anchor-external/root.json", p);
        compare("$anchor-external", swaggerParseResult);
    }

    @Test
    public void testAnchorInt() throws Exception {
        ParseOptions p = new ParseOptions();
        p.setResolve(true);
        SwaggerParseResult swaggerParseResult = readLocation("src/test/resources/3.1.0/dereference/schema/$anchor-internal/root.json", p);
        compare("$anchor-internal", swaggerParseResult);
    }

    @Test
    public void testAnchorUnresolve() throws Exception {
        ParseOptions p = new ParseOptions();
        p.setResolve(true);
        SwaggerParseResult swaggerParseResult = readLocation("src/test/resources/3.1.0/dereference/schema/$anchor-not-found/root.json", p);
        compare("$anchor-not-found", swaggerParseResult);
    }


    public void compare(String dir, SwaggerParseResult result) throws Exception {
        ObjectMapper mapper = Json31.mapper().copy();
        mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
        mapper.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
        String actual = mapper.writer(new DefaultPrettyPrinter().withObjectIndenter(new DefaultIndenter().withLinefeed("\n"))).writeValueAsString(result.getOpenAPI());
        org.testng.Assert.assertEquals(actual,
                FileUtils.readFileToString(new File("src/test/resources/3.1.0/dereference/schema/" + dir + "/dereferenced.json")));
    }

}
