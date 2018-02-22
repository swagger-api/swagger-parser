package io.swagger.v3.parser;



import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.PathParameter;
import io.swagger.v3.oas.models.parameters.QueryParameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.parser.core.models.AuthorizationValue;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import io.swagger.v3.parser.processors.ComponentsProcessor;
import io.swagger.v3.parser.processors.PathsProcessor;

import mockit.Injectable;
import mockit.Mocked;
import mockit.StrictExpectations;
import org.testng.annotations.Test;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.testng.Assert.*;

public class SwaggerResolverTest {

    private static final String REMOTE_REF_JSON = "https://gist.githubusercontent.com/gracekarina/76ad8156f03a3cb1223abffb891d7269/raw/3950c7bcbbdd47a635cd649ec57ac5390fed3500/remote_ref_json#/components/schemas/Tag";
    private static final String REMOTE_REF_YAML = "https://gist.githubusercontent.com/gracekarina/975d133674bea6b22d09ce4ec349597d/raw/f1e31e732ac9e0d69cd197e012b4ed7eac3e07fb/remote_ref_yaml#/components/schemas/Tag";

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
        testSimpleRemoteModelProperty(REMOTE_REF_JSON);
    }

    @Test(description = "resolve a simple remote model property definition in yaml")
    public void testYamlSimpleRemoteModelProperty() {
        testSimpleRemoteModelProperty(REMOTE_REF_YAML);
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
        testArrayRemoteModelProperty(REMOTE_REF_JSON);
    }

    @Test(description = "resolve an array remote model property definition in yaml")
    public void testYamlArrayRemoteModelProperty() {
        testArrayRemoteModelProperty(REMOTE_REF_YAML);
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
        testMapRemoteModelProperty(REMOTE_REF_JSON);
    }

    @Test(description = "resolve an map remote model property definition in yaml")
    public void testYamlMapRemoteModelProperty() {
        testMapRemoteModelProperty(REMOTE_REF_YAML);
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
        testOperationBodyparamRemoteRefs(REMOTE_REF_JSON);
    }

    @Test(description = "resolve operation bodyparam remote refs in yaml")
    public void testYamlOperationBodyparamRemoteRefs() {
        testOperationBodyparamRemoteRefs(REMOTE_REF_YAML);
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
        Yaml.prettyPrint(swagger);
        final List<Parameter> params = swagger.getPaths().get("/fun").getGet().getParameters();
        assertEquals(params.size(), 1);
        final Parameter param = params.get(0);
        assertEquals(param.getName(), "skip");
    }

    //@org.junit.Test//(description = "resolve operation body parameter remote refs")
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
        testResponseRemoteRefs(REMOTE_REF_JSON);
    }

    @Test(description = "resolve response remote refs in yaml")
    public void testYamlResponseRemoteRefs() {
        testResponseRemoteRefs(REMOTE_REF_YAML);
    }

    @Test(description = "resolve array response remote refs in yaml")
    public void testYamlArrayResponseRemoteRefs() {
        final OpenAPI swagger = new OpenAPI();
        swagger.path("/fun", new PathItem()
                .get(new Operation()
                        .responses(new ApiResponses().addApiResponse("200", new ApiResponse()
                                .content(new Content().addMediaType("*/*",new MediaType().schema(
                                        new ArraySchema().items(
                                                new Schema().$ref(REMOTE_REF_YAML)))))))));

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

    @Test
    public void testCodegenIssue5753() {
        OpenAPI swagger = new OpenAPIV3Parser().read("./relative-file-references/yaml/issue-5753.yaml");

        Json.prettyPrint(swagger);
    }
}
