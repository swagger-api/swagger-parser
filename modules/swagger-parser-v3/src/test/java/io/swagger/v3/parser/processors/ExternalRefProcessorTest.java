package io.swagger.v3.parser.processors;


import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.ResolverCache;
import io.swagger.v3.parser.core.models.AuthorizationValue;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import io.swagger.v3.parser.models.RefFormat;
import io.swagger.v3.parser.urlresolver.PermittedUrlsChecker;
import io.swagger.v3.parser.util.RemoteUrl;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertSame;


public class ExternalRefProcessorTest {

    ResolverCache cache = mock(ResolverCache.class);

    OpenAPI openAPI = mock(OpenAPI.class);

    Schema mockedModel = mock(Schema.class);

    private MockedStatic<RemoteUrl> remoteUrl;

    @BeforeMethod
    public void mockRemoteUrl() {
        remoteUrl = Mockito.mockStatic(RemoteUrl.class);
    }

    @AfterMethod(alwaysRun = true)
    public void closeRemoteUrl() {
        remoteUrl.close();
    }

    @Test
    public void testProcessRefToExternalDefinition_NoNameConflict() {

        final String ref = "http://my.company.com/path/to/file.json#/foo/bar";
        final RefFormat refFormat = RefFormat.URL;

        // like a cascaded mock: openAPI.getComponents() returns a mock whose maps are empty
        Components components = mock(Components.class);
        when(openAPI.getComponents()).thenReturn(components);
        when(cache.loadRef(ref, refFormat, Schema.class)).thenReturn(mockedModel);

        String newRef = new ExternalRefProcessor(cache, openAPI).processRefToExternalSchema(ref, refFormat);
        assertEquals(newRef, "bar");

        // the original expectations: each of these calls must have happened
        verify(cache).getRenamedRef(ref);
        verify(cache).loadRef(ref, refFormat, Schema.class);
        verify(cache).putRenamedRef(ref, "bar");
        verify(components).addSchemas("bar", mockedModel);
        verify(cache).addReferencedKey("bar");
    }

    @Test
    public void testSecuritySchemesWithSameExternalNameReceiveDistinctKeys() {
        final String firstRef = "https://example.test/first.yaml#/sharedAuth";
        final String secondRef = "https://example.test/second.yaml#/sharedAuth";
        SecurityScheme first = new SecurityScheme().type(SecurityScheme.Type.APIKEY).name("X-API-Key")
                .in(SecurityScheme.In.HEADER);
        SecurityScheme second = new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer");
        OpenAPI testedOpenAPI = new OpenAPI().components(new Components());

        when(cache.loadRef(firstRef, RefFormat.URL, SecurityScheme.class)).thenReturn(first);
        when(cache.loadRef(secondRef, RefFormat.URL, SecurityScheme.class)).thenReturn(second);

        ExternalRefProcessor processor = new ExternalRefProcessor(cache, testedOpenAPI);
        assertEquals(processor.processRefToExternalSecurityScheme(firstRef, RefFormat.URL), "sharedAuth");
        assertEquals(processor.processRefToExternalSecurityScheme(secondRef, RefFormat.URL), "sharedAuth_1");
        assertEquals(testedOpenAPI.getComponents().getSecuritySchemes().get("sharedAuth"), first);
        assertEquals(testedOpenAPI.getComponents().getSecuritySchemes().get("sharedAuth_1"), second);
    }

    @Test
    public void testResponseAllocatorExaminesOccupiedSuffixesWithoutOverwritingPlaceholder() {
        final String ref = "https://example.test/incoming.yaml#/sharedResponse";
        ApiResponse placeholder = new ApiResponse().$ref("https://example.test/other.yaml#/sharedResponse");
        ApiResponse firstSuffix = new ApiResponse().description("first suffix");
        ApiResponse secondSuffix = new ApiResponse().description("second suffix");
        ApiResponse incoming = new ApiResponse().description("incoming");
        Components components = new Components()
                .addResponses("sharedResponse", placeholder)
                .addResponses("sharedResponse_1", firstSuffix)
                .addResponses("sharedResponse_2", secondSuffix);
        OpenAPI testedOpenAPI = new OpenAPI().components(components);

        when(cache.loadRef(ref, RefFormat.URL, ApiResponse.class)).thenReturn(incoming);

        String assignedName = new ExternalRefProcessor(cache, testedOpenAPI)
                .processRefToExternalResponse(ref, RefFormat.URL);

        assertEquals(assignedName, "sharedResponse_3");
        assertSame(testedOpenAPI.getComponents().getResponses().get("sharedResponse"), placeholder);
        assertEquals(placeholder.get$ref(), "https://example.test/other.yaml#/sharedResponse");
        assertSame(testedOpenAPI.getComponents().getResponses().get("sharedResponse_3"), incoming);
    }

