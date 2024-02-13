package io.swagger.parser;

import io.swagger.models.*;
import io.swagger.models.auth.AuthorizationValue;
import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.parameters.PathParameter;
import io.swagger.models.parameters.QueryParameter;
import io.swagger.models.parameters.RefParameter;
import io.swagger.models.properties.*;
import io.swagger.parser.processors.DefinitionsProcessor;
import io.swagger.parser.processors.PathsProcessor;
import io.swagger.parser.util.SwaggerDeserializationResult;
import io.swagger.util.Json;
import mockit.Injectable;
import mockit.Mocked;
import mockit.StrictExpectations;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.*;

public class SwaggerResolverTest {

    private static final String REMOTE_REF_JSON = "http://petstore.swagger.io/v2/swagger.json#/definitions/Tag";
    private static final String REMOTE_REF_YAML = "http://petstore.swagger.io/v2/swagger.yaml#/definitions/Tag";

    @Test
    public void testSwaggerResolver(@Injectable final Swagger swagger,
                                    @Injectable final List<AuthorizationValue> auths,
                                    @Mocked final ResolverCache cache,
                                    @Mocked final DefinitionsProcessor definitionsProcessor,
                                    @Mocked final PathsProcessor pathsProcessor) throws Exception {

        new StrictExpectations() {{
            new ResolverCache(swagger, auths, null);
            result = cache;
            times = 1;

            new DefinitionsProcessor(cache, swagger);
            result = definitionsProcessor;
            times = 1;

            new PathsProcessor(cache, swagger, withInstanceOf(SwaggerResolver.Settings.class));
            result = pathsProcessor;
            times = 1;

            pathsProcessor.processPaths();
            times = 1;

            definitionsProcessor.processDefinitions();
            times = 1;

        }};

        assertEquals(new SwaggerResolver(swagger, auths, null).resolve(), swagger);
    }

    @Test
    public void testSwaggerResolver_NullSwagger() throws Exception {
        assertNull(new SwaggerResolver(null, null, null).resolve());
    }

    private void testSimpleRemoteModelProperty(String remoteRef) {
        final Swagger swagger = new Swagger();
        swagger.addDefinition(
                "Sample", new ModelImpl()
                        .property("remoteRef", new RefProperty(remoteRef)));

        final Swagger resolved = new SwaggerResolver(swagger, null).resolve();
        final Property prop = resolved.getDefinitions().get("Sample").getProperties().get("remoteRef");
        assertTrue(prop instanceof RefProperty);
        final RefProperty ref = (RefProperty) prop;
        assertEquals(ref.get$ref(), "#/definitions/Tag");
        assertNotNull(swagger.getDefinitions().get("Tag"));
    }

    @Test(description = "resolve a simple remote model property definition in json")
    public void testJsonSimpleRemoteModelProperty() {
        testSimpleRemoteModelProperty(REMOTE_REF_JSON);
    }

    @Test(description = "resolve a simple remote model property definition in yaml")
    public void testYamlSimpleRemoteModelProperty() {
        testSimpleRemoteModelProperty(REMOTE_REF_YAML);
    }

    private void testArrayRemoteModelProperty(String remoteRef) {
        final Swagger swagger = new Swagger();
        swagger.addDefinition(
                "Sample", new ModelImpl()
                        .property("remoteRef", new ArrayProperty(new RefProperty(remoteRef))));

        final Swagger resolved = new SwaggerResolver(swagger, null).resolve();
        final Property prop = resolved.getDefinitions().get("Sample").getProperties().get("remoteRef");
        assertTrue(prop instanceof ArrayProperty);
        final ArrayProperty ap = (ArrayProperty) prop;
        final RefProperty ref = (RefProperty) ap.getItems();
        assertEquals(ref.get$ref(), "#/definitions/Tag");
        assertNotNull(swagger.getDefinitions().get("Tag"));
    }

    @Test(description = "resolve an array remote model property definition in json")
    public void testJsonArrayRemoteModelProperty() {
        testArrayRemoteModelProperty(REMOTE_REF_JSON);
    }

    @Test(description = "resolve an array remote model property definition in yaml")
    public void testYamlArrayRemoteModelProperty() {
        testArrayRemoteModelProperty(REMOTE_REF_YAML);
    }

    private void testMapRemoteModelProperty(String remoteRef) {
        final Swagger swagger = new Swagger();
        swagger.addDefinition(
                "Sample", new ModelImpl()
                        .property("remoteRef", new MapProperty(new RefProperty(remoteRef))));

        final Swagger resolved = new SwaggerResolver(swagger, null).resolve();
        final Property prop = resolved.getDefinitions().get("Sample").getProperties().get("remoteRef");
        assertTrue(prop instanceof MapProperty);
        final MapProperty ap = (MapProperty) prop;
        final RefProperty ref = (RefProperty) ap.getAdditionalProperties();
        assertEquals(ref.get$ref(), "#/definitions/Tag");
        assertNotNull(swagger.getDefinitions().get("Tag"));
    }

    @Test(description = "resolve an map remote model property definition in json")
    public void testJsonMapRemoteModelProperty() {
        testMapRemoteModelProperty(REMOTE_REF_JSON);
    }

