package io.swagger.parser.v3.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.swagger.oas.models.ExternalDocumentation;
import io.swagger.oas.models.OpenAPI;
import io.swagger.oas.models.PathItem;
import io.swagger.oas.models.Paths;
//import io.swagger.oas.models.headers.Headers;
import io.swagger.oas.models.security.SecurityRequirement;
import io.swagger.oas.models.tags.Tag;
import io.swagger.oas.models.info.Info;
import io.swagger.oas.models.info.License;
import io.swagger.oas.models.info.Contact;
import io.swagger.oas.models.responses.ApiResponse;
import io.swagger.oas.models.responses.ApiResponses;
import io.swagger.parser.models.SwaggerParseResult;
import io.swagger.oas.models.servers.Server;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.util.List;
import java.util.Map;

public class OpenAPIDeserializerTest {

    @Test(dataProvider = "data")
    public void readInfoObject(JsonNode rootNode) throws Exception {


        final OpenAPIDeserializer deserializer = new OpenAPIDeserializer();
        final SwaggerParseResult result = deserializer.deserialize(rootNode);

        Assert.assertNotNull(result);

        final OpenAPI openAPI = result.getOpenAPI();
        Assert.assertNotNull(openAPI);
        Assert.assertEquals(openAPI.getOpenapi(),"3.0.0-RC1");

        final Info info = openAPI.getInfo();
        Assert.assertNotNull(info);
        Assert.assertEquals(info.getTitle(), "Sample Pet Store App");
        Assert.assertEquals(info.getDescription(), "This is a sample server Petstore");
        Assert.assertEquals(info.getTermsOfService(), "http://swagger.io/terms/");

        final Contact contact = info.getContact();
        Assert.assertNotNull(contact);
        Assert.assertEquals(contact.getName(),"API Support");
        Assert.assertEquals(contact.getUrl(),"http://www.example.com/support");
        Assert.assertEquals(contact.getEmail(),"support@example.com");

        final License license = info.getLicense();
        Assert.assertNotNull(license);
        Assert.assertEquals(license.getName(), "Apache 2.0");
        Assert.assertEquals(license.getUrl(), "http://www.apache.org/licenses/LICENSE-2.0.html");

        Assert.assertEquals(info.getVersion(), "1.0.1");

    }

    @Test(dataProvider = "data")
    public void readServerObject(JsonNode rootNode) throws Exception {
        final OpenAPIDeserializer deserializer = new OpenAPIDeserializer();
        final SwaggerParseResult result = deserializer.deserialize(rootNode);

        Assert.assertNotNull(result);

        final OpenAPI openAPI = result.getOpenAPI();
        Assert.assertNotNull(openAPI);

        final List<Server> server = openAPI.getServers();
        Assert.assertNotNull(server);
        Assert.assertNotNull(server.get(0));
        Assert.assertNotNull(server.get(0).getUrl());
        Assert.assertEquals(server.get(0).getUrl(),"http://petstore.swagger.io/v2");

        Assert.assertNotNull(server.get(1));
        Assert.assertNotNull(server.get(1).getUrl());
        Assert.assertNotNull(server.get(1).getDescription());
        Assert.assertEquals(server.get(1).getUrl(),"https://development.gigantic-server.com/v1");
        Assert.assertEquals(server.get(1).getDescription(),"Development server");

        Assert.assertNotNull(server.get(2));
        Assert.assertNotNull(server.get(2).getVariables());
        Assert.assertNotNull(server.get(2).getVariables().values());
        Assert.assertNotNull(server.get(2).getVariables().get("username"));
        Assert.assertEquals(server.get(2).getVariables().get("username").getDefault(),"demo");
        Assert.assertEquals(server.get(2).getVariables().get("username").getDescription(),"this value is assigned by the service provider, in this example `gigantic-server.com`");
        Assert.assertNotNull(server.get(2).getVariables().get("port").getEnum());
        Assert.assertEquals(server.get(2).getVariables().get("port").getEnum().get(0),"8443");
        Assert.assertEquals(server.get(2).getVariables().get("port").getEnum().get(1),"443");
        Assert.assertEquals(server.get(2).getVariables().get("port").getDefault(),"8443");
        Assert.assertNotNull(server.get(2).getVariables().get("port"));
        Assert.assertNotNull(server.get(2).getVariables().get("basePath"));
        Assert.assertEquals(server.get(2).getVariables().get("basePath").getDescription(),"testing overwriting");
        Assert.assertEquals(server.get(2).getVariables().get("basePath").getDefault(),"v2");


    }

