package io.swagger.v3.parser.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.parser.core.models.AuthorizationValue;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import io.swagger.v3.parser.OpenAPIV3Parser;
import mockit.Injectable;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.testng.Assert.assertEquals;

import static org.testng.Assert.assertTrue;

public class OpenAPIDeserializerTest {

    @Test
    public void testAllOfSchema(@Injectable List<AuthorizationValue> auths){
        String yaml = "openapi: '3.0'\n" +
            "components:\n" +
            "  schemas:\n" +
            "    Pet:\n" +
            "      type: object\n" +
            "      required:\n" +
            "      - pet_type\n" +
            "      properties:\n" +
            "        pet_type:\n" +
            "          type: string\n" +
            "    Cat:\n" +
            "      allOf:\n" +
            "      - $ref: '#/components/schemas/Pet'\n" +
            "      - type: object\n" +
            "        # all other properties specific to a `Cat`\n" +
            "        properties:\n" +
            "          name:\n" +
            "            type: string\n";
  
        OpenAPIV3Parser parser = new OpenAPIV3Parser();
  
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
  
        SwaggerParseResult result = parser.readContents(yaml,auths,options);
        List<String> messageList = result.getMessages();
        Set<String> messages = new HashSet<>(messageList);
  
        Schema catSchema = result.getOpenAPI().getComponents().getSchemas().get("Cat");
        assertTrue(catSchema != null);
        assertTrue(catSchema instanceof ComposedSchema);
        
        ComposedSchema catCompSchema = (ComposedSchema) catSchema;
        List<Schema> allOfSchemas = catCompSchema.getAllOf();
        assertTrue(allOfSchemas != null);
        assertEquals(allOfSchemas.size(), 2);
        
        Schema refPetSchema = allOfSchemas.get(0);
        assertTrue(refPetSchema != null);
        assertEquals(refPetSchema.get$ref(), "#/components/schemas/Pet");
  
        Schema otherSchema = allOfSchemas.get(1);
        assertTrue(otherSchema != null);
        assertTrue(otherSchema.getProperties() != null);
        Schema nameProp = (Schema) otherSchema.getProperties().get("name");
        assertTrue(nameProp != null);
        assertEquals(nameProp.getType(), "string");
        
    }  
  
    @Test
    public void testOneOfSchema(@Injectable List<AuthorizationValue> auths){
        String yaml = "openapi: '3.0'\n" +
            "components:\n" +
            "  schemas:\n" +
            "    Cat:\n" +
            "      type: object\n" +
            "      # all properties specific to a `Cat`\n" +
            "      properties:\n" +
            "        purring:\n" +
            "          type: string\n" +
            "    Dog:\n" +
            "      type: object\n" +
            "      # all properties specific to a `Dog`\n" +
            "      properties:\n" +
            "        bark:\n" +
            "          type: string\n" +
            "    Pet:\n" +
            "      oneOf: \n" +
            "       - $ref: '#/components/schemas/Cat'\n" +      
            "       - $ref: '#/components/schemas/Dog'\n" +
            "       - type: object\n" +
            "         # neither a `Cat` nor a `Dog`\n" +
            "         properties:\n" +
            "           name:\n" +
            "             type: string\n" ;
  
        OpenAPIV3Parser parser = new OpenAPIV3Parser();
  
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
  
        SwaggerParseResult result = parser.readContents(yaml,auths,options);
        List<String> messageList = result.getMessages();
        Set<String> messages = new HashSet<>(messageList);
  
        Schema petSchema = result.getOpenAPI().getComponents().getSchemas().get("Pet");
        assertTrue(petSchema != null);
        assertTrue(petSchema instanceof ComposedSchema);
        
        ComposedSchema petCompSchema = (ComposedSchema) petSchema;
        List<Schema> oneOfSchemas = petCompSchema.getOneOf();
        assertTrue(oneOfSchemas != null);
        assertEquals(oneOfSchemas.size(), 3);
        
        Schema refCatSchema = oneOfSchemas.get(0);
        assertTrue(refCatSchema != null);
        assertEquals(refCatSchema.get$ref(), "#/components/schemas/Cat");
  
        Schema refDogSchema = oneOfSchemas.get(1);
        assertTrue(refDogSchema != null);
        assertEquals(refDogSchema.get$ref(), "#/components/schemas/Dog");
        
        Schema otherSchema = oneOfSchemas.get(2);
        assertTrue(otherSchema != null);
        Schema nameProp = (Schema) otherSchema.getProperties().get("name");
        assertTrue(nameProp != null);
        assertEquals(nameProp.getType(), "string");
        
    }  

    @Test
    public void testAnyOfSchema(@Injectable List<AuthorizationValue> auths){
        String yaml = "openapi: '3.0'\n" +
            "components:\n" +
            "  schemas:\n" +
            "    id:\n" +
            "      anyOf: \n" +
            "       - type: string\n" +      
            "       - type: number\n" ;
  
        OpenAPIV3Parser parser = new OpenAPIV3Parser();
  
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
  
        SwaggerParseResult result = parser.readContents(yaml,auths,options);
        List<String> messageList = result.getMessages();
        Set<String> messages = new HashSet<>(messageList);
  
        Schema idSchema = result.getOpenAPI().getComponents().getSchemas().get("id");
        assertTrue(idSchema != null);
        assertTrue(idSchema instanceof ComposedSchema);
        
        ComposedSchema idCompSchema = (ComposedSchema) idSchema;
        List<Schema> anyOfSchemas = idCompSchema.getAnyOf();
        assertTrue(anyOfSchemas != null);
        assertEquals(anyOfSchemas.size(), 2);
        
        Schema stringSchema = anyOfSchemas.get(0);
        assertTrue(stringSchema != null);
        assertEquals(stringSchema.getType(), "string");
  
        Schema numberSchema = anyOfSchemas.get(1);
        assertTrue(numberSchema != null);
        assertEquals(numberSchema.getType(), "number");
        
    }  
    
