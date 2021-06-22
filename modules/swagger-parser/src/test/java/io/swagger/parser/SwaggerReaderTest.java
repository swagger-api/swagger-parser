package io.swagger.parser;

import io.swagger.matchers.SerializationMatchers;
import io.swagger.models.*;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.parameters.PathParameter;
import io.swagger.models.parameters.QueryParameter;
import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.RefProperty;
import io.swagger.parser.util.SwaggerDeserializationResult;
import io.swagger.util.ResourceUtils;
import org.testng.annotations.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;

import static org.testng.Assert.*;

public class SwaggerReaderTest {
    @Test(description = "it should read the uber api with file scheme")
    public void readUberApiFromFile() {
        final SwaggerParser parser = new SwaggerParser();
        java.nio.file.Path currentRelativePath = Paths.get("");
        String curPath = currentRelativePath.toAbsolutePath().toString();
        final Swagger swagger = parser.read("file:///" + curPath + "/src/test/resources/uber.json");
        assertNotNull(swagger);
    }

    @Test(description = "it should read the uber api with file scheme and spaces")
    public void readUberApiFromFileWithSpaces() {
        final SwaggerParser parser = new SwaggerParser();
        java.nio.file.Path currentRelativePath = Paths.get("");
        String curPath = currentRelativePath.toAbsolutePath().toString();
        final Swagger swagger = parser.read("file:///" + curPath + "/src/test/resources/s%20p%20a%20c%20e%20s/uber.json");
        assertNotNull(swagger);
    }

    @Test(description = "it should read the uber api from Path URI")
    public void readUberApiFromPathUri() {
        final SwaggerParser parser = new SwaggerParser();
        java.nio.file.Path uberPath = Paths.get("src/test/resources/uber.json");
        final Swagger swagger = parser.read(uberPath.toUri().toString());
        assertNotNull(swagger);
    }

    @Test(description = "it should read the uber api from File URI")
    public void readUberApiFromFileUri() {
        final SwaggerParser parser = new SwaggerParser();
        java.io.File uberFile = new java.io.File("src/test/resources/uber.json");
        final Swagger swagger = parser.read(uberFile.toURI().toString());
        assertNotNull(swagger);
    }

    @Test(description = "it should read the uber api with url string without file scheme")
    public void readUberApiFromFileNoScheme() {
        final SwaggerParser parser = new SwaggerParser();
        final Swagger swagger = parser.read("src/test/resources/uber.json");
        assertNotNull(swagger);
    }

    @Test(description = "it should read the uber api")
    public void readUberApi() {
        final SwaggerParser parser = new SwaggerParser();
        final Swagger swagger = parser.read("uber.json");
        assertNotNull(swagger);
    }

    @Test(description = "it should read the simple example with minimum values")
    public void readSimpleExampleWithMinimumValues() {
        final SwaggerParser parser = new SwaggerParser();
        final Swagger swagger = parser.read("sampleWithMinimumValues.yaml");
        final QueryParameter qp = (QueryParameter) swagger.getPaths().get("/pets").getGet().getParameters().get(0);
        assertEquals(qp.getMinimum(), new BigDecimal("0.0"));
    }

    @Test(description = "it should read the simple example with model extensions")
    public void readSimpleExampleWithModelExtensions() {
        final SwaggerParser parser = new SwaggerParser();
        final Swagger swagger = parser.read("sampleWithMinimumValues.yaml");
        final Model model = swagger.getDefinitions().get("Cat");
        assertNotNull(model.getVendorExtensions().get("x-extension-here"));
    }

    @Test(description = "it should detect yaml")
    public void detectYaml() {
        final SwaggerParser parser = new SwaggerParser();
        final Swagger swagger = parser.read("minimal_y");
        assertEquals(swagger.getSwagger(), "2.0");
    }

    @Test(description = "it should detect json")
    public void detectJson() {
        final SwaggerParser parser = new SwaggerParser();
        final Swagger swagger = parser.read("minimal_j");
        assertEquals(swagger.getSwagger(), "2.0");
    }

    @Test(description = "it should read the issue 16 resource")
    public void testIssue16() {
        final SwaggerParser parser = new SwaggerParser();
        final Swagger swagger = parser.read("issue_16.yaml");
        assertNotNull(swagger);
    }

    @Test(description = "it should test https://github.com/swagger-api/swagger-codegen/issues/469")
    public void testIssue469() {
        final SwaggerParser parser = new SwaggerParser();
        final Swagger swagger = parser.read("issue_469.json");
        final String expectedJson = "{" +
                "  \"id\" : 12345," +
                "  \"name\" : \"Gorilla\"" +
                "}";
        SerializationMatchers.assertEqualsToJson(swagger.getDefinitions().get("Pet").getExample(), expectedJson);
    }

