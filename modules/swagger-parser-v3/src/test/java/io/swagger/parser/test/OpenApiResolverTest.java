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
import io.swagger.oas.models.media.AllOfSchema;
import io.swagger.oas.models.media.ArraySchema;
import io.swagger.oas.models.media.MediaType;
import io.swagger.oas.models.media.Schema;
import io.swagger.oas.models.parameters.RequestBody;
import io.swagger.oas.models.responses.ApiResponse;
import io.swagger.parser.models.AuthorizationValue;
import io.swagger.parser.models.SwaggerParseResult;
import io.swagger.parser.v3.OpenAPIResolver;
import io.swagger.parser.v3.util.OpenAPIDeserializer;
import io.swagger.oas.models.parameters.Parameter;

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


public class OpenApiResolverTest {

    protected int serverPort = getDynamicPort();
    protected WireMockServer wireMockServer;

    @BeforeClass
    private void setUpWireMockServer() throws IOException {
        this.wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        this.wireMockServer.start();
        this.serverPort = wireMockServer.port();
        WireMock.configureFor(this.serverPort);

        String pathFile = FileUtils.readFileToString(new File("src/test/resources/remote_references/remote_pathItem.yaml"));

        WireMock.stubFor(get(urlPathMatching("/remote/path"))
                .willReturn(aResponse()
                        .withStatus(HttpURLConnection.HTTP_OK)
                        .withHeader("Content-type", "application/yaml")
                        .withBody(pathFile
                                .getBytes(StandardCharsets.UTF_8))));

        pathFile = FileUtils.readFileToString(new File("src/test/resources/remote_references/remote_schema_user.yaml"));

        WireMock.stubFor(get(urlPathMatching("/remote/schema"))
                .willReturn(aResponse()
                        .withStatus(HttpURLConnection.HTTP_OK)
                        .withHeader("Content-type", "application/yaml")
                        .withBody(pathFile
                                .getBytes(StandardCharsets.UTF_8))));

        pathFile = FileUtils.readFileToString(new File("src/test/resources/remote_references/responses_notFound.yaml"));

        WireMock.stubFor(get(urlPathMatching("/remote/response"))
                .willReturn(aResponse()
                        .withStatus(HttpURLConnection.HTTP_OK)
                        .withHeader("Content-type", "application/yaml")
                        .withBody(pathFile
                                .getBytes(StandardCharsets.UTF_8))));

        pathFile = FileUtils.readFileToString(new File("src/test/resources/remote_references/requestBody.yaml"));

        WireMock.stubFor(get(urlPathMatching("/remote/requestBody"))
                .willReturn(aResponse()
                        .withStatus(HttpURLConnection.HTTP_OK)
                        .withHeader("Content-type", "application/yaml")
                        .withBody(pathFile
                                .getBytes(StandardCharsets.UTF_8))));

        pathFile = FileUtils.readFileToString(new File("src/test/resources/remote_references/remote_parameter.yaml"));

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
    }

    @AfterClass
    private void tearDownWireMockServer() {
        this.wireMockServer.stop();
    }

