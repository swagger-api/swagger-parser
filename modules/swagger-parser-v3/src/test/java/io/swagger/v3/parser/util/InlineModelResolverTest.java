package io.swagger.v3.parser.util;



import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.testng.annotations.Test;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;

@SuppressWarnings({"static-method", "rawtypes"})
public class InlineModelResolverTest {

    @Test
    public void testIssue1018() throws Exception {
        ParseOptions options = new ParseOptions();
        options.setFlatten(true);
        OpenAPI openAPI = new OpenAPIV3Parser().read("flatten.json",null, options);

        assertNotNull(openAPI);
        assertNotNull(openAPI.getComponents().getSchemas().get("ReturnInformation_manufacturer_signin_credentials").getRequired());
    }


    @Test
    public void testIssue705() throws Exception {
        ParseOptions options = new ParseOptions();
        options.setFlatten(true);
        OpenAPI openAPI = new OpenAPIV3Parser().read("issue-705.yaml",null, options);

        assertNotNull(openAPI);
        assertNotNull(openAPI.getComponents().getSchemas().get("inline_response_200").getType());
    }


    @Test
    public void resolveInlineModelTestWithoutTitle() throws Exception {
        OpenAPI openAPI = new OpenAPI();
        openAPI.setComponents(new Components());


        Schema objectSchema = new ObjectSchema();
        objectSchema.setDefault("default");
        objectSchema.setReadOnly(false);
        objectSchema.setDescription("description");
        objectSchema.setName("name");
        objectSchema.addProperties("street", new StringSchema());
        objectSchema.addProperties("city", new StringSchema());

        Schema schema =  new Schema();
        schema.setName("user");
        schema.setDescription("a common user");
        List<String> required = new ArrayList<>();
        required.add("address");
        schema.setRequired(required);
        schema.addProperties("name", new StringSchema());
        schema.addProperties("address", objectSchema);


        openAPI.getComponents().addSchemas("User", schema);

        new InlineModelResolver().flatten(openAPI);

        Schema user = openAPI.getComponents().getSchemas().get("User");

        assertNotNull(user);
        Schema address = (Schema)user.getProperties().get("address");
        assertTrue((address.get$ref()!= null));
        Schema userAddress = openAPI.getComponents().getSchemas().get("User_address");
        assertNotNull(userAddress);
        assertNotNull(userAddress.getProperties().get("city"));
        assertNotNull(userAddress.getProperties().get("street"));
    }

    @Test
    public void resolveInlineModelTestWithTitle() throws Exception {
        OpenAPI openAPI = new OpenAPI();
        openAPI.setComponents(new Components());


        Schema objectSchema = new ObjectSchema();
        objectSchema.setTitle("UserAddressTitle");
        objectSchema.setDefault("default");
        objectSchema.setReadOnly(false);
        objectSchema.setDescription("description");
        objectSchema.setName("name");
        objectSchema.addProperties("street", new StringSchema());
        objectSchema.addProperties("city", new StringSchema());

        Schema schema =  new Schema();
        schema.setName("user");
        schema.setDescription("a common user");
        List<String> required = new ArrayList<>();
        required.add("address");
        schema.setRequired(required);
        schema.addProperties("name", new StringSchema());
        schema.addProperties("address", objectSchema);


        openAPI.getComponents().addSchemas("User", schema);

        new InlineModelResolver().flatten(openAPI);

        Schema user = openAPI.getComponents().getSchemas().get("User");

        assertNotNull(user);
        Schema address = (Schema)user.getProperties().get("address");
        assertTrue( address.get$ref() != null);

        Schema userAddressTitle = openAPI.getComponents().getSchemas().get("UserAddressTitle");
        assertNotNull(userAddressTitle);
        assertNotNull(userAddressTitle.getProperties().get("city"));
        assertNotNull(userAddressTitle.getProperties().get("street"));
    }

    @Test
    public void resolveInlineModel2EqualInnerModels() throws Exception {
        OpenAPI openAPI = new OpenAPI();
        openAPI.setComponents(new Components());


        Schema objectSchema = new ObjectSchema();
        objectSchema.setTitle("UserAddressTitle");
        objectSchema.setDefault("default");
        objectSchema.setReadOnly(false);
        objectSchema.setDescription("description");
        objectSchema.setName("name");
        objectSchema.addProperties("street", new StringSchema());
        objectSchema.addProperties("city", new StringSchema());

        Schema schema =  new Schema();
        schema.setName("user");
        schema.setDescription("a common user");
        List<String> required = new ArrayList<>();
        required.add("address");
        schema.setRequired(required);
        schema.addProperties("name", new StringSchema());
        schema.addProperties("address", objectSchema);


        openAPI.getComponents().addSchemas("User", schema);

        Schema addressSchema = new ObjectSchema();
        addressSchema.setTitle("UserAddressTitle");
        addressSchema.setDefault("default");
        addressSchema.setReadOnly(false);
        addressSchema.setDescription("description");
        addressSchema.setName("name");
        addressSchema.addProperties("street", new StringSchema());
        addressSchema.addProperties("city", new StringSchema());

        Schema anotherSchema =  new Schema();
        anotherSchema.setName("user");
        anotherSchema.setDescription("a common user");
        List<String> requiredFields = new ArrayList<>();
        requiredFields.add("address");
        anotherSchema.setRequired(requiredFields);
        anotherSchema.addProperties("name", new StringSchema());
        anotherSchema.addProperties("lastName", new StringSchema());
        anotherSchema.addProperties("address", addressSchema);

        openAPI.getComponents().addSchemas("AnotherUser", anotherSchema);

        new InlineModelResolver().flatten(openAPI);

        Schema user = openAPI.getComponents().getSchemas().get("User");

        assertNotNull(user);
        Schema addressSchema1 = (Schema) user.getProperties().get("address");
        assertTrue(addressSchema1.get$ref() != null);

        Schema address = openAPI.getComponents().getSchemas().get("UserAddressTitle");
        assertNotNull(address);
        assertNotNull(address.getProperties().get("city"));
        assertNotNull(address.getProperties().get("street"));
        Schema duplicateAddress = openAPI.getComponents().getSchemas().get("UserAddressTitle_0");
        assertNull(duplicateAddress);
    }