    @Test(description = "it should read the issue 59 resource")
    public void testIssue59() throws IOException {
        final SwaggerParser parser = new SwaggerParser();
        final String path = "uber.json";

        final Swagger swaggerFromString = parser.parse(ResourceUtils.loadClassResource(getClass(), path));
        final Swagger swaggerFromFile = parser.read(path);

        assertEquals(swaggerFromFile, swaggerFromString);
    }

    @Test
    public void testIssueSwaggerUi1792() throws Exception {
        final SwaggerParser parser = new SwaggerParser();
        final String path = "thing.json";

        final Swagger swaggerFromString = parser.parse(ResourceUtils.loadClassResource(getClass(), path));

        Model thing = swaggerFromString.getDefinitions().get("Thing");
        assertTrue(thing instanceof ComposedModel);

        ComposedModel composedModel = (ComposedModel) thing;
        List<Model> models = composedModel.getAllOf();
        assertTrue(models.size() == 2);
        assertTrue(models.get(0) instanceof RefModel);
        assertTrue(models.get(1) instanceof ModelImpl);

        Model thingSummary = swaggerFromString.getDefinitions().get("ThingSummary");
        assertTrue(thingSummary instanceof ModelImpl);
    }

    @Test
    public void testIssue207() throws Exception {
        String spec = "{\n" +
                "  \"swagger\": \"2.0\",\n" +
                "  \"paths\": {\n" +
                "    \"/foo\": {\n" +
                "      \"get\": {\n" +
                "        \"parameters\": {},\n" +
                "        \"responses\": {\n" +
                "          \"200\": {\n" +
                "            \"description\": \"successful operation\",\n" +
                "            \"schema\": {\n" +
                "              \"type\": \"array\",\n" +
                "              \"items\": {\n" +
                "                \"$ref\": \"#/definitions/Pet\"\n" +
                "              }\n" +
                "            }\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  },\n" +
                "  \"definitions\": {\n" +
                "    \"Pet\": {\n" +
                "      \"type\": \"object\",\n" +
                "      \"properties\": {\n" +
                "        \"name\": {\n" +
                "          \"type\": \"string\"\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}";

        SwaggerDeserializationResult result = new SwaggerParser().readWithInfo(spec);

        assertNotNull(result);
        Response response = result.getSwagger().getPath("/foo").getGet().getResponses().get("200");
        assertNotNull(response);
        Property schema = response.getSchema();
        assertTrue(schema instanceof ArrayProperty);
        ArrayProperty ap = (ArrayProperty) schema;
        assertTrue(ap.getItems() instanceof RefProperty);
    }

    @Test
    public void testIssue208() throws Exception {
        String spec = "{\n" +
                "  \"swagger\": \"2.0\",\n" +
                "  \"paths\": {},\n" +
                "  \"definitions\": {\n" +
                "    \"Dog\": {\n" +
                "      \"title\": \"Dog\",\n" +
                "      \"type\": \"object\",\n" +
                "      \"allOf\": [\n" +
                "        {\n" +
                "          \"$ref\": \"#/definitions/Pet\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"x-color\": \"red\"\n" +
                "    }\n" +
                "  }\n" +
                "}";

        SwaggerDeserializationResult result = new SwaggerParser().readWithInfo(spec);

        assertNotNull(result);
        Swagger swagger = result.getSwagger();

        assertNotNull(swagger);
        Model dog = swagger.getDefinitions().get("Dog");
        assertNotNull(dog);
        assertTrue(dog instanceof ComposedModel);
        ComposedModel cm = (ComposedModel) dog;
        assertEquals(cm.getTitle(), "Dog");
        assertTrue(cm.getAllOf().size() == 1);
        assertTrue(cm.getAllOf().get(0) instanceof RefModel);
        assertNotNull(cm.getVendorExtensions());
        assertEquals(cm.getVendorExtensions().get("x-color"), "red");
    }

    @Test(description = "issue 206, not supported yet")
    public void testIssue206() {
        String spec =
            "swagger: '2.0'\n" +
            "paths: {}\n" +
            "definitions:\n" +
            "  Model:\n" +
            "    properties:\n" +
            "      name:\n" +
            "        type: string\n" +
            "        required: true";

        SwaggerDeserializationResult result = new SwaggerParser().readWithInfo(spec);
        // TODO: when 206 is resolved, enable the assertions here
    }

    @Test
    public void testIssue205() {
        String spec =
            "swagger: '2.0'\n" +
            "info:\n" +
            "  title: nice\n" +
            "paths: {}\n" +
            "definitions:\n" +
            "  Empty:\n" +
            "    type: string\n" +
            "    description: 'Expected empty response could be {}'";

        SwaggerDeserializationResult result = new SwaggerParser().readWithInfo(spec);
        assertTrue(result.getMessages().size() == 0);

        Swagger swagger = result.getSwagger();
        Model definition = swagger.getDefinitions().get("Empty");
        assertNotNull(definition);
        assertTrue(definition instanceof ModelImpl);
    }

