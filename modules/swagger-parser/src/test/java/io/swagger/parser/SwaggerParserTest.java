package io.swagger.parser;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.models.ArrayModel;
import io.swagger.models.ComposedModel;
import io.swagger.models.Model;
import io.swagger.models.ModelImpl;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.RefModel;
import io.swagger.models.Response;
import io.swagger.models.Swagger;
import io.swagger.models.auth.ApiKeyAuthDefinition;
import io.swagger.models.auth.OAuth2Definition;
import io.swagger.models.auth.SecuritySchemeDefinition;
import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.parameters.FormParameter;
import io.swagger.models.parameters.HeaderParameter;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.parameters.PathParameter;
import io.swagger.models.parameters.QueryParameter;
import io.swagger.models.parameters.RefParameter;
import io.swagger.models.parameters.SerializableParameter;
import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.ByteArrayProperty;
import io.swagger.models.properties.ComposedProperty;
import io.swagger.models.properties.IntegerProperty;
import io.swagger.models.properties.MapProperty;
import io.swagger.models.properties.ObjectProperty;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.RefProperty;
import io.swagger.models.properties.StringProperty;
import io.swagger.parser.util.TestUtils;
import io.swagger.parser.util.SwaggerDeserializationResult;
import io.swagger.parser.util.TestUtils;
import io.swagger.util.Json;
import io.swagger.util.Yaml;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.testng.reporters.Files;