    @Test
    public void resolveInlineModel2DifferentInnerModelsWIthSameTitle() throws Exception {
        OpenAPI openAPI = new OpenAPI();
        openAPI.setComponents(new Components());

        Schema objectSchema = new ObjectSchema();
        objectSchema.setTitle("UserAddressTitle");
        objectSchema.setDefault("default");
        objectSchema.setReadOnly(false);
        objectSchema.setDescription("description");
        objectSchema.setName("name");
        objectSchema.addProperties("street", new StringSchema());
        objectSchema.addProperties("city", new StringSchema());

        Schema schema =  new Schema();
        schema.setName("user");
        schema.setDescription("a common user");
        List<String> required = new ArrayList<>();
        required.add("address");
        schema.setRequired(required);
        schema.addProperties("name", new StringSchema());
        schema.addProperties("address", objectSchema);


        openAPI.getComponents().addSchemas("User", schema);

        Schema addressSchema = new ObjectSchema();
        addressSchema.setTitle("UserAddressTitle");
        addressSchema.setDefault("default");
        addressSchema.setReadOnly(false);
        addressSchema.setDescription("description");
        addressSchema.setName("name");
        addressSchema.addProperties("street", new StringSchema());
        addressSchema.addProperties("city", new StringSchema());
        addressSchema.addProperties("apartment", new StringSchema());


        Schema anotherSchema =  new Schema();
        anotherSchema.setName("AnotherUser");
        anotherSchema.setDescription("a common user");
        List<String> requiredFields = new ArrayList<>();
        requiredFields.add("address");
        anotherSchema.setRequired(requiredFields);
        anotherSchema.addProperties("name", new StringSchema());
        anotherSchema.addProperties("lastName", new StringSchema());
        anotherSchema.addProperties("address", addressSchema);


        openAPI.getComponents().addSchemas("AnotherUser", anotherSchema);

        new InlineModelResolver().flatten(openAPI);

        Schema user = openAPI.getComponents().getSchemas().get("User");

        assertNotNull(user);
        Schema userAddress = (Schema) user.getProperties().get("address");
        assertTrue( userAddress.get$ref()!= null);

        Schema address = openAPI.getComponents().getSchemas().get("UserAddressTitle");
        assertNotNull(address);
        assertNotNull(address.getProperties().get("city"));
        assertNotNull(address.getProperties().get("street"));
        Schema duplicateAddress = openAPI.getComponents().getSchemas().get("UserAddressTitle_1");
        assertNotNull(duplicateAddress);
        assertNotNull(duplicateAddress.getProperties().get("city"));
        assertNotNull(duplicateAddress.getProperties().get("street"));
        assertNotNull(duplicateAddress.getProperties().get("apartment"));
    }


    @Test
    public void testInlineResponseModel() throws Exception {
        OpenAPI openAPI = new OpenAPI();


        StringSchema stringSchema1 = new StringSchema();

        ObjectSchema objectSchema1 = new ObjectSchema();
        objectSchema1.addProperties("name", stringSchema1);
        objectSchema1.addExtension("x-ext", "ext-prop");

        MediaType mediaType1 = new MediaType();
        mediaType1.setSchema(objectSchema1);

        Content content1 = new Content();
        content1.addMediaType("*/*", mediaType1 );

        ApiResponse response1= new ApiResponse();
        response1.setDescription("it works!");
        response1.setContent(content1);

        ApiResponses responses1 = new ApiResponses();
        responses1.addApiResponse("200",response1);

        Operation operation1 = new Operation();
        operation1.setResponses(responses1);

        PathItem pathItem1 = new PathItem();
        pathItem1.setGet(operation1);
        openAPI.path("/foo/bar",pathItem1);



        StringSchema stringSchema2 = new StringSchema();

        ObjectSchema objectSchema2 = new ObjectSchema();
        objectSchema2.addProperties("name", stringSchema2);
        objectSchema2.addExtension("x-ext", "ext-prop");
        MediaType mediaType2 = new MediaType();
        mediaType2.setSchema(objectSchema2);

        Content content2 = new Content();
        content2.addMediaType("*/*", mediaType2 );

        ApiResponse response2 = new ApiResponse();
        response2.setDescription("it works!");
        response2.addExtension("x-foo","bar");
        response2.setContent(content2);

        ApiResponses responses2 = new ApiResponses();
        responses2.addApiResponse("200",response2);

        Operation operation2 = new Operation();
        operation2.setResponses(responses2);

        PathItem pathItem2 = new PathItem();
        pathItem2.setGet(operation2);
        openAPI.path("/foo/baz",pathItem2);


        new InlineModelResolver().flatten(openAPI);

        Map<String, ApiResponse> responses = openAPI.getPaths().get("/foo/bar").getGet().getResponses();

        ApiResponse response = responses.get("200");
        assertNotNull(response);

        Schema schema = response.getContent().get("*/*").getSchema();
        assertTrue(schema.get$ref() != null);
        assertEquals(1, schema.getExtensions().size());
        assertEquals("ext-prop", schema.getExtensions().get("x-ext"));

        Schema model = openAPI.getComponents().getSchemas().get("inline_response_200");
        assertTrue(model.getProperties().size() == 1);
        assertNotNull(model.getProperties().get("name"));
        assertTrue(model.getProperties().get("name") instanceof StringSchema);
    }


    @Test
    public void testInlineResponseModelWithTitle() throws Exception {
        OpenAPI openAPI = new OpenAPI();

        String responseTitle = "GetBarResponse";

        StringSchema stringSchema1 = new StringSchema();

        ObjectSchema objectSchema1 = new ObjectSchema();
        objectSchema1.setTitle(responseTitle);
        objectSchema1.addProperties("name", stringSchema1);


        MediaType mediaType1 = new MediaType();
        mediaType1.setSchema(objectSchema1);

        Content content1 = new Content();
        content1.addMediaType("*/*", mediaType1 );

        ApiResponse response1= new ApiResponse();
        response1.setDescription("it works!");
        response1.setContent(content1);

        ApiResponses responses1 = new ApiResponses();
        responses1.addApiResponse("200",response1);

        Operation operation1 = new Operation();
        operation1.setResponses(responses1);

        PathItem pathItem1 = new PathItem();
        pathItem1.setGet(operation1);
        openAPI.path("/foo/bar",pathItem1);



        StringSchema stringSchema2 = new StringSchema();

        ObjectSchema objectSchema2 = new ObjectSchema();
        objectSchema2.addProperties("name", stringSchema2);
        objectSchema2.addExtension("x-foo", "bar");

        MediaType mediaType2 = new MediaType();
        mediaType2.setSchema(objectSchema2);

        Content content2 = new Content();
        content2.addMediaType("*/*", mediaType2 );

        ApiResponse response2 = new ApiResponse();
        response2.setDescription("it works!");

        response2.setContent(content2);

        ApiResponses responses2 = new ApiResponses();
        responses2.addApiResponse("200",response2);

        Operation operation2 = new Operation();
        operation2.setResponses(responses2);

        PathItem pathItem2 = new PathItem();
        pathItem2.setGet(operation2);
        openAPI.path("/foo/baz",pathItem2);



        new InlineModelResolver().flatten(openAPI);

        Map<String, ApiResponse> responses = openAPI.getPaths().get("/foo/bar").getGet().getResponses();

        ApiResponse response = responses.get("200");
        assertNotNull(response);
        assertTrue(response.getContent().get("*/*").getSchema().get$ref() != null );

        Schema model = openAPI.getComponents().getSchemas().get(responseTitle);
        assertTrue(model.getProperties().size() == 1);
        assertNotNull(model.getProperties().get("name"));
        assertTrue(model.getProperties().get("name") instanceof StringSchema);
    }