    @Test(dataProvider = "data")
    public void readContentObject(JsonNode rootNode) throws Exception {
        final OpenAPIDeserializer deserializer = new OpenAPIDeserializer();
        final SwaggerParseResult result = deserializer.deserialize(rootNode);

        Assert.assertNotNull(result);

        final OpenAPI openAPI = result.getOpenAPI();
        Assert.assertNotNull(openAPI);

        final Paths paths = openAPI.getPaths();
        Assert.assertNotNull(paths);

        PathItem petByStatusEndpoint = paths.get("/pet/findByStatus");
        Assert.assertNotNull(petByStatusEndpoint.getGet());
        Assert.assertNotNull(petByStatusEndpoint.getGet().getParameters());
        Assert.assertEquals(petByStatusEndpoint.getGet().getParameters().size(), 1);
        Assert.assertNotNull(petByStatusEndpoint.getGet().getParameters().get(0).getContent());
        Assert.assertEquals(petByStatusEndpoint.getGet().getParameters().get(0).getContent().size(),3);
        Assert.assertEquals(petByStatusEndpoint.getGet().getParameters().get(0).getContent().get("application/json").getSchema().getType(),"array");
        Assert.assertEquals(petByStatusEndpoint.getGet().getParameters().get(0).getContent().get("application/json").getExample(),"example string");
        Assert.assertEquals(petByStatusEndpoint.getGet().getParameters().get(0).getContent().get("application/json").getExamples().get("list").getSummary(),"List of Names");
        Assert.assertEquals(petByStatusEndpoint.getGet().getParameters().get(0).getContent().get("application/json").getSchema().getType(),"array");
        Assert.assertEquals(petByStatusEndpoint.getGet().getParameters().get(0).getContent().get("application/json").getEncoding().get("profileImage").getContentType(),"image/png, image/jpeg");
        //Assert.assertNotNull(petByStatusEndpoint.getGet().getParameters().get(0).getContent().get("application/json").getEncoding().get("profileImage").getHeaders());

        Assert.assertEquals(petByStatusEndpoint.getGet().getParameters().get(0).getContent().get("application/xml").getExamples().get("list").getSummary(),"List of names");
        Assert.assertEquals(petByStatusEndpoint.getGet().getParameters().get(0).getContent().get("application/xml").getExamples().get("list").getValue(),"<Users><User name='Bob'/><User name='Diane'/><User name='Mary'/><User name='Bill'/></Users>");

        Assert.assertNotNull(petByStatusEndpoint.getGet().getParameters().get(0).getContent().get("application/xml").getExamples().get("empty").getSummary());
        Assert.assertEquals(petByStatusEndpoint.getGet().getParameters().get(0).getContent().get("application/xml").getExamples().get("empty").getSummary(),"Empty list");
        Assert.assertEquals(petByStatusEndpoint.getGet().getParameters().get(0).getContent().get("application/xml").getExamples().get("empty").getValue(),"<Users/>");


        Assert.assertEquals(petByStatusEndpoint.getGet().getParameters().get(0).getContent().get("text/plain").getExamples().get("list").getSummary(),"List of names");
        Assert.assertEquals(petByStatusEndpoint.getGet().getParameters().get(0).getContent().get("text/plain").getExamples().get("list").getValue(),"Bob,Diane,Mary,Bill");
        Assert.assertEquals(petByStatusEndpoint.getGet().getParameters().get(0).getContent().get("text/plain").getExamples().get("empty").getSummary(),"Empty");
        Assert.assertEquals(petByStatusEndpoint.getGet().getParameters().get(0).getContent().get("text/plain").getExamples().get("empty").getValue(),"");

        PathItem petEndpoint = paths.get("/pet");
        Assert.assertNotNull(petEndpoint.getPut());
        Assert.assertNotNull(petEndpoint.getPut().getResponses().get("400").getContent().get("application/json"));
        Assert.assertEquals(petEndpoint.getPut().getResponses().get("400").getContent().size(),1);
        Assert.assertEquals(petEndpoint.getPut().getResponses().get("400").getContent().get("application/json").getSchema().getType(), "array");
    }

