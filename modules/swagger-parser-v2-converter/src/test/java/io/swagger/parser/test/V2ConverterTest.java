package io.swagger.parser.test;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import io.swagger.v3.parser.converter.SwaggerConverter;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.testng.annotations.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

public class V2ConverterTest {
    private static final String PET_STORE_JSON = "petstore.json";
    private static final String PET_STORE_YAML = "petstore.yaml";
    private static final String PARAMETER_CONVERSION_JSON = "parameter-conversion.json";
    private static final String ISSUE_2_JSON = "issue-2.json";
    private static final String ISSUE_3_JSON = "issue-3.json";
    private static final String ISSUE_4_JSON = "issue-4.json";
    private static final String ISSUE_6_JSON = "issue-6.json";
    private static final String ISSUE_8_JSON = "issue-8.json";
    private static final String ISSUE_11_JSON = "issue-11.json";
    private static final String ISSUE_13_JSON = "issue-13.json";
    private static final String ISSUE_14_JSON = "issue-14.json";
    private static final String ISSUE_15_JSON = "issue-15.json";
    private static final String ISSUE_16_JSON = "issue-16.json";
    private static final String ISSUE_17_JSON = "issue-17.json";
    private static final String ISSUE_18_JSON = "issue-18.json";
    private static final String ISSUE_19_JSON = "issue-19.json";
    private static final String ISSUE_20_JSON = "issue-20.json";
    private static final String ISSUE_21_JSON = "issue-21.json";
    private static final String ISSUE_22_JSON = "issue-22.json";
    private static final String ISSUE_23_JSON = "issue-23.json";
    private static final String ISSUE_25_JSON = "issue-25.json";
    private static final String ISSUE_26_JSON = "issue-26.json";
    private static final String ISSUE_27_JSON = "issue-27.json";
    private static final String ISSUE_28_JSON = "issue-28.json";
    private static final String ISSUE_30_JSON = "issue-30.json";
    private static final String ISSUE_31_JSON = "issue-31.json";
    private static final String ISSUE_32_JSON = "issue-32.json";
    private static final String ISSUE_33_JSON = "issue-33.json";
    private static final String ISSUE_35_JSON = "issue-35.json";
    private static final String ISSUE_36_JSON = "issue-36.json";
    private static final String ISSUE_597_JSON = "issue-597.json";
    private static final String ISSUE_599_JSON = "issue-599.json";
    private static final String ISSUE_600_JSON = "issue-600.json";
    private static final String ISSUE_455_JSON = "issue-455.json";
    private static final String ISSUE_540_JSON = "issue-540.json";
    private static final String ISSUE_647_JSON = "issue-647.yaml";
    private static final String ISSUE_662_JSON = "issue-662.yaml";
    private static final String ISSUE_672_JSON = "issue-672.json";
    private static final String ISSUE_673_YAML = "issue-673.yaml";
    private static final String ISSUE_676_JSON = "issue-676.json";
    private static final String ISSUE_708_YAML = "issue-708.yaml";
    private static final String ISSUE_755_YAML = "issue-755.yaml";