    @Test
    public void testSkipInlineMatchesFalse() {
        final OpenAPI openAPI = new OpenAPI();

        final InlineModelResolver inlineModelResolver = new InlineModelResolver();

        final Schema operationAlphaInAsset = new ObjectSchema();
        operationAlphaInAsset.setTitle("operationAlphaInAsset");
        operationAlphaInAsset.addProperties("id1", new IntegerSchema());
        operationAlphaInAsset.addProperties("id2", new IntegerSchema());

        final Schema operationAlphaIn = new ObjectSchema();
        operationAlphaIn.setTitle("operationAlphaIn");
        operationAlphaIn.addProperties("asset", operationAlphaInAsset);

        final Schema operationAlphaRequest = new ObjectSchema();
        operationAlphaRequest.setTitle("operationAlphaRequest");
        operationAlphaRequest.addProperties("in", operationAlphaIn);

        final Schema operationBetaInAsset = new ObjectSchema();
        operationBetaInAsset.setTitle("operationBetaInAsset");
        operationBetaInAsset.addProperties("id1", new IntegerSchema());
        operationBetaInAsset.addProperties("id2", new IntegerSchema());

        final Schema operationBetaIn = new ObjectSchema();
        operationBetaIn.setTitle("operationBetaIn");
        operationBetaIn.addProperties("asset", operationBetaInAsset);

        final Schema operationBetaRequest = new ObjectSchema();
        operationBetaRequest.setTitle("operationBetaRequest");
        operationBetaRequest.addProperties("in", operationBetaIn);

        openAPI.path("/operationAlpha", new PathItem()
                .get(new Operation()
                        .requestBody(new RequestBody()
                                .content(new Content().addMediaType("*/*", new MediaType()
                                        .schema(operationAlphaRequest))))));

        openAPI.path("/operationBeta", new PathItem()
                .get(new Operation()
                        .requestBody(new RequestBody()
                                .content(new Content().addMediaType("*/*", new MediaType()
                                        .schema(operationBetaRequest))))));

        inlineModelResolver.flatten(openAPI);

        assertNotNull(openAPI);
        assertNotNull(openAPI.getComponents());
        assertNotNull(openAPI.getComponents().getSchemas());
        assertEquals(openAPI.getComponents().getSchemas().size(), 6);
    }

    @Test
    public void testSkipInlineMatchesTrue() {
        final OpenAPI openAPI = new OpenAPI();

        final InlineModelResolver inlineModelResolver = new InlineModelResolver(false, false, true, false);

        final Schema operationAlphaInAsset = new ObjectSchema();
        operationAlphaInAsset.setTitle("operationAlphaInAsset");
        operationAlphaInAsset.addProperties("id1", new IntegerSchema());
        operationAlphaInAsset.addProperties("id2", new IntegerSchema());

        final Schema operationAlphaIn = new ObjectSchema();
        operationAlphaIn.setTitle("operationAlphaIn");
        operationAlphaIn.addProperties("asset", operationAlphaInAsset);

        final Schema operationAlphaRequest = new ObjectSchema();
        operationAlphaRequest.setTitle("operationAlphaRequest");
        operationAlphaRequest.addProperties("in", operationAlphaIn);

        final Schema operationBetaInAsset = new ObjectSchema();
        operationBetaInAsset.setTitle("operationBetaInAsset");
        operationBetaInAsset.addProperties("id1", new IntegerSchema());
        operationBetaInAsset.addProperties("id2", new IntegerSchema());

        final Schema operationBetaIn = new ObjectSchema();
        operationBetaIn.setTitle("operationBetaIn");
        operationBetaIn.addProperties("asset", operationBetaInAsset);

        final Schema operationBetaRequest = new ObjectSchema();
        operationBetaRequest.setTitle("operationBetaRequest");
        operationBetaRequest.addProperties("in", operationBetaIn);

        openAPI.path("/operationAlpha", new PathItem()
                .get(new Operation()
                        .requestBody(new RequestBody()
                                .content(new Content().addMediaType("*/*", new MediaType()
                                        .schema(operationAlphaRequest))))));

        openAPI.path("/operationBeta", new PathItem()
                .get(new Operation()
                        .requestBody(new RequestBody()
                                .content(new Content().addMediaType("*/*", new MediaType()
                                        .schema(operationBetaRequest))))));

        inlineModelResolver.flatten(openAPI);

        assertNotNull(openAPI);
        assertNotNull(openAPI.getComponents());
        assertNotNull(openAPI.getComponents().getSchemas());
        assertEquals(6, openAPI.getComponents().getSchemas().size());
    }

    @Test
    public void resolveInlineArrayModelWithTitle() throws Exception {
        OpenAPI openAPI = new OpenAPI();
        openAPI.setComponents(new Components());

        Schema objectSchema = new ObjectSchema();
        objectSchema.setTitle("InnerUserTitle");
        objectSchema.setDefault("default");
        objectSchema.setReadOnly(false);
        objectSchema.setDescription("description");
        objectSchema.setName("name");
        objectSchema.addProperties("street", new StringSchema());
        objectSchema.addProperties("city", new StringSchema());

        ArraySchema arraySchema =  new ArraySchema();
        List<String> required = new LinkedList<>();
        required.add("name");
        arraySchema.setRequired(required);
        arraySchema.setItems(objectSchema);


        openAPI.getComponents().addSchemas("User", arraySchema);


        new InlineModelResolver().flatten(openAPI);

        Schema model = openAPI.getComponents().getSchemas().get("User");
        assertTrue(model instanceof ArraySchema);

        Schema user = openAPI.getComponents().getSchemas().get("InnerUserTitle");
        assertNotNull(user);
        assertEquals("description", user.getDescription());
    }

    @Test
    public void resolveInlineArrayModelWithoutTitle() throws Exception {
        OpenAPI openAPI = new OpenAPI();
        openAPI.setComponents(new Components());

        Schema objectSchema = new ObjectSchema();
        objectSchema.setDefault("default");
        objectSchema.setReadOnly(false);
        objectSchema.setDescription("description");
        objectSchema.setName("name");
        objectSchema.addProperties("street", new StringSchema());
        objectSchema.addProperties("city", new StringSchema());

        ArraySchema arraySchema =  new ArraySchema();
        List<String> required = new LinkedList<>();
        required.add("name");
        arraySchema.setRequired(required);
        arraySchema.setItems(objectSchema);

        openAPI.getComponents().addSchemas("User", arraySchema);


        new InlineModelResolver().flatten(openAPI);

        Schema model = openAPI.getComponents().getSchemas().get("User");
        assertTrue(model instanceof ArraySchema);

        Schema user = openAPI.getComponents().getSchemas().get("User_inner");
        assertNotNull(user);
        assertEquals("description", user.getDescription());
    }




