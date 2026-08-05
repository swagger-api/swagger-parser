package io.swagger.v3.parser.processors;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.callbacks.Callback;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.links.Link;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.parser.ResolverCache;
import io.swagger.v3.parser.models.RefFormat;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;

/**
 * Regression coverage for dotted external non-schema components. Assertions describe the intended resolved output
 * and intentionally fail where processors currently leave bare names, lose failed refs, or reuse a colliding key.
 */
public class DottedNonSchemaComponentTest {

    // Wrong: the parser leaves "error.response" as a bare ref.
    // It should create the internal ref "#/components/responses/error.response".
    @Test
    public void testDottedExternalResponseUsesQualifiedInternalRef() {
        String ref = "./responses/error.response.yaml";
        OpenAPI openAPI = new OpenAPI();
        ApiResponse resolved = new ApiResponse().description("Bad request");
        ApiResponse response = new ApiResponse().$ref(ref);

        new ResponseProcessor(cacheWith(openAPI, ref, resolved), openAPI).processResponse(response);

        assertEquals(response.get$ref(), "#/components/responses/error.response");
        assertSame(openAPI.getComponents().getResponses().get("error.response"), resolved);
    }

    // Wrong: the parser leaves "create.request" as a bare ref.
    // It should create the internal ref "#/components/requestBodies/create.request".
    @Test
    public void testDottedExternalRequestBodyUsesQualifiedInternalRef() {
        String ref = "./request-bodies/create.request.yaml";
        OpenAPI openAPI = new OpenAPI();
        RequestBody resolved = new RequestBody().description("Create request");
        RequestBody requestBody = new RequestBody().$ref(ref);

        new RequestBodyProcessor(cacheWith(openAPI, ref, resolved), openAPI).processRequestBody(requestBody);

        assertEquals(requestBody.get$ref(), "#/components/requestBodies/create.request");
        assertSame(openAPI.getComponents().getRequestBodies().get("create.request"), resolved);
    }

    // Wrong: the parser leaves "request.id" as a bare ref.
    // It should create the internal ref "#/components/parameters/request.id".
    @Test
    public void testDottedExternalParameterUsesQualifiedInternalRef() {
        String ref = "./parameters/request.id.yaml";
        OpenAPI openAPI = new OpenAPI();
        Parameter resolved = new Parameter().name("requestId").in("query");
        Parameter parameter = new Parameter().$ref(ref);

        new ParameterProcessor(cacheWith(openAPI, ref, resolved), openAPI).processParameter(parameter);

        assertEquals(parameter.get$ref(), "#/components/parameters/request.id");
        assertSame(openAPI.getComponents().getParameters().get("request.id"), resolved);
    }

    // Wrong: the parser leaves "rate.limit" as a bare ref.
    // It should create the internal ref "#/components/headers/rate.limit".
    @Test
    public void testDottedExternalHeaderUsesQualifiedInternalRef() {
        String ref = "./headers/rate.limit.yaml";
        OpenAPI openAPI = new OpenAPI();
        Header resolved = new Header().description("Rate limit");
        Header header = new Header().$ref(ref);

        new HeaderProcessor(cacheWith(openAPI, ref, resolved), openAPI).processHeader(header);

        assertEquals(header.get$ref(), "#/components/headers/rate.limit");
        assertSame(openAPI.getComponents().getHeaders().get("rate.limit"), resolved);
    }

    // Wrong: the parser leaves "error.example" as a bare ref.
    // It should create the internal ref "#/components/examples/error.example".
    @Test
    public void testDottedExternalExampleUsesQualifiedInternalRef() {
        String ref = "./examples/error.example.yaml";
        OpenAPI openAPI = new OpenAPI();
        Example resolved = new Example().summary("Error example");
        Example example = new Example().$ref(ref);

        new ExampleProcessor(cacheWith(openAPI, ref, resolved), openAPI).processExample(example);

        assertEquals(example.get$ref(), "#/components/examples/error.example");
        assertSame(openAPI.getComponents().getExamples().get("error.example"), resolved);
    }

    // Wrong: the parser leaves "account.link" as a bare ref.
    // It should create the internal ref "#/components/links/account.link".
    @Test
    public void testDottedExternalLinkUsesQualifiedInternalRef() {
        String ref = "./links/account.link.yaml";
        OpenAPI openAPI = new OpenAPI();
        Link resolved = new Link().operationId("getAccount");
        Link link = new Link().$ref(ref);

        new LinkProcessor(cacheWith(openAPI, ref, resolved), openAPI).processLink(link);

        assertEquals(link.get$ref(), "#/components/links/account.link");
        assertSame(openAPI.getComponents().getLinks().get("account.link"), resolved);
    }

