package io.swagger.parser.test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.swagger.oas.models.OpenAPI;
import io.swagger.oas.models.callbacks.Callback;
import io.swagger.oas.models.examples.Example;
import io.swagger.oas.models.headers.Header;
import io.swagger.oas.models.links.Link;
import io.swagger.oas.models.media.ArraySchema;
import io.swagger.oas.models.media.ComposedSchema;
import io.swagger.oas.models.media.MediaType;
import io.swagger.oas.models.media.Schema;
import io.swagger.oas.models.parameters.RequestBody;
import io.swagger.oas.models.responses.ApiResponse;
import io.swagger.oas.models.security.SecurityScheme;
import io.swagger.parser.models.AuthorizationValue;
import io.swagger.parser.models.ParseOptions;
import io.swagger.parser.models.SwaggerParseResult;
import io.swagger.parser.v3.OpenAPIResolver;
import io.swagger.parser.v3.OpenAPIV3Parser;
import io.swagger.parser.v3.util.OpenAPIDeserializer;
import io.swagger.oas.models.parameters.Parameter;

import io.swagger.parser.v3.util.ResolverFully;
import io.swagger.util.Json;
import mockit.Injectable;

import org.apache.commons.io.FileUtils;
import org.testng.Assert;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;


public class OpenAPIResolverTest {

    protected int serverPort = getDynamicPort();
    protected WireMockServer wireMockServer;

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

        pathFile = FileUtils.readFileToString(new File("src/test/resources/remote_references/remote_responses.yaml"));

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

        WireMock.stubFor(get(urlPathMatching("/remote/link"))
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


    }

    @AfterClass
    private void tearDownWireMockServer() {
        this.wireMockServer.stop();
    }