    @Test
    public void resolveInlineRequestBody() throws Exception {
        OpenAPI openAPI = new OpenAPI();


        ObjectSchema objectSchema = new ObjectSchema();
        objectSchema.addProperties("street", new StringSchema());

        Schema schema = new Schema();
        schema.addProperties("address", objectSchema);
        schema.addProperties("name", new StringSchema());

        MediaType mediaType = new MediaType();
        mediaType.setSchema(schema);

        Content content = new Content();
        content.addMediaType("*/*", mediaType );

        RequestBody requestBody = new RequestBody();
        requestBody.setContent(content);

        Operation operation = new Operation();
        operation.setRequestBody(requestBody);

        PathItem pathItem = new PathItem();
        pathItem.setGet(operation);
        openAPI.path("/hello",pathItem);

        new InlineModelResolver().flatten(openAPI);

        Operation getOperation = openAPI.getPaths().get("/hello").getGet();
        RequestBody body = getOperation.getRequestBody();
        assertTrue(body.getContent().get("*/*").getSchema().get$ref() != null);

        Schema bodySchema = openAPI.getComponents().getSchemas().get("hello_body");
        assertTrue(bodySchema instanceof Schema);

        assertNotNull(bodySchema.getProperties().get("address"));
    }
    
    @Test
    public void resolveInlineRequestBody_maxTwoPathParts() throws Exception {
        OpenAPI openAPI = new OpenAPI();

        ObjectSchema objectSchema = new ObjectSchema();
        objectSchema.addProperties("street", new StringSchema());

        Schema schema = new Schema();
        schema.addProperties("address", objectSchema);
        schema.addProperties("name", new StringSchema());

        MediaType mediaType = new MediaType();
        mediaType.setSchema(schema);

        Content content = new Content();
        content.addMediaType("*/*", mediaType );

        RequestBody requestBody = new RequestBody();
        requestBody.setContent(content);

        Operation operation = new Operation();
        operation.setRequestBody(requestBody);

        PathItem pathItem = new PathItem();
        pathItem.setGet(operation);
        openAPI.path("/api/cloud/greet/hello",pathItem);

        new InlineModelResolver().flatten(openAPI);

        Operation getOperation = openAPI.getPaths().get("/api/cloud/greet/hello").getGet();
        RequestBody body = getOperation.getRequestBody();
        assertTrue(body.getContent().get("*/*").getSchema().get$ref() != null);

        Schema bodySchema = openAPI.getComponents().getSchemas().get("greet_hello_body");
        assertTrue(bodySchema instanceof Schema);

        assertNotNull(bodySchema.getProperties().get("address"));
    }
    
    @Test
    public void resolveInlineRequestBody_stripsDotsFromPath() throws Exception {
        OpenAPI openAPI = new OpenAPI();

        ObjectSchema objectSchema = new ObjectSchema();
        objectSchema.addProperties("street", new StringSchema());

        Schema schema = new Schema();
        schema.addProperties("address", objectSchema);
        schema.addProperties("name", new StringSchema());

        MediaType mediaType = new MediaType();
        mediaType.setSchema(schema);

        Content content = new Content();
        content.addMediaType("*/*", mediaType );

        RequestBody requestBody = new RequestBody();
        requestBody.setContent(content);

        Operation operation = new Operation();
        operation.setRequestBody(requestBody);

        PathItem pathItem = new PathItem();
        pathItem.setGet(operation);
        openAPI.path("/api/Cloud.Greet.Hello",pathItem);

        new InlineModelResolver(true, true).flatten(openAPI);

        Operation getOperation = openAPI.getPaths().get("/api/Cloud.Greet.Hello").getGet();
        RequestBody body = getOperation.getRequestBody();
        assertEquals("use dot as common word separator: as it occurs frequently on OData services", 
            "#/components/schemas/ApiCloudGreetHelloBody", 
            body.getContent().get("*/*").getSchema().get$ref());

        Schema bodySchema = openAPI.getComponents().getSchemas().get("ApiCloudGreetHelloBody");
        assertTrue(bodySchema instanceof Schema);

        assertNotNull(bodySchema.getProperties().get("address"));
    }

    @Test
    public void resolveInlineParameter() throws Exception {
        OpenAPI openAPI = new OpenAPI();


        ObjectSchema objectSchema = new ObjectSchema();
        objectSchema.addProperties("street", new StringSchema());

        Schema schema = new Schema();
        schema.addProperties("address", objectSchema);
        schema.addProperties("name", new StringSchema());

        Parameter parameter = new Parameter();
        parameter.setName("name");
        parameter.setSchema(schema);

        List parameters = new ArrayList();
        parameters.add(parameter);


        Operation operation = new Operation();
        operation.setParameters(parameters);

        PathItem pathItem = new PathItem();
        pathItem.setGet(operation);
        openAPI.path("/hello",pathItem);

        new InlineModelResolver().flatten(openAPI);

        Operation getOperation = openAPI.getPaths().get("/hello").getGet();
        Parameter param = getOperation.getParameters().get(0);
        assertTrue(param.getSchema().get$ref() != null);



        Schema bodySchema = openAPI.getComponents().getSchemas().get("name");
        assertTrue(bodySchema instanceof Schema);

        assertNotNull(bodySchema.getProperties().get("address"));
    }

   @Test
    public void resolveInlineRequestBodyWithTitle() throws Exception {
        OpenAPI openAPI = new OpenAPI();

        ObjectSchema objectSchema = new ObjectSchema();
        objectSchema.addProperties("street", new StringSchema());
        objectSchema.addProperties("name", new StringSchema());
        Schema addressModelItem = new Schema();
        String addressModelName = "DetailedAddress";
        addressModelItem.setTitle(addressModelName);
        addressModelItem.addProperties("address", objectSchema);

        openAPI.path("/hello", new PathItem()
                .get(new Operation()
                        .requestBody(new RequestBody()
                                .content(new Content().addMediaType("*/*", new MediaType()
                                .schema(addressModelItem))))));

        new InlineModelResolver().flatten(openAPI);

        Operation operation = openAPI.getPaths().get("/hello").getGet();
        RequestBody requestBody = operation.getRequestBody();
        assertTrue(requestBody.getContent().get("*/*").getSchema().get$ref() != null);

        Schema body = openAPI.getComponents().getSchemas().get(addressModelName);
        assertTrue(body instanceof Schema);

        assertNotNull(body.getProperties().get("address"));
    }