    @Test
    public void componentsResolver(@Injectable final List<AuthorizationValue> auths) throws Exception {
        final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

        String pathFile = FileUtils.readFileToString(new File("src/test/resources/oas3.yaml"));
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
        assertEquals(category,schemas.get("Category"));

        //remote url schema
        Schema user = (Schema) pet.getProperties().get("user");
        assertEquals(user.getType(),"object");
        Schema id = (Schema) user.getProperties().get("id");
        assertEquals(id.getType(),"integer");
        assertEquals(id.getFormat(),"int64");

        //ArraySchema items
        ArraySchema tagsProperty = (ArraySchema) pet.getProperties().get("tags");
        assertEquals(tagsProperty.getItems(), schemas.get("Tag"));
        assertEquals(tagsProperty.getType(),"array");

        //Schema not
        assertEquals(schemas.get("OrderRef").getNot(), schemas.get("Category"));

        //Schema additionalProperties
        assertEquals(schemas.get("OrderRef").getAdditionalProperties(), schemas.get("User"));
        //System.out.println(schemas.get("OrderRef").getAdditionalProperties());

        //AllOfSchema
        AllOfSchema extended = (AllOfSchema) schemas.get("ExtendedErrorModel");
        Schema root = (Schema) extended.getAllOf().get(0).getProperties().get("rootCause");
        assertEquals(root, schemas.get("Category"));


        Map<String, ApiResponse> responses = openAPI.getComponents().getResponses();

        //remote url response
        ApiResponse found = responses.get("Found");
        assertEquals(found.getDescription(),"Remote Description");
        assertEquals(found.getContent().get("application/json").getSchema(), extended);

        //internal response headers
        ApiResponse illegalInput = responses.get("IllegalInput");
        assertEquals(illegalInput.getHeaders().get("X-Ref-Limit-Limit"),openAPI.getComponents().getHeaders().get("X-Rate-Limit-Reset"));

        //internal response links
        assertEquals(illegalInput.getLinks().get("address"),openAPI.getComponents().getLinks().get("unsubscribe"));

        //internal url response schema
        MediaType generalError = responses.get("GeneralError").getContent().get("application/json");
        assertEquals(generalError.getSchema(),schemas.get("ExtendedErrorModel"));


        Map<String, RequestBody> requestBodies = openAPI.getComponents().getRequestBodies();

        //internal url requestBody schema
        RequestBody requestBody1 = requestBodies.get("requestBody1");
        MediaType xmlMedia = requestBody1.getContent().get("application/json");
        assertEquals(xmlMedia.getSchema(),schemas.get("Pet"));

        //internal url requestBody ArraySchema
        RequestBody requestBody2 = requestBodies.get("requestBody2");
        MediaType jsonMedia = requestBody2.getContent().get("application/json");
        ArraySchema items = (ArraySchema) jsonMedia.getSchema();
        assertEquals(items.getItems(),schemas.get("User"));

        //internal request body
        assertEquals(requestBody2,requestBodies.get("requestBody3"));

        //remote request body url
        assertEquals(requestBodies.get("reference").getContent().get("application/json").getSchema(),schemas.get("Pet"));

        Map<String, Parameter> parameters = openAPI.getComponents().getParameters();

        //remote url parameter
        assertEquals(parameters.get("remoteParameter").getSchema(),extended);

        //internal Schema Parameter
        assertEquals(parameters.get("newParam").getSchema(),schemas.get("Tag"));

        //parameter examples
        assertEquals(parameters.get("contentParameter").getExamples().get("cat"),openAPI.getComponents().getExamples().get("cat"));

        //parameter content schema

        assertEquals(parameters.get("contentParameter").getContent().get("application/json").getSchema(),extended);

        //internal Schema header
        Map<String, Header> headers = openAPI.getComponents().getHeaders();
        //header remote schema ref
        assertEquals(headers.get("X-Rate-Limit-Remaining").getSchema(),schemas.get("User"));
        //header examples
        assertEquals(headers.get("X-Rate-Limit-Reset").getExamples().get(0),openAPI.getComponents().getExamples().get("dog") );

        //remote header ref
        assertEquals(headers.get("X-Ref-Limit-Limit").getExamples().get(0),openAPI.getComponents().getExamples().get("dog") );


        //header content
        assertEquals(headers.get("X-Rate-Limit-Reset").getContent().get("application/json").getSchema(),extended);

        Map<String, Example> examples = openAPI.getComponents().getExamples();

        //internal url example
        Example frogExample = examples.get("frog");
        assertEquals(frogExample.getSummary(),"An example of a cat");

        //remote example url
        assertEquals(examples.get("referenceCat").getSummary(),"An example of a cat");


        Map<String, Link> links = openAPI.getComponents().getLinks();
        //internal link
        assertEquals(openAPI.getComponents().getLinks().get("referenced"),links.get("unsubscribe"));
        //remote ref link
        assertEquals(openAPI.getComponents().getLinks().get("subscribe").getOperationId(),"cancelHookCallback");


        Map<String, Callback> callbacks = openAPI.getComponents().getCallbacks();
        // internal callback reference
        assertEquals(callbacks.get("referenced"),callbacks.get("failed"));
        //callback pathItem -> operation ->requestBody
        assertEquals(callbacks.get("heartbeat").get("$request.query.heartbeat-url").getPost().getRequestBody(),requestBodies.get("requestBody3"));
        //remote callback ref
        assertEquals(callbacks.get("remoteCallback").get("$response.body#/successUrl").getPost().getRequestBody(),requestBodies.get("requestBody1"));

    }

    @Test
    public void pathsResolver(@Injectable final List<AuthorizationValue> auths) throws Exception {
        final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

        String pathFile = FileUtils.readFileToString(new File("src/test/resources/oas3.yaml"));
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
        assertEquals(schema.getItems(),openAPI.getComponents().getSchemas().get("VeryComplexType"));

        //replace of parameters in operation and remove the ones from the pathItem
        Assert.assertNotNull(openAPI.getPaths().get("/pet").getPost().getParameters());
        Assert.assertNull(openAPI.getPaths().get("/pet").getParameters());

        //remote ref pathItem
        Assert.assertNull(openAPI.getPaths().get("/pathItemRef").getParameters());
        assertEquals(openAPI.getPaths().get("/pathItemRef").getSummary(),"summary");
        assertEquals(openAPI.getPaths().get("/pathItemRef").getPost().getResponses().get("405").getDescription(),"Invalid input");

        //internal pathItem operation -> response -> schema
        Assert.assertNotNull(openAPI.getPaths().get("/pet/{petId}").getGet().getResponses());
        assertEquals(openAPI.getPaths().get("/pet/{petId}").getGet().getResponses().get("200").getContent().get("application/xml").getSchema(),openAPI.getComponents().getSchemas().get("Pet"));

        //internal pathItem -> operation -> callback -> pathItem -> operation -> response -> schema
        assertEquals(openAPI.getPaths().get("/pet/{petId}").getGet().getCallbacks().get("mainHook").get("$request.body#/url").getPost().getResponses().get("200").getContent().get("application/xml").getSchema(),
                     openAPI.getComponents().getSchemas().get("Pet"));

        //internal pathItem -> operation -> requestBody
        assertEquals(openAPI.getPaths().get("/pet/findByStatus").getGet().getRequestBody().getContent().get("multipart/mixed").getSchema().getProperties().get("id"),openAPI.getComponents().getSchemas().get("Pet"));
    }

    private static int getDynamicPort() {
        return new Random().ints(50000, 60000).findFirst().getAsInt();
    }

}