    @Test
    public void testIssue136() {
        String spec =
            "swagger: '2.0'\n" +
            "info:\n" +
            "  title: issue 136\n" +
            "paths:\n" +
            "  /foo:\n" +
            "    get:\n" +
            "      parameters: []\n" +
            "      responses:\n" +
            "        200:\n" +
            "          description: 'the pet'\n" +
            "          schema:\n" +
            "            $ref: 'https://petstore.swagger.io/v2/swagger.json#/definitions/Pet'";

        SwaggerDeserializationResult result = new SwaggerParser().readWithInfo(spec);

        Swagger swagger = result.getSwagger();
        Property property = swagger.getPath("/foo").getGet().getResponses().get("200").getSchema();
        assertNotNull(property);
        assertTrue(property instanceof RefProperty);
    }

    @Test
    public void testIssue192() {
        String spec =
                "swagger: '2.0'\n" +
                "info:\n" +
                "  title: issue 192\n" +
                "paths:\n" +
                "  /foo:\n" +
                "    get:\n" +
                "      parameters:\n" +
                "        - name: Code\n" +
                "          in: path\n" +
                "          description: The code\n" +
                "          required: true\n" +
                "          type: string\n" +
                "          minLength: 4\n" +
                "          maxLength: 5\n" +
                "          pattern: '^[a-zA-Z]'\n" +
                "      responses:\n" +
                "        200:\n" +
                "          description: 'the pet'";

        SwaggerDeserializationResult result = new SwaggerParser().readWithInfo(spec);

        Swagger swagger = result.getSwagger();
        Parameter param = swagger.getPath("/foo").getGet().getParameters().get(0);
        assertTrue(param instanceof PathParameter);
        PathParameter pathParameter = (PathParameter) param;

        assertEquals(pathParameter.getMinLength(), new Integer(4));
        assertEquals(pathParameter.getMaxLength(), new Integer(5));
        assertNull(pathParameter.isReadOnly());
        assertEquals(pathParameter.getPattern(), "^[a-zA-Z]");
    }

    @Test
    public void testIssue277() {
        final SwaggerParser parser = new SwaggerParser();
        final Swagger swagger = parser.read("issue_277.yaml");

        Path path = swagger.getPath("/buckets/{bucketKey}/details");
        assertNotNull(path);
        Operation get = path.getGet();
        assertNotNull(get);
        assertTrue(get.getParameters().size() == 1);
        Parameter param1 = get.getParameters().get(0);
        assertEquals(param1.getIn(), "path");
        assertEquals(param1.getName(), "bucketKey");
    }

    @Test
    public void testIssue364() {
        String spec =
            "swagger: '2.0'\n" +
            "info:\n" +
            "  title: issue 192\n" +
            "paths:\n" +
            "  /foo:\n" +
            "    get:\n" +
            "      parameters:\n" +
            "        - name: Code\n" +
            "          in: query\n" +
            "          description: The code\n" +
            "          required: true\n" +
            "          readOnly: true\n" +
            "          allowEmptyValue: true\n" +
            "      responses:\n" +
            "        200:\n" +
            "          description: 'the pet'";

        SwaggerDeserializationResult result = new SwaggerParser().readWithInfo(spec);

        Swagger swagger = result.getSwagger();
        Parameter param = swagger.getPath("/foo").getGet().getParameters().get(0);
        assertTrue(param instanceof QueryParameter);
        QueryParameter pathParameter = (QueryParameter) param;

        assertTrue(pathParameter.isReadOnly());
        assertTrue(pathParameter.getAllowEmptyValue());
    }

    @Test(description = "it should read an example within an inlined schema")
    public void testIssue1261InlineSchemaExample() {
        final SwaggerParser parser = new SwaggerParser();
        final Swagger swagger = parser.read("issue-1261.yaml");

        Path path = swagger.getPath("/user");
        assertNotNull(path);
        Operation get = path.getGet();
        assertNotNull(get);
        assertTrue(get.getResponses().size() == 1);
        Response response = get.getResponses().get("200");
        Property schema = response.getSchema();
        Object example = schema.getExample();
        assertNotNull(example);
        assertTrue(example instanceof LinkedHashMap);
        LinkedHashMap exampleMap = (LinkedHashMap) example;
        assertEquals(Integer.parseInt(exampleMap.get("id").toString()), 42);
        assertEquals(exampleMap.get("name").toString(), "Arthur Dent");
    }
}