    @Test
    public void propertyTest(@Injectable List<AuthorizationValue> auths){
        String yaml = "openapi: 3.0.0\n"+
                        "paths:\n"+
                        "  /primitiveBody/inline:\n" +
                        "    post:\n" +
                        "      x-swagger-router-controller: TestController\n" +
                        "      operationId: inlineRequiredBody\n" +
                        "      requestBody:\n" +
                        "        content:\n" +
                        "          application/json:\n" +
                        "            schema:\n" +
                        "              type: object\n" +
                        "              properties:\n" +
                        "                name:\n" +
                        "                  type: string\n" +
                        "        required: true\n" +
                        "      responses:\n" +
                        "        '200':\n" +
                        "          description: ok!";





        OpenAPIV3Parser parser = new OpenAPIV3Parser();

        ParseOptions options = new ParseOptions();
        options.setResolve(true);

        SwaggerParseResult result = parser.readContents(yaml,auths,options);
        OpenAPI openAPI = result.getOpenAPI();
        Map<String,Schema> properties = openAPI.getPaths().get("/primitiveBody/inline").getPost().getRequestBody().getContent().get("application/json").getSchema().getProperties();

        assertTrue(properties.get("name") instanceof StringSchema);


    }

    @Test
    public void testExamples(@Injectable List<AuthorizationValue> auths){
        String yaml = "openapi: 3.0.0\n"+
                        "info:\n"+
                        "  title: httpbin\n"+
                        "  version: 0.0.0\n"+
                        "servers:\n"+
                        "  - url: http://httpbin.org\n"+
                        "paths:\n"+
                        "  /post:\n"+
                        "    post:\n"+
                        "      summary: Returns the POSTed data\n"+
                        "      requestBody:\n"+
                        "        content:\n"+
                        "          application/json:\n"+
                        "            schema:\n"+
                        "              $ref: '#/components/schemas/AnyValue'\n"+
                        "            examples:\n"+
                        "              AnObject:\n"+
                        "                $ref: '#/components/examples/AnObject'\n"+
                        "              ANull:\n"+
                        "                $ref: '#/components/examples/ANull'\n"+
                        "          application/yaml:\n"+
                        "            schema:\n"+
                        "              $ref: '#/components/schemas/AnyValue'\n"+
                        "            examples:\n"+
                        "              AString:\n"+
                        "                $ref: '#/components/examples/AString'\n"+
                        "              AnArray:\n"+
                        "                $ref: '#/components/examples/AnArray'\n"+
                        "          text/plain:\n"+
                        "            schema:\n"+
                        "              type: string\n"+
                        "              example: Hi there\n"+
                        "          application/x-www-form-urlencoded:\n"+
                        "            schema:\n"+
                        "              type: object\n"+
                        "              properties:\n"+
                        "                id:\n"+
                        "                  type: integer\n"+
                        "                name:\n"+
                        "                  type: string\n"+
                        "            example:\n"+
                        "              id: 42\n"+
                        "              name: Arthur Dent\n"+
                        "      responses:\n"+
                        "        '200':\n"+
                        "          description: OK\n"+
                        "          content:\n"+
                        "            application/json:\n"+
                        "              schema:\n"+
                        "                type: object\n"+
                        "\n"+
                        "  #/response-headers:\n"+
                        "  /:\n"+
                        "    get:\n"+
                        "      summary: Returns a response with the specified headers\n"+
                        "      parameters:\n"+
                        "        - in: header\n"+
                        "          name: Server\n"+
                        "          required: true\n"+
                        "          schema:\n"+
                        "            type: string\n"+
                        "          examples:\n"+
                        "            httpbin:\n"+
                        "              value: httpbin\n"+
                        "            unicorn:\n"+
                        "              value: unicorn\n"+
                        "        - in: header\n"+
                        "          name: X-Request-Id\n"+
                        "          required: true\n"+
                        "          schema:\n"+
                        "            type: integer\n"+
                        "          example: 37\n"+
                        "      responses:\n"+
                        "        '200':\n"+
                        "          description: A response with the specified headers\n"+
                        "          headers:\n"+
                        "            Server:\n"+
                        "              schema:\n"+
                        "                type: string\n"+
                        "              examples:\n"+
                        "                httpbin:\n"+
                        "                  value: httpbin\n"+
                        "                unicorn:\n"+
                        "                  value: unicorn\n"+
                        "            X-Request-Id:\n"+
                        "              schema:\n"+
                        "                type: integer\n"+
                        "              example: 37\n"+
                        "\n"+
                        "components:\n"+
                        "  schemas:\n"+
                        "    AnyValue:\n"+
                        "      nullable: true\n"+
                        "      description: Can be anything - string, object, array, null, etc.\n"+
                        "\n"+
                        "  examples:\n"+
                        "    AString:\n"+
                        "      value: Hi there\n"+
                        "    ANumber:\n"+
                        "      value: 42\n"+
                        "    ANull:\n"+
                        "      value: null\n"+
                        "    AnArray:\n"+
                        "      value: [1, 2, 3]\n"+
                        "    AnObject:\n"+
                        "      value:\n"+
                        "        id:  42\n"+
                        "        name: Arthur Dent";

        OpenAPIV3Parser parser = new OpenAPIV3Parser();

        ParseOptions options = new ParseOptions();
        options.setResolve(true);

        SwaggerParseResult result = parser.readContents(yaml,auths,options);
        OpenAPI openAPI = result.getOpenAPI();
        MediaType mediaTypeJson = openAPI.getPaths().get("/post").getPost().getRequestBody().getContent().get("application/json");
        Header header1 = openAPI.getPaths().get("/").getGet().getResponses().get("200").getHeaders().get("Server");
        Header header2 = openAPI.getPaths().get("/").getGet().getResponses().get("200").getHeaders().get("X-Request-Id");
        Parameter parameter1 = openAPI.getPaths().get("/").getGet().getParameters().get(0);
        Parameter parameter2 = openAPI.getPaths().get("/").getGet().getParameters().get(1);


        Assert.assertNotNull(mediaTypeJson.getExamples());
        Assert.assertEquals(mediaTypeJson.getExamples().get("AnObject").get$ref(),"#/components/examples/AnObject");

        Assert.assertNotNull(header1.getExamples());
        Assert.assertEquals(header1.getExamples().get("httpbin").getValue(),"httpbin");

        Assert.assertNotNull(header2.getExample());
        Assert.assertEquals(header2.getExample(),37);

        Assert.assertNotNull(parameter1.getExamples());
        Assert.assertEquals(parameter1.getExamples().get("unicorn").getValue(),"unicorn");

        Assert.assertNotNull(parameter2.getExample());
        Assert.assertEquals(parameter2.getExample(),37);
    }