    private static final String API_BATCH_PATH = "/api/batch/";
    private static final String PETS_PATH = "/pets";
    private static final String PET_FIND_BY_STATUS_PATH = "/pet/findByStatus";
    private static final String PET_PATH = "/pet";
    private static final String FILE_PATH = "/file";
    private static final String POST_PATH = "/post";
    private static final String LOGIN_PATH = "/login";
    private static final String USERS_PATH = "/users";
    private static final String APPLICATION_YAML = "application/yaml";
    private static final String APPLICATION_JSON = "application/json";
    private static final String PASSWORD_VALUE = "p@55w0rd";
    private static final String PETSTORE_URL = "http://petstore.swagger.io/api";
    private static final String VALUE = "value";
    private static final String APPLICATION_PDF = "application/pdf";
    private static final String BINARY_FORMAT = "binary";
    private static final String ARRAY_TYPE = "array";
    private static final String PET_SCHEMA = "Pet";
    private static final String PET_TAG = "pet";
    private static final String RESPONSE_200OK_COMPONENT = "200OK";
    private static final String DEFAULT_RESPONSE_COMPONENT = "DefaultResponse";
    private static final String SECURITY_SCHEMA_OAUTH2 = "OAuth2Implicit";
    private static final String AUTHORIZATION_URL = "http://swagger.io/api/oauth/dialog";
    private static final String SCOPE_WRITE_PETS = "write:pets";
    private static final String SCOPE_READ_PETS = "read:pets";
    private static final String WRITE_PETS_VALUE = "modify pets in your account";
    private static final String READ_PETS_VALUE = "read your pets";
    private static final String EXTENDED_ERROR_MODEL = "ExtendedErrorModel";
    private static final String ERROR_MODEL_REF = "#/components/schemas/ErrorModel";
    private static final String MAP_OBJECTS_MODEL = "MapOfObjects";
    private static final String OBJECT = "object";
    private static final String OBJECT_REF = "#/components/schemas/Object";
    private static final String USER_LOGIN_PATH = "/user/login";
    private static final String X_RATE_LIMIT = "X-Rate-Limit";
    private static final String X_EXPIRES_AFTER = "X-Expires-After";
    private static final String X_RATE_LIMIT_DESCRIPTION = "calls per hour allowed by the user";
    private static final String X_EXPIRES_AFTER_DESCRIPTION = "date in UTC when token expires";
    private static final String X_EXAMPLE = "x-example";
    private static final String CONTENT_TYPE = "application/x-www-form-urlencoded";
    private static final String URLENCODED_CONTENT = "application/x-www-form-urlencoded";
    private static final String PATTERN = "^[a-zA-Z0-9]+$";
    private static final String FOO_PATH = "/{foo}";
    private static final String FOO_VALUE = "foooooo";
    private static final String CODE_EXAMPLE = "2866bbb7-ba38-4da3-b6b6-25d1ec6c161f";
    private static final String EMAIL = "email";
    private static final String PASSWORD = "password";
    private static final String EMAIL_VALUE = "bob@example.com";
    private static final String MONDAY_TUESDAY_VALUE = "monday_tuesday";
    private static final String TUESDAY_WEDNESDAY_VALUE = "tuesday_wednesday";
    private static final String WEDNESDAY_THURSDAY_VALUE = "wednesday_thursday";
    private static final String ARTHUR_DENT_NAME = "Arthur Dent";
    private static final String NAME = "name";
    private static final String USER_MODEL = "User";
    private static final String ID = "id";
    private static final String FRIEND_IDS = "friend_ids";
    private static final String ARRAY_OF_USERS_MODEL = "ArrayOfUsers";
    private static final String REQUEST_BODY_FORMEMAIL = "#/components/requestBodies/formEmail";
    private static final String HEAD_OPERATION = "Head Operation";
    private static final String OPTIONS_OPERATION = "Options Operation";
    private static final String AVAILABLE = "available";
    private static final String PENDING = "pending";
    private static final String SOLD = "sold";
    private static final String ARRAY_VALUES = "[{\"id\":-1,\"name\":\"Marvin the Paranoid Android\"}," +
            "{\"id\":1000000,\"name\":\"Zaphod Beeblebrox\",\"friends\":[15]}]";
    private static final String SCHEMAS_A_REF = "#/components/schemas/A";

    private static final int MAX_LENGTH = 60;
    private static final int REQUIRED_SIZE = 2;
    private static final int MIN_ITEMS = 1;
    private static final int PARAMETERS_SIZE = 1;
    private static final int PROPERTIES_SIZE = 4;
    private static final int ENUM_SIZE = 3;
    private static final int MAXIMUM = 100;
    private static final int MIN_LENGTH = 3;
    private static final int NUMBER_VALUE_TWENTY = 20;
    private static final double MULTIPLE_OF_VALUE = 0.01D;
    private static final long DEFAULT_VALUE = 11L;
    private static final int EXAMPLE_8_NUMBER = 8;
    private static final int EXAMPLE_42_NUMBER = 42;

    @Test
    public void testConvertPetstore() throws Exception {
        SwaggerConverter converter = new SwaggerConverter();
        String swaggerAsString = new String(Files.readAllBytes(Paths.get(getClass().getClassLoader()
                .getResource(PET_STORE_YAML).toURI())));
        SwaggerParseResult result = converter.readContents(swaggerAsString, null, null);

        assertNotNull(result.getOpenAPI());
    }

    @Test
    public void testIssue455() throws Exception {
        OpenAPI oas = getConvertedOpenAPIFromJsonFile(ISSUE_455_JSON);

        assertNotNull(oas);
        assertEquals(PARAMETERS_SIZE, oas.getPaths().size());

        PathItem pathItem = oas.getPaths().get(API_BATCH_PATH);
        assertNotNull(pathItem);

        assertEquals(PARAMETERS_SIZE, pathItem.getGet().getParameters().size());
    }

