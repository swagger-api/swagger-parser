package io.swagger.parser.test;

import io.swagger.oas.models.OpenAPI;
import io.swagger.oas.models.PathItem;
import io.swagger.parser.models.SwaggerParseResult;
import io.swagger.parser.v2.SwaggerConverter;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

public class V2ConverterTest {
    private static final String ISSUE_2_JSON = "issue-2.json";
    private static final String ISSUE_3_JSON = "issue-3.json";
    private static final String ISSUE_4_JSON = "issue-4.json";
    private static final String ISSUE_6_JSON = "issue-6.json";
    private static final String ISSUE_11_JSON = "issue-11.json";
    private static final String ISSUE_12_JSON = "issue-12.json";
    private static final String ISSUE_14_JSON = "issue-14.json";
    private static final String ISSUE_16_JSON = "issue-16.json";
    private static final String ISSUE_18_JSON = "issue-18.json";
    private static final String ISSUE_31_JSON = "issue-31.json";
    private static final String ISSUE_455_JSON = "issue-455.json";

    @Test
    public void testConvertPetstore() throws Exception {
        SwaggerConverter converter = new SwaggerConverter();
        String swaggerAsString = new String(Files.readAllBytes(Paths.get(getClass().getClassLoader().getResource("petstore.yaml").toURI())));
        SwaggerParseResult result = converter.readContents(swaggerAsString, null, null);

        assertNotNull(result.getOpenAPI());
    }

    @Test
    public void testIssue455() throws Exception {
        OpenAPI oas = getConvertedOpenAPIFromJsonFile(ISSUE_455_JSON);

        assertNotNull(oas);
        assertTrue(oas.getPaths().size() == 1);

        PathItem pathItem = oas.getPaths().get("/api/batch/");
        assertNotNull(pathItem);

        assertTrue(pathItem.getGet().getParameters().size() == 1);
    }

    @Test
    public void testIssue2() throws Exception {
        OpenAPI oas = getConvertedOpenAPIFromJsonFile(ISSUE_2_JSON);
        assertEquals(2, oas.getPaths().get("/api/batch/").getGet().getResponses().size());

    }

    @Test
    public void testIssue3() throws Exception {
        OpenAPI oas = getConvertedOpenAPIFromJsonFile(ISSUE_3_JSON);
        assertNotNull(oas.getServers());
        assertEquals("http://petstore.swagger.io/api", oas.getServers().get(0).getUrl());
    }

    @Test
    public void testIssue4() throws Exception {
        OpenAPI oas = getConvertedOpenAPIFromJsonFile(ISSUE_4_JSON);
        assertNotNull(oas.getPaths().get("/pets").getGet().getResponses().get("200").getContent());
    }

    @Test
    public void testIssue6() throws Exception {
        OpenAPI oas = getConvertedOpenAPIFromJsonFile(ISSUE_6_JSON);
        assertEquals(11l, oas.getPaths().get("/pets").getGet().getParameters().get(0).getSchema().getDefault());
    }

    @Test
    public void testIssue11() throws Exception {
        OpenAPI oas = getConvertedOpenAPIFromJsonFile(ISSUE_11_JSON);
        assertNotNull(oas);
    }

    @Test
    public void testIssue12() throws Exception {
        OpenAPI oas = getConvertedOpenAPIFromJsonFile(ISSUE_12_JSON);
        assertNotNull(oas.getExternalDocs());
    }

    @Test
    public void testIssue14() throws Exception {
        OpenAPI oas = getConvertedOpenAPIFromJsonFile(ISSUE_14_JSON);
        assertEquals("value", oas.getPaths().get("/pets").getGet().getParameters().get(0).getExtensions().get("x-example"));
    }

    @Test
    public void testIssue16() throws Exception {
        OpenAPI oas = getConvertedOpenAPIFromJsonFile(ISSUE_16_JSON);
        assertNotNull(oas.getSecurity());
        assertNotNull(oas.getComponents().getSecuritySchemes());
    }

    @Test
    public void testIssue18() throws Exception {
        OpenAPI oas = getConvertedOpenAPIFromJsonFile(ISSUE_18_JSON);
        assertNotNull(oas.getPaths().get("/something").getGet().getExternalDocs());
    }

    @Test
    public void testIssue31() throws Exception {
        OpenAPI oas = getConvertedOpenAPIFromJsonFile(ISSUE_31_JSON);
        assertNull(oas.getServers());
    }

    private OpenAPI getConvertedOpenAPIFromJsonFile(String file) throws IOException, URISyntaxException {
        SwaggerConverter converter = new SwaggerConverter();
        String swaggerAsString = new String(Files.readAllBytes(Paths.get(getClass().getClassLoader().getResource(file).toURI())));

        SwaggerParseResult result = converter.readContents(swaggerAsString, null, null);
        assertNotNull(result);
        return result.getOpenAPI();
    }
}