    @Test(dataProvider = "data")
    public void readRequestBodyObject(JsonNode rootNode) throws Exception {
        final OpenAPIDeserializer deserializer = new OpenAPIDeserializer();
        final SwaggerParseResult result = deserializer.deserialize(rootNode);

        Assert.assertNotNull(result);

        final OpenAPI openAPI = result.getOpenAPI();
        Assert.assertNotNull(openAPI);

        final Paths paths = openAPI.getPaths();
        Assert.assertNotNull(paths);

        PathItem petByStatusEndpoint = paths.get("/pet/findByStatus");
        Assert.assertNotNull(petByStatusEndpoint.getGet().getRequestBody());
        Assert.assertEquals(petByStatusEndpoint.getGet().getRequestBody().getDescription(),"pet store to add to the system");
        Assert.assertTrue(petByStatusEndpoint.getGet().getRequestBody().getRequired(),"true");
        Assert.assertNotNull(petByStatusEndpoint.getGet().getRequestBody().getContent().get("multipart/mixed"));
        Assert.assertEquals(petByStatusEndpoint.getGet().getRequestBody().getContent().get("multipart/mixed").getSchema().getType(),"object");
        //TODO: Assert.assertNotNull(petByStatusEndpoint.getGet().getRequestBody().getContent().get("multipart/mixed").getSchema().getProperties());
        Assert.assertEquals(petByStatusEndpoint.getGet().getRequestBody().getContent().get("multipart/mixed").getEncoding().get("historyMetadata").getContentType(),"application/xml; charset=utf-8");
        //Assert.assertNotNull(petByStatusEndpoint.getGet().getRequestBody().getContent().get("multipart/mixed").getEncoding().get("profileImage").getHeaders());
        //Headers petHeader = (Headers)petByStatusEndpoint.getGet().getRequestBody().getContent().get("multipart/mixed").getEncoding().get("profileImage").getHeaders();
        //Assert.assertNotNull(petHeader.get("X-Rate-Limit"));
    }

    @Test(dataProvider = "data")
    public void readSecurityRequirementsObject(JsonNode rootNode) throws Exception {
        final OpenAPIDeserializer deserializer = new OpenAPIDeserializer();
        final SwaggerParseResult result = deserializer.deserialize(rootNode);

        Assert.assertNotNull(result);

        final OpenAPI openAPI = result.getOpenAPI();
        Assert.assertNotNull(openAPI);

        final List<SecurityRequirement> requirements = openAPI.getSecurity();
        Assert.assertNotNull(requirements);
        Assert.assertEquals(requirements.size(),2);

        SecurityRequirement requirement = requirements.get(0);
        Assert.assertTrue(requirement.containsKey("api_key"));

        requirement = requirements.get(1);
        Assert.assertTrue(requirement.containsKey("tokenAuth"));


    }

    @Test(dataProvider = "data")
    public void readExtensions(JsonNode rootNode) throws Exception {
        final OpenAPIDeserializer deserializer = new OpenAPIDeserializer();
        final SwaggerParseResult result = deserializer.deserialize(rootNode);

        Assert.assertNotNull(result);

        final OpenAPI openAPI = result.getOpenAPI();
        Assert.assertNotNull(openAPI);
        Assert.assertNotNull(openAPI.getExtensions());
        Assert.assertTrue(openAPI.getExtensions().containsKey("x-origin"));
        Object object = openAPI.getExtensions().get("x-origin");

        Assert.assertTrue(object instanceof List);
        List elements = (List) object;
        Assert.assertEquals(elements.size(), 1);
        Map<String, Object> map = (Map) elements.get(0);
        Assert.assertEquals(map.get("url"), "http://petstore.swagger.io/v2/swagger.json");
        Assert.assertEquals(map.get("format"), "swagger");
        Assert.assertEquals(map.get("version"), "2.0");

        Map<String, Object> converter = (Map<String, Object>) map.get("converter");
        Assert.assertNotNull(converter);
        Assert.assertEquals(converter.get("url"), "https://github.com/mermade/swagger2openapi");
        Assert.assertEquals(converter.get("version"), "1.2.1");

        object = openAPI.getExtensions().get("x-api-title");
        Assert.assertTrue(object instanceof String);
        Assert.assertEquals("pet store test api", object.toString());
    }