    @Test
    public void testSchemaExample(@Injectable List<AuthorizationValue> auths){
        String yaml = "openapi: '3.0.0'\n" +
                "components:\n" +
                "  schemas:\n"+
                "    Address:\n" +
                "      required:\n" +
                "      - street\n" +
                "      type: object\n" +
                "      x-swagger-router-model: io.swagger.oas.test.models.Address\n" +
                "      properties:\n" +
                "        street:\n" +
                "          type: string\n" +
                "          example: 12345 El Monte Road\n" +
                "        city:\n" +
                "          type: string\n" +
                "          example: Los Altos Hills\n" +
                "        state:\n" +
                "          type: string\n" +
                "          example: CA\n" +
                "        zip:\n" +
                "          type: string\n" +
                "          example: '94022'";

        OpenAPIV3Parser parser = new OpenAPIV3Parser();

        ParseOptions options = new ParseOptions();
        options.setResolve(true);

        SwaggerParseResult result = parser.readContents(yaml,auths,options);
        OpenAPI openAPI = result.getOpenAPI();
        Schema stateSchemaProperty = (Schema)openAPI.getComponents().getSchemas().get("Address").getProperties().get("state");

        Assert.assertNotNull(stateSchemaProperty.getExample());
        Assert.assertEquals(stateSchemaProperty.getExample(),"CA" );
    }

    @Test
    public void testOptionalParameter(@Injectable List<AuthorizationValue> auths) {
        String yaml = "openapi: 3.0.0\n" +
                "paths:\n" +
                "  \"/pet\":\n" +
                "    summary: summary\n" +
                "    description: description\n" +
                "    post:\n" +
                "      summary: Add a new pet to the store\n" +
                "      description: ''\n" +
                "      operationId: addPet\n" +
                "      parameters:\n" +
                "      - name: status\n" +
                "        in: query\n" +
                "        description: Status values that need to be considered for filter\n" +
                "        schema:\n" +
                "          type: array\n" +
                "          items:\n" +
                "            type: string\n" +
                "            enum:\n" +
                "            - available\n" +
                "            - pending\n" +
                "            - sold\n" +
                "          default: available";
        OpenAPIV3Parser parser = new OpenAPIV3Parser();

        ParseOptions options = new ParseOptions();
        options.setResolve(true);

        SwaggerParseResult result = parser.readContents(yaml,auths,options);
        OpenAPI openAPI = result.getOpenAPI();
        Parameter parameter = openAPI.getPaths().get("/pet").getPost().getParameters().get(0);

        Assert.assertFalse(parameter.getRequired());
    }

    @Test void testDiscriminatorObject(@Injectable List<AuthorizationValue> auths){
        String yaml = "openapi: '3.0.0'\n" +
                "components:\n" +
                "  schemas:\n" +
                "    Pet:\n" +
                "      type: object\n" +
                "      required:\n" +
                "      - pet_type\n" +
                "      properties:\n" +
                "        pet_type:\n" +
                "          type: string\n" +
                "      discriminator:\n" +
                "        propertyName: pet_type\n" +
                "        mapping:\n" +
                "          cachorro: Dog\n" +
                "    Cat:\n" +
                "      allOf:\n" +
                "      - $ref: '#/components/schemas/Pet'\n" +
                "      - type: object\n" +
                "        # all other properties specific to a `Cat`\n" +
                "        properties:\n" +
                "          name:\n" +
                "            type: string\n" +
                "    Dog:\n" +
                "      allOf:\n" +
                "      - $ref: '#/components/schemas/Pet'\n" +
                "      - type: object\n" +
                "        # all other properties specific to a `Dog`\n" +
                "        properties:\n" +
                "          bark:\n" +
                "            type: string\n" +
                "    Lizard:\n" +
                "      allOf:\n" +
                "      - $ref: '#/components/schemas/Pet'\n" +
                "      - type: object\n" +
                "        # all other properties specific to a `Lizard`\n" +
                "        properties:\n" +
                "          lovesRocks:\n" +
                "            type: boolean";

        OpenAPIV3Parser parser = new OpenAPIV3Parser();

        ParseOptions options = new ParseOptions();
        options.setResolve(true);

        SwaggerParseResult result = parser.readContents(yaml,auths,options);
        List<String> messageList = result.getMessages();
        Set<String> messages = new HashSet<>(messageList);

        assertEquals(result.getOpenAPI().getComponents().getSchemas().get("Pet").getDiscriminator().getPropertyName(),"pet_type");
        assertEquals(result.getOpenAPI().getComponents().getSchemas().get("Pet").getDiscriminator().getMapping().get("cachorro"),"Dog" );
        assertTrue(messages.contains("attribute paths is missing"));
        assertTrue(messages.contains("attribute info is missing"));

    }

