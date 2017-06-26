package io.swagger.parser.test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
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
import io.swagger.parser.v3.util.RemoteUrl;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Mocked;
import org.testng.Assert;

import org.testng.annotations.Test;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;


public class OpenAPIResolverTest {

   @Mocked
    public RemoteUrl remoteUrl = new RemoteUrl();

    private static String pathItemRef_yaml;

    static {
        try {
            pathItemRef_yaml = readFile("src/test/resources/remote_references/remote_pathItem.yaml");
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    static String readFile(String name) throws Exception {
        return new String(Files.readAllBytes(new File(name).toPath()), Charset.forName("UTF-8"));
    }

    @Test
    public void testRemotePathItem(@Injectable final List<AuthorizationValue> auths) throws Exception {
        new Expectations() {{
            remoteUrl.urlToString("https://localhost:8080/remote_references/remote_pathItem", new ArrayList<>());
            result = pathItemRef_yaml ;
        }};

        final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        final JsonNode rootNode = mapper.readTree(Files.readAllBytes(java.nio.file.Paths.get(getClass().getResource("/oas3.yaml").toURI())));

        final OpenAPIDeserializer deserializer = new OpenAPIDeserializer();
        final SwaggerParseResult result = deserializer.deserialize(rootNode);
        Assert.assertNotNull(result);

        final OpenAPI openAPI = result.getOpenAPI();
        Assert.assertNotNull(openAPI);

        assertEquals(new OpenAPIResolver(openAPI, auths, null).resolve(), openAPI);


        Assert.assertNotNull(openAPI.getPaths().get("/pathItemRef"));

    }

    @Test
    public void componentsResolver(@Injectable final List<AuthorizationValue> auths) throws Exception {
        final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        final JsonNode rootNode = mapper.readTree(Files.readAllBytes(java.nio.file.Paths.get(getClass().getResource("/oas3.yaml").toURI())));
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

        //AllOfSchema
        AllOfSchema extended = (AllOfSchema) schemas.get("ExtendedErrorModel");
        Schema root = (Schema) extended.getAllOf().get(0).getProperties().get("rootCause");
        assertEquals(root, schemas.get("Category"));

        Map<String, ApiResponse> responses = openAPI.getComponents().getResponses();

        //remote url response
        ApiResponse notFound = responses.get("Found");
        assertEquals(notFound.getDescription(),"Remote Description");

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

        assertEquals(requestBody2,requestBodies.get("requestBody3"));

        //internal Schema Parameter
        Map<String, Parameter> parameters = openAPI.getComponents().getParameters();
        assertEquals(parameters.get("newParam").getSchema(),schemas.get("Tag"));

        //internal Schema header
        Map<String, Header> headers = openAPI.getComponents().getHeaders();
        //TODO header ref
        assertEquals(headers.get("X-Rate-Limit-Remaining").getSchema(),schemas.get("User"));

        Map<String, Example> examples = openAPI.getComponents().getExamples();

        //internal url example
        Example frogExample = examples.get("frog");
        assertEquals(frogExample.getSummary(),"An example of a cat");

        Map<String, Link> links = openAPI.getComponents().getLinks();
        assertEquals(openAPI.getComponents().getLinks().get("referenced"),links.get("unsubscribe"));


        Map<String, Callback> callback = openAPI.getComponents().getCallbacks();
        //System.out.println(openAPI.getComponents().getCallbacks());

    }

    @Test
    public void pathsResolver(@Injectable final List<AuthorizationValue> auths) throws Exception {
        final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        final JsonNode rootNode = mapper.readTree(Files.readAllBytes(java.nio.file.Paths.get(getClass().getResource("/oas3.yaml").toURI())));
        final OpenAPIDeserializer deserializer = new OpenAPIDeserializer();
        final SwaggerParseResult result = deserializer.deserialize(rootNode);

        Assert.assertNotNull(result);
        final OpenAPI openAPI = result.getOpenAPI();
        Assert.assertNotNull(openAPI);
        assertEquals(new OpenAPIResolver(openAPI, auths, null).resolve(), openAPI);
        ArraySchema schema = (ArraySchema) openAPI.getPaths().get("/pet").getPut().getResponses().get("400").getContent().get("application/json").getSchema();
        assertEquals(schema.getItems(),openAPI.getComponents().getSchemas().get("VeryComplexType"));
        assertEquals(openAPI.getPaths().get("/pathItemRef2"),openAPI.getPaths().get("/pet"));
        //System.out.println(openAPI.getPaths().get("/pet").getPost().getParameters());
        System.out.println(openAPI.getPaths().get("/pet/{petId}").getGet().getResponses());
        System.out.println(openAPI.getPaths().get("/pet/{petId}").getGet().getCallbacks());



    }

}