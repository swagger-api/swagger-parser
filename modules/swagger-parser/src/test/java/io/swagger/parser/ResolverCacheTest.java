package io.swagger.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.models.Model;
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
import mockit.StrictExpectations;
import org.apache.commons.lang3.tuple.Pair;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.*;

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

//    @Test
    public void testLoadExternalRef_TwoRefsFromSameFile(@Injectable final Model expectedResult1,
                                                        @Injectable final Model expectedResult2) throws Exception {

        final RefFormat format = RefFormat.URL;
        final String file = "http://my.company.com/path/to/file.json";
        final String path1 = "#/hello/world/foo/bar";
        final String path2 = "#/hello/world/this/that";
        final String fullRef1 = file + path1;
        final String fullRef2 = file + path2;
        final String contentsOfExternalFile = "really good json";
        final Pair<JsonNode, JsonNode> nodePair = constructJsonTree("hello", "world", "foo", "bar");

        ObjectNode parentNode = (ObjectNode) nodePair.getLeft();
        ObjectNode intermediateNode = (ObjectNode) parentNode.get("hello").get("world");
        final ObjectNode secondLeaf = intermediateNode.putObject("this").putObject("that");


        new Expectations() {{
            RefUtils.readExternalRef(file, format, auths, null);
            times = 1;
            result = contentsOfExternalFile;

            DeserializationUtils.deserializeIntoTree(contentsOfExternalFile, file);
            times = 1;
            result = nodePair.getLeft();

            DeserializationUtils.deserialize(nodePair.getRight(), file, Model.class);
            times = 1;
            result = expectedResult1;

            DeserializationUtils.deserializeIntoTree(contentsOfExternalFile, file);
            times = 1;
            result = nodePair.getLeft();

            DeserializationUtils.deserialize(secondLeaf, file, Model.class);
            times = 1;
            result = expectedResult2;
        }};

        ResolverCache cache = new ResolverCache(swagger, auths, "http://my.company.com/path/parent.json");

        Model firstRefResult = cache.loadRef(fullRef1, format, Model.class);
        assertEquals(expectedResult1, cache.getResolutionCache().get(fullRef1));
        assertEquals(expectedResult1, firstRefResult);

        Model secondRefResult = cache.loadRef(fullRef2, format, Model.class);
        assertEquals(expectedResult2, cache.getResolutionCache().get(fullRef2));
        assertEquals(expectedResult2, secondRefResult);

        assertEquals(contentsOfExternalFile, cache.getExternalFileCache().get(file));
    }

//    @Test
    public void testLoadExternalRef_WithDefinitionPath(@Injectable final Model expectedResult) throws Exception {

        final RefFormat format = RefFormat.URL;
        final String file = "http://my.company.com/path/to/file.json";
        final String path = "#/hello/world/foo/bar";
        final String fullRef = file + path;
        final String contentsOfExternalFile = "really good json";
        final Pair<JsonNode, JsonNode> nodePair = constructJsonTree("hello", "world", "foo", "bar");

        new StrictExpectations() {{
            RefUtils.readExternalRef(file, format, auths, null);
            times = 1;
            result = contentsOfExternalFile;

            DeserializationUtils.deserializeIntoTree(contentsOfExternalFile, file);
            times = 1;
            result = nodePair.getLeft();

            DeserializationUtils.deserialize(nodePair.getRight(), file, Model.class);
            times = 1;
            result = expectedResult;
        }};

        ResolverCache cache = new ResolverCache(swagger, auths, "http://my.company.com/path/parent.json");
        Model firstActualResult = cache.loadRef(fullRef, format, Model.class);

        assertEquals(contentsOfExternalFile, cache.getExternalFileCache().get(file));
        assertEquals(expectedResult, cache.getResolutionCache().get(fullRef));
        assertEquals(expectedResult, firstActualResult);

        //requesting the same ref a second time should not result in reading the external file again
        Model secondActualResult = cache.loadRef(fullRef, format, Model.class);
        assertEquals(expectedResult, secondActualResult);
    }

//    @Test
    public void testLoadExternalRef_WithInvalidDefinitionPath() throws Exception {

        final RefFormat format = RefFormat.URL;
        final String file = "http://my.company.com/path/to/file.json";
        final String path = "hello/world/bar/foo";
        final String fullRef = file + "#/" + path;
        final String contentsOfExternalFile = "really good json";
        final Pair<JsonNode, JsonNode> nodePair = constructJsonTree("hello", "world", "foo", "bar");

        new StrictExpectations() {{
            RefUtils.readExternalRef(file, format, auths, null);
            times = 1;
            result = contentsOfExternalFile;

            DeserializationUtils.deserializeIntoTree(contentsOfExternalFile, file);
            times = 1;
            result = nodePair.getLeft();

        }};

        ResolverCache cache = new ResolverCache(swagger, auths, "http://my.company.com/path/parent.json");

        try {
            cache.loadRef(fullRef, format, Model.class);
            fail("Should have thrown an exception");
        } catch(RuntimeException e) {
            assertEquals("Could not find " + path + " in contents of " + file, e.getMessage());
        }
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
    public void testLoadInternalResponseRef(@Injectable Response mockedResponse) throws Exception {
        Swagger swagger = new Swagger();
        Map<String,Response> responses = new HashMap<>();
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
        Map<String,Response> responses = new HashMap<>();
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