    @Test
    public void testEmpty(@Injectable List<AuthorizationValue> auths) {
        String json = "{}";

        OpenAPIV3Parser parser = new OpenAPIV3Parser();

        ParseOptions options = new ParseOptions();
        options.setResolve(true);

        SwaggerParseResult result = parser.readContents(json,auths,options);
        List<String> messageList = result.getMessages();
        Set<String> messages = new HashSet<>(messageList);

        assertTrue(messages.contains("attribute openapi is missing"));
    }

    @Test
    public void testAlmostEmpty(@Injectable List<AuthorizationValue> auths) {
        String yaml = "openapi: '3.0.0'\n" +
                      "new: extra";

        OpenAPIV3Parser parser = new OpenAPIV3Parser();

        ParseOptions options = new ParseOptions();
        options.setResolve(true);

        SwaggerParseResult result = parser.readContents(yaml,auths,options);
        List<String> messageList = result.getMessages();
        Set<String> messages = new HashSet<>(messageList);

        assertTrue(messages.contains("attribute info is missing"));
        assertTrue(messages.contains("attribute paths is missing"));
        assertTrue(messages.contains("attribute components is missing"));
        assertTrue(messages.contains("attribute new is unexpected"));
    }

    @Test(dataProvider = "data")
    public void readInfoObject(JsonNode rootNode) throws Exception {


        final OpenAPIDeserializer deserializer = new OpenAPIDeserializer();
        final SwaggerParseResult result = deserializer.deserialize(rootNode);

        Assert.assertNotNull(result);

        final OpenAPI openAPI = result.getOpenAPI();
        Assert.assertNotNull(openAPI);
        Assert.assertEquals(openAPI.getOpenapi(),"3.0.0");


        final Info info = openAPI.getInfo();
        Assert.assertNotNull(info);
        Assert.assertEquals(info.getTitle(), "Sample Pet Store App");
        Assert.assertEquals(info.getDescription(), "This is a sample server Petstore");
        Assert.assertEquals(info.getTermsOfService(), "http://swagger.io/terms/");
        Assert.assertNotNull(info.getExtensions().get("x-info"));
        Assert.assertEquals(info.getExtensions().get("x-info").toString(),"info extension");

        final Contact contact = info.getContact();
        Assert.assertNotNull(contact);
        Assert.assertEquals(contact.getName(),"API Support");
        Assert.assertEquals(contact.getUrl(),"http://www.example.com/support");
        Assert.assertEquals(contact.getEmail(),"support@example.com");
        Assert.assertNotNull(contact.getExtensions().get("x-contact"));
        Assert.assertEquals(contact.getExtensions().get("x-contact").toString(),"contact extension");

        final License license = info.getLicense();
        Assert.assertNotNull(license);
        Assert.assertEquals(license.getName(), "Apache 2.0");
        Assert.assertEquals(license.getUrl(), "http://www.apache.org/licenses/LICENSE-2.0.html");
        Assert.assertNotNull(license.getExtensions());

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
        Assert.assertNotNull(server.get(2).getExtensions().get("x-server"));
        Assert.assertEquals(server.get(2).getExtensions().get("x-server").toString(),"server extension");
        Assert.assertEquals(server.get(2).getVariables().get("basePath").getDescription(),"testing overwriting");
        Assert.assertEquals(server.get(2).getVariables().get("basePath").getDefault(),"v2");


    }

    @Test
    public void readMissingServerObject() throws Exception {
        final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        final JsonNode rootNode = mapper.readTree(Files.readAllBytes(java.nio.file.Paths.get(getClass().getResource("/oas.yaml").toURI())));

        final OpenAPIDeserializer deserializer = new OpenAPIDeserializer();
        final SwaggerParseResult result = deserializer.deserialize(rootNode);

        Assert.assertNotNull(result);

        final OpenAPI openAPI = result.getOpenAPI();
        Assert.assertNotNull(openAPI);

        assertEquals(openAPI.getServers().get(0).getUrl(),"/");
    }

