package io.swagger.v3.parser.test;


import com.fasterxml.jackson.databind.JsonNode;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ByteArraySchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.PathParameter;
import io.swagger.v3.oas.models.parameters.QueryParameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.AuthorizationValue;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import mockit.Injectable;
import org.apache.commons.io.FileUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.reporters.Files;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.testng.Assert.*;


public class OpenAPIV3ParserTest {
    protected int serverPort = getDynamicPort();
    protected WireMockServer wireMockServer;

    @Test
    public void issue682() throws Exception {
        OpenAPIV3Parser parser = new OpenAPIV3Parser();
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setResolveCombinators(false);
        options.setResolveFully(true);

        String location = getClass().getResource("/odin.yaml").toString();
        Assert.assertNotNull(location);
        final SwaggerParseResult result = parser.readLocation(location, null, options);
        Assert.assertNotNull(result.getOpenAPI());
        Assert.assertTrue(result.getMessages().isEmpty());
        Assert.assertTrue(result.getOpenAPI().getPaths().get("/JTasker/startRun").getPost().getRequestBody().getContent().get("application/json").getSchema().getProperties().size() == 2);
    }

    @Test
    public void testPattern() {
        final OpenAPI openAPI = new OpenAPIV3Parser().readLocation("testPattern.yaml", null, new ParseOptions()).getOpenAPI();

        Schema s = openAPI.getComponents().getSchemas().get("SomeObj");
        Assert.assertEquals(s.getPattern(),"^[A-Z]+$"); //ERROR: got null
    }


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
    public void test30(@Injectable final List<AuthorizationValue> auths) throws Exception{



       String pathFile = FileUtils.readFileToString(new File("src/test/resources/oas3.yaml.template"));
        pathFile = pathFile.replace("${dynamicPort}", String.valueOf(this.serverPort));
        ParseOptions options = new ParseOptions();
        options.setResolve(true);

        SwaggerParseResult result = new OpenAPIV3Parser().readContents(pathFile, auths, options  );

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
    public void testResolveEmpty(@Injectable final List<AuthorizationValue> auths) throws Exception{
        String pathFile = FileUtils.readFileToString(new File("src/test/resources/empty-oas.yaml"));
        ParseOptions options = new ParseOptions();
        options.setResolveFully(true);

        SwaggerParseResult result = new OpenAPIV3Parser().readContents(pathFile, auths, options  );

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getOpenAPI());
    }