    @Test
    public void notResolveNonModelRequestBody() throws Exception {
        OpenAPI openAPI = new OpenAPI();

        openAPI.path("/hello", new PathItem()
                .get(new Operation()
                        .requestBody(new RequestBody()
                                .content(new Content().addMediaType("*/*", new MediaType().schema(new Schema()
                                        .type("string")
                                        .format("binary")))))));

        new InlineModelResolver().flatten(openAPI);

        Operation operation = openAPI.getPaths().get("/hello").getGet();
        RequestBody body = operation.getRequestBody();
        assertTrue(body.getContent().get("*/*").getSchema() instanceof Schema);
        Schema schema = body.getContent().get("*/*").getSchema();
        assertEquals("string", schema.getType());
        assertEquals("binary", schema.getFormat());
    }

    @Test
    public void resolveInlineArrayRequestBody() throws Exception {
        OpenAPI openAPI = new OpenAPI();

        ObjectSchema addressSchema = new ObjectSchema();
        addressSchema.addProperties("street",new StringSchema());

        ObjectSchema objectSchema =new ObjectSchema();
        objectSchema.addProperties("address", addressSchema);

        ArraySchema arraySchema = new ArraySchema();
        arraySchema.items(objectSchema);


        openAPI.path("/hello", new PathItem()
                .get(new Operation()
                        .requestBody(new RequestBody()
                                .content(new Content().addMediaType("*/*",new MediaType()
                                .schema(arraySchema))))));

        new InlineModelResolver().flatten(openAPI);

        RequestBody body = openAPI.getPaths().get("/hello").getGet().getRequestBody();
        Schema schema = body.getContent().get("*/*").getSchema();

        assertTrue(schema instanceof ArraySchema);

        ArraySchema am = (ArraySchema) schema;
        Schema inner = am.getItems();
        assertTrue(inner.get$ref() != null);

        assertEquals( "#/components/schemas/hello_body",inner.get$ref());

        Schema inline = openAPI.getComponents().getSchemas().get("hello_body");
        assertNotNull(inline);
        assertTrue(inline instanceof Schema);

        Schema address = (Schema) inline.getProperties().get("address");
        assertNotNull(address);

        assertEquals( "#/components/schemas/hello_address",address.get$ref());


        Schema inlineProp = openAPI.getComponents().getSchemas().get("hello_address");
        assertNotNull(inlineProp);
        assertTrue(inlineProp instanceof Schema);

        assertNotNull(inlineProp.getProperties().get("street"));
        assertTrue(inlineProp.getProperties().get("street") instanceof StringSchema);
    }

    @Test
    public void resolveInlineArrayResponse() throws Exception {
        OpenAPI openAPI = new OpenAPI();

        ObjectSchema items = new ObjectSchema();
        items.addExtension("x-ext", "ext-items");
        items.addProperties("name", new StringSchema());


        ArraySchema schema = new ArraySchema()
                .items(items);
        schema.addExtension("x-ext", "ext-prop");

        ApiResponse response  = new ApiResponse();
        response.addExtension("x-foo", "bar");
        response.description("it works!");
        response.content(new Content().addMediaType("*/*", new MediaType().schema(schema)));

        openAPI.path("/foo/baz", new PathItem()
                .get(new Operation()
                        .responses(new ApiResponses().addApiResponse("200",response))));

        new InlineModelResolver().flatten(openAPI);

        ApiResponse apiResponse = openAPI.getPaths().get("/foo/baz").getGet().getResponses().get("200");
        assertNotNull(apiResponse);

        assertNotNull(apiResponse.getContent().get("*/*").getSchema());
        Schema responseProperty = apiResponse.getContent().get("*/*").getSchema();

        // no need to flatten more
        assertTrue(responseProperty instanceof ArraySchema);

        ArraySchema ap = (ArraySchema) responseProperty;
        assertEquals(1, ap.getExtensions().size());
        assertEquals("ext-prop", ap.getExtensions().get("x-ext"));

        Schema p = ap.getItems();

        assertNotNull(p);

        assertEquals("#/components/schemas/inline_response_200", p.get$ref());

        assertEquals(1, p.getExtensions().size());
        assertEquals("ext-items", p.getExtensions().get("x-ext"));

        Schema inline = openAPI.getComponents().getSchemas().get("inline_response_200");
        assertNotNull(inline);
        assertTrue(inline instanceof Schema);

        assertNotNull(inline.getProperties().get("name"));
        assertTrue(inline.getProperties().get("name") instanceof StringSchema);
    }

    @Test
    public void resolveInlineArrayResponseWithTitle() throws Exception {
        OpenAPI openAPI = new OpenAPI();

        ApiResponse apiResponse  = new ApiResponse();
        apiResponse.addExtension("x-foo", "bar");
        apiResponse.description("it works!");

        Map<String,Schema> properties = new HashMap<>();
        properties.put("name", new StringSchema());

        apiResponse.content(new Content().addMediaType("*/*", new MediaType().schema(new ArraySchema()
                        .items(new ObjectSchema()
                                .title("FooBar")
                                .properties(properties)))));

        openAPI.path("/foo/baz", new PathItem()
                .get(new Operation()
                        .responses(new ApiResponses().addApiResponse("200",apiResponse))));

        new InlineModelResolver().flatten(openAPI);

        ApiResponse response = openAPI.getPaths().get("/foo/baz").getGet().getResponses().get("200");
        assertNotNull(response);

        assertNotNull(response.getContent().get("*/*").getSchema());
        Schema responseProperty = response.getContent().get("*/*").getSchema();

        // no need to flatten more
        assertTrue(responseProperty instanceof ArraySchema);

        ArraySchema ap = (ArraySchema) responseProperty;
        Schema p = ap.getItems();

        assertNotNull(p);

        assertEquals(p.get$ref(), "#/components/schemas/"+ "FooBar");


        Schema inline = openAPI.getComponents().getSchemas().get("FooBar");
        assertNotNull(inline);
        assertTrue(inline instanceof Schema);
        assertNotNull(inline.getProperties().get("name"));
        assertTrue(inline.getProperties().get("name") instanceof StringSchema);
    }