    @Test(description = "Missing array item type in parameters")
    public void testIssue1() throws Exception {
        OpenAPI oas = getConvertedOpenAPIFromJsonFile(PET_STORE_JSON);
        Parameter statusParameter = oas.getPaths().get(PET_FIND_BY_STATUS_PATH).getGet().getParameters().get(0);
        assertNotNull(statusParameter);
        assertTrue(statusParameter.getSchema() instanceof ArraySchema);
        ArraySchema arraySchema = (ArraySchema) statusParameter.getSchema();
        assertEquals(ARRAY_TYPE, arraySchema.getType());
        assertEquals(ENUM_SIZE, arraySchema.getItems().getEnum().size());
    }

    @Test(description = "Response Code")
    public void testIssue2() throws Exception {
        OpenAPI oas = getConvertedOpenAPIFromJsonFile(ISSUE_2_JSON);
        assertEquals(REQUIRED_SIZE, oas.getPaths().get(API_BATCH_PATH).getGet().getResponses().size());

    }

    @Test(description = "Servers when Hosts, basePath, scheme")
    public void testIssue3() throws Exception {
        OpenAPI oas = getConvertedOpenAPIFromJsonFile(ISSUE_3_JSON);
        assertNotNull(oas.getServers());
        assertEquals(PETSTORE_URL, oas.getServers().get(0).getUrl());
    }

    @Test(description = "Contents in Responses")
    public void testIssue4() throws Exception {
        OpenAPI oas = getConvertedOpenAPIFromJsonFile(ISSUE_4_JSON);
        assertNotNull(oas.getPaths().get(PETS_PATH).getGet().getResponses().get("200").getContent());
    }

    @Test(description = "Tags are missing in the converted spec")
    public void testIssue5() throws Exception {
        OpenAPI oas = getConvertedOpenAPIFromJsonFile(PET_STORE_JSON);
        //Global Tags
        List<Tag> tags = oas.getTags();
        assertNotNull(tags);
        assertEquals(PET_TAG, tags.get(0).getName());
        //Operation Tag
        Operation petPut = oas.getPaths().get(PET_PATH).getPut();
        assertNotNull(petPut.getTags());
        assertEquals(PET_TAG, petPut.getTags().get(0));
    }

    @Test(description = "Default value in parameters")
    public void testIssue6() throws Exception {
        OpenAPI oas = getConvertedOpenAPIFromJsonFile(ISSUE_6_JSON);
        assertEquals(DEFAULT_VALUE, oas.getPaths().get(PETS_PATH).getGet().
                getParameters().get(0).getSchema().getDefault());
    }

    @Test(description = "Body Converted to RequestBody")
    public void testIssue8() throws Exception {
        OpenAPI oas = getConvertedOpenAPIFromJsonFile(ISSUE_8_JSON);
        assertNotNull(oas.getPaths().get(PETS_PATH).getGet().getRequestBody());
    }

    @Test(description = "Response Headers")
    public void testIssue10() throws Exception {
        OpenAPI oas = getConvertedOpenAPIFromJsonFile(PET_STORE_JSON);
        Map<String, Header> headers = oas.getPaths().get(USER_LOGIN_PATH).getGet().getResponses().
                get("200").getHeaders();
        assertEquals(X_RATE_LIMIT_DESCRIPTION, headers.get(X_RATE_LIMIT).getDescription());
        assertEquals(X_EXPIRES_AFTER_DESCRIPTION, headers.get(X_EXPIRES_AFTER).getDescription());
    }

    @Test(description = "Minimal Spec error")
    public void testIssue11() throws Exception {
        OpenAPI oas = getConvertedOpenAPIFromJsonFile(ISSUE_11_JSON);
        assertNotNull(oas);
    }

    @Test(description = "Extensions in External Docs")
    public void testIssue12() throws Exception {
        OpenAPI oas = getConvertedOpenAPIFromJsonFile(PET_STORE_JSON);
        for (Tag tag : oas.getTags()) {
            if (tag.getExternalDocs() != null) {
                assertNull(tag.getExternalDocs().getExtensions());
            }
        }
    }

    @Test(description = "Extensions in External Docs")
    public void testIssue13() throws Exception {
        OpenAPI oas = getConvertedOpenAPIFromJsonFile(ISSUE_13_JSON);
        assertNotNull(oas.getExternalDocs());
    }