    @Test
    public void testEqualResolvedResponsesReuseOneKey() {
        final String ref = "https://example.test/incoming.yaml#/sharedResponse";
        ApiResponse existing = new ApiResponse().description("same response");
        ApiResponse incoming = new ApiResponse().description("same response");
        OpenAPI testedOpenAPI = new OpenAPI().components(
                new Components().addResponses("sharedResponse", existing));

        when(cache.loadRef(ref, RefFormat.URL, ApiResponse.class)).thenReturn(incoming);

        String assignedName = new ExternalRefProcessor(cache, testedOpenAPI)
                .processRefToExternalResponse(ref, RefFormat.URL);

        assertEquals(assignedName, "sharedResponse");
        assertEquals(testedOpenAPI.getComponents().getResponses().size(), 1);
        assertSame(testedOpenAPI.getComponents().getResponses().get("sharedResponse"), existing);
    }

    @Test
    public void testEquivalentResponsePlaceholderUsesCanonicalReferenceIdentity() {
        final String ref = "https://example.test/responses/common.yaml#/sharedResponse";
        final String equivalentRef = "https://example.test/responses/../responses/common.yaml#/sharedResponse";
        ApiResponse placeholder = new ApiResponse().$ref(equivalentRef);
        ApiResponse incoming = new ApiResponse().description("resolved response");
        OpenAPI testedOpenAPI = new OpenAPI().components(
                new Components().addResponses("sharedResponse", placeholder));

        when(cache.loadRef(ref, RefFormat.URL, ApiResponse.class)).thenReturn(incoming);
        when(cache.refsAreEquivalent(equivalentRef, ref)).thenReturn(true);

        String assignedName = new ExternalRefProcessor(cache, testedOpenAPI)
                .processRefToExternalResponse(ref, RefFormat.URL);

        assertEquals(assignedName, "sharedResponse");
        assertEquals(testedOpenAPI.getComponents().getResponses().size(), 1);
        assertSame(testedOpenAPI.getComponents().getResponses().get("sharedResponse"), incoming);
    }

    @Test
    public void testNonSchemaComponentNamesRemainCaseSensitive() {
        final String ref = "https://example.test/responses.yaml#/SharedResponse";
        ApiResponse lowerCase = new ApiResponse().description("lower case key");
        ApiResponse incoming = new ApiResponse().description("upper case key");
        OpenAPI testedOpenAPI = new OpenAPI().components(
                new Components().addResponses("sharedResponse", lowerCase));

        when(cache.loadRef(ref, RefFormat.URL, ApiResponse.class)).thenReturn(incoming);

        String assignedName = new ExternalRefProcessor(cache, testedOpenAPI)
                .processRefToExternalResponse(ref, RefFormat.URL);

        assertEquals(assignedName, "SharedResponse");
        assertEquals(testedOpenAPI.getComponents().getResponses().size(), 2);
        assertSame(testedOpenAPI.getComponents().getResponses().get("sharedResponse"), lowerCase);
        assertSame(testedOpenAPI.getComponents().getResponses().get("SharedResponse"), incoming);
    }

    @Test
    public void testFailedResponseLoadReturnsOriginalRef() {
        final String ref = "https://example.test/missing.yaml#/MissingResponse";
        OpenAPI testedOpenAPI = new OpenAPI();

        when(cache.loadRef(ref, RefFormat.URL, ApiResponse.class)).thenReturn(null);

        assertEquals(new ExternalRefProcessor(cache, testedOpenAPI)
                .processRefToExternalResponse(ref, RefFormat.URL), ref);
    }

    @Test
    public void testRecursiveExternalSchemaReusesRenameCacheEntry() {
        final String ref = "schemas.yaml#/Recursive";
        Schema recursive = new Schema().$ref(ref);
        OpenAPI testedOpenAPI = new OpenAPI().components(new Components());

        when(cache.getRenamedRef(ref)).thenReturn(null, "Recursive");
        when(cache.loadRef(ref, RefFormat.RELATIVE, Schema.class)).thenReturn(recursive);

        String assignedName = new ExternalRefProcessor(cache, testedOpenAPI)
                .processRefToExternalSchema(ref, RefFormat.RELATIVE);

        assertEquals(assignedName, "Recursive");
        assertEquals(testedOpenAPI.getComponents().getSchemas().size(), 1);
        assertSame(testedOpenAPI.getComponents().getSchemas().get("Recursive"), recursive);
        assertEquals(recursive.get$ref(), "#/components/schemas/Recursive");
    }