    @Test
    public void componentsResolver(@Injectable final List<AuthorizationValue> auths) throws Exception {
        final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

        String pathFile = FileUtils.readFileToString(new File("src/test/resources/oas3.yaml.template"));
        pathFile = pathFile.replace("${dynamicPort}", String.valueOf(this.serverPort));

        final JsonNode rootNode = mapper.readTree(pathFile.getBytes());
        final OpenAPIDeserializer deserializer = new OpenAPIDeserializer();
        final SwaggerParseResult result = deserializer.deserialize(rootNode);

        Assert.assertNotNull(result);
        final OpenAPI openAPI = result.getOpenAPI();
        Assert.assertNotNull(openAPI);
        assertEquals(new OpenAPIResolver(openAPI, auths, null).resolve(), openAPI);

        Map<String, Schema> schemas = openAPI.getComponents().getSchemas();

        //internal url schema
        Schema pet = schemas.get("Pet");
        Schema category = (Schema) pet.getProperties().get("category");
        assertEquals(category.get$ref(),"#/components/schemas/Category");

        //remote url schema
        Schema user = (Schema) pet.getProperties().get("user");
        assertEquals(user.get$ref(),"#/components/schemas/User");


        //ArraySchema items
        ArraySchema tagsProperty = (ArraySchema) pet.getProperties().get("tags");
        assertEquals(tagsProperty.getItems().get$ref(), "#/components/schemas/ExampleSchema" );
        assertEquals(tagsProperty.getType(),"array");
        Assert.assertNotNull(openAPI.getComponents().getSchemas().get("ExampleSchema"));

        //Schema not
        assertEquals(schemas.get("OrderRef").getNot().get$ref(), "#/components/schemas/Category");

        //Schema additionalProperties
        assertEquals(schemas.get("OrderRef").getAdditionalProperties().get$ref(), "#/components/schemas/User");

        //AllOfSchema
        ComposedSchema extended = (ComposedSchema) schemas.get("ExtendedErrorModel");
        Schema root = (Schema) extended.getAllOf().get(0).getProperties().get("rootCause");
        assertEquals(root.get$ref(), "#/components/schemas/Category");


        Map<String, ApiResponse> responses = openAPI.getComponents().getResponses();

        //remote url response
        ApiResponse found = responses.get("Found");
        assertEquals(openAPI.getComponents().getResponses().get("RemoteResponse").getDescription(),"Remote Description");
        assertEquals(found.get$ref(), "#/components/responses/RemoteResponse");

        //internal response headers
        ApiResponse illegalInput = responses.get("IllegalInput");
        assertEquals(illegalInput.getHeaders().get("X-Ref-Limit-Limit").get$ref(),"#/components/headers/X-Rate-Limit-Reset");

        //internal response links
        assertEquals(illegalInput.getLinks().get("address").get$ref(),"#/components/links/unsubscribe");

        //internal url response schema
        MediaType generalError = responses.get("GeneralError").getContent().get("application/json");
        assertEquals(generalError.getSchema().get$ref(),"#/components/schemas/ExtendedErrorModel");


        Map<String, RequestBody> requestBodies = openAPI.getComponents().getRequestBodies();

        //internal url requestBody schema
        RequestBody requestBody1 = requestBodies.get("requestBody1");
        MediaType xmlMedia = requestBody1.getContent().get("application/json");
        assertEquals(xmlMedia.getSchema().get$ref(),"#/components/schemas/Pet");

        //internal url requestBody ArraySchema
        RequestBody requestBody2 = requestBodies.get("requestBody2");
        MediaType jsonMedia = requestBody2.getContent().get("application/json");
        ArraySchema items = (ArraySchema) jsonMedia.getSchema();
        assertEquals(items.getItems().get$ref(),"#/components/schemas/User");

        //internal request body
        assertEquals("#/components/requestBodies/requestBody2",requestBodies.get("requestBody3").get$ref());

        //remote request body url
        assertEquals(requestBodies.get("reference").get$ref(),"#/components/requestBodies/remote_requestBody");

        Map<String, Parameter> parameters = openAPI.getComponents().getParameters();

        //remote url parameter
        assertEquals(parameters.get("remoteParameter").get$ref(),"#/components/parameters/parameter");


        //internal Schema Parameter
        assertEquals(parameters.get("newParam").getSchema().get$ref(),"#/components/schemas/Tag");

        //parameter examples
        assertEquals(parameters.get("contentParameter").getExamples().get("cat"),openAPI.getComponents().getExamples().get("cat"));

        //parameter content schema

        assertEquals(parameters.get("contentParameter").getContent().get("application/json").getSchema().get$ref(),"#/components/schemas/ExtendedErrorModel");

        //internal Schema header
        Map<String, Header> headers = openAPI.getComponents().getHeaders();
        //header remote schema ref
        assertEquals(headers.get("X-Rate-Limit-Remaining").getSchema().get$ref(),"#/components/schemas/User");

        //header examples
        assertEquals(headers.get("X-Rate-Limit-Reset").getExamples().get("headerExample").get$ref(), "#/components/examples/dog" );
        //remote header ref
        assertEquals(headers.get("X-Ref-Limit-Limit").get$ref(),"#/components/headers/X-Rate-Limit-Reset" );


        //header content
        assertEquals(headers.get("X-Rate-Limit-Reset").getContent().get("application/json").getSchema().get$ref(),"#/components/schemas/ExtendedErrorModel");

        Map<String, Example> examples = openAPI.getComponents().getExamples();

        //internal url example
        Example frogExample = examples.get("frog");
        assertEquals(frogExample.get$ref(),"#/components/examples/cat");

        //remote example url
        assertEquals(examples.get("referenceCat").get$ref(),"#/components/examples/example");

        //internal url securityScheme
        SecurityScheme scheme = openAPI.getComponents().getSecuritySchemes().get("reference");
        assertEquals(scheme.getType(),SecurityScheme.Type.APIKEY);

        SecurityScheme remoteScheme = openAPI.getComponents().getSecuritySchemes().get("remote_reference");
        assertEquals(remoteScheme.getType(), SecurityScheme.Type.OAUTH2);


        Map<String, Link> links = openAPI.getComponents().getLinks();
        //internal link
        assertEquals(openAPI.getComponents().getLinks().get("referenced").get$ref(),"#/components/links/unsubscribe");
        //remote ref link
        assertEquals(openAPI.getComponents().getLinks().get("subscribe").get$ref(),"#/components/links/link");


        Map<String, Callback> callbacks = openAPI.getComponents().getCallbacks();
        // internal callback reference
        assertEquals(callbacks.get("referenced").get("$ref").get$ref(),"#/components/callbacks/failed");
        //callback pathItem -> operation ->requestBody
        assertEquals(callbacks.get("heartbeat").get("$request.query.heartbeat-url").getPost().getRequestBody().get$ref(),"#/components/requestBodies/requestBody3");
        //remote callback ref
        assertEquals(callbacks.get("remoteCallback").get("$ref").get$ref(),"#/components/callbacks/callback");

    }