    @Test(description = "X-example in parameters")
    public void testIssue14() throws Exception {
        OpenAPI oas = getConvertedOpenAPIFromJsonFile(ISSUE_14_JSON);
        assertEquals(VALUE, oas.getPaths().get(PETS_PATH).getGet()
                .getParameters().get(0).getExample());
    }

    @Test(description = "Convert extensions everywhere applicable #15")
    public void testIssue15() throws Exception {
        OpenAPI oas = getConvertedOpenAPIFromJsonFile(ISSUE_15_JSON);
        assertNotNull(oas);

        Info info = oas.getInfo();
        assertNotNull(info.getExtensions().get("x-apis-json"));
        assertNotNull(info.getLicense().getExtensions().get("x-notes"));
        assertNotNull(oas.getExternalDocs().getExtensions().get("x-docs-extension"));
        assertNotNull(oas.getTags().get(0).getExtensions().get("x-tag-extension"));
        assertNotNull(oas.getTags().get(0).getExternalDocs().getExtensions().get("x-tag-docs-extension"));

        PathItem pathItem = oas.getPaths().get("/something");
        assertNotNull(pathItem.getExtensions().get("x-path-item-extension"));

        Operation get = pathItem.getGet();
        assertNotNull(get.getExtensions().get("x-version"));
        assertNotNull(get.getExternalDocs().getExtensions().get("x-operation-docs-extension"));
        assertNotNull(get.getResponses().get("200").getExtensions().get("x-response-extension"));

        ArraySchema schema = (ArraySchema) get.getParameters().get(0).getSchema();
        assertNull(schema.getItems().getExtensions().get(X_EXAMPLE));

        Map<String, SecurityScheme> securitySchemes = oas.getComponents().getSecuritySchemes();
        assertNotNull(securitySchemes);
        assertNotNull(securitySchemes.get("OAuth2Implicit").getExtensions().get("x-auth-extension"));
    }

    @Test(description = "Security missing")
    public void testIssue16() throws Exception {
        OpenAPI oas = getConvertedOpenAPIFromJsonFile(ISSUE_16_JSON);
        assertNotNull(oas.getSecurity());
        assertNotNull(oas.getComponents().getSecuritySchemes());
    }

    @Test(description = "Referenced parameters are converted incorrectly")
    public void testIssue17() throws Exception {
        OpenAPI oas = getConvertedOpenAPIFromJsonFile(ISSUE_17_JSON);
        Map<String, RequestBody> requestBodies = oas.getComponents().getRequestBodies();
        assertNotNull(requestBodies.get("formEmail").getContent().get("multipart/form-data"));
        assertNotNull(requestBodies.get("formPassword").getContent().get("multipart/form-data"));
        assertNotNull(requestBodies.get("bodyParam").getContent().get("*/*"));
        assertEquals(oas.getPaths().get("/formPost").getPost().getParameters().get(0).get$ref(),
                REQUEST_BODY_FORMEMAIL);
        assertNotNull(oas.getPaths().get("/report/{userId}").getGet().getRequestBody().
                getContent().get("multipart/form-data").getSchema().getProperties().get("limitForm"));
    }

    @Test(description = "External Docs in Operations")
    public void testIssue18() throws Exception {
        OpenAPI oas = getConvertedOpenAPIFromJsonFile(ISSUE_18_JSON);
        assertNotNull(oas.getPaths().get(PETS_PATH).getGet().getExternalDocs());
    }

