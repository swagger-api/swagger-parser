package io.swagger.v3.parser.test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.callbacks.Callback;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.links.Link;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.PathParameter;
import io.swagger.v3.oas.models.parameters.QueryParameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.parser.OpenAPIResolver;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.ResolverCache;
import io.swagger.v3.parser.core.models.AuthorizationValue;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import io.swagger.v3.parser.processors.ComponentsProcessor;
import io.swagger.v3.parser.processors.PathsProcessor;
import io.swagger.v3.parser.util.OpenAPIDeserializer;
import io.swagger.v3.parser.util.ResolverFully;
import mockit.Injectable;
import mockit.Mocked;
import mockit.StrictExpectations;
import org.apache.commons.io.FileUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;


public class OpenAPIResolverTest {

    protected int serverPort = getDynamicPort();
    protected WireMockServer wireMockServer;
    private static final String REMOTE_REF_JSON = "http://localhost:${dynamicPort}/remote_ref_json#/components/schemas/Tag";
    private static final String REMOTE_REF_YAML = "http://localhost:${dynamicPort}/remote_ref_yaml#/components/schemas/Tag";

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

        pathFile = FileUtils.readFileToString(new File("src/test/resources/remote_references/remote_ref_yaml.yaml"));


        WireMock.stubFor(get(urlPathMatching("/remote_ref_yaml"))
                .willReturn(aResponse()
                        .withStatus(HttpURLConnection.HTTP_OK)
                        .withHeader("Content-type", "application/yaml")
                        .withBody(pathFile
                                .getBytes(StandardCharsets.UTF_8))));

        pathFile = FileUtils.readFileToString(new File("src/test/resources/remote_references/remote_ref_json.json"));


