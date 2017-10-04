package io.swagger.parser.v3.util;

import io.swagger.oas.models.Components;
import io.swagger.oas.models.OpenAPI;
import io.swagger.oas.models.Operation;
import io.swagger.oas.models.PathItem;
import io.swagger.oas.models.media.ArraySchema;
import io.swagger.oas.models.media.Content;
import io.swagger.oas.models.media.MediaType;
import io.swagger.oas.models.media.ObjectSchema;
import io.swagger.oas.models.media.Schema;
import io.swagger.oas.models.media.StringSchema;
import io.swagger.oas.models.parameters.RequestBody;
import io.swagger.oas.models.responses.ApiResponse;
import io.swagger.oas.models.responses.ApiResponses;
import io.swagger.util.Json;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.testng.AssertJUnit.*;

@SuppressWarnings("static-method")
public class InlineModelResolverTest {
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

        Schema bodySchema = openAPI.getComponents().getSchemas().get("body");
        assertTrue(bodySchema instanceof Schema);

        assertNotNull(bodySchema.getProperties().get("address"));
    }

   @Test
    public void resolveInlineBodyParameterWithTitle() throws Exception {
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
        RequestBody bp = operation.getRequestBody();
        assertTrue(bp.getContent().get("*/*").getSchema().get$ref() != null);

        Schema body = openAPI.getComponents().getSchemas().get(addressModelName);
        assertTrue(body instanceof Schema);

        assertNotNull(body.getProperties().get("address"));
    }

    /*@Test
    public void notResolveNonModelBodyParameter() throws Exception {
        Swagger swagger = new Swagger();

        swagger.path("/hello", new Path()
                .get(new Operation()
                        .parameter(new BodyParameter()
                                .name("body")
                                .schema(new ModelImpl()
                                        .type("string")
                                        .format("binary")))));

        new InlineModelResolver().flatten(swagger);

        Operation operation = swagger.getPaths().get("/hello").getGet();
        BodyParameter bp = (BodyParameter)operation.getParameters().get(0);
        assertTrue(bp.getSchema() instanceof ModelImpl);
        ModelImpl m = (ModelImpl) bp.getSchema();
        assertEquals("string", m.getType());
        assertEquals("binary", m.getFormat());
    }

    @Test
    public void resolveInlineArrayBodyParameter() throws Exception {
        Swagger swagger = new Swagger();

        swagger.path("/hello", new Path()
                .get(new Operation()
                        .parameter(new BodyParameter()
                                .name("body")
                                .schema(new ArrayModel()
                                        .items(new ObjectProperty()
                                                .property("address", new ObjectProperty()
                                                        .property("street", new StringProperty())))))));

        new InlineModelResolver().flatten(swagger);

        Parameter param = swagger.getPaths().get("/hello").getGet().getParameters().get(0);
        assertTrue(param instanceof BodyParameter);

        BodyParameter bp = (BodyParameter) param;
        Model schema = bp.getSchema();

        assertTrue(schema instanceof ArrayModel);

        ArrayModel am = (ArrayModel) schema;
        Property inner = am.getItems();
        assertTrue(inner instanceof RefProperty);

        RefProperty rp = (RefProperty) inner;

        assertEquals(rp.getType(), "ref");
        assertEquals(rp.get$ref(), "#/definitions/body");
        assertEquals(rp.getSimpleRef(), "body");

        Model inline = swagger.getDefinitions().get("body");
        assertNotNull(inline);
        assertTrue(inline instanceof ModelImpl);
        ModelImpl impl = (ModelImpl) inline;
        RefProperty rpAddress = (RefProperty) impl.getProperties().get("address");
        assertNotNull(rpAddress);
        assertEquals(rpAddress.getType(), "ref");
        assertEquals(rpAddress.get$ref(), "#/definitions/hello_address");
        assertEquals(rpAddress.getSimpleRef(), "hello_address");

        Model inlineProp = swagger.getDefinitions().get("hello_address");
        assertNotNull(inlineProp);
        assertTrue(inlineProp instanceof ModelImpl);
        ModelImpl implProp = (ModelImpl) inlineProp;
        assertNotNull(implProp.getProperties().get("street"));
        assertTrue(implProp.getProperties().get("street") instanceof StringProperty);
    }

    @Test
    public void resolveInlineArrayResponse() throws Exception {
        Swagger swagger = new Swagger();

        ArrayProperty schema = new ArrayProperty()
                .items(new ObjectProperty()
                        .property("name", new StringProperty())
                        .vendorExtension("x-ext", "ext-items"))
                .vendorExtension("x-ext", "ext-prop");
        swagger.path("/foo/baz", new Path()
                .get(new Operation()
                        .response(200, new Response()
                                .vendorExtension("x-foo", "bar")
                                .description("it works!")
                                .schema(schema))));

        new InlineModelResolver().flatten(swagger);

        Response response = swagger.getPaths().get("/foo/baz").getGet().getResponses().get("200");
        assertNotNull(response);

        assertNotNull(response.getSchema());
        Property responseProperty = response.getSchema();

        // no need to flatten more
        assertTrue(responseProperty instanceof ArrayProperty);

        ArrayProperty ap = (ArrayProperty) responseProperty;
        assertEquals(1, ap.getVendorExtensions().size());
        assertEquals("ext-prop", ap.getVendorExtensions().get("x-ext"));

        Property p = ap.getItems();

        assertNotNull(p);

        RefProperty rp = (RefProperty) p;
        assertEquals(rp.getType(), "ref");
        assertEquals(rp.get$ref(), "#/definitions/inline_response_200");
        assertEquals(rp.getSimpleRef(), "inline_response_200");
        assertEquals(1, rp.getVendorExtensions().size());
        assertEquals("ext-items", rp.getVendorExtensions().get("x-ext"));

        Model inline = swagger.getDefinitions().get("inline_response_200");
        assertNotNull(inline);
        assertTrue(inline instanceof ModelImpl);
        ModelImpl impl = (ModelImpl) inline;
        assertNotNull(impl.getProperties().get("name"));
        assertTrue(impl.getProperties().get("name") instanceof StringProperty);
    }

    @Test
    public void resolveInlineArrayResponseWithTitle() throws Exception {
        Swagger swagger = new Swagger();

        swagger.path("/foo/baz", new Path()
                .get(new Operation()
                        .response(200, new Response()
                                .vendorExtension("x-foo", "bar")
                                .description("it works!")
                                .schema(new ArrayProperty()
                                        .items(new ObjectProperty()
                                                .title("FooBar")
                                                .property("name", new StringProperty()))))));

        new InlineModelResolver().flatten(swagger);

        Response response = swagger.getPaths().get("/foo/baz").getGet().getResponses().get("200");
        assertNotNull(response);

        assertNotNull(response.getSchema());
        Property responseProperty = response.getSchema();

        // no need to flatten more
        assertTrue(responseProperty instanceof ArrayProperty);

        ArrayProperty ap = (ArrayProperty) responseProperty;
        Property p = ap.getItems();

        assertNotNull(p);

        RefProperty rp = (RefProperty) p;
        assertEquals(rp.getType(), "ref");
        assertEquals(rp.get$ref(), "#/definitions/"+ "FooBar");
        assertEquals(rp.getSimpleRef(), "FooBar");

        Model inline = swagger.getDefinitions().get("FooBar");
        assertNotNull(inline);
        assertTrue(inline instanceof ModelImpl);
        ModelImpl impl = (ModelImpl) inline;
        assertNotNull(impl.getProperties().get("name"));
        assertTrue(impl.getProperties().get("name") instanceof StringProperty);
    }

    @Test
    public void testInlineMapResponse() throws Exception {
        Swagger swagger = new Swagger();

        MapProperty schema = new MapProperty();
        schema.setAdditionalProperties(new StringProperty());
        schema.setVendorExtension("x-ext", "ext-prop");

        swagger.path("/foo/baz", new Path()
                .get(new Operation()
                        .response(200, new Response()
                                .vendorExtension("x-foo", "bar")
                                .description("it works!")
                                .schema(schema))));
        new InlineModelResolver().flatten(swagger);
        Json.prettyPrint(swagger);

        Response response = swagger.getPaths().get("/foo/baz").getGet().getResponses().get("200");

        Property property = response.getSchema();
        assertTrue(property instanceof MapProperty);
        assertTrue(swagger.getDefinitions().size() == 0);
        assertEquals(1, property.getVendorExtensions().size());
        assertEquals("ext-prop", property.getVendorExtensions().get("x-ext"));
    }

    @Test
    public void testInlineMapResponseWithObjectProperty() throws Exception {
        Swagger swagger = new Swagger();

        MapProperty schema = new MapProperty();
        schema.setAdditionalProperties(new ObjectProperty()
                .property("name", new StringProperty()));
        schema.setVendorExtension("x-ext", "ext-prop");

        swagger.path("/foo/baz", new Path()
                .get(new Operation()
                        .response(200, new Response()
                                .vendorExtension("x-foo", "bar")
                                .description("it works!")
                                .schema(schema))));
        new InlineModelResolver().flatten(swagger);

        Response response = swagger.getPaths().get("/foo/baz").getGet().getResponses().get("200");
        Property property = response.getSchema();
        assertTrue(property instanceof MapProperty);
        assertEquals(1, property.getVendorExtensions().size());
        assertEquals("ext-prop", property.getVendorExtensions().get("x-ext"));
        assertTrue(swagger.getDefinitions().size() == 1);

        Model inline = swagger.getDefinitions().get("inline_response_200");
        assertTrue(inline instanceof ModelImpl);
        ModelImpl impl = (ModelImpl) inline;
        assertNotNull(impl.getProperties().get("name"));
        assertTrue(impl.getProperties().get("name") instanceof StringProperty);
    }

    @Test
    public void testArrayResponse() {
        Swagger swagger = new Swagger();

        ArrayProperty schema = new ArrayProperty();
        schema.setItems(new ObjectProperty()
                .property("name", new StringProperty()));

        swagger.path("/foo/baz", new Path()
                .get(new Operation()
                        .response(200, new Response()
                                .vendorExtension("x-foo", "bar")
                                .description("it works!")
                                .schema(schema))));
        new InlineModelResolver().flatten(swagger);

        Response response = swagger.getPaths().get("/foo/baz").getGet().getResponses().get("200");
        assertTrue(response.getSchema() instanceof ArrayProperty);

        ArrayProperty am = (ArrayProperty) response.getSchema();
        Property items = am.getItems();
        assertTrue(items instanceof RefProperty);
        RefProperty rp = (RefProperty) items;
        assertEquals(rp.getType(), "ref");
        assertEquals(rp.get$ref(), "#/definitions/inline_response_200");
        assertEquals(rp.getSimpleRef(), "inline_response_200");

        Model inline = swagger.getDefinitions().get("inline_response_200");
        assertTrue(inline instanceof ModelImpl);
        ModelImpl impl = (ModelImpl) inline;
        assertNotNull(impl.getProperties().get("name"));
        assertTrue(impl.getProperties().get("name") instanceof StringProperty);
    }

    @Test
    public void testBasicInput() {
        Swagger swagger = new Swagger();

        ModelImpl user = new ModelImpl()
                .property("name", new StringProperty());

        swagger.path("/foo/baz", new Path()
                .post(new Operation()
                        .parameter(new BodyParameter()
                                .name("myBody")
                                .schema(new RefModel("User")))));

        swagger.addDefinition("User", user);

        new InlineModelResolver().flatten(swagger);

        Json.prettyPrint(swagger);
    }

    @Test
    public void testArbitraryObjectBodyParam() {
        Swagger swagger = new Swagger();

        swagger.path("/hello", new Path()
                .get(new Operation()
                        .parameter(new BodyParameter()
                                .name("body")
                                .schema(new ModelImpl()))));

        new InlineModelResolver().flatten(swagger);

        Operation operation = swagger.getPaths().get("/hello").getGet();
        BodyParameter bp = (BodyParameter)operation.getParameters().get(0);
        assertTrue(bp.getSchema() instanceof ModelImpl);
        ModelImpl m = (ModelImpl) bp.getSchema();
        assertNull(m.getType());
    }

    @Test
    public void testArbitraryObjectBodyParamInline() {
        Swagger swagger = new Swagger();

        swagger.path("/hello", new Path()
                .get(new Operation()
                        .parameter(new BodyParameter()
                                .name("body")
                                .schema(new ModelImpl()
                                        .property("arbitrary", new ObjectProperty())))));

        new InlineModelResolver().flatten(swagger);

        Operation operation = swagger.getPaths().get("/hello").getGet();
        BodyParameter bp = (BodyParameter)operation.getParameters().get(0);
        assertTrue(bp.getSchema() instanceof RefModel);

        Model body = swagger.getDefinitions().get("body");
        assertTrue(body instanceof ModelImpl);

        ModelImpl impl = (ModelImpl) body;
        Property p = impl.getProperties().get("arbitrary");
        assertNotNull(p);
        assertTrue(p instanceof ObjectProperty);
    }

    @Test
    public void testArbitraryObjectBodyParamWithArray() {
        Swagger swagger = new Swagger();

        swagger.path("/hello", new Path()
                .get(new Operation()
                        .parameter(new BodyParameter()
                                .name("body")
                                .schema(new ArrayModel()
                                        .items(new ObjectProperty())))));

        new InlineModelResolver().flatten(swagger);

        Parameter param = swagger.getPaths().get("/hello").getGet().getParameters().get(0);
        assertTrue(param instanceof BodyParameter);

        BodyParameter bp = (BodyParameter) param;
        Model schema = bp.getSchema();

        assertTrue(schema instanceof ArrayModel);

        ArrayModel am = (ArrayModel) schema;
        Property inner = am.getItems();
        assertTrue(inner instanceof ObjectProperty);

        ObjectProperty op = (ObjectProperty) inner;
        assertNotNull(op);
        assertNull(op.getProperties());
    }

    @Test
    public void testArbitraryObjectBodyParamArrayInline() {
        Swagger swagger = new Swagger();

        swagger.path("/hello", new Path()
                .get(new Operation()
                        .parameter(new BodyParameter()
                                .name("body")
                                .schema(new ArrayModel()
                                        .items(new ObjectProperty()
                                                .property("arbitrary", new ObjectProperty()))))));

        new InlineModelResolver().flatten(swagger);

        Parameter param = swagger.getPaths().get("/hello").getGet().getParameters().get(0);
        assertTrue(param instanceof BodyParameter);

        BodyParameter bp = (BodyParameter) param;
        Model schema = bp.getSchema();

        assertTrue(schema instanceof ArrayModel);

        ArrayModel am = (ArrayModel) schema;
        Property inner = am.getItems();
        assertTrue(inner instanceof RefProperty);

        RefProperty rp = (RefProperty) inner;

        assertEquals(rp.getType(), "ref");
        assertEquals(rp.get$ref(), "#/definitions/body");
        assertEquals(rp.getSimpleRef(), "body");

        Model inline = swagger.getDefinitions().get("body");
        assertNotNull(inline);
        assertTrue(inline instanceof ModelImpl);
        ModelImpl impl = (ModelImpl) inline;
        Property p = impl.getProperties().get("arbitrary");
        assertNotNull(p);
        assertTrue(p instanceof ObjectProperty);
    }

    @Test
    public void testArbitraryObjectResponse() {
        Swagger swagger = new Swagger();

        swagger.path("/foo/bar", new Path()
                .get(new Operation()
                        .response(200, new Response()
                                .description("it works!")
                                .schema(new ObjectProperty()))));
        new InlineModelResolver().flatten(swagger);

        Map<String, Response> responses = swagger.getPaths().get("/foo/bar").getGet().getResponses();

        Response response = responses.get("200");
        assertNotNull(response);
        assertTrue(response.getSchema() instanceof ObjectProperty);
        ObjectProperty op = (ObjectProperty) response.getSchema();
        assertNull(op.getProperties());
    }

    @Test
    public void testArbitraryObjectResponseArray() {
        Swagger swagger = new Swagger();

        swagger.path("/foo/baz", new Path()
                .get(new Operation()
                        .response(200, new Response()
                                .description("it works!")
                                .schema(new ArrayProperty()
                                        .items(new ObjectProperty())))));
        new InlineModelResolver().flatten(swagger);

        Response response = swagger.getPaths().get("/foo/baz").getGet().getResponses().get("200");
        assertTrue(response.getSchema() instanceof ArrayProperty);

        ArrayProperty am = (ArrayProperty) response.getSchema();
        Property items = am.getItems();
        assertTrue(items instanceof ObjectProperty);
        ObjectProperty op = (ObjectProperty) items;
        assertNull(op.getProperties());
    }

    @Test
    public void testArbitraryObjectResponseArrayInline() {
        Swagger swagger = new Swagger();

        swagger.path("/foo/baz", new Path()
                .get(new Operation()
                        .response(200, new Response()
                                .vendorExtension("x-foo", "bar")
                                .description("it works!")
                                .schema(new ArrayProperty()
                                        .items(new ObjectProperty()
                                                .property("arbitrary", new ObjectProperty()))))));

        new InlineModelResolver().flatten(swagger);

        Response response = swagger.getPaths().get("/foo/baz").getGet().getResponses().get("200");
        assertNotNull(response);

        assertNotNull(response.getSchema());
        Property responseProperty = response.getSchema();
        assertTrue(responseProperty instanceof ArrayProperty);

        ArrayProperty ap = (ArrayProperty) responseProperty;
        Property p = ap.getItems();
        assertNotNull(p);

        RefProperty rp = (RefProperty) p;
        assertEquals(rp.getType(), "ref");
        assertEquals(rp.get$ref(), "#/definitions/inline_response_200");
        assertEquals(rp.getSimpleRef(), "inline_response_200");

        Model inline = swagger.getDefinitions().get("inline_response_200");
        assertNotNull(inline);
        assertTrue(inline instanceof ModelImpl);
        ModelImpl impl = (ModelImpl) inline;
        Property inlineProp = impl.getProperties().get("arbitrary");
        assertNotNull(inlineProp);
        assertTrue(inlineProp instanceof ObjectProperty);
        ObjectProperty op = (ObjectProperty) inlineProp;
        assertNull(op.getProperties());
    }

    @Test
    public void testArbitraryObjectResponseMapInline() {
        Swagger swagger = new Swagger();

        MapProperty schema = new MapProperty();
        schema.setAdditionalProperties(new ObjectProperty());

        swagger.path("/foo/baz", new Path()
                .get(new Operation()
                        .response(200, new Response()
                                .description("it works!")
                                .schema(schema))));
        new InlineModelResolver().flatten(swagger);

        Response response = swagger.getPaths().get("/foo/baz").getGet().getResponses().get("200");

        Property property = response.getSchema();
        assertTrue(property instanceof MapProperty);
        assertTrue(swagger.getDefinitions().size() == 0);
        Property inlineProp = ((MapProperty) property).getAdditionalProperties();
        assertTrue(inlineProp instanceof ObjectProperty);
        ObjectProperty op = (ObjectProperty) inlineProp;
        assertNull(op.getProperties());
    }

    @Test
    public void testArbitraryObjectModelInline() {
        Swagger swagger = new Swagger();

        swagger.addDefinition("User", new ModelImpl()
                .name("user")
                .description("a common user")
                .property("name", new StringProperty())
                .property("arbitrary", new ObjectProperty()
                        .title("title")
                        ._default("default")
                        .access("access")
                        .readOnly(false)
                        .required(true)
                        .description("description")
                        .name("name")));

        new InlineModelResolver().flatten(swagger);

        ModelImpl user = (ModelImpl)swagger.getDefinitions().get("User");
        assertNotNull(user);
        Property inlineProp = user.getProperties().get("arbitrary");
        assertTrue(inlineProp instanceof ObjectProperty);
        ObjectProperty op = (ObjectProperty) inlineProp;
        assertNull(op.getProperties());
    }

    @Test
    public void testArbitraryObjectModelWithArrayInlineWithoutTitle() {
        Swagger swagger = new Swagger();

        swagger.addDefinition("User", new ArrayModel()
                .items(new ObjectProperty()
                        ._default("default")
                        .access("access")
                        .readOnly(false)
                        .required(true)
                        .description("description")
                        .name("name")
                        .property("arbitrary", new ObjectProperty())));

        new InlineModelResolver().flatten(swagger);

        Model model = swagger.getDefinitions().get("User");
        assertTrue(model instanceof ArrayModel);
        ArrayModel am = (ArrayModel) model;
        Property inner = am.getItems();
        assertTrue(inner instanceof RefProperty);

        ModelImpl userInner = (ModelImpl)swagger.getDefinitions().get("User_inner");
        assertNotNull(userInner);
        Property inlineProp = userInner.getProperties().get("arbitrary");
        assertTrue(inlineProp instanceof ObjectProperty);
        ObjectProperty op = (ObjectProperty) inlineProp;
        assertNull(op.getProperties());
    }

    @Test
    public void testArbitraryObjectModelWithArrayInlineWithTitle() {
        Swagger swagger = new Swagger();

        swagger.addDefinition("User", new ArrayModel()
                .items(new ObjectProperty()
                        .title("InnerUserTitle")
                        ._default("default")
                        .access("access")
                        .readOnly(false)
                        .required(true)
                        .description("description")
                        .name("name")
                        .property("arbitrary", new ObjectProperty())));

        new InlineModelResolver().flatten(swagger);

        Model model = swagger.getDefinitions().get("User");
        assertTrue(model instanceof ArrayModel);
        ArrayModel am = (ArrayModel) model;
        Property inner = am.getItems();
        assertTrue(inner instanceof RefProperty);

        ModelImpl userInner = (ModelImpl)swagger.getDefinitions().get("InnerUserTitle");
        assertNotNull(userInner);
        Property inlineProp = userInner.getProperties().get("arbitrary");
        assertTrue(inlineProp instanceof ObjectProperty);
        ObjectProperty op = (ObjectProperty) inlineProp;
        assertNull(op.getProperties());
    }*/
}