    @Test(description = "Request Body Completions")
    public void testIssue19() throws Exception {
        OpenAPI oas = getConvertedOpenAPIFromJsonFile(ISSUE_19_JSON);
        RequestBody requestBody = oas.getPaths().get(POST_PATH).getPost().getRequestBody();
        assertNotNull(requestBody);
        Schema schema = requestBody.getContent().get(URLENCODED_CONTENT).getSchema();
        assertNotNull(schema);
        Map properties = schema.getProperties();
        assertEquals(PROPERTIES_SIZE, properties.size());
        ArraySchema ids = (ArraySchema) properties.get("ids");
        assertEquals(new Integer(MIN_ITEMS), ids.getMinItems());
        assertEquals(new Integer(MAXIMUM), ids.getMaxItems());
        //Fixed from Issue 19
        assertEquals(Boolean.TRUE, ids.getUniqueItems());

        Schema login = (Schema) properties.get("login");
        assertEquals(new Integer(MIN_LENGTH), login.getMinLength());
        assertEquals(new Integer(MAX_LENGTH), login.getMaxLength());
        assertEquals(PATTERN, login.getPattern());
        Schema favNumber = (Schema) properties.get("favNumber");
        assertEquals(new BigDecimal(MAXIMUM), favNumber.getMinimum());
        assertEquals(new BigDecimal(MAXIMUM), favNumber.getMaximum());
        assertEquals(Boolean.TRUE, favNumber.getExclusiveMinimum());
        assertEquals(Boolean.TRUE, favNumber.getExclusiveMaximum());
        assertEquals(new BigDecimal(MULTIPLE_OF_VALUE), new BigDecimal(favNumber.getMultipleOf().doubleValue()));

        Schema dayOfWeek = (Schema) properties.get("dayOfWeek");
        assertEquals(MONDAY_TUESDAY_VALUE, dayOfWeek.getDefault());
        assertEquals(MONDAY_TUESDAY_VALUE, dayOfWeek.getEnum().get(0));
        assertEquals(TUESDAY_WEDNESDAY_VALUE, dayOfWeek.getEnum().get(1));
        assertEquals(WEDNESDAY_THURSDAY_VALUE, dayOfWeek.getEnum().get(2));
    }

    @Test(description = "Response $ref's ")
    public void testIssue20() throws Exception {
        OpenAPI oas = getConvertedOpenAPIFromJsonFile(ISSUE_20_JSON);
        assertNotNull(oas.getComponents().getResponses().get(RESPONSE_200OK_COMPONENT));
        assertNotNull(oas.getComponents().getResponses().get(DEFAULT_RESPONSE_COMPONENT));
    }

    @Test(description = "File Responses to Binary")
    public void testIssue21() throws Exception {
        OpenAPI oas = getConvertedOpenAPIFromJsonFile(ISSUE_21_JSON);
        assertEquals(BINARY_FORMAT, oas.getPaths().get(FILE_PATH).getGet()
                .getResponses().get("200").getContent().get(APPLICATION_PDF).getSchema().getFormat());
    }

    @Test(description = "Converting Hosts Without Schema")
    public void testIssue22() throws Exception {
        OpenAPI oas = getConvertedOpenAPIFromJsonFile(ISSUE_22_JSON);
        assertNotNull(oas);
    }

    @Test(description = "$ref not updated in components (additional properties)")
    public void testIssue23() throws Exception {
        OpenAPI oas = getConvertedOpenAPIFromJsonFile(ISSUE_23_JSON);
        assertTrue(oas.getComponents().getSchemas().get(MAP_OBJECTS_MODEL).getAdditionalProperties() instanceof Schema);
        Schema additionalProperties = (Schema) oas.getComponents().getSchemas().get(MAP_OBJECTS_MODEL).getAdditionalProperties();
        assertEquals(OBJECT_REF,additionalProperties.get$ref());
    }

    @Test(description = "Covert path item $refs")
    public void testIssue25() throws Exception {
        OpenAPI oas = getConvertedOpenAPIFromJsonFile(ISSUE_25_JSON);
        assertNotNull(oas);
        assertEquals(oas.getPaths().get("/foo2").get$ref(), "#/paths/~1foo");
    }

    @Test(description = "Convert allOff")
    public void testIssue26() throws Exception {
        OpenAPI oas = getConvertedOpenAPIFromJsonFile(ISSUE_26_JSON);
        ComposedSchema extendedErrorModel = (ComposedSchema) oas.getComponents()
                .getSchemas().get(EXTENDED_ERROR_MODEL);
        assertEquals(ERROR_MODEL_REF, extendedErrorModel.getAllOf().get(0).get$ref());
        assertEquals(OBJECT, extendedErrorModel.getAllOf().get(1).getType());
    }

    @Test(description = "500 Error Models with discriminator")
    public void testIssue27() throws Exception {
        OpenAPI oas = getConvertedOpenAPIFromJsonFile(ISSUE_27_JSON);
        assertEquals(REQUIRED_SIZE, oas.getComponents().getSchemas().get(PET_SCHEMA).getRequired().size());
    }

    @Test(description = "OAuth 2 flows and URLs were lost ")
    public void testIssue28() throws Exception {
        OpenAPI oas = getConvertedOpenAPIFromJsonFile(ISSUE_28_JSON);
        OAuthFlow oAuth2Implicit = oas.getComponents().getSecuritySchemes()
                .get(SECURITY_SCHEMA_OAUTH2).getFlows().getImplicit();
        assertEquals(AUTHORIZATION_URL, oAuth2Implicit.getAuthorizationUrl());
        assertEquals(WRITE_PETS_VALUE, oAuth2Implicit.getScopes().get(SCOPE_WRITE_PETS));
        assertEquals(READ_PETS_VALUE, oAuth2Implicit.getScopes().get(SCOPE_READ_PETS));
    }