    @Test
    public void pathsResolver(@Injectable final List<AuthorizationValue> auths) throws Exception {
        final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

        String pathFile = FileUtils.readFileToString(new File("src/test/resources/oas3.yaml.template"));
        pathFile = pathFile.replace("${dynamicPort}", String.valueOf(this.serverPort));

        final JsonNode rootNode = mapper.readTree(pathFile.getBytes());
        final OpenAPIDeserializer deserializer = new OpenAPIDeserializer();
        final SwaggerParseResult result = deserializer.deserialize(rootNode);

        Assert.assertNotNull(result);
        final OpenAPI openAPI = result.getOpenAPI();
        Assert.assertNotNull(openAPI);

        assertEquals(new OpenAPIResolver(openAPI, auths, null).resolve(), openAPI);


        //internal url pathItem
        assertEquals(openAPI.getPaths().get("/pathItemRef2"),openAPI.getPaths().get("/pet"));

        //internal array schema inside operation -> responses -> content
        ArraySchema schema = (ArraySchema) openAPI.getPaths().get("/pet").getPut().getResponses().get("400").getContent().get("application/json").getSchema();
        assertEquals(schema.getItems().get$ref(),"#/components/schemas/VeryComplexType");

        //replace of parameters in operation and remove the ones from the pathItem
        Assert.assertNotNull(openAPI.getPaths().get("/pet").getPost().getParameters());
        Assert.assertNull(openAPI.getPaths().get("/pet").getParameters());

        //remote ref pathItem
        assertEquals(openAPI.getPaths().get("/pathItemRef").getSummary(),"summary");
        assertEquals(openAPI.getPaths().get("/pathItemRef").getPost().getResponses().get("405").getDescription(),"Invalid input");

        //internal pathItem operation -> response -> schema
        Assert.assertNotNull(openAPI.getPaths().get("/pet/{petId}").getGet().getResponses());
        assertEquals(openAPI.getPaths().get("/pet/{petId}").getGet().getResponses().get("200").getContent().get("application/xml").getSchema().get$ref(),"#/components/schemas/Pet");

        //internal pathItem -> operation -> callback -> pathItem -> operation -> response -> schema
        assertEquals(openAPI.getPaths().get("/pet/{petId}").getGet().getCallbacks().get("mainHook").get("$request.body#/url").getPost().getResponses().get("200").getContent().get("application/xml").getSchema().get$ref(),
                     "#/components/schemas/Pet");

        //internal pathItem -> operation -> requestBody
        Schema id = (Schema) openAPI.getPaths().get("/pet/findByStatus").getGet().getRequestBody().getContent().get("multipart/mixed").getSchema().getProperties().get("id");
        assertEquals(id.get$ref(),"#/components/schemas/Pet");

        //internal parameter url
        assertEquals(openAPI.getPaths().get("/store/inventory").getGet().getParameters().get(0), openAPI.getComponents().getParameters().get("limitParam"));
    }

    @Test
    public void testSelfReferenceResolution(@Injectable final List<AuthorizationValue> auths)throws Exception {

        String yaml = "" +
                "openapi: 3.0.0\n" +
                "paths:\n" +
                "  \"/selfRefB\":\n" +
                "    get:\n" +
                "      requestBody:\n" +
                "        description: user to add to the system\\n\"+\n" +
                "        content:\n" +
                "         'application/json':\n" +
                "             schema:\n" +
                "                $ref: '#/components/schemas/SchemaB'\n" +
                "components:\n" +
                "  schemas:\n" +
                "    SchemaA:\n" +
                "      properties:\n" +
                "        name:\n" +
                "          type: string\n" +
                "        modelB:\n" +
                "          $ref: '#/components/schemas/SchemaB'\n" +
                "    SchemaB:\n" +
                "      properties:\n" +
                "        modelB:\n" +
                "          $ref: '#/components/schemas/SchemaB'";

        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setResolveFully(true);

        OpenAPI openAPI = new OpenAPIV3Parser().readContents(yaml,auths,options).getOpenAPI();
        ResolverFully resolverUtil = new ResolverFully();
        resolverUtil.resolveFully(openAPI);
        System.out.println(openAPI.getPaths().get("/selfRefB").getGet().getRequestBody().getContent().get("application/json"));

        RequestBody body = openAPI.getPaths().get("/selfRefB").getGet().getRequestBody();
        Schema schema = body.getContent().get("application/json").getSchema();

        assertEquals(schema,openAPI.getComponents().getSchemas().get("SchemaB"));
    }