    @Test
    public void testInlineMapResponse() throws Exception {
        OpenAPI openAPI = new OpenAPI();

        Schema schema = new Schema();
        schema.setAdditionalProperties(new StringSchema());
        schema.addExtension("x-ext", "ext-prop");

        ApiResponse apiResponse = new ApiResponse();
        apiResponse.description("it works!");
        MediaType mediaType = new MediaType();
        mediaType.setSchema(schema);

        Content content = new Content();
        content.addMediaType("*/*",mediaType);

        apiResponse.setContent(content);
        apiResponse.addExtension("x-foo", "bar");

        ApiResponses apiResponses = new ApiResponses();
        apiResponses.addApiResponse("200",apiResponse);


        openAPI.path("/foo/baz", new PathItem()
                .get(new Operation()
                        .responses(apiResponses)));


        new InlineModelResolver().flatten(openAPI);

        ApiResponse response = openAPI.getPaths().get("/foo/baz").getGet().getResponses().get("200");

        Schema property = response.getContent().get("*/*").getSchema();
        assertTrue(property.getAdditionalProperties() != null);
        assertTrue(openAPI.getComponents().getSchemas() == null);
        assertEquals(1, property.getExtensions().size());
        assertEquals("ext-prop", property.getExtensions().get("x-ext"));
    }

    @Test
    public void testInlineMapResponseWithObjectSchema() throws Exception {
        OpenAPI openAPI = new OpenAPI();

        Schema schema = new Schema();
        schema.setAdditionalProperties(new ObjectSchema()
                .addProperties("name", new StringSchema()));
        schema.addExtension("x-ext", "ext-prop");

        ApiResponse apiResponse = new ApiResponse()
                .description("it works!")
                .content(new Content().addMediaType("*/*",new MediaType().schema(schema)));
        apiResponse.addExtension("x-foo", "bar");

        ApiResponses apiResponses = new ApiResponses().addApiResponse("200",apiResponse);

        openAPI.path("/foo/baz", new PathItem()
                .get(new Operation()
                        .responses(apiResponses)));

        new InlineModelResolver().flatten(openAPI);

        ApiResponse response = openAPI.getPaths().get("/foo/baz").getGet().getResponses().get("200");
        Schema property = response.getContent().get("*/*").getSchema();
        assertTrue(property.getAdditionalProperties() != null);
        assertEquals(1, property.getExtensions().size());
        assertEquals("ext-prop", property.getExtensions().get("x-ext"));
        assertTrue(openAPI.getComponents().getSchemas().size() == 1);

        Schema inline = openAPI.getComponents().getSchemas().get("inline_response_map200");
        assertTrue(inline instanceof Schema);
        assertNotNull(inline.getProperties().get("name"));
        assertTrue(inline.getProperties().get("name") instanceof StringSchema);
    }

    @Test
    public void testArrayResponse() {
        OpenAPI openAPI = new OpenAPI();


        ObjectSchema objectSchema = new ObjectSchema();
        objectSchema.addProperties("name", new StringSchema());
        ArraySchema schema = new ArraySchema();
        schema.setItems(objectSchema);

        ApiResponse apiResponse = new ApiResponse();
        apiResponse.addExtension("x-foo", "bar");
        apiResponse.setDescription("it works!");
        apiResponse.setContent(new Content().addMediaType("*/*", new MediaType().schema(schema)));

        openAPI.path("/foo/baz", new PathItem()
                .get(new Operation()
                        .responses(new ApiResponses().addApiResponse("200", apiResponse))));

        new InlineModelResolver().flatten(openAPI);

        ApiResponse response = openAPI.getPaths().get("/foo/baz").getGet().getResponses().get("200");
        assertTrue(response.getContent().get("*/*").getSchema() instanceof ArraySchema);

        ArraySchema am = (ArraySchema) response.getContent().get("*/*").getSchema();
        Schema items = am.getItems();
        assertTrue(items.get$ref() != null);

        assertEquals(items.get$ref(), "#/components/schemas/inline_response_200");


        Schema inline = openAPI.getComponents().getSchemas().get("inline_response_200");
        assertTrue(inline instanceof Schema);

        assertNotNull(inline.getProperties().get("name"));
        assertTrue(inline.getProperties().get("name") instanceof StringSchema);
    }

    @Test
    public void testBasicInput() {
        OpenAPI openAPI = new OpenAPI();
        openAPI.setComponents(new Components());

        Schema user = new Schema();
        user.addProperties("name", new StringSchema());

        openAPI.path("/foo/baz", new PathItem()
                .post(new Operation()
                        .requestBody(new RequestBody()
                                .content(new Content().addMediaType("*/*",new MediaType().schema(new Schema().$ref("User")))))));

        openAPI.getComponents().addSchemas("User", user);

        new InlineModelResolver().flatten(openAPI);
    }

    @Test
    public void testArbitraryRequestBody() {
        OpenAPI openAPI = new OpenAPI();

        openAPI.path("/hello", new PathItem()
                .get(new Operation()
                        .requestBody(new RequestBody()
                                .content(new Content().addMediaType("*/*",new MediaType().schema(new Schema()))))));

        new InlineModelResolver().flatten(openAPI);

        Operation operation = openAPI.getPaths().get("/hello").getGet();
        RequestBody requestBody = operation.getRequestBody();
        assertTrue(requestBody.getContent().get("*/*").getSchema() instanceof Schema);
        Schema schema = requestBody.getContent().get("*/*").getSchema();
        assertNull(schema.getType());
    }

    @Test
    public void testArbitraryObjectRequestBodyInline() {
        OpenAPI swagger = new OpenAPI();

        Schema schema = new Schema();
        schema.addProperties("arbitrary", new ObjectSchema());

        swagger.path("/hello", new PathItem()
                .get(new Operation()
                        .requestBody(new RequestBody()
                                .content(new Content().addMediaType("*/*",new MediaType().schema(schema))))));

        new InlineModelResolver().flatten(swagger);

        Operation operation = swagger.getPaths().get("/hello").getGet();
        RequestBody requestBody = operation.getRequestBody();
        assertTrue(requestBody.getContent().get("*/*").getSchema().get$ref() != null);

        Schema body = swagger.getComponents().getSchemas().get("hello_body");
        assertTrue(body instanceof Schema);


        Schema property = (Schema) body.getProperties().get("arbitrary");
        assertNotNull(property);
        assertTrue(property instanceof ObjectSchema);
    }

    @Test
    public void testArbitraryObjectBodyParamWithArray() {
        OpenAPI openAPI = new OpenAPI();

        openAPI.path("/hello", new PathItem()
                .get(new Operation()
                        .requestBody(new RequestBody()
                                .content(new Content().addMediaType("*/*",new MediaType().schema(new ArraySchema()
                                        .items(new ObjectSchema())))))));

        new InlineModelResolver().flatten(openAPI);

        RequestBody requestBody = openAPI.getPaths().get("/hello").getGet().getRequestBody();


        Schema schema = requestBody.getContent().get("*/*").getSchema();

        assertTrue(schema instanceof ArraySchema);

        ArraySchema arraySchema = (ArraySchema) schema;
        Schema inner = arraySchema.getItems();
        assertTrue(inner instanceof ObjectSchema);

        ObjectSchema property = (ObjectSchema) inner;
        assertNotNull(property);
        assertNull(property.getProperties());
    }