    @Test(dataProvider = "data")
    public void readTagObject(JsonNode rootNode) throws Exception {
        final OpenAPIDeserializer deserializer = new OpenAPIDeserializer();
        final SwaggerParseResult result = deserializer.deserialize(rootNode);

        Assert.assertNotNull(result);

        final OpenAPI openAPI = result.getOpenAPI();
        Assert.assertNotNull(openAPI);

        final List<Tag> Tag = openAPI.getTags();
        Assert.assertNotNull(Tag);
        Assert.assertNotNull(Tag.get(0));
        Assert.assertNotNull(Tag.get(0).getName());
        Assert.assertEquals(Tag.get(0).getName(),"pet");
        Assert.assertNotNull(Tag.get(0).getDescription());
        Assert.assertEquals(Tag.get(0).getDescription(),"Everything about your Pets");
        Assert.assertNotNull(Tag.get(0).getExternalDocs());

        Assert.assertNotNull(Tag.get(1));
        Assert.assertNotNull(Tag.get(1).getName());
        Assert.assertNotNull(Tag.get(1).getDescription());
        Assert.assertEquals(Tag.get(1).getName(),"store");
        Assert.assertEquals(Tag.get(1).getDescription(),"Access to Petstore orders");
    }

    @Test(dataProvider = "data")
    public void readExamplesObject(JsonNode rootNode) throws Exception {
        final OpenAPIDeserializer deserializer = new OpenAPIDeserializer();
        final SwaggerParseResult result = deserializer.deserialize(rootNode);

        Assert.assertNotNull(result);

        final OpenAPI openAPI = result.getOpenAPI();
        Assert.assertNotNull(openAPI);

        final Paths paths = openAPI.getPaths();
        Assert.assertNotNull(paths);
        Assert.assertEquals(paths.size(), 14);

        //parameters operation get
        PathItem petByStatusEndpoint = paths.get("/pet/findByStatus");
        Assert.assertNotNull(petByStatusEndpoint.getGet());
        Assert.assertNotNull(petByStatusEndpoint.getGet().getParameters());
        Assert.assertEquals(petByStatusEndpoint.getGet().getParameters().size(), 1);
        Assert.assertEquals(petByStatusEndpoint.getGet().getParameters().get(0).getName(), "status");
        Assert.assertEquals(petByStatusEndpoint.getGet().getParameters().get(0).getIn(),"query");

    }

    @Test(dataProvider = "data")
    public void readSchemaObject(JsonNode rootNode) throws Exception {
        final OpenAPIDeserializer deserializer = new OpenAPIDeserializer();
        final SwaggerParseResult result = deserializer.deserialize(rootNode);

        Assert.assertNotNull(result);

        final OpenAPI openAPI = result.getOpenAPI();
        Assert.assertNotNull(openAPI);

        final Paths paths = openAPI.getPaths();
        Assert.assertNotNull(paths);
        Assert.assertEquals(paths.size(), 14);

        //parameters operation get
        PathItem petByStatusEndpoint = paths.get("/pet/findByStatus");
        Assert.assertNotNull(petByStatusEndpoint.getGet());
        Assert.assertNotNull(petByStatusEndpoint.getGet().getParameters());
        Assert.assertEquals(petByStatusEndpoint.getGet().getParameters().size(), 1);
        Assert.assertNotNull(petByStatusEndpoint.getGet().getParameters().get(0).getSchema());
        Assert.assertEquals(petByStatusEndpoint.getGet().getParameters().get(0).getSchema().getFormat(), "int64");
        Assert.assertEquals(petByStatusEndpoint.getGet().getParameters().get(0).getSchema().getXml().getNamespace(), "http://example.com/schema/sample");
        Assert.assertEquals(petByStatusEndpoint.getGet().getParameters().get(0).getSchema().getXml().getPrefix(), "sample");
        Assert.assertEquals(petByStatusEndpoint.getGet().getParameters().get(0).getIn(),"query");
    }