import java.io.File;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public class SwaggerParserTest {

    @Test
    public void testIssueRelativeRefs2(){
        String location = "exampleSpecs/specs/my-domain/test-api/v1/test-api-swagger_v1.json";
        Swagger swagger = new SwaggerParser().read(location, null, true);
        assertNotNull(swagger);
        Map<String, Model> definitions = swagger.getDefinitions();
        Assert.assertTrue(definitions.get("confirmMessageType_v01").getProperties().get("resources") instanceof ArrayProperty);
        ArrayProperty arraySchema = (ArrayProperty) definitions.get("confirmMessageType_v01").getProperties().get("resources");
        ObjectProperty prop = (ObjectProperty) arraySchema.getItems();
        RefProperty refProperty = (RefProperty) prop.getProperties().get("resourceID");
        assertEquals(refProperty.get$ref(),"#/definitions/simpleIDType_v01");
    }

    @Test
    public void testIssue111() {
        SwaggerDeserializationResult result = new SwaggerParser().readWithInfo("issue-111.yaml", null, true);
        assertTrue(result.getMessages().get(0).equals("attribute definitions.Filter.items is missing"));
        assertNotNull(result.getSwagger());
    }

    @Test
    public void testIssueDefinitionWithDots_2() {
        Swagger swagger = new SwaggerParser().read("SimpleAPI.yaml");
        assertNotNull(swagger);
    }

    @Test
    public void testIssueDefinitionWithDots() {
        Swagger swagger = new SwaggerParser().read("API-Service-2.0.0-swagger.yaml");
        assertNotNull(swagger);
    }

    @Test
    public void testIssue995() {
        Swagger swagger = new SwaggerParser().read("issue-995/digitalExp-Product-Unresolved.yaml");
        assertNotNull(swagger);
        assertTrue(swagger.getDefinitions().size() == 6);
        assertNotNull(swagger.getDefinitions().get("MobileProduct"));
        assertNotNull(swagger.getDefinitions().get("FixedVoiceProduct"));
        assertNotNull(swagger.getDefinitions().get("InternetProduct"));
    }

    @Test
    public void testIssue927() {
        Swagger swagger = new SwaggerParser().read("issue-927/issue-927.yaml");
        assertNotNull(swagger);
        assertTrue(swagger.getDefinitions().size() == 3);
        assertNotNull(swagger.getDefinitions().get("Pet"));
        assertNotNull(swagger.getDefinitions().get("Cat"));
        assertNotNull(swagger.getDefinitions().get("Dog"));
    }

    @Test
    public void testIssue901_2() {
        Swagger swagger = new SwaggerParser().read("issue-901/spec2.yaml");
        assertNotNull(swagger);
        assertNotNull(swagger.getDefinitions());
        ArrayProperty arraySchema = (ArrayProperty) swagger.getDefinitions().get("Test.Definition").getProperties().get("stuff");
        String internalRef = ((RefProperty) arraySchema.getItems()).get$ref();
        assertEquals(internalRef,"#/definitions/TEST.THING.OUT.Stuff");
    }

    @Test
    public void testIssue901() {
        Swagger swagger = new SwaggerParser().read("issue-901/spec.yaml");
        assertNotNull(swagger);
        String internalRef = ((RefModel)swagger.getPaths().get("/test").getPut().getResponses().get("200").getResponseSchema()).get$ref();
        assertEquals(internalRef,"#/definitions/Test.Definition");
        assertNotNull(swagger.getDefinitions());

    }

    @Test
    public void testIssue435() {
        Swagger swagger = new SwaggerParser().read("issue-435/main.yaml");
        assertNotNull(swagger.getDefinitions().get("sub"));
        assertNotNull(swagger.getDefinitions().get("subsub"));
    }


    @Test
    public void testIssue845() {
        SwaggerDeserializationResult swaggerDeserializationResult = new SwaggerParser().readWithInfo("");
        assertEquals(swaggerDeserializationResult.getMessages().get(0), "empty or null swagger supplied");
    }
  
    @Test
    public void testIssue834() {
        Swagger swagger = new SwaggerParser().read("issue-834/index.yaml", null, true);
        assertNotNull(swagger);

        Response foo200 =swagger.getPaths().get("/foo").getGet().getResponses().get("200");
        assertNotNull(foo200);
        RefModel model200 = (RefModel) foo200.getResponseSchema();
        String foo200SchemaRef = model200.get$ref();
        assertEquals(foo200SchemaRef, "#/definitions/schema");

        Response foo300 = swagger.getPaths().get("/foo").getGet().getResponses().get("300");
        assertNotNull(foo300);
        RefModel model300 = (RefModel) foo300.getResponseSchema();
        String foo300SchemaRef = model300.get$ref();
        assertEquals(foo300SchemaRef, "#/definitions/schema");

        Response bar200 = swagger.getPaths().get("/bar").getGet().getResponses().get("200");
        assertNotNull(bar200);
        RefModel modelBar200 = (RefModel) bar200.getResponseSchema();
        String bar200SchemaRef = modelBar200.get$ref();
        assertEquals(bar200SchemaRef, "#/definitions/schema");
    }

    @Test
    public void testIssue811_RefSchema_ToRefSchema() {
        final Swagger swagger = new SwaggerParser().read("oapi-reference-test2/index.yaml", null, true);
        Assert.assertNotNull(swagger);
        RefModel model = (RefModel) swagger.getPaths().get("/").getGet().getResponses().get("200").getResponseSchema();
        Assert.assertEquals(model.get$ref() ,"#/definitions/schema-with-reference");
    }

    @Test
    public void testIssue811() throws Exception {
        SwaggerParser parser = new SwaggerParser();
        final Swagger swagger = parser.read("src/test/resources/oapi-reference-test/index.yaml");
        Assert.assertNotNull(swagger);
        assertTrue(swagger.getPaths().get("/").getGet().getResponses().get("200").getResponseSchema() instanceof RefModel);
        RefModel model = (RefModel) swagger.getPaths().get("/").getGet().getResponses().get("200").getResponseSchema();
        Assert.assertEquals(model.get$ref(),"#/definitions/schema-with-reference");

    }

    @Test
    public void testIssue727() throws Exception {
        SwaggerParser parser = new SwaggerParser();
        final Swagger swagger = parser.read("src/test/resources/issue-714/serviceA.yaml");
        Assert.assertNotNull(swagger);
        assertNotNull(swagger.getPaths().get("/test").getGet().getParameters().get(0));
        assertTrue(swagger.getDefinitions().size() == 1);
        assertNotNull(swagger.getDefinitions().get("refA"));
    }

    @Test
    public void testIssue704() throws Exception {
        SwaggerParser parser = new SwaggerParser();
        final Swagger swagger = parser.read("src/test/resources/sample/swagger.json");
        Assert.assertNotNull(swagger);

        assertNotNull(swagger.getPaths().get("/api/Address").getGet());
        assertTrue(swagger.getDefinitions().size() == 1);
        assertNotNull(swagger.getDefinitions().get("AddressEx"));
    }

    @Test
    public void testIssue697() throws Exception {
        String yaml = "{\n" +
                "    \"swagger\": \"2.0\",\n" +
                "    \"info\": {\n" +
                "        \"version\": \"1.0\",\n" +
                "        \"title\": \"x-example\"\n" +
                "    },\n" +
                "    \"host\": \"httpbin.org\",\n" +
                "    \"basePath\": \"/anything\",\n" +
                "    \"schemes\": [\n" +
                "        \"http\"\n" +
                "    ],\n" +
                "    \"paths\": {\n" +
                "        \"/{foo}\": {\n" +
                "            \"get\": {\n" +
                "                \"responses\": {\n" +
                "                    \"200\": {\n" +
                "                        \"description\": \"OK\"\n" +
                "                    }\n" +
                "                },\n" +
                "                \"deprecated\": false\n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "}";

        SwaggerParser parser = new SwaggerParser();

        Swagger swagger = parser.parse(yaml);
        assertFalse(swagger.getPaths().get("/{foo}").getGet().isDeprecated());

    }

    @Test
    public void testNPEIssue684() throws Exception {
        SwaggerParser parser = new SwaggerParser();
        final Swagger swagger = parser.read("src/test/resources/issue-684.json");
        Assert.assertNotNull(swagger);
        assertTrue(swagger.getParameters().get("ParamDef3") instanceof RefParameter);
    }

    @Test
    public void testRefPaths() throws Exception {
        String yaml = "swagger: '2.0'\n" +
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

        SwaggerParser parser = new SwaggerParser();

        Swagger swagger = parser.parse(yaml);

        assertEquals(swagger.getPaths().get("foo"),swagger.getPaths().get("foo2"));
        

    }
    @Test
    public void testModelParameters() throws Exception {
        String yaml = "swagger: '2.0'\n" +
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

        SwaggerParser parser = new SwaggerParser();

        Swagger swagger = parser.parse(yaml);

    }

    @Test
    public void testParseSharedPathParameters() throws Exception {
        String yaml =
                "swagger: '2.0'\n" +
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

        SwaggerParser parser = new SwaggerParser();

        Swagger swagger = parser.parse(yaml);
        List<Parameter> parameters = swagger.getPath("/persons/{id}").getGet().getParameters();
        assertTrue(parameters.size() == 2);
        Parameter id = parameters.get(0);
        assertEquals(id.getDescription(), "yes");
    }

    @Test
    public void testParseRefPathParameters() throws Exception {
        String yaml =
                "swagger: '2.0'\n" +
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
                        "            $ref: '#/definitions/report'\n" +
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
        SwaggerParser parser = new SwaggerParser();
        Swagger swagger = parser.parse(yaml);
    }

    @Test
    public void testUniqueParameters() throws Exception {
        String yaml =
                "swagger: '2.0'\n" +
                        "info:\n" +
                        "  title: test\n" +
                        "  version: '0.0.0'\n" +
                        "parameters:\n" +
                        "  foo-id:\n" +
                        "    name: id\n" +
                        "    in: path\n" +
                        "    type: string\n" +
                        "    required: true\n" +
                        "paths:\n" +
                        "  /foos/{id}:\n" +
                        "    parameters:\n" +
                        "        - $ref: '#/parameters/foo-id'\n" +
                        "    get:\n" +
                        "      responses:\n" +
                        "        200:\n" +
                        "          schema:\n" +
                        "            $ref: '#/definitions/foo'\n" +
                        "    put:\n" +
                        "      parameters:\n" +
                        "        - name: foo\n" +
                        "          in: body\n" +
                        "          required: true\n" +
                        "          schema:\n" +
                        "            $ref: '#/definitions/foo'\n" +
                        "      responses:\n" +
                        "        200:\n" +
                        "          schema:\n" +
                        "            $ref: '#/definitions/foo'\n" +
                        "definitions:\n" +
                        "  foo:\n" +
                        "    type: object\n" +
                        "    properties:\n" +
                        "      id:\n" +
                        "        type: string\n" +
                        "    required:\n" +
                        "      - id\n";
        SwaggerParser parser = new SwaggerParser();
        Swagger swagger = parser.parse(yaml);
        List<Parameter> parameters = swagger.getPath("/foos/{id}").getPut().getParameters();
        assertTrue(parameters.size() == 2);
    }

    @Test
    public void testLoadRelativeFileTree_Json() throws Exception {
        final Swagger swagger = doRelativeFileTest("src/test/resources/relative-file-references/json/parent.json");
        //Json.mapper().writerWithDefaultPrettyPrinter().writeValue(new File("resolved.json"), swagger);
    }



    @Test
    public void testPetstore() throws Exception {
        SwaggerParser parser = new SwaggerParser();
        SwaggerDeserializationResult result = parser.readWithInfo("src/test/resources/petstore.json", null, true);

        assertNotNull(result);
        assertTrue(result.getMessages().isEmpty());

        Swagger swagger = result.getSwagger();
        Map<String, Model> definitions = swagger.getDefinitions();
        Set<String> expectedDefinitions = new HashSet<String>();
        expectedDefinitions.add("User");
        expectedDefinitions.add("Category");
        expectedDefinitions.add("Pet");
        expectedDefinitions.add("Tag");
        expectedDefinitions.add("Order");
        expectedDefinitions.add("PetArray");
        assertEquals(definitions.keySet(), expectedDefinitions);

        Model petModel = definitions.get("Pet");
        Set<String> expectedPetProps = new HashSet<String>();
        expectedPetProps.add("id");
        expectedPetProps.add("category");
        expectedPetProps.add("name");
        expectedPetProps.add("photoUrls");
        expectedPetProps.add("tags");
        expectedPetProps.add("status");
        assertEquals(petModel.getProperties().keySet(), expectedPetProps);

        ArrayModel petArrayModel = (ArrayModel) definitions.get("PetArray");
        assertEquals(petArrayModel.getType(), "array");
        RefProperty refProp = (RefProperty) petArrayModel.getItems();
        assertEquals(refProp.get$ref(), "#/definitions/Pet");
        assertNull(petArrayModel.getProperties());
    }

    @Test
    public void testFileReferenceWithVendorExt() throws Exception {
        SwaggerParser parser = new SwaggerParser();
        final Swagger swagger = parser.read("src/test/resources/file-reference-with-vendor-ext/b.yaml");
        Map<String, Model> definitions = swagger.getDefinitions();
        assertTrue(definitions.get("z").getVendorExtensions().get("x-foo") instanceof Map);
        assertEquals(((Map) definitions.get("z").getVendorExtensions().get("x-foo")).get("bar"), "baz");
        assertTrue(definitions.get("x").getVendorExtensions().get("x-foo") instanceof Map);
        assertEquals(((Map) definitions.get("x").getVendorExtensions().get("x-foo")).get("bar"), "baz");
    }

    @Test
    public void testTroublesomeFile() throws Exception {
        SwaggerParser parser = new SwaggerParser();
        final Swagger swagger = parser.read("src/test/resources/troublesome.yaml");
    }

    @Test
    public void testLoadRelativeFileTree_Yaml() throws Exception {
        JsonToYamlFileDuplicator.duplicateFilesInYamlFormat("src/test/resources/relative-file-references/json",
                "src/test/resources/relative-file-references/yaml");
        final Swagger swagger = doRelativeFileTest("src/test/resources/relative-file-references/yaml/parent.yaml");
        assertNotNull(Yaml.mapper().writeValueAsString(swagger));
        assertTrue(swagger.getParameters().get("param2") instanceof HeaderParameter);
    }

    @Test
    public void testLoadnestedExternalResponseReferencesFile_Yaml() throws Exception {
        final Swagger swagger = doRelativeResponseFileTest("src/test/resources/nested-external-response-references/swagger-root.yaml");
        assertNotNull(Yaml.mapper().writeValueAsString(swagger));
    }
    
    @Test
    public void testLoadRecursiveExternalDef() throws Exception {
        SwaggerParser parser = new SwaggerParser();
        final Swagger swagger = parser.read("src/test/resources/file-reference-to-recursive-defs/b.yaml");

        Map<String, Model> definitions = swagger.getDefinitions();
        assertEquals(((RefProperty) ((ArrayProperty) definitions.get("v").getProperties().get("children")).getItems()).get$ref(), "#/definitions/v");
        assertTrue(definitions.containsKey("y"));
        assertEquals(((RefProperty) ((ArrayProperty) definitions.get("x").getProperties().get("children")).getItems()).get$ref(), "#/definitions/y");
    }

    @Test
    public void testLoadNestedItemsReferences() {
        SwaggerParser parser = new SwaggerParser();
        SwaggerDeserializationResult result = parser.readWithInfo("src/test/resources/nested-items-references/b.yaml", null, true);
        Swagger swagger = result.getSwagger();
        Map<String, Model> definitions = swagger.getDefinitions();
        assertTrue(definitions.containsKey("z"));
        assertTrue(definitions.containsKey("w"));
    }

    @Test
    public void testIssue75() {
        SwaggerParser parser = new SwaggerParser();
        final Swagger swagger = parser.read("src/test/resources/issue99.json");

        BodyParameter param = (BodyParameter) swagger.getPaths().get("/albums").getPost().getParameters().get(0);
        Model model = param.getSchema();

        assertNotNull(model);
        assertTrue(model instanceof ArrayModel);

        ArrayModel am = (ArrayModel) model;
        assertTrue(am.getItems() instanceof ByteArrayProperty);
        assertEquals(am.getItems().getFormat(), "byte");
    }

    @Test(enabled = false, description = "see https://github.com/swagger-api/swagger-parser/issues/337")
    public void testIssue62() {
        SwaggerParser parser = new SwaggerParser();
        final Swagger swagger = parser.read("https://raw.githubusercontent.com/OAI/OpenAPI-Specification/master/fixtures/v2.0/json/resources/resourceWithLinkedDefinitions.json");

        assertNotNull(swagger.getPaths().get("/pets/{petId}").getGet());
    }

    @Test
    public void testIssue146() {
        SwaggerParser parser = new SwaggerParser();
        final Swagger swagger = parser.read("src/test/resources/issue_146.yaml");
        assertNotNull(swagger);
        QueryParameter p = ((QueryParameter) swagger.getPaths().get("/checker").getGet().getParameters().get(0));
        StringProperty pp = (StringProperty) p.getItems();
        assertTrue("registration".equalsIgnoreCase(pp.getEnum().get(0)));
    }

    @Test(description = "Test (path & form) parameter's required attribute")
    public void testParameterRequired() {
        SwaggerParser parser = new SwaggerParser();
        final Swagger swagger = parser.read("src/test/resources/petstore.json");
        final List<Parameter> operationParams = swagger.getPath("/pet/{petId}").getPost().getParameters();

        final PathParameter pathParameter = (PathParameter) operationParams.get(0);
        Assert.assertTrue(pathParameter.getRequired());

        final FormParameter formParameter = (FormParameter) operationParams.get(1);
        Assert.assertFalse(formParameter.getRequired());
    }

    @Test(description = "Test consumes and produces in top level and operation level")
    public void testConsumesAndProduces() {
        SwaggerParser parser = new SwaggerParser();
        final Swagger swagger = parser.read("src/test/resources/consumes_and_produces");
        Assert.assertNotNull(swagger);

        // test consumes and produces at spec level
        Assert.assertEquals(swagger.getConsumes(), Arrays.asList("application/json"));
        Assert.assertEquals(swagger.getProduces(), Arrays.asList("application/xml"));

        // test consumes and produces at operation level
        Assert.assertEquals(swagger.getPath("/pets").getPost().getConsumes(), Arrays.asList("image/jpeg"));
        Assert.assertEquals(swagger.getPath("/pets").getPost().getProduces(), Arrays.asList("image/png"));

        // test empty consumes and produces at operation level
        Assert.assertEquals(swagger.getPath("/pets").getGet().getConsumes(), Collections.<String>emptyList());
        Assert.assertEquals(swagger.getPath("/pets").getGet().getProduces(), Collections.<String>emptyList());

        // test consumes and produces not defined at operation level
        Assert.assertNull(swagger.getPath("/pets").getPatch().getConsumes());
        Assert.assertNull(swagger.getPath("/pets").getPatch().getProduces());

    }

    @Test
    public void testIssue108() {
        SwaggerParser parser = new SwaggerParser();
        final Swagger swagger = parser.read("src/test/resources/issue_108.json");

        assertNotNull(swagger);
    }

    @Test
    public void testIssue() {
        String yaml =
                "swagger: '2.0'\n" +
                        "info:\n" +
                        "  description: 'No description provided.'\n" +
                        "  version: '2.0'\n" +
                        "  title: 'My web service'\n" +
                        "  x-endpoint-name: 'default'\n" +
                        "paths:\n" +
                        "  x-nothing: 'sorry not supported'\n" +
                        "  /foo:\n" +
                        "    x-something: 'yes, it is supported'\n" +
                        "    get:\n" +
                        "      parameters: []\n" +
                        "      responses:\n" +
                        "        200:\n" +
                        "          description: 'Swagger API document for this service'\n" +
                        "x-some-vendor:\n" +
                        "  sometesting: 'bye!'";
        SwaggerParser parser = new SwaggerParser();
        SwaggerDeserializationResult result = parser.readWithInfo(yaml);

        Swagger swagger = result.getSwagger();

        assertEquals(((Map) swagger.getVendorExtensions().get("x-some-vendor")).get("sometesting"), "bye!");
        assertEquals(swagger.getPath("/foo").getVendorExtensions().get("x-something"), "yes, it is supported");
        assertTrue(result.getMessages().size() == 1);
        assertEquals(result.getMessages().get(0), "attribute paths.x-nothing is unsupported");
    }

    @Test
    public void testIssue292WithNoCollectionFormat() {
        String yaml =
                "swagger: '2.0'\n" +
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
                        "      responses:\n" +
                        "        200:\n" +
                        "          description: Successful response";
        SwaggerParser parser = new SwaggerParser();
        SwaggerDeserializationResult result = parser.readWithInfo(yaml);

        Swagger swagger = result.getSwagger();

        Parameter param = swagger.getPaths().get("/persons").getGet().getParameters().get(0);
        QueryParameter qp = (QueryParameter) param;
        assertNull(qp.getCollectionFormat());
    }

    @Test
    public void testIssue292WithCSVCollectionFormat() {
        String yaml =
                "swagger: '2.0'\n" +
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
                        "        collectionFormat: csv\n" +
                        "      responses:\n" +
                        "        200:\n" +
                        "          description: Successful response";
        SwaggerParser parser = new SwaggerParser();
        SwaggerDeserializationResult result = parser.readWithInfo(yaml);

        Swagger swagger = result.getSwagger();

        Parameter param = swagger.getPaths().get("/persons").getGet().getParameters().get(0);
        QueryParameter qp = (QueryParameter) param;
        assertEquals(qp.getCollectionFormat(), "csv");
    }
    
    @Test
    public void testIssue286() {
        SwaggerParser parser = new SwaggerParser();

        Swagger swagger = parser.read("issue_286.yaml");
        Property response = swagger.getPath("/").getGet().getResponses().get("200").getSchema();
        assertTrue(response instanceof RefProperty);
        assertEquals(((RefProperty) response).getSimpleRef(), "issue_286_PetList");
        assertNotNull(swagger.getDefinitions().get("issue_286_Allergy"));
    }

    @Test
    public void testIssue286WithModel() {
        SwaggerParser parser = new SwaggerParser();

        Swagger swagger = parser.read("issue_286.yaml");
        Model response = swagger.getPath("/").getGet().getResponses().get("200").getResponseSchema();
        assertTrue(response instanceof RefModel);
        assertEquals( "issue_286_PetList", ((RefModel) response).getSimpleRef());
        assertNotNull(swagger.getDefinitions().get("issue_286_Allergy"));
    }

    @Test
    public void testIssue360() {
        SwaggerParser parser = new SwaggerParser();
        final Swagger swagger = parser.read("src/test/resources/issue_360.yaml");
        assertNotNull(swagger);

        Parameter parameter = swagger.getPath("/pets").getPost().getParameters().get(0);
        assertNotNull(parameter);
        assertTrue(parameter instanceof BodyParameter);

        BodyParameter bp = (BodyParameter) parameter;

        assertNotNull(bp.getSchema());
        Model model = bp.getSchema();
        assertTrue(model instanceof ModelImpl);

        ModelImpl impl = (ModelImpl) model;
        assertNotNull(impl.getProperties().get("foo"));

        Map<String, Object> extensions = bp.getVendorExtensions();
        assertNotNull(extensions);

        assertNotNull(extensions.get("x-examples"));
        Object o = extensions.get("x-examples");
        assertTrue(o instanceof Map);

        Map<String, Object> on = (Map<String, Object>) o;

        Object jn = on.get("application/json");
        assertTrue(jn instanceof Map);

        Map<String, Object> objectNode = (Map<String, Object>) jn;
        assertEquals(objectNode.get("foo"), "bar");

        Parameter stringBodyParameter = swagger.getPath("/otherPets").getPost().getParameters().get(0);

        assertTrue(stringBodyParameter instanceof BodyParameter);
        BodyParameter sbp = (BodyParameter) stringBodyParameter;
        assertTrue(sbp.getRequired());
        assertEquals(sbp.getName(), "simple");

        Model sbpModel = sbp.getSchema();
        assertTrue(sbpModel instanceof ModelImpl);
        ModelImpl sbpModelImpl = (ModelImpl) sbpModel;

        assertEquals(sbpModelImpl.getType(), "string");
        assertEquals(sbpModelImpl.getFormat(), "uuid");

        Parameter refBodyParameter = swagger.getPath("/evenMorePets").getPost().getParameters().get(0);

        assertTrue(refBodyParameter instanceof BodyParameter);
        BodyParameter ref = (BodyParameter) refBodyParameter;
        assertTrue(ref.getRequired());
        assertEquals(ref.getName(), "simple");

        Model refModel = ref.getSchema();
        assertTrue(refModel instanceof RefModel);
        RefModel refModelImpl = (RefModel) refModel;

        assertEquals(refModelImpl.getSimpleRef(), "Pet");
    }

    private Swagger doRelativeFileTest(String location) {
        SwaggerParser parser = new SwaggerParser();
        SwaggerDeserializationResult readResult = parser.readWithInfo(location, null, true);
        if (readResult.getMessages().size() > 0) {
            Json.prettyPrint(readResult.getMessages());
        }
        final Swagger swagger = readResult.getSwagger();

        final Path path = swagger.getPath("/health");
        assertEquals(path.getClass(), Path.class); //we successfully converted the RefPath to a Path

        final List<Parameter> parameters = path.getParameters();
        assertParamDetails(parameters, 0, QueryParameter.class, "param1", "query");
        assertParamDetails(parameters, 1, HeaderParameter.class, "param2", "header");

        final Operation operation = path.getGet();
        final List<Parameter> operationParams = operation.getParameters();
        assertParamDetails(operationParams, 0, PathParameter.class, "param3", "path");
        assertParamDetails(operationParams, 1, HeaderParameter.class, "param4", "header");
        assertParamDetails(operationParams, 2, BodyParameter.class, "body", "body");
        final BodyParameter bodyParameter = (BodyParameter) operationParams.get(2);
        assertEquals(((RefModel) bodyParameter.getSchema()).get$ref(), "#/definitions/health");

        final Map<String, Response> responsesMap = operation.getResponses();

        assertResponse(swagger, responsesMap, "200", "Health information from the server", "#/definitions/health");
        assertResponse(swagger, responsesMap, "400", "Your request was not valid", "#/definitions/error");
        assertResponse(swagger, responsesMap, "500", "An unexpected error occur during processing", "#/definitions/error");

        final Map<String, Model> definitions = swagger.getDefinitions();
        final ModelImpl refInDefinitions = (ModelImpl) definitions.get("refInDefinitions");
        assertEquals(refInDefinitions.getDescription(), "The example model");
        expectedPropertiesInModel(refInDefinitions, "foo", "bar");

        final ArrayModel arrayModel = (ArrayModel) definitions.get("arrayModel");
        final RefProperty arrayModelItems = (RefProperty) arrayModel.getItems();
        assertEquals(arrayModelItems.get$ref(), "#/definitions/foo");

        final ModelImpl fooModel = (ModelImpl) definitions.get("foo");
        assertEquals(fooModel.getDescription(), "Just another model");
        expectedPropertiesInModel(fooModel, "hello", "world");

        final ComposedModel composedCat = (ComposedModel) definitions.get("composedCat");
        final ModelImpl child = (ModelImpl) composedCat.getChild();
        expectedPropertiesInModel(child, "huntingSkill", "prop2", "reflexes", "reflexMap");
        final ArrayProperty reflexes = (ArrayProperty) child.getProperties().get("reflexes");
        final RefProperty reflexItems = (RefProperty) reflexes.getItems();
        assertEquals(reflexItems.get$ref(), "#/definitions/reflex");
        assertTrue(definitions.containsKey(reflexItems.getSimpleRef()));

        final MapProperty reflexMap = (MapProperty) child.getProperties().get("reflexMap");
        final RefProperty reflexMapAdditionalProperties = (RefProperty) reflexMap.getAdditionalProperties();
        assertEquals(reflexMapAdditionalProperties.get$ref(), "#/definitions/reflex");

        assertEquals(composedCat.getInterfaces().size(), 2);
        assertEquals(composedCat.getInterfaces().get(0).get$ref(), "#/definitions/pet");
        assertEquals(composedCat.getInterfaces().get(1).get$ref(), "#/definitions/foo_2");

        return swagger;
    }

    private Swagger doRelativeResponseFileTest(String location) {
        SwaggerParser parser = new SwaggerParser();
        SwaggerDeserializationResult readResult = parser.readWithInfo(location, null, true);
        
        if (readResult.getMessages().size() > 0) {
            Json.prettyPrint(readResult.getMessages());
        }
        final Swagger swagger = readResult.getSwagger();
        
        Json.prettyPrint(swagger);
        
        final Path path = swagger.getPath("/users");
        assertEquals(path.getClass(), Path.class); //we successfully converted the RefPath to a Path

        final Operation operation = path.getGet();

        final Map<String, Response> responsesMap = operation.getResponses();

        assertResponse(swagger, responsesMap, "200", "OK", "#/definitions/User");

        final Map<String, Model> definitions = swagger.getDefinitions();
        final ModelImpl refInDefinitions = (ModelImpl) definitions.get("UserX");
        expectedPropertiesInModel(refInDefinitions, "address");

        final ModelImpl refInDefinitionsAddress = (ModelImpl) definitions.get("Address");
        expectedPropertiesInModel(refInDefinitionsAddress, "postal", "country");

        final ModelImpl refInDefinitionsCountry = (ModelImpl) definitions.get("Country");
        expectedPropertiesInModel(refInDefinitionsCountry, "name");

        final ModelImpl refInDefinitionsAddress_2 = (ModelImpl) definitions.get("Address_2");
        expectedPropertiesInModel(refInDefinitionsAddress_2, "postal", "country");

        final ModelImpl refInDefinitionsCountry_2 = (ModelImpl) definitions.get("Country_2");
        expectedPropertiesInModel(refInDefinitionsCountry_2, "name");        
        
        return swagger;
    }
    
    
    private void expectedPropertiesInModel(ModelImpl model, String... expectedProperties) {
        assertEquals(model.getProperties().size(), expectedProperties.length);
        for (String expectedProperty : expectedProperties) {
            assertTrue(model.getProperties().containsKey(expectedProperty));
        }
    }

    private void assertResponse(Swagger swagger, Map<String, Response> responsesMap, String responseCode,
                                String expectedDescription, String expectedSchemaRef) {
        final Response response = responsesMap.get(responseCode);
        final RefProperty schema = (RefProperty) response.getSchema();
        assertEquals(response.getDescription(), expectedDescription);
        assertEquals(schema.getClass(), RefProperty.class);
        assertEquals(schema.get$ref(), expectedSchemaRef);
        assertTrue(swagger.getDefinitions().containsKey(schema.getSimpleRef()));
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
        SwaggerParser parser = new SwaggerParser();
        final Swagger swagger = parser.read("src/test/resources/relative-file-references/json/parent.json");
        assertTrue(swagger.getDefinitions().containsKey("externalArray"));
        assertTrue(swagger.getDefinitions().containsKey("referencedByLocalArray"));
        assertTrue(swagger.getDefinitions().containsKey("externalObject"));
        assertTrue(swagger.getDefinitions().containsKey("referencedByLocalElement"));
        assertTrue(swagger.getDefinitions().containsKey("referencedBy"));
        assertEquals(((RefProperty)swagger.getDefinitions().get("externalObject").getProperties().get("hello1")).get$ref(),
                "#/definitions/referencedByLocalElement"); //issue #434
    }

    @Test
    public void testCodegenPetstore() {
        SwaggerParser parser = new SwaggerParser();
        final Swagger swagger = parser.read("src/test/resources/petstore-codegen.yaml");
        ModelImpl enumModel = (ModelImpl) swagger.getDefinitions().get("Enum_Test");
        assertNotNull(enumModel);
        Property enumProperty = enumModel.getProperties().get("enum_integer");
        assertNotNull(enumProperty);

        assertTrue(enumProperty instanceof IntegerProperty);
        IntegerProperty enumIntegerProperty = (IntegerProperty) enumProperty;
        List<Integer> integers = enumIntegerProperty.getEnum();
        assertEquals(integers.get(0), new Integer(1));
        assertEquals(integers.get(1), new Integer(-1));

        Operation getOrderOperation = swagger.getPath("/store/order/{orderId}").getGet();
        assertNotNull(getOrderOperation);
        Parameter orderId = getOrderOperation.getParameters().get(0);
        assertTrue(orderId instanceof PathParameter);
        PathParameter orderIdPathParam = (PathParameter) orderId;
        assertNotNull(orderIdPathParam.getMinimum());

        BigDecimal minimum = orderIdPathParam.getMinimum();
        assertEquals(minimum.toString(), "1");

        FormParameter formParam = (FormParameter) swagger.getPath("/fake").getPost().getParameters().get(3);

        assertEquals(formParam.getMinimum().toString(), "32.1");
    }

    @Test
    public void testIssue339() throws Exception {
        SwaggerParser parser = new SwaggerParser();
        final Swagger swagger = parser.read("src/test/resources/issue-339.json");

        Parameter param = swagger.getPath("/store/order/{orderId}").getGet().getParameters().get(0);
        assertTrue(param instanceof PathParameter);
        PathParameter pp = (PathParameter) param;

        assertTrue(pp.getMinimum().toString().equals("1"));
        assertTrue(pp.getMaximum().toString().equals("5"));
    }

    @Test
    public void testCodegenIssue4555() throws Exception {
        SwaggerParser parser = new SwaggerParser();
        String yaml = "swagger: '2.0'\n" +
                "\n" +
                "info:\n" +
                "  title: test\n" +
                "  version: \"0.0.1\"\n" +
                "\n" +
                "schemes:\n" +
                "  - http\n" +
                "produces:\n" +
                "  - application/json\n" +
                "\n" +
                "paths:\n" +
                "  /contents/{id}:\n" +
                "    parameters:\n" +
                "      - name: id\n" +
                "        in: path\n" +
                "        description: test\n" +
                "        required: true\n" +
                "        type: integer\n" +
                "\n" +
                "    get:\n" +
                "      description: test\n" +
                "      responses:\n" +
                "        200:\n" +
                "          description: OK\n" +
                "          schema:\n" +
                "            $ref: '#/definitions/Content'\n" +
                "\n" +
                "definitions:\n" +
                "  Content:\n" +
                "    type: object\n" +
                "    title: \t\ttest";
        final SwaggerDeserializationResult result = parser.readWithInfo(yaml);

        // can't parse with tabs!
        assertNull(result.getSwagger());
    }

    @Test
    public void testConverterIssue17() throws Exception {
        String yaml =
                "swagger: '2.0'\n" +
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
                        "        collectionFormat: csv\n" +
                        "      responses:\n" +
                        "        200:\n" +
                        "          description: Successful response\n"+
                        "          schema:\n" +
                        "            $ref: '#/definitions/Content'\n" +
                        "definitions:\n" +
                                "  Content:\n" +
                                "    type: object";
        SwaggerDeserializationResult result = new SwaggerParser().readWithInfo(yaml, Boolean.FALSE);

        assertNotNull(result.getSwagger());
        assertEquals(((RefProperty) result.getSwagger().getPaths().
                get("/persons").getGet().getResponses().get("200")
                .getSchema()).get$ref(), "#/definitions/Content");
    }

    @Test
    public void testIssue393() {
        SwaggerParser parser = new SwaggerParser();

        String yaml =
                "swagger: '2.0'\n" +
                        "info:\n" +
                        "  title: x\n" +
                        "  version: 1.0.0\n" +
                        "paths:\n" +
                        "  /test:\n" +
                        "    get:\n" +
                        "      parameters: []\n" +
                        "      responses:\n" +
                        "        '400':\n" +
                        "          description: |\n" +
                        "            The account could not be created because a credential didn't meet the complexity requirements.\n" +
                        "          x-error-refs:\n" +
                        "            - '$ref': '#/x-error-defs/credentialTooShort'\n" +
                        "            - '$ref': '#/x-error-defs/credentialTooLong'\n" +
                        "x-error-defs:\n" +
                        "  credentialTooShort:\n" +
                        "    errorID: credentialTooShort";
        final SwaggerDeserializationResult result = parser.readWithInfo(yaml);

        assertNotNull(result.getSwagger());
        Swagger swagger = result.getSwagger();

        assertNotNull(swagger.getVendorExtensions().get("x-error-defs"));
    }

    @Test
    public void testBadFormat() throws Exception {
        SwaggerParser parser = new SwaggerParser();
        final Swagger swagger = parser.read("src/test/resources/bad_format.yaml");

        Path path = swagger.getPath("/pets");

        Parameter parameter = path.getGet().getParameters().get(0);
        assertNotNull(parameter);
        assertTrue(parameter instanceof QueryParameter);
        QueryParameter queryParameter = (QueryParameter) parameter;
        assertEquals(queryParameter.getName(), "query-param-int32");
        assertNotNull(queryParameter.getEnum());
        assertEquals(queryParameter.getEnum().size(), 3);
        List<Object> enumValues = queryParameter.getEnumValue();
        assertEquals(enumValues.get(0), 1);
        assertEquals(enumValues.get(1), 2);
        assertEquals(enumValues.get(2), 7);

        parameter = path.getGet().getParameters().get(1);
        assertNotNull(parameter);
        assertTrue(parameter instanceof QueryParameter);
        queryParameter = (QueryParameter) parameter;
        assertEquals(queryParameter.getName(), "query-param-invalid-format");
        assertNotNull(queryParameter.getEnum());
        assertEquals(queryParameter.getEnum().size(), 3);
        enumValues = queryParameter.getEnumValue();
        assertEquals(enumValues.get(0), 1);
        assertEquals(enumValues.get(1), 2);
        assertEquals(enumValues.get(2), 7);

        parameter = path.getGet().getParameters().get(2);
        assertNotNull(parameter);
        assertTrue(parameter instanceof QueryParameter);
        queryParameter = (QueryParameter) parameter;
        assertEquals(queryParameter.getName(), "query-param-collection-format-and-uniqueItems");
        assertEquals(queryParameter.getCollectionFormat(), "multi");
        assertEquals(queryParameter.isUniqueItems(), true);
    }
    @Test
    public void testNumberAttributes() throws Exception {
        SwaggerParser parser = new SwaggerParser();
        Swagger swagger = parser.read(TestUtils.getResourceAbsolutePath("/number_attributes.yaml"));

        ModelImpl numberType = (ModelImpl)swagger.getDefinitions().get("NumberType");
        assertNotNull(numberType);
        assertNotNull(numberType.getEnum());
        assertEquals(numberType.getEnum().size(), 2);
        List<String> numberTypeEnumValues = numberType.getEnum();
        assertEquals(numberTypeEnumValues.get(0), "1.0");
        assertEquals(numberTypeEnumValues.get(1), "2.0");
        assertEquals(numberType.getDefaultValue(), new BigDecimal("1.0"));
        assertEquals(numberType.getMinimum(), new BigDecimal("1.0"));
        assertEquals(numberType.getMaximum(), new BigDecimal("2.0"));

        ModelImpl numberDoubleType = (ModelImpl)swagger.getDefinitions().get("NumberDoubleType");
        assertNotNull(numberDoubleType);
        assertNotNull(numberDoubleType.getEnum());
        assertEquals(numberDoubleType.getEnum().size(), 2);
        List<String> numberDoubleTypeEnumValues = numberDoubleType.getEnum();
        assertEquals(numberDoubleTypeEnumValues.get(0), "1.0");
        assertEquals(numberDoubleTypeEnumValues.get(1), "2.0");
        assertEquals(numberDoubleType.getDefaultValue(), new BigDecimal("1.0"));
        assertEquals(numberDoubleType.getMinimum(), new BigDecimal("1.0"));
        assertEquals(numberDoubleType.getMaximum(), new BigDecimal("2.0"));

        ModelImpl integerType = (ModelImpl)swagger.getDefinitions().get("IntegerType");
        assertNotNull(integerType);
        assertNotNull(integerType.getEnum());
        assertEquals(integerType.getEnum().size(), 2);
        List<String> integerTypeEnumValues = integerType.getEnum();
        assertEquals(integerTypeEnumValues.get(0), "1");
        assertEquals(integerTypeEnumValues.get(1), "2");
        assertEquals(integerType.getDefaultValue(), new Integer("1"));
        assertEquals(integerType.getMinimum(), new BigDecimal("1"));
        assertEquals(integerType.getMaximum(), new BigDecimal("2"));

        ModelImpl integerInt32Type = (ModelImpl)swagger.getDefinitions().get("IntegerInt32Type");
        assertNotNull(integerInt32Type);
        assertNotNull(integerInt32Type.getEnum());
        assertEquals(integerInt32Type.getEnum().size(), 2);
        List<String> integerInt32TypeEnumValues = integerInt32Type.getEnum();
        assertEquals(integerInt32TypeEnumValues.get(0), "1");
        assertEquals(integerInt32TypeEnumValues.get(1), "2");
        assertEquals(integerInt32Type.getDefaultValue(), new Integer("1"));
        assertEquals(integerInt32Type.getMinimum(), new BigDecimal("1"));
        assertEquals(integerInt32Type.getMaximum(), new BigDecimal("2"));


    }

    @Test
    public void testDefinitionExample() throws Exception {
        SwaggerParser parser = new SwaggerParser();
        final Swagger swagger = parser.read(TestUtils.getResourceAbsolutePath("/definition_example.yaml"));

        ModelImpl model;
        ArrayModel arrayModel;

        model = (ModelImpl)swagger.getDefinitions().get("NumberType");
        assertEquals((Double)model.getExample(), 2.0d, 0d);

        model = (ModelImpl)swagger.getDefinitions().get("IntegerType");
        assertEquals((int)model.getExample(), 2);

        model = (ModelImpl)swagger.getDefinitions().get("StringType");
        assertEquals((String)model.getExample(), "2");

        model = (ModelImpl)swagger.getDefinitions().get("ObjectType");
        assertTrue(model.getExample() instanceof Map);
        Map objectExample = (Map) model.getExample();
        assertEquals((String)objectExample.get("propertyA"), "valueA");
        assertEquals((Integer)objectExample.get("propertyB"), new Integer(123));

        arrayModel = (ArrayModel)swagger.getDefinitions().get("ArrayType");
        assertTrue(arrayModel.getExample() instanceof List);
        List<Map> arrayExample = (List<Map>) arrayModel.getExample();
        assertEquals((String)arrayExample.get(0).get("propertyA"), "valueA1");
        assertEquals((Integer)arrayExample.get(0).get("propertyB"), new Integer(123));
        assertEquals((String)arrayExample.get(1).get("propertyA"), "valueA2");
        assertEquals((Integer)arrayExample.get(1).get("propertyB"), new Integer(456));

        model = (ModelImpl)swagger.getDefinitions().get("NumberTypeStringExample");
        assertEquals((String)model.getExample(), "2.0");

        model = (ModelImpl)swagger.getDefinitions().get("IntegerTypeStringExample");
        assertEquals((String)model.getExample(), "2");

        model = (ModelImpl)swagger.getDefinitions().get("StringTypeStringExample");
        assertEquals((String)model.getExample(), "2");

        model = (ModelImpl)swagger.getDefinitions().get("ObjectTypeStringExample");
        assertEquals((String)model.getExample(), "{\"propertyA\": \"valueA\", \"propertyB\": 123}");

        arrayModel = (ArrayModel) swagger.getDefinitions().get("ArrayTypeStringExample");
        assertEquals((String)arrayModel.getExample(), "[{\"propertyA\": \"valueA1\", \"propertyB\": 123}, {\"propertyA\": \"valueA2\", \"propertyB\": 456}]");
    }

    @Test
    public void testIssue357() {
        SwaggerParser parser = new SwaggerParser();
        final Swagger swagger = parser.read("src/test/resources/issue_357.yaml");
        assertNotNull(swagger);
        List<Parameter> getParams = swagger.getPath("/testApi").getGet().getParameters();
        assertEquals(2, getParams.size());
        for (Parameter param : getParams) {
            SerializableParameter sp = (SerializableParameter) param;
            switch (param.getName()) {
                case "pathParam1":
                    assertEquals(sp.getType(), "integer");
                    break;
                case "pathParam2":
                    assertEquals(sp.getType(), "string");
                    break;
                default:
                    fail("Unexpected parameter named " + sp.getName());
                    break;
            }
        }
    }

    @Test
    public void testIssue358() {
        SwaggerParser parser = new SwaggerParser();
        final Swagger swagger = parser.read("src/test/resources/issue_358.yaml");
        assertNotNull(swagger);
        List<Parameter> parms = swagger.getPath("/testApi").getGet().getParameters();
        assertEquals(1, parms.size());
        assertEquals("pathParam", parms.get(0).getName());
        assertEquals("string", ((SerializableParameter) parms.get(0)).getType());
    }

    @Test
    public void testIncompatibleRefs() {
        String yaml =
                "swagger: '2.0'\n" +
                        "paths:\n" +
                        "  /test:\n" +
                        "    post:\n" +
                        "      parameters:\n" +
                        "        # this is not the correct reference type\n" +
                        "        - $ref: '#/definitions/Model'\n" +
                        "        - in: body\n" +
                        "          name: incorrectType\n" +
                        "          required: true\n" +
                        "          schema:\n" +
                        "            $ref: '#/definitions/Model'\n" +
                        "      responses:\n" +
                        "        200:\n" +
                        "          # this is not the correct reference type\n" +
                        "          $ref: '#/definitions/Model'\n" +
                        "        400:\n" +
                        "          definitions: this is right\n" +
                        "          schema:\n" +
                        "            $ref: '#/definitions/Model'\n" +
                        "definitions:\n" +
                        "  Model:\n" +
                        "    type: object";
        SwaggerDeserializationResult result = new SwaggerParser().readWithInfo(yaml);
        assertNotNull(result.getSwagger());
    }

    @Test
    public void testIssue243() {
        String yaml =
                "swagger: \"2.0\"\n" +
                        "info:\n" +
                        "  version: 0.0.0\n" +
                        "  title: Simple API\n" +
                        "paths:\n" +
                        "  /:\n" +
                        "    get:\n" +
                        "      responses:\n" +
                        "        '200':\n" +
                        "          description: OK\n" +
                        "          schema:\n" +
                        "            $ref: \"#/definitions/Simple\"\n" +
                        "definitions:\n" +
                        "  Simple:\n" +
                        "    type: string";
        SwaggerDeserializationResult result = new SwaggerParser().readWithInfo(yaml);
        assertNotNull(result.getSwagger());
    }

    @Test
    public void testIssue594() {
        String yaml =
                "swagger: '2.0'\n" +
                        "paths:\n" +
                        "  /test:\n" +
                        "    post:\n" +
                        "      parameters:\n" +
                        "        - name: body\n" +
                        "          in: body\n" +
                        "          description: Hello world\n" +
                        "          schema:\n" +
                        "            type: array\n" +
                        "            minItems: 1\n" +
                        "            maxItems: 1\n" +
                        "            items: \n" +
                        "              $ref: \"#/definitions/Pet\"\n" +
                        "      responses:\n" +
                        "        200:\n" +
                        "          description: 'OK'\n";
        SwaggerDeserializationResult result = new SwaggerParser().readWithInfo(yaml);
        assertNotNull(result.getSwagger());
        ArrayModel schema = (ArrayModel)((BodyParameter)result.getSwagger().getPaths().get("/test").getPost().getParameters().get(0)).getSchema();
        assertEquals(((RefProperty)schema.getItems()).get$ref(),"#/definitions/Pet");
        assertNotNull(schema.getMaxItems());
        assertNotNull(schema.getMinItems());

    }

    @Test
    public void testIssue450() {
        String desc = "An array of Pets";
        String xTag = "x-my-tag";
        String xVal = "An extension tag";
        String yaml =
                "swagger: \"2.0\"\n" +
                        "info:\n" +
                        "  version: 0.0.0\n" +
                        "  title: Simple API\n" +
                        "paths:\n" +
                        "  /:\n" +
                        "    get:\n" +
                        "      responses:\n" +
                        "        '200':\n" +
                        "          description: OK\n" +
                        "definitions:\n" +
                        "  PetArray:\n" +
                        "    type: array\n" +
                        "    items:\n" +
                        "      $ref: \"#/definitions/Pet\"\n" +
                        "    description: " + desc + "\n" +
                        "    " + xTag + ": " + xVal + "\n" +
                        "  Pet:\n" +
                        "    type: object\n" +
                        "    properties:\n" +
                        "      id:\n" +
                        "        type: string";
        SwaggerDeserializationResult result = new SwaggerParser().readWithInfo(yaml);
        assertNotNull(result.getSwagger());
        final Swagger swagger = result.getSwagger();

        Model petArray = swagger.getDefinitions().get("PetArray");
        assertNotNull(petArray);
        assertTrue(petArray instanceof ArrayModel);
        assertEquals(petArray.getDescription(), desc);
        assertNotNull(petArray.getVendorExtensions());
        assertNotNull(petArray.getVendorExtensions().get(xTag));
        assertEquals(petArray.getVendorExtensions().get(xTag), xVal);
    }

    @Test
    public void testIssue480() {
        final Swagger swagger = new SwaggerParser().read("src/test/resources/issue-480.yaml");

        for (String key : swagger.getSecurityDefinitions().keySet()) {
            SecuritySchemeDefinition definition = swagger.getSecurityDefinitions().get(key);
            if ("petstore_auth".equals(key)) {
                assertTrue(definition instanceof OAuth2Definition);
                OAuth2Definition oauth = (OAuth2Definition) definition;
                assertEquals("This is a description", oauth.getDescription());
            }
            if ("api_key".equals(key)) {
                assertTrue(definition instanceof ApiKeyAuthDefinition);
                ApiKeyAuthDefinition auth = (ApiKeyAuthDefinition) definition;
                assertEquals("This is another description", auth.getDescription());
            }
        }
    }

    @Test
    public void checkAllOfAreTaken() {
        Swagger swagger = new SwaggerParser().read("src/test/resources/allOf-example/allOf.json");
        assertEquals(2, swagger.getDefinitions().size());

    }

    @Test(description = "Issue #616 Relative references inside of 'allOf'")
    public void checkAllOfWithRelativeReferencesAreFound() {
        Swagger swagger = new SwaggerParser().read("src/test/resources/allOf-relative-file-references/parent.json");
        assertEquals(4, swagger.getDefinitions().size());
    }

    @Test(description = "Issue #616 Relative references inside of 'allOf'")
    public void checkAllOfWithRelativeReferencesIssue604() {
        Swagger swagger = new SwaggerParser().read("src/test/resources/allOf-relative-file-references/swagger.yaml");
        assertEquals(2, swagger.getDefinitions().size());
    }

    @Test(description = "Test that validate resolution of external references in allOf of property")
    public void checkExtRefResolveInPropertiesWithAllOf() {
        Swagger swagger = new SwaggerParser().read("src/test/resources/allOf-property-relative-file-references/parent.yaml");
        assertEquals(2, swagger.getDefinitions().size());
        assertEquals(1, swagger.getDefinitions().get("test").getProperties().size());

        ComposedProperty property = (ComposedProperty) swagger.getDefinitions().get("test").getProperties().get("property");
        assertEquals(1, property.getVendorExtensions().size());
        assertEquals(1, property.getAllOf().size());

        RefProperty refProperty = (RefProperty) property.getAllOf().get(0);
        assertEquals("#/definitions/def.def", refProperty.get$ref());

    }

    @Test(description = "A string example should not be over quoted when parsing a yaml string")
    public void readingSpecStringShouldNotOverQuotingStringExample() throws Exception {
        SwaggerParser parser = new SwaggerParser();
        final Swagger swagger = parser.read("src/test/resources/over-quoted-example.yaml", null, false);

        Map<String, Model> definitions = swagger.getDefinitions();
        assertEquals("NoQuotePlease", definitions.get("CustomerType").getExample());
    }

    @Test(description = "A string example should not be over quoted when parsing a yaml node")
    public void readingSpecNodeShouldNotOverQuotingStringExample() throws Exception {
        String yaml = Files.readFile(new File("src/test/resources/over-quoted-example.yaml"));
        JsonNode rootNode = Yaml.mapper().readValue(yaml, JsonNode.class);
        SwaggerParser parser = new SwaggerParser();
        Swagger swagger = parser.read(rootNode,true);

        Map<String, Model> definitions = swagger.getDefinitions();
        assertEquals("NoQuotePlease", definitions.get("CustomerType").getExample());
    }

    @Test
    public void testRefNameConflicts() throws Exception {
        Swagger swagger = new SwaggerParser().read("name-conflicts/refs-name-conflict/a.yaml");

        assertTrue(swagger.getDefinitions().size() == 2);

        assertEquals("#/definitions/PersonObj", ((RefProperty) swagger.getPath("/newPerson").getPost().getResponses().get("200").getSchema()).get$ref());
        assertEquals("#/definitions/PersonObj_2", ((RefProperty) swagger.getPath("/oldPerson").getPost().getResponses().get("200").getSchema()).get$ref());
        assertEquals("#/definitions/PersonObj_2", ((RefProperty) swagger.getPath("/yetAnotherPerson").getPost().getResponses().get("200").getSchema()).get$ref());
        assertEquals("local", swagger.getDefinitions().get("PersonObj").getProperties().get("location").getExample());
        assertEquals("referred", swagger.getDefinitions().get("PersonObj_2").getProperties().get("location").getExample());
    }

    @Test
    public void testRefAdditionalProperties() throws Exception {
        Swagger swagger = new SwaggerParser().read("src/test/resources/additionalProperties.yaml");

        Assert.assertNotNull(swagger);

        Assert.assertTrue(swagger.getDefinitions().size() == 3);
        
        Assert.assertNotNull(swagger.getDefinitions().get("link-object"));
        Assert.assertNotNull(swagger.getDefinitions().get("rel-data"));
        Assert.assertNotNull(swagger.getDefinitions().get("result"));
    }

    @Test
    public void testRefEnum() throws Exception {
        Swagger swagger = new SwaggerParser().read("src/test/resources/refEnum.yaml");

        Assert.assertNotNull(swagger);

        Assert.assertTrue(swagger.getDefinitions().size() == 5);

        Assert.assertNotNull(swagger.getDefinitions().get("PrintInfo"));
        Assert.assertNotNull(swagger.getDefinitions().get("SomeEnum"));
        Assert.assertNotNull(swagger.getDefinitions().get("ShippingInfo"));
    }

    @Test
    public void testIssue643() throws Exception {
        Swagger swagger = new SwaggerParser().read("src/test/resources/issue_643.yaml");

        Assert.assertNotNull(swagger);

        Assert.assertTrue(swagger.getDefinitions().size() == 1);

        Assert.assertNotNull(swagger.getDefinitions().get("XYZResponse"));

    }

    @Test
    public void testIssueGrace() throws Exception {
        Swagger swagger = new SwaggerParser().read("src/test/resources/issue_657/issue_657.json");

        Assert.assertNotNull(swagger);

        Assert.assertTrue(swagger.getDefinitions().size() == 2);

        Assert.assertNotNull(swagger.getDefinitions().get("Person"));
        Assert.assertNotNull(swagger.getDefinitions().get("Persons"));

    }

    @Test
    public void testLoadExternalNestedDefinitions() throws Exception {
        SwaggerParser parser = new SwaggerParser();
        final Swagger swagger = parser.read("src/test/resources/nested-references/b.yaml");

        Map<String, Model> definitions = swagger.getDefinitions();
        assertTrue(definitions.containsKey("x"));
        assertTrue(definitions.containsKey("y"));
        assertTrue(definitions.containsKey("z"));
        assertTrue(definitions.containsKey("referencedByLocalElement"));
        assertEquals("#/definitions/k_2", ((RefModel) definitions.get("i")).get$ref());
        assertEquals("k-definition", definitions.get("k").getTitle());
        assertEquals("k-definition", definitions.get("k_2").getTitle());
        assertEquals(((RefModel) definitions.get("l")).get$ref(), "#/definitions/referencedByLocalElement"); //issue #434
    }

    @Test(description = "Parser not honoring redirect responses")
    public void testIssue844() {
        SwaggerParser parser = new SwaggerParser();
        final SwaggerDeserializationResult swagger = parser.readWithInfo("src/test/resources/reusableParametersWithExternalRef.json", null, true);
        assertNotNull(swagger.getSwagger());
        assertEquals(swagger.getSwagger().getPath("/pets/{id}").getGet().getParameters().get(0).getIn(), "header");
    }

    @Test
    public void testIssue258() {
        SwaggerDeserializationResult result = new SwaggerParser().readWithInfo("duplicateOperationId.json", null, true);
        assertNotNull(result);
        assertNotNull(result.getSwagger());
        assertEquals(result.getMessages().get(0), "attribute paths.'/pets/{id}'(post).operationId is repeated");
    }

    @Test
    public void testIssue913() {
        SwaggerParser parser = new SwaggerParser();
        final Swagger swagger = parser.read("src/test/resources/issue-913/BS/ApiSpecification.yaml");
        Assert.assertNotNull(swagger);
        Assert.assertNotNull(swagger.getDefinitions().get("indicatorType"));
        Assert.assertEquals(swagger.getDefinitions().get("indicatorType").getProperties().size(),1);
    }
}