    @Test
    public void testIssue85(@Injectable final List<AuthorizationValue> auths) {
        String yaml =
                "openapi: '3.0.0'\n" +
                        "paths: \n" +
                        "  /test/method: \n" +
                        "    post: \n" +
                        "      parameters: \n" +
                        "        - \n" +
                        "          in: \"path\"\n" +
                        "          name: \"body\"\n" +
                        "          required: false\n" +
                        "          schema: \n" +
                        "            $ref: '#/components/Schemas/StructureA'\n" +
                        "components: \n" +
                        "   schemas:\n" +
                        "       StructureA: \n" +
                        "           type: object\n" +
                        "           properties: \n" +
                        "               someProperty: \n" +
                        "                   type: string\n" +
                        "               arrayOfOtherType: \n" +
                        "                   type: array\n" +
                        "                   items: \n" +
                        "                       $ref: '#/definitions/StructureB'\n" +
                        "       StructureB: \n" +
                        "           type: object\n" +
                        "           properties: \n" +
                        "               someProperty: \n" +
                        "                   type: string\n";

        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setResolveFully(true);

        OpenAPI openAPI = new OpenAPIV3Parser().readContents(yaml,auths,options).getOpenAPI();
        ResolverFully resolverUtil = new ResolverFully();
        resolverUtil.resolveFully(openAPI);
        Parameter param = openAPI.getPaths().get("/test/method").getPost().getParameters().get(0);

        Schema schema = param.getSchema();
        assertNotNull(schema.getProperties().get("someProperty"));

        ArraySchema am = (ArraySchema) schema.getProperties().get("arrayOfOtherType");
        assertNotNull(am);
        Schema prop = am.getItems();
        assertTrue(prop instanceof Schema);
    }

    @Test
    public void selfReferenceTest(@Injectable final List<AuthorizationValue> auths) {
        String yaml = "" +
                "openapi: '3.0.0'\n" +
                "paths:\n" +
                "  /selfRefA:\n" +
                "    get:\n" +
                "      parameters:\n" +
                "        - in: query\n" +
                "          name: body\n" +
                "          schema:\n" +
                "            $ref: '#/components/Schemas/SchemaA'\n" +
                "  /selfRefB:\n" +
                "    get:\n" +
                "      parameters:\n" +
                "        - in: query\n" +
                "          name: body\n" +
                "          schema:\n" +
                "            $ref: '#/components/Schemas/SchemaB'\n" +
                "  /selfRefC:\n" +
                "    get:\n" +
                "      parameters:\n" +
                "        - in: query\n" +
                "          name: body\n" +
                "          schema:\n" +
                "            $ref: '#/components/Schemas/SchemaC'\n" +
                "  /selfRefD:\n" +
                "    get:\n" +
                "      parameters: []\n" +
                "      responses:\n" +
                "           default:\n"+
                "               content:\n"+
                "                'application/json':\n"+
                "                     schema:\n"+
                "                        type: array\n" +
                "                        items:\n" +
                "                           $ref: '#/components/Schemas/SchemaA'\n" +
                "  /selfRefE:\n" +
                "    get:\n" +
                "      parameters: []\n" +
                "      responses:\n" +
                "           default:\n"+
                "               content:\n"+
                "                'application/json':\n"+
                "                     schema:\n"+
                "                        type: array\n" +
                "                        items:\n" +
                "                           $ref: '#/components/Schemas/SchemaA'\n" +

                "components:\n" +
                "   schemas:\n" +
                "       SchemaA:\n" +
                "           properties:\n" +
                "               modelB:\n" +
                "                   $ref: '#/components/Schemas/SchemaB'\n" +
                "       SchemaB:\n" +
                "           properties:\n" +
                "                modelB:\n" +
                "                    $ref: '#/components/Schemas/SchemaB'\n" +
                "       SchemaC:\n" +
                "            properties:\n" +
                "               modelA:\n" +
                "                   $ref: '#/components/Schemas/SchemaA'";

        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setResolveFully(true);

        OpenAPI openAPI = new OpenAPIV3Parser().readContents(yaml,auths,options).getOpenAPI();
        ResolverFully resolverUtil = new ResolverFully();
        resolverUtil.resolveFully(openAPI);

        Schema schemaB = openAPI.getPaths().get("/selfRefB").getGet().getParameters().get(0).getSchema();
        assertTrue(schemaB instanceof Schema);

        assertEquals(schemaB, openAPI.getComponents().getSchemas().get("SchemaB"));

        Schema schema = openAPI.getPaths().get("/selfRefE").getGet().getResponses().get("default").getContent().get("application/json").getSchema();
        assertTrue(schema instanceof ArraySchema);
        ArraySchema arraySchema = (ArraySchema) schema;
        assertEquals(arraySchema.getItems(), openAPI.getComponents().getSchemas().get("SchemaA"));

    }

    private static int getDynamicPort() {
        return new Random().ints(50000, 60000).findFirst().getAsInt();
    }

}