    @Test
    public void readEmptyServerObject() throws Exception {
        final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        final JsonNode rootNode = mapper.readTree(Files.readAllBytes(java.nio.file.Paths.get(getClass().getResource("/oas2.yaml.template").toURI())));

        final OpenAPIDeserializer deserializer = new OpenAPIDeserializer();
        final SwaggerParseResult result = deserializer.deserialize(rootNode);

        Assert.assertNotNull(result);

        final OpenAPI openAPI = result.getOpenAPI();
        Assert.assertNotNull(openAPI);

        assertEquals(openAPI.getServers().get(0).getUrl(),"/");
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

        Assert.assertEquals(petByStatusEndpoint.getGet().getParameters().get(0).getContent().get("application/xml").getExamples().get("list").getSummary(),"List of names");
        Assert.assertEquals(petByStatusEndpoint.getGet().getParameters().get(0).getContent().get("application/xml").getExamples().get("list").getValue(),"<Users><User name='Bob'/><User name='Diane'/><User name='Mary'/><User name='Bill'/></Users>");

        Assert.assertNotNull(petByStatusEndpoint.getGet().getParameters().get(0).getContent().get("application/xml").getExamples().get("empty").getSummary());
        Assert.assertEquals(petByStatusEndpoint.getGet().getParameters().get(0).getContent().get("application/xml").getExamples().get("empty").getSummary(),"Empty list");
        Assert.assertEquals(petByStatusEndpoint.getGet().getParameters().get(0).getContent().get("application/xml").getExamples().get("empty").getValue(),"<Users/>");


        Assert.assertEquals(petByStatusEndpoint.getGet().getParameters().get(0).getContent().get("text/plain").getExamples().get("list").getSummary(),"List of names");
        Assert.assertEquals(petByStatusEndpoint.getGet().getParameters().get(0).getContent().get("text/plain").getExamples().get("list").getValue(),"Bob,Diane,Mary,Bill");
        Assert.assertEquals(petByStatusEndpoint.getGet().getParameters().get(0).getContent().get("text/plain").getExamples().get("empty").getSummary(),"Empty");
        Assert.assertNull(petByStatusEndpoint.getGet().getParameters().get(0).getContent().get("text/plain").getExamples().get("empty").getValue());

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
        assertTrue(petByStatusEndpoint.getGet().getRequestBody().getRequired(),"true");
        Assert.assertNotNull(petByStatusEndpoint.getGet().getRequestBody().getContent().get("multipart/mixed"));
        Assert.assertEquals(petByStatusEndpoint.getGet().getRequestBody().getContent().get("multipart/mixed").getSchema().getType(),"object");
        Assert.assertNotNull(petByStatusEndpoint.getGet().getRequestBody().getContent().get("multipart/mixed").getSchema().getProperties());


        Assert.assertEquals(petByStatusEndpoint.getGet().getRequestBody().getContent().get("multipart/mixed").getEncoding().get("historyMetadata").getContentType(),"application/xml; charset=utf-8");
        Assert.assertNotNull(petByStatusEndpoint.getGet().getRequestBody().getContent().get("multipart/mixed").getEncoding().get("profileImage").getHeaders());
        Assert.assertNotNull(petByStatusEndpoint.getGet().getRequestBody().getContent().get("multipart/mixed").getEncoding().get("profileImage").getHeaders().get("X-Rate-Limit"));
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
        assertTrue(requirement.containsKey("api_key"));

        requirement = requirements.get(1);
        assertTrue(requirement.containsKey("tokenAuth"));


    }

    @Test(dataProvider = "data")
    public void readSecuritySchemesObject(JsonNode rootNode) throws Exception {
        final OpenAPIDeserializer deserializer = new OpenAPIDeserializer();
        final SwaggerParseResult result = deserializer.deserialize(rootNode);

        Assert.assertNotNull(result);

        List<String> messages = result.getMessages();
        assertTrue(!messages.contains("attribute components.securitySchemes'.petstore_auth'.name is missing"));
        assertTrue(!messages.contains("attribute components.securitySchemes'.petstore_auth'.in is missing"));
        assertTrue(!messages.contains("attribute components.securitySchemes'.petstore_auth'.scheme is missing"));
        assertTrue(!messages.contains("attribute components.securitySchemes'.petstore_auth'.openIdConnectUrl is missing"));
        
        assertTrue(!messages.contains("attribute components.securitySchemes'.petstore_auth'.tokenUrl is missing"));
        assertTrue(!messages.contains("attribute components.securitySchemes'.petstore_auth_password'.authorizationUrl is missing"));
        assertTrue(!messages.contains("attribute components.securitySchemes'.petstore_auth_clientCredentials'.authorizationUrl is missing"));
        
        assertTrue(!messages.contains("attribute components.securitySchemes'.api_key'.scheme is missing"));
        assertTrue(!messages.contains("attribute components.securitySchemes'.api_key'.flows is missing"));
        assertTrue(!messages.contains("attribute components.securitySchemes'.api_key'.openIdConnectUrl is missing"));
        
        assertTrue(!messages.contains("attribute components.securitySchemes'.http'.name is missing"));
        assertTrue(!messages.contains("attribute components.securitySchemes'.http'.in is missing"));
        assertTrue(!messages.contains("attribute components.securitySchemes'.http'.flows is missing"));
        assertTrue(!messages.contains("attribute components.securitySchemes'.http'.openIdConnectUrl is missing"));
        
        assertTrue(!messages.contains("attribute components.securitySchemes'.openID'.name is missing"));
        assertTrue(!messages.contains("attribute components.securitySchemes'.openID'.in is missing"));
        assertTrue(!messages.contains("attribute components.securitySchemes'.openID'.scheme is missing"));
        assertTrue(!messages.contains("attribute components.securitySchemes'.openID'.flows is missing"));
        
        
        
        final OpenAPI openAPI = result.getOpenAPI();
        Assert.assertNotNull(openAPI);

        final Map<String, SecurityScheme> securitySchemes = openAPI.getComponents().getSecuritySchemes();
        Assert.assertNotNull(securitySchemes);
        Assert.assertEquals(securitySchemes.size(),9);

        SecurityScheme securityScheme = securitySchemes.get("reference");
        assertTrue(securityScheme.get$ref().equals("#/components/securitySchemes/api_key"));

        securityScheme = securitySchemes.get("remote_reference");
        assertTrue(securityScheme.get$ref().equals("http://localhost:${dynamicPort}/remote/security#/petstore_remote"));
        
        securityScheme = securitySchemes.get("petstore_auth");
        assertTrue(securityScheme.getType()== SecurityScheme.Type.OAUTH2);
        
        securityScheme = securitySchemes.get("petstore_auth_password");
        assertTrue(securityScheme.getType()== SecurityScheme.Type.OAUTH2);
        
        securityScheme = securitySchemes.get("petstore_auth_clientCredentials");
        assertTrue(securityScheme.getType()== SecurityScheme.Type.OAUTH2);
        
        securityScheme = securitySchemes.get("petstore_auth_authorizationCode");
        assertTrue(securityScheme.getType()== SecurityScheme.Type.OAUTH2);
        
        securityScheme = securitySchemes.get("api_key");
        assertTrue(securityScheme.getType()== SecurityScheme.Type.APIKEY);
        
        securityScheme = securitySchemes.get("http");
        assertTrue(securityScheme.getType()== SecurityScheme.Type.HTTP);

        securityScheme = securitySchemes.get("openID");
        assertTrue(securityScheme.getType()== SecurityScheme.Type.OPENIDCONNECT);
    }
    