    @Test(description = "Convert collectionFormat #1 - path, query, header parameters")
    public void testIssue30() throws Exception {
        OpenAPI oas = getConvertedOpenAPIFromJsonFile(ISSUE_30_JSON);
        assertNotNull(oas);
    }

    @Test(description = "No Servers - without host, basePath, scheme")
    public void testIssue31() throws Exception {
        OpenAPI oas = getConvertedOpenAPIFromJsonFile(ISSUE_31_JSON);
        assertNull(oas.getServers());
    }

    @Test(description = "Convert schema, property and array examples")
    public void testIssue32() throws Exception {
        OpenAPI oas = getConvertedOpenAPIFromJsonFile(ISSUE_32_JSON);
        Map<String, Schema> schemas = oas.getComponents().getSchemas();
        assertNotNull(schemas);
        Map properties = schemas.get(USER_MODEL).getProperties();
        assertNotNull(properties);
        assertEquals(((Schema) properties.get(ID)).getExample(), EXAMPLE_42_NUMBER);
        assertEquals(((Schema) properties.get(NAME)).getExample(), ARTHUR_DENT_NAME);
        assertEquals(((ArraySchema) properties.get(FRIEND_IDS)).getItems().getExample(), EXAMPLE_8_NUMBER);
        final List<Integer> numbers = new ArrayList<>();
        numbers.add(3);
        numbers.add(4);
        numbers.add(5);
        assertEquals(((ArraySchema) properties.get(FRIEND_IDS)).getExample(), numbers);
        assertEquals(Json.mapper().writeValueAsString(schemas.get(ARRAY_OF_USERS_MODEL).getExample()), ARRAY_VALUES);
    }

    @Test(description = "Convert response examples")
    public void testIssue33() throws Exception {
        OpenAPI oas = getConvertedOpenAPIFromJsonFile(ISSUE_33_JSON);
        ApiResponse apiResponse = oas.getPaths().get(USERS_PATH).getGet().getResponses().get("200");
        assertNotNull(apiResponse);
        assertNotNull(apiResponse.getContent().get(APPLICATION_YAML).getExample());
        assertNotNull(apiResponse.getContent().get(APPLICATION_JSON));
    }

    @Test(description = "Nice to have: Convert x-nullable to nullable")
    public void testIssue35() throws Exception {
        OpenAPI oas = getConvertedOpenAPIFromJsonFile(ISSUE_35_JSON);
        Operation getOperation = oas.getPaths().get(FOO_PATH).getGet();
        assertNotNull(getOperation);
        List<Parameter> parameters = getOperation.getParameters();
        assertNotNull(parameters);
        assertEquals(parameters.get(0).getSchema().getNullable(), Boolean.TRUE);
        assertEquals(parameters.get(1).getSchema().getNullable(), Boolean.FALSE);
        assertEquals(parameters.get(2).getSchema().getNullable(), Boolean.TRUE);
        assertEquals(getOperation.getResponses().get("200").getContent().get("*/*").getSchema().getNullable(),
                Boolean.TRUE);
        Schema user = oas.getComponents().getSchemas().get(USER_MODEL);
        assertNotNull(user);
        assertEquals(user.getNullable(), Boolean.TRUE);
        assertEquals(((Schema) user.getProperties().get(ID)).getNullable(), Boolean.TRUE);
    }

    @Test(description = "Nice to have: Convert x-example to example")
    public void testIssue36() throws Exception {
        OpenAPI oas = getConvertedOpenAPIFromJsonFile(ISSUE_36_JSON);
        List<Parameter> parameters = oas.getPaths().get(FOO_PATH).getGet().getParameters();
        assertNotNull(parameters);
        assertEquals(parameters.get(0).getExample(), FOO_VALUE);
        assertEquals(parameters.get(1).getExample(), NUMBER_VALUE_TWENTY);
        assertEquals(parameters.get(2).getExample(), CODE_EXAMPLE);

        RequestBody requestBody = oas.getPaths().get(LOGIN_PATH).getPost().getRequestBody();
        assertNotNull(requestBody);
        Map properties = requestBody.getContent().get(CONTENT_TYPE).getSchema().getProperties();
        assertEquals(((Schema) properties.get(EMAIL)).getExample(), EMAIL_VALUE);
        assertEquals(((Schema) properties.get(PASSWORD)).getExample(), PASSWORD_VALUE);
    }