    @Test(description = "resolve an map remote model property definition in yaml")
    public void testYamlMapRemoteModelProperty() {
        testMapRemoteModelProperty(REMOTE_REF_YAML);
    }

    private void testOperationBodyparamRemoteRefs(String remoteRef) {
        final Swagger swagger = new Swagger();
        swagger.path("/fun", new Path()
                .get(new Operation()
                        .parameter(new BodyParameter()
                                .schema(new RefModel(remoteRef)))));

        final Swagger resolved = new SwaggerResolver(swagger, null).resolve();
        final BodyParameter param = (BodyParameter) swagger.getPaths().get("/fun").getGet().getParameters().get(0);
        final RefModel ref = (RefModel) param.getSchema();
        assertEquals(ref.get$ref(), "#/definitions/Tag");
        assertNotNull(swagger.getDefinitions().get("Tag"));
    }

    @Test(description = "resolve operation bodyparam remote refs in json")
    public void testJsonOperationBodyparamRemoteRefs() {
        testOperationBodyparamRemoteRefs(REMOTE_REF_JSON);
    }

    @Test(description = "resolve operation bodyparam remote refs in yaml")
    public void testYamlOperationBodyparamRemoteRefs() {
        testOperationBodyparamRemoteRefs(REMOTE_REF_YAML);
    }

    @Test(description = "resolve operation parameter remote refs")
    public void testOperationParameterRemoteRefs() {
        final Swagger swagger = new Swagger();
        swagger.path("/fun", new Path()
                .get(new Operation()
                        .parameter(new RefParameter("#/parameters/SampleParameter"))));

        swagger.parameter("SampleParameter", new QueryParameter()
                .name("skip")
                .property(new IntegerProperty()));

        final Swagger resolved = new SwaggerResolver(swagger, null).resolve();
        final List<Parameter> params = swagger.getPaths().get("/fun").getGet().getParameters();
        assertEquals(params.size(), 1);
        final QueryParameter param = (QueryParameter) params.get(0);
        assertEquals(param.getName(), "skip");
    }

    @org.junit.Test//(description = "resolve operation body parameter remote refs")
    public void testOperationBodyParameterRemoteRefs() {
        final ModelImpl schema = new ModelImpl();

        final Swagger swagger = new Swagger();
        swagger.path("/fun", new Path()
                .get(new Operation()
                        .parameter(new RefParameter("#/parameters/SampleParameter"))));

        swagger.path("/times", new Path()
                .get(new Operation()
                        .parameter(new RefParameter("#/parameters/SampleParameter"))));

        swagger.parameter("SampleParameter", new BodyParameter()
                .name("skip")
                .schema(schema));

        final Swagger resolved = new SwaggerResolver(swagger, null).resolve();
        final List<Parameter> params = swagger.getPaths().get("/fun").getGet().getParameters();
        assertEquals(params.size(), 1);
        final BodyParameter param = (BodyParameter) params.get(0);
        assertEquals(param.getName(), "skip");
    }

    private void testResponseRemoteRefs(String remoteRef) {
        final Swagger swagger = new Swagger();
        swagger.path("/fun", new Path()
                .get(new Operation()
                        .response(200, new Response()
                                .responseSchema(new RefModel(remoteRef)))));

        final Swagger resolved = new SwaggerResolver(swagger, null).resolve();
        final Response response = swagger.getPaths().get("/fun").getGet().getResponsesObject().get("200");
        final RefModel ref = (RefModel) response.getResponseSchema();
        assertEquals(ref.get$ref(), "#/definitions/Tag");
        assertNotNull(swagger.getDefinitions().get("Tag"));
    }

    @Test(description = "resolve response remote refs in json")
    public void testJsonResponseRemoteRefs() {
        testResponseRemoteRefs(REMOTE_REF_JSON);
    }

    @Test(description = "resolve response remote refs in yaml")
    public void testYamlResponseRemoteRefs() {
        testResponseRemoteRefs(REMOTE_REF_YAML);
    }

    @Test(description = "resolve array response remote refs in yaml")
    public void testYamlArrayResponseRemoteRefs() {
        final Swagger swagger = new Swagger();
        swagger.path("/fun", new Path()
                .get(new Operation()
                        .response(200, new Response()
                                .responseSchema(
                                        new ArrayModel().items(
                                                new RefProperty(REMOTE_REF_YAML))))));

        final Swagger resolved = new SwaggerResolver(swagger, null).resolve();
        final Response response = swagger.getPaths().get("/fun").getGet().getResponsesObject().get("200");
        final ArrayModel array = (ArrayModel) response.getResponseSchema();
        assertNotNull(array.getItems());

        final RefProperty ref = (RefProperty) array.getItems();
        assertEquals(ref.get$ref(), "#/definitions/Tag");
        assertNotNull(swagger.getDefinitions().get("Tag"));
    }