    @Test
    public void testDottedExternalCallbackUsesQualifiedInternalRef() {
        String ref = "./callbacks/payment.callback.yaml";
        OpenAPI openAPI = new OpenAPI();
        Callback resolved = new Callback();
        Callback callback = new Callback();
        callback.set$ref(ref);

        new CallbackProcessor(cacheWith(openAPI, ref, resolved), openAPI).processCallback(callback);

        assertEquals(callback.get$ref(), "#/components/callbacks/payment.callback");
        assertSame(openAPI.getComponents().getCallbacks().get("payment.callback"), resolved);
    }

    @Test
    public void testDottedExternalSecuritySchemeUsesDottedComponentName() {
        String ref = "./security/oauth.scheme.yaml";
        OpenAPI openAPI = new OpenAPI();
        SecurityScheme resolved = new SecurityScheme().type(SecurityScheme.Type.OAUTH2);
        StubResolverCache cache = cacheWith(openAPI, ref, resolved);

        String name = new ExternalRefProcessor(cache, openAPI)
                .processRefToExternalSecurityScheme(ref, RefFormat.RELATIVE);

        assertEquals(name, "oauth.scheme");
        assertSame(openAPI.getComponents().getSecuritySchemes().get("oauth.scheme"), resolved);
    }

    @Test
    public void testDottedExternalPathItemCachesDottedDerivedName() {
        String ref = "./paths/account.path.yaml";
        OpenAPI openAPI = new OpenAPI();
        PathItem resolved = new PathItem();
        StubResolverCache cache = cacheWith(openAPI, ref, resolved);

        PathItem result = new ExternalRefProcessor(cache, openAPI)
                .processRefToExternalPathItem(ref, RefFormat.RELATIVE);

        assertSame(result, resolved);
        assertEquals(cache.getRenamedRef(ref), "account.path");
    }

    // Wrong: a missing file causes a NullPointerException.
    // For example, "./responses/missing.response.yaml" should stay unchanged when it cannot be loaded.
    @Test
    public void testMissingExternalResponseRetainsOriginalRef() {
        String ref = "./responses/missing.response.yaml";
        OpenAPI openAPI = new OpenAPI();
        ApiResponse response = new ApiResponse().$ref(ref);

        new ResponseProcessor(new StubResolverCache(openAPI), openAPI).processResponse(response);

        assertEquals(response.get$ref(), ref);
    }

    // Wrong: both files are given the same name, "error.response", so the second response is lost.
    // The second component should use "error.response_1".
    @Test
    public void testDottedExternalResponseCollisionUsesDeterministicSuffix() {
        String firstRef = "./first/error.response.yaml";
        String secondRef = "./second/error.response.yaml";
        OpenAPI openAPI = new OpenAPI();
        StubResolverCache cache = new StubResolverCache(openAPI);
        ApiResponse first = new ApiResponse().description("First");
        ApiResponse second = new ApiResponse().description("Second");
        cache.add(firstRef, first);
        cache.add(secondRef, second);
        ExternalRefProcessor processor = new ExternalRefProcessor(cache, openAPI);

        assertEquals(processor.processRefToExternalResponse(firstRef, RefFormat.RELATIVE), "error.response");
        assertEquals(processor.processRefToExternalResponse(secondRef, RefFormat.RELATIVE), "error.response_1");
        assertSame(openAPI.getComponents().getResponses().get("error.response"), first);
        assertSame(openAPI.getComponents().getResponses().get("error.response_1"), second);
    }

    private StubResolverCache cacheWith(OpenAPI openAPI, String ref, Object value) {
        StubResolverCache cache = new StubResolverCache(openAPI);
        cache.add(ref, value);
        return cache;
    }

    private static class StubResolverCache extends ResolverCache {
        private final Map<String, Object> refs = new HashMap<>();

        StubResolverCache(OpenAPI openAPI) {
            super(openAPI, null, null);
        }

        void add(String ref, Object value) {
            refs.put(ref, value);
        }

        @Override
        public <T> T loadRef(String ref, RefFormat refFormat, Class<T> expectedType) {
            Object value = refs.get(ref);
            return value == null ? null : expectedType.cast(value);
        }
    }
}
