package io.swagger.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.models.Model;
import io.swagger.models.Swagger;
import io.swagger.models.auth.AuthorizationValue;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.refs.RefFormat;
import io.swagger.parser.util.DeserializationUtils;
import io.swagger.parser.util.RefUtils;
import mockit.Injectable;
import mockit.Mocked;
import mockit.StrictExpectations;
import org.apache.commons.lang3.tuple.Pair;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.fail;

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

        new StrictExpectations() {{
            RefUtils.readExternalRef(ref, format, auths, null);
            times = 1;
            result = contentsOfExternalFile;

            DeserializationUtils.deserialize(contentsOfExternalFile, ref, Model.class);
            times = 1;
            result = expectedResult;
        }};

        ResolverCache cache = new ResolverCache(swagger, auths);

        Model firstActualResult = cache._loadRef(ref, RefFormat.URL, Model.class, "http://my.company.com/path/parent.json");

        assertEquals(contentsOfExternalFile, cache.getExternalFileCache().get(ref));
        assertEquals(expectedResult, cache.getResolutionCache().get(ref));
        assertEquals(expectedResult, firstActualResult);

        //requesting the same ref a second time should not result in reading the external file again
        Model secondActualResult = cache._loadRef(ref, format, Model.class, "http://my.company.com/path/parent.json");
        assertEquals(expectedResult, secondActualResult);

    }

    @Test
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


        new StrictExpectations() {{
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

        ResolverCache cache = new ResolverCache(swagger, auths);

        Model firstRefResult = cache._loadRef(fullRef1, format, Model.class, "http://my.company.com/path/parent.json");
        assertEquals(expectedResult1, cache.getResolutionCache().get(fullRef1));
        assertEquals(expectedResult1, firstRefResult);

        Model secondRefResult = cache._loadRef(fullRef2, format, Model.class, "http://my.company.com/path/parent.json");
        assertEquals(expectedResult2, cache.getResolutionCache().get(fullRef2));
        assertEquals(expectedResult2, secondRefResult);

        assertEquals(contentsOfExternalFile, cache.getExternalFileCache().get(file));
    }

    @Test
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

        ResolverCache cache = new ResolverCache(swagger, auths);
        Model firstActualResult = cache._loadRef(fullRef, format, Model.class, "http://my.company.com/path/parent.json");

        assertEquals(contentsOfExternalFile, cache.getExternalFileCache().get(file));
        assertEquals(expectedResult, cache.getResolutionCache().get(fullRef));
        assertEquals(expectedResult, firstActualResult);

        //requesting the same ref a second time should not result in reading the external file again
        Model secondActualResult = cache._loadRef(fullRef, format, Model.class, "http://my.company.com/path/parent.json");
        assertEquals(expectedResult, secondActualResult);
    }

    @Test
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

        ResolverCache cache = new ResolverCache(swagger, auths);

        try
        {
            cache._loadRef(fullRef, format, Model.class, "http://my.company.com/path/parent.json");
            fail("Should have thrown an exception");
        } catch(RuntimeException e) {
            assertEquals("Could not find " + path + " in contents of " + file, e.getMessage());
        }
    }

    @Test
    public void testLoadExternalRef_WithRelativeDefinitionPath(@Injectable final Model expectedResult) throws Exception {

        final RefFormat format = RefFormat.URL;
        final String file = "http://my.company.com/path/to/file.json";
        final String path = "#/hello/world/foo/bar";
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

        ResolverCache cache = new ResolverCache(swagger, auths);
        Model firstActualResult = cache._loadRef(path, format, Model.class, file);

        assertEquals(contentsOfExternalFile, cache.getExternalFileCache().get(file));
        assertEquals(expectedResult, cache.getResolutionCache().get(file+path));
        assertEquals(expectedResult, firstActualResult);

        //requesting the same ref a second time should not result in reading the external file again
        Model secondActualResult = cache._loadRef(path, format, Model.class, file);
        assertEquals(expectedResult, secondActualResult);
    }


    //    @Test
    public void testLoadInternalParameterRef(@Injectable Parameter mockedParameter) throws Exception {
        Swagger swagger = new Swagger();
        swagger.parameter("foo", mockedParameter);

        ResolverCache cache = new ResolverCache(swagger, auths);
        Parameter actualResult = cache.loadRef("#/parameters/foo", RefFormat.INTERNAL, Parameter.class, null);
        assertEquals(actualResult, mockedParameter);

        assertNull(cache.loadRef("#/parameters/bar", RefFormat.INTERNAL, Parameter.class, null));
        assertNull(cache.loadRef("#/params/foo", RefFormat.INTERNAL, Parameter.class, null));
    }

    @Test
    public void testLoadInternalDefinitionRef(@Injectable Model mockedModel) throws Exception {
        Swagger swagger = new Swagger();
        swagger.addDefinition("foo", mockedModel);

        ResolverCache cache = new ResolverCache(swagger, auths);
        Model actualResult = cache.loadRef("#/definitions/foo", RefFormat.INTERNAL, Model.class, null);
        assertEquals(actualResult, mockedModel);

        assertNull(cache.loadRef("#/definitions/bar", RefFormat.INTERNAL, Model.class, null));
        assertNull(cache.loadRef("#/defs/bar", RefFormat.INTERNAL, Model.class, null));
    }

    @Test
    public void testRenameCache() throws Exception {
        ResolverCache cache = new ResolverCache(swagger, auths);

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