    @Test(description = "resolve shared path parameters")
    public void testSharedPathParametersTest() {
        final Swagger swagger = new Swagger();
        Operation operation = new Operation()
                .response(200, new Response().description("ok!"));
        Path path = new Path().get(operation);
        path.addParameter(new QueryParameter()
                .name("username")
                .property(new StringProperty()));
        swagger.path("/fun", path);

        final Swagger resolved = new SwaggerResolver(swagger, null).resolve();
        assertNull(resolved.getPaths().get("/fun").getParameters());
        assertTrue(resolved.getPaths().get("/fun").getGet().getParameters().size() == 1);
    }

    @Test(description = "resolve top-level parameters")
    public void testSharedSwaggerParametersTest() {
        final Swagger swagger = new Swagger();
        swagger.path("/fun", new Path()
                .get(new Operation()
                        .parameter(new RefParameter("username"))
                        .response(200, new Response().description("ok!"))));

        swagger.parameter("username", new QueryParameter()
                .name("username")
                .property(new StringProperty()));

        final Swagger resolved = new SwaggerResolver(swagger, null).resolve();
        assertTrue(resolved.getParameters().size() == 1);
        assertTrue(resolved.getPaths().get("/fun").getGet().getParameters().size() == 1);
    }

    @Test(description = "resolve top-level responses")
    public void testSharedResponses() {
        final Swagger swagger = new Swagger();
        swagger.path("/fun", new Path()
                .get(new Operation()
                        .parameter(new RefParameter("username"))
                        .response(200, new RefResponse("foo"))));

        swagger.response("foo", new Response().description("ok!"));

        final Swagger resolved = new SwaggerResolver(swagger, null).resolve();
        Response response = resolved.getPath("/fun").getGet().getResponsesObject().get("200");
        assertTrue(response.getDescription().equals("ok!"));
        assertTrue(response instanceof Response);
    }

    @Test
    public void testIssue291() {
        String json =
                "{\n" +
                "  \"swagger\": \"2.0\",\n" +
                "  \"info\": {\n" +
                "    \"title\": \"test spec\",\n" +
                "    \"version\": \"1.0\"\n" +
                "  },\n" +
                "  \"parameters\": {\n" +
                "    \"testParam\": {\n" +
                "      \"collectionFormat\": \"csv\",\n" +
                "      \"name\": \"test\",\n" +
                "      \"in\": \"query\",\n" +
                "      \"type\": \"array\",\n" +
                "      \"items\": {\n" +
                "        \"type\": \"string\"\n" +
                "      }\n" +
                "    }\n" +
                "  },\n" +
                "  \"paths\": {\n" +
                "    \"/test\": {\n" +
                "      \"get\": {\n" +
                "        \"description\": \"test get\",\n" +
                "        \"parameters\": [\n" +
                "          {\n" +
                "            \"$ref\": \"#/parameters/testParam\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"responses\": {\n" +
                "          \"default\": {\n" +
                "             \"description\": \"test response\"\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}";
        SwaggerParser parser = new SwaggerParser();
        SwaggerDeserializationResult result = parser.readWithInfo(json);

        Swagger swagger = result.getSwagger();

        final Swagger resolved = new SwaggerResolver(swagger, null).resolve();


        Parameter param = resolved.getPaths().get("/test").getGet().getParameters().get(0);
        QueryParameter qp = (QueryParameter) param;
        assertEquals(qp.getCollectionFormat(), "csv");
    }

    @Test
    public void testSettingsAddParametersToEachOperationDisabled() {
        String yaml =
                "---\n" +
                        "swagger: '2.0'\n" +
                        "info:\n" +
                        "  title: test spec\n" +
                        "  version: '1.0'\n" +
                        "paths:\n" +
                        "  \"/test/{id}\":\n" +
                        "    parameters:\n" +
                        "    - name: id\n" +
                        "      in: path\n" +
                        "      type: string\n" +
                        "      required: true\n" +
                        "    get:\n" +
                        "      description: test get\n" +
                        "      parameters:\n" +
                        "      - name: page\n" +
                        "        in: query\n" +
                        "        type: string\n" +
                        "      responses:\n" +
                        "        default:\n" +
                        "          description: test response\n";

        SwaggerParser parser = new SwaggerParser();
        SwaggerDeserializationResult result = parser.readWithInfo(yaml);

        Swagger swagger = result.getSwagger();

        final Swagger resolved = new SwaggerResolver(swagger, null, null,
                new SwaggerResolver.Settings().addParametersToEachOperation(false))
                .resolve();


        assertEquals(resolved.getPaths().get("/test/{id}").getParameters().size(), 1);
        PathParameter pp = (PathParameter)resolved.getPaths().get("/test/{id}").getParameters().get(0);
        assertEquals(pp.getName(), "id");

        assertEquals(resolved.getPaths().get("/test/{id}").getGet().getParameters().size(), 1);
        QueryParameter qp = (QueryParameter)resolved.getPaths().get("/test/{id}").getGet().getParameters().get(0);
        assertEquals(qp.getName(), "page");
    }

    @Test
    public void testCodegenIssue5753() {
        Swagger swagger = new SwaggerParser().read("./relative-file-references/yaml/issue-5753.yaml");

        Json.prettyPrint(swagger);
    }
}
