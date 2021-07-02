package io.swagger.parser;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.swagger.models.Responses;
import org.apache.commons.lang3.tuple.Pair;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.swagger.models.Model;
import io.swagger.models.Path;
import io.swagger.models.Response;
import io.swagger.models.Swagger;
import io.swagger.models.auth.AuthorizationValue;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.refs.RefFormat;
import io.swagger.parser.util.DeserializationUtils;
import io.swagger.parser.util.RefUtils;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Mocked;

public class ResolverCacheTest {

    @Mocked
    RefUtils refUtils;

    @Mocked
    DeserializationUtils deserializationUtils;

    @Injectable
    Swagger swagger;
    @Injectable
    List<AuthorizationValue> auths;

    @Test
    public void testMock(@Injectable final Model expectedResult) throws Exception {
        final RefFormat format = RefFormat.URL;
        final String ref = "http://my.company.com/path/to/file.json";
        final String contentsOfExternalFile = "really good json";

        new Expectations() {{
            RefUtils.readExternalUrlRef(ref, format, auths, "http://my.company.com/path/parent.json");
            times = 1;
            result = contentsOfExternalFile;

            DeserializationUtils.deserialize(contentsOfExternalFile, ref, Model.class);
            times = 1;
            result = expectedResult;
        }};

        ResolverCache cache = new ResolverCache(swagger, auths, "http://my.company.com/path/parent.json");

        Model firstActualResult = cache.loadRef(ref, RefFormat.URL, Model.class);


        assertEquals(firstActualResult, expectedResult);
    }

    @Test
    public void testLoadExternalRef_NoDefinitionPath(@Injectable final Model expectedResult) throws Exception {

        final RefFormat format = RefFormat.URL;
        final String ref = "http://my.company.com/path/to/file.json";
        final String contentsOfExternalFile = "really good json";

        new Expectations() {{
            RefUtils.readExternalUrlRef(ref, format, auths, "http://my.company.com/path/parent.json");
            times = 1;
            result = contentsOfExternalFile;

            DeserializationUtils.deserialize(contentsOfExternalFile, ref, Model.class);
            times = 1;
            result = expectedResult;
        }};

        ResolverCache cache = new ResolverCache(swagger, auths, "http://my.company.com/path/parent.json");

        Model firstActualResult = cache.loadRef(ref, RefFormat.URL, Model.class);

        assertEquals(contentsOfExternalFile, cache.getExternalFileCache().get(ref));
        assertEquals(expectedResult, cache.getResolutionCache().get(ref));
        assertEquals(expectedResult, firstActualResult);

        //requesting the same ref a second time should not result in reading the external file again
        Model secondActualResult = cache.loadRef(ref, format, Model.class);
        assertEquals(expectedResult, secondActualResult);

    }

    @Test
    public void testLoadExternalRefWithEscapedCharacters() throws Exception {
        final RefFormat format = RefFormat.URL;
        final String ref = "http://my.company.com/path/to/main.yaml";
        final String contentsOfExternalFile = "swagger: \"2.0\"\n" + 
            "\n" + 
            "info:\n" + 
            "  version: 1.0.0\n" + 
            "  title: Path include test case child\n" + 
            "\n" + 
            "paths:\n" + 
            "  /foo~bar~1:\n" + 
            "    get:\n" + 
            "      responses:\n" + 
            "        200:\n" + 
            "          description: \"Request successful\"\n";

        new Expectations() {{
            RefUtils.readExternalUrlRef(ref, format, auths, "http://my.company.com/path/parent.json");
            times = 1;
            result = contentsOfExternalFile;
        }};

        ResolverCache cache = new ResolverCache(swagger, auths, "http://my.company.com/path/parent.json");

        Path path = cache.loadRef(ref+"#/paths/~1foo~0bar~01", RefFormat.URL, Path.class);
        assertNotNull(path);
    }

    @Test
    public void testLoadInternalParameterRef(@Injectable Parameter mockedParameter) throws Exception {
        Swagger swagger = new Swagger();
        swagger.parameter("foo", mockedParameter);

        ResolverCache cache = new ResolverCache(swagger, auths, null);
        Parameter actualResult = cache.loadRef("#/parameters/foo", RefFormat.INTERNAL, Parameter.class);
        assertEquals(actualResult, mockedParameter);

        assertNull(cache.loadRef("#/parameters/bar", RefFormat.INTERNAL, Parameter.class));
        assertNull(cache.loadRef("#/params/foo", RefFormat.INTERNAL, Parameter.class));
    }
    