    @Test
    public void testResolveFullyExample() throws Exception{


        String pathFile = FileUtils.readFileToString(new File("src/test/resources/oas3.yaml.template"));
        pathFile = pathFile.replace("${dynamicPort}", String.valueOf(this.serverPort));
        ParseOptions options = new ParseOptions();
        //options.setResolve(true);
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
    public void testInlineModelResolver(@Injectable final List<AuthorizationValue> auths) throws Exception{


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
    public void test30NoOptions(@Injectable final List<AuthorizationValue> auths) throws Exception{



        String pathFile = FileUtils.readFileToString(new File("src/test/resources/oas3.yaml.template"));
        pathFile = pathFile.replace("${dynamicPort}", String.valueOf(this.serverPort));

        SwaggerParseResult result = new OpenAPIV3Parser().readContents(pathFile, auths,null);

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getOpenAPI());
        assertEquals(result.getOpenAPI().getOpenapi(), "3.0.1");
        assertEquals(result.getOpenAPI().getComponents().getSchemas().get("OrderRef").getType(),"object");
    }

    @Test
    public void testShellMethod(@Injectable final List<AuthorizationValue> auths){

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
    public void testRefAdditionalProperties() throws Exception {
        OpenAPI openAPI = new OpenAPIV3Parser().read("src/test/resources/relative/additionalProperties.yaml");

        Assert.assertNotNull(openAPI);
        Assert.assertTrue(openAPI.getComponents().getSchemas().size() == 3);
        Assert.assertNotNull(openAPI.getComponents().getSchemas().get("link-object"));
        Assert.assertNotNull(openAPI.getComponents().getSchemas().get("rel-data"));
        Assert.assertNotNull(openAPI.getComponents().getSchemas().get("result"));
    }

    @Test
    public void testRefAndInlineAllOf(@Injectable final List<AuthorizationValue> auths) throws Exception {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setResolveFully(true);
        OpenAPI openAPI = new OpenAPIV3Parser().read("src/test/resources/allOfAndRef.yaml",auths,options);

        Assert.assertNotNull(openAPI);
        Assert.assertTrue(openAPI.getComponents().getSchemas().size() == 2);
        Assert.assertNotNull(openAPI.getComponents().getSchemas().get("UserEx"));
        Assert.assertNotNull(openAPI.getComponents().getSchemas().get("User"));
        Assert.assertTrue(openAPI.getPaths().get("/refToAllOf").getGet().getResponses().get("200").getContent().get("application/json").getSchema().getProperties().size() == 2);
    }

    @Test
    public void testComposedRefResolvingIssue628(@Injectable final List<AuthorizationValue> auths) throws Exception {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        OpenAPI openAPI = new OpenAPIV3Parser().read("src/test/resources/composedSchemaRef.yaml", auths, options);

        Assert.assertNotNull(openAPI);

        Assert.assertTrue(openAPI.getComponents().getSchemas().size() == 5);
        Assert.assertNotNull(openAPI.getComponents().getSchemas().get("Cat"));
        Assert.assertNotNull(openAPI.getComponents().getSchemas().get("Dog"));
        Assert.assertNotNull(openAPI.getComponents().getSchemas().get("Pet"));
        Assert.assertNotNull(openAPI.getComponents().getSchemas().get("Lion"));
        Assert.assertNotNull(openAPI.getComponents().getSchemas().get("Bear"));


    }

    @Test
    public void testComposedSchemaAdjacent(@Injectable final List<AuthorizationValue> auths) throws Exception {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        OpenAPI openAPI = new OpenAPIV3Parser().read("src/test/resources/composedSchemaRef.yaml", auths, options);

        Assert.assertNotNull(openAPI);

        Assert.assertTrue(openAPI.getComponents().getSchemas().size() == 5);
        Schema schema = openAPI.getPaths().get("/path").getGet().getResponses().get("200").getContent().get("application/json").getSchema();
        Assert.assertTrue(schema instanceof ComposedSchema);
        ComposedSchema composedSchema = (ComposedSchema) schema;
        Assert.assertTrue(composedSchema.getOneOf().size() == 2);
        Assert.assertTrue(composedSchema.getAllOf().size() == 1);
    }

    @Test
    public void testOneOfExternalRefConflictName() throws Exception {
        OpenAPI openAPI = new OpenAPIV3Parser().read("./oneof_name_conflict/oneOf-external-ref-name-conflict.yaml");
        Assert.assertNotNull(openAPI);
        Schema pet = openAPI.getComponents().getSchemas().get("Pet");
        Assert.assertNotNull(pet);
        Assert.assertTrue(pet.getDiscriminator().getMapping().containsKey("Cat"));
        Assert.assertTrue(pet.getDiscriminator().getMapping().get("Cat").equals("#/components/schemas/Cat_2"));
    }

    @Test
    public void int64ExampleWithoutOverflow() throws Exception {
        OpenAPI openAPI = new OpenAPIV3Parser().read("src/test/resources/int64example.yaml");
        IntegerSchema date = ((IntegerSchema) openAPI.getPaths().get("/foo").getGet().getResponses().get("200").getContent().get("application/json").getSchema().getProperties().get("date"));
        Assert.assertEquals("1516042231144", date.getExample().toString());
    }


    @Test
    public void testRefPaths() throws Exception {
        String yaml = "openapi: '3.0.0'\n" +
                "info:\n" +
                "  version: 0.0.0\n" +
                "  title: Path item $ref test\n" +
                "paths:\n" +
                "  /foo:\n" +
                "    get:\n" +
                "      responses:\n" +
                "        200:\n" +
                "          description: OK\n" +
                "  /foo2:\n" +
                "    $ref: '#/paths/~1foo'";

        OpenAPIV3Parser parser = new OpenAPIV3Parser();

        OpenAPI openAPI = (parser.readContents(yaml,null,null)).getOpenAPI();

        assertEquals(openAPI.getPaths().get("foo"),openAPI.getPaths().get("foo2"));


    }
    @Test
    public void testModelParameters() throws Exception {
        String yaml = "openapi: '2.0'\n" +
                "info:\n" +
                "  version: \"0.0.0\"\n" +
                "  title: test\n" +
                "paths:\n" +
                "  /foo:\n" +
                "    get:\n" +
                "      parameters:\n" +
                "      - name: amount\n" +
                "        in: body\n" +
                "        schema:\n" +
                "          type: integer\n" +
                "          format: int64\n" +
                "          description: amount of money\n" +
                "          default: 1000\n" +
                "          maximum: 100000\n" +
                "      responses:\n" +
                "        200:\n" +
                "          description: ok";

        OpenAPIV3Parser parser = new OpenAPIV3Parser();

        OpenAPI openAPI = (parser.readContents(yaml,null,null)).getOpenAPI();

    }

    @Test
    public void testParseSharedPathParameters() throws Exception {
        String yaml =
                "openapi: '3.0.0'\n" +
                        "info:\n" +
                        "  version: \"0.0.0\"\n" +
                        "  title: test\n" +
                        "paths:\n" +
                        "  /persons/{id}:\n" +
                        "    parameters:\n" +
                        "      - in: path\n" +
                        "        name: id\n" +
                        "        type: string\n" +
                        "        required: true\n" +
                        "        description: \"no\"\n" +
                        "    get:\n" +
                        "      parameters:\n" +
                        "        - name: id\n" +
                        "          in: path\n" +
                        "          required: true\n" +
                        "          type: string\n" +
                        "          description: \"yes\"\n" +
                        "        - name: name\n" +
                        "          in: query\n" +
                        "          type: string\n" +
                        "      responses:\n" +
                        "        200:\n" +
                        "          description: ok\n";

        OpenAPIV3Parser parser = new OpenAPIV3Parser();

        OpenAPI openAPI = (parser.readContents(yaml, null, null)).getOpenAPI();
        List<Parameter> parameters = openAPI.getPaths().get("/persons/{id}").getGet().getParameters();
        assertTrue(parameters.size() == 2);
        Parameter id = parameters.get(0);
        assertEquals(id.getDescription(), "yes");
    }

    @Test
    public void testParseRefPathParameters() throws Exception {
        String yaml =
                "openAPI: '2.0'\n" +
                        "info:\n" +
                        "  title: test\n" +
                        "  version: '0.0.0'\n" +
                        "parameters:\n" +
                        "  report-id:\n" +
                        "    name: id\n" +
                        "    in: path\n" +
                        "    type: string\n" +
                        "    required: true\n" +
                        "paths:\n" +
                        "  /reports/{id}:\n" +
                        "    parameters:\n" +
                        "        - $ref: '#/parameters/report-id'\n" +
                        "    put:\n" +
                        "      parameters:\n" +
                        "        - name: id\n" +
                        "          in: body\n" +
                        "          required: true\n" +
                        "          schema:\n" +
                        "            $ref: '#/components/schemas/report'\n" +
                        "      responses:\n" +
                        "        200:\n" +
                        "          description: ok\n" +
                        "definitions:\n" +
                        "  report:\n" +
                        "    type: object\n" +
                        "    properties:\n" +
                        "      id:\n" +
                        "        type: string\n" +
                        "      name:\n" +
                        "        type: string\n" +
                        "    required:\n" +
                        "    - id\n" +
                        "    - name\n";
        OpenAPIV3Parser parser = new OpenAPIV3Parser();
        OpenAPI openAPI = (parser.readContents(yaml,null,null)).getOpenAPI();
    }

    @Test
    public void testUniqueParameters() throws Exception {
        String yaml =
                "openapi: 3.0.0\n" +
                        "servers: []\n" +
                        "info:\n" +
                        "  title: test\n" +
                        "  version: 0.0.0\n" +
                        "paths:\n" +
                        "  '/foos/{id}':\n" +
                        "    parameters:\n" +
                        "      - $ref: '#/components/parameters/foo-id'\n" +
                        "    get:\n" +
                        "      responses:\n" +
                        "        '200':\n" +
                        "          description: OK\n" +
                        "          content:\n" +
                        "            '*/*':\n" +
                        "              schema:\n" +
                        "                $ref: '#/components/schemas/foo'\n" +
                        "    put:\n" +
                        "      responses:\n" +
                        "        '200':\n" +
                        "          description: OK\n" +
                        "          content:\n" +
                        "            '*/*':\n" +
                        "              schema:\n" +
                        "                $ref: '#/components/schemas/foo'\n" +
                        "      requestBody:\n" +
                        "        content:\n" +
                        "          application/json:\n" +
                        "            schema:\n" +
                        "              $ref: '#/components/schemas/foo'\n" +
                        "        required: true\n" +
                        "components:\n" +
                        "  parameters:\n" +
                        "    foo-id:\n" +
                        "      name: id\n" +
                        "      in: path\n" +
                        "      required: true\n" +
                        "      schema:\n" +
                        "        type: string\n" +
                        "  schemas:\n" +
                        "    foo:\n" +
                        "      type: object\n" +
                        "      properties:\n" +
                        "        id:\n" +
                        "          type: string\n" +
                        "      required:\n" +
                        "        - id";

        OpenAPIV3Parser parser = new OpenAPIV3Parser();
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        OpenAPI openAPI = (parser.readContents(yaml,null,options)).getOpenAPI();
        List<Parameter> parameters = openAPI.getPaths().get("/foos/{id}").getPut().getParameters();
        assertNotNull(openAPI.getPaths().get("/foos/{id}").getPut().getRequestBody());
        assertTrue(parameters.size() == 1);
    }

    @Test
    public void testLoadRelativeFileTree_Json() throws Exception {
        final OpenAPI openAPI = doRelativeFileTest("src/test/resources/relative-file-references/json/parent.json");
        //Json.mapper().writerWithDefaultPrettyPrinter().writeValue(new File("resolved.json"), openAPI);
    }

    @Test
    public void testLoadExternalNestedDefinitions() throws Exception {
        OpenAPIV3Parser parser = new OpenAPIV3Parser();
        final OpenAPI openAPI = parser.read("src/test/resources/nested-references/b.yaml");

        Map<String, Schema> definitions = openAPI.getComponents().getSchemas();
        assertTrue(definitions.containsKey("x"));
        assertTrue(definitions.containsKey("y"));
        assertTrue(definitions.containsKey("z"));
        assertEquals( definitions.get("i").get$ref(),"#/components/schemas/k_2");
        assertEquals( definitions.get("k").getTitle(), "k-definition");
        assertEquals( definitions.get("k_2").getTitle(), "k-definition");
    }

    @Test
    public void testPetstore() throws Exception {
        OpenAPIV3Parser parser = new OpenAPIV3Parser();
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        SwaggerParseResult result = parser.readLocation("src/test/resources/petstore.yaml", null, options);

        assertNotNull(result);
        assertTrue(result.getMessages().isEmpty());

        OpenAPI openAPI = result.getOpenAPI();
        Map<String, Schema> definitions = openAPI.getComponents().getSchemas();
        Set<String> expectedDefinitions = new HashSet<String>();
        expectedDefinitions.add("User");
        expectedDefinitions.add("Category");
        expectedDefinitions.add("Pet");
        expectedDefinitions.add("Tag");
        expectedDefinitions.add("Order");
        expectedDefinitions.add("PetArray");
        assertEquals(definitions.keySet(), expectedDefinitions);

        Schema petModel = definitions.get("Pet");
        Set<String> expectedPetProps = new HashSet<String>();
        expectedPetProps.add("id");
        expectedPetProps.add("category");
        expectedPetProps.add("name");
        expectedPetProps.add("photoUrls");
        expectedPetProps.add("tags");
        expectedPetProps.add("status");
        assertEquals(petModel.getProperties().keySet(), expectedPetProps);

        ArraySchema petArrayModel = (ArraySchema) definitions.get("PetArray");
        assertEquals(petArrayModel.getType(), "array");
        Schema refProp = petArrayModel.getItems();
        assertEquals(refProp.get$ref(), "#/components/schemas/Pet");
        assertNull(petArrayModel.getProperties());
    }

    @Test
    public void testFileReferenceWithVendorExt() throws Exception {
        OpenAPIV3Parser parser = new OpenAPIV3Parser();
        final OpenAPI openAPI = parser.read("src/test/resources/file-reference-with-vendor-ext/b.yaml");
        Map<String, Schema> definitions = openAPI.getComponents().getSchemas();
        assertTrue(definitions.get("z").getExtensions().get("x-foo") instanceof Map);
        assertEquals(((Map) definitions.get("z").getExtensions().get("x-foo")).get("bar"), "baz");
        assertTrue(definitions.get("x").getExtensions().get("x-foo") instanceof Map);
        assertEquals(((Map) definitions.get("x").getExtensions().get("x-foo")).get("bar"), "baz");
    }

    @Test
    public void testTroublesomeFile() throws Exception {
        OpenAPIV3Parser parser = new OpenAPIV3Parser();
        final OpenAPI openAPI = parser.read("src/test/resources/troublesome.yaml");
    }

    @Test
    public void testLoadRelativeFileTree_Yaml() throws Exception {
        JsonToYamlFileDuplicator.duplicateFilesInYamlFormat("src/test/resources/relative-file-references/json",
                "src/test/resources/relative-file-references/yaml");
        final OpenAPI openAPI = doRelativeFileTest("src/test/resources/relative-file-references/yaml/parent.yaml");

        assertNotNull(Yaml.mapper().writeValueAsString(openAPI));
    }

    @Test
    public void testLoadRecursiveExternalDef() throws Exception {
        OpenAPIV3Parser parser = new OpenAPIV3Parser();
        final OpenAPI openAPI = parser.read("src/test/resources/file-reference-to-recursive-defs/b.yaml");

        Map<String, Schema> definitions = openAPI.getComponents().getSchemas();
        assertEquals((((ArraySchema) definitions.get("v").getProperties().get("children")).getItems()).get$ref(), "#/components/schemas/v");
        assertTrue(definitions.containsKey("y"));
        assertEquals((((ArraySchema) definitions.get("x").getProperties().get("children")).getItems()).get$ref(), "#/components/schemas/y");
    }

    @Test
    public void testLoadNestedItemsReferences() {
        OpenAPIV3Parser parser = new OpenAPIV3Parser();
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        SwaggerParseResult result = parser.readLocation("src/test/resources/nested-items-references/b.yaml", null, options);
        OpenAPI openAPI = result.getOpenAPI();
        Map<String, Schema> definitions = openAPI.getComponents().getSchemas();
        assertTrue(definitions.containsKey("z"));
        assertTrue(definitions.containsKey("w"));
    }

    @Test
    public void testIssue75() {
        OpenAPIV3Parser parser = new OpenAPIV3Parser();
        final OpenAPI openAPI = parser.read("src/test/resources/issue99.yaml");
        RequestBody body =  openAPI.getPaths().get("/albums").getPost().getRequestBody();
        Schema model = body.getContent().get("application/json").getSchema();

        assertNotNull(model);
        assertTrue(model instanceof ArraySchema);

        ArraySchema am = (ArraySchema) model;
        assertTrue(am.getItems() instanceof ByteArraySchema);
        assertEquals(am.getItems().getFormat(), "byte");
    }

    @Test(enabled = false, description = "see https://github.com/openAPI-api/openAPI-parser/issues/337")
    public void testIssue62() {
        OpenAPIV3Parser parser = new OpenAPIV3Parser();
        final OpenAPI openAPI = parser.read("https://raw.githubusercontent.com/OAI/OpenAPI-Specification/master/fixtures/v2.0/json/resources/resourceWithLinkedDefinitions.json");

        assertNotNull(openAPI.getPaths().get("/pets/{petId}").getGet());
    }

    @Test
    public void testIssue146() {
        OpenAPIV3Parser parser = new OpenAPIV3Parser();
        final OpenAPI openAPI = parser.read("src/test/resources/issue_146.yaml");
        assertNotNull(openAPI);
        QueryParameter p = ((QueryParameter) openAPI.getPaths().get("/checker").getGet().getParameters().get(0));
        StringSchema pp = (StringSchema) ((ArraySchema)p.getSchema()).getItems();
        assertTrue("registration".equalsIgnoreCase(pp.getEnum().get(0)));
    }

    @Test(description = "Test (path & form) parameter's required attribute")
    public void testParameterRequired() {
        OpenAPIV3Parser parser = new OpenAPIV3Parser();
        final OpenAPI openAPI = parser.read("src/test/resources/petstore.yaml");
        final List<Parameter> operationParams = openAPI.getPaths().get("/pet/{petId}").getPost().getParameters();

        final PathParameter pathParameter = (PathParameter) operationParams.get(0);
        Assert.assertTrue(pathParameter.getRequired());
    }

    @Test
    public void testIssue108() {
        OpenAPIV3Parser parser = new OpenAPIV3Parser();
        final OpenAPI openAPI = parser.read("src/test/resources/issue_108.yaml");

        assertNotNull(openAPI);
    }

    @Test
    public void testIssue() {
        String yaml =
                "openapi: 3.0.0\n" +
                        "servers: []\n" +
                        "info:\n" +
                        "  description: No description provided.\n" +
                        "  version: '2.0'\n" +
                        "  title: My web service\n" +
                        "  x-endpoint-name: default\n" +
                        "paths:\n" +
                        "  x-nothing: sorry not supported\n" +
                        "  /foo:\n" +
                        "    x-something: 'yes, it is supported'\n" +
                        "    get:\n" +
                        "      responses:\n" +
                        "        '200':\n" +
                        "          description: OpenAPI API document for this service\n" +
                        "x-some-vendor:\n" +
                        "  sometesting: bye!";
        OpenAPIV3Parser parser = new OpenAPIV3Parser();
        SwaggerParseResult result = parser.readContents(yaml,null,null);

        OpenAPI openAPI = result.getOpenAPI();

        assertEquals(((Map) openAPI.getExtensions().get("x-some-vendor")).get("sometesting"), "bye!");
        assertEquals(openAPI.getPaths().get("/foo").getExtensions().get("x-something"), "yes, it is supported");
        assertTrue(result.getMessages().size() == 1);
        assertEquals(result.getMessages().get(0), "attribute paths.x-nothing is unsupported");
    }


    @Test
    public void testIssue292WithCSVCollectionFormat() {
        String yaml =
                "openapi: '3.0.0'\n" +
                        "info:\n" +
                        "  version: '0.0.0'\n" +
                        "  title: nada\n" +
                        "paths:\n" +
                        "  /persons:\n" +
                        "    get:\n" +
                        "      parameters:\n" +
                        "      - name: testParam\n" +
                        "        in: query\n" +
                        "        type: array\n" +
                        "        items:\n" +
                        "          type: string\n" +
                        "        explode: false\n" +
                        "      responses:\n" +
                        "        200:\n" +
                        "          description: Successful response";
        OpenAPIV3Parser parser = new OpenAPIV3Parser();
        SwaggerParseResult result = parser.readContents(yaml,null,null);

        OpenAPI openAPI = result.getOpenAPI();

        Parameter param = openAPI.getPaths().get("/persons").getGet().getParameters().get(0);
        QueryParameter qp = (QueryParameter) param;
        assertTrue(qp.getStyle().toString().equals("form"));
        Assert.assertFalse(qp.getExplode());
    }

    @Test
    public void testIssue255() {
        OpenAPIV3Parser parser = new OpenAPIV3Parser();

        OpenAPI openAPI = parser.read("objectExample.yaml");
        assertEquals(openAPI.getComponents().getSchemas().get("SamplePayload").getExample().toString(), "[{\"op\":\"replace\",\"path\":\"/s\",\"v\":\"w\"}]");
    }

    @Test
    public void testIssue286() {
        OpenAPIV3Parser parser = new OpenAPIV3Parser();

        OpenAPI openAPI = parser.read("issue_286.yaml");
        Schema response = openAPI.getPaths().get("/").getGet().getResponses().get("200").getContent().get("*/*").getSchema();
        assertTrue(response.get$ref() != null);
        assertEquals(response.get$ref(), "#/components/schemas/issue_286_PetList");
        assertNotNull(openAPI.getComponents().getSchemas().get("issue_286_Allergy"));
    }

    @Test
    public void testIssue360() {
        OpenAPIV3Parser parser = new OpenAPIV3Parser();
        final OpenAPI openAPI = parser.read("src/test/resources/issue_360.yaml");
        assertNotNull(openAPI);

        RequestBody body = openAPI.getPaths().get("/pets").getPost().getRequestBody();
        assertNotNull(body);

        assertNotNull(body.getContent().get("application/json").getSchema());
        Schema model = body.getContent().get("application/json").getSchema();

        assertNotNull(model.getProperties().get("foo"));

        Map<String, Object> extensions = body.getExtensions();
        assertNotNull(extensions);

        assertNotNull(extensions.get("x-examples"));
        Object o = extensions.get("x-examples");
        assertTrue(o instanceof Map);

        Map<String, Object> on = (Map<String, Object>) o;

        Object jn = on.get("application/json");
        assertTrue(jn instanceof Map);

        Map<String, Object> objectNode = (Map<String, Object>) jn;
        assertEquals(objectNode.get("foo"), "bar");

        RequestBody stringBodyParameter = openAPI.getPaths().get("/otherPets").getPost().getRequestBody();

        assertTrue(stringBodyParameter.getRequired());


        Schema sbpModel = stringBodyParameter.getContent().get("application/json").getSchema();

        assertEquals(sbpModel.getType(), "string");
        assertEquals(sbpModel.getFormat(), "uuid");

        RequestBody bodyParameter = openAPI.getPaths().get("/evenMorePets").getPost().getRequestBody();

        assertTrue(bodyParameter.getRequired());

        Schema refModel = bodyParameter.getContent().get("application/json").getSchema();
        assertTrue(refModel.get$ref() != null);


        assertEquals(refModel.get$ref(), "#/components/schemas/Pet");
    }

    private OpenAPI doRelativeFileTest(String location) {
        OpenAPIV3Parser parser = new OpenAPIV3Parser();
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        SwaggerParseResult readResult = parser.readLocation(location, null, options);

        if (readResult.getMessages().size() > 0) {
            Json.prettyPrint(readResult.getMessages());
        }
        final OpenAPI openAPI = readResult.getOpenAPI();


        final PathItem path = openAPI.getPaths().get("/health");

        assertEquals(path.getClass(), PathItem.class); //we successfully converted the RefPath to a Path

        final List<Parameter> parameters = path.getParameters();
        assertParamDetails(parameters, 0, QueryParameter.class, "param1", "query");
        assertParamDetails(parameters, 1, HeaderParameter.class, "param2", "header");

        final Operation operation = path.getGet();
        final List<Parameter> operationParams = operation.getParameters();
        assertParamDetails(operationParams, 0, PathParameter.class, "param3", "path");
        assertParamDetails(operationParams, 1, HeaderParameter.class, "param4", "header");


        final Map<String, ApiResponse> responsesMap = operation.getResponses();

        assertResponse(openAPI, responsesMap, "200","application/json", "Health information from the server", "#/components/schemas/health");
        assertResponse(openAPI, responsesMap, "400","*/*", "Your request was not valid", "#/components/schemas/error");
        assertResponse(openAPI, responsesMap, "500","*/*", "An unexpected error occur during processing", "#/components/schemas/error");

        final Map<String, Schema> definitions = openAPI.getComponents().getSchemas();
        final Schema refInDefinitions = definitions.get("refInDefinitions");
        assertEquals(refInDefinitions.getDescription(), "The example model");
        expectedPropertiesInModel(refInDefinitions, "foo", "bar");

        final ArraySchema arrayModel = (ArraySchema) definitions.get("arrayModel");
        final Schema arrayModelItems = arrayModel.getItems();
        assertEquals(arrayModelItems.get$ref(), "#/components/schemas/foo");

        final Schema fooModel = definitions.get("foo");
        assertEquals(fooModel.getDescription(), "Just another model");
        expectedPropertiesInModel(fooModel, "hello", "world");

        final ComposedSchema composedCat = (ComposedSchema) definitions.get("composedCat");
        final Schema child =  composedCat.getAllOf().get(2);
        expectedPropertiesInModel(child, "huntingSkill", "prop2", "reflexes", "reflexMap");
        final ArraySchema reflexes = (ArraySchema) child.getProperties().get("reflexes");
        final Schema reflexItems = reflexes.getItems();
        assertEquals(reflexItems.get$ref(), "#/components/schemas/reflex");
        assertTrue(definitions.containsKey(reflexItems.get$ref().substring(reflexItems.get$ref().lastIndexOf("/")+1)));

        final Schema reflexMap = (Schema) child.getProperties().get("reflexMap");
        final Schema reflexMapAdditionalProperties = (Schema) reflexMap.getAdditionalProperties();
        assertEquals(reflexMapAdditionalProperties.get$ref(), "#/components/schemas/reflex");

        assertEquals(composedCat.getAllOf().size(), 3);
        assertEquals(composedCat.getAllOf().get(0).get$ref(), "#/components/schemas/pet");
        assertEquals(composedCat.getAllOf().get(1).get$ref(), "#/components/schemas/foo_2");

        return openAPI;
    }

    private void expectedPropertiesInModel(Schema model, String... expectedProperties) {
        assertEquals(model.getProperties().size(), expectedProperties.length);
        for (String expectedProperty : expectedProperties) {
            assertTrue(model.getProperties().containsKey(expectedProperty));
        }
    }

    private void assertResponse(OpenAPI openAPI, Map<String, ApiResponse> responsesMap, String responseCode,String mediaType,
                                String expectedDescription, String expectedSchemaRef) {
        final ApiResponse response = responsesMap.get(responseCode);
        final Schema schema =  response.getContent().get(mediaType).getSchema();
        assertEquals(response.getDescription(), expectedDescription);
        assertEquals(schema.getClass(), Schema.class);
        assertEquals(schema.get$ref(), expectedSchemaRef);
        assertTrue(openAPI.getComponents().getSchemas().containsKey(schema.get$ref().substring(schema.get$ref().lastIndexOf("/")+1)));
    }

    private void assertParamDetails(List<Parameter> parameters, int index, Class<?> expectedType,
                                    String expectedName, String expectedIn) {
        final Parameter param1 = parameters.get(index);
        assertEquals(param1.getClass(), expectedType);
        assertEquals(param1.getName(), expectedName);
        assertEquals(param1.getIn(), expectedIn);
    }

    @Test
    public void testNestedReferences() {
        OpenAPIV3Parser parser = new OpenAPIV3Parser();
        final OpenAPI openAPI = parser.read("src/test/resources/relative-file-references/json/parent.json");
        assertTrue(openAPI.getComponents().getSchemas().containsKey("externalArray"));
        assertTrue(openAPI.getComponents().getSchemas().containsKey("referencedByLocalArray"));
        assertTrue(openAPI.getComponents().getSchemas().containsKey("externalObject"));
        assertTrue(openAPI.getComponents().getSchemas().containsKey("referencedByLocalElement"));
        assertTrue(openAPI.getComponents().getSchemas().containsKey("referencedBy"));
    }

    @Test
    public void testCodegenPetstore() {
        OpenAPIV3Parser parser = new OpenAPIV3Parser();
        final OpenAPI openAPI = parser.read("src/test/resources/petstore-codegen.yaml");
        Schema enumModel =  openAPI.getComponents().getSchemas().get("Enum_Test");
        assertNotNull(enumModel);
        Schema enumProperty = (Schema)enumModel.getProperties().get("enum_integer");
        assertNotNull(enumProperty);

        assertTrue(enumProperty instanceof IntegerSchema);
        IntegerSchema enumIntegerProperty = (IntegerSchema) enumProperty;
        List<Number> integers =  enumIntegerProperty.getEnum();
        assertEquals(integers.get(0), new Integer(1));
        assertEquals(integers.get(1), new Integer(-1));

        Operation getOrderOperation = openAPI.getPaths().get("/store/order/{orderId}").getGet();
        assertNotNull(getOrderOperation);
        Parameter orderId = getOrderOperation.getParameters().get(0);
        assertTrue(orderId instanceof PathParameter);
        PathParameter orderIdPathParam = (PathParameter) orderId;
        assertNotNull(orderIdPathParam.getSchema().getMinimum());

        BigDecimal minimum = orderIdPathParam.getSchema().getMinimum();
        assertEquals(minimum.toString(), "1");
    }

    @Test
    public void testIssue339() throws Exception {
        OpenAPIV3Parser parser = new OpenAPIV3Parser();
        final OpenAPI openAPI = parser.read("src/test/resources/issue-339.yaml");

        Parameter param = openAPI.getPaths().get("/store/order/{orderId}").getGet().getParameters().get(0);
        assertTrue(param instanceof PathParameter);
        PathParameter pp = (PathParameter) param;

        assertTrue(pp.getSchema().getMinimum().toString().equals("1"));
        assertTrue(pp.getSchema().getMaximum().toString().equals("5"));
    }

    @Test
    public void testCodegenIssue4555() throws Exception {
        OpenAPIV3Parser parser = new OpenAPIV3Parser();
        String yaml = "openapi: 3.0.0\n" +
                "info:\n" +
                "  title: test\n" +
                "  version: \"0.0.1\"\n" +
                "\n" +
                "paths:\n" +
                "  '/contents/{id}':\n" +
                "    parameters:\n" +
                "      - name: id\n" +
                "        in: path\n" +
                "        description: test\n" +
                "        required: true\n" +
                "        schema:\n" +
                "          type: integer\n" +
                "  get:\n" +
                "    description: test\n" +
                "    responses:\n" +
                "      '200':\n" +
                "        description: OK\n" +
                "        schema: null\n" +
                "        $ref: '#/components/schemas/Content'\n" +
                "components:\n" +
                "  schemas:\n" +
                "    Content:\n" +
                "      type: object\n" +
                "      title: \t\ttest";

        final SwaggerParseResult result = parser.readContents(yaml,null,null);

        // can't parse with tabs!
        assertNull(result.getOpenAPI());
    }

    @Test
    public void testConverterIssue17() throws Exception {
        String yaml = "openapi: 3.0.0\n" +
                "info:\n" +
                "  version: 0.0.0\n" +
                "  title: nada\n" +
                "paths:\n" +
                "  /persons:\n" +
                "    get:\n" +
                "      parameters:\n" +
                "        - name: testParam\n" +
                "          in: query\n" +
                "          style: form\n" +
                "          schema:\n" +
                "            type: array\n" +
                "            items:\n" +
                "              type: string\n" +
                "      responses:\n" +
                "        '200':\n" +
                "          description: Successful response\n" +
                "          content:\n" +
                "            '*/*':\n" +
                "              schema:\n" +
                "                $ref: '#/components/schemas/Content'\n" +
                "components:\n" +
                "  schemas:\n" +
                "    Content:\n" +
                "      type: object" ;

        ParseOptions options = new ParseOptions();
        options.setResolve(false);
        SwaggerParseResult result = new OpenAPIV3Parser().readContents(yaml,null, options);

        assertNotNull(result.getOpenAPI());
        assertEquals((result.getOpenAPI().getPaths().
                get("/persons").getGet().getResponses().get("200")
                .getContent().get("*/*")
                .getSchema()).get$ref(), "#/components/schemas/Content");
    }

    @Test
    public void testIssue393() {
        OpenAPIV3Parser parser = new OpenAPIV3Parser();
        String yaml = "openapi: 3.0.0\n" +
                "info:\n" +
                "  title: x\n" +
                "  version: 1.0.0\n" +
                "paths:\n" +
                "  /test:\n" +
                "    get:\n" +
                "      responses:\n" +
                "        '400':\n" +
                "          description: >\n" +
                "            The account could not be created because a credential didn't meet\n" +
                "            the complexity requirements.\n" +
                "          x-error-refs:\n" +
                "            - $ref: '#/x-error-defs/credentialTooShort'\n" +
                "            - $ref: '#/x-error-defs/credentialTooLong'\n" +
                "x-error-defs:\n" +
                "  credentialTooShort:\n" +
                "    errorID: credentialTooShort";
        final SwaggerParseResult result = parser.readContents(yaml,null,null);

        assertNotNull(result.getOpenAPI());
        OpenAPI openAPI = result.getOpenAPI();

        assertNotNull(openAPI.getExtensions().get("x-error-defs"));
    }

    @Test
    public void testBadFormat() throws Exception {
        OpenAPIV3Parser parser = new OpenAPIV3Parser();
        final OpenAPI openAPI = parser.read("src/test/resources/bad_format.yaml");

        PathItem path = openAPI.getPaths().get("/pets");

        Parameter parameter = path.getGet().getParameters().get(0);
        assertNotNull(parameter);
        assertTrue(parameter instanceof QueryParameter);
        QueryParameter queryParameter = (QueryParameter) parameter;
        assertEquals(queryParameter.getName(), "query-param-int32");
        assertNotNull(queryParameter.getSchema().getEnum());
        assertEquals(queryParameter.getSchema().getEnum().size(), 3);
        List<Object> enumValues = queryParameter.getSchema().getEnum();
        assertEquals(enumValues.get(0), 1);
        assertEquals(enumValues.get(1), 2);
        assertEquals(enumValues.get(2), 7);

        parameter = path.getGet().getParameters().get(1);
        assertNotNull(parameter);
        assertTrue(parameter instanceof QueryParameter);
        queryParameter = (QueryParameter) parameter;
        assertEquals(queryParameter.getName(), "query-param-invalid-format");
        assertNotNull(queryParameter.getSchema().getEnum());
        assertEquals(queryParameter.getSchema().getEnum().size(), 3);
        enumValues = queryParameter.getSchema().getEnum();
        assertEquals(enumValues.get(0), 1);
        assertEquals(enumValues.get(1), 2);
        assertEquals(enumValues.get(2), 7);

        parameter = path.getGet().getParameters().get(2);
        assertNotNull(parameter);
        assertTrue(parameter instanceof QueryParameter);
        queryParameter = (QueryParameter) parameter;
        assertEquals(queryParameter.getName(), "query-param-collection-format-and-uniqueItems");
        assertTrue(queryParameter.getSchema().getUniqueItems());
    }

    @Test
    public void testIssue357() {
        OpenAPIV3Parser parser = new OpenAPIV3Parser();
        final OpenAPI openAPI = parser.read("src/test/resources/issue_357.yaml");
        assertNotNull(openAPI);
        List<Parameter> getParams = openAPI.getPaths().get("/testApi").getGet().getParameters();
        assertEquals(2, getParams.size());
        for (Parameter param : getParams) {

            switch (param.getName()) {
                case "pathParam1":
                    assertEquals(param.getSchema().getType(), "integer");
                    break;
                case "pathParam2":
                    assertEquals(param.getSchema().getType(), "string");
                    break;
                default:
                    fail("Unexpected parameter named " + param.getName());
                    break;
            }
        }
    }

    @Test
    public void testIssue358() {
        OpenAPIV3Parser parser = new OpenAPIV3Parser();
        final OpenAPI openAPI = parser.read("src/test/resources/issue_358.yaml");
        assertNotNull(openAPI);
        List<Parameter> parms = openAPI.getPaths().get("/testApi").getGet().getParameters();
        assertEquals(1, parms.size());
        assertEquals("pathParam", parms.get(0).getName());
        assertEquals("string",  parms.get(0).getSchema().getType());
    }

    @Test
    public void testIncompatibleRefs() {
        String yaml =
                "openapi: 3.0.0\n" +
                        "servers: []\n" +
                        "paths:\n" +
                        "  /test:\n" +
                        "    post:\n" +
                        "      responses:\n" +
                        "        '200':\n" +
                        "          $ref: '#/components/schemas/Schema'\n" +
                        "        '400':\n" +
                        "          definitions: this is right\n" +
                        "          description: Bad Request\n" +
                        "          content:\n" +
                        "            '*/*':\n" +
                        "              schema:\n" +
                        "                $ref: '#/components/schemas/Schema'\n" +
                        "      requestBody:\n" +
                        "        content:\n" +
                        "          application/json:\n" +
                        "            schema:\n" +
                        "              $ref: '#/components/schemas/Schema'\n" +
                        "        required: true\n" +
                        "info:\n" +
                        "  version: ''\n" +
                        "  title: ''\n" +
                        "components:\n" +
                        "  schemas:\n" +
                        "    Schema: {}";
        SwaggerParseResult result = new OpenAPIV3Parser().readContents(yaml,null,null);
        assertNotNull(result.getOpenAPI());
    }

    @Test
    public void testIssue243() {
        String yaml =
                "openapi: 3.0.0\n" +
                        "servers: []\n" +
                        "info:\n" +
                        "  version: 0.0.0\n" +
                        "  title: Simple API\n" +
                        "paths:\n" +
                        "  /:\n" +
                        "    get:\n" +
                        "      responses:\n" +
                        "        '200':\n" +
                        "          description: OK\n" +
                        "          content:\n" +
                        "            '*/*':\n" +
                        "              schema:\n" +
                        "                $ref: '#/components/schemas/Simple'\n" +
                        "components:\n" +
                        "  schemas:\n" +
                        "    Simple:\n" +
                        "      type: string";
        SwaggerParseResult result = new OpenAPIV3Parser().readContents(yaml,null,null);
        assertNotNull(result.getOpenAPI());
    }

    @Test
    public void testIssue594() {
        String yaml = "openapi: 3.0.0\n" +
                "servers: []\n" +
                "paths:\n" +
                "  /test:\n" +
                "    post:\n" +
                "      responses:\n" +
                "        '200':\n" +
                "          description: OK\n" +
                "      requestBody:\n" +
                "        content:\n" +
                "          application/json:\n" +
                "            schema:\n" +
                "              type: array\n" +
                "              minItems: 1\n" +
                "              maxItems: 1\n" +
                "              items:\n" +
                "                $ref: \"#/components/schemas/Pet\"\n" +
                "        description: Hello world\n" +
                "info:\n" +
                "  version: ''\n" +
                "  title: ''";
        SwaggerParseResult result = new OpenAPIV3Parser().readContents(yaml,null,null);
        assertNotNull(result.getOpenAPI());
        ArraySchema schema = (ArraySchema)(result.getOpenAPI().getPaths().get("/test").getPost().getRequestBody().getContent().get("application/json").getSchema());
        assertEquals(schema.getItems().get$ref(),"#/components/schemas/Pet");
        assertNotNull(schema.getMaxItems());
        assertNotNull(schema.getMinItems());

    }

    @Test
    public void testIssue450() {
        String desc = "An array of Pets";
        String xTag = "x-my-tag";
        String xVal = "An extension tag";
        String yaml =
                "openapi: 3.0.0\n" +
                        "servers: []\n" +
                        "info:\n" +
                        "  version: 0.0.0\n" +
                        "  title: Simple API\n" +
                        "paths:\n" +
                        "  /:\n" +
                        "    get:\n" +
                        "      responses:\n" +
                        "        '200':\n" +
                        "          description: OK\n" +
                        "components:\n" +
                        "  schemas:\n" +
                        "    PetArray:\n" +
                        "      type: array\n" +
                        "      items:\n" +
                        "        $ref: '#/components/schemas/Pet'\n" +
                        "      description: An array of Pets\n" +
                        "      x-my-tag: An extension tag\n" +
                        "    Pet:\n" +
                        "      type: object\n" +
                        "      properties:\n" +
                        "        id:\n" +
                        "          type: string";
        SwaggerParseResult result = new OpenAPIV3Parser().readContents(yaml,null,null);
        assertNotNull(result.getOpenAPI());
        final OpenAPI openAPI = result.getOpenAPI();

        Schema petArray = openAPI.getComponents().getSchemas().get("PetArray");
        assertNotNull(petArray);
        assertTrue(petArray instanceof ArraySchema);
        assertEquals(petArray.getDescription(), desc);
        assertNotNull(petArray.getExtensions());
        assertNotNull(petArray.getExtensions().get(xTag));
        assertEquals(petArray.getExtensions().get(xTag), xVal);
    }

    @Test
    public void testIssue480() {
        final OpenAPI openAPI = new OpenAPIV3Parser().read("src/test/resources/issue-480.yaml");

        for (String key : openAPI.getComponents().getSecuritySchemes().keySet()) {
            SecurityScheme definition = openAPI.getComponents().getSecuritySchemes().get(key);
            if ("petstore_auth".equals(key)) {
                assertTrue(definition.getType().equals(SecurityScheme.Type.OAUTH2) );
                //OAuth2 oauth = (OAuth2Definition) definition;
                assertEquals("This is a description", definition.getDescription());
            }
            if ("api_key".equals(key)) {
                assertTrue(definition.getType().equals(SecurityScheme.Type.APIKEY) );
                assertEquals("This is another description", definition.getDescription());
            }
        }
    }

    @Test
    public void checkAllOfAreTaken() {
        OpenAPI openAPI = new OpenAPIV3Parser().read("src/test/resources/allOf-example/allOf.yaml");
        assertEquals(2, openAPI.getComponents().getSchemas().size());

    }

    @Test(description = "Issue #616 Relative references inside of 'allOf'")
    public void checkAllOfWithRelativeReferencesAreFound() {
        OpenAPI openAPI = new OpenAPIV3Parser().read("src/test/resources/allOf-relative-file-references/parent.yaml");
        assertEquals(4, openAPI.getComponents().getSchemas().size());
    }

    @Test(description = "Issue #616 Relative references inside of 'allOf'")
    public void checkAllOfWithRelativeReferencesIssue604() {
        OpenAPI openAPI = new OpenAPIV3Parser().read("src/test/resources/allOf-relative-file-references/swagger.yaml");
        assertEquals(2, openAPI.getComponents().getSchemas().size());
    }

    @Test(description = "A string example should not be over quoted when parsing a yaml string")
    public void readingSpecStringShouldNotOverQuotingStringExample() throws Exception {
        OpenAPIV3Parser parser = new OpenAPIV3Parser();
        ParseOptions options = new ParseOptions();
        options.setResolve(false);
        final OpenAPI openAPI = parser.read("src/test/resources/over-quoted-example.yaml", null, options);

        Map<String, Schema> definitions = openAPI.getComponents().getSchemas();
        assertEquals("NoQuotePlease", definitions.get("CustomerType").getExample());
    }

    @Test(description = "A string example should not be over quoted when parsing a yaml node")
    public void readingSpecNodeShouldNotOverQuotingStringExample() throws Exception {
        String yaml = Files.readFile(new File("src/test/resources/over-quoted-example.yaml"));
        JsonNode rootNode = Yaml.mapper().readValue(yaml, JsonNode.class);
        OpenAPIV3Parser parser = new OpenAPIV3Parser();
        OpenAPI openAPI = (parser.readWithInfo(rootNode)).getOpenAPI();

        Map<String, Schema> definitions = openAPI.getComponents().getSchemas();
        assertEquals("NoQuotePlease", definitions.get("CustomerType").getExample());
    }


    private static int getDynamicPort() {
        return new Random().ints(10000, 20000).findFirst().getAsInt();
    }
}