    @Test(dataProvider = "data")
    public void readExternalDocsObject(JsonNode rootNode) throws Exception {
        final OpenAPIDeserializer deserializer = new OpenAPIDeserializer();
        final SwaggerParseResult result = deserializer.deserialize(rootNode);

        Assert.assertNotNull(result);

        final OpenAPI openAPI = result.getOpenAPI();
        Assert.assertNotNull(openAPI);

        final ExternalDocumentation externalDocumentation = openAPI.getExternalDocs();
        Assert.assertNotNull(externalDocumentation);
        Assert.assertNotNull(externalDocumentation.getUrl());
        Assert.assertEquals(externalDocumentation.getUrl(),"http://swagger.io");

        Assert.assertNotNull(externalDocumentation.getDescription());
        Assert.assertEquals(externalDocumentation.getDescription(),"Find out more about Swagger");

    }

    @Test(dataProvider = "data")
    public void readPathsObject(JsonNode rootNode) throws Exception {

        final OpenAPIDeserializer deserializer = new OpenAPIDeserializer();
        final SwaggerParseResult result = deserializer.deserialize(rootNode);

        Assert.assertNotNull(result);

        final OpenAPI openAPI = result.getOpenAPI();
        Assert.assertNotNull(openAPI);


        final Paths paths = openAPI.getPaths();
        Assert.assertNotNull(paths);
        Assert.assertEquals(paths.size(), 14);



        PathItem petEndpoint = paths.get("/pet");
        //System.out.println("$REF: "+ petEndpoint.getRef());
        Assert.assertNotNull(petEndpoint);
        Assert.assertEquals(petEndpoint.getSummary(),"summary");
        Assert.assertEquals(petEndpoint.getDescription(),"description");
        Assert.assertNotNull(petEndpoint.getPost().getExternalDocs());
        Assert.assertEquals(petEndpoint.getPost().getExternalDocs().getUrl(),"http://swagger.io");
        Assert.assertEquals(petEndpoint.getPost().getExternalDocs().getDescription(),"Find out more");
        Assert.assertEquals(petEndpoint.getPost().getRequestBody().getDescription(),"user to add to the system");
        Assert.assertTrue(petEndpoint.getPost().getRequestBody().getRequired(),"required");


        //Operation post
        Assert.assertNotNull(petEndpoint.getPost());
        Assert.assertNotNull(petEndpoint.getPost().getTags());
        Assert.assertEquals(petEndpoint.getPost().getTags().size(), 1);
        Assert.assertEquals(petEndpoint.getPost().getSummary(), "Add a new pet to the store");
        Assert.assertEquals(petEndpoint.getPost().getDescription(),"");
        Assert.assertEquals(petEndpoint.getPost().getOperationId(), "addPet");
        Assert.assertNotNull(petEndpoint.getServers());
        Assert.assertEquals(petEndpoint.getServers().size(), 1);
        Assert.assertNotNull(petEndpoint.getParameters());
        Assert.assertEquals(petEndpoint.getParameters().size(), 1);
        Assert.assertNotNull(petEndpoint.getPost().getParameters());
        ApiResponses responses = petEndpoint.getPost().getResponses();
        Assert.assertNotNull(responses);
        Assert.assertTrue(responses.containsKey("405"));
        ApiResponse response = responses.get("405");
        Assert.assertEquals(response.getDescription(), "Invalid input");
        //Assert.assertEquals(response.getHeaders().get("X-Rate-Limit").getDescription(), "calls per hour allowed by the user");


        //parameters operation get

        PathItem petByStatusEndpoint = paths.get("/pet/findByStatus");
        Assert.assertNotNull(petByStatusEndpoint.getGet());
        Assert.assertNotNull(petByStatusEndpoint.getGet().getTags());
        Assert.assertEquals(petByStatusEndpoint.getGet().getParameters().size(), 1);
        Assert.assertEquals(petByStatusEndpoint.getGet().getParameters().get(0).getIn(),"query");

    }



    @DataProvider(name="data")
    private Object[][] getRootNode() throws Exception {
        final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        final JsonNode rootNode = mapper.readTree(Files.readAllBytes(java.nio.file.Paths.get(getClass().getResource("/oas3.yaml").toURI())));
        return new Object[][]{new Object[]{rootNode}};
    }



}
