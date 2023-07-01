package io.swagger.v3.parser.test;


import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;

import static org.testng.Assert.assertEquals;

import static org.testng.Assert.assertNotNull;

import static org.testng.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;


import org.apache.commons.io.FileUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.AuthorizationValue;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;



public class OpenAPIV3ParserRemoteResolvingTest {
    protected int serverPort = getDynamicPort();
    protected WireMockServer wireMockServer;

    List<AuthorizationValue> auths = new ArrayList<>();

    @BeforeClass
    private void setUpWireMockServer() throws IOException {
        this.wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        this.wireMockServer.start();
        this.serverPort = wireMockServer.port();
        WireMock.configureFor(this.serverPort);

        String pathFile = FileUtils.readFileToString(new File("src/test/resources/remote_references/remote_pathItem.yaml.template"));

        WireMock.stubFor(get(urlPathMatching("/remote/path"))
                .willReturn(aResponse()
                        .withStatus(HttpURLConnection.HTTP_OK)
                        .withHeader("Content-type", "application/yaml")
                        .withBody(pathFile
                                .getBytes(StandardCharsets.UTF_8))));

        pathFile = FileUtils.readFileToString(new File("src/test/resources/remote_references/remote_schema.yaml"));

        WireMock.stubFor(get(urlPathMatching("/remote/schema"))
                .willReturn(aResponse()
                        .withStatus(HttpURLConnection.HTTP_OK)
                        .withHeader("Content-type", "application/yaml")
                        .withBody(pathFile
                                .getBytes(StandardCharsets.UTF_8))));

        pathFile = FileUtils.readFileToString(new File("src/test/resources/remote_references/remote_responses.yaml.template"));
        pathFile = pathFile.replace("${dynamicPort}", String.valueOf(this.serverPort));

        WireMock.stubFor(get(urlPathMatching("/remote/response"))
                .willReturn(aResponse()
                        .withStatus(HttpURLConnection.HTTP_OK)
                        .withHeader("Content-type", "application/yaml")
                        .withBody(pathFile
                                .getBytes(StandardCharsets.UTF_8))));

        pathFile = FileUtils.readFileToString(new File("src/test/resources/remote_references/remote_requestBody.yaml"));

        WireMock.stubFor(get(urlPathMatching("/remote/requestBody"))
                .willReturn(aResponse()
                        .withStatus(HttpURLConnection.HTTP_OK)
                        .withHeader("Content-type", "application/yaml")
                        .withBody(pathFile
                                .getBytes(StandardCharsets.UTF_8))));

        pathFile = FileUtils.readFileToString(new File("src/test/resources/remote_references/remote_parameter.yaml.template"));
        pathFile = pathFile.replace("${dynamicPort}", String.valueOf(this.serverPort));

        WireMock.stubFor(get(urlPathMatching("/remote/parameter"))
                .willReturn(aResponse()
                        .withStatus(HttpURLConnection.HTTP_OK)
                        .withHeader("Content-type", "application/yaml")
                        .withBody(pathFile
                                .getBytes(StandardCharsets.UTF_8))));

        pathFile = FileUtils.readFileToString(new File("src/test/resources/remote_references/remote_example.yaml"));

        WireMock.stubFor(get(urlPathMatching("/remote/example"))
                .willReturn(aResponse()
                        .withStatus(HttpURLConnection.HTTP_OK)
                        .withHeader("Content-type", "application/yaml")
                        .withBody(pathFile
                                .getBytes(StandardCharsets.UTF_8))));

        pathFile = FileUtils.readFileToString(new File("src/test/resources/remote_references/remote_link.yaml"));

        WireMock.stubFor(get(urlPathMatching("/remote/link"))
                .willReturn(aResponse()
                        .withStatus(HttpURLConnection.HTTP_OK)
                        .withHeader("Content-type", "application/yaml")
                        .withBody(pathFile
                                .getBytes(StandardCharsets.UTF_8))));

        pathFile = FileUtils.readFileToString(new File("src/test/resources/remote_references/remote_callback.yaml"));

        WireMock.stubFor(get(urlPathMatching("/remote/callback"))
                .willReturn(aResponse()
                        .withStatus(HttpURLConnection.HTTP_OK)
                        .withHeader("Content-type", "application/yaml")
                        .withBody(pathFile
                                .getBytes(StandardCharsets.UTF_8))));

        pathFile = FileUtils.readFileToString(new File("src/test/resources/remote_references/remote_securityScheme.yaml"));

        WireMock.stubFor(get(urlPathMatching("/remote/security"))
                .willReturn(aResponse()
                        .withStatus(HttpURLConnection.HTTP_OK)
                        .withHeader("Content-type", "application/yaml")
                        .withBody(pathFile
                                .getBytes(StandardCharsets.UTF_8))));

        pathFile = FileUtils.readFileToString(new File("src/test/resources/oas4.yaml"));

        WireMock.stubFor(get(urlPathMatching("/remote/spec"))
                .willReturn(aResponse()
                        .withStatus(HttpURLConnection.HTTP_OK)
                        .withHeader("Content-type", "application/yaml")
                        .withBody(pathFile
                                .getBytes(StandardCharsets.UTF_8))));

        pathFile = FileUtils.readFileToString(new File("src/test/resources/flatten.json"));

        WireMock.stubFor(get(urlPathMatching("/remote/json"))
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
    public void test30() throws Exception{

        String pathFile = FileUtils.readFileToString(new File("src/test/resources/oas3.yaml.template"));
        pathFile = pathFile.replace("${dynamicPort}", String.valueOf(this.serverPort));
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        SwaggerParseResult result = new OpenAPIV3Parser().readContents(pathFile, auths, options);

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getOpenAPI());
        assertEquals(result.getOpenAPI().getOpenapi(), "3.0.1");
        assertEquals(result.getOpenAPI().getComponents().getSchemas().get("OrderRef").getType(),"object");
    }

    @Test
    public void testResolveFully() throws Exception{
        String pathFile = FileUtils.readFileToString(new File("src/test/resources/oas3.yaml.template"));
        pathFile = pathFile.replace("${dynamicPort}", String.valueOf(this.serverPort));
        ParseOptions options = new ParseOptions();
        options.setResolveFully(true);

        SwaggerParseResult result = new OpenAPIV3Parser().readContents(pathFile, new ArrayList<>(), options  );

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getOpenAPI());
        assertEquals(result.getOpenAPI().getOpenapi(), "3.0.1");
        assertEquals(result.getOpenAPI().getComponents().getSchemas().get("OrderRef").getType(),"object");
    }