    @Test
    public void testSchemaLookupPrefersExactKeyBeforeCaseInsensitiveFallback() {
        final String ref = "https://example.test/schemas.yaml#/Pet";
        Schema lowerCaseSchema = new Schema().addProperties("lower", new StringSchema());
        Schema exactSchema = new Schema().addProperties("exact", new StringSchema());
        Schema incoming = new Schema().addProperties("exact", new StringSchema());
        OpenAPI testedOpenAPI = new OpenAPI().components(new Components()
                .addSchemas("pet", lowerCaseSchema)
                .addSchemas("Pet", exactSchema));

        when(cache.loadRef(ref, RefFormat.URL, Schema.class)).thenReturn(incoming);

        String assignedName = new ExternalRefProcessor(cache, testedOpenAPI)
                .processRefToExternalSchema(ref, RefFormat.URL);

        assertEquals(assignedName, "Pet");
        assertEquals(testedOpenAPI.getComponents().getSchemas().size(), 2);
        assertSame(testedOpenAPI.getComponents().getSchemas().get("Pet"), exactSchema);
        assertSame(testedOpenAPI.getComponents().getSchemas().get("pet"), lowerCaseSchema);
    }

    @Test
    public void testNestedExternalRefs() {
        final RefFormat refFormat = RefFormat.URL;

        //Swagger test instance
        OpenAPI testedOpenAPI = new OpenAPI();

        //Start with customer, add address property to it
        final String customerURL = "http://my.company.com/path/to/customer.json#/definitions/Customer";

        final Schema customerModel = new Schema();
        Map<String, Schema> custProps = new HashMap<>();
        Schema address = new Schema();
        final String addressURL = "http://my.company.com/path/to/address.json#/definitions/Address";
        address.set$ref(addressURL);
        custProps.put("Address", address);

        //Create a 'local' reference to something in #/definitions, this should be ignored a no longer result in a null pointer exception
        final String loyaltyURL = "#/definitions/LoyaltyScheme";

        Schema loyaltyProp = new Schema();
        loyaltyProp.set$ref(loyaltyURL);
        loyaltyProp.setName("LoyaltyCardNumber");
        List<String> required = new ArrayList<>();
        required.add("LoyaltyCardNumber");
        loyaltyProp.setRequired(required);

        custProps.put("Loyalty", loyaltyProp);
        customerModel.setProperties(custProps);

        //create address model, add Contact Ref Property to it
        final Schema addressModel = new Schema();
        Map<String, Schema> addressProps = new HashMap<>();
        Schema contact = new Schema();
        final String contactURL = "http://my.company.com/path/to/Contact.json#/definitions/Contact";
        contact.set$ref(contactURL);
        addressProps.put("Contact", contact);
        addressModel.setProperties(addressProps);


        //Create contact model, with basic type property
        final Schema contactModel = new Schema();
        Schema contactProp = new StringSchema();
        contactProp.setName("PhoneNumber");
        List<String> requiredList = new ArrayList<>();
        requiredList.add("PhoneNumber");
        contactProp.setRequired(requiredList);
        Map<String, Schema> contactProps = new HashMap<>();
        contactProps.put("PhoneNumber", contactProp);
        contactModel.setProperties(contactProps);

        when(cache.loadRef(customerURL, refFormat, Schema.class)).thenReturn(customerModel);
        when(cache.loadRef(addressURL, refFormat, Schema.class)).thenReturn(addressModel);
        when(cache.loadRef(contactURL, refFormat, Schema.class)).thenReturn(contactModel);

        new ExternalRefProcessor(cache, testedOpenAPI).processRefToExternalSchema(customerURL, refFormat);

        verify(cache, times(1)).loadRef(customerURL, refFormat, Schema.class);

        assertNotNull(testedOpenAPI.getComponents().getSchemas().get("Customer"));
        assertNotNull(testedOpenAPI.getComponents().getSchemas().get("Contact"));
        assertNotNull(testedOpenAPI.getComponents().getSchemas().get("Address"));
    }