    @Test
    public void testLoadInternalParameterRefWithSpaces(@Injectable Parameter mockedParameter) throws Exception {
        Swagger swagger = new Swagger();
        swagger.parameter("foo bar", mockedParameter);

        ResolverCache cache = new ResolverCache(swagger, auths, null);
        Parameter actualResult = cache.loadRef("#/parameters/foo bar", RefFormat.INTERNAL, Parameter.class);
        assertEquals(actualResult, mockedParameter);
    }

    @Test
    public void testLoadInternalDefinitionRef(@Injectable Model mockedModel) throws Exception {
        Swagger swagger = new Swagger();
        swagger.addDefinition("foo", mockedModel);

        ResolverCache cache = new ResolverCache(swagger, auths, null);
        Model actualResult = cache.loadRef("#/definitions/foo", RefFormat.INTERNAL, Model.class);
        assertEquals(actualResult, mockedModel);

        assertNull(cache.loadRef("#/definitions/bar", RefFormat.INTERNAL, Model.class));
        assertNull(cache.loadRef("#/defs/bar", RefFormat.INTERNAL, Model.class));
    }

    @Test
    public void testLoadInternalDefinitionRefWithSpaces(@Injectable Model mockedModel) throws Exception {
        Swagger swagger = new Swagger();
        swagger.addDefinition("foo bar", mockedModel);

        ResolverCache cache = new ResolverCache(swagger, auths, null);
        Model actualResult = cache.loadRef("#/definitions/foo bar", RefFormat.INTERNAL, Model.class);
        assertEquals(actualResult, mockedModel);
    }
    
    @Test
    public void testLoadInternalDefinitionRefWithEscapedCharacters(@Injectable Model mockedModel) throws Exception {
        Swagger swagger = new Swagger();
        swagger.addDefinition("foo~bar/baz~1", mockedModel);

        ResolverCache cache = new ResolverCache(swagger, auths, null);
        Model actualResult = cache.loadRef("#/definitions/foo~0bar~1baz~01", RefFormat.INTERNAL, Model.class);
        assertEquals(actualResult, mockedModel);
    }
    
    @Test
    public void testLoadInternalResponseRef(@Injectable Response mockedResponse) throws Exception {
        Swagger swagger = new Swagger();
        Responses responses = new Responses();
        responses.put("foo", mockedResponse);
        swagger.setResponses(responses);

        ResolverCache cache = new ResolverCache(swagger, auths, null);
        Response actualResult = cache.loadRef("#/responses/foo", RefFormat.INTERNAL, Response.class);
        assertEquals(actualResult, mockedResponse);

        assertNull(cache.loadRef("#/responses/bar", RefFormat.INTERNAL, Response.class));
    }

    @Test
    public void testLoadInternalResponseRefWithSpaces(@Injectable Response mockedResponse) throws Exception {
        Swagger swagger = new Swagger();
        Responses responses = new Responses();
        responses.put("foo bar", mockedResponse);
        swagger.setResponses(responses);

        ResolverCache cache = new ResolverCache(swagger, auths, null);
        Response actualResult = cache.loadRef("#/responses/foo bar", RefFormat.INTERNAL, Response.class);
        assertEquals(actualResult, mockedResponse);
    }

    @Test
    public void testRenameCache() throws Exception {
        ResolverCache cache = new ResolverCache(swagger, auths, null);

        assertNull(cache.getRenamedRef("foo"));
        cache.putRenamedRef("foo", "bar");
        assertEquals(cache.getRenamedRef("foo"), "bar");
    }

    private Pair<JsonNode, JsonNode> constructJsonTree(String... properties) {
        JsonNodeFactory factory = new JsonNodeFactory(true);
        final ObjectNode parent = factory.objectNode();
        ObjectNode current = parent;

        for (String property : properties) {
            current = current.putObject(property);
        }

        current.put("key", "value");

        return Pair.of((JsonNode) parent, (JsonNode) current);
    }
}
