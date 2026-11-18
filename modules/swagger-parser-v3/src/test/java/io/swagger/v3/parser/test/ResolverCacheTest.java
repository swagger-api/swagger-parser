package io.swagger.v3.parser.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.parser.ResolverCache;
import io.swagger.v3.parser.core.models.AuthorizationValue;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import io.swagger.v3.parser.models.RefFormat;
import io.swagger.v3.parser.urlresolver.PermittedUrlsChecker;
import io.swagger.v3.parser.util.DeserializationUtils;
import io.swagger.v3.parser.util.RefUtils;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Mocked;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNotSame;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class ResolverCacheTest {

    @Mocked
    RefUtils refUtils;

    @Injectable
    OpenAPI openAPI;

    List<AuthorizationValue> auths = new ArrayList<>();

    @Injectable
    Parameter mockedParameter;

    @Injectable
    Schema mockedModel;

    @Injectable
    ApiResponse mockedResponse;

    @Injectable
    DeserializationUtils deserializationUtils;

    @BeforeMethod
    public void init() {
        openAPI = new OpenAPI();
    }

    @Test
    public void testMock() throws JsonProcessingException {

        final RefFormat format = RefFormat.URL;
        final String ref = "http://my.company.com/path/to/file.json";
        final String contentsOfExternalFile = "really good json";

        ParseOptions parseOptions = new ParseOptions();
        parseOptions.setResolve(true);
        parseOptions.setValidateExternalRefs(true);

        new Expectations(deserializationUtils) {{
            RefUtils.readExternalUrlRef(ref, format, auths, "http://my.company.com/path/parent.json", (PermittedUrlsChecker) any);
            times = 1;
            result = contentsOfExternalFile;

            DeserializationUtils.deserializeIntoTree(contentsOfExternalFile, ref, parseOptions, (SwaggerParseResult) any);
            times = 1;
            result = new ObjectMapper().readTree("{\"type\":  \"string\"}");

        }};
        ResolverCache cache = new ResolverCache(openAPI, auths, "http://my.company.com/path/parent.json", new HashSet<>(), parseOptions);

        Schema firstActualResult = cache.loadRef(ref, RefFormat.URL, Schema.class);


        assertEquals(firstActualResult.getType(), "string");
    }

    @Test
    public void testLoadExternalRef_NoDefinitionPath() throws JsonProcessingException {

        final RefFormat format = RefFormat.URL;
        final String ref = "http://my.company.com/path/to/file.json";
        final String contentsOfExternalFile = "really good json";

        ParseOptions parseOptions = new ParseOptions();
        parseOptions.setResolve(true);
        parseOptions.setValidateExternalRefs(true);

        new Expectations(deserializationUtils) {{
            RefUtils.readExternalUrlRef(ref, format, auths, "http://my.company.com/path/parent.json", (PermittedUrlsChecker) any);
            times = 1;
            result = contentsOfExternalFile;

            DeserializationUtils.deserializeIntoTree(contentsOfExternalFile, ref, parseOptions, (SwaggerParseResult) any);
            times = 1;
            result = new ObjectMapper().readTree("{\"type\":  \"string\"}");
        }};

        ResolverCache cache = new ResolverCache(openAPI, auths, "http://my.company.com/path/parent.json", new HashSet<>(), parseOptions);

        Schema firstActualResult = cache.loadRef(ref, RefFormat.URL, Schema.class);

        assertEquals(cache.getExternalFileCache().get(ref), contentsOfExternalFile);
        assertEquals(((Schema) cache.getResolutionCache().get(ref)).getType(), "string");
        assertEquals(firstActualResult.getType(), "string");

        //requesting the same ref a second time should not result in reading the external file again
        Schema secondActualResult = cache.loadRef(ref, format, Schema.class);
        assertEquals(secondActualResult.getType(), "string");

    }

    @Test
    public void testLoadExternalRefWithEscapedCharacters() {
        final RefFormat format = RefFormat.URL;
        final String ref = "http://my.company.com/path/to/main.yaml";
        final String contentsOfExternalFile = "openAPI: \"2.0\"\n" +
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
            RefUtils.readExternalUrlRef(ref, format, auths, "http://my.company.com/path/parent.json", (PermittedUrlsChecker) any);
            times = 1;
            result = contentsOfExternalFile;
        }};

        ResolverCache cache = new ResolverCache(openAPI, auths, "http://my.company.com/path/parent.json");

        PathItem path = cache.loadRef(ref + "#/paths/~1foo~0bar~01", RefFormat.URL, PathItem.class);
        assertNotNull(path);
    }

    @Test
    public void testLoadExternalRefResponseWithNoContent() {
        final RefFormat format = RefFormat.URL;
        final String ref = "http://my.company.com/path/to/main.yaml";
        final String contentsOfExternalFile = "openapi: 3.0.0\n" +
                "\n" +
                "info:\n" +
                "  version: 1.0.0\n" +
                "  title: Response include test case child\n" +
                "\n" +
                "components:\n" +
                "  responses:\n" +
                "    200:\n" +
                "      description: Success\n";

        new Expectations() {{
            RefUtils.readExternalUrlRef(ref, format, auths, "http://my.company.com/path/parent.json", (PermittedUrlsChecker) any);
            times = 1;
            result = contentsOfExternalFile;
        }};

        ResolverCache cache = new ResolverCache(openAPI, auths, "http://my.company.com/path/parent.json");

        ApiResponse response = cache.loadRef(ref + "#/components/responses/200", RefFormat.URL, ApiResponse.class);
        assertNotNull(response);
        assertEquals(response.getDescription(), "Success");
        assertNull(response.getContent());
    }

    @Test
    public void testLoadInternalParameterRef() {
        openAPI.components(new Components().addParameters("foo", mockedParameter));

        ResolverCache cache = new ResolverCache(openAPI, auths, null);
        Parameter actualResult = cache.loadRef("#/components/parameters/foo", RefFormat.INTERNAL, Parameter.class);
        assertEquals(actualResult, mockedParameter);

        assertNull(cache.loadRef("#/components/parameters/bar", RefFormat.INTERNAL, Parameter.class));
        assertNull(cache.loadRef("#/params/foo", RefFormat.INTERNAL, Parameter.class));
    }

    @Test
    public void testLoadInternalParameterRefWithSpaces() {
        openAPI.components(new Components().addParameters("foo bar", mockedParameter));

        ResolverCache cache = new ResolverCache(openAPI, auths, null);
        Parameter actualResult = cache.loadRef("#/components/parameters/foo bar", RefFormat.INTERNAL, Parameter.class);
        assertEquals(actualResult, mockedParameter);
    }

    @Test
    public void testLoadInternalDefinitionRef() {
        openAPI.components(new Components().addSchemas("foo", mockedModel));

        ResolverCache cache = new ResolverCache(openAPI, auths, null);
        Schema actualResult = cache.loadRef("#/components/schemas/foo", RefFormat.INTERNAL, Schema.class);
        assertEquals(actualResult, mockedModel);

        assertNull(cache.loadRef("#/components/schemas/bar", RefFormat.INTERNAL, Schema.class));
        assertNull(cache.loadRef("#/defs/bar", RefFormat.INTERNAL, Schema.class));
    }

    @Test
    public void testLoadInternalDefinitionRefWithSpaces() {
        openAPI.components(new Components().addSchemas("foo bar", mockedModel));

        ResolverCache cache = new ResolverCache(openAPI, auths, null);
        Schema actualResult = cache.loadRef("#/components/schemas/foo bar", RefFormat.INTERNAL, Schema.class);
        assertEquals(actualResult, mockedModel);
    }

    @Test
    public void testLoadInternalDefinitionRefWithEscapedCharacters() {
        openAPI.components(new Components().addSchemas("foo~bar/baz~1", mockedModel));

        ResolverCache cache = new ResolverCache(openAPI, auths, null);
        Schema actualResult = cache.loadRef("#/components/schemas/foo~0bar~1baz~01", RefFormat.INTERNAL, Schema.class);
        assertEquals(actualResult, mockedModel);
    }

    @Test
    public void testLoadInternalResponseRef() {
        openAPI.components(new Components().addResponses("foo", mockedResponse));

        ResolverCache cache = new ResolverCache(openAPI, auths, null);
        ApiResponse actualResult = cache.loadRef("#/components/responses/foo", RefFormat.INTERNAL, ApiResponse.class);
        assertEquals(actualResult, mockedResponse);

        assertNull(cache.loadRef("#/components/responses/bar", RefFormat.INTERNAL, ApiResponse.class));
    }

    @Test
    public void testLoadInternalResponseRefWithSpaces() {
        openAPI.components(new Components().addResponses("foo bar", mockedResponse));

        ResolverCache cache = new ResolverCache(openAPI, auths, null);
        ApiResponse actualResult = cache.loadRef("#/components/responses/foo bar", RefFormat.INTERNAL, ApiResponse.class);
        assertEquals(actualResult, mockedResponse);
    }

    @Test
    public void testRootDocumentUriIsEstablishedOnceForSupportedRoots() throws Exception {
        Path rootFile = Files.createTempFile("resolver-cache-root", ".yaml");
        ResolverCache filesystemCache = new ResolverCache(openAPI, auths, rootFile.toString());
        URI filesystemUri = getRootDocumentUri(filesystemCache);
        Files.delete(rootFile);

        assertEquals(filesystemUri, rootFile.toAbsolutePath().normalize().toUri());
        assertEquals(getRootDocumentUri(filesystemCache), filesystemUri);

        ResolverCache fileCache = new ResolverCache(
                openAPI, auths, "file:///tmp/schemas/../root.yaml?v=1#section");
        assertEquals(getRootDocumentUri(fileCache), new URI("file:/tmp/root.yaml?v=1"));

        ResolverCache httpCache = new ResolverCache(
                openAPI, auths, "https://example.com/api/../root.yaml?v=1#section");
        assertEquals(getRootDocumentUri(httpCache),
                new URI("https://example.com/root.yaml?v=1"));
    }

    @Test
    public void testUnsupportedAndAmbiguousRootsDisableRootIdentity() throws Exception {
        String[] roots = {
                "classpath:/root.yaml",
                "jar:file:/tmp/specs.jar!/root.yaml",
                "urn:swagger:root",
                "http://example.com/%zz",
                "missing-relative-root.yaml"
        };

        for (String root : roots) {
            assertNull(getRootDocumentUri(new ResolverCache(openAPI, auths, root)), root);
        }
    }

    @Test
    public void testClasspathRootKeepsExistingExternalLoadingPath() {
        Schema rootSchema = new Schema().type("object");
        openAPI.components(new Components().addSchemas("Foo", rootSchema));
        final String root = "classpath:/root.yaml";
        final String file = "external.yaml";
        final String contents = "components:\n  schemas:\n    Foo:\n      type: string\n";

        new Expectations() {{
            RefUtils.readExternalClasspathRef(file, RefFormat.RELATIVE, auths, root,
                    (PermittedUrlsChecker) any);
            times = 1;
            result = contents;
        }};

        Schema result = new ResolverCache(openAPI, auths, root).loadRef(
                file + "#/components/schemas/Foo", RefFormat.RELATIVE, Schema.class);

        assertNotSame(result, rootSchema);
        assertEquals(result.getType(), "string");
    }

    @Test
    public void testDotSegmentHttpReferenceReusesRootWithoutLoading() {
        Schema rootSchema = new Schema().type("object");
        openAPI.components(new Components().addSchemas("Foo", rootSchema));
        final String root = "https://example.com/api/root.yaml";
        final String file = "https://example.com/api/nested/../root.yaml";
        final String ref = file + "#/components/schemas/Foo";
        final List<AuthorizationValue> rootAuths = new ArrayList<>();
        rootAuths.add(new AuthorizationValue("Authorization", "token", "header"));
        ParseOptions options = new ParseOptions();
        options.setSafelyResolveURL(true);

        final int[] permissionChecks = {0};
        ResolverCache cache = new ResolverCache(openAPI, rootAuths, root, new HashSet<>(), options) {
            @Override
            protected void checkUrlIsPermitted(String refSet) {
                permissionChecks[0]++;
                super.checkUrlIsPermitted(refSet);
            }
        };

        Schema result = cache.loadRef(ref, RefFormat.URL, Schema.class);

        assertSame(result, rootSchema);
        assertEquals(permissionChecks[0], 1);
        assertFalse(cache.getExternalFileCache().containsKey(file));
        assertSame(cache.getResolutionCache().get(ref), rootSchema);
    }

    @Test
    public void testRootReferenceUsesObjectPresentAtCacheConstruction() {
        Schema originalRootSchema = new Schema().type("object");
        openAPI.components(new Components().addSchemas("Foo", originalRootSchema));
        final String root = "https://example.com/api/root.yaml";
        ResolverCache cache = new ResolverCache(openAPI, auths, root);

        openAPI.getComponents().getSchemas().put("Foo", new Schema().type("string"));

        Schema result = cache.loadRef(
                root + "#/components/schemas/Foo", RefFormat.URL, Schema.class);

        assertSame(result, originalRootSchema);
        assertEquals(cache.getRootReferenceName(root + "#/components/schemas/Foo"), "Foo");
    }

    @Test
    public void testSameFilenameInDifferentDirectoryDoesNotReuseRoot() {
        Schema rootSchema = new Schema().type("object");
        openAPI.components(new Components().addSchemas("Foo", rootSchema));
        final String root = "https://example.com/api/root.yaml";
        final String file = "https://example.com/api/sub/root.yaml";
        final String contents = "components:\n  schemas:\n    Foo:\n      type: string\n";

        new Expectations() {{
            RefUtils.readExternalUrlRef(file, RefFormat.URL, auths, root,
                    (PermittedUrlsChecker) any);
            times = 1;
            result = contents;
        }};

        Schema result = new ResolverCache(openAPI, auths, root).loadRef(
                file + "#/components/schemas/Foo", RefFormat.URL, Schema.class);

        assertNotSame(result, rootSchema);
        assertEquals(result.getType(), "string");
    }

    @Test
    public void testHttpQueryIsPartOfRootIdentity() {
        Schema rootSchema = new Schema().type("object");
        openAPI.components(new Components().addSchemas("Foo", rootSchema));
        final String root = "https://example.com/api/root.yaml?v=1";
        final String file = "https://example.com/api/root.yaml?v=2";
        final String contents = "components:\n  schemas:\n    Foo:\n      type: integer\n";

        new Expectations() {{
            RefUtils.readExternalUrlRef(file, RefFormat.URL, auths, root,
                    (PermittedUrlsChecker) any);
            times = 1;
            result = contents;
        }};

        Schema result = new ResolverCache(openAPI, auths, root).loadRef(
                file + "#/components/schemas/Foo", RefFormat.URL, Schema.class);

        assertNotSame(result, rootSchema);
        assertEquals(result.getType(), "integer");
    }

    @Test
    public void testUnsupportedAbsentAndWrongTypePointersUseExternalDocument() {
        Schema rootSchema = new Schema().type("object");
        openAPI.components(new Components().addSchemas("Foo", rootSchema));
        final String root = "https://example.com/api/root.yaml";
        final String contents =
                "components:\n" +
                "  schemas:\n" +
                "    External:\n" +
                "      type: string\n" +
                "    Foo:\n" +
                "      name: id\n" +
                "      in: query\n" +
                "      properties:\n" +
                "        value:\n" +
                "          type: integer\n";

        new Expectations() {{
            RefUtils.readExternalUrlRef(root, RefFormat.URL, auths, root,
                    (PermittedUrlsChecker) any);
            times = 1;
            result = contents;
        }};

        ResolverCache cache = new ResolverCache(openAPI, auths, root);
        String absentRef = root + "#/components/schemas/External";
        String deeperRef = root + "#/components/schemas/Foo/properties/value";
        String wrongTypeRef = root + "#/components/schemas/Foo";
        Schema absent = cache.loadRef(absentRef, RefFormat.URL, Schema.class);
        Schema deeper = cache.loadRef(deeperRef, RefFormat.URL, Schema.class);
        Parameter wrongType = cache.loadRef(wrongTypeRef, RefFormat.URL, Parameter.class);

        assertEquals(absent.getType(), "string");
        assertEquals(deeper.getType(), "integer");
        assertNotNull(wrongType);
        assertEquals(wrongType.getName(), "id");
        assertNull(cache.getRootReferenceName(absentRef));
        assertNull(cache.getRootReferenceName(deeperRef));
        assertNull(cache.getRootReferenceName(wrongTypeRef));
    }

    @Test
    public void testRootReuseDoesNotReplaceExistingCanonicalEntry() throws Exception {
        Header rootHeader = new Header().description("root");
        openAPI.components(new Components().addHeaders("Root", rootHeader));
        final String root = "https://example.com/api/root.yaml";
        final String ref = root + "#/components/headers/Root";
        ResolverCache cache = new ResolverCache(openAPI, auths, root);
        Schema existingCanonicalObject = new Schema().type("string");
        Map<String, Object> canonicalCache = getCanonicalResolutionCache(cache);
        canonicalCache.put(ref, existingCanonicalObject);

        Header result = cache.loadRef(ref, RefFormat.URL, Header.class);

        assertSame(result, rootHeader);
        assertSame(cache.getResolutionCache().get(ref), rootHeader);
        assertSame(canonicalCache.get(ref), existingCanonicalObject);
    }

    @Test
    public void testRenameCache() {
        ResolverCache cache = new ResolverCache(openAPI, auths, null);

        assertNull(cache.getRenamedRef("foo"));
        cache.putRenamedRef("foo", "bar");
        assertEquals(cache.getRenamedRef("foo"), "bar");
    }

    private URI getRootDocumentUri(ResolverCache cache) throws Exception {
        Field field = ResolverCache.class.getDeclaredField("rootDocumentUri");
        field.setAccessible(true);
        return (URI) field.get(cache);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getCanonicalResolutionCache(ResolverCache cache) throws Exception {
        Field field = ResolverCache.class.getDeclaredField("canonicalResolutionCache");
        field.setAccessible(true);
        return (Map<String, Object>) field.get(cache);
    }

    @Test
    public void testEquivalentUriRefsShareCacheIdentity() {
        final String refWithDotSegments =
                "http://my.company.com/schemas/../schemas/file.yaml#/components/schemas/Foo";
        final String canonicalRef =
                "http://my.company.com/schemas/file.yaml#/components/schemas/Foo";
        final String contents = "components:\n  schemas:\n    Foo:\n      type: string\n";

        new Expectations() {{
            RefUtils.readExternalUrlRef(
                    "http://my.company.com/schemas/../schemas/file.yaml",
                    RefFormat.URL,
                    auths,
                    "http://my.company.com/root.yaml",
                    (PermittedUrlsChecker) any);
            times = 1;
            result = contents;
        }};

        ResolverCache cache = new ResolverCache(openAPI, auths, "http://my.company.com/root.yaml");
        Schema first = cache.loadRef(refWithDotSegments, RefFormat.URL, Schema.class);
        Schema second = cache.loadRef(canonicalRef, RefFormat.URL, Schema.class);

        assertSame(first, second);
        assertEquals(cache.getExternalFileCache().size(), 2);
        assertEquals(
                cache.getExternalFileCache().get(
                        "http://my.company.com/schemas/../schemas/file.yaml"),
                contents);
        assertEquals(
                cache.getExternalFileCache().get("http://my.company.com/schemas/file.yaml"),
                contents);
        assertEquals(cache.getResolutionCache().size(), 2);
        assertSame(cache.getResolutionCache().get(refWithDotSegments), first);
        assertSame(cache.getResolutionCache().get(canonicalRef), first);
        cache.putRenamedRef(refWithDotSegments, "Foo");
        assertEquals(cache.getRenamedRef(canonicalRef), "Foo");
        assertEquals(cache.getRenameCache().get(refWithDotSegments), "Foo");
        assertNull(cache.getRenameCache().get(canonicalRef));
    }

    @Test
    public void testEquivalentApiResponseRefsShareCacheIdentity() {
        final String refWithDotSegments =
                "http://my.company.com/responses/../responses/common.yaml#/components/responses/Error";
        final String canonicalRef =
                "http://my.company.com/responses/common.yaml#/components/responses/Error";
        final String contents =
                "components:\n  responses:\n    Error:\n      description: Error response\n";

        new Expectations() {{
            RefUtils.readExternalUrlRef(
                    "http://my.company.com/responses/../responses/common.yaml",
                    RefFormat.URL,
                    auths,
                    "http://my.company.com/root.yaml",
                    (PermittedUrlsChecker) any);
            times = 1;
            result = contents;
        }};

        ResolverCache cache = new ResolverCache(openAPI, auths, "http://my.company.com/root.yaml");
        ApiResponse first = cache.loadRef(refWithDotSegments, RefFormat.URL, ApiResponse.class);
        ApiResponse second = cache.loadRef(canonicalRef, RefFormat.URL, ApiResponse.class);

        assertSame(first, second);
        assertEquals(first.getDescription(), "Error response");
        assertEquals(cache.getExternalFileCache().size(), 2);
        assertEquals(cache.getResolutionCache().size(), 2);
        assertSame(cache.getResolutionCache().get(refWithDotSegments), first);
        assertSame(cache.getResolutionCache().get(canonicalRef), first);
    }

    @Test
    public void testIssue2016EquivalentRelativeRefsShareRenameCacheIdentity() {
        ResolverCache cache = new ResolverCache(openAPI, auths, null);
        String refWithRedundantDotSegment = "./../A.yaml#/components/schemas/A";
        String equivalentRef = "../A.yaml#/components/schemas/A";

        cache.putRenamedRef(refWithRedundantDotSegment, "A");

        assertEquals(cache.getRenamedRef(equivalentRef), "A");
        assertEquals(cache.getRenameCache().size(), 1);
        assertEquals(cache.getRenameCache().get(refWithRedundantDotSegment), "A");
        assertNull(cache.getRenameCache().get(equivalentRef));
    }

    @Test
    public void testRefEquivalenceUsesNormalizedCacheIdentity() {
        ResolverCache cache = new ResolverCache(openAPI, auths, null);

        assertTrue(cache.refsAreEquivalent(
                "./components/schemas/Thing.yaml#/components/schemas/Thing",
                "components/foo/../schemas/Thing.yaml#/components/schemas/Thing"));
        assertFalse(cache.refsAreEquivalent(
                "inventory.yaml#/components/schemas/Pet",
                "pets.yaml#/components/schemas/Pet"));
    }

    @Test
    public void testCanonicalRenameCacheKeysPreserveUriParts() {
        ResolverCache cache = new ResolverCache(openAPI, auths, null);

        cache.putRenamedRef(
                "file:///tmp/schemas/../Foo.yaml#/components/schemas/Foo", "FileFoo");
        cache.putRenamedRef(
                "/tmp/schemas/../Foo.yaml#/components/schemas/Foo", "AbsoluteFoo");
        cache.putRenamedRef(
                "https://example.com/a/../Foo.yaml?version=1#/components/schemas/Foo", "HttpFoo");
        cache.putRenamedRef("#/components/schemas/Foo", "InternalFoo");
        cache.putRenamedRef("C:\\schemas\\Foo.yaml", "WindowsPath");
        cache.putRenamedRef(
                "my schemas/../Foo.yaml#/components/schemas/Foo", "UnencodedSpace");

        assertEquals(
                cache.getRenameCache().get(
                        "file:///tmp/schemas/../Foo.yaml#/components/schemas/Foo"),
                "FileFoo");
        assertEquals(
                cache.getRenameCache().get(
                        "/tmp/schemas/../Foo.yaml#/components/schemas/Foo"),
                "AbsoluteFoo");
        assertEquals(
                cache.getRenameCache().get(
                        "https://example.com/a/../Foo.yaml?version=1#/components/schemas/Foo"),
                "HttpFoo");
        assertEquals(cache.getRenameCache().get("#/components/schemas/Foo"), "InternalFoo");
        assertEquals(cache.getRenameCache().get("C:\\schemas\\Foo.yaml"), "WindowsPath");
        assertEquals(
                cache.getRenameCache().get(
                        "my schemas/../Foo.yaml#/components/schemas/Foo"),
                "UnencodedSpace");
        assertEquals(
                cache.getRenamedRef("file:/tmp/Foo.yaml#/components/schemas/Foo"),
                "FileFoo");
        assertEquals(
                cache.getRenamedRef("/tmp/Foo.yaml#/components/schemas/Foo"),
                "AbsoluteFoo");
        assertEquals(
                cache.getRenamedRef(
                        "https://example.com/Foo.yaml?version=1#/components/schemas/Foo"),
                "HttpFoo");
    }
}