    @Test(description = "OpenAPI v2 converter - examples are lost")
    public void testIssue540() throws Exception {
        OpenAPI oas = getConvertedOpenAPIFromJsonFile(ISSUE_540_JSON);
        Operation operation = oas.getPaths().get("/example").getPost();
        assertNotNull(operation);
        Object example = operation.getRequestBody().getContent().get(APPLICATION_JSON).getExample();
        assertNotNull(example);
        example = operation.getResponses().get("200").getContent().get(APPLICATION_JSON);
        assertNotNull(example);

    }

    @Test(description = "OpenAPI v2 converter - enum values for array parameters are lost ")
    public void testIssue597() throws Exception {
        OpenAPI oas = getConvertedOpenAPIFromJsonFile(ISSUE_597_JSON);
        List<Parameter> parameters = oas.getPaths().get(FOO_PATH).getGet().getParameters();
        assertNotNull(parameters);
        List anEnum = parameters.get(0).getSchema().getEnum();
        assertNotNull(anEnum);
        assertEquals(anEnum.get(0), AVAILABLE);
        assertEquals(anEnum.get(1), PENDING);
        assertEquals(anEnum.get(2), SOLD);
    }

    @Test(description = "Parser Issue: OpenAPI v2 converter - HEAD and OPTIONS operations are lost")
    public void testIssue599() throws Exception {
        OpenAPI oas = getConvertedOpenAPIFromJsonFile(ISSUE_599_JSON);
        Operation operation = oas.getPaths().get(FOO_PATH).getHead();
        assertNotNull(operation);
        assertEquals(operation.getDescription(), HEAD_OPERATION);

        operation = oas.getPaths().get(FOO_PATH).getOptions();
        assertNotNull(operation);
        assertEquals(operation.getDescription(), OPTIONS_OPERATION);
    }

    @Test(description = "OpenAPI v2 converter - required Form parameters are converted as optional ")
    public void testIssue600() throws Exception {
        OpenAPI oas = getConvertedOpenAPIFromJsonFile(ISSUE_600_JSON);

        RequestBody requestBody = oas.getPaths().get(LOGIN_PATH).getPost().getRequestBody();
        assertNotNull(requestBody);
        assertTrue(requestBody.getRequired());
        Schema schema = requestBody.getContent().get(CONTENT_TYPE).getSchema();
        List required = schema.getRequired();
        assertNotNull(required);
        assertEquals(required.size(), REQUIRED_SIZE);
    }

    @Test(description = "OpenAPI v2 converter - ref in RequestBodies are correctly updated")
    public void testIssue647() throws Exception {
        OpenAPI oas = getConvertedOpenAPIFromJsonFile(ISSUE_647_JSON);

        String ref = oas.getComponents().getRequestBodies().get("b").getContent().get("*/*").getSchema().get$ref();
        assertEquals(ref, SCHEMAS_A_REF);
    }

    @Test(description = "OpenAPI v2 converter - NPE when no \"paths\" is empty")
    public void testIssue662() throws Exception {
        OpenAPI oas = getConvertedOpenAPIFromJsonFile(ISSUE_662_JSON);
        assertTrue(oas.getPaths().isEmpty());
    }

    @Test(description = "OpenAPI v2 converter - NullPointerException when converting an spec #")
    public void testIssue672() throws Exception {
        OpenAPI oas = getConvertedOpenAPIFromJsonFile(ISSUE_672_JSON);
        assertNotNull(oas);
    }

    @Test(description = "OpenAPI v2 converter - integer elements of enum are converted to String")
    public void testIssue676() throws Exception {
        OpenAPI oas = getConvertedOpenAPIFromJsonFile(ISSUE_676_JSON);
        List<Parameter> parameters = oas.getPaths().get(FOO_PATH).getGet().getParameters();
        assertNotNull(parameters);

        List anEnum = parameters.get(0).getSchema().getEnum();
        assertNotNull(anEnum);
        assertEquals(anEnum.get(0), 1);
        assertEquals(anEnum.get(1), -2);

        anEnum = parameters.get(1).getSchema().getEnum();
        assertNotNull(anEnum);
        assertEquals(anEnum.get(0), new BigDecimal(1));
        assertEquals(anEnum.get(1), new BigDecimal(-2));

        anEnum = parameters.get(2).getSchema().getEnum();
        assertNotNull(anEnum);
        assertEquals(anEnum.get(0), true);
        assertEquals(anEnum.get(1), false);
    }