    @Test
    public void issue1455_testResolveFullyV2_shouldNotThrowNPE() throws Exception{
        String pathFile = FileUtils.readFileToString(new File("src/test/resources/swagger.json"));
        pathFile = pathFile.replace("${dynamicPort}", String.valueOf(this.serverPort));
        ParseOptions options = new ParseOptions();
        options.setResolveFully(true);

        SwaggerParseResult result = new OpenAPIV3Parser().readContents(pathFile, new ArrayList<>(), options  );

        Assert.assertNotNull(result);
        Assert.assertNull(result.getOpenAPI());
        Assert.assertNotNull(result.getMessages());
        Assert.assertEquals(result.getMessages().size(), 1);
        Assert.assertEquals(result.getMessages().get(0), "attribute openapi is missing");
    }

    @Test
    public void testResolveFullyExample() throws Exception{
        String pathFile = FileUtils.readFileToString(new File("src/test/resources/oas3.yaml.template"));
        pathFile = pathFile.replace("${dynamicPort}", String.valueOf(this.serverPort));
        ParseOptions options = new ParseOptions();
        options.setResolveFully(true);

        SwaggerParseResult result = new OpenAPIV3Parser().readContents(pathFile, new ArrayList<>(), options  );

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getOpenAPI());
        Components components = result.getOpenAPI().getComponents();
        ApiResponse response = result.getOpenAPI().getPaths().get("/mockResponses/objectMultipleExamples").getGet().getResponses().get("200");
        assertEquals(response.getContent().get("application/json").getExamples().get("ArthurDent"), components.getExamples().get("Arthur"));
        assertEquals(response.getContent().get("application/xml").getExamples().get("Trillian"), components.getExamples().get("Trillian"));
    }

    @Test
    public void testInlineModelResolver() throws Exception{

        String pathFile = FileUtils.readFileToString(new File("src/test/resources/flatten.json"));
        pathFile = pathFile.replace("${dynamicPort}", String.valueOf(this.serverPort));
        ParseOptions options = new ParseOptions();
        options.setFlatten(true);

        SwaggerParseResult result = new OpenAPIV3Parser().readContents(pathFile, auths, options);

        Assert.assertNotNull(result);
        OpenAPI openAPI = result.getOpenAPI();
        Assert.assertNotNull(openAPI);
        Schema user = openAPI.getComponents().getSchemas().get("User");

        assertNotNull(user);
        Schema address = (Schema)user.getProperties().get("address");

        assertTrue((address.get$ref()!= null));

        Schema userAddress = openAPI.getComponents().getSchemas().get("User_address");
        assertNotNull(userAddress);
        assertNotNull(userAddress.getProperties().get("city"));
        assertNotNull(userAddress.getProperties().get("street"));
    }

    @Test
    public void test30NoOptions() throws Exception{
        String pathFile = FileUtils.readFileToString(new File("src/test/resources/oas3.yaml.template"));
        pathFile = pathFile.replace("${dynamicPort}", String.valueOf(this.serverPort));

        SwaggerParseResult result = new OpenAPIV3Parser().readContents(pathFile, auths,null);

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getOpenAPI());
        assertEquals(result.getOpenAPI().getOpenapi(), "3.0.1");
        assertEquals(result.getOpenAPI().getComponents().getSchemas().get("OrderRef").getType(),"object");
    }

    @Test
    public void testShellMethod(){
        String url = "http://localhost:${dynamicPort}/remote/spec";
        url = url.replace("${dynamicPort}", String.valueOf(this.serverPort));

        OpenAPI openAPI = new OpenAPIV3Parser().read(url);
        Assert.assertNotNull(openAPI);
        assertEquals(openAPI.getOpenapi(), "3.0.1");
    }

    @Test
    public void testInlineModelResolverByUrl(){
        String url = "http://localhost:${dynamicPort}/remote/json";
        url = url.replace("${dynamicPort}", String.valueOf(this.serverPort));

        ParseOptions options = new ParseOptions();
        options.setFlatten(true);

        SwaggerParseResult result = new OpenAPIV3Parser().readLocation(url,new ArrayList<>(),options);
        Assert.assertNotNull(result);
        OpenAPI openAPI = result.getOpenAPI();
        Assert.assertNotNull(openAPI);
        Schema user = openAPI.getComponents().getSchemas().get("User");

        assertNotNull(user);
        Schema address = (Schema)user.getProperties().get("address");

        assertTrue((address.get$ref()!= null));

        Schema userAddress = openAPI.getComponents().getSchemas().get("User_address");
        assertNotNull(userAddress);
        assertNotNull(userAddress.getProperties().get("city"));
        assertNotNull(userAddress.getProperties().get("street"));
    }

    @Test
    public void testIssueSameRefsDifferentModel() throws IOException {
        String pathFile = FileUtils.readFileToString(new File("src/test/resources/same-refs-different-model-domain.yaml"), "UTF-8");
        WireMock.stubFor(get(urlPathMatching("/issue-domain"))
                .willReturn(aResponse()
                        .withStatus(HttpURLConnection.HTTP_OK)
                        .withHeader("Content-type", "application/json")
                        .withBody(pathFile
                                .getBytes(StandardCharsets.UTF_8))));

        pathFile = FileUtils.readFileToString(new File("src/test/resources/same-refs-different-model.yaml"), "UTF-8");
        pathFile = pathFile.replace("${dynamicPort}", String.valueOf(this.serverPort));

        OpenAPIV3Parser parser = new OpenAPIV3Parser();
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setResolveFully(true);

        final SwaggerParseResult openAPI = parser.readContents(pathFile, null, options);
        Yaml.prettyPrint(openAPI);

        assertEquals(openAPI.getMessages().size(), 0);
    }

    @Test
    public void testIssue251() throws IOException {
        String pathFile = FileUtils.readFileToString(new File("src/test/resources/domain.yaml"), "UTF-8");
        WireMock.stubFor(get(urlPathMatching("/domain"))
                .willReturn(aResponse()
                        .withStatus(HttpURLConnection.HTTP_OK)
                        .withHeader("Content-type", "application/json")
                        .withBody(pathFile
                                .getBytes(StandardCharsets.UTF_8))));

        pathFile = FileUtils.readFileToString(new File("src/test/resources/issue251.yaml"), "UTF-8");
        pathFile = pathFile.replace("${dynamicPort}", String.valueOf(this.serverPort));

        OpenAPIV3Parser parser = new OpenAPIV3Parser();
        ParseOptions options = new ParseOptions();
        options.setResolve(true);

        final SwaggerParseResult parseResult = parser.readContents(pathFile, null, options);

        assertEquals(parseResult.getMessages().size(), 0);
        assertTrue(parseResult.getOpenAPI().getComponents().getSchemas().size() == 2);
        assertTrue(parseResult.getOpenAPI().getPaths().get("/parse").getGet().getParameters().get(0).getSchema().get$ref().equals("#/components/schemas/Parse"));
    }

    private static int getDynamicPort() {
        return new Random().ints(10000, 20000).findFirst().getAsInt();
    }
}