    @Test
    public void testArbitraryObjectBodyParamArrayInline() {
        OpenAPI openAPI = new OpenAPI();

        ObjectSchema items = new ObjectSchema();
        items.addProperties("arbitrary", new ObjectSchema());

        openAPI.path("/hello", new PathItem()
                .get(new Operation()
                        .requestBody(new RequestBody()
                                .content(new Content().addMediaType("*/*",new MediaType().schema(new ArraySchema().items(items)))))));

        new InlineModelResolver().flatten(openAPI);

        RequestBody requestBody = openAPI.getPaths().get("/hello").getGet().getRequestBody();

        Schema schema = requestBody.getContent().get("*/*").getSchema();

        assertTrue(schema instanceof ArraySchema);

        ArraySchema arraySchema = (ArraySchema) schema;
        Schema inner = arraySchema.getItems();
        assertTrue(inner.get$ref() != null);


        assertEquals(inner.get$ref(), "#/components/schemas/hello_body");

        Schema inline = openAPI.getComponents().getSchemas().get("hello_body");
        assertNotNull(inline);

        Schema p = (Schema)inline.getProperties().get("arbitrary");
        assertNotNull(p);
        assertTrue(p instanceof ObjectSchema);
    }

    @Test
    public void testArbitraryObjectResponse() {
        OpenAPI openAPI = new OpenAPI();

        openAPI.path("/foo/bar", new PathItem()
                .get(new Operation()
                        .responses(new ApiResponses().addApiResponse("200", new ApiResponse()
                                .description("it works!")
                                .content(new Content().addMediaType("*/*", new MediaType().schema(new ObjectSchema())))))));

        new InlineModelResolver().flatten(openAPI);

        Map<String, ApiResponse> responses = openAPI.getPaths().get("/foo/bar").getGet().getResponses();

        ApiResponse response = responses.get("200");
        assertNotNull(response);
        assertTrue(response.getContent().get("*/*").getSchema() instanceof ObjectSchema);
        ObjectSchema op = (ObjectSchema) response.getContent().get("*/*").getSchema();
        assertNull(op.getProperties());
    }

    @Test
    public void testArbitraryObjectResponseArray() {
        OpenAPI openAPI = new OpenAPI();

        openAPI.path("/foo/baz", new PathItem()
                .get(new Operation()
                        .responses(new ApiResponses().addApiResponse("200", new ApiResponse()
                                .description("it works!")
                                .content(new Content().addMediaType("*/*", new MediaType().schema(new ArraySchema()
                                        .items(new ObjectSchema()))))))));

        new InlineModelResolver().flatten(openAPI);

        ApiResponse response = openAPI.getPaths().get("/foo/baz").getGet().getResponses().get("200");
        assertTrue(response.getContent().get("*/*").getSchema() instanceof ArraySchema);

        ArraySchema arraySchema = (ArraySchema) response.getContent().get("*/*").getSchema();
        Schema items = arraySchema.getItems();
        assertTrue(items instanceof ObjectSchema);
        ObjectSchema op = (ObjectSchema) items;
        assertNull(op.getProperties());
    }

    @Test
    public void testArbitraryObjectResponseArrayInline() {
        OpenAPI openAPI = new OpenAPI();

        ArraySchema arraySchema = new ArraySchema();
        ObjectSchema objectSchema = new ObjectSchema();
        objectSchema.addProperties("arbitrary", new ObjectSchema());
        arraySchema.items(objectSchema);


        ApiResponse apiResponse =  new ApiResponse();
        apiResponse.addExtension("x-foo", "bar");
        apiResponse.description("it works!");
        apiResponse.content(new Content().addMediaType("*/*", new MediaType().schema(arraySchema)));



        openAPI.path("/foo/baz", new PathItem()
                .get(new Operation()
                        .responses(new ApiResponses().addApiResponse("200",apiResponse))));

        new InlineModelResolver().flatten(openAPI);

        ApiResponse response = openAPI.getPaths().get("/foo/baz").getGet().getResponses().get("200");
        assertNotNull(response);

        assertNotNull(response.getContent().get("*/*").getSchema());
        Schema responseProperty = response.getContent().get("*/*").getSchema();
        assertTrue(responseProperty instanceof ArraySchema);

        ArraySchema arraySchema1 = (ArraySchema) responseProperty;
        Schema items = arraySchema1.getItems();
        assertNotNull(items);

        assertEquals( "#/components/schemas/inline_response_200",items.get$ref());

        Schema inline = openAPI.getComponents().getSchemas().get("inline_response_200");
        assertNotNull(inline);
        assertTrue(inline instanceof Schema);

        Schema inlineProp = (Schema) inline.getProperties().get("arbitrary");
        assertNotNull(inlineProp);
        assertTrue(inlineProp instanceof ObjectSchema);
        assertNull(inlineProp.getProperties());
    }

    @Test
    public void testArbitraryObjectResponseMapInline() {
        OpenAPI openAPI = new OpenAPI();

        Schema schema = new Schema();
        schema.setAdditionalProperties(new ObjectSchema());

        openAPI.path("/foo/baz", new PathItem()
                .get(new Operation()
                        .responses(new ApiResponses().addApiResponse("200", new ApiResponse()
                                .description("it works!")
                                .content(new Content().addMediaType("*/*", new MediaType().schema(schema)))))));

        new InlineModelResolver().flatten(openAPI);

        ApiResponse response = openAPI.getPaths().get("/foo/baz").getGet().getResponses().get("200");

        Schema property = response.getContent().get("*/*").getSchema();
        assertTrue(property.getAdditionalProperties() != null);
        assertTrue(property.getAdditionalProperties() instanceof  Schema);
        assertTrue(openAPI.getComponents().getSchemas() == null);
        Schema inlineProp = (Schema)property.getAdditionalProperties();
        assertTrue(inlineProp instanceof ObjectSchema);
        ObjectSchema op = (ObjectSchema) inlineProp;
        assertNull(op.getProperties());
    }

    @Test
    public void testArbitraryObjectModelInline() {
        OpenAPI openAPI = new OpenAPI();
        openAPI.setComponents(new Components());

        Schema userSchema = new Schema();
        userSchema.setName("user");
        userSchema.setDescription("a common user");
        userSchema.addProperties("name", new StringSchema());

        ObjectSchema objectSchema = new ObjectSchema();
        objectSchema.setTitle("title");
        objectSchema.setDefault("default");
        objectSchema.setReadOnly(false);
        objectSchema.setDescription("description");
        objectSchema.setName("name");

        userSchema.addProperties("arbitrary", objectSchema);
        List required = new ArrayList();
        required.add("arbitrary");
        userSchema.setRequired(required);


        openAPI.getComponents().addSchemas("User", userSchema);

        new InlineModelResolver().flatten(openAPI);

        Schema user = openAPI.getComponents().getSchemas().get("User");
        assertNotNull(user);
        Schema inlineProp = (Schema) user.getProperties().get("arbitrary");
        assertTrue(inlineProp instanceof ObjectSchema);
        assertNull(inlineProp.getProperties());
    }

