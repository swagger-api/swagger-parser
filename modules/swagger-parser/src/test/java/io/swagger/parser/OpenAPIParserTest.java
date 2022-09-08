package io.swagger.parser;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.PathItem;

import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;

import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import io.swagger.v3.core.util.Json;
import java.math.BigDecimal;
import java.math.MathContext;
import org.junit.Test;
import org.testng.Assert;

import java.util.Map;

import java.util.List;


import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class OpenAPIParserTest {

    @Test
    public void testIssue1608(){
        ParseOptions options = new ParseOptions();
        options.setResolveFully(true);
        OpenAPIParser openAPIParser = new OpenAPIParser();
        SwaggerParseResult swaggerParseResult = openAPIParser.readLocation("issue1608.json", null, options);
        Schema schema = swaggerParseResult.getOpenAPI().getPaths().get("/pet").getPut().getRequestBody().getContent().get("application/json").getSchema();
        assertEquals(schema.getRequired().size(), 1);
    }

    @Test
    public void testIssue1143(){
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        SwaggerParseResult result = new OpenAPIParser().readLocation("issue-1143.json",null,options);
        assertNotNull(result.getOpenAPI());
        assertNotNull(result.getOpenAPI().getComponents().getSchemas().get("RedisResource"));
        assertNotNull(result.getOpenAPI().getComponents().getSchemas().get("identificacion_usuario_aplicacion"));
    }

    @Test
    public void testIssue1621() {
        final ParseOptions parseOptions = new ParseOptions();
        parseOptions.setResolve(true);
        parseOptions.setResolveFully(true);
        parseOptions.setResolveCombinators(false);
        OpenAPIParser openAPIParser = new OpenAPIParser();
        SwaggerParseResult swaggerParseResult = openAPIParser.readLocation("issue-1621/example.openapi.yaml", null, parseOptions);
        assertEquals(0, swaggerParseResult.getMessages().size());
        OpenAPI api = swaggerParseResult.getOpenAPI();
        assertEquals("POST Example", api.getPaths()
                .get("/example")
                .getPost()
                .getRequestBody()
                .getContent()
                .get("application/json")
                .getSchema()
                .getTitle());
    }

    @Test
    public void testIssue749() {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        SwaggerParseResult result = new OpenAPIParser().readLocation("issue749-main.yaml", null, options);
        assertNotNull(result);

        OpenAPI openAPI = result.getOpenAPI();
        assertNotNull(openAPI);

        Components components = openAPI.getComponents();
        assertNotNull(components);

        PathItem pathItem = openAPI.getPaths().get("/some/ping");
        assertNotNull(pathItem);
        List<Parameter> parameters = pathItem.getGet().getParameters();
        assertNotNull(parameters);
        assertEquals(parameters.size(), 1);
        assertEquals(parameters.get(0).getName(), "i");
        assertNotNull(parameters.get(0).getSchema());
        assertEquals(parameters.get(0).getSchema().get$ref(), "#/components/schemas/SomeId");

        Map<String, Schema> schemas = components.getSchemas();
        assertNotNull(schemas);
        assertEquals(schemas.size(), 1);
        assertNotNull(schemas.get("SomeId"));
    }

    @Test
    public void testSimple() {
        SwaggerParseResult result = new OpenAPIParser().readLocation("petstore.yaml", null, null);

        assertNotNull(result);
        assertNotNull(result.getOpenAPI());
        assertEquals(result.getOpenAPI().getOpenapi(), "3.0.1");
    }


    @Test
    public void testIssue887() {
        ParseOptions options = new ParseOptions();
        SwaggerParseResult result = new OpenAPIParser().readLocation("apiWithMultipleTags.json", null, null);
        System.out.println(result.getMessages());
        assertNotNull(result);
        assertNotNull(result.getOpenAPI());
        assertEquals(result.getMessages().get(1), "attribute tags.sample is repeated");
    }

    @Test
    public void testIssue895() {
        SwaggerParseResult result = new OpenAPIParser().readLocation("issue895.yaml", null, null);
        assertNotNull(result);
        assertNotNull(result.getOpenAPI());
        assertEquals(result.getMessages().get(0),"attribute info.contact.test");
        assertEquals(result.getMessages().get(1),"attribute info.license.test1");

    }

    @Test
    public void testIssue892() {
        SwaggerParseResult result = new OpenAPIParser().readLocation("issue892-main.yaml", null, null);

        assertEquals(result.getMessages().size(),1);
        assertNotNull(result.getOpenAPI());
        assertEquals(result.getOpenAPI().getOpenapi(), "3.0.1");
    }

    @Test
    public void testIssue934() {
        SwaggerParseResult result = new OpenAPIParser().readLocation("issue-934.yaml", null, null);

        assertNotNull(result);
        assertEquals(result.getMessages().size(),1);
        assertNotNull(result.getOpenAPI());
        assertEquals(result.getOpenAPI().getOpenapi(), "3.0.1");
    }

    @Test
    public void testIssue768() {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        SwaggerParseResult result = new OpenAPIParser().readLocation("issue768-main.yaml", null, options);
        assertNotNull(result);

        OpenAPI openAPI = result.getOpenAPI();
        assertNotNull(openAPI);

        Components components = openAPI.getComponents();
        assertNotNull(components);

        Map<String, Schema> schemas = components.getSchemas();
        assertNotNull(schemas);

        assertEquals(schemas.size(), 1);
    }

    @Test
    public void test30Url() {
        String location = "https://raw.githubusercontent.com/OAI/OpenAPI-Specification/master/examples/v3.0/petstore.yaml";

        SwaggerParseResult result = new OpenAPIParser().readLocation(location, null, null);

        assertNotNull(result);
        assertNotNull(result.getOpenAPI());
        assertEquals(result.getOpenAPI().getOpenapi(), "3.0.0");
    }

    @Test
    public void testConverterWithFlatten() {
        String yaml = "swagger: \"2.0\"\n" +
                "info:\n" +
                "  description: \"Foo\"\n" +
                "  version: \"1.0.0\"\n" +
                "host: \"something.com\"\n" +
                "basePath: \"/\"\n" +
                "schemes:\n" +
                "  - \"https\"\n" +
                "consumes:\n" +
                "  - \"application/json\"\n" +
                "produces:\n" +
                "  - \"application/json\"\n" +
                "paths:\n" +
                "  /example:\n" +
                "    get:\n" +
                "      responses:\n" +
                "        200:\n" +
                "          description: \"OK\"\n" +
                "          schema:\n" +
                "            $ref: \"#/definitions/Foo\"\n" +
                "    parameters: []\n" +
                "definitions:\n" +
                "  Foo:\n" +
                "    type: \"object\"\n" +
                "    required:\n" +
                "    properties:\n" +
                "      nested:\n" +
                "        type: \"object\"\n" +
                "        properties:\n" +
                "          color:\n" +
                "            type: \"string\"";

        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setFlatten(true);
        SwaggerParseResult result = new OpenAPIParser().readContents(yaml, null, options);
        OpenAPI openAPI = result.getOpenAPI();
        assertEquals(openAPI.getComponents().getSchemas().size(), 2);
    }

    @Test
    public void test30() {
        String json =
            "{\n" +
            "  \"openapi\": \"3.0.1\",\n" +
            "  \"info\": {\n" +
            "    \"title\": \"Swagger Petstore\",\n" +
            "    \"description\": \"This is a sample server Petstore server. You can find out more about Swagger at [http://swagger.io](http://swagger.io) or on [irc.freenode.net, #swagger](http://swagger.io/irc/). For this sample, you can use the api key `special-key` to test the authorization filters.\",\n" +
            "    \"termsOfService\": \"http://swagger.io/terms/\",\n" +
            "    \"contact\": {\n" +
            "      \"email\": \"apiteam@swagger.io\"\n" +
            "    },\n" +
            "    \"license\": {\n" +
            "      \"name\": \"Apache 2.0\",\n" +
            "      \"url\": \"http://www.apache.org/licenses/LICENSE-2.0.html\"\n" +
            "    },\n" +
            "    \"version\": \"1.0.0\"\n" +
            "  }\n" +
            "}";

        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        SwaggerParseResult result = new OpenAPIParser().readContents(json, null, options);

        assertNotNull(result);
        assertNotNull(result.getOpenAPI());
        assertEquals(result.getOpenAPI().getOpenapi(), "3.0.1");
    }

    @Test
    public void testParsingPrettifiedExtensions() throws Exception {
        String json =
                "{\n" +
                        "  \"openapi\": \"3.0.1\",\n" +
                        "  \"x-some-extension\": \"some-value\"\n" +
                        "}";

        SwaggerParseResult result = new OpenAPIParser().readContents(json, null, null);
        assertNotNull(result);
        OpenAPI openAPI = result.getOpenAPI();
        assertNotNull(openAPI);
        assertNotNull(openAPI.getExtensions());
        assertEquals(openAPI.getExtensions().get("x-some-extension"), "some-value");

        String prettyJson = Json.pretty(openAPI);

        SwaggerParseResult prettyResult = new OpenAPIParser().readContents(prettyJson, null, null);
        assertNotNull(prettyResult);
        OpenAPI prettyOpenAPI = prettyResult.getOpenAPI();
        assertNotNull(prettyOpenAPI);
        assertNotNull(prettyOpenAPI.getExtensions());
        assertEquals(prettyOpenAPI.getExtensions().get("x-some-extension"), "some-value");
    }

    @Test
    public void testIssue799() {
        OpenAPIParser openApiParser = new OpenAPIParser();
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setFlatten(true);

        OpenAPI openAPI = openApiParser.readLocation("issue799.json", null, options).getOpenAPI();
        Assert.assertEquals(((Schema)openAPI.getComponents().getSchemas().get("v1beta3.Binding").getProperties().get("metadata")).get$ref(),"#/components/schemas/v1beta3.ObjectMeta");
        RequestBody bodyParameter = openAPI.getPaths().get("/api/v1beta3/namespaces/{namespaces}/bindings").getPost().getRequestBody();
        Assert.assertEquals( bodyParameter.getContent().get("*/*").getSchema().get$ref(), "#/components/schemas/v1beta3.Binding");
        Assert.assertEquals( openAPI.getPaths().get("/api/v1beta3/namespaces/{namespaces}/componentstatuses/{name}").getGet().getResponses().get("200").getContent().get("application/json").getSchema().get$ref(), "#/components/schemas/v1beta3.ComponentStatus");
        Assert.assertEquals( openAPI.getPaths().get("/api/v1beta3/namespaces/{namespaces}/componentstatuses").getGet().getResponses().get("200").getContent().get("application/json").getSchema().get$ref(), "#/components/schemas/v1beta3.ComponentStatusList");
        Schema conditionsProperty = (Schema) openAPI.getComponents().getSchemas().get("v1beta3.ComponentStatus").getProperties().get("conditions");
        assertTrue( conditionsProperty instanceof ArraySchema);
        Schema items = ((ArraySchema)conditionsProperty).getItems();
        assertTrue( items.get$ref() != null);
        Assert.assertEquals( items.get$ref(), "#/components/schemas/v1beta3.ObjectReference");


    }

    @Test
    public void testIssue813() throws Exception {

        String inputSpec = "{\n" +
                "  \"swagger\": \"2.0\",\n" +
                "  \"info\": {\n" +
                "    \"description\": \"This is a sample server Petstore server.  You can find out more about Swagger at <a href=\\\"http://swagger.io\\\">http://swagger.io</a> or on irc.freenode.net, #swagger.  For this sample, you can use the api key \\\"special-key\\\" to test the authorization filters\",\n" +
                "    \"version\": \"1.0.0\",\n" +
                "    \"title\": \"Swagger Petstore\",\n" +
                "    \"termsOfService\": \"http://helloreverb.com/terms/\",\n" +
                "    \"contact\": {\n" +
                "      \"email\": \"apiteam@wordnik.com\"\n" +
                "    },\n" +
                "    \"license\": {\n" +
                "      \"name\": \"Apache-2.0\",\n" +
                "      \"url\": \"http://www.apache.org/licenses/LICENSE-2.0.html\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"host\": \"petstore.swagger.io\",\n" +
                "  \"basePath\": \"/v2\",\n" +
                "  \"schemes\": [\n" +
                "    \"http\"\n" +
                "  ],\n" +
                "  \"paths\": {\n" +
                "    \"/pet\": {\n" +
                "      \"post\": {\n" +
                "        \"tags\": [\n" +
                "          \"pet\"\n" +
                "        ],\n" +
                "        \"summary\": \"Add a new pet to the store\",\n" +
                "        \"description\": \"\",\n" +
                "        \"operationId\": \"addPet\",\n" +
                "        \"consumes\": [\n" +
                "          \"application/json\",\n" +
                "          \"application/xml\"\n" +
                "        ],\n" +
                "        \"produces\": [\n" +
                "          \"application/json\",\n" +
                "          \"application/xml\"\n" +
                "        ],\n" +
                "        \"parameters\": [{\n" +
                "          \"in\": \"body\",\n" +
                "          \"name\": \"body\",\n" +
                "          \"description\": \"Pet object that needs to be added to the store\",\n" +
                "          \"required\": false,\n" +
                "          \"schema\": {\n" +
                "            \"$ref\": \"#/definitions/Pet\"\n" +
                "          }\n" +
                "        }],\n" +
                "        \"responses\": {\n" +
                "          \"405\": {\n" +
                "            \"description\": \"Invalid input\"\n" +
                "          }\n" +
                "        },\n" +
                "        \"security\": [{\n" +
                "          \"petstore_auth\": [\n" +
                "            \"write:pets\",\n" +
                "            \"read:pets\"\n" +
                "          ]\n" +
                "        }]\n" +
                "      },\n" +
                "      \"put\": {\n" +
                "        \"tags\": [\n" +
                "          \"pet\"\n" +
                "        ],\n" +
                "        \"summary\": \"Update an existing pet\",\n" +
                "        \"description\": \"\",\n" +
                "        \"operationId\": \"updatePet\",\n" +
                "        \"consumes\": [\n" +
                "          \"application/json\",\n" +
                "          \"application/xml\"\n" +
                "        ],\n" +
                "        \"produces\": [\n" +
                "          \"application/json\",\n" +
                "          \"application/xml\"\n" +
                "        ],\n" +
                "        \"parameters\": [{\n" +
                "          \"in\": \"body\",\n" +
                "          \"name\": \"body\",\n" +
                "          \"description\": \"Pet object that needs to be added to the store\",\n" +
                "          \"required\": false,\n" +
                "          \"schema\": {\n" +
                "            \"$ref\": \"#/definitions/Pet\"\n" +
                "          }\n" +
                "        }],\n" +
                "        \"responses\": {\n" +
                "          \"405\": {\n" +
                "            \"description\": \"Validation exception\"\n" +
                "          },\n" +
                "          \"404\": {\n" +
                "            \"description\": \"Pet not found\"\n" +
                "          },\n" +
                "          \"400\": {\n" +
                "            \"description\": \"Invalid ID supplied\"\n" +
                "          }\n" +
                "        },\n" +
                "        \"security\": [{\n" +
                "          \"petstore_auth\": [\n" +
                "            \"write:pets\",\n" +
                "            \"read:pets\"\n" +
                "          ]\n" +
                "        }]\n" +
                "      }\n" +
                "    },\n" +
                "    \"securityDefinitions\": {\n" +
                "      \"api_key\": {\n" +
                "        \"type\": \"apiKey\",\n" +
                "        \"name\": \"api_key\",\n" +
                "        \"in\": \"header\"\n" +
                "      },\n" +
                "      \"petstore_auth\": {\n" +
                "        \"type\": \"oauth2\",\n" +
                "        \"authorizationUrl\": \"http://petstore.swagger.io/api/oauth/dialog\",\n" +
                "        \"flow\": \"implicit\",\n" +
                "        \"scopes\": {\n" +
                "          \"write:pets\": \"modify pets in your account\",\n" +
                "          \"read:pets\": \"read your pets\"\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"definitions\": {\n" +
                "      \"Pet\": {\n" +
                "        \"required\": [\n" +
                "          \"name\",\n" +
                "          \"photoUrls\"\n" +
                "        ],\n" +
                "        \"properties\": {\n" +
                "          \"id\": {\n" +
                "            \"type\": \"integer\",\n" +
                "            \"format\": \"int64\"\n" +
                "          },\n" +
                "          \"category\": {\n" +
                "            \"$ref\": \"#/definitions/Category\"\n" +
                "          },\n" +
                "          \"name\": {\n" +
                "            \"type\": \"string\",\n" +
                "            \"example\": \"doggie\"\n" +
                "          },\n" +
                "          \"photoUrls\": {\n" +
                "            \"type\": \"array\",\n" +
                "            \"xml\": {\n" +
                "              \"name\": \"photoUrl\",\n" +
                "              \"wrapped\": true\n" +
                "            },\n" +
                "            \"items\": {\n" +
                "              \"type\": \"string\"\n" +
                "            }\n" +
                "          },\n" +
                "          \"tags\": {\n" +
                "            \"type\": \"array\",\n" +
                "            \"xml\": {\n" +
                "              \"name\": \"tag\",\n" +
                "              \"wrapped\": true\n" +
                "            },\n" +
                "            \"items\": {\n" +
                "              \"$ref\": \"#/definitions/Tag\"\n" +
                "            }\n" +
                "          },\n" +
                "          \"status\": {\n" +
                "            \"type\": \"string\",\n" +
                "            \"description\": \"pet status in the store\",\n" +
                "            \"enum\": [\n" +
                "              \"available\",\n" +
                "              \"pending\",\n" +
                "              \"sold\"\n" +
                "            ]\n" +
                "          }\n" +
                "        },\n" +
                "        \"xml\": {\n" +
                "          \"name\": \"Pet\"\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}";

        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setFlatten(true);
        SwaggerParseResult result = new OpenAPIParser().readContents(inputSpec, null, options);
        assertTrue(result.getOpenAPI() != null);

    }

    @Test

    public void testIssue844() {
        OpenAPIParser openApiParser = new OpenAPIParser();
        ParseOptions options = new ParseOptions();
        options.setResolve(true);

        OpenAPI openAPI = openApiParser.readLocation("reusableParametersWithExternalRef.json", null, options).getOpenAPI();
        assertNotNull(openAPI);
        assertEquals(openAPI.getPaths().get("/pets/{id}").getGet().getParameters().get(0).getIn(), "header");
    }

    @Test
    public void testIssue258() {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        SwaggerParseResult result = new OpenAPIParser().readLocation("duplicateOperationId.json", null, options);

        System.out.println(result.getMessages());
        assertNotNull(result);
        assertNotNull(result.getOpenAPI());
        assertEquals(result.getMessages().get(0), "attribute paths.'/pets/{id}'(post).operationId is repeated");
   }

    @Test
    public void testIssueRelativeRefs2(){
        String location = "exampleSpecs/specs/my-domain/test-api/v1/test-api-swagger_v1.json";
        ParseOptions po = new ParseOptions();
        po.setResolve(true);
        SwaggerParseResult result = new OpenAPIParser().readLocation(location, null, po);

        assertNotNull(result.getOpenAPI());
        OpenAPI openAPI = result.getOpenAPI();

        Map<String, Schema> schemas = openAPI.getComponents().getSchemas();
        Assert.assertTrue(schemas.get("confirmMessageType_v01").getProperties().get("resources") instanceof ArraySchema);

        ArraySchema arraySchema = (ArraySchema) schemas.get("confirmMessageType_v01").getProperties().get("resources");
        Schema prop = (Schema) arraySchema.getItems().getProperties().get("resourceID");

        assertEquals(prop.get$ref(),"#/components/schemas/simpleIDType_v01");
    }

    @Test
    public void testIssueRelativeRefs1(){
        String location = "specs2/my-domain/test-api/v1/test-api-swagger_v1.json";
        ParseOptions po = new ParseOptions();
        po.setResolve(true);
        SwaggerParseResult result = new OpenAPIParser().readLocation(location, null, po);

        assertNotNull(result.getOpenAPI());
        OpenAPI openAPI = result.getOpenAPI();

        Map<String, Schema> schemas = openAPI.getComponents().getSchemas();
        Assert.assertTrue(schemas.get("test-api-schema_v01").getProperties().get("testingApi") instanceof ArraySchema);

        ArraySchema arraySchema = (ArraySchema) schemas.get("test-api-schema_v01").getProperties().get("testingApi");
        Schema prop = (Schema) arraySchema.getItems().getProperties().get("itemID");

        assertEquals(prop.get$ref(),"#/components/schemas/simpleIDType_v01");
    }

    @Test
    public void testIssue879() {
        OpenAPIParser openApiParser = new OpenAPIParser();
        ParseOptions options = new ParseOptions();
        OpenAPI openAPI = openApiParser.readLocation("issue_879.yaml", null, options).getOpenAPI();

        String ref = openAPI.getPaths()
                .get("/register")
                .getPost()
                .getCallbacks()
                .get("myEvent")
                .get$ref();
        assertEquals(ref, "#/components/callbacks/callbackEvent");
    }

    @Test
    public void testIssue959() {
        OpenAPIParser openAPIParser = new OpenAPIParser();
        SwaggerParseResult result =  openAPIParser.readLocation("issue959.json",null,null);
        assertEquals(result.getMessages().get(0),"attribute paths.'/pets/{petId}'(get).parameters.There are duplicate parameter values");

        result =  openAPIParser.readLocation("issue959PathLevelDuplication.json",null,null);
        assertEquals(result.getMessages().get(0),"attribute paths.'/pets'.There are duplicate parameter values");

    }

    @Test
    public void testIssue1003_ExtensionsClassloader() {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        SwaggerParseResult api = null;
        try {
            // Temporarily switch tccl to an unproductive cl
            final ClassLoader tcclTemp = new java.net.URLClassLoader(new java.net.URL[] {},
                ClassLoader.getSystemClassLoader());
            Thread.currentThread().setContextClassLoader(tcclTemp);
            api = new OpenAPIParser().readLocation("src/test/resources/petstore.yaml",null,null);
        } finally {
            Thread.currentThread().setContextClassLoader(tccl);
        }
        assertNotNull(api);
    }

    @Test
    public void testIssue1070() {
        SwaggerParseResult result = new OpenAPIParser().readLocation("issue1070.yaml", null, null);
        List required = result.getOpenAPI().getComponents().getSchemas().get("AmountAndCurrency").getRequired();
        assertEquals(required.size(), 2);
        assertTrue(required.contains("Amount"));
        assertTrue(required.contains("Currency"));
    }
  
    public void testIssue1086() {
        OpenAPIParser openApiParser = new OpenAPIParser();
        ParseOptions options = new ParseOptions();
        OpenAPI openAPI = openApiParser.readLocation("issue1086.yaml", null, options).getOpenAPI();
        Map<String, Schema> schemas = openAPI.getComponents().getSchemas();
        ObjectSchema schema = (ObjectSchema) schemas.get("AssessCandidate").getProperties().get("test_results");
        Schema score = schema.getProperties().get("score");
        assertEquals(score.getMultipleOf().intValue(), 1);
    }

    @Test
    public void testIssue1433_ResolveSchemaWithoutType() {
        OpenAPIParser openApiParser = new OpenAPIParser();
        ParseOptions options = new ParseOptions();
        options.setResolveFully(true);

        OpenAPI openAPI = openApiParser.readLocation("issue_1433-resolve-schema-without-type.yaml", null, options).getOpenAPI();
        final Schema requestBodySchema = openAPI.getPaths().get("/foo").getPost().getRequestBody().getContent().get("application/json").getSchema();
        assertNotNull(requestBodySchema);

        final Map properties = requestBodySchema.getProperties();
        assertEquals(properties.size(), 2);

        final Object bar = properties.get("bar");
        assertEquals(bar.getClass(), StringSchema.class);

        final Object input = properties.get("input");
        assertEquals(input.getClass(), Schema.class);

        final Map inputProperties = ((Schema) input).getProperties();
        assertNotNull(inputProperties);
        assertEquals(inputProperties.size(),1);

        final Object baz = inputProperties.get("baz");
        assertEquals(baz.getClass(), StringSchema.class);
    }

    @Test
    public void testMultipleOfBetweenZeroAndOne() {
        String spec =
                "openapi: 3.0.0\n" +
                "info:\n" +
                "  version: 0.0.0\n" +
                "  title: \"test\"\n" +
                "components:\n" +
                "  schemas:\n" +
                "    Test:\n" +
                "      type: object\n" +
                "      properties:\n" +
                "        decimal_value:\n" +
                "          type: number\n" +
                "          multipleOf: 0.3\n";

        OpenAPIParser openApiParser = new OpenAPIParser();
        ParseOptions options = new ParseOptions();

        OpenAPI openAPI = openApiParser.readContents(spec, null, options).getOpenAPI();
        ObjectSchema schema = (ObjectSchema) openAPI.getComponents().getSchemas().get("Test");
        Schema decimalValue = schema.getProperties().get("decimal_value");
        BigDecimal multipleOf = decimalValue.getMultipleOf();
        assertEquals(multipleOf, new BigDecimal("0.3", new MathContext(multipleOf.precision())));
    }

    @Test
    public void testConvertWindowsPathsToUnixWhenResolvingServerPaths() {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        SwaggerParseResult result = new OpenAPIParser().readLocation("exampleSpecs\\specs\\issue1553.yaml", null, options);

        assertEquals("/api/customer1/v1", result.getOpenAPI().getServers().get(0).getUrl());
    }

    @Test
    public void testInlineResponseName() throws Exception {
        ParseOptions options = new ParseOptions();
        options.setFlatten(true);
        OpenAPIParser openApiParser = new OpenAPIParser();
        SwaggerParseResult result = openApiParser.readLocation("inlineResponsesTest3.json",null, options);

        assertNotNull(result.getOpenAPI());
        assertNotNull(result.getOpenAPI().getComponents().getSchemas().get("inline_response_200").getType());
    }
    
    @Test
    public void testInlineResponseNameBasedOnEndpointName() throws Exception {
        ParseOptions options = new ParseOptions();
        options.setFlatten(true);
        options.setNameInlineResponsesBasedOnEndpoint(true);
        options.setCamelCaseFlattenNaming(true);
        OpenAPIParser openApiParser = new OpenAPIParser();
        SwaggerParseResult result = openApiParser.readLocation("inlineResponsesTest3.json",null, options);

        assertNotNull(result.getOpenAPI());
        assertNotNull(result.getOpenAPI().getComponents().getSchemas().get("PostDisconnectResponseBodyInline").getType());
    }
}