        WireMock.stubFor(get(urlPathMatching("/remote_ref_json"))
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
    public void componentsResolver() throws Exception {
        final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

        String pathFile = FileUtils.readFileToString(new File("src/test/resources/oas3.yaml.template"));
        pathFile = pathFile.replace("${dynamicPort}", String.valueOf(this.serverPort));

        final JsonNode rootNode = mapper.readTree(pathFile.getBytes());
        final OpenAPIDeserializer deserializer = new OpenAPIDeserializer();
        final SwaggerParseResult result = deserializer.deserialize(rootNode);

        Assert.assertNotNull(result);
        final OpenAPI openAPI = result.getOpenAPI();
        Assert.assertNotNull(openAPI);
        assertEquals(new OpenAPIResolver(openAPI, new ArrayList<>(), null).resolve(), openAPI);

        Map<String, Schema> schemas = openAPI.getComponents().getSchemas();

        //internal url schema
        Schema pet = schemas.get("Pet");
        Schema category = (Schema) pet.getProperties().get("category");
        assertEquals(category.get$ref(),"#/components/schemas/Category");

        //remote url schema
        Schema user = (Schema) pet.getProperties().get("user");
        assertEquals(user.get$ref(),"#/components/schemas/User_2");


        //ArraySchema items
        ArraySchema tagsProperty = (ArraySchema) pet.getProperties().get("tags");
        assertEquals(tagsProperty.getItems().get$ref(), "#/components/schemas/ExampleSchema" );
        assertEquals(tagsProperty.getType(),"array");
        Assert.assertNotNull(openAPI.getComponents().getSchemas().get("ExampleSchema"));

        //Schema not
        assertEquals(schemas.get("OrderRef").getNot().get$ref(), "#/components/schemas/Category");

        //Schema additionalProperties
        assertTrue(schemas.get("OrderRef").getAdditionalProperties() instanceof Schema);
        Schema additionalProperties = (Schema) schemas.get("OrderRef").getAdditionalProperties();
        assertEquals(additionalProperties.get$ref(), "#/components/schemas/User_2");

        //AllOfSchema
        ComposedSchema extended = (ComposedSchema) schemas.get("ExtendedErrorModel");
        Schema root = (Schema) extended.getAllOf().get(0).getProperties().get("rootCause");
        assertEquals(root.get$ref(), "#/components/schemas/Category");


        Map<String, ApiResponse> responses = openAPI.getComponents().getResponses();

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
        assertEquals(headers.get("X-Rate-Limit-Remaining").getSchema().get$ref(),"#/components/schemas/User_2");

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
    public void pathsResolver() throws Exception {
        final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

        String pathFile = FileUtils.readFileToString(new File("src/test/resources/oas3.yaml.template"));
        pathFile = pathFile.replace("${dynamicPort}", String.valueOf(this.serverPort));

        final JsonNode rootNode = mapper.readTree(pathFile.getBytes());
        final OpenAPIDeserializer deserializer = new OpenAPIDeserializer();
        final SwaggerParseResult result = deserializer.deserialize(rootNode);

        Assert.assertNotNull(result);
        final OpenAPI openAPI = result.getOpenAPI();
        Assert.assertNotNull(openAPI);

        assertEquals(new OpenAPIResolver(openAPI, new ArrayList<>(), null).resolve(), openAPI);


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
                "openapi: 3.0.1\n" +
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
        //System.out.println(openAPI.getPaths().get("/selfRefB").getGet().getRequestBody().getContent().get("application/json"));

        RequestBody body = openAPI.getPaths().get("/selfRefB").getGet().getRequestBody();
        Schema schema = body.getContent().get("application/json").getSchema();

        assertEquals(schema,openAPI.getComponents().getSchemas().get("SchemaB"));
    }

    @Test
    public void testIssue85(@Injectable final List<AuthorizationValue> auths) {
        String yaml =
                "openapi: '3.0.1'\n" +
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
                "openapi: 3.0.1\n" +
                "paths:\n" +
                "  /selfRefA:\n" +
                "    get:\n" +
                "      requestBody:\n" +
                "        content:\n" +
                "          application/json:\n" +
                "            schema:\n" +
                "              $ref: '#/components/schemas/ModelA'\n" +
                "      responses:\n" +
                "        default:\n" +
                "          description: Default response\n" +
                "  /selfRefB:\n" +
                "    get:\n" +
                "      requestBody:\n" +
                "        content:\n" +
                "          application/json:\n" +
                "            schema:\n" +
                "              $ref: '#/components/schemas/ModelB'\n" +
                "      responses:\n" +
                "        default:\n" +
                "          description: Default response\n" +
                "  /selfRefC:\n" +
                "    get:\n" +
                "      requestBody:\n" +
                "        content:\n" +
                "          application/json:\n" +
                "            schema:\n" +
                "              $ref: '#/components/schemas/ModelC'\n" +
                "      responses:\n" +
                "        default:\n" +
                "          description: Default response\n" +
                "  /selfRefD:\n" +
                "    get:\n" +
                "      responses:\n" +
                "        default:\n" +
                "          description: Default response\n" +
                "          content:\n" +
                "            '*/*':\n" +
                "              schema:\n" +
                "                $ref: '#/components/schemas/ModelA'\n" +
                "  /selfRefE:\n" +
                "    get:\n" +
                "      responses:\n" +
                "        default:\n" +
                "          description: Default response\n" +
                "          content:\n" +
                "            '*/*':\n" +
                "              schema:\n" +
                "                type: array\n" +
                "                items:\n" +
                "                  $ref: '#/components/schemas/ModelA'\n" +
                "info:\n" +
                "  version: ''\n" +
                "  title: ''\n" +
                "components:\n" +
                "  schemas:\n" +
                "    ModelA:\n" +
                "      properties:\n" +
                "        modelB:\n" +
                "          $ref: '#/components/schemas/ModelB'\n" +
                "    ModelB:\n" +
                "      properties:\n" +
                "        modelB:\n" +
                "          $ref: '#/components/schemas/ModelB'\n" +
                "    ModelC:\n" +
                "      properties:\n" +
                "        modelA:\n" +
                "          $ref: '#/components/schemas/ModelA'";

        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setResolveFully(true);

        OpenAPI openAPI = new OpenAPIV3Parser().readContents(yaml,auths,options).getOpenAPI();

        Schema schemaB = openAPI.getPaths().get("/selfRefB").getGet().getRequestBody().getContent().get("application/json").getSchema();
        assertTrue(schemaB instanceof Schema);

        assertEquals(openAPI.getComponents().getSchemas().get("ModelB"), schemaB);

        Schema schema = openAPI.getPaths().get("/selfRefE").getGet().getResponses().get("default").getContent().get("*/*").getSchema();
        assertTrue(schema instanceof ArraySchema);
        ArraySchema arraySchema = (ArraySchema) schema;
        assertEquals(openAPI.getComponents().getSchemas().get("ModelA"), arraySchema.getItems());

    }

    @Test
    public void resolveAllOfWithoutAggregatingParameters(@Injectable final List<AuthorizationValue> auths) {
        ParseOptions options = new ParseOptions();
        options.setResolveFully(true);
        options.setResolveCombinators(false);

        // Testing components/schemas
        OpenAPI openAPI = new OpenAPIV3Parser().readLocation("src/test/resources/composed.yaml",auths,options).getOpenAPI();

        ComposedSchema allOf = (ComposedSchema) openAPI.getComponents().getSchemas().get("ExtendedAddress");
        assertEquals(allOf.getAllOf().size(), 2);

        assertTrue(allOf.getAllOf().get(0).getProperties().containsKey("street"));
        assertTrue(allOf.getAllOf().get(1).getProperties().containsKey("gps"));

        // Testing path item
        ComposedSchema schema = (ComposedSchema) openAPI.getPaths().get("/withInvalidComposedModel").getPost().getRequestBody().getContent().get("application/json").getSchema();

        // In fact the schema resolved previously is the same of /withInvalidComposedModel
        assertEquals(schema, allOf);

        assertEquals(schema.getAllOf().size(), 2);

        assertTrue(schema.getAllOf().get(0).getProperties().containsKey("street"));
        assertTrue(schema.getAllOf().get(1).getProperties().containsKey("gps"));

    }

    @Test
    public void resolveComposedReferenceAllOfSchema(@Injectable final List<AuthorizationValue> auths){



        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setResolveFully(true);

        OpenAPI openAPI = new OpenAPIV3Parser().readLocation("src/test/resources/composed.yaml",auths,options).getOpenAPI();


        assertTrue(openAPI.getPaths().get("/withInvalidComposedModelArray").getPost().getRequestBody().getContent().get("application/json").getSchema() instanceof ArraySchema);
        ArraySchema arraySchema = (ArraySchema) openAPI.getPaths().get("/withInvalidComposedModelArray").getPost().getRequestBody().getContent().get("application/json").getSchema();

        assertTrue(arraySchema.getItems() instanceof ObjectSchema);

    }

    @Test
    public void resolveComposedSchema(@Injectable final List<AuthorizationValue> auths){

        ParseOptions options = new ParseOptions();
        options.setResolveCombinators(false);
        options.setResolveFully(true);
        OpenAPI openAPI = new OpenAPIV3Parser().readLocation("src/test/resources/oneof-anyof.yaml",auths,options).getOpenAPI();


        assertTrue(openAPI.getPaths().get("/mixed-array").getGet().getResponses().get("200").getContent().get("application/json").getSchema() instanceof ArraySchema);
        ArraySchema arraySchema = (ArraySchema) openAPI.getPaths().get("/mixed-array").getGet().getResponses().get("200").getContent().get("application/json").getSchema();
        assertTrue(arraySchema.getItems() instanceof ComposedSchema);
        ComposedSchema oneOf = (ComposedSchema) arraySchema.getItems();
        assertEquals(oneOf.getOneOf().get(0).getType(), "string");

        //System.out.println(openAPI.getPaths().get("/oneOf").getGet().getResponses().get("200").getContent().get("application/json").getSchema() );
        assertTrue(openAPI.getPaths().get("/oneOf").getGet().getResponses().get("200").getContent().get("application/json").getSchema() instanceof ComposedSchema);
        ComposedSchema oneOfSchema = (ComposedSchema) openAPI.getPaths().get("/oneOf").getGet().getResponses().get("200").getContent().get("application/json").getSchema();
        assertEquals(oneOfSchema.getOneOf().get(0).getType(), "object");

    }

    @Test
    public void referringSpecWithoutComponentsTag() throws Exception {
        ParseOptions resolve = new ParseOptions();
        resolve.setResolveFully(true);
        final OpenAPI openAPI = new OpenAPIV3Parser().read("./ref-without-component/a.yaml", null, resolve);

        Map<String, Schema> schemas = openAPI.getComponents().getSchemas();
        Assert.assertEquals("Example value", schemas.get("CustomerType").getExample());
    }


    @Test
    public void testRefNameConflicts() throws Exception {
        ParseOptions options = new ParseOptions();
        options.setResolveFully(true);
        OpenAPI openAPI = new OpenAPIV3Parser().readLocation("/refs-name-conflict/a.yaml",null, options).getOpenAPI();

        assertEquals("local", ((Schema) openAPI.getPaths().get("/newPerson").getPost().getResponses().get("200").getContent().get("*/*").getSchema().getProperties().get("location")).getExample());
        assertEquals("referred", ((Schema)openAPI.getPaths().get("/oldPerson").getPost().getResponses().get("200").getContent().get("*/*").getSchema().getProperties().get("location")).getExample());
        assertEquals("referred", ((Schema)openAPI.getPaths().get("/yetAnotherPerson").getPost().getResponses().get("200").getContent().get("*/*").getSchema().getProperties().get("location")).getExample());
        assertEquals("local", ((Schema) openAPI.getComponents().getSchemas().get("PersonObj").getProperties().get("location")).getExample());
        assertEquals("referred", ((Schema) openAPI.getComponents().getSchemas().get("PersonObj_2").getProperties().get("location")).getExample());
    }


    @Test
    public void testParameterOnPathLevel() throws Exception {
        String yaml = "openapi: 3.0.1\n" +
                "info:\n" +
                "  version: '1.0.0'\n" +
                "  title: 'title'\n" +
                "  description: 'description'\n" +
                "paths:\n" +
                "  /foo:\n" +
                "    parameters:\n" +
                "      - in: query\n" +
                "        name: bar\n" +
                "        schema:\n" +
                "          type: string\n" +
                "    get:\n" +
                "      responses:\n" +
                "        '200':\n" +
                "          description: OK";
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        OpenAPI openAPI = new OpenAPIV3Parser().readContents(yaml, null, options).getOpenAPI();
        assertNotNull(openAPI);
        List<Parameter> getParameters = openAPI.getPaths().get("/foo").getGet().getParameters();
        assertNotNull(getParameters);
        assertEquals(1, getParameters.size());
        assertEquals("bar", getParameters.get(0).getName());
    }

    @Test
    public void testComposedSchemaAdjacent(@Injectable final List<AuthorizationValue> auths) throws Exception {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setResolveFully(true);
        OpenAPI openAPI = new OpenAPIV3Parser().read("src/test/resources/composedSchemaRef.yaml", auths, options);

        Assert.assertNotNull(openAPI);

        Assert.assertTrue(openAPI.getComponents().getSchemas().size() == 5);
        Schema schema = openAPI.getPaths().get("/path").getGet().getResponses().get("200").getContent().get("application/json").getSchema();
        Assert.assertTrue(schema.getProperties().size() == 4);

        ComposedSchema schemaOneOf = (ComposedSchema) openAPI.getPaths().get("/oneOf").getGet().getResponses().get("200").getContent().get("application/json").getSchema();
        Assert.assertTrue(schemaOneOf.getOneOf().size() == 3);

        ComposedSchema schemaAnyOf = (ComposedSchema) openAPI.getPaths().get("/anyOf").getGet().getResponses().get("200").getContent().get("application/json").getSchema();
        Assert.assertTrue(schemaAnyOf.getAnyOf().size() == 3);

    }

    @Test
    public void testComposedSchemaAdjacentWithExamples(@Injectable final List<AuthorizationValue> auths) throws Exception {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setResolveFully(true);
        OpenAPI openAPI = new OpenAPIV3Parser().read("src/test/resources/anyOf_OneOf.yaml", auths, options);

        Assert.assertNotNull(openAPI);

        Assert.assertTrue(openAPI.getComponents().getSchemas().size() == 2);

        Schema schemaPath = openAPI.getPaths().get("/path").getGet().getResponses().get("200").getContent().get("application/json").getSchema();
        Assert.assertTrue(schemaPath instanceof Schema);
        Assert.assertTrue(schemaPath.getProperties().size() == 5);
        Assert.assertTrue(schemaPath.getExample() instanceof HashSet);
        Set<Object> examples = (HashSet) schemaPath.getExample();
        Assert.assertTrue(examples.size() == 2);
    }


    @Test
    public void testSwaggerResolver(@Injectable final OpenAPI swagger,
                                    @Injectable final List<AuthorizationValue> auths,
                                    @Mocked final ResolverCache cache,
                                    @Mocked final ComponentsProcessor componentsProcessor,
                                    @Mocked final PathsProcessor pathsProcessor) throws Exception {

        new StrictExpectations() {{
            new ResolverCache(swagger, auths, null);
            result = cache;
            times = 1;

            new ComponentsProcessor(swagger, cache);
            result = componentsProcessor;
            times = 1;

            new PathsProcessor(cache, swagger, withInstanceOf(OpenAPIResolver.Settings.class));
            result = pathsProcessor;
            times = 1;

            pathsProcessor.processPaths();
            times = 1;

            componentsProcessor.processComponents();
            times = 1;

        }};

        assertEquals(new OpenAPIResolver(swagger, auths, null).resolve(), swagger);
    }

    @Test
    public void testSwaggerResolver_NullSwagger() throws Exception {
        assertNull(new OpenAPIResolver(null, null, null).resolve());
    }

    private void testSimpleRemoteModelProperty(String remoteRef) {
        final OpenAPI swagger = new OpenAPI();
        swagger.components(new Components().addSchemas("Sample", new Schema()
                .addProperties("remoteRef", new Schema().$ref(remoteRef))));


        final OpenAPI resolved = new OpenAPIResolver(swagger, null).resolve();
        final Schema prop = (Schema) resolved.getComponents().getSchemas().get("Sample").getProperties().get("remoteRef");
        assertTrue(prop.get$ref() != null);

        assertEquals(prop.get$ref(), "#/components/schemas/Tag");
        assertNotNull(swagger.getComponents().getSchemas().get("Tag"));
    }

    @Test(description = "resolve a simple remote model property definition in json")
    public void testJsonSimpleRemoteModelProperty() {
        testSimpleRemoteModelProperty(replacePort(REMOTE_REF_JSON));
    }

    @Test(description = "resolve a simple remote model property definition in yaml")
    public void testYamlSimpleRemoteModelProperty() {
        testSimpleRemoteModelProperty(replacePort(REMOTE_REF_YAML));
    }

    private void testArrayRemoteModelProperty(String remoteRef) {

        final OpenAPI swagger = new OpenAPI();
        swagger.components(new Components().addSchemas("Sample", new Schema()
                .addProperties("remoteRef", new ArraySchema().items(new Schema().$ref(remoteRef)))));

        final OpenAPI resolved = new OpenAPIResolver(swagger, null).resolve();
        final Schema prop = (Schema) resolved.getComponents().getSchemas().get("Sample").getProperties().get("remoteRef");
        assertTrue(prop instanceof ArraySchema);
        final ArraySchema ap = (ArraySchema) prop;
        assertEquals(ap.getItems().get$ref(), "#/components/schemas/Tag");
        assertNotNull(swagger.getComponents().getSchemas().get("Tag"));
    }

    @Test(description = "resolve an array remote model property definition in json")
    public void testJsonArrayRemoteModelProperty() {
        testArrayRemoteModelProperty(replacePort(REMOTE_REF_JSON));
    }

    @Test(description = "resolve an array remote model property definition in yaml")
    public void testYamlArrayRemoteModelProperty() {
        testArrayRemoteModelProperty(replacePort(REMOTE_REF_YAML));
    }

    private void testMapRemoteModelProperty(String remoteRef) {
        final OpenAPI swagger = new OpenAPI();
        swagger.components(new Components().addSchemas("Sample", new Schema()
                .addProperties("remoteRef", new Schema().additionalProperties(new Schema().$ref(remoteRef)))));

        final OpenAPI resolved = new OpenAPIResolver(swagger, null).resolve();
        final Schema prop = (Schema)resolved.getComponents().getSchemas().get("Sample").getProperties().get("remoteRef");
        assertTrue(prop.getAdditionalProperties() != null);


        assertEquals(((Schema) prop.getAdditionalProperties()).get$ref(), "#/components/schemas/Tag");
        assertNotNull(swagger.getComponents().getSchemas().get("Tag"));
    }

    @Test(description = "resolve an map remote model property definition in json")
    public void testJsonMapRemoteModelProperty() {
        testMapRemoteModelProperty(replacePort(REMOTE_REF_JSON));
    }

    @Test(description = "resolve an map remote model property definition in yaml")
    public void testYamlMapRemoteModelProperty() {
        testMapRemoteModelProperty(replacePort(REMOTE_REF_YAML));
    }

    private void testOperationBodyparamRemoteRefs(String remoteRef) {
        final OpenAPI swagger = new OpenAPI();
        swagger.path("/fun", new PathItem()
                .get(new Operation()
                        .requestBody(new RequestBody()
                                .content(new Content().addMediaType("*/*",new MediaType()
                                        .schema(new Schema().$ref(remoteRef)))))));

        final OpenAPI resolved = new OpenAPIResolver(swagger, null).resolve();
        final RequestBody param =  swagger.getPaths().get("/fun").getGet().getRequestBody();
        final Schema ref =  param.getContent().get("*/*").getSchema();
        assertEquals(ref.get$ref(), "#/components/schemas/Tag");
        assertNotNull(swagger.getComponents().getSchemas().get("Tag"));
    }

    @Test(description = "resolve operation bodyparam remote refs in json")
    public void testJsonOperationBodyparamRemoteRefs() {
        testOperationBodyparamRemoteRefs(replacePort(REMOTE_REF_JSON));
    }

    @Test(description = "resolve operation bodyparam remote refs in yaml")
    public void testYamlOperationBodyparamRemoteRefs() {

        testOperationBodyparamRemoteRefs(replacePort(REMOTE_REF_YAML));
    }

    @Test(description = "resolve operation parameter remote refs")
    public void testOperationParameterRemoteRefs() {
        final OpenAPI swagger = new OpenAPI();
        List<Parameter> parameters = new ArrayList<>();

        parameters.add(new Parameter().$ref("#/components/parameters/SampleParameter"));

        swagger.path("/fun", new PathItem()
                .get(new Operation()
                        .parameters(parameters)));

        swagger.components(new Components().addParameters("SampleParameter", new QueryParameter()
                .name("skip")
                .schema(new IntegerSchema())));

        final OpenAPI resolved = new OpenAPIResolver(swagger, null).resolve();

        final List<Parameter> params = swagger.getPaths().get("/fun").getGet().getParameters();
        assertEquals(params.size(), 1);
        final Parameter param = params.get(0);
        assertEquals(param.getName(), "skip");
    }


    @Test
    public void testOperationBodyParameterRemoteRefs() {
        final Schema schema = new Schema();

        final OpenAPI swagger = new OpenAPI();
        swagger.path("/fun", new PathItem()
                .get(new Operation()
                        .parameters(Arrays.asList(new Parameter().$ref("#/components/parameters/SampleParameter")))));

        swagger.path("/times", new PathItem()
                .get(new Operation()
                        .parameters(Arrays.asList(new Parameter().$ref("#/components/parameters/SampleParameter")))));

        swagger.components(new Components().addParameters("SampleParameter", new Parameter()
                .name("skip")
                .schema(schema)));

        final OpenAPI resolved = new OpenAPIResolver(swagger, null).resolve();
        final List<Parameter> params = swagger.getPaths().get("/fun").getGet().getParameters();
        assertEquals(params.size(), 1);
        final Parameter param =  params.get(0);
        assertEquals(param.getName(), "skip");
    }

    private void testResponseRemoteRefs(String remoteRef) {
        final OpenAPI swagger = new OpenAPI();
        swagger.path("/fun", new PathItem()
                .get(new Operation()
                        .responses(new ApiResponses().addApiResponse( "200", new ApiResponse()
                                .content(new Content().addMediaType("*/*",new MediaType().schema(new Schema().$ref(remoteRef))))))));

        final OpenAPI resolved = new OpenAPIResolver(swagger, null).resolve();
        final ApiResponse response = swagger.getPaths().get("/fun").getGet().getResponses().get("200");
        final Schema ref = response.getContent().get("*/*").getSchema();
        assertEquals(ref.get$ref(), "#/components/schemas/Tag");
        assertNotNull(swagger.getComponents().getSchemas().get("Tag"));
    }

    @Test(description = "resolve response remote refs in json")
    public void testJsonResponseRemoteRefs() {
        testResponseRemoteRefs(replacePort(REMOTE_REF_JSON));
    }

    @Test(description = "resolve response remote refs in yaml")
    public void testYamlResponseRemoteRefs() {
        testResponseRemoteRefs(replacePort(REMOTE_REF_YAML));
    }

    @Test(description = "resolve array response remote refs in yaml")
    public void testYamlArrayResponseRemoteRefs() {
        final OpenAPI swagger = new OpenAPI();
        swagger.path("/fun", new PathItem()
                .get(new Operation()
                        .responses(new ApiResponses().addApiResponse("200", new ApiResponse()
                                .content(new Content().addMediaType("*/*",new MediaType().schema(
                                        new ArraySchema().items(
                                                new Schema().$ref(replacePort(REMOTE_REF_YAML))))))))));

        final OpenAPI resolved = new OpenAPIResolver(swagger, null).resolve();
        final ApiResponse response = swagger.getPaths().get("/fun").getGet().getResponses().get("200");
        final ArraySchema array = (ArraySchema) response.getContent().get("*/*").getSchema();
        assertNotNull(array.getItems());

        assertEquals(array.getItems().get$ref(), "#/components/schemas/Tag");
        assertNotNull(swagger.getComponents().getSchemas().get("Tag"));
    }

    @Test(description = "resolve shared path parameters")
    public void testSharedPathParametersTest() {
        final OpenAPI swagger = new OpenAPI();
        Operation operation = new Operation()
                .responses(new ApiResponses().addApiResponse("200", new ApiResponse().description("ok!")));
        PathItem path = new PathItem().get(operation);
        path.addParametersItem(new QueryParameter()
                .name("username")
                .schema(new StringSchema()));
        swagger.path("/fun", path);

        final OpenAPI resolved = new OpenAPIResolver(swagger, null).resolve();
        assertNull(resolved.getPaths().get("/fun").getParameters());
        assertTrue(resolved.getPaths().get("/fun").getGet().getParameters().size() == 1);
    }

    @Test(description = "resolve top-level parameters")
    public void testSharedSwaggerParametersTest() {
        final OpenAPI swagger = new OpenAPI();
        List<Parameter> parameters = new ArrayList<>();
        parameters.add(0,new Parameter().$ref("username"));
        swagger.path("/fun", new PathItem()
                .get(new Operation()
                        .parameters(parameters)
                        .responses(new ApiResponses().addApiResponse("200", new ApiResponse().description("ok!")))));

        swagger.components(new Components().addParameters("username", new QueryParameter()
                .name("username")
                .schema(new StringSchema())));

        final OpenAPI resolved = new OpenAPIResolver(swagger, null).resolve();
        assertTrue(resolved.getComponents().getParameters().size() == 1);
        assertTrue(resolved.getPaths().get("/fun").getGet().getParameters().size() == 1);
    }

    @Test(description = "resolve top-level responses")
    public void testSharedResponses() {
        final OpenAPI swagger = new OpenAPI();
        List<Parameter> parameters = new ArrayList<>();
        parameters.add(0,new Parameter().$ref("username"));
        swagger.path("/fun", new PathItem()
                .get(new Operation()
                        .parameters(parameters)
                        .responses(new ApiResponses().addApiResponse("200", new ApiResponse().$ref("#/components/responses/foo")))));

        swagger.components(new Components().addResponses("foo", new ApiResponse().description("ok!")));

        final OpenAPI resolved = new OpenAPIResolver(swagger, null).resolve();
        ApiResponse response = resolved.getPaths().get("/fun").getGet().getResponses().get("200");
        assertTrue(response.getDescription().equals("ok!"));
        assertTrue(response instanceof ApiResponse);
    }

    @Test
    public void testIssue291() {
        String json = "openapi: 3.0.0\n" +
                "servers: []\n" +
                "info:\n" +
                "  title: test spec\n" +
                "  version: '1.0'\n" +
                "paths:\n" +
                "  /test:\n" +
                "    get:\n" +
                "      description: test get\n" +
                "      parameters:\n" +
                "        - $ref: '#/components/parameters/testParam'\n" +
                "      responses:\n" +
                "        default:\n" +
                "          description: test response\n" +
                "components:\n" +
                "  parameters:\n" +
                "    testParam:\n" +
                "      name: test\n" +
                "      in: query\n" +
                "      style: form\n" +
                "      schema:\n" +
                "        type: array\n" +
                "        items:\n" +
                "          type: string";
        OpenAPIV3Parser parser = new OpenAPIV3Parser();
        SwaggerParseResult result = parser.readContents(json, null, null);

        OpenAPI swagger = result.getOpenAPI();
        final OpenAPI resolved = new OpenAPIResolver(swagger, null).resolve();


        Parameter param = resolved.getPaths().get("/test").getGet().getParameters().get(0);
        QueryParameter qp = (QueryParameter) param;
        //assertEquals(qp.getCollectionFormat(), "csv");
    }

    @Test
    public void testSettingsAddParametersToEachOperationDisabled() {
        String yaml ="openapi: 3.0.0\n" +
                "servers: []\n" +
                "info:\n" +
                "  title: test spec\n" +
                "  version: '1.0'\n" +
                "paths:\n" +
                "  '/test/{id}':\n" +
                "    parameters:\n" +
                "      - name: id\n" +
                "        in: path\n" +
                "        required: true\n" +
                "        schema:\n" +
                "          type: string\n" +
                "    get:\n" +
                "      description: test get\n" +
                "      parameters:\n" +
                "        - name: page\n" +
                "          in: query\n" +
                "          schema:\n" +
                "            type: string\n" +
                "      responses:\n" +
                "        default:\n" +
                "          description: test response";

        OpenAPIV3Parser parser = new OpenAPIV3Parser();
        SwaggerParseResult result = parser.readContents(yaml,null,null);

        OpenAPI swagger = result.getOpenAPI();

        final OpenAPI resolved = new OpenAPIResolver(swagger, null, null,
                new OpenAPIResolver.Settings().addParametersToEachOperation(false))
                .resolve();


        assertEquals(resolved.getPaths().get("/test/{id}").getParameters().size(), 1);
        PathParameter pp = (PathParameter)resolved.getPaths().get("/test/{id}").getParameters().get(0);
        assertEquals(pp.getName(), "id");

        assertEquals(resolved.getPaths().get("/test/{id}").getGet().getParameters().size(), 1);
        QueryParameter qp = (QueryParameter)resolved.getPaths().get("/test/{id}").getGet().getParameters().get(0);
        assertEquals(qp.getName(), "page");
    }


    public String replacePort(String url){
        String pathFile = url.replace("${dynamicPort}", String.valueOf(this.serverPort));
        return pathFile;
    }

    private static int getDynamicPort() {
        return new Random().ints(50000, 60000).findFirst().getAsInt();
    }

}