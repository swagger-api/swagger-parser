package io.swagger.v3.parser.test;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.callbacks.Callback;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.links.Link;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class Issue2055Test {

    @Test
    public void externalParametersWithSameNameKeepBothDefinitions() {
        OpenAPI openAPI = parse();
        Operation post = openAPI.getPaths().get("/a-path").getPost();
        Operation put = openAPI.getPaths().get("/a-path").getPut();

        assertEquals(post.getParameters().get(0).getDescription(), "There can be only 1");
        assertEquals(put.getParameters().get(0).getDescription(), "There can be only 2");

        Map<String, Parameter> parameters = openAPI.getComponents().getParameters();
        assertEquals(parameters.size(), 10);
        assertTrue(parameters.keySet().containsAll(Arrays.asList("limit", "limit_1")));
        assertTrue(parameters.values().stream()
                .anyMatch(parameter -> "There can be only 1".equals(parameter.getDescription())));
        assertTrue(parameters.values().stream()
                .anyMatch(parameter -> "There can be only 2".equals(parameter.getDescription())));
    }

    @DataProvider(name = "parameterLocations")
    public Object[][] parameterLocations() {
        return new Object[][]{
                {"path", "pathParam", 0},
                {"query", "queryParam", 1},
                {"header", "headerParam", 2},
                {"cookie", "cookieParam", 3}
        };
    }

    @Test(dataProvider = "parameterLocations")
    public void externalParametersWithSameNameKeepBothDefinitionsForEveryLocation(
            String location, String componentBaseName, int parameterIndex) {
        OpenAPI openAPI = parse();
        Operation post = openAPI.getPaths().get("/parameter-types/{pathParam}").getPost();
        Operation put = openAPI.getPaths().get("/parameter-types/{pathParam}").getPut();
        Parameter first = post.getParameters().get(parameterIndex);
        Parameter second = put.getParameters().get(parameterIndex);

        assertEquals(first.getIn(), location);
        assertEquals(second.getIn(), location);
        assertEquals(first.getDescription(), location + " parameter from file one");
        assertEquals(second.getDescription(), location + " parameter from file two");

        Map<String, Parameter> parameters = openAPI.getComponents().getParameters();
        assertTrue(parameters.keySet().containsAll(Arrays.asList(componentBaseName, componentBaseName + "_1")));
        assertTrue(parameters.values().stream()
                .anyMatch(parameter -> (location + " parameter from file one").equals(parameter.getDescription())));
        assertTrue(parameters.values().stream()
                .anyMatch(parameter -> (location + " parameter from file two").equals(parameter.getDescription())));
    }

    @DataProvider(name = "externalContentComponentTypes")
    public Object[][] externalContentComponentTypes() {
        return new Object[][]{
                {"requestBodies", "a-request", 2},
                {"responses", "a-response", 3}
        };
    }

    @Test(dataProvider = "externalContentComponentTypes")
    public void externalContentComponentsWithSameNameKeepBothDefinitions(
            String componentType, String componentBaseName, int expectedComponentCount) {
        OpenAPI openAPI = parse();
        Operation post = openAPI.getPaths().get("/a-path").getPost();
        Operation put = openAPI.getPaths().get("/a-path").getPut();
        String postRef = contentComponentRef(post, componentType);
        String putRef = contentComponentRef(put, componentType);

        assertNotEquals(postRef, putRef);

        Map<String, ?> components = contentComponents(openAPI, componentType);
        assertEquals(components.size(), expectedComponentCount);
        assertTrue(components.keySet().containsAll(Arrays.asList(componentBaseName, componentBaseName + "_1")));
        assertTrue(content(assertLocalRefResolves(postRef, componentType, components)).containsKey("text/plain"));
        assertTrue(content(assertLocalRefResolves(putRef, componentType, components)).containsKey("application/json"));
    }

    @Test
    public void allCollisionProneComponentTypesKeepDistinctDefinitions() {
        OpenAPI openAPI = parse("src/test/resources/issue-2055/all-components.yaml");

        Map<String, Header> headers = openAPI.getComponents().getHeaders();
        assertTrue(headers.keySet().containsAll(Arrays.asList(
                "sharedHeader", "sharedHeader_1", "sharedHeader_2", "sharedHeader_3", "sharedHeader_4")));
        assertEquals(headers.size(), 5, "Repeated references after suffix allocation must reuse their assigned key");
        assertTrue(headers.values().stream().anyMatch(header -> "header from file one".equals(header.getDescription())));
        assertTrue(headers.values().stream().anyMatch(header -> "header from file two".equals(header.getDescription())));

        Map<String, Link> links = openAPI.getComponents().getLinks();
        assertTrue(links.keySet().containsAll(Arrays.asList("sharedLink", "sharedLink_1")));
        assertNotEquals(links.get("sharedLink").getOperationId(), links.get("sharedLink_1").getOperationId());

        Map<String, Example> examples = openAPI.getComponents().getExamples();
        assertTrue(examples.keySet().containsAll(Arrays.asList(
                "sharedExample", "sharedExample_1", "equalExample")));
        assertFalse(examples.containsKey("equalExample_1"));

        Map<String, Callback> callbacks = openAPI.getComponents().getCallbacks();
        assertTrue(callbacks.keySet().containsAll(Arrays.asList("sharedCallback", "sharedCallback_1")));
        assertEquals(callbacks.size(), 2, "Equivalent URI spellings must reuse the canonical callback key");
    }

    @Test
    public void securitySchemesRetainTheirLocalKeys() {
        OpenAPI openAPI = parse("src/test/resources/issue-2055/all-components.yaml");
        Map<String, SecurityScheme> schemes = openAPI.getComponents().getSecuritySchemes();

        assertEquals(schemes.size(), 2);
        assertEquals(schemes.get("firstAuth").getType(), SecurityScheme.Type.APIKEY);
        assertEquals(schemes.get("secondAuth").getType(), SecurityScheme.Type.HTTP);
    }

    @Test
    public void everyRewrittenLocalReferenceResolvesToItsExpectedComponent() {
        OpenAPI openAPI = parse("src/test/resources/issue-2055/all-components.yaml");
        Operation first = openAPI.getPaths().get("/first").getPost();
        Operation second = openAPI.getPaths().get("/second").getPost();
        Operation equivalent = openAPI.getPaths().get("/equivalent").getPost();

        RequestBody firstRequest = assertLocalRefResolves(first.getRequestBody().get$ref(), "requestBodies",
                openAPI.getComponents().getRequestBodies());
        RequestBody secondRequest = assertLocalRefResolves(second.getRequestBody().get$ref(), "requestBodies",
                openAPI.getComponents().getRequestBodies());
        assertEquals(firstRequest.getDescription(), "request from file one");
        assertEquals(secondRequest.getDescription(), "request from file two");

        ApiResponse firstResponse = assertLocalRefResolves(first.getResponses().get("200").get$ref(), "responses",
                openAPI.getComponents().getResponses());
        ApiResponse secondResponse = assertLocalRefResolves(second.getResponses().get("200").get$ref(), "responses",
                openAPI.getComponents().getResponses());
        assertEquals(firstResponse.getDescription(), "response from file one");
        assertEquals(secondResponse.getDescription(), "response from file two");

        Callback firstCallback = assertLocalRefResolves(first.getCallbacks().get("event").get$ref(), "callbacks",
                openAPI.getComponents().getCallbacks());
        Callback secondCallback = assertLocalRefResolves(second.getCallbacks().get("event").get$ref(), "callbacks",
                openAPI.getComponents().getCallbacks());
        Callback repeatedFirstCallback = assertLocalRefResolves(equivalent.getCallbacks().get("event").get$ref(),
                "callbacks", openAPI.getComponents().getCallbacks());
        assertTrue(firstCallback.containsKey("{$request.body#/callbackUrl}"));
        assertTrue(secondCallback.containsKey("{$request.body#/callbackUrl}"));
        assertEquals(firstCallback.get("{$request.body#/callbackUrl}").getPost()
                .getResponses().get("204").getDescription(), "callback from file one");
        assertEquals(secondCallback.get("{$request.body#/callbackUrl}").getPost()
                .getResponses().get("204").getDescription(), "callback from file two");
        assertSame(repeatedFirstCallback, firstCallback);
    }

    private OpenAPI parse() {
        return parse("src/test/resources/issue-2055/openapi.json");
    }

    private OpenAPI parse(String location) {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);

        SwaggerParseResult result = new OpenAPIV3Parser()
                .readLocation(location, null, options);

        assertNotNull(result.getOpenAPI());
        assertTrue(result.getMessages().isEmpty(), "Unexpected parser messages: " + result.getMessages());
        assertNotNull(result.getOpenAPI().getComponents());
        return result.getOpenAPI();
    }

    private String componentName(String ref) {
        return ref.substring(ref.lastIndexOf('/') + 1);
    }

    private String contentComponentRef(Operation operation, String componentType) {
        if ("requestBodies".equals(componentType)) {
            return operation.getRequestBody().get$ref();
        }
        return operation.getResponses().get("200").get$ref();
    }

    private Map<String, ?> contentComponents(OpenAPI openAPI, String componentType) {
        if ("requestBodies".equals(componentType)) {
            return openAPI.getComponents().getRequestBodies();
        }
        return openAPI.getComponents().getResponses();
    }

    private Content content(Object component) {
        if (component instanceof RequestBody) {
            return ((RequestBody) component).getContent();
        }
        return ((ApiResponse) component).getContent();
    }

    private <T> T assertLocalRefResolves(String ref, String componentType, Map<String, T> components) {
        assertNotNull(ref);
        assertTrue(ref.startsWith("#/components/" + componentType + "/"), "Expected a local component ref: " + ref);
        T component = components.get(componentName(ref));
        assertNotNull(component, "Reference does not resolve to a component: " + ref);
        return component;
    }
}
