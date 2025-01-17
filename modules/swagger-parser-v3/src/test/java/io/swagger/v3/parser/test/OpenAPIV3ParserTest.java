package io.swagger.v3.parser.test;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.links.Link;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.parameters.*;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.parser.OpenAPIResolver;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.AuthorizationValue;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import io.swagger.v3.parser.util.SchemaTypeUtil;
import org.apache.commons.io.FileUtils;
import org.hamcrest.CoreMatchers;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.testng.reporters.Files;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.util.*;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.testng.Assert.*;


public class OpenAPIV3ParserTest {
    List<AuthorizationValue> auths = new ArrayList<>();

    @Test
    public void testFailedToResolveResponseReferences() {
        OpenAPIV3Parser openApiParser = new OpenAPIV3Parser();
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        SwaggerParseResult parseResult = openApiParser.readLocation("issue-2037/openapi.yaml", null, options);
        OpenAPI openAPI = parseResult.getOpenAPI();

        Assert.assertTrue(openAPI.getPaths().get("/get").get$ref() == null);
        Assert.assertEquals(openAPI.getPaths().get("/get").getGet().getResponses().get("200").getContent().get("application/json").getSchema().get$ref(), "#/components/schemas/ResponsesRef");
    }


    @Test
    public void testFailedToResolveExternalReferences() {
        OpenAPIV3Parser openApiParser = new OpenAPIV3Parser();
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        SwaggerParseResult parseResult = openApiParser.readLocation("resolve-external-ref/failedToResolveExternalRefs.yaml", null, options);
        OpenAPI openAPI = parseResult.getOpenAPI();

        Assert.assertTrue(openAPI.getPaths().get("/permAssignments").get$ref() == null);
        Assert.assertEquals(openAPI.getPaths().get("/permAssignments").getGet().getResponses().get("202").getContent().get("application/vnd.api+json").getSchema().get$ref(),"#/components/schemas/schemaResponseSuccess");
        Assert.assertTrue(openAPI.getPaths().get("/permAssignmentChangeRequests").get$ref() == null);
        Assert.assertEquals(openAPI.getPaths().get("/permAssignmentChangeRequests").getGet().getResponses().get("202").getContent().get("application/vnd.api+json").getSchema().get$ref(),"#/components/schemas/schemaResponseSuccess");
        Assert.assertTrue(openAPI.getPaths().get("/permAssignmentChange").get$ref() == null);
        Assert.assertEquals(openAPI.getPaths().get("/permAssignmentChange").getGet().getResponses().get("201").getContent().get("application/vnd.api+json").getSchema().get$ref(),"#/components/schemas/Error");
        Assert.assertEquals(openAPI.getPaths().get("/permAssignmentChange").getGet().getResponses().get("404").getContent().get("application/vnd.api+json").getSchema().get$ref(),"#/components/schemas/RemoteError");

    }

    @Test
    public void testIssueDereferencingComposedSchemaOneOf() {
        OpenAPIV3Parser openApiParser = new OpenAPIV3Parser();
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        SwaggerParseResult parseResult = openApiParser.readLocation("resolverIssue/Beta-1-swagger.yaml", null, options);
        OpenAPI openAPI = parseResult.getOpenAPI();
        assertNotNull(openAPI.getComponents().getSchemas().get("naNumber"));
        assertNotNull(openAPI.getComponents().getSchemas().get("unNumber"));
    }

    @Test
    public void testIssue1621() {
        final ParseOptions parseOptions = new ParseOptions();
        parseOptions.setResolve(true);
        parseOptions.setResolveFully(true);
        parseOptions.setResolveCombinators(false);
        SwaggerParseResult parseResult = new OpenAPIV3Parser().readLocation("issue-1621/example.openapi.yaml", null, parseOptions);
        assertEquals(0, parseResult.getMessages().size());
        OpenAPI api = parseResult.getOpenAPI();
        assertEquals( api.getPaths()
                .get("/example")
                .getPost()
                .getRequestBody()
                .getContent()
                .get("application/json")
                .getSchema()
                .getTitle(),"POST Example");
    }

