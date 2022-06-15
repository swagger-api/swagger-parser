package io.swagger.v3.parser.test;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
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
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class OpenAPIV31ParserPathItemTest {
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


        // /basic

        String pathFile = FileUtils.readFileToString(new File("src/test/resources/3.1.0/dereference/pathItem/basic/root.yaml"));
        pathFile = pathFile.replace("${dynamicPort}", String.valueOf(this.serverPort));

        WireMock.stubFor(get(urlPathMatching("/basic/root.yaml"))
                .willReturn(aResponse()
                        .withStatus(HttpURLConnection.HTTP_OK)
                        .withHeader("Content-type", "application/yaml")
                        .withBody(pathFile
                                .getBytes(StandardCharsets.UTF_8))));

        pathFile = FileUtils.readFileToString(new File("src/test/resources/3.1.0/dereference/pathItem/basic/nested/domain.yaml"));
        pathFile = pathFile.replace("${dynamicPort}", String.valueOf(this.serverPort));

        WireMock.stubFor(get(urlPathMatching("/basic/nested/domain.yaml"))
                .willReturn(aResponse()
                        .withStatus(HttpURLConnection.HTTP_OK)
                        .withHeader("Content-type", "application/yaml")
                        .withBody(pathFile
                                .getBytes(StandardCharsets.UTF_8))));


        // /external-only/root.json
        pathFile = FileUtils.readFileToString(new File("src/test/resources/3.1.0/dereference/pathItem/external-only/root.json"));
        pathFile = pathFile.replace("${dynamicPort}", String.valueOf(this.serverPort));

        WireMock.stubFor(get(urlPathMatching("/external-only/root.json"))
                .willReturn(aResponse()
                        .withStatus(HttpURLConnection.HTTP_OK)
                        .withHeader("Content-type", "application/json")
                        .withBody(pathFile
                                .getBytes(StandardCharsets.UTF_8))));

        pathFile = FileUtils.readFileToString(new File("src/test/resources/3.1.0/dereference/pathItem/external-only/ex.json"));
        pathFile = pathFile.replace("${dynamicPort}", String.valueOf(this.serverPort));

        WireMock.stubFor(get(urlPathMatching("/external-only/ex.json"))
                .willReturn(aResponse()
                        .withStatus(HttpURLConnection.HTTP_OK)
                        .withHeader("Content-type", "application/json")
                        .withBody(pathFile
                                .getBytes(StandardCharsets.UTF_8))));

        // external-indirections
        pathFile = FileUtils.readFileToString(new File("src/test/resources/3.1.0/dereference/pathItem/external-indirections/root.json"));
        pathFile = pathFile.replace("${dynamicPort}", String.valueOf(this.serverPort));

        WireMock.stubFor(get(urlPathMatching("/external-indirections/root.json"))
                .willReturn(aResponse()
                        .withStatus(HttpURLConnection.HTTP_OK)
                        .withHeader("Content-type", "application/json")
                        .withBody(pathFile
                                .getBytes(StandardCharsets.UTF_8))));

        pathFile = FileUtils.readFileToString(new File("src/test/resources/3.1.0/dereference/pathItem/external-indirections/ex1.json"));
        pathFile = pathFile.replace("${dynamicPort}", String.valueOf(this.serverPort));

        WireMock.stubFor(get(urlPathMatching("/external-indirections/ex1.json"))
                .willReturn(aResponse()
                        .withStatus(HttpURLConnection.HTTP_OK)
                        .withHeader("Content-type", "application/json")
                        .withBody(pathFile
                                .getBytes(StandardCharsets.UTF_8))));

        pathFile = FileUtils.readFileToString(new File("src/test/resources/3.1.0/dereference/pathItem/external-indirections/ex2.json"));
        pathFile = pathFile.replace("${dynamicPort}", String.valueOf(this.serverPort));

        WireMock.stubFor(get(urlPathMatching("/external-indirections/ex2.json"))
                .willReturn(aResponse()
                        .withStatus(HttpURLConnection.HTTP_OK)
                        .withHeader("Content-type", "application/json")
                        .withBody(pathFile
                                .getBytes(StandardCharsets.UTF_8))));

        // direct-external-circular

        pathFile = FileUtils.readFileToString(new File("src/test/resources/3.1.0/dereference/pathItem/direct-external-circular/root.json"));
        pathFile = pathFile.replace("${dynamicPort}", String.valueOf(this.serverPort));

        WireMock.stubFor(get(urlPathMatching("/direct-external-circular/root.json"))
                .willReturn(aResponse()
                        .withStatus(HttpURLConnection.HTTP_OK)
                        .withHeader("Content-type", "application/json")
                        .withBody(pathFile
                                .getBytes(StandardCharsets.UTF_8))));

        pathFile = FileUtils.readFileToString(new File("src/test/resources/3.1.0/dereference/pathItem/direct-external-circular/ex.json"));
        pathFile = pathFile.replace("${dynamicPort}", String.valueOf(this.serverPort));

        WireMock.stubFor(get(urlPathMatching("/direct-external-circular/ex.json"))
                .willReturn(aResponse()
                        .withStatus(HttpURLConnection.HTTP_OK)
                        .withHeader("Content-type", "application/json")
                        .withBody(pathFile
                                .getBytes(StandardCharsets.UTF_8))));

        // indirect-external-circular
        pathFile = FileUtils.readFileToString(new File("src/test/resources/3.1.0/dereference/pathItem/indirect-external-circular/root.json"));
        pathFile = pathFile.replace("${dynamicPort}", String.valueOf(this.serverPort));

        WireMock.stubFor(get(urlPathMatching("/indirect-external-circular/root.json"))
                .willReturn(aResponse()
                        .withStatus(HttpURLConnection.HTTP_OK)
                        .withHeader("Content-type", "application/json")
                        .withBody(pathFile
                                .getBytes(StandardCharsets.UTF_8))));

        pathFile = FileUtils.readFileToString(new File("src/test/resources/3.1.0/dereference/pathItem/indirect-external-circular/ex1.json"));
        pathFile = pathFile.replace("${dynamicPort}", String.valueOf(this.serverPort));

        WireMock.stubFor(get(urlPathMatching("/indirect-external-circular/ex1.json"))
                .willReturn(aResponse()
                        .withStatus(HttpURLConnection.HTTP_OK)
                        .withHeader("Content-type", "application/json")
                        .withBody(pathFile
                                .getBytes(StandardCharsets.UTF_8))));

        pathFile = FileUtils.readFileToString(new File("src/test/resources/3.1.0/dereference/pathItem/indirect-external-circular/ex2.json"));
        pathFile = pathFile.replace("${dynamicPort}", String.valueOf(this.serverPort));

        WireMock.stubFor(get(urlPathMatching("/indirect-external-circular/ex2.json"))
                .willReturn(aResponse()
                        .withStatus(HttpURLConnection.HTTP_OK)
                        .withHeader("Content-type", "application/json")
                        .withBody(pathFile
                                .getBytes(StandardCharsets.UTF_8))));


        // internal-external

        pathFile = FileUtils.readFileToString(new File("src/test/resources/3.1.0/dereference/pathItem/internal-external/root.json"));
        pathFile = pathFile.replace("${dynamicPort}", String.valueOf(this.serverPort));

        WireMock.stubFor(get(urlPathMatching("/internal-external/root.json"))
                .willReturn(aResponse()
                        .withStatus(HttpURLConnection.HTTP_OK)
                        .withHeader("Content-type", "application/json")
                        .withBody(pathFile
                                .getBytes(StandardCharsets.UTF_8))));

        pathFile = FileUtils.readFileToString(new File("src/test/resources/3.1.0/dereference/pathItem/internal-external/ex.json"));
        pathFile = pathFile.replace("${dynamicPort}", String.valueOf(this.serverPort));

        WireMock.stubFor(get(urlPathMatching("/internal-external/ex.json"))
                .willReturn(aResponse()
                        .withStatus(HttpURLConnection.HTTP_OK)
                        .withHeader("Content-type", "application/json")
                        .withBody(pathFile
                                .getBytes(StandardCharsets.UTF_8))));

        // external-internal-nested

        pathFile = FileUtils.readFileToString(new File("src/test/resources/3.1.0/dereference/pathItem/external-internal-nested/root.yaml"));
        pathFile = pathFile.replace("${dynamicPort}", String.valueOf(this.serverPort));

        WireMock.stubFor(get(urlPathMatching("/external-internal-nested/root.yaml"))
                .willReturn(aResponse()
                        .withStatus(HttpURLConnection.HTTP_OK)
                        .withHeader("Content-type", "application/yaml")
                        .withBody(pathFile
                                .getBytes(StandardCharsets.UTF_8))));

        pathFile = FileUtils.readFileToString(new File("src/test/resources/3.1.0/dereference/pathItem/external-internal-nested/nested/domain.yaml"));
        pathFile = pathFile.replace("${dynamicPort}", String.valueOf(this.serverPort));

        WireMock.stubFor(get(urlPathMatching("/external-internal-nested/nested/domain.yaml"))
                .willReturn(aResponse()
                        .withStatus(HttpURLConnection.HTTP_OK)
                        .withHeader("Content-type", "application/yaml")
                        .withBody(pathFile
                                .getBytes(StandardCharsets.UTF_8))));
    }

    @AfterClass
    private void tearDownWireMockServer() {
        this.wireMockServer.stop();
    }


    @Test
    public void testPathItemBasicRef() throws Exception {
        ParseOptions p = new ParseOptions();
        p.setResolve(true);
        String uri = "http://localhost:" + this.serverPort + "/basic/root.yaml";
        SwaggerParseResult swaggerParseResult = new OpenAPIV3Parser().readLocation(uri, null, p);
        assertNotNull(swaggerParseResult.getOpenAPI());
        assertTrue(swaggerParseResult.getMessages().isEmpty());
        assertEquals(Yaml31.pretty(swaggerParseResult.getOpenAPI()), "openapi: 3.1.0\n" +
                "info:\n" +
                "  title: parse-api\n" +
                "  description: Test swagger-parser\n" +
                "  version: 1.0.0\n" +
                "servers:\n" +
                "- url: /\n" +
                "paths:\n" +
                "  /externalref:\n" +
                "    get:\n" +
                "      description: ExternalRef PathItem\n" +
                "      operationId: ExternalRef PathItem\n" +
                "      responses:\n" +
                "        \"200\":\n" +
                "          description: OK\n" +
                "  /relativeref:\n" +
                "    get:\n" +
                "      description: RelativeRef PathItem\n" +
                "      operationId: RelativeRef PathItem\n" +
                "      responses:\n" +
                "        \"200\":\n" +
                "          description: OK\n" +
                "  /internalref:\n" +
                "    $ref: '#/components/pathItems/InternalRef'\n" +
                "  /internalreftoexternal:\n" +
                "    $ref: '#/components/pathItems/InternalRefToExternal'\n" +
                "  /internal:\n" +
                "    get:\n" +
                "      description: Internal PathItem\n" +
                "      operationId: Internal PathItem\n" +
                "      responses:\n" +
                "        \"200\":\n" +
                "          description: OK\n" +
                "components:\n" +
                "  pathItems:\n" +
                "    InternalRefToExternal:\n" +
                "      get:\n" +
                "        description: DomainInternalRefToExternal PathItem\n" +
                "        operationId: DomainInternalRefToExternal PathItem\n" +
                "        responses:\n" +
                "          \"200\":\n" +
                "            description: OK\n" +
                "    InternalRef:\n" +
                "      get:\n" +
                "        description: InternalRef PathItem\n" +
                "        operationId: InternalRef PathItem\n" +
                "        responses:\n" +
                "          \"200\":\n" +
                "            description: OK\n");


    }

    @Test
    public void testPathItemExternalInternalNestedRef() throws Exception {
        ParseOptions p = new ParseOptions();
        p.setResolve(true);
        String uri = "http://localhost:" + this.serverPort + "/external-internal-nested/root.yaml";
        SwaggerParseResult swaggerParseResult = new OpenAPIV3Parser().readLocation(uri, null, p);
        assertNotNull(swaggerParseResult.getOpenAPI());
        assertTrue(swaggerParseResult.getMessages().isEmpty());
        assertEquals(Yaml31.pretty(swaggerParseResult.getOpenAPI()), "openapi: 3.1.0\n" +
                "info:\n" +
                "  title: parse-api\n" +
                "  description: Test swagger-parser\n" +
                "  version: 1.0.0\n" +
                "servers:\n" +
                "- url: /\n" +
                "paths:\n" +
                "  /externalref:\n" +
                "    get:\n" +
                "      description: InternalDomainRef PathItem\n" +
                "      operationId: InternalDomainRef PathItem\n" +
                "      responses:\n" +
                "        \"200\":\n" +
                "          description: OK\n" +
                "  /relativeref:\n" +
                "    get:\n" +
                "      description: RelativeRef PathItem\n" +
                "      operationId: RelativeRef PathItem\n" +
                "      responses:\n" +
                "        \"200\":\n" +
                "          description: OK\n" +
                "  /internalref:\n" +
                "    $ref: '#/components/pathItems/InternalRef'\n" +
                "  /internal:\n" +
                "    get:\n" +
                "      description: Internal PathItem\n" +
                "      operationId: Internal PathItem\n" +
                "      responses:\n" +
                "        \"200\":\n" +
                "          description: OK\n" +
                "components:\n" +
                "  pathItems:\n" +
                "    InternalRef:\n" +
                "      get:\n" +
                "        description: InternalRef PathItem\n" +
                "        operationId: InternalRef PathItem\n" +
                "        responses:\n" +
                "          \"200\":\n" +
                "            description: OK\n");


    }

    @Test
    public void testPathItemExternalOnlyRef() throws Exception {
        ParseOptions p = new ParseOptions();
        p.setResolve(true);
        String uri = "http://localhost:" + this.serverPort + "/external-only/root.json";
        SwaggerParseResult swaggerParseResult = new OpenAPIV3Parser().readLocation(uri, null, p);
        assertNotNull(swaggerParseResult.getOpenAPI());
        assertFalse(swaggerParseResult.getMessages().isEmpty());
        assertEquals(Yaml31.pretty(swaggerParseResult.getOpenAPI()), "openapi: 3.1.0\n" +
                "servers:\n" +
                "- url: /\n" +
                "paths:\n" +
                "  /path1:\n" +
                "    summary: path item summary\n" +
                "    description: path item description\n" +
                "    get: {}\n");
    }

    @Test
    public void testPathItemExternalIndirectionsRef() throws Exception {
        ParseOptions p = new ParseOptions();
        p.setResolve(true);
        String uri = "http://localhost:" + this.serverPort + "/external-indirections/root.json";
        SwaggerParseResult swaggerParseResult = new OpenAPIV3Parser().readLocation(uri, null, p);
        assertNotNull(swaggerParseResult.getOpenAPI());
        assertFalse(swaggerParseResult.getMessages().isEmpty());
        assertEquals(Yaml31.pretty(swaggerParseResult.getOpenAPI()), "openapi: 3.1.0\n" +
                "servers:\n" +
                "- url: /\n" +
                "paths:\n" +
                "  /path1:\n" +
                "    summary: path item summary\n" +
                "    description: path item description\n" +
                "    get: {}\n");
    }

    @Test
    public void testPathItemDirectExternalCircularRef() throws Exception {
        ParseOptions p = new ParseOptions();
        p.setResolve(true);
        String uri = "http://localhost:" + this.serverPort + "/direct-external-circular/root.json";
        SwaggerParseResult swaggerParseResult = new OpenAPIV3Parser().readLocation(uri, null, p);
        assertNotNull(swaggerParseResult.getOpenAPI());
        assertFalse(swaggerParseResult.getMessages().isEmpty());
        assertEquals(Yaml31.pretty(swaggerParseResult.getOpenAPI()), "openapi: 3.1.0\n" +
                "servers:\n" +
                "- url: /\n" +
                "paths:\n" +
                "  /path1:\n" +
                "    $ref: ./ex.json\n");
    }

    @Test
    public void testPathItemIndirectExternalCircularRef() throws Exception {
        ParseOptions p = new ParseOptions();
        p.setResolve(true);
        String uri = "http://localhost:" + this.serverPort + "/indirect-external-circular/root.json";
        SwaggerParseResult swaggerParseResult = new OpenAPIV3Parser().readLocation(uri, null, p);
        assertNotNull(swaggerParseResult.getOpenAPI());
        assertFalse(swaggerParseResult.getMessages().isEmpty());
        assertEquals(Yaml31.pretty(swaggerParseResult.getOpenAPI()), "openapi: 3.1.0\n" +
                "servers:\n" +
                "- url: /\n" +
                "paths:\n" +
                "  /path1:\n" +
                "    $ref: ./ex1.json\n");
    }

    @Test
    public void testPathItemInternalExternalRef() throws Exception {
        ParseOptions p = new ParseOptions();
        p.setResolve(true);
        String uri = "http://localhost:" + this.serverPort + "/internal-external/root.json";
        SwaggerParseResult swaggerParseResult = new OpenAPIV3Parser().readLocation(uri, null, p);
        assertNotNull(swaggerParseResult.getOpenAPI());
        assertFalse(swaggerParseResult.getMessages().isEmpty());
        assertEquals(Yaml31.pretty(swaggerParseResult.getOpenAPI()), "openapi: 3.1.0\n" +
                "servers:\n" +
                "- url: /\n" +
                "paths:\n" +
                "  /path1:\n" +
                "    summary: path item summary\n" +
                "    description: path item description\n" +
                "    get: {}\n" +
                "  /path3:\n" +
                "    summary: path item summary\n" +
                "    description: path item description\n" +
                "    get: {}\n" +
                "  /path4:\n" +
                "    summary: path item summary\n" +
                "    description: path item description\n" +
                "    get: {}\n");
    }
}