    @Test(dataProvider = "data")
    public void readExtensions(JsonNode rootNode) throws Exception {
        final OpenAPIDeserializer deserializer = new OpenAPIDeserializer();
        final SwaggerParseResult result = deserializer.deserialize(rootNode);

        Assert.assertNotNull(result);

        final OpenAPI openAPI = result.getOpenAPI();
        Assert.assertNotNull(openAPI);
        Assert.assertNotNull(openAPI.getExtensions());
        assertTrue(openAPI.getExtensions().containsKey("x-origin"));
        Object object = openAPI.getExtensions().get("x-origin");

        assertTrue(object instanceof List);
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
        assertTrue(object instanceof String);
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
        Assert.assertEquals(paths.size(), 18);

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
        Assert.assertEquals(paths.size(), 18);

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
    public void readProducesTestEndpoint(JsonNode rootNode) throws Exception {
        final OpenAPIDeserializer deserializer = new OpenAPIDeserializer();
        final SwaggerParseResult result = deserializer.deserialize(rootNode);

        Assert.assertNotNull(result);

        final OpenAPI openAPI = result.getOpenAPI();
        Assert.assertNotNull(openAPI);

        final Paths paths = openAPI.getPaths();
        Assert.assertNotNull(paths);
        Assert.assertEquals(paths.size(), 18);

        //parameters operation get
        PathItem producesTestEndpoint = paths.get("/producesTest");
        Assert.assertNotNull(producesTestEndpoint.getGet());
        Assert.assertNotNull(producesTestEndpoint.getGet().getParameters());
        Assert.assertTrue(producesTestEndpoint.getGet().getParameters().isEmpty());

        Operation operation = producesTestEndpoint.getGet();
        ApiResponses responses = operation.getResponses();
        Assert.assertNotNull(responses);
        Assert.assertFalse(responses.isEmpty());

        ApiResponse response = responses.get("200");
        Assert.assertNotNull(response);
        Assert.assertEquals("it works", response.getDescription());

        Content content = response.getContent();
        Assert.assertNotNull(content);
        MediaType mediaType = content.get("application/json");
        Assert.assertNotNull(mediaType);

        Schema schema = mediaType.getSchema();
        Assert.assertNotNull(schema);
        Assert.assertTrue(schema instanceof ObjectSchema);

        ObjectSchema objectSchema = (ObjectSchema) schema;
        schema = objectSchema.getProperties().get("name");
        Assert.assertNotNull(schema);

        Assert.assertTrue(schema instanceof StringSchema);
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
        //System.out.println(openAPI);

        final Paths paths = openAPI.getPaths();
        Assert.assertNotNull(paths);
        Assert.assertEquals(paths.size(), 18);


        PathItem petRef = paths.get("/pathItemRef");

        PathItem petEndpoint = paths.get("/pet");
        Assert.assertNotNull(petEndpoint);
        Assert.assertEquals(petEndpoint.getSummary(),"summary");
        Assert.assertEquals(petEndpoint.getDescription(),"description");
        Assert.assertNotNull(petEndpoint.getPost().getExternalDocs());
        Assert.assertEquals(petEndpoint.getPost().getExternalDocs().getUrl(),"http://swagger.io");
        Assert.assertEquals(petEndpoint.getPost().getExternalDocs().getDescription(),"Find out more");


        //Operation post
        Assert.assertNotNull(petEndpoint.getPost());
        Assert.assertNotNull(petEndpoint.getPost().getTags());
        Assert.assertEquals(petEndpoint.getPost().getTags().size(), 1);
        Assert.assertEquals(petEndpoint.getPost().getSummary(), "Add a new pet to the store");
        Assert.assertNull(petEndpoint.getPost().getDescription());
        Assert.assertEquals(petEndpoint.getPost().getOperationId(), "addPet");
        Assert.assertNotNull(petEndpoint.getServers());
        Assert.assertEquals(petEndpoint.getServers().size(), 1);
        Assert.assertNotNull(petEndpoint.getParameters());
        Assert.assertEquals(petEndpoint.getParameters().size(), 2);
        Assert.assertNotNull(petEndpoint.getPost().getParameters());
        Parameter parameter = petEndpoint.getParameters().get(0);
        Assert.assertEquals(petEndpoint.getPost().getSecurity().get(0).get("petstore_auth").get(0), "write:pets");
        Assert.assertEquals(petEndpoint.getPost().getSecurity().get(0).get("petstore_auth").get(1), "read:pets");

        ApiResponses responses = petEndpoint.getPost().getResponses();
        Assert.assertNotNull(responses);
        assertTrue(responses.containsKey("405"));
        ApiResponse response = responses.get("405");
        Assert.assertEquals(response.getDescription(), "Invalid input");
        Assert.assertEquals(response.getHeaders().get("X-Rate-Limit").getDescription(), "calls per hour allowed by the user");


        //parameters operation get

        PathItem petByStatusEndpoint = paths.get("/pet/findByStatus");
        Assert.assertNotNull(petByStatusEndpoint.getGet());
        Assert.assertNotNull(petByStatusEndpoint.getGet().getTags());
        Assert.assertEquals(petByStatusEndpoint.getGet().getParameters().size(), 1);
        Assert.assertEquals(petByStatusEndpoint.getGet().getParameters().get(0).getIn(),"query");
        Assert.assertEquals(petByStatusEndpoint.getGet().getCallbacks().get("mainHook").get("$request.body#/url").getPost().getResponses().get("200").getDescription(),"webhook successfully processed operation");

    }

    @Test(dataProvider = "data")
    public void readComponentsObject(JsonNode rootNode) throws Exception {


        final OpenAPIDeserializer deserializer = new OpenAPIDeserializer();
        final SwaggerParseResult result = deserializer.deserialize(rootNode);

        Assert.assertNotNull(result);

        final OpenAPI openAPI = result.getOpenAPI();
        Assert.assertNotNull(openAPI);
        Assert.assertEquals(openAPI.getOpenapi(),"3.0.0");

        final Components component = openAPI.getComponents();
        Assert.assertNotNull(component);
        Assert.assertNotNull(component.getCallbacks());
        Assert.assertEquals(component.getCallbacks().get("heartbeat").get("$request.query.heartbeat-url").getPost().getResponses().get("200").getDescription(),"Consumer acknowledged the callback");
        //System.out.println(component.getCallbacks().get("referenced"));
        Assert.assertEquals(component.getCallbacks().get("failed").get("$response.body#/failedUrl").getPost().getResponses().get("200").getDescription(),"Consumer acknowledged the callback failed");

        Assert.assertNotNull(component.getExamples());
        Assert.assertEquals(component.getExamples().get("cat").getSummary(),"An example of a cat");
        Assert.assertNotNull(component.getExamples().get("cat").getValue());


        Assert.assertNotNull(component.getHeaders());
        Assert.assertEquals(component.getHeaders().get("X-Rate-Limit-Limit").getDescription(),"The number of allowed requests in the current period");
        Assert.assertEquals(component.getHeaders().get("X-Rate-Limit-Limit").getSchema().getType(),"integer");


        Assert.assertNotNull(component.getLinks());
        Assert.assertEquals(component.getLinks().get("unsubscribe").getOperationId(),"cancelHookCallback");
        Assert.assertNotNull(component.getLinks().get("unsubscribe").getParameters());
        Assert.assertEquals(component.getLinks().get("unsubscribe").getExtensions().get("x-link"), "link extension");

        Assert.assertNotNull(component.getParameters());
        Assert.assertEquals(component.getParameters().get("skipParam").getName(),"skip");
        Assert.assertEquals(component.getParameters().get("skipParam").getIn(),"query");
        Assert.assertEquals(component.getParameters().get("skipParam").getDescription(),"number of items to skip");
        assertTrue(component.getParameters().get("skipParam").getRequired());
        Assert.assertEquals(component.getParameters().get("skipParam").getSchema().getType(),"integer");

        Assert.assertNotNull(component.getRequestBodies());
        Assert.assertEquals(component.getRequestBodies().get("requestBody1").getDescription(),"request body in components");
        Assert.assertEquals(component.getRequestBodies().get("requestBody1").getContent().get("application/json").getSchema().get$ref(),"#/components/schemas/Pet");
        Assert.assertEquals(component.getRequestBodies().get("requestBody1").getContent().get("application/xml").getSchema().get$ref(),"#/components/schemas/Pet");
        Assert.assertEquals(component.getRequestBodies().get("requestBody2").getContent().get("application/json").getSchema().getType().toString(),"array");
        Assert.assertNotNull(component.getRequestBodies().get("requestBody2").getContent().get("application/json").getSchema());

        Assert.assertNotNull(component.getResponses());
        Assert.assertEquals(component.getResponses().get("NotFound").getDescription(),"Entity not found.");
        Assert.assertEquals(component.getResponses().get("IllegalInput").getDescription(),"Illegal input for operation.");
        Assert.assertEquals(component.getResponses().get("GeneralError").getDescription(),"General Error");
        Assert.assertEquals(component.getResponses().get("GeneralError").getContent().get("application/json").getSchema().get$ref(),"#/components/schemas/ExtendedErrorModel");


        Assert.assertNotNull(component.getSchemas());
        Assert.assertEquals(component.getSchemas().get("Category").getType(),"object");
        Assert.assertEquals(component.getSchemas().get("ApiResponse").getRequired().get(0),"name");
        Assert.assertEquals(component.getSchemas().get("Order").getType(),"object");
        Assert.assertEquals(component.getSchemas().get("Order").getNot().getType(),"integer");
        Assert.assertEquals(component.getSchemas().get("Order").getAdditionalProperties().getType(),"integer");

        Schema schema = (Schema) component.getSchemas().get("Order").getProperties().get("status");

        Map<String, Schema> properties = (Map<String, Schema>) component.getSchemas().get("Order").getProperties();

        Assert.assertNotNull(properties);

        Assert.assertEquals(properties.get("status").getType(),"string");
        Assert.assertEquals(properties.get("status").getDescription(), "Order Status");
        Assert.assertEquals(properties.get("status").getEnum().get(0), "placed");


        Assert.assertNotNull(component.getSecuritySchemes());
        Assert.assertEquals(component.getSecuritySchemes().get("petstore_auth").getType().toString(), "oauth2");
        Assert.assertEquals(component.getSecuritySchemes().get("petstore_auth").getFlows().getImplicit().getAuthorizationUrl(), "http://petstore.swagger.io/oauth/dialog");
        Assert.assertNotNull(component.getSecuritySchemes().get("petstore_auth").getFlows().getImplicit().getScopes());//TODO

        Assert.assertNotNull(component.getExtensions());
        assertTrue(component.getExtensions().containsKey("x-component"));
        Object object = component.getExtensions().get("x-component");

        assertTrue(object instanceof List);
        List elements = (List) object;
        Assert.assertEquals(elements.size(), 1);
        Map<String, Object> map = (Map) elements.get(0);
        Assert.assertEquals(map.get("url"), "http://component.swagger.io/v2/swagger.json");
        Assert.assertEquals(map.get("format"), "OAS");
        Assert.assertEquals(map.get("version"), "3.0");

        Map<String, Object> converter = (Map<String, Object>) map.get("converter");
        Assert.assertNotNull(converter);
        Assert.assertEquals(converter.get("url"), "https://github.com/mermade/oas3");
        Assert.assertEquals(converter.get("version"), "1.2.3");

        object = component.getExtensions().get("x-api-title");
        assertTrue(object instanceof String);
        Assert.assertEquals("pet store test api in components", object.toString());
    }

    @Test
    public void readOAS(/*JsonNode rootNode*/) throws Exception {
        final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        final JsonNode rootNode = mapper.readTree(Files.readAllBytes(java.nio.file.Paths.get(getClass().getResource("/oas4.yaml").toURI())));
        final OpenAPIDeserializer deserializer = new OpenAPIDeserializer();
        final SwaggerParseResult result = deserializer.deserialize(rootNode);

        Assert.assertNotNull(result);

        final OpenAPI openAPI = result.getOpenAPI();
        Assert.assertNotNull(openAPI);

        final Paths paths = openAPI.getPaths();
        Assert.assertNotNull(paths);

        Assert.assertNotNull(paths);
        Assert.assertEquals(paths.size(), 114);



        PathItem stripe = paths.get("/v1/3d_secure");

        Assert.assertNotNull(stripe);


        Assert.assertNotNull(stripe.getPost());
        Assert.assertNull(stripe.getPost().getDescription());
        Assert.assertEquals(stripe.getPost().getOperationId(), "Create3DSecure");
        Assert.assertNotNull(stripe.getPost().getParameters());

        ApiResponses responses = stripe.getPost().getResponses();
        Assert.assertNotNull(responses);
        assertTrue(responses.containsKey("200"));
        ApiResponse response = responses.get("200");
        Assert.assertEquals(response.getDescription(), "Successful response.");
        Assert.assertEquals(response.getContent().get("application/json").getSchema().get$ref(),"#/components/schemas/three_d_secure");



        PathItem stripeGet = paths.get("/v1/account/external_accounts");

        Assert.assertNotNull(stripeGet);


        Assert.assertNotNull(stripeGet.getGet());
        Assert.assertNull(stripeGet.getGet().getDescription());
        Assert.assertEquals(stripeGet.getGet().getOperationId(), "AllAccountExternalAccounts");
        Assert.assertNotNull(stripeGet.getGet().getParameters());

        ApiResponses responsesGet = stripeGet.getGet().getResponses();
        Assert.assertNotNull(responsesGet);
        assertTrue(responsesGet.containsKey("200"));
        ApiResponse responseGet = responsesGet.get("200");
        Assert.assertEquals(responseGet.getDescription(), "Successful response.");
        Map<String, Schema> properties = (Map<String, Schema>) responseGet.getContent().get("application/json").getSchema().getProperties();

        Assert.assertNotNull(properties);
        Assert.assertNull(properties.get("data").getType());
        Assert.assertEquals(properties.get("has_more").getDescription(), "True if this list has another page of items after this one that can be fetched.");
        assertTrue(properties.get("data") instanceof ComposedSchema );


        ComposedSchema data =  (ComposedSchema) properties.get("data");
        assertTrue(data.getOneOf().get(0) instanceof ArraySchema );
        ArraySchema items = (ArraySchema)data.getOneOf().get(0);
        Assert.assertEquals(items.getItems().get$ref(),"#/components/schemas/bank_account");

    }
    
    @DataProvider(name="data")
    private Object[][] getRootNode() throws Exception {
        final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        final JsonNode rootNode = mapper.readTree(Files.readAllBytes(java.nio.file.Paths.get(getClass().getResource("/oas3.yaml.template").toURI())));
        return new Object[][]{new Object[]{rootNode}};
    }


}