    @Test
    public void testIssue1865() throws Exception {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        SwaggerParseResult result = new OpenAPIV3Parser().readLocation("src/test/resources/issue-1865/openapi30.yaml", null, options);

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getOpenAPI());
        Assert.assertEquals(result.getOpenAPI().getComponents().getSchemas().get("Foo").get$ref(), "#/components/schemas/foomodel");
    }

    @Test
    public void testIssue1777() {
        OpenAPIV3Parser openApiParser = new OpenAPIV3Parser();
        ParseOptions options = new ParseOptions();
        options.setResolveFully(true);
        SwaggerParseResult parseResult = openApiParser.readLocation("issue-1777/issue1777.yaml", null, options);
        OpenAPI openAPI = parseResult.getOpenAPI();
        Schema id = ((Schema)openAPI.getComponents().getSchemas().get("customer").getProperties().get("id"));
        assertEquals(id.getType(), "string");
        assertEquals(id.getFormat(), "uuid");
        assertEquals(id.getDescription(), "The id of the customer");
        assertNotNull(id.getExtensions().get("x-apigen-mapping"));
        assertEquals(id.getMinLength(), (Integer) 36);
        assertEquals(id.getMaxLength(), (Integer) 36);
        assertNotNull(id.getPattern());
    }

    @Test
    public void testIssue1802() {
        OpenAPIV3Parser openApiParser = new OpenAPIV3Parser();
        ParseOptions options = new ParseOptions();
        options.setResolveFully(true);
        SwaggerParseResult parseResult = openApiParser.readLocation("issue-1802/issue1802.yaml", null, options);
        OpenAPI openAPI = parseResult.getOpenAPI();
        Map<String, Schema> props = openAPI.getComponents().getSchemas().get("standard_response_res_one").getProperties();
        assertNotNull(props.get("data"));
        assertNotNull(props.get("result"));
    }

    @Test
    public void testIssue1780() {
        ParseOptions options = new ParseOptions();
        options.setInferSchemaType(false);
        String defaultSchemaType = "---\n" +
                "openapi: '3.0'\n" +
                "info:\n" +
                "  description: This is a credit card account service tier 3 microservice\n" +
                "  title: SC-CCS-CCSRV_AC Credit Card Account Service\n" +
                "  version: 1.0.0\n" +
                "tags:\n" +
                "- name: creditCardAccount\n" +
                "  description: Credit Card Accounts\n" +
                "paths: {}\n" +
                "x-ibm-configuration: {}";
        SwaggerParseResult result = new OpenAPIV3Parser().readContents(defaultSchemaType, null, options);
        OpenAPI openAPI = result.getOpenAPI();
        assertNotNull(openAPI);
        assertTrue(result.getMessages().contains("The provided definition does not specify a valid version field"));

    }

    @Test
    public void testParametersAndResponsesAsNumbers() throws Exception {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setResolveResponses(true);
        SwaggerParseResult result = new OpenAPIV3Parser().readLocation("src/test/resources/parametersAsNumbers/swagger.yaml", null, options);

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getOpenAPI());
        Assert.assertEquals(result.getOpenAPI().getPaths().get("/api/deal/{dealId}").getGet().getParameters().get(0).getName(), "dealId");
        Assert.assertEquals(result.getOpenAPI().getPaths().get("/api/deal/{dealId}").getGet().getResponses().get("200").getDescription(), "Success");
    }

    @Test
    public void testIssue1758() throws Exception{
        ParseOptions options = new ParseOptions();
        SwaggerParseResult result = new OpenAPIV3Parser().readLocation("src/test/resources/issue1758.yaml", null, options);

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getOpenAPI());
        assertEquals(result.getMessages().size(),9);
        assertTrue(result.getMessages().contains("paths.'/path1'.$ref target #/components/schemas/xFoo is not of expected type PathItem"));
        assertTrue(result.getMessages().contains("paths.'/foo'(get).parameters.$ref target #/components/schemas/xFoo is not of expected type Parameter/Header"));
        assertTrue(result.getMessages().contains("paths.'/foo'(get).responses.default.headers.three.$ref target #/components/schemas/xFoo is not of expected type Header/Parameter"));
        assertTrue(result.getMessages().contains("paths.'/foo'(get).requestBody.$ref target #/components/schemas/xFoo is not of expected type RequestBody"));
        assertTrue(result.getMessages().contains("paths.'/foo'(get).responses.200.links.user.$ref target #/components/schemas/xFoo is not of expected type Link"));
        assertTrue(result.getMessages().contains("paths.'/foo'(get).responses.200.content.'application/json'.schema.$ref target #/components/parameters/pet is not of expected type Schema"));
        assertTrue(result.getMessages().contains("paths.'/foo'(get).responses.200.content.'application/json'.examples.one.$ref target #/components/schemas/xFoo is not of expected type Examples"));
        assertTrue(result.getMessages().contains("paths.'/foo'(get).responses.400.$ref target #/components/schemas/xFoo is not of expected type Response"));
        assertTrue(result.getMessages().contains("paths.'/foo'(get).callbacks.$ref target #/components/schemas/xFoo is not of expected type Callback"));

    }

    @Test(description = "Test for not setting the schema type as default")
    public void testNotDefaultSchemaType() {
        ParseOptions options = new ParseOptions();
        options.setInferSchemaType(false);
        String defaultSchemaType = "openapi: 3.0.0\n" +
                "info:\n" +
                "  title: ping test\n" +
                "  version: '1.0'\n" +
                "servers:\n" +
                "  - url: 'http://localhost:8000/'\n" +
                "paths:\n" +
                "  /ping:\n" +
                "    get:\n" +
                "      operationId: pingGet\n" +
                "      responses:\n" +
                "        '201':\n" +
                "          description: OK\n" +
                "components:\n" +
                "  schemas:\n" +
                "    AnyValueModelInline:\n" +
                "      description: test any value inline\n" +
                "      type: object\n" +
                "      properties:\n" +
                "        array_any_value:\n" +
                "          items: {}\n" +
                "        map_any_value:\n" +
                "         additionalProperties: {}\n" +
                "        any_value: {}\n" +
                "        any_value_with_desc:\n" +
                "          description: inline any value\n" +
                "        any_value_nullable:\n" +
                "          nullable: true\n" +
                "          description: inline any value nullable\n" +
                "        map_any_value_with_desc:\n" +
                "          additionalProperties: \n" +
                "            description: inline any value\n" +
                "        map_any_value_nullable:\n" +
                "          additionalProperties:\n" +
                "            nullable: true\n" +
                "            description: inline any value nullable\n" +
                "        array_any_value_with_desc:\n" +
                "          items: \n" +
                "            description: inline any value\n" +
                "        array_any_value_nullable:\n" +
                "          items:\n" +
                "            nullable: true\n" +
                "            description: inline any value nullable";
        SwaggerParseResult result = new OpenAPIV3Parser().readContents(defaultSchemaType, null, options);
        OpenAPI openAPI = result.getOpenAPI();
        assertNotNull(openAPI);

        //map_any_value as object when it should be null
        assertNotNull(openAPI.getComponents().getSchemas().get("AnyValueModelInline").getProperties().get("map_any_value"));
        assertTrue(openAPI.getComponents().getSchemas().get("AnyValueModelInline").getProperties().get("map_any_value") instanceof Schema);
        Schema schema = (Schema)openAPI.getComponents().getSchemas().get("AnyValueModelInline").getProperties().get("map_any_value");
        assertNull(schema.getType());

        //map_any_value_with_desc as object when it should be null
        assertNotNull(openAPI.getComponents().getSchemas().get("AnyValueModelInline").getProperties().get("map_any_value_with_desc"));
        assertTrue(openAPI.getComponents().getSchemas().get("AnyValueModelInline").getProperties().get("map_any_value_with_desc") instanceof Schema);
        schema = (Schema)openAPI.getComponents().getSchemas().get("AnyValueModelInline").getProperties().get("map_any_value_with_desc");
        assertNull(schema.getType());

        //map_any_value_nullable as object when it should be null
        assertNotNull(openAPI.getComponents().getSchemas().get("AnyValueModelInline").getProperties().get("map_any_value_nullable"));
        assertTrue(openAPI.getComponents().getSchemas().get("AnyValueModelInline").getProperties().get("map_any_value_nullable") instanceof Schema);
        schema = (Schema)openAPI.getComponents().getSchemas().get("AnyValueModelInline").getProperties().get("map_any_value_nullable");
        assertNull(schema.getType());

        //array_any_value as array when it should be null
        assertNotNull(openAPI.getComponents().getSchemas().get("AnyValueModelInline").getProperties().get("array_any_value"));
        assertTrue(openAPI.getComponents().getSchemas().get("AnyValueModelInline").getProperties().get("array_any_value") instanceof Schema);
        schema = (Schema)openAPI.getComponents().getSchemas().get("AnyValueModelInline").getProperties().get("array_any_value");
        assertNotNull(schema.getItems());
        assertNull(schema.getType());

        //array_any_value_with_desc as array when it should be null
        assertNotNull(openAPI.getComponents().getSchemas().get("AnyValueModelInline").getProperties().get("array_any_value_with_desc"));
        assertTrue(openAPI.getComponents().getSchemas().get("AnyValueModelInline").getProperties().get("array_any_value_with_desc") instanceof Schema);
        schema = (Schema)openAPI.getComponents().getSchemas().get("AnyValueModelInline").getProperties().get("array_any_value_with_desc");
        assertNotNull(((Schema)openAPI.getComponents().getSchemas().get("AnyValueModelInline").getProperties().get("array_any_value_with_desc")).getItems().getDescription());
        assertNull(schema.getType());

        //array_any_value_nullable
        assertNotNull(openAPI.getComponents().getSchemas().get("AnyValueModelInline").getProperties().get("array_any_value_nullable"));
        assertTrue(openAPI.getComponents().getSchemas().get("AnyValueModelInline").getProperties().get("array_any_value_nullable") instanceof Schema);
        schema = (Schema)openAPI.getComponents().getSchemas().get("AnyValueModelInline").getProperties().get("array_any_value_nullable");
        assertNull(schema.getType());
        assertNotNull(schema.getItems());
        assertNotNull(schema.getItems().getDescription());
        assertTrue(schema.getItems().getNullable());
    }

    @Test
    public void testIssue1637_StyleAndContent() throws IOException {
        ParseOptions options = new ParseOptions();
        SwaggerParseResult result = new OpenAPIV3Parser().readLocation("src/test/resources/issue1637.yaml", null, options);

        Assert.assertNull(result.getOpenAPI().getPaths().get("/test").getGet().getParameters().get(0).getStyle());
        Assert.assertNull(result.getOpenAPI().getPaths().get("/test").getGet().getParameters().get(0).getExplode());
        Assert.assertNotNull(result.getOpenAPI().getPaths().get("/test").getGet().getParameters().get(0).getContent());
    }

    @Test
    public void testIssue1643_True() throws Exception{
        ParseOptions options = new ParseOptions();
        String issue1643 = "openapi: \"3.0.0\"\n" +
                "info:\n" +
                "  version: 1.0.0\n" +
                "  title: People\n" +
                "paths:\n" +
                "  /person:\n" +
                "    get:\n" +
                "      operationId: getPerson\n" +
                "      parameters:\n" +
                "        - name: name\n" +
                "          in: query\n" +
                "          required: true\n" +
                "          schema:\n" +
                "            type: string\n" +
                "      responses:\n" +
                "        '200':\n" +
                "          description: The person with the given name.          \n" +
                "          content:\n" +
                "            application/json:\n" +
                "              schema:\n" +
                "                $ref: \"#/components/schemas/Person\"\n" +
                "components:\n" +
                "  schemas:\n" +
                "    Person:\n" +
                "      required:\n" +
                "      - name\n" +
                "      type: object\n" +
                "      properties:\n" +
                "        name:\n" +
                "          type: string\n" +
                "        employee:\n" +
                "          $ref: \"#/components/schemas/Employee\"";
        SwaggerParseResult result = new OpenAPIV3Parser().readContents(issue1643, null, options);

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getOpenAPI());
        assertEquals(result.getMessages().size(),1);
        assertTrue(result.getMessages().contains("attribute components.schemas.Person.Employee is not of type `schema`"));
    }

    @Test
    public void testIssue1643_False() throws Exception{
        ParseOptions options = new ParseOptions();
        options.setValidateInternalRefs(false);
        String issue1643 = "openapi: \"3.0.0\"\n" +
                "info:\n" +
                "  version: 1.0.0\n" +
                "  title: People\n" +
                "paths:\n" +
                "  /person:\n" +
                "    get:\n" +
                "      operationId: getPerson\n" +
                "      parameters:\n" +
                "        - name: name\n" +
                "          in: query\n" +
                "          required: true\n" +
                "          schema:\n" +
                "            type: string\n" +
                "      responses:\n" +
                "        '200':\n" +
                "          description: The person with the given name.          \n" +
                "          content:\n" +
                "            application/json:\n" +
                "              schema:\n" +
                "                $ref: \"#/components/schemas/Person\"\n" +
                "components:\n" +
                "  schemas:\n" +
                "    Person:\n" +
                "      required:\n" +
                "      - name\n" +
                "      type: object\n" +
                "      properties:\n" +
                "        name:\n" +
                "          type: string\n" +
                "        employee:\n" +
                "          $ref: \"#/components/schemas/Employee\"";
        SwaggerParseResult result = new OpenAPIV3Parser().readContents(issue1643, null, options);

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getOpenAPI());
        assertEquals(result.getMessages().size(),0);
        assertFalse(result.getMessages().contains("attribute components.schemas.Person.Employee is not of type `schema`"));
    }

    @Test
    public void testExampleFormatByte() throws Exception{

        ParseOptions options = new ParseOptions();
        SwaggerParseResult result = new OpenAPIV3Parser().readLocation("src/test/resources/issue1630.yaml", null, options);

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getOpenAPI());
        OpenAPI openAPI = result.getOpenAPI();
        Schema model = (Schema) openAPI.getComponents().getSchemas().get("Response").getProperties().get("content");
        assertTrue(model instanceof ByteArraySchema);
        ByteArraySchema byteArraySchema = (ByteArraySchema) model;
        assertEquals(new String((byte[])byteArraySchema.getExample()), "VGhpc1Nob3VsZFBhc3MK");
        System.setProperty(SchemaTypeUtil.BINARY_AS_STRING, "true");
        result = new OpenAPIV3Parser().readLocation("src/test/resources/issue1630.yaml", null, options);
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getOpenAPI());
        openAPI = result.getOpenAPI();
        model = (Schema) openAPI.getComponents().getSchemas().get("Response").getProperties().get("content");
        assertTrue(model instanceof StringSchema);
        StringSchema stringSchema = (StringSchema) model;
        assertEquals(stringSchema.getExample(), "VGhpc1Nob3VsZFBhc3MK");
        System.clearProperty(SchemaTypeUtil.BINARY_AS_STRING);

    }

    @Test
    public void testIssue1658() throws Exception{
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        SwaggerParseResult result = new OpenAPIV3Parser().readLocation("src/test/resources/issue-1658/issue1658.yaml", null, options);

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getOpenAPI());
        OpenAPI openAPI = result.getOpenAPI();
        assertTrue(result.getMessages().size() == 0);
        assertFalse(result.getMessages().contains("String index out of range: -1"));
        assertEquals(openAPI.getComponents().getSchemas().get("ref1").get$ref(), "#/components/schemas/ref2");
        assertTrue(openAPI.getComponents().getSchemas().get("ref2") != null);
        assertTrue(openAPI.getComponents().getSchemas().get("ref2").get$ref() == null);

    }

    @Test
    public void testIssue1644_NullValue() throws Exception{
        ParseOptions options = new ParseOptions();
        String issue1644 = "openapi: 3.0.0\n" +
                "info:\n" +
                "  title: Operations\n" +
                "  version: 0.0.0\n" +
                "paths:\n" +
                "  \"/operations\":\n" +
                "    post:\n" +
                "      parameters:\n" +
                "        - name: param0\n" +
                "          schema:\n" +
                "            type: string\n" +
                "      responses:\n" +
                "        default:\n" +
                "          description: None\n";
        SwaggerParseResult result = new OpenAPIV3Parser().readContents(issue1644, null, options);

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getOpenAPI());
        assertEquals(result.getMessages().size(),1);
        assertTrue(result.getMessages().contains("attribute paths.'/operations'(post).parameters.[param0].in is missing"));
        assertFalse(result.getMessages().contains("attribute paths.'/operations'(post).parameters.[param0].in is not of type `string`"));
    }

    @Test
    public void testIssue1644_EmptyValue() throws Exception{
        ParseOptions options = new ParseOptions();
        String issue1644 = "openapi: 3.0.0\n" +
                "info:\n" +
                "  title: Operations\n" +
                "  version: 0.0.0\n" +
                "paths:\n" +
                "  \"/operations\":\n" +
                "    post:\n" +
                "      parameters:\n" +
                "        - name: param0\n" +
                "          in: ''\n" +
                "          schema:\n" +
                "            type: string\n" +
                "      responses:\n" +
                "        default:\n" +
                "          description: None\n";
        SwaggerParseResult result = new OpenAPIV3Parser().readContents(issue1644, null, options);

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getOpenAPI());
        assertEquals(result.getMessages().size(),1);
        assertTrue(result.getMessages().contains("attribute paths.'/operations'(post).parameters.[param0].in is not of type `[query|header|path|cookie]`"));
    }


    @Test
    public void testEmptyStrings_False() throws Exception{
        ParseOptions options = new ParseOptions();
        options.setAllowEmptyString(false);
        SwaggerParseResult result = new OpenAPIV3Parser().readLocation("src/test/resources/empty-strings.yaml", null, options);

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getOpenAPI());
        OpenAPI openAPI = result.getOpenAPI();
        assertNull(openAPI.getInfo().getTitle());
        assertNotNull(openAPI.getInfo().getVersion());
        assertNull(openAPI.getInfo().getLicense().getName());
        assertNull(openAPI.getPaths().get("/something").getGet().getResponses().get("200").getDescription());
        assertNull(openAPI.getPaths().get("/something").getGet().getParameters().get(0).getDescription());
    }


    @Test
    public void testEmptyStrings_True() throws Exception{
        ParseOptions options = new ParseOptions();
        options.setAllowEmptyString(true);
        SwaggerParseResult result = new OpenAPIV3Parser().readLocation("src/test/resources/empty-strings.yaml", null, options);

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getOpenAPI());
        OpenAPI openAPI = result.getOpenAPI();
        assertNotNull(openAPI.getInfo().getTitle());
        assertNotNull(openAPI.getInfo().getLicense().getName());
        assertNotNull(openAPI.getPaths().get("/something").getGet().getResponses().get("200").getDescription());
        assertNotNull(openAPI.getPaths().get("/something").getGet().getParameters().get(0).getDescription());
    }


    @Test
    public void testIssue1561() {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        SwaggerParseResult result = new OpenAPIV3Parser().readLocation("issue-1561/swagger.yaml", null, options);
        OpenAPI openAPI = result.getOpenAPI();
        assertTrue(openAPI.getComponents().getResponses().size() == 3);
    }

    @Test
    public void testAnonymousModelAllOf() {
        ParseOptions options = new ParseOptions();
        options.setResolveFully(true);
        SwaggerParseResult result = new OpenAPIV3Parser().readLocation("issue203/issue203AllOf.yaml", null, options);
        OpenAPI openAPI = result.getOpenAPI();
        assertEquals(openAPI.getComponents().getSchemas().get("Supplier").getXml().getName(),"supplierObject");
    }

    @Test
    public void testIssue1518() {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        SwaggerParseResult result = new OpenAPIV3Parser().readLocation("issue-1518/api.json", null, options);
        OpenAPI openAPI = result.getOpenAPI();
        assertTrue(((Schema)openAPI.getComponents().getSchemas().get("Analemmata").getProperties().get("tashotSipe")).get$ref().equals("#/components/schemas/TashotSipe"));
        assertTrue(openAPI.getComponents().getSchemas().get("analemmata") == null);
    }

    @Test
    public void testIssue1518StackOverFlow() {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        try {
            SwaggerParseResult result = new OpenAPIV3Parser().readLocation("example0/api.json", null, options);
            Yaml.prettyPrint(result.getOpenAPI().getComponents().getSchemas());
        }catch (StackOverflowError stackOverflowError){
            assertTrue(false);
        }
    }

    @Test
    public void testCantReadDeepProperties() {
        OpenAPIV3Parser parser = new OpenAPIV3Parser();
        ParseOptions options = new ParseOptions();
        options.setResolveFully(true);
        options.setValidateInternalRefs(false);

        final SwaggerParseResult parseResult = parser.readLocation("src/test/resources/cant-read-deep-properties.yaml", null, options);
        assertEquals(parseResult.getMessages().size(), 0);
        Schema projects = (Schema) parseResult.getOpenAPI().getComponents().getSchemas().get("Project").getProperties().get("project_type");
        assertEquals(projects.getType(), "integer");
    }

    @Test
    public void testIssueSameRefsDifferentModelValid() {
        OpenAPIV3Parser parser = new OpenAPIV3Parser();
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setResolveFully(true);

        final SwaggerParseResult openAPI = parser.readLocation("src/test/resources/same-refs-different-model-valid.yaml", null, options);
        assertEquals(openAPI.getMessages().size(), 0);
    }

    @Test
    public void testIssue1398() {
        ParseOptions options = new ParseOptions();
        SwaggerParseResult result = new OpenAPIV3Parser().readLocation("issue1398.yaml", null, options);
        assertEquals(result.getMessages().get(0), "paths.'/pet/{petId}'(get).parameters.[petId].schemas.multipleOf value must be > 0");
    }

    @Test
    public void testIssue1367() {
        OpenAPIV3Parser openApiParser = new OpenAPIV3Parser();
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setResolveCombinators(true);
        options.setResolveFully(true);
        options.setFlatten(true);
        SwaggerParseResult parseResult = openApiParser.readLocation("issue-1367.yaml", null, options);
        OpenAPI openAPI = parseResult.getOpenAPI();
        assertTrue(((Schema)openAPI.getComponents().getSchemas().get("TestDTO").getProperties().get("choice")).getEnum() != null);
    }

    @Test
    public void testDeserializeExampleFlag() {
        OpenAPIV3Parser openApiParser = new OpenAPIV3Parser();
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setResolveCombinators(true);
        options.setResolveFully(true);
        options.setFlatten(true);
        SwaggerParseResult parseResult = openApiParser.readLocation("exampleFlag.yaml", null, options);
        OpenAPI openAPI = parseResult.getOpenAPI();
        assertTrue(openAPI.getComponents().getSchemas().get("TestDTO").getExampleSetFlag());
        assertNull(openAPI.getComponents().getSchemas().get("TestDTO").getExample());
        assertTrue(openAPI.getComponents().getSchemas().get("TestString").getExampleSetFlag());
        assertNull(openAPI.getComponents().getSchemas().get("TestString").getExample());
        assertTrue(openAPI.getComponents().getSchemas().get("TestNumber").getExampleSetFlag());
        assertNull(openAPI.getComponents().getSchemas().get("TestNumber").getExample());

        assertFalse(openAPI.getComponents().getSchemas().get("TestDTOMissing").getExampleSetFlag());
        assertNull(openAPI.getComponents().getSchemas().get("TestDTOMissing").getExample());
        assertFalse(openAPI.getComponents().getSchemas().get("TestStringMissing").getExampleSetFlag());
        assertNull(openAPI.getComponents().getSchemas().get("TestStringMissing").getExample());
        assertFalse(openAPI.getComponents().getSchemas().get("TestNumberMissing").getExampleSetFlag());
        assertNull(openAPI.getComponents().getSchemas().get("TestNumberMissing").getExample());
    }

    @Test
    public void testExampleFlag() {
        OpenAPIV3Parser openApiParser = new OpenAPIV3Parser();
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setResolveCombinators(true);
        options.setResolveFully(true);
        options.setFlatten(true);
        SwaggerParseResult parseResult = openApiParser.readLocation("media-type-null-example.yaml", null, options);

        OpenAPI openAPI = parseResult.getOpenAPI();

        assertNull(openAPI.getPaths().get("/pets/{petId}").getGet().getResponses().get("200").getContent().get("application/json").getExample());
        assertTrue(openAPI.getPaths().get("/pets/{petId}").getGet().getResponses().get("200").getContent().get("application/json").getExampleSetFlag());

        assertNull(openAPI.getPaths().get("/pet").getPost().getResponses().get("200").getContent().get("application/json").getExample());
        assertFalse(openAPI.getPaths().get("/pet").getPost().getResponses().get("200").getContent().get("application/json").getExampleSetFlag());

        assertNotNull(openAPI.getPaths().get("/pet").getPost().getRequestBody().getContent().get("application/json").getExample());
        assertNotNull(openAPI.getPaths().get("/pet").getPost().getRequestBody().getContent().get("application/json").getExample());

        assertTrue(openAPI.getPaths().get("/pet").getPost().getRequestBody().getContent().get("application/json").getExampleSetFlag());

        assertNotNull(openAPI.getPaths().get("/object-with-null-example").getGet().getResponses().get("200").getContent().get("application/json").getExamples().get("foo").getValue());
        assertTrue(openAPI.getPaths().get("/object-with-null-example").getGet().getResponses().get("200").getContent().get("application/json").getExamples().get("foo").getValueSetFlag());
        assertNull(openAPI.getPaths().get("/object-with-null-example").getGet().getResponses().get("200").getContent().get("application/json").getExamples().get("bar").getValue());
        assertTrue(openAPI.getPaths().get("/object-with-null-example").getGet().getResponses().get("200").getContent().get("application/json").getExamples().get("bar").getValueSetFlag());

        assertNotNull(openAPI.getPaths().get("/object-with-null-in-schema-example").getGet().getResponses().get("200").getContent().get("application/json").getExamples().get("a").getValue());
        assertTrue(openAPI.getPaths().get("/object-with-null-in-schema-example").getGet().getResponses().get("200").getContent().get("application/json").getExamples().get("a").getValueSetFlag());
        assertNotNull(openAPI.getPaths().get("/object-with-null-in-schema-example").getGet().getResponses().get("200").getContent().get("application/json").getExamples().get("b").getValue());
        assertTrue(openAPI.getPaths().get("/object-with-null-in-schema-example").getGet().getResponses().get("200").getContent().get("application/json").getExamples().get("b").getValueSetFlag());
        assertNotNull(openAPI.getPaths().get("/object-with-null-in-schema-example").getGet().getResponses().get("200").getContent().get("application/json").getExamples().get("c").getValue());
        assertTrue(openAPI.getPaths().get("/object-with-null-in-schema-example").getGet().getResponses().get("200").getContent().get("application/json").getExamples().get("c").getValueSetFlag());
        assertNull(openAPI.getPaths().get("/object-with-null-in-schema-example").getGet().getResponses().get("200").getContent().get("application/json").getExamples().get("d").getValue());
        assertTrue(openAPI.getPaths().get("/object-with-null-in-schema-example").getGet().getResponses().get("200").getContent().get("application/json").getExamples().get("d").getValueSetFlag());


        assertNull(openAPI.getComponents().getSchemas().get("ObjectWithNullExample").getExample());
        assertTrue(openAPI.getComponents().getSchemas().get("ObjectWithNullExample").getExampleSetFlag());

        assertNotNull(openAPI.getComponents().getSchemas().get("ObjectWithNullInSchemaExample").getExample());
        assertTrue(openAPI.getComponents().getSchemas().get("ObjectWithNullInSchemaExample").getExampleSetFlag());

        assertNotNull(((Schema)openAPI.getComponents().getSchemas().get("ObjectWithNullPropertyExample").getProperties().get("a")).getExample());
        assertTrue(((Schema)openAPI.getComponents().getSchemas().get("ObjectWithNullPropertyExample").getProperties().get("a")).getExampleSetFlag());
        assertNull(((Schema)openAPI.getComponents().getSchemas().get("ObjectWithNullPropertyExample").getProperties().get("b")).getExample());
        assertTrue(((Schema)openAPI.getComponents().getSchemas().get("ObjectWithNullPropertyExample").getProperties().get("b")).getExampleSetFlag());

        assertNull(openAPI.getComponents().getSchemas().get("StringWithNullExample").getExample());
        assertTrue(openAPI.getComponents().getSchemas().get("StringWithNullExample").getExampleSetFlag());

        assertNull(openAPI.getComponents().getSchemas().get("ArrayWithNullArrayExample").getExample());
        assertTrue(openAPI.getComponents().getSchemas().get("ArrayWithNullArrayExample").getExampleSetFlag());

        assertNull(((ArraySchema)openAPI.getComponents().getSchemas().get("ArrayWithNullItemExample")).getItems().getExample());
        assertTrue(((ArraySchema)openAPI.getComponents().getSchemas().get("ArrayWithNullItemExample")).getItems().getExampleSetFlag());
    }

    @Test
    public void testIssueFlattenAdditionalPropertiesSchemaInlineModelTrue() {
        OpenAPIV3Parser openApiParser = new OpenAPIV3Parser();
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setFlatten(true);
        options.setFlattenComposedSchemas(true);
        options.setCamelCaseFlattenNaming(true);
        SwaggerParseResult parseResult = openApiParser.readLocation("additionalPropertiesFlatten.yaml", null, options);
        OpenAPI openAPI = parseResult.getOpenAPI();

        //responses
        assertNotNull(openAPI.getComponents().getSchemas().get("InlineResponseMap200"));
        assertEquals(((ComposedSchema)openAPI.getComponents().getSchemas().get("InlineResponseMap200")).getOneOf().get(0).get$ref(),"#/components/schemas/Macaw1");
        assertNotNull(openAPI.getComponents().getSchemas().get("InlineResponseMapItems404"));
        assertEquals(((ComposedSchema)openAPI.getComponents().getSchemas().get("InlineResponseMapItems404")).getAnyOf().get(0).get$ref(),"#/components/schemas/Macaw2");
    }


    @Test
    public void testIssueFlattenArraySchemaItemsInlineModelFalse() {
        OpenAPIV3Parser openApiParser = new OpenAPIV3Parser();
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setFlatten(true);
        options.setFlattenComposedSchemas(false);
        options.setCamelCaseFlattenNaming(false);
        SwaggerParseResult parseResult = openApiParser.readLocation("flattenArrayItems.yaml", null, options);
        OpenAPI openAPI = parseResult.getOpenAPI();

        //responses
        assertNull(openAPI.getComponents().getSchemas().get("Inline_response_items200"));
        assertNull(openAPI.getComponents().getSchemas().get("Inline_response_400"));

        //parameters
        assertNull(openAPI.getComponents().getSchemas().get("Inline_parameter_items_bodylimit"));
        assertNull(openAPI.getComponents().getSchemas().get("Pagelimit"));

        //requestBodies
        assertNull(openAPI.getComponents().getSchemas().get("Body"));
        assertNull(openAPI.getComponents().getSchemas().get("Inline_response_items200"));

        //components
        assertNull(openAPI.getComponents().getSchemas().get("Inline_array_items_ArrayTest"));

    }

    @Test
    public void testIssueFlattenArraySchemaItemsInlineModelTrue() {
        OpenAPIV3Parser openApiParser = new OpenAPIV3Parser();
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setFlatten(true);
        options.setFlattenComposedSchemas(true);
        options.setCamelCaseFlattenNaming(true);
        SwaggerParseResult parseResult = openApiParser.readLocation("flattenArrayItems.yaml", null, options);
        OpenAPI openAPI = parseResult.getOpenAPI();

        //responses
        assertNotNull(openAPI.getComponents().getSchemas().get("InlineResponseItems200"));
        assertEquals(((ComposedSchema)openAPI.getComponents().getSchemas().get("InlineResponseItems200")).getAnyOf().get(0).get$ref(),"#/components/schemas/Macaw");
        assertNotNull(openAPI.getComponents().getSchemas().get("InlineResponse400"));
        assertEquals(((ComposedSchema)openAPI.getComponents().getSchemas().get("InlineResponse400")).getAnyOf().get(0).get$ref(),"#/components/schemas/Macaw3");

        //parameters
        assertNotNull(openAPI.getComponents().getSchemas().get("InlineParameterItemsBodylimit"));
        assertEquals(((ComposedSchema)openAPI.getComponents().getSchemas().get("InlineParameterItemsBodylimit")).getAnyOf().get(0).get$ref(),"#/components/schemas/Macaw1");
        assertNotNull(openAPI.getComponents().getSchemas().get("Pagelimit"));
        assertEquals(((ComposedSchema)openAPI.getComponents().getSchemas().get("Pagelimit")).getOneOf().get(0).get$ref(),"#/components/schemas/Macaw2");

        //requestBodies
        assertNotNull(openAPI.getComponents().getSchemas().get("RequestBodiesBody"));
        assertEquals(((ComposedSchema)openAPI.getComponents().getSchemas().get("RequestBodiesBody")).getAllOf().get(1).get$ref(),"#/components/schemas/requestBodiesAllOf2");
        assertNotNull(openAPI.getComponents().getSchemas().get("InlineResponseItems200"));
        assertEquals(((ComposedSchema)openAPI.getComponents().getSchemas().get("InlineBodyItemsApplicationxmlrequestBodies")).getAllOf().get(1).get$ref(),"#/components/schemas/ApplicationxmlAllOf2");

        //components
        assertNotNull(openAPI.getComponents().getSchemas().get("InlineArrayItemsArrayTest"));
        assertEquals(((ComposedSchema)openAPI.getComponents().getSchemas().get("InlineArrayItemsArrayTest")).getOneOf().get(1).get$ref(),"#/components/schemas/ArrayTestOneOf2");
    }


    @Test
    public void testCamelCaseFlattenNamingFalse() {
        OpenAPIV3Parser openApiParser = new OpenAPIV3Parser();
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setFlatten(true);
        options.setFlattenComposedSchemas(true);
        options.setCamelCaseFlattenNaming(false);
        SwaggerParseResult parseResult = openApiParser.readLocation("FlattenComposedSchemasAtComponents.yaml", null, options);
        OpenAPI openAPI = parseResult.getOpenAPI();
        assertNotNull(openAPI);
        assertEquals(((ComposedSchema)openAPI.getComponents().getSchemas().get("contact-base-model")).getAllOf().get(0).get$ref(),"#/components/schemas/contactbasemodelAllOf_1");
    }

    @Test
    public void testIssueFlattenComposedSchemaInlineModel() {
        OpenAPIV3Parser openApiParser = new OpenAPIV3Parser();
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setFlatten(true);
        options.setFlattenComposedSchemas(true);
        options.setCamelCaseFlattenNaming(true);
        SwaggerParseResult parseResult = openApiParser.readLocation("FlattenComposedSchemasAtComponents.yaml", null, options);
        OpenAPI openAPI = parseResult.getOpenAPI();
        assertNotNull(openAPI);
        assertEquals(((ComposedSchema)openAPI.getComponents().getSchemas().get("contact-base-model")).getAllOf().get(0).get$ref(),"#/components/schemas/ContactBaseModelAllOf1");
        assertEquals(((ComposedSchema)openAPI.getComponents().getSchemas().get("Test")).getOneOf().get(1).get$ref(),"#/components/schemas/TestOneOf2");
    }

    @Test
    public void testIssueFlattenComposedSchemaInlineModelFlagFalse() {
        OpenAPIV3Parser openApiParser = new OpenAPIV3Parser();
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setFlatten(true);
        options.setFlattenComposedSchemas(false);
        SwaggerParseResult parseResult = openApiParser.readLocation("FlattenComposedSchemasAtComponents.yaml", null, options);
        OpenAPI openAPI = parseResult.getOpenAPI();
        assertNotNull(openAPI);
        assertNull(((ComposedSchema)openAPI.getComponents().getSchemas().get("contact-base-model")).getAllOf().get(0).get$ref());
        assertNull(((ComposedSchema)openAPI.getComponents().getSchemas().get("Test")).getOneOf().get(1).get$ref());
    }

    @Test
    public void testIssue1309() {
        OpenAPIV3Parser openApiParser = new OpenAPIV3Parser();
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        SwaggerParseResult parseResult = openApiParser.readLocation("issue-1309.yaml", null, options);
        OpenAPI openAPI = parseResult.getOpenAPI();
        assertNotNull(openAPI);
        assertEquals(parseResult.getMessages().get(0),"attribute components.schemas.customer-not-found.examples is unexpected");
    }

    @Test
    public void testIssue1316() {
        OpenAPIV3Parser openApiParser = new OpenAPIV3Parser();
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        SwaggerParseResult parseResult = openApiParser.readLocation("Issue1316.yaml", null, options);
        OpenAPI openAPI = parseResult.getOpenAPI();
        assertNotNull(openAPI);
        assertTrue(parseResult.getMessages().size() == 0);
    }


    @Test
    public void testIssue_1292() {
        OpenAPIV3Parser openApiParser = new OpenAPIV3Parser();
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        SwaggerParseResult parseResult = openApiParser.readLocation("issue-1292/petstore.yml", null, options);
        OpenAPI openAPI = parseResult.getOpenAPI();
        assertNotNull(openAPI.getPaths().get("/pets").getGet().getResponses().get("200").getContent().get("application/json").getSchema().get$ref(), "#/components/schemas/Pets");
        assertNotNull(openAPI.getPaths().get("/pets").getGet().getResponses().getDefault().getContent().get("application/json").getSchema().get$ref(), "#/components/schemas/Error");
        assertNotNull(openAPI.getComponents().getSchemas().get("Pet"));
        assertNotNull(openAPI.getComponents().getSchemas().get("Pets"));
        assertNotNull(openAPI.getComponents().getSchemas().get("Error"));
    }

    @Test
    public void testIssue_505() {
        OpenAPIV3Parser openApiParser = new OpenAPIV3Parser();
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        SwaggerParseResult parseResult = openApiParser.readLocation("issue-505/petstore.yml", null, options);
        OpenAPI openAPI = parseResult.getOpenAPI();

        assertNotNull(openAPI.getComponents().getSchemas().get("DateWithExample"));
        assertNotNull(openAPI.getComponents().getExamples().get("DateWithExample"));
        assertNotNull(openAPI.getComponents().getLinks().get("userRepository"));

        assertEquals(3, openAPI.getPaths().get("/pets").getGet().getParameters().size());
    }

    @Test
    public void testFlattenComposedSchema() {
        OpenAPIV3Parser openApiParser = new OpenAPIV3Parser();
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setFlatten(true);
        options.setFlattenComposedSchemas(true);
        SwaggerParseResult parseResult = openApiParser.readLocation("flattenComposedSchemaComplete.json", null, options);

        OpenAPI openAPI = parseResult.getOpenAPI();
        assertNotNull(openAPI.getComponents().getSchemas().get("val_Members_val_member"));
        assertNotNull(openAPI.getComponents().getSchemas().get("val_MemberProducts_val_product"));
    }

    @Test
    public void testNotFlattenComposedSchema() {
        OpenAPIV3Parser openApiParser = new OpenAPIV3Parser();
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setFlatten(true);

        SwaggerParseResult parseResult = openApiParser.readLocation("flattenComposedSchemaComplete.json", null, options);

        OpenAPI openAPI = parseResult.getOpenAPI();
        assertNull(openAPI.getComponents().getSchemas().get("val_Members_val_member"));
        assertNotNull(openAPI.getComponents().getSchemas().get("val_MemberProducts_val_product"));
    }

    @Test
    public void testCodegenIssue8601() {
        OpenAPIV3Parser openApiParser = new OpenAPIV3Parser();
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setFlatten(true);
        SwaggerParseResult parseResult = openApiParser.readLocation("codegen-issue-8601.yaml", null, options);

        OpenAPI openAPI = parseResult.getOpenAPI();
        assertNotNull(openAPI.getComponents().getSchemas().get("status"));
        assertNotNull(openAPI.getComponents().getSchemas().get("test_body"));
        assertNotNull(openAPI.getComponents().getSchemas().get("inline_response_200"));
        assertNotNull(openAPI.getComponents().getSchemas().get("test_body_1"));
        assertNotNull(openAPI.getComponents().getSchemas().get("Test1"));
        assertNotNull(openAPI.getComponents().getSchemas().get("Test2"));
    }

    @Test
    public void testCodegenIssue9773() {
        OpenAPIV3Parser openApiParser = new OpenAPIV3Parser();
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setFlatten(true);
        SwaggerParseResult parseResult = openApiParser.readLocation("codegen-issue-9773/openapi.yaml", null, options);

        OpenAPI openAPI = parseResult.getOpenAPI();
        assertNotNull(openAPI.getComponents().getSchemas().get("Foo"));
    }

    @Test
    public void testIssueResolveCallBacks() {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setFlatten(true);
        SwaggerParseResult parseResult = new OpenAPIV3Parser().readLocation("callbacks-issue/swagger.yaml", null, options);
        OpenAPI spec = parseResult.getOpenAPI();
        assertNotNull(spec);
        assertEquals(spec.getPaths().get("/webhook").getPost().getCallbacks().get("WebhookVerificationEvent").get$ref(),"#/components/callbacks/WebhookVerificationEvent");
    }

    @Test
    public void testIssue1190() {
        OpenAPIV3Parser openApiParser = new OpenAPIV3Parser();
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setFlatten(true);
        SwaggerParseResult parseResult = openApiParser.readLocation("issue1190/issue1190.yaml", null, options);

        OpenAPI openAPI = parseResult.getOpenAPI();
        assertNotNull(openAPI.getComponents().getSchemas().get("SomeObj_lorem"));
    }

    @Test
    public void testIssue1147() {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setFlatten(true);
        SwaggerParseResult parseResult = new OpenAPIV3Parser().readLocation("issue-1147/issue1147.yaml", null, options);
        OpenAPI apispec = parseResult.getOpenAPI();
        assertNotNull(apispec);
        assertEquals(((Schema)apispec.getComponents().getSchemas().get("StringObject").getProperties().get("val")).get$ref(),"#/components/schemas/String");
    }

    @Test
    public void testIssue1148_Flatten_Dot() {
        ParseOptions options = new ParseOptions();
        options.setFlatten(true);
        SwaggerParseResult parseResult = new OpenAPIV3Parser().readLocation("issue1148.yaml", null, options);
        OpenAPI apispec = parseResult.getOpenAPI();
        assertNotNull(apispec);
        assertEquals(((Schema)apispec.getComponents().getSchemas().get("Some.User").getProperties().get("address")).get$ref(),"#/components/schemas/Some.User_address");
    }

    @Test
    public void testIssue1169() {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        SwaggerParseResult parseResult = new OpenAPIV3Parser().readLocation("issue1169.yaml", null, options);
        assertTrue(parseResult.getMessages().size() == 0);
        OpenAPI apispec = parseResult.getOpenAPI();
        assertNotNull(apispec);
    }

    @Test
    public void testIssue1169noSplit() {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        SwaggerParseResult parseResult = new OpenAPIV3Parser().readLocation("issue1169-noSplit.yaml", null, options);
        assertTrue(parseResult.getMessages().size() == 0);
        OpenAPI apispec = parseResult.getOpenAPI();
        assertNotNull(apispec);
    }



    @Test
    public void testIssue339() throws Exception {
        OpenAPIV3Parser openAPIV3Parser = new OpenAPIV3Parser();
        OpenAPI api = openAPIV3Parser.read("issue-339.yaml");
        assertNotNull(api);
        Parameter param = api.getPaths().get("/store/order/{orderId}").getGet().getParameters().get(0);
        assertTrue(param instanceof PathParameter);
        PathParameter pp = (PathParameter) param;
        assertTrue(pp.getSchema().getMinimum().toString().equals("1"));
        assertTrue(pp.getSchema().getMaximum().toString().equals("5"));
    }

    @Test
    public void testIssue1119() {
        OpenAPIV3Parser parser = new OpenAPIV3Parser();
        parser.setEncoding("ISO-8859-1");
        OpenAPI openAPI = parser.read("src/test/resources/issue-1119.yaml");
        assertNotNull(openAPI);
        assertEquals(openAPI.getPaths().get("/pets").getGet().getParameters().get(0).getDescription(),"Cuántos artículos devolver al mismo tiempo (máximo 100)");
    }

    @Test
    public void testIssue1108() {
        OpenAPIV3Parser parser = new OpenAPIV3Parser();
        OpenAPI openAPI = parser.read("issue-1108.yaml");
        assertNotNull(openAPI);
        assertNotNull(openAPI.getPaths().get("/pets").getGet().getParameters().get(0).getAllowReserved());
    }

    @Test
    public void testRemoteParameterIssue1094() throws Exception{
        OpenAPI result = new OpenAPIV3Parser().read("issue-1094/swagger.yaml");
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getComponents().getSchemas().get("PlmnId"));
    }

    @Test
    public void testIssue1071() {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);

        SwaggerParseResult parseResult = new OpenAPIV3Parser().readLocation("issue-1071.yaml", null, options);
        OpenAPI apispec = parseResult.getOpenAPI();
        assertNotNull(apispec);
        Schema test = apispec.getPaths().get("/mapschema").getGet().getResponses().get("200").getContent().get("application/json").getSchema();
        assertTrue(test instanceof MapSchema);
    }

    @Test
    public void testIssue1071True() {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        SwaggerParseResult parseResult = new OpenAPIV3Parser().readLocation("issue-1071-true.yaml", null, options);
        OpenAPI apispec = parseResult.getOpenAPI();
        assertNotNull(apispec);
        Schema test = apispec.getPaths().get("/mapschema").getGet().getResponses().get("200").getContent().get("application/json").getSchema();
        assertTrue(test instanceof MapSchema);
        assertTrue(test.getAdditionalProperties() instanceof Boolean);
        assertTrue((Boolean)test.getAdditionalProperties());
    }

    @Test
    public void testIssue1071False() {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        SwaggerParseResult parseResult = new OpenAPIV3Parser().readLocation("issue-1071-false.yaml", null, options);
        OpenAPI apispec = parseResult.getOpenAPI();
        assertNotNull(apispec);
        Schema test = apispec.getPaths().get("/mapschema").getGet().getResponses().get("200").getContent().get("application/json").getSchema();
        assertTrue(test instanceof ObjectSchema);
        assertTrue(test.getAdditionalProperties() instanceof Boolean);
        assertFalse((Boolean)test.getAdditionalProperties());
    }

    @Test
    public void testIssue1039() {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        SwaggerParseResult parseResult = new OpenAPIV3Parser().readLocation("issue_1039.yaml", null, options);
        OpenAPI apispec = parseResult.getOpenAPI();
        assertNotNull(apispec);
        assertEquals(apispec.getPaths().get("/pets").getGet().getParameters().get(0).getSchema().getType(),"array");
    }

    @Test
    public void testIssue1015() {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setResolveCombinators(true);
        SwaggerParseResult parseResult = new OpenAPIV3Parser().readLocation("issue-1015.json", null, options);
        if (parseResult.getMessages() != null && !parseResult.getMessages().isEmpty()) {
            parseResult.getMessages().forEach(s -> System.out.println(s));
            fail("Error while loading apispec!");
        }
        OpenAPI apispec = parseResult.getOpenAPI();
        assertNotNull(apispec);
    }

    @Test
    public void testIssueIntegerDefault() {
        OpenAPIV3Parser parser = new OpenAPIV3Parser();
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        final OpenAPI openAPI = parser.readLocation("integerDefault.yaml", null, options).getOpenAPI();
        Assert.assertNotNull(openAPI);
        Assert.assertFalse (((IntegerSchema) openAPI.getPaths().get("/fileUpload").getPost().getRequestBody().getContent().get("multipart/form-data").getSchema().getProperties().get("intMetadata")).getFormat() == "int32");
        Assert.assertNull ( openAPI.getPaths().get("/mockResponses/primitiveDoubleResponse").getGet().getResponses().get("200").getContent().get("application/json").getSchema().getFormat());
        Assert.assertNull ( openAPI.getPaths().get("/issue-125").getGet().getResponses().get("200").getContent().get("*/*").getSchema().getFormat());
        Assert.assertNull (openAPI.getPaths().get("/primitiveBody/binary").getPost().getRequestBody().getContent().get("application/octet-stream").getSchema().getFormat());
    }

    @Test
    public void testIssue983() {
        OpenAPIV3Parser parser = new OpenAPIV3Parser();
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        final OpenAPI openAPI = parser.readLocation("issue-983.yaml", null, options).getOpenAPI();
        Assert.assertNotNull(openAPI);
        Assert.assertNotNull(openAPI.getComponents().getSchemas().get("InventoryId"));
    }

    @Test
    public void testIssue913() {
        OpenAPIV3Parser parser = new OpenAPIV3Parser();
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        final OpenAPI openAPI = parser.readLocation("issue-913/BS/ApiSpecification.yaml", null, options).getOpenAPI();
        Assert.assertNotNull(openAPI);
        Assert.assertNotNull(openAPI.getComponents().getSchemas().get("indicatorType"));
        Assert.assertEquals(openAPI.getComponents().getSchemas().get("indicatorType").getProperties().size(),1);
    }

    @Test
    public void testIssue901_2() {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        OpenAPI openAPI = new OpenAPIV3Parser().readLocation("issue-901/spec2.yaml",null,options).getOpenAPI();
        assertNotNull(openAPI);
        assertNotNull(openAPI.getComponents());
        ArraySchema arraySchema = (ArraySchema) openAPI.getComponents().getSchemas().get("Test.Definition").getProperties().get("stuff");
        String internalRef = arraySchema.getItems().get$ref();
        assertEquals(internalRef,"#/components/schemas/TEST.THING.OUT.Stuff");
    }

    @Test
    public void testIssue901() {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        OpenAPI openAPI = new OpenAPIV3Parser().readLocation("issue-901/spec.yaml",null,options).getOpenAPI();
        assertNotNull(openAPI);
        String internalRef = openAPI.getPaths().get("/test").getPut().getResponses().get("200").getContent().get("application/json").getSchema().get$ref();
        assertEquals(internalRef,"#/components/schemas/Test.Definition");
        assertNotNull(openAPI.getComponents());
    }

    @Test
    public void testIssue853() {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setFlatten(true);
        final OpenAPI openAPI = new OpenAPIV3Parser().readLocation("issue-837-853-1131/main.yaml", null, options).getOpenAPI();
        Assert.assertNotNull(openAPI);

        Operation post = openAPI.getPaths().get("/guests").getPost();
        Assert.assertNotNull(post);

        Content content = post.getResponses().get("201").getContent();
        Assert.assertNotNull(content);

        Map<String, Example> examples = content.get("application/json").getExamples();
        Assert.assertEquals(examples.size(), 1);
        assertNotNull(openAPI.getComponents());
        assertNotNull(openAPI.getComponents().getExamples());
        assertNotNull(openAPI.getComponents().getExamples().get("testExample"));
        assertEquals(((LinkedHashMap<String, Object>)openAPI.getComponents().getExamples().get("testExample").getValue()).get("test"),"value");
    }

    @Test
    public void testIssue837() {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        final OpenAPI openAPI = new OpenAPIV3Parser().readLocation("issue-837-853-1131/main.yaml", null, options).getOpenAPI();
        Assert.assertNotNull(openAPI);

        Content content = openAPI.getPaths().get("/events").getGet().getResponses().get("200").getContent();
        Assert.assertNotNull(content);

        Map<String, Example> examples = content.get("application/json").getExamples();
        Assert.assertEquals(examples.size(), 3);
        Assert.assertEquals(((ObjectNode) examples.get("plain").getValue()).get("test").asText(), "plain");
        Assert.assertEquals(examples.get("local").get$ref(), "#/components/examples/LocalRef");
        Assert.assertEquals(examples.get("external").get$ref(), "#/components/examples/ExternalRef");
    }

    @Test
    public void testIssue1131() {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        final OpenAPI openAPI = new OpenAPIV3Parser().readLocation("issue-837-853-1131/main.yaml", null, options).getOpenAPI();
        Assert.assertNotNull(openAPI);

        Content content = openAPI.getPaths().get("/events").getGet().getRequestBody().getContent();
        Assert.assertNotNull(content);

        Map<String, Example> examples = content.get("application/json").getExamples();
        Assert.assertEquals(examples.size(), 3);
        Assert.assertEquals(((ObjectNode) examples.get("plain").getValue()).get("test").asText(), "plain");
        Assert.assertEquals(examples.get("local").get$ref(), "#/components/examples/LocalRef");
        Assert.assertEquals(examples.get("external").get$ref(), "#/components/examples/ExternalRef");

        // Also cover the case from Issue 853
        Operation post = openAPI.getPaths().get("/guests").getPost();
        Assert.assertNotNull(post);

        content = post.getRequestBody().getContent();
        Assert.assertNotNull(content);

        examples = content.get("application/json").getExamples();
        Assert.assertEquals(examples.size(), 1);
        assertNotNull(openAPI.getComponents());
        assertNotNull(openAPI.getComponents().getExamples());
        assertNotNull(openAPI.getComponents().getExamples().get("testExample"));
        assertEquals(((LinkedHashMap<String, Object>)openAPI.getComponents().getExamples().get("testExample").getValue()).get("test"),"value");
    }

    @Test
    public void testIssue834() {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setResolveResponses(true);
        SwaggerParseResult result = new OpenAPIV3Parser().readLocation("issue-834/index.yaml", null, options);
        assertNotNull(result.getOpenAPI());

        Content foo200Content = result.getOpenAPI().getPaths().get("/foo").getGet().getResponses().get("200").getContent();
        assertNotNull(foo200Content);
        String foo200SchemaRef = foo200Content.get("application/json").getSchema().get$ref();
        assertEquals(foo200SchemaRef, "#/components/schemas/schema");

        Content foo300Content = result.getOpenAPI().getPaths().get("/foo").getGet().getResponses().get("300").getContent();
        assertNotNull(foo300Content);
        String foo300SchemaRef = foo300Content.get("application/json").getSchema().get$ref();
        assertEquals(foo300SchemaRef, "#/components/schemas/schema");

        Content bar200Content = result.getOpenAPI().getPaths().get("/bar").getGet().getResponses().get("200").getContent();
        assertNotNull(bar200Content);
        String bar200SchemaRef = bar200Content.get("application/json").getSchema().get$ref();
        assertEquals(bar200SchemaRef, "#/components/schemas/schema");
    }

    @Test
    public void testIssue811_RefSchema_ToRefSchema() {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        final OpenAPI openAPI = new OpenAPIV3Parser().readLocation("oapi-reference-test2/index.yaml", null, options).getOpenAPI();

        Assert.assertNotNull(openAPI);
        Assert.assertEquals(openAPI.getPaths().get("/").getGet().getResponses().get("200").getContent().get("application/json").getSchema().get$ref(),"#/components/schemas/schema-with-reference");
    }

    @Test
    public void testIssue811() {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setResolveResponses(true);
        final OpenAPI openAPI = new OpenAPIV3Parser().readLocation("oapi-reference-test/index.yaml", null, options).getOpenAPI();
        Assert.assertNotNull(openAPI);
        Assert.assertEquals(openAPI.getPaths().get("/").getGet().getResponses().get("200").getContent().get("application/json").getSchema().get$ref(),"#/components/schemas/schema-with-reference");
    }

    @Test
    public void testIssue719() {
        final OpenAPI openAPI = new OpenAPIV3Parser().readLocation("extensions-responses.yaml", null, new ParseOptions()).getOpenAPI();
        Assert.assertNotNull(openAPI);
        Assert.assertNotNull(openAPI.getPaths().getExtensions());
        Assert.assertNotNull(openAPI.getPaths().get("/something").getGet().getResponses().getExtensions());
    }

    @Test
    public void issue682() throws Exception {
        OpenAPIV3Parser parser = new OpenAPIV3Parser();
        ParseOptions options = new ParseOptions();
        options.setResolve(true);

        final SwaggerParseResult result = parser.readLocation("src/test/resources/sample/SwaggerPetstore.yaml", null, options);
        Assert.assertNotNull(result.getOpenAPI());
        Assert.assertTrue(result.getMessages().isEmpty());
        Assert.assertNotNull(result.getOpenAPI().getPaths().get("/pets").getGet());
    }

    @Test
    public void issue941() throws Exception {
        OpenAPIV3Parser parser = new OpenAPIV3Parser();

        final OpenAPI result = parser.read("src/test/resources/sample/SwaggerPetstore.yaml");
        Assert.assertNotNull(result);
        assertEquals("Documentation de l'API élaboré par nos soins", result.getInfo().getDescription());
    }

    @Test
    public void issueRelativeRefs2() throws Exception {
        OpenAPIV3Parser parser = new OpenAPIV3Parser();
        ParseOptions options = new ParseOptions();
        options.setResolve(true);

        final SwaggerParseResult result = parser.readLocation("src/test/resources/relative-upper-directory/swagger.yaml", null, options);
        Assert.assertNotNull(result.getOpenAPI());
        OpenAPI openAPI = result.getOpenAPI();
        assertNotNull(openAPI.getPaths().get("/api/Address").getGet());
        assertTrue(openAPI.getComponents().getSchemas().size() == 1);
        assertNotNull(openAPI.getComponents().getSchemas().get("AddressEx"));
    }

    @Test
    public void testPattern() {
        final OpenAPI openAPI = new OpenAPIV3Parser().readLocation("testPattern.yaml", null, new ParseOptions()).getOpenAPI();
        Schema s = openAPI.getComponents().getSchemas().get("SomeObj");
        Assert.assertEquals(s.getPattern(),"^[A-Z]+$"); //ERROR: got null
    }

    @Test
    public void testResolveEmpty() throws Exception{
        String pathFile = FileUtils.readFileToString(new File("src/test/resources/empty-oas.yaml"));
        ParseOptions options = new ParseOptions();
        options.setResolveFully(true);

        SwaggerParseResult result = new OpenAPIV3Parser().readContents(pathFile, auths, options  );

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getOpenAPI());
    }




    @Test
    public void testRemotePathItemIssue1103() throws Exception{
        OpenAPI result = new OpenAPIV3Parser().read("issue-1103/remote-pathItem-swagger.yaml");
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getPaths().get("/Translation/{lang}"));
        Assert.assertEquals(result.getPaths().get("/Translation/{lang}").getPut().getParameters().get(0).getName(), "lang");
    }

    @Test
    public void testRemoteParameterIssue1103() throws Exception{
        OpenAPI result = new OpenAPIV3Parser().read("issue-1103/remote-parameter-swagger.yaml");
        Assert.assertNotNull(result);
        Assert.assertEquals(result.getPaths().get("/Translation/{lang}").getPut().getParameters().get(0).getName(), "lang");
    }

    @Test
    public void testIssue1105() throws Exception {
        OpenAPI openAPI = new OpenAPIV3Parser().read("issue-1105/swagger-api.yaml");
        Assert.assertNotNull(openAPI);
        Assert.assertNotNull(openAPI.getComponents().getSchemas().get("ErrorCodeDescription"));
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
    public void testRefAndInlineAllOf() throws Exception {
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
    public void testComposedRefResolvingIssue628() throws Exception {
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
    public void testComposedSchemaAdjacent() throws Exception {
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
        Assert.assertTrue(pet.getDiscriminator().getMapping().get("Cat").equals("#/components/schemas/Cat_1"));
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
                "    $ref: \"#/paths/~1foo\"";

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
                        "        - $ref: \"#/parameters/report-id\"\n" +
                        "    put:\n" +
                        "      parameters:\n" +
                        "        - name: id\n" +
                        "          in: body\n" +
                        "          required: true\n" +
                        "          schema:\n" +
                        "            $ref: \"#/components/schemas/report\"\n" +
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
                        "      - $ref: \"#/components/parameters/foo-id\"\n" +
                        "    get:\n" +
                        "      responses:\n" +
                        "        '200':\n" +
                        "          description: OK\n" +
                        "          content:\n" +
                        "            '*/*':\n" +
                        "              schema:\n" +
                        "                $ref: \"#/components/schemas/foo\"\n" +
                        "    put:\n" +
                        "      responses:\n" +
                        "        '200':\n" +
                        "          description: OK\n" +
                        "          content:\n" +
                        "            '*/*':\n" +
                        "              schema:\n" +
                        "                $ref: \"#/components/schemas/foo\"\n" +
                        "      requestBody:\n" +
                        "        content:\n" +
                        "          application/json:\n" +
                        "            schema:\n" +
                        "              $ref: \"#/components/schemas/foo\"\n" +
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
    }

    @Test
    public void testLoadExternalNestedDefinitions() throws Exception {
        OpenAPIV3Parser parser = new OpenAPIV3Parser();
        final OpenAPI openAPI = parser.read("src/test/resources/nested-references/b.yaml");

        Map<String, Schema> definitions = openAPI.getComponents().getSchemas();
        assertTrue(definitions.containsKey("x"));
        assertTrue(definitions.containsKey("y"));
        assertTrue(definitions.containsKey("z"));
        assertEquals( definitions.get("i").get$ref(),"#/components/schemas/k");
        assertEquals( definitions.get("k").getTitle(), "k-definition");
    }

    @Test
    public void testLoadExternalNestedDefinitionsWithReferenceWithinSubfolder() throws Exception {
        OpenAPIV3Parser parser = new OpenAPIV3Parser();
        final OpenAPI openAPI = parser.read("src/test/resources/nested-references-2/main.yaml");

        Map<String, Schema> definitions = openAPI.getComponents().getSchemas();
        assertTrue(definitions.containsKey("GreetingResponse"));
        assertTrue(definitions.containsKey("GreetingMessage"));
        assertTrue(definitions.containsKey("greeting-message"));

        String greetingMessageRef = ((Schema) definitions.get("GreetingResponse")
                .getProperties().get("GreetingMessage")).get$ref();

        assertEquals(greetingMessageRef,"#/components/schemas/GreetingMessage");
        assertEquals(definitions.get("GreetingMessage").get$ref(),"#/components/schemas/greeting-message");

        String description = ((Schema)definitions.get("greeting-message")
                .getProperties().get("Text")).getDescription();
        assertEquals(description,"Text message");
    }

    @Test
    public void testLoadExternalNestedDefinitionsWithReferenceWithinSameFolder() throws Exception {
        OpenAPIV3Parser parser = new OpenAPIV3Parser();
        final OpenAPI openAPI = parser.read("src/test/resources/nested-references-3/main.yaml");

        Map<String, Schema> definitions = openAPI.getComponents().getSchemas();
        assertTrue(definitions.containsKey("GreetingResponse"));
        assertTrue(definitions.containsKey("GreetingMessage"));
        assertTrue(definitions.containsKey("greeting-message"));

        String greetingMessageRef = ((Schema) definitions.get("GreetingResponse")
                .getProperties().get("GreetingMessage")).get$ref();

        assertEquals(greetingMessageRef,"#/components/schemas/GreetingMessage");
        assertEquals(definitions.get("GreetingMessage").get$ref(),"#/components/schemas/greeting-message");

        String description = ((Schema)definitions.get("greeting-message")
                .getProperties().get("Text")).getDescription();
        assertEquals(description,"Text message");
    }

    @Test
    public void testLoadExternalNestedDefinitionsWithReferenceOnDifferentFolderLevels() throws Exception {
        OpenAPIV3Parser parser = new OpenAPIV3Parser();
        final OpenAPI openAPI = parser.read("src/test/resources/nested-references-4/main.yaml");

        Map<String, Schema> definitions = openAPI.getComponents().getSchemas();
        assertTrue(definitions.containsKey("GreetingResponse"));
        assertTrue(definitions.containsKey("GreetingMessage"));
        assertTrue(definitions.containsKey("greeting-message"));

        String greetingMessageRef = ((Schema) definitions.get("GreetingResponse")
                .getProperties().get("GreetingMessage")).get$ref();

        assertEquals(greetingMessageRef,"#/components/schemas/GreetingMessage");
        assertEquals(definitions.get("GreetingMessage").get$ref(),"#/components/schemas/greeting-message");

        String description = ((Schema)definitions.get("greeting-message")
                .getProperties().get("Text")).getDescription();
        assertEquals(description,"Text message");
    }

    @Test
    public void testPetstore() throws Exception {
        OpenAPIV3Parser parser = new OpenAPIV3Parser();
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        SwaggerParseResult result = parser.readLocation("src/test/resources/petstore.yaml", null, options);

        assertNotNull(result);
        assertTrue(result.getMessages().size()==2);

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

    @Test
    public void testRelativePath() {
        OpenAPIV3Parser parser = new OpenAPIV3Parser();
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setResolveResponses(true);
        SwaggerParseResult readResult = parser.readLocation("src/test/resources/relative-issue/api.yaml", null, options);
        Yaml.prettyPrint(readResult.getOpenAPI());
        Assert.assertEquals(readResult.getOpenAPI().getPaths().get("/scans").getGet().getResponses().get("500").getContent().get("application/json").getSchema().get$ref(), "#/components/schemas/ErrorMessage");
    }

    @Test
    public void testRelativePath2() {
        OpenAPIV3Parser parser = new OpenAPIV3Parser();
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setResolveResponses(true);
        SwaggerParseResult readResult = parser.readLocation("src/test/resources/codegen-remote-responses/openapi.yaml", null, options);
        Assert.assertEquals(readResult.getOpenAPI().getPaths().get("/pet/findByTags").getGet().getResponses().get("default").getContent().get("application/json").getSchema().get$ref(), "#/components/schemas/ErrorModel");
    }

    @Test
    public void testExternalRefsNormalization() throws Exception {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        SwaggerParseResult result = new OpenAPIV3Parser()
                .readLocation("src/test/resources/oas3.fetched/openapi3.yaml", null, options);

        OpenAPI openAPI = result.getOpenAPI();
        Schema originalModel = openAPI.getComponents().getSchemas().get("Event");
        Schema duplicateModel = openAPI.getComponents().getSchemas().get("Event_1");
        System.out.println("component schemas found: " + openAPI.getComponents().getSchemas().keySet());
        assertNull(duplicateModel);
        assertNotNull(originalModel);
    }

    private OpenAPI doRelativeFileTest(String location) {
        OpenAPIV3Parser parser = new OpenAPIV3Parser();
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setResolveResponses(true);
        SwaggerParseResult readResult = parser.readLocation(location, null, options);

        if (readResult.getMessages().size() > 0) {
            Json.prettyPrint(readResult.getMessages());
        }
        final OpenAPI openAPI = readResult.getOpenAPI();
        final PathItem path = openAPI.getPaths().get("/health");
        assertEquals(path.getClass(), PathItem.class); //we successfully converted the RefPath to a Path

        final List<Parameter> parameters = path.getParameters();
        assertNull(parameters);

        final Operation operation = path.getGet();
        final List<Parameter> operationParams = operation.getParameters();
        assertParamDetails(operationParams, 0, QueryParameter.class, "param1", "query");
        assertParamDetails(operationParams, 1, HeaderParameter.class, "param2", "header");
        assertParamDetails(operationParams, 2, PathParameter.class, "param3", "path");
        assertParamDetails(operationParams, 3, HeaderParameter.class, "param4", "header");

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
        assertEquals(composedCat.getAllOf().get(1).get$ref(), "#/components/schemas/foo_1");

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
                "        $ref: \"#/components/schemas/Content\"\n" +
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
                "                $ref: \"#/components/schemas/Content\"\n" +
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
                "            - $ref: \"#/x-error-defs/credentialTooShort\"\n" +
                "            - $ref: \"#/x-error-defs/credentialTooLong\"\n" +
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
                        "          $ref: \"#/components/schemas/Schema\"\n" +
                        "        '400':\n" +
                        "          definitions: this is right\n" +
                        "          description: Bad Request\n" +
                        "          content:\n" +
                        "            '*/*':\n" +
                        "              schema:\n" +
                        "                $ref: \"#/components/schemas/Schema\"\n" +
                        "      requestBody:\n" +
                        "        content:\n" +
                        "          application/json:\n" +
                        "            schema:\n" +
                        "              $ref: \"#/components/schemas/Schema\"\n" +
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
                        "                $ref: \"#/components/schemas/Simple\"\n" +
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
                        "        $ref: \"#/components/schemas/Pet\"\n" +
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

    @Test
    public void checkPathParameterRequiredValue() {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        SwaggerParseResult swaggerParseResult = new OpenAPIV3Parser().readLocation("src/test/resources/issue-1319.yaml", null, options);
        assertEquals(2, swaggerParseResult.getMessages().size());
        assertEquals(2, swaggerParseResult.getOpenAPI().getComponents().getSchemas().size());
        assertEquals(2, swaggerParseResult.getOpenAPI().getPaths().size());
        assertEquals(1, swaggerParseResult.getOpenAPI().getComponents().getParameters().size());
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
        String yaml = Files.readFile(new FileInputStream("src/test/resources/over-quoted-example.yaml"));
        JsonNode rootNode = Yaml.mapper().readValue(yaml, JsonNode.class);
        OpenAPIV3Parser parser = new OpenAPIV3Parser();
        OpenAPI openAPI = (parser.parseJsonNode(null, rootNode)).getOpenAPI();

        Map<String, Schema> definitions = openAPI.getComponents().getSchemas();
        assertEquals("NoQuotePlease", definitions.get("CustomerType").getExample());
    }

    @Test(description = "Issue 855: Request Body internal refs are not being resolved")
    public void shouldParseRequestBody() {
        ParseOptions parseOptions = new ParseOptions();
        parseOptions.setResolveFully(true);
        OpenAPI openAPI = new OpenAPIV3Parser().read("src/test/resources/issue_855.yaml", null, parseOptions);
        Content actualComponentContent = openAPI.getComponents().getRequestBodies().get("ASinglePet").getContent();
        Content actualPathContent = openAPI.getPaths().get("/adopt").getPost().getRequestBody().getContent();
        Map properties = actualComponentContent.get("application/petstore+json").getSchema().getProperties();
        assertNotNull(properties);
        assertEquals(properties.size(), 2);
        assertNotNull(actualPathContent);
        assertEquals(actualPathContent, actualComponentContent);
    }

    @Test
    public void testIssue915() {
        ParseOptions parseOptions = new ParseOptions();
        parseOptions.setResolveFully(true);
        OpenAPI openAPI = new OpenAPIV3Parser().read("src/test/resources/issue_918.yaml", null, parseOptions);
        Map<String, Header> headers = openAPI.getPaths().get("/2.0/users/").getGet().getResponses().get("200").getHeaders();
        String description = headers.get("X-Rate-Limit").getDescription();
        assertEquals(description, "The number of allowed requests in the current period");
    }

    @Test
    public void testIssue931() {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setValidateInternalRefs(false);
        SwaggerParseResult result = new OpenAPIV3Parser().readLocation("Issue_931.json", null, options);
        assertNotNull(result.getOpenAPI());
        assertTrue(result.getMessages().size() > 0);
        assertEquals(result.getMessages().get(0).contains("doesn't adhere to regular expression ^[a-zA-Z0-9\\.\\-_]+$"), true);

    }

    @Test
    public void testIssue948() {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        SwaggerParseResult result = new OpenAPIV3Parser().readLocation("Issue_948.json", null, options);
        new OpenAPIResolver(result.getOpenAPI()).resolve();
        assertNotNull(result.getOpenAPI());
    }

    @Test
    public void shouldParseParameters() {
        ParseOptions parseOptions = new ParseOptions();
        parseOptions.setResolveFully(true);
        OpenAPI openAPI = new OpenAPIV3Parser().read("src/test/resources/issue_877.yaml", null, parseOptions);
        Parameter parameter = openAPI.getPaths().get("/adopt").getGet().getParameters().get(0);
        assertNotNull(parameter);
        assertEquals(parameter.getIn(), "path");
        assertEquals(parameter.getName(), "playerId");
    }

    @Test
    public void testIssue884() {
        ParseOptions parseOptions = new ParseOptions();
        parseOptions.setResolveFully(true);
        OpenAPI openAPI = new OpenAPIV3Parser().read("src/test/resources/issue_884.yaml", null, parseOptions);
        Map<String, Link> links = openAPI.getPaths().get("/2.0/repositories/{username}").getGet().getResponses().get("200").getLinks();
        Link userRepository = links.get("userRepository");
        String operationId = userRepository.getOperationId();
        assertEquals(operationId, "getRepository");
        assertNotNull(userRepository.getHeaders());
    }

    @Test
    public void testLinkIssue() {
        ParseOptions parseOptions = new ParseOptions();
        parseOptions.setResolveFully(true);
        OpenAPI openAPI = new OpenAPIV3Parser().read("src/test/resources/linkIssue.yaml", null, parseOptions);
        Map<String, Link> links = openAPI.getPaths().get("/2.0/repositories/{username}").getGet().getResponses().get("200").getLinks();
        Object requestBody = links.get("userRepository").getRequestBody();
        assertEquals(requestBody, "$response.body#/slug");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Schema> issue975ExtractPropertiesFromTestResource() {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        OpenAPI openAPI = new OpenAPIV3Parser().readLocation("issue-975/contract/openapi.yaml", null, options).getOpenAPI();
        Schema myResponseSchema = openAPI.getComponents().getSchemas().get("MyResponse");
        return myResponseSchema.getProperties();
    }

    @Test(description = "Test that relative references are resolvable when property is a reference to a relative file.")
    public void testIssue975_property() {
        Map<String, Schema> properties = issue975ExtractPropertiesFromTestResource();
        assertEquals(properties.get("images").get$ref(), "#/components/schemas/Image");
    }

    @Test(description = "Test that relative references are resolvable when property is an array with a reference to a relative file.")
    public void testIssue975_array() {
        Map<String, Schema> properties = issue975ExtractPropertiesFromTestResource();
        ArraySchema imagesArray = (ArraySchema) properties.get("imagesArray");
        assertEquals(imagesArray.getItems().get$ref(), "#/components/schemas/Image");
    }

    @Test(description = "Test that relative references are resolvable when property is a map with a reference to a relative file.")
    public void testIssue975_map() {
        Map<String, Schema> properties = issue975ExtractPropertiesFromTestResource();
        Schema imagesMap = (Schema) properties.get("imagesMap").getAdditionalProperties();
        assertEquals(imagesMap.get$ref(), "#/components/schemas/Image");
    }

    @Test(description = "Test that relative references are resolvable when property is an array with a map with a reference to a relative file.")
    public void testIssue975_array_map() {
        Map<String, Schema> properties = issue975ExtractPropertiesFromTestResource();
        ArraySchema imagesArray = (ArraySchema) properties.get("imagesArrayMap");
        Schema imagesdMap = (Schema) imagesArray.getItems().getAdditionalProperties();
        assertEquals(imagesdMap.get$ref(), "#/components/schemas/Image");
    }

    @Test(description = "Test that relative references are resolvable when property is a map with an array with a reference to a relative file.")
    public void testIssue975_map_array() {
        Map<String, Schema> properties = issue975ExtractPropertiesFromTestResource();
        ArraySchema imagesArray = (ArraySchema) properties.get("imagesMapArray").getAdditionalProperties();
        assertEquals(imagesArray.getItems().get$ref(), "#/components/schemas/Image");
    }

    @Test(description = "Test that relative references are resolvable when property is an array with an array with a reference to a relative file.")
    public void testIssue975_array_array() {
        Map<String, Schema> properties = issue975ExtractPropertiesFromTestResource();
        ArraySchema imagesArray = (ArraySchema) properties.get("imagesArrayArray");
        imagesArray = (ArraySchema) imagesArray.getItems();
        assertEquals(imagesArray.getItems().get$ref(), "#/components/schemas/Image");
    }

    @Test(description = "Test that relative references are resolvable when property is a map with a map with a reference to a relative file.")
    public void testIssue975_map_map() {
        Map<String, Schema> properties = issue975ExtractPropertiesFromTestResource();
        Schema imagesMap = (Schema) properties.get("imagesMapMap").getAdditionalProperties();
        imagesMap = (Schema) imagesMap.getAdditionalProperties();
        assertEquals(imagesMap.get$ref(), "#/components/schemas/Image");
    }

    @Test(description = "Test that relative references are resolvable when property is an oneOf a reference to a relative file.")
    public void testIssue975_oneOf() {
        Map<String, Schema> properties = issue975ExtractPropertiesFromTestResource();
        ComposedSchema composed = (ComposedSchema) properties.get("oneOfExample");
        assertEquals(composed.getOneOf().get(0).get$ref(), "#/components/schemas/Image");
    }

    @Test(description = "Test that relative references are resolvable when property is an anyOf a reference to a relative file.")
    public void testIssue975_anyOf() {
        Map<String, Schema> properties = issue975ExtractPropertiesFromTestResource();
        ComposedSchema composed = (ComposedSchema) properties.get("anyOfExample");
        assertEquals(composed.getAnyOf().get(0).get$ref(), "#/components/schemas/Image");
    }

    @Test(description = "Test that relative references are resolvable when property is an allOf a reference to a relative file.")
    public void testIssue975_allOf() {
        Map<String, Schema> properties = issue975ExtractPropertiesFromTestResource();
        ComposedSchema composed = (ComposedSchema) properties.get("allOfExample");
        assertEquals(composed.getAllOf().get(0).get$ref(), "#/components/schemas/Image");
    }

    @Test
    public void testValidationIssue() {
        ParseOptions parseOptions = new ParseOptions();
        parseOptions.setResolveFully(true);
        SwaggerParseResult result = new OpenAPIV3Parser().readLocation("src/test/resources/validation/path-parameter-validation.yaml", null, parseOptions);
        assertThat(result.getMessages().size(), CoreMatchers.is(0));
    }

    @Test
    public void shouldParseExternalSchemaModelHavingReferenceToItsLocalModel() {
        // given
        String location = "src/test/resources/issue-1040/api.yaml";
        OpenAPIV3Parser tested = new OpenAPIV3Parser();

        // when
        OpenAPI result = tested.read(location);

        // then
        Components components = result.getComponents();
        Schema modelSchema = components.getSchemas().get("Value");

        assertThat(modelSchema, notNullValue());
        assertThat(modelSchema.getProperties().get("id"), instanceOf(Schema.class));
        assertThat(((Schema) modelSchema.getProperties().get("id")).get$ref(), equalTo("#/components/schemas/ValueId"));
    }

    @Test(description = "Test that extensions can be found on the class classloader in addition to tccl.")
    public void testIssue1003_ExtensionsClassloader() {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        OpenAPI api = null;
        try {
            // Temporarily switch tccl to an unproductive cl
            final ClassLoader tcclTemp = new java.net.URLClassLoader(new URL[] {},
                    ClassLoader.getSystemClassLoader());
            Thread.currentThread().setContextClassLoader(tcclTemp);
            api = new OpenAPIV3Parser().read("src/test/resources/test.yaml");
        } finally {
            Thread.currentThread().setContextClassLoader(tccl);
        }
        assertNotNull(api);
    }

    @Test
    public void shouldParseApiWithMultipleParameterReferences() {
        // given
        String location = "src/test/resources/issue-1063/api.yaml";
        ParseOptions options = new ParseOptions();
        OpenAPIV3Parser tested = new OpenAPIV3Parser();

        // when
        SwaggerParseResult result = tested.readLocation(location, emptyList(), options);

        // then
        OpenAPI api = result.getOpenAPI();
        Map<String, Parameter> parameters = api.getComponents().getParameters();
        assertThat(parameters.keySet(), equalTo(new HashSet<>(asList("IdParam", "NameParam"))));
        assertThat(parameters.get("IdParam").getName(), equalTo("id"));
        assertThat(parameters.get("NameParam").getName(), equalTo("name"));

        assertThat(result.getMessages(), equalTo(emptyList()));

    }

    @Test
    public void shouldParseApiWithParametersUsingContentvsSchema() {
        // Tests that the content method of specifying the format of a parameter
        // gets resolved.
        // Test checks if an API's single parameter of array type gets fully resolved to
        // referenced definitions.
        String location = "src/test/resources/issue-1078/api.yaml";
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        // This test uses an Array in the parameters, test if it get's fully resolved.
        options.setResolveFully(true);
        OpenAPIV3Parser tested = new OpenAPIV3Parser();

        // Parse yaml
        SwaggerParseResult result = tested.readLocation(location, emptyList(), options);

        OpenAPI api = result.getOpenAPI();
        Paths paths = api.getPaths();

        // First ensure all schemas were resolved, this is important when this library
        // is used to generate code
        Components components = api.getComponents();
        assertNotNull(components);
        assertThat(components.getSchemas().size(), equalTo(4));
        assertNotNull(components.getSchemas().get("LocationType"));
        assertNotNull(components.getSchemas().get("Lat"));
        assertNotNull(components.getSchemas().get("Long"));
        assertNotNull(components.getSchemas().get("SearchResult"));

        PathItem apiEndpoint = paths.get("/api-endpoint-1");
        List<Parameter> parameters = apiEndpoint.getGet().getParameters();

        // Ensure there's only one parameter in this test
        assertThat(parameters.size(), equalTo(1));

        // We are testing content for a parameter so make sure its there.
        Content content = parameters.get(0).getContent();
        assertNotNull(content);
        // spec says only one content is permitted in 3.x
        assertThat( content.size(), equalTo(1));

        // Ensure there's a media type
        MediaType mediaType = content.entrySet().iterator().next().getValue();
        assertNotNull(mediaType);

        // This test has a single parameter of type array
        Schema parameterSchema = mediaType.getSchema();
        Assert.assertTrue(parameterSchema instanceof ArraySchema);
        ArraySchema arraySchema = (ArraySchema)parameterSchema;

        // Test if the item schema was resolved properly
        Schema itemSchema = arraySchema.getItems();
        assertNotNull(itemSchema);
        Assert.assertTrue(itemSchema instanceof ObjectSchema);

        // Ensure the referenced item's schema has been resolved.
        ObjectSchema objSchema = (ObjectSchema)itemSchema;
        Map<String, Schema> objectItemSchemas = objSchema.getProperties();
        assertThat( objectItemSchemas.size(), equalTo(2));
        Assert.assertTrue(objectItemSchemas.get("lat") instanceof IntegerSchema);
        Assert.assertTrue(objectItemSchemas.get("long") instanceof IntegerSchema);
    }

    @Test
    public void testIssue1063() {
        // given
        String location = "src/test/resources/issue-1063/openapi.yaml";
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        OpenAPIV3Parser tested = new OpenAPIV3Parser();

        // when
        SwaggerParseResult result = tested.readLocation(location, emptyList(), options);

        // then
        OpenAPI api = result.getOpenAPI();
        assertEquals(api.getPaths().get("/anPath").getGet().getParameters().get(0).getName(), "customer-id");
        assertEquals(api.getPaths().get("/anPath").getGet().getParameters().get(1).getName(), "unit-id");

        assertThat(result.getMessages(), equalTo(emptyList()));

    }

    @Test
    public void testIssue1177() {
        String path = "/issue-1177/swagger.yaml";

        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setResolveFully(true);
        options.setResolveResponses(true);

        OpenAPI openAPI = new OpenAPIV3Parser().readLocation(path, auths, options).getOpenAPI();

        // $ref response with $ref header
        ApiResponse petsListApiResponse = openAPI.getPaths().get("/update-pets").getPost().getResponses().get("200");
        assertNotNull(petsListApiResponse);

        Header sessionIdHeader = petsListApiResponse.getHeaders().get("x-session-id");
        assertNotNull(sessionIdHeader);

        Schema petsListSchema = openAPI.getComponents().getSchemas().get("PetsList");
        assertNotNull(petsListSchema);

        assertNotNull(openAPI.getComponents().getHeaders());
        Header sessionIdHeaderComponent = openAPI.getComponents().getHeaders().get("x-session-id");
        assertTrue(sessionIdHeader == sessionIdHeaderComponent);

        assertTrue(petsListApiResponse.getContent().get("application/json").getSchema() == petsListSchema);
    }

    @Test
    public void testResolveFullyMap() {
        ParseOptions options = new ParseOptions();
        options.setResolveFully(false);
        OpenAPI openAPI = new OpenAPIV3Parser().readLocation("resolve-fully-map.yaml", null, options).getOpenAPI();
        String yaml = Yaml.pretty(openAPI);
        assertTrue(yaml.contains("$ref"));

        options = new ParseOptions();
        options.setResolveFully(true);
        openAPI = new OpenAPIV3Parser().readLocation("resolve-fully-map.yaml", null, options).getOpenAPI();
        yaml = Yaml.pretty(openAPI);
        assertFalse(yaml.contains("$ref"));
    }

    @Test
    public void testParseOptionsSkipMatchesFalse() {
        final String location = "src/test/resources/skipMatches.yaml";

        final ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setFlatten(true);
        options.setSkipMatches(false);

        final OpenAPIV3Parser parserUnderTest = new OpenAPIV3Parser();

        final SwaggerParseResult result = parserUnderTest.readLocation(location, null, options);

        final OpenAPI openAPI = result.getOpenAPI();

        assertNotNull(openAPI);
        assertNotNull(openAPI.getComponents());
        assertNotNull(openAPI.getComponents().getSchemas());
        assertEquals(openAPI.getComponents().getSchemas().size(), 6);
    }

    @Test
    public void testParseOptionsSkipMatchesTrue() {
        final String location = "src/test/resources/skipMatches.yaml";

        final ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setFlatten(true);
        options.setSkipMatches(true);

        final OpenAPIV3Parser parserUnderTest = new OpenAPIV3Parser();
        final SwaggerParseResult result = parserUnderTest.readLocation(location, null, options);
        final OpenAPI openAPI = result.getOpenAPI();

        assertNotNull(openAPI);
        assertNotNull(openAPI.getComponents());
        assertNotNull(openAPI.getComponents().getSchemas());
        assertEquals(6, openAPI.getComponents().getSchemas().size());
    }

    @Test
    public void testIssue1335() {
        final ParseOptions options = new ParseOptions();
        options.setResolve(true);

        SwaggerParseResult result = new OpenAPIV3Parser()
                .readLocation("src/test/resources/issue1335.yaml", null, options);
        assertNotNull(result.getOpenAPI().getComponents().getExamples().get("ex1"));
    }

    @Test
    public void testEmptyQueryParameterExample() {
        final ParseOptions options = new ParseOptions();
        options.setResolve(true);

        SwaggerParseResult result = new OpenAPIV3Parser()
                .readLocation("src/test/resources/emptyQueryParameter.yaml", null, options);
        assertEquals("", result.getOpenAPI().getPaths().get("/foo").getGet().getParameters().get(0).getExample());
    }

    @Test
    public void testBlankQueryParameterExample() {
        final ParseOptions options = new ParseOptions();
        options.setResolve(true);

        SwaggerParseResult result = new OpenAPIV3Parser()
                .readLocation("src/test/resources/blankQueryParameter.yaml", null, options);
        assertEquals(" ", result.getOpenAPI().getPaths().get("/foo").getGet().getParameters().get(0).getExample());
    }

    @Test
    public void testRegressionIssue1236() {
        final ParseOptions options = new ParseOptions();
        options.setResolve(true);

        SwaggerParseResult result = new OpenAPIV3Parser()
                .readLocation("src/test/resources/testRegressionIssue1236.yaml", null, options);
        assertTrue(result.getMessages().size() == 0);

    }

    @Test
    public void testSampleParser() {
        final String location = "src/test/resources/issue-1211.json";

        final ParseOptions options = new ParseOptions();
        options.setResolve(true);

        final OpenAPIV3Parser parser = new OpenAPIV3Parser();
        final SwaggerParseResult result = parser.readLocation(location, null, options);
        System.out.println(result.getMessages());
        OpenAPI openAPI = result.getOpenAPI();

        assertNotNull(result.getOpenAPI());
        assertTrue(result.getMessages().size() > 0);
        assertEquals(result.getMessages().get(0).contains("components.schemas.Pet. writeOnly and readOnly are both present"), true);

    }

    @Test
    public void testDuplicateHttpStatusCodesJson() {
        final String location = "src/test/resources/duplicateHttpStatusCodes.json";

        final ParseOptions options = new ParseOptions();
        options.setResolve(true);

        final OpenAPIV3Parser parser = new OpenAPIV3Parser();
        final SwaggerParseResult result = parser.readLocation(location, null, options);
        List<String> messages = result.getMessages();
        assertEquals(1, messages.size());
        assertEquals(messages.get(0), "Duplicate field '200' in `src/test/resources/duplicateHttpStatusCodes.json`");

    }

    @Test
    public void testDuplicateHttpStatusCodesYaml() {
        final String location = "src/test/resources/duplicateHttpStatusCodes.yaml";

        final ParseOptions options = new ParseOptions();
        options.setResolve(true);

        final OpenAPIV3Parser parser = new OpenAPIV3Parser();
        final SwaggerParseResult result = parser.readLocation(location, null, options);
        List<String> messages = result.getMessages();
        assertEquals(1, messages.size());
        assertEquals(messages.get(0), "Duplicate field 200 in `src/test/resources/duplicateHttpStatusCodes.yaml`");

    }


    @Test
    public void testDiscriminatorSingleFileNoMapping() throws Exception {
        OpenAPI openAPI = new OpenAPIV3Parser().read("./discriminator-mapping-resolution/single-file-no-mapping.yaml");
        Assert.assertNotNull(openAPI);
        Schema cat = openAPI.getComponents().getSchemas().get("Cat");
        Assert.assertNotNull(cat);
    }

    @Test
    public void testDiscriminatorSeparateFileNoMapping() throws Exception {
        OpenAPI openAPI = new OpenAPIV3Parser().read("./discriminator-mapping-resolution/main-no-mapping.yaml");
        Assert.assertNotNull(openAPI);
        Schema cat = openAPI.getComponents().getSchemas().get("Cat");
        Assert.assertNull(cat); // FIXME, issue #970 still exists in this form
    }

    @Test
    public void testDiscriminatorSingleFilePlainMapping() throws Exception {
        OpenAPI openAPI = new OpenAPIV3Parser().read("./discriminator-mapping-resolution/single-file-plain-mapping.yaml");
        Assert.assertNotNull(openAPI);
        Schema cat = openAPI.getComponents().getSchemas().get("Cat");
        Assert.assertNotNull(cat);
    }

    @Test
    public void testDiscriminatorSeparateFilePlainMapping() throws Exception {
        OpenAPI openAPI = new OpenAPIV3Parser().read("./discriminator-mapping-resolution/main-plain-mapping.yaml");
        Assert.assertNotNull(openAPI);
        Schema cat = openAPI.getComponents().getSchemas().get("Cat");
        Assert.assertNotNull(cat);
    }

    @Test
    public void testDiscriminatorSingleFileInternalMapping() throws Exception {
        OpenAPI openAPI = new OpenAPIV3Parser().read("./discriminator-mapping-resolution/single-file-internal-mapping.yaml");
        Assert.assertNotNull(openAPI);
        Schema cat = openAPI.getComponents().getSchemas().get("Cat");
        Assert.assertNotNull(cat);
    }

    @Test
    public void testDiscriminatorSeparateFileInternalMapping() throws Exception {
        OpenAPI openAPI = new OpenAPIV3Parser().read("./discriminator-mapping-resolution/main-internal-mapping.yaml");
        Assert.assertNotNull(openAPI);
        Schema cat = openAPI.getComponents().getSchemas().get("Cat");
        Assert.assertNotNull(cat);
    }

    @Test
    public void testDiscriminatorSameFileExternalMapping() throws Exception {
        OpenAPI openAPI = new OpenAPIV3Parser().read("./discriminator-mapping-resolution/main-external-mapping.yaml");
        Assert.assertNotNull(openAPI);
        Schema cat = openAPI.getComponents().getSchemas().get("Cat");
        Assert.assertNotNull(cat);
    }

    @Test
    public void testDiscriminatorSeparateFileExternalMapping() throws Exception {
        OpenAPI openAPI = new OpenAPIV3Parser().read("./discriminator-mapping-resolution/main-external-mapping-3files.yaml");
        Assert.assertNotNull(openAPI);
        Schema cat = openAPI.getComponents().getSchemas().get("Cat");
        Assert.assertNotNull(cat);
    }


    // Relative external refs failed on windows due to \ issue
    @Test
    public void testRelativePathIssue1543() {
        OpenAPIV3Parser parser = new OpenAPIV3Parser();
        ParseOptions options = new ParseOptions();
        options.setResolveFully(true);
        options.setResolve(true);
        SwaggerParseResult readResult = parser.readLocation("src/test/resources/issue-1543/openapi.yaml", null, options);

        if (readResult.getMessages().size() > 0) {
            Assert.assertFalse(readResult.getMessages().get(0).contains("end -1"));
        }
    }

    @Test
    public void testIssue1540() throws Exception{
        OpenAPI openAPI = new OpenAPIV3Parser().read("./issue-1540/a.json");
        Assert.assertNotNull(openAPI);
        Map<String, Object> extensions = openAPI.getExtensions();
        Assert.assertNotNull(extensions);
        Assert.assertTrue(!extensions.isEmpty());
        Assert.assertNotNull(extensions.get("x-setting"));
        Map<String, Object> testPutExtensions = openAPI.getPaths().get("/test").getPut().getExtensions();
        Assert.assertNotNull(testPutExtensions);
        Assert.assertTrue(!testPutExtensions.isEmpty());
        Assert.assertNotNull(testPutExtensions.get("x-order"));
        Assert.assertEquals((String)testPutExtensions.get("x-order"),"2147483647");
    }

    @Test
    public void testIssue1592() throws Exception {
        File file = new File("src/test/resources/issue-1592.jar");
        URL url = new URL("jar:" + file.toURI() + "!/test.yaml");
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        SwaggerParseResult result = new OpenAPIV3Parser().readLocation(url.toString(), null, options);
        OpenAPI openAPI = result.getOpenAPI();
        assertNotNull(openAPI);
    }

    @Test
    public void testIssue1266() throws Exception{
        String yamlString = FileUtils.readFileToString(new File("src/test/resources/issue-1266/issue-1266.yaml"), "UTF-8");
        String yamlStringResolved = FileUtils.readFileToString(new File("src/test/resources/issue-1266/issue-1266-resolved.yaml"), "UTF-8");
        ParseOptions options = new ParseOptions();
        options.setResolveFully(true);
        options.setResolveCombinators(true);
        OpenAPI openAPI = new OpenAPIV3Parser().readContents(yamlString, null, options).getOpenAPI();
        Assert.assertNotNull(openAPI);
        assertEquals(Yaml.pretty(openAPI), yamlStringResolved);
    }

    @Test(description = "option true, adds Original Location to messages when ref is relative/local")
    public void testValidateExternalRefsTrue() {
        ParseOptions options = new ParseOptions();
        options.setValidateExternalRefs(true);
        options.setResolve(true);
        SwaggerParseResult result = new OpenAPIV3Parser().readLocation("./swos-443/root.yaml", null, options);
        OpenAPI openAPI = result.getOpenAPI();
        assertNotNull(openAPI);
        assertNotNull(result.getMessages());
        assertEquals(result.getMessages().size(), 20);
        assertTrue(result.getMessages().contains("attribute components.requestBodies.NewItem.asdasd is unexpected (./ref.yaml)"));
        assertTrue(result.getMessages().contains("attribute components.requestBodies.NewItem.descasdasdription is unexpected (./ref.yaml)"));
        assertTrue(result.getMessages().contains("attribute components.responses.GeneralError.descrsaiption is unexpected (./ref.yaml)"));
        assertTrue(result.getMessages().contains("attribute components.responses.GeneralError.asdas is unexpected (./ref.yaml)"));
        assertTrue(result.getMessages().contains("attribute components.responses.GeneralError.description is missing (./ref.yaml)"));
        assertTrue(result.getMessages().contains("attribute components.schemas.Examples.nonExpected is unexpected (./ref.yaml)"));
        assertTrue(result.getMessages().contains("attribute components.parameters.skipParam.[skip].in is not of type `[query|header|path|cookie]` (./ref.yaml)"));
        assertTrue(result.getMessages().contains("attribute components.securitySchemes.api_key.namex is unexpected (./ref.yaml)"));
        assertTrue(result.getMessages().contains("attribute components.securitySchemes.api_key.name is missing (./ref.yaml)"));
        assertTrue(result.getMessages().contains("attribute components.securitySchemes.api_key.in is not of type `cookie|header|query` (./ref.yaml)"));
        assertTrue(result.getMessages().contains("attribute components.callbacks.webhookVerificationEvent.postx is unexpected (./ref.yaml)"));
        assertTrue(result.getMessages().contains("attribute components.headers.X-Rate-Limit-Limit.descriptasdd is unexpected (./ref.yaml)"));
        assertTrue(result.getMessages().contains("attribute components.links.unsubscribe.parametersx is unexpected (./ref.yaml)"));
        assertTrue(result.getMessages().contains("attribute components.examples.response-example.summaryx is unexpected (./ref.yaml)"));
        assertTrue(result.getMessages().contains("components.examples.response-example. value and externalValue are both present (./ref.yaml)"));
        assertTrue(result.getMessages().contains("attribute components.callbacks.failed.wrongField is not of type `object` (./ref.yaml)"));
        assertTrue(result.getMessages().contains("attribute paths.~1refPet(get).responses is missing (./ref.yaml)"));

        //error message in main file
        assertTrue(result.getMessages().contains("attribute components.schemas.InvalidSchema.invalid is unexpected"));

        assertTrue(result.getMessages().contains("attribute components.schemas.ErrorModel.properties is not of type `object` (./ref.yaml)"));
        assertTrue(result.getMessages().contains("attribute components.schemas.Examples.properties is not of type `object` (./ref.yaml)"));

    }

    @Test(description = "test that a model in a folder that has a ref to a model in the classpath is properly resolved.")
    public void testIssue1891() {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setFlatten(true);

        SwaggerParseResult result = new OpenAPIV3Parser().readLocation("./issue-1891/openapi.yaml", null, options);
        assertTrue(result.getMessages().isEmpty());

        // Expect all references to be properly resolved to local references.
        OpenAPI openAPI = result.getOpenAPI();
        Schema<?> localModel = openAPI.getComponents().getSchemas().get("LocalModel");
        Schema<?> typesModel = openAPI.getComponents().getSchemas().get("TypesModel");
        Schema<?> sharedModel = openAPI.getComponents().getSchemas().get("SharedModel");

        assertEquals("#/components/schemas/TypesModel", localModel.getProperties().get("sharedModelField").get$ref());
        assertEquals("#/components/schemas/SharedModel", typesModel.get$ref());
        assertNotNull(sharedModel);
    }

    @Test(description = "directly parsed  definition, tested in previous method as reference relative/local ")
    public void testValidateDefinition() {
        ParseOptions options = new ParseOptions();
        options.setValidateExternalRefs(false);
        SwaggerParseResult result = new OpenAPIV3Parser().readLocation("./swos-443/ref.yaml", null, options);
        OpenAPI openAPI = result.getOpenAPI();
        assertNotNull(openAPI);
        assertNotNull(result.getMessages());
        assertTrue(result.getMessages().contains("attribute components.schemas.ErrorModel.properties is not of type `object`"));
        assertTrue(result.getMessages().contains("attribute components.schemas.Examples.properties is not of type `object`"));
    }

    @Test(description = "option false, does not add Original Location to messages when ref is relative/local")
    public void testValidateExternalRefsFalse() {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setValidateExternalRefs(false);
        SwaggerParseResult result = new OpenAPIV3Parser().readLocation("./swos-443/root.yaml", null, options);
        OpenAPI openAPI = result.getOpenAPI();
        assertNotNull(openAPI);
        //keeps error messages only from original spec
        assertTrue(result.getMessages().contains("attribute components.schemas.InvalidSchema.invalid is unexpected"));
        assertTrue(result.getMessages().contains("An exception was thrown while trying to deserialize the contents of ./ref.yaml into type class io.swagger.v3.oas.models.callbacks.Callback"));
    }

    @Test
    public void testNullExample() throws Exception{
        String yamlString = FileUtils.readFileToString(new File("src/test/resources/null-full-example.yaml"), "UTF-8");
        String yamlStringResolved = FileUtils.readFileToString(new File("src/test/resources/null-full-example-resolved.yaml"), "UTF-8");
        ParseOptions options = new ParseOptions();
        options.setResolveFully(true);
        options.setResolveCombinators(false);
        OpenAPI openAPI = new OpenAPIV3Parser().readContents(yamlString, null, options).getOpenAPI();
        Assert.assertNotNull(openAPI);
        assertEquals(Yaml.pretty(openAPI), yamlStringResolved);
    }

    @Test
    public void testInternalRefsValidation() throws Exception {
        String yamlString = FileUtils.readFileToString(new File("src/test/resources/internal-refs.yaml"), "UTF-8");
        ParseOptions options = new ParseOptions();
        SwaggerParseResult parseResult = new OpenAPIV3Parser().readContents(yamlString, null, options);
        assertEquals(parseResult.getMessages().size(), 1);
    }

    @Test
    public void testIssue1902_component_example_not_parse_ordinal_json() {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        SwaggerParseResult result = new OpenAPIV3Parser()
                .readLocation("src/test/resources/issue1902/1902-string-example-in-component.yaml",
                        null, options);
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getOpenAPI());
        OpenAPI openAPI = result.getOpenAPI();
        Example expectedExample = openAPI.getComponents().getExamples().get("Things");
        assertNotNull(expectedExample);
    }

    @Test
    public void testIssue_1746_headers_relative_paths() {
        OpenAPIV3Parser openApiParser = new OpenAPIV3Parser();
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        SwaggerParseResult parseResult = openApiParser.readLocation("issue-1746/petstore.yml", null, options);
        OpenAPI openAPI = parseResult.getOpenAPI();
        assertEquals(openAPI.getPaths().get("/pets").getGet().getResponses().get("200").getHeaders().get("x-next").get$ref(), "#/components/headers/LocationInHeaders");
    }

    @Test(description = "Test safe resolving")
    public void test31SafeURLResolving() {
        ParseOptions parseOptions = new ParseOptions();
        parseOptions.setResolveFully(true);
        parseOptions.setSafelyResolveURL(true);
        List<String> allowList = Collections.emptyList();
        List<String> blockList = Collections.emptyList();
        parseOptions.setRemoteRefAllowList(allowList);
        parseOptions.setRemoteRefBlockList(blockList);

        SwaggerParseResult result = new OpenAPIV3Parser().readLocation("safeResolving/oas30SafeUrlResolvingWithPetstore.yaml", null, parseOptions);
        if (result.getMessages() != null) {
            for (String message : result.getMessages()) {
                assertTrue(message.contains("Server returned HTTP response code: 403"));
            }
        }
    }

    @Test(description = "Test safe resolving with blocked URL")
    public void test31SafeURLResolvingWithBlockedURL() {
        ParseOptions parseOptions = new ParseOptions();
        parseOptions.setResolveFully(true);
        parseOptions.setSafelyResolveURL(true);
        List<String> allowList = Collections.emptyList();
        List<String> blockList = Arrays.asList("petstore3.swagger.io");
        parseOptions.setRemoteRefAllowList(allowList);
        parseOptions.setRemoteRefBlockList(blockList);

        SwaggerParseResult result = new OpenAPIV3Parser().readLocation("safeResolving/oas30SafeUrlResolvingWithPetstore.yaml", null, parseOptions);

        if (result.getMessages() != null) {
            for (String message : result.getMessages()) {
                assertTrue(
                        message.contains("Server returned HTTP response code: 403") ||
                                message.contains("URL is part of the explicit denylist. URL [https://petstore3.swagger.io/api/v3/openapi.json]"));
            }
        }
    }

    @Test(description = "Test safe resolving with turned off safelyResolveURL option")
    public void test31SafeURLResolvingWithTurnedOffSafeResolving() {
        ParseOptions parseOptions = new ParseOptions();
        parseOptions.setResolveFully(true);
        parseOptions.setSafelyResolveURL(false);
        List<String> allowList = Collections.emptyList();
        List<String> blockList = Arrays.asList("petstore3.swagger.io");
        parseOptions.setRemoteRefAllowList(allowList);
        parseOptions.setRemoteRefBlockList(blockList);

        SwaggerParseResult result = new OpenAPIV3Parser().readLocation("safeResolving/oas30SafeUrlResolvingWithPetstore.yaml", null, parseOptions);
        if (result.getMessages() != null) {
            for (String message : result.getMessages()) {
                assertTrue(message.contains("Server returned HTTP response code: 403"));
            }
        }
    }

    @Test(description = "Test safe resolving with localhost and blocked url")
    public void test31SafeURLResolvingWithLocalhostAndBlockedURL() {
        ParseOptions parseOptions = new ParseOptions();
        parseOptions.setResolveFully(true);
        parseOptions.setSafelyResolveURL(true);

        SwaggerParseResult result = new OpenAPIV3Parser().readLocation("safeResolving/oas30SafeUrlResolvingWithLocalhost.yaml", null, parseOptions);
        if (result.getMessages() != null) {
            for (String message : result.getMessages()) {
                assertTrue(
                        message.contains("Server returned HTTP response code: 403") ||
                                message.contains("IP is restricted"));
            }
        }
    }

    @Test(description = "Test safe resolving with localhost url")
    public void test31SafeURLResolvingWithLocalhost() {
        ParseOptions parseOptions = new ParseOptions();
        parseOptions.setResolveFully(true);
        parseOptions.setSafelyResolveURL(true);
        List<String> blockList = Arrays.asList("petstore.swagger.io");
        parseOptions.setRemoteRefBlockList(blockList);

        String error = "URL is part of the explicit denylist. URL [https://petstore.swagger.io/v2/swagger.json]";
        SwaggerParseResult result = new OpenAPIV3Parser().readLocation("safeResolving/oas30SafeUrlResolvingWithLocalhost.yaml", null, parseOptions);

        if (result.getMessages() != null) {
            for (String message : result.getMessages()) {
                assertTrue(
                        message.contains("Server returned HTTP response code: 403") ||
                                message.contains("IP is restricted") ||
                                message.contains(error)
                );
            }
        }
    }

    @Test
    public void testIssue1886() {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setFlatten(true);
        OpenAPIV3Parser openApiParser = new OpenAPIV3Parser();
        SwaggerParseResult parseResult = openApiParser.readLocation("issue-1886/openapi.yaml", null, options);
        OpenAPI openAPI = parseResult.getOpenAPI();
        assertEqualsNoOrder(
            openAPI.getComponents().getSchemas().keySet(),
            Arrays.asList("ArrayPojo", "Enum1", "Enum2", "Enum3", "MapPojo", "SetPojo", "SimplePojo",
                "TransactionsPatchRequestBody", "additional-properties", "array-pojo", "locale-translation-item",
                "map-pojo", "set-pojo", "simple-pojo", "translation-item")
        );
    }

    @Test
    public void testIssue2081() {
        ParseOptions options = new ParseOptions();
        options.setResolveFully(true);
        OpenAPIV3Parser openApiParser = new OpenAPIV3Parser();
        SwaggerParseResult parseResult = openApiParser.readLocation("issue2081/openapi.yaml", null, options);
        OpenAPI openAPI = parseResult.getOpenAPI();
        Yaml.prettyPrint(openAPI);
        assertEquals(openAPI.getComponents().getSchemas().get("PetCreate").getRequired().size(), 1);
        assertEquals(openAPI.getComponents().getSchemas().get("PetCreate").getProperties().size(), 2);
    }

    @Test(description = "responses should be inline")
    public void testFullyResolveResponses() {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setResolveResponses(true);
        OpenAPIV3Parser openApiParser = new OpenAPIV3Parser();
        SwaggerParseResult parseResult = openApiParser.readLocation("resolve-responses-test.yaml", null, options);
        OpenAPI openAPI = parseResult.getOpenAPI();
        assertNull(openAPI.getPaths().get("/users").getGet().getResponses().get("400").get$ref());
        assertNull(openAPI.getPaths().get("/users").getPost().getResponses().get("400").get$ref());
        assertNull(openAPI.getPaths().get("/users").getPost().getResponses().get("422").get$ref());
        assertNull(openAPI.getPaths().get("/users/{userId}").getGet().getResponses().get("404").get$ref());
        assertNull(openAPI.getPaths().get("/users/{userId}").getPut().getResponses().get("400").get$ref());
        assertNull(openAPI.getPaths().get("/users/{userId}").getPut().getResponses().get("404").get$ref());
        assertNull(openAPI.getPaths().get("/users/{userId}").getDelete().getResponses().get("400").get$ref());
        assertNull(openAPI.getPaths().get("/users/{userId}").getDelete().getResponses().get("404").get$ref());
    }

    @Test(description = "responses should not be inline")
    public void testResolveResponsesRef() {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        OpenAPIV3Parser openApiParser = new OpenAPIV3Parser();
        SwaggerParseResult parseResult = openApiParser.readLocation("resolve-responses-test.yaml", null, options);
        OpenAPI openAPI = parseResult.getOpenAPI();
        assertEquals(openAPI.getPaths().get("/users").getGet().getResponses().get("400").get$ref(), "#/components/responses/BadRequest");
        assertEquals(openAPI.getPaths().get("/users").getPost().getResponses().get("400").get$ref(), "#/components/responses/BadRequest");
        assertEquals(openAPI.getPaths().get("/users").getPost().getResponses().get("422").get$ref(), "#/components/responses/UnprocessableEntity");
        assertEquals(openAPI.getPaths().get("/users/{userId}").getGet().getResponses().get("404").get$ref(), "#/components/responses/NotFound");
        assertEquals(openAPI.getPaths().get("/users/{userId}").getPut().getResponses().get("400").get$ref(), "#/components/responses/BadRequest");
        assertEquals(openAPI.getPaths().get("/users/{userId}").getPut().getResponses().get("404").get$ref(), "#/components/responses/NotFound");
        assertEquals(openAPI.getPaths().get("/users/{userId}").getDelete().getResponses().get("400").get$ref(), "#/components/responses/BadRequest");
        assertEquals(openAPI.getPaths().get("/users/{userId}").getDelete().getResponses().get("404").get$ref(), "#/components/responses/NotFound");
    }

    @Test
    public void testResolveOASWithFlatten(){
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setFlatten(true);
        OpenAPIV3Parser openApiParser = new OpenAPIV3Parser();
        SwaggerParseResult parseResult = openApiParser.readLocation("resolve-flatten-SH-configuration-test.yaml", null, options);
        OpenAPI openAPI = parseResult.getOpenAPI();
        assertNull(openAPI.getComponents().getSchemas().get("#/components/schemas/inline_response_404"));
        assertNull(openAPI.getComponents().getSchemas().get("#/components/schemas/inline_response_200"));
    }

    @Test(description = "responses should be inline with using resolveFully = true")
    public void testResolveFullyResponses(){
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setResolveFully(true);
        OpenAPIV3Parser openApiParser = new OpenAPIV3Parser();
        SwaggerParseResult parseResult = openApiParser.readLocation("resolve-responses-test.yaml", null, options);
        OpenAPI openAPI = parseResult.getOpenAPI();
        assertNull(openAPI.getPaths().get("/users").getGet().getResponses().get("400").get$ref());

    }
}
