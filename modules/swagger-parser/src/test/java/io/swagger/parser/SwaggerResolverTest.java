package io.swagger.parser;

import io.swagger.models.ModelImpl;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.RefModel;
import io.swagger.models.Response;
import io.swagger.models.Swagger;
import io.swagger.models.auth.AuthorizationValue;
import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.parameters.QueryParameter;
import io.swagger.models.parameters.RefParameter;
import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.IntegerProperty;
import io.swagger.models.properties.MapProperty;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.RefProperty;
import io.swagger.parser.processors.DefinitionsProcessor;
import io.swagger.parser.processors.PathsProcessor;

import mockit.Injectable;
import mockit.Mocked;
import mockit.StrictExpectations;

import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

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

            new PathsProcessor(cache, swagger);
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

    @Test(description = "resolve operation body parameter remote refs")
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
                                .schema(new RefProperty(remoteRef)))));

        final Swagger resolved = new SwaggerResolver(swagger, null).resolve();
        final Response response = swagger.getPaths().get("/fun").getGet().getResponses().get("200");
        final RefProperty ref = (RefProperty) response.getSchema();
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
                                .schema(
                                        new ArrayProperty(
                                                new RefProperty(REMOTE_REF_YAML))))));

        final Swagger resolved = new SwaggerResolver(swagger, null).resolve();
        final Response response = swagger.getPaths().get("/fun").getGet().getResponses().get("200");
        final ArrayProperty array = (ArrayProperty) response.getSchema();
        assertNotNull(array.getItems());

        final RefProperty ref = (RefProperty) array.getItems();
        assertEquals(ref.get$ref(), "#/definitions/Tag");
        assertNotNull(swagger.getDefinitions().get("Tag"));
    }
}