    @Test
    public void testRelativeRefIncludingUrlRef() throws Exception {
        final RefFormat refFormat = RefFormat.RELATIVE;

        final String url = "https://my.example.remote.url.com/globals.yaml";

        final String expectedResult = "components:\n" +
                "  schemas:\n" +
                "    link-object:\n" +
                "      type: object\n" +
                "      additionalProperties:\n" +
                "        \"$ref\": \"#/components/schemas/rel-data\"\n" +
                "    rel-data:\n" +
                "      type: object\n" +
                "      required:\n" +
                "      - href\n" +
                "      properties:\n" +
                "        href:\n" +
                "          type: string\n" +
                "        note:\n" +
                "          type: string\n" +
                "    result:\n" +
                "      type: object\n" +
                "      properties:\n" +
                "        name:\n" +
                "          type: string\n" +
                "        _links:\n" +
                "          \"$ref\": \"#/components/schemas/link-object\"\n";

        remoteUrl.when(() -> RemoteUrl.urlToString(eq(url), eq(null), nullable(PermittedUrlsChecker.class))).thenReturn(expectedResult);

        OpenAPI mockedOpenAPI = new OpenAPI();
        mockedOpenAPI.setComponents(new Components());
        mockedOpenAPI.getComponents().setSchemas(new HashMap<>());
        ResolverCache mockedResolverCache = new ResolverCache(mockedOpenAPI, null, null);

        ExternalRefProcessor processor = new ExternalRefProcessor(mockedResolverCache, mockedOpenAPI);

        processor.processRefToExternalSchema("./relative-with-url/relative-with-url.yaml#/relative-with-url", refFormat);
        assertEquals(((Schema) mockedOpenAPI.getComponents().getSchemas().get("relative-with-url").getProperties().get("Foo")).get$ref(),
                "https://my.example.remote.url.com/globals.yaml#/components/schemas/link-object");
        assertTrue(mockedOpenAPI.getComponents().getSchemas().containsKey("link-object"));
        assertTrue(mockedOpenAPI.getComponents().getSchemas().containsKey("rel-data"));
        remoteUrl.verify(() -> RemoteUrl.urlToString(eq(url), eq(null), nullable(PermittedUrlsChecker.class)), times(1));
        // assert that ref is relative ref is resolved. and the file path is from root yaml file.
        assertEquals(((Schema) mockedOpenAPI.getComponents().getSchemas().get("relative-with-url").getProperties().get("Bar")).get$ref(),
                "./relative-with-url/relative-with-local.yaml#/relative-same-file");
    }

    @Test
    public void testHandleComposedSchemasInArrayItems() {
        OpenAPIV3Parser openApiParser = new OpenAPIV3Parser();
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        SwaggerParseResult parseResult = openApiParser.readLocation("issue-2071/openapi.yaml", null, options);
        OpenAPI openAPI = parseResult.getOpenAPI();

        Map<String, Schema> components = openAPI.getComponents().getSchemas();
        assertEquals(components.size(), 5);
        assertTrue(components.containsKey("Response"));
        assertTrue(components.containsKey("ProductRow"));
        assertTrue(components.containsKey("ProductRowType"));
        assertTrue(components.containsKey("OrderRow"));
        assertTrue(components.containsKey("OrderRowType"));
    }

    @Test
    public void testHandleInlineRefsInComposedSchemas() {
        OpenAPIV3Parser openApiParser = new OpenAPIV3Parser();
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        SwaggerParseResult parseResult = openApiParser.readLocation("issue-2104/openapi.yaml", null, options);
        OpenAPI openAPI = parseResult.getOpenAPI();

        Map<String, Schema> components = openAPI.getComponents().getSchemas();
        assertEquals(components.size(), 4);
        assertTrue(components.containsKey("ResponseAllOf"));
        assertTrue(components.containsKey("ResponseOneOf"));
        assertTrue(components.containsKey("ResponseAnyOf"));
        assertTrue(components.containsKey("Product"));

        Schema allOfInlineProduct = (Schema) ((Schema) components.get("ResponseAllOf").getAllOf().get(1)).getProperties().get("product");
        assertEquals(allOfInlineProduct.get$ref(), "#/components/schemas/Product");

        Schema oneOfInlineProduct = (Schema) ((Schema) components.get("ResponseOneOf").getOneOf().get(1)).getProperties().get("product");
        assertEquals(oneOfInlineProduct.get$ref(), "#/components/schemas/Product");

        Schema anyOfInlineProduct = (Schema) ((Schema) components.get("ResponseAnyOf").getAnyOf().get(1)).getProperties().get("product");
        assertEquals(anyOfInlineProduct.get$ref(), "#/components/schemas/Product");
    }

    @Test
    public void testAdditionalPropertiesReferenceResolution() {
        String inputSpec = "src/test/resources/issue-2218/main.yaml";
        List<AuthorizationValue> authorizationValues = null;

        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        SwaggerParseResult result = new OpenAPIV3Parser().readLocation(inputSpec, authorizationValues, options);

        OpenAPI openAPI = result.getOpenAPI();

        Map<String, Schema> schemas = openAPI.getComponents().getSchemas();
        assertNotNull(schemas, "Schemas should not be null");
        Assert.assertTrue(schemas.containsKey("FlagsOfFlags"), "FlagsOfFlags schema should be resolved and available in components");
        Assert.assertTrue(schemas.containsKey("Flags"), "Flags schema should be resolved and available in components");
        Assert.assertTrue(schemas.containsKey("Flag"), "Flag schema should be resolved and available in components");
    }

}