    @Test
    public void testArbitraryObjectModelWithArrayInlineWithoutTitle() {
        OpenAPI openAPI = new OpenAPI();
        openAPI.setComponents(new Components());

        Schema items = new ObjectSchema();
        items.setDefault("default");
        items.setReadOnly(false);
        items.setDescription("description");
        items.setName("name");
        items.addProperties("arbitrary", new ObjectSchema());

        openAPI.getComponents().addSchemas("User", new ArraySchema().items(items).addRequiredItem("name"));

        new InlineModelResolver().flatten(openAPI);

        Schema model = openAPI.getComponents().getSchemas().get("User");
        assertTrue(model instanceof ArraySchema);
        ArraySchema am = (ArraySchema) model;
        Schema inner = am.getItems();
        assertTrue(inner.get$ref() != null);

        Schema userInner = openAPI.getComponents().getSchemas().get("User_inner");
        assertNotNull(userInner);
        Schema inlineProp = (Schema)userInner.getProperties().get("arbitrary");
        assertTrue(inlineProp instanceof ObjectSchema);
        ObjectSchema op = (ObjectSchema) inlineProp;
        assertNull(op.getProperties());
    }

    @Test
    public void testArbitraryObjectModelWithArrayInlineWithTitle() {
        OpenAPI openAPI = new OpenAPI();
        openAPI.setComponents(new Components());

        Schema items = new ObjectSchema();
        items.setTitle("InnerUserTitle");
        items.setDefault("default");
        items.setReadOnly(false);
        items.setDescription("description");
        items.setName("name");
        items.addProperties("arbitrary", new ObjectSchema());

        openAPI.getComponents().addSchemas("User", new ArraySchema().items(items).addRequiredItem("name"));

        new InlineModelResolver().flatten(openAPI);

        Schema model = openAPI.getComponents().getSchemas().get("User");
        assertTrue(model instanceof ArraySchema);
        ArraySchema am = (ArraySchema) model;
        Schema inner = am.getItems();
        assertTrue(inner.get$ref() != null);

        Schema userInner = openAPI.getComponents().getSchemas().get("InnerUserTitle");
        assertNotNull(userInner);
        Schema inlineProp = (Schema) userInner.getProperties().get("arbitrary");
        assertTrue(inlineProp instanceof ObjectSchema);
        ObjectSchema op = (ObjectSchema) inlineProp;
        assertNull(op.getProperties());
    }


    @Test(description = "https://github.com/swagger-api/swagger-parser/issues/1527")
    public void testInlineItemsSchema() throws Exception {
        ParseOptions options = new ParseOptions();
        options.setFlatten(true);
        OpenAPI openAPI = new OpenAPIV3Parser().read("flatten.json",null, options);

        assertNotNull(openAPI);
        assertNotNull(openAPI.getComponents().getSchemas().get("inline_response_200"));
    }

    @Test(description = "https://github.com/swagger-api/swagger-parser/issues/1200")
    public void testSchemaPropertiesBeingPassedToFlattenedModel() {
        OpenAPI openAPI = new OpenAPI();
        openAPI.setComponents(new Components());

        Schema address = new ObjectSchema();
        address.setDeprecated(false);
        address.setDescription("My address");
        address.setExclusiveMaximum(true);
        address.setExclusiveMinimum(true);
        address.setFormat("format");
        address.setMinLength(Integer.getInteger("10"));
        address.setMaximum(BigDecimal.valueOf(50));
        address.setMaxItems(Integer.getInteger("1"));
        address.setMaxLength(Integer.getInteger("100"));
        address.setMaxProperties(Integer.getInteger("1"));
        address.setMinimum(BigDecimal.ZERO);
        address.setMinItems(Integer.getInteger("0"));
        address.setMinLength(Integer.getInteger("10"));
        address.setMinProperties(Integer.getInteger("0"));
        address.setMultipleOf(BigDecimal.valueOf(2));
        address.setName("Address");
        address.setNullable(true);
        address.setPattern("%dd");
        address.setReadOnly(false);
        address.setTitle("my address");
        address.setUniqueItems(true);
        address.setWriteOnly(false);
        address.addProperties("city", new StringSchema());


        Schema user = new ObjectSchema();
        user.setTitle("InnerUserTitle");
        user.setDefault("default");
        user.setReadOnly(false);
        user.setDescription("user description");
        user.setName("user name");
        user.addProperties("address", address);

        openAPI.getComponents().addSchemas("User", user);

        new InlineModelResolver(true, true).flatten(openAPI);

        Schema model = openAPI.getComponents().getSchemas().get("User");
        assertTrue(model instanceof ObjectSchema);

        Schema userAddress = openAPI.getComponents().getSchemas().get("MyAddress");
        assertNotNull(userAddress);
        assertEquals(userAddress.getDeprecated(), Boolean.FALSE);
        assertEquals(userAddress.getDescription(), "My address");
        assertEquals(userAddress.getExclusiveMaximum(), Boolean.TRUE);
        assertEquals(userAddress.getExclusiveMinimum(), Boolean.TRUE);
        assertEquals(userAddress.getFormat(), "format");
        assertEquals(userAddress.getMaximum(), BigDecimal.valueOf(50));
        assertEquals(userAddress.getMaxItems(), Integer.getInteger("1"));
        assertEquals(userAddress.getMaxLength(), Integer.getInteger("100"));
        assertEquals(userAddress.getMaxProperties(), Integer.getInteger("1"));
        assertEquals(userAddress.getMinimum(), BigDecimal.ZERO);
        assertEquals(userAddress.getMinItems(), Integer.getInteger("1"));
        assertEquals(userAddress.getMinLength(), Integer.getInteger("100"));
        assertEquals(userAddress.getMinProperties(), Integer.getInteger("0"));
        assertEquals(userAddress.getMultipleOf(), BigDecimal.valueOf(2));
        assertEquals(userAddress.getName(), "Address");
        assertEquals(userAddress.getNullable(), Boolean.TRUE);
        assertEquals(userAddress.getPattern(), "%dd");
        assertEquals(userAddress.getReadOnly(), Boolean.FALSE);
        assertEquals(userAddress.getTitle(), "my address");
        assertEquals(userAddress.getUniqueItems(), Boolean.TRUE);
        assertEquals(userAddress.getWriteOnly(), Boolean.FALSE);

    }
}