    @Test(description = "OpenAPI v2 converter - Error in BodyParameter convertion")
    public void testIssue673() throws Exception {
        final OpenAPI oas = getConvertedOpenAPIFromJsonFile(ISSUE_673_YAML);
        assertNotNull(oas);
        Schema schema = oas.getPaths().get("/integer").getPost().getRequestBody().getContent().get("application/json").getSchema();
        assertNotNull(schema);
        assertTrue(schema.getUniqueItems());
        assertTrue(schema.getExclusiveMaximum());
        assertTrue(schema.getExclusiveMinimum());
        assertEquals(3, schema.getMultipleOf().toBigInteger().intValue());
        assertEquals(new BigDecimal(5), schema.getMinimum());
        assertEquals(new BigDecimal(7), schema.getMaximum());

        schema = oas.getPaths().get("/string").getPost().getRequestBody().getContent().get("application/json").getSchema();
        assertEquals(2, schema.getMinLength().intValue());
        assertEquals(7, schema.getMaxLength().intValue());
        assertEquals("aaa", schema.getPattern());
    }

    @Test(description = "OpenAPI v2 converter - Migrate minLength, maxLength and pattern of String property")
    public void testIssue708() throws Exception {
        final OpenAPI oas = getConvertedOpenAPIFromJsonFile(ISSUE_708_YAML);
        assertNotNull(oas);
        Schema schema = oas.getComponents().getSchemas().get("SomeObj");
        assertNotNull(schema);
        assertEquals(schema.getMinLength(), Integer.valueOf(1));
        assertEquals(schema.getMaxLength(), Integer.valueOf(3));
        assertEquals(schema.getPattern(), "^[0-9]+$");
    }

    @Test(description = "OpenAPI v2 converter - Missing Parameter.style property")
    public void testParameterConversion() throws Exception {
        OpenAPI oas = getConvertedOpenAPIFromJsonFile(PARAMETER_CONVERSION_JSON);
        List<Parameter> parameters = oas.getPaths().get(FOO_PATH).getGet().getParameters();
        assertNotNull(parameters);

        Parameter parameter = parameters.get(0);
        assertEquals(parameter.getStyle(), Parameter.StyleEnum.FORM);
        assertFalse(parameter.getExplode());

        parameter = parameters.get(1);
        assertEquals(parameter.getStyle(), Parameter.StyleEnum.FORM);
        assertTrue(parameter.getExplode());

        parameter = parameters.get(2);
        assertEquals(parameter.getStyle(), Parameter.StyleEnum.SIMPLE);
        assertFalse(parameter.getExplode());

        parameter = parameters.get(3);
        assertEquals(parameter.getStyle(), Parameter.StyleEnum.SIMPLE);
        assertFalse(parameter.getExplode());
    }

    @Test(description = "OpenAPI v2 converter - SwaggerParseResult has message")
    public void testSwaggerParseResultHasMessage() throws Exception {
        SwaggerConverter converter = new SwaggerConverter();
        String swaggerAsString = new String(Files.readAllBytes(Paths.get(getClass().getClassLoader().getResource(PET_STORE_JSON).toURI())));
        ParseOptions parseOptions = new ParseOptions();
        parseOptions.setResolve(false);
        SwaggerParseResult result = converter.readContents(swaggerAsString, null, parseOptions);

        assertNotNull(result.getMessages());
    }

    @Test(description = "OpenAPI v2 converter - Conversion of a spec without a info section")
    public void testIssue755() throws Exception {
        final OpenAPI oas = getConvertedOpenAPIFromJsonFile(ISSUE_755_YAML);
        assertNotNull(oas);
    }

    private OpenAPI getConvertedOpenAPIFromJsonFile(String file) throws IOException, URISyntaxException {
        SwaggerConverter converter = new SwaggerConverter();
        String swaggerAsString = new String(Files.readAllBytes(Paths.get(getClass().getClassLoader().getResource(file).toURI())));
        ParseOptions parseOptions = new ParseOptions();
        parseOptions.setResolve(false);
        SwaggerParseResult result = converter.readContents(swaggerAsString, null, parseOptions);
        assertNotNull(result);
        return result.getOpenAPI();
    }
}