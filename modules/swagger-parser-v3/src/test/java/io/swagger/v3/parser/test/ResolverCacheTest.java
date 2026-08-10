package io.swagger.v3.parser.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

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
    public void testRenameCache() {
        ResolverCache cache = new ResolverCache(openAPI, auths, null);

        assertNull(cache.getRenamedRef("foo"));
        cache.putRenamedRef("foo", "bar");
        assertEquals(cache.getRenamedRef("foo"), "bar");
    }
}
