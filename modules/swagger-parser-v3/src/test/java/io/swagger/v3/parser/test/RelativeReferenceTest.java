package io.swagger.v3.parser.test;


import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import io.swagger.v3.parser.urlresolver.PermittedUrlsChecker;
import io.swagger.v3.parser.util.RemoteUrl;
import mockit.Expectations;
import mockit.Mocked;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;


public class RelativeReferenceTest {
    @Mocked
    RemoteUrl remoteUrl;

    private static final String SPEC =
            "openapi: 3.0.0\n" +
                    "servers:\n" +
                    "  - url: /\n" +
                    "  - url: https://localhost:8080/{version}\n" +
                    "    description: The local server\n" +
                    "    variables:\n" +
                    "      version:\n" +
                    "        default: v2\n" +
                    "        enum:\n" +
                    "          - v1\n" +
                    "          - v2\n" +
                    "info:\n" +
                    "  description: It works.\n" +
                    "  version: 1.0.0\n" +
                    "  title: My API\n" +
                    "paths:\n" +
                    "  /samplePath:\n" +
                    "    $ref: './path/samplePath.yaml'";
    private static final String SAMPLE_PATH =
            "get:\n" +
                    "  responses:\n" +
                    "    '200':\n" +
                    "      description: It works\n" +
                    "  requestBody:\n" +
                    "    content:\n" +
                    "      application/json:\n" +
                    "        schema:\n" +
                    "          type: object\n" +
                    "    required: true";

    @Test
    public void testIssueServerUrlValidation() throws Exception {
        new Expectations() {{
            RemoteUrl.urlToString("http://foo.bar.com/swagger.json", Collections.emptyList());
            times = 1;
            result = SPEC;
        }};

        SwaggerParseResult swaggerParseResult = new OpenAPIV3Parser().readLocation("http://foo.bar.com/swagger.json", null, new ParseOptions());
        assertNotNull(swaggerParseResult.getOpenAPI());
        assertTrue(swaggerParseResult.getMessages().isEmpty());
    }

    @Test
    public void testIssue213() throws Exception {
        new Expectations() {{
            RemoteUrl.urlToString("http://foo.bar.com/swagger.json", Collections.emptyList());
            times = 1;
            result = SPEC;

            RemoteUrl.urlToString("http://foo.bar.com/path/samplePath.yaml", Collections.emptyList(), (PermittedUrlsChecker) any);
            times = 1;
            result = SAMPLE_PATH;
        }};


        OpenAPI swagger = new OpenAPIV3Parser().read("http://foo.bar.com/swagger.json");
        assertNotNull(swagger);
        assertNotNull(swagger.getPaths().get("/samplePath"));

        assertNotNull(swagger.getPaths().get("/samplePath").getGet());
        assertNotNull(swagger.getPaths().get("/samplePath").getGet().getRequestBody());
        RequestBody body = swagger.getPaths().get("/samplePath").getGet().getRequestBody();
        assertNotNull(body.getContent().get("application/json").getSchema());
    }

    @Test
    public void testIssue409() {
        String yaml = "openapi: 3.0.0\n" +
                "servers:\n" +
                "  - url: /\n" +
                "info:\n" +
                "  title: My API\n" +
                "  description: It works.\n" +
                "  version: 1.0.0\n" +
                "paths: {}\n" +
                "components:\n" +
                "  schemas:\n" +
                "\n" +
                "  # ===============================================================================\n" +
                "  # Fragments\n" +
                "  # ===============================================================================\n" +
                "\n" +
                "    ID:\n" +
                "      description: An entity identifer\n" +
                "      type: integer\n" +
                "      format: int64\n" +
                "      readOnly: true\n" +
                "\n" +
                "  # ===========================================================================\n" +
                "  # Users\n" +
                "  # ===========================================================================\n" +
                "\n" +
                "    User:\n" +
                "      type: object\n" +
                "      required:\n" +
                "        - emailAddress\n" +
                "      properties:\n" +
                "        id:\n" +
                "          $ref: \"#/components/schemas/ID\"\n" +
                "        emailAddress:\n" +
                "          type: string\n" +
                "          format: email\n" +
                "          minLength: 6\n" +
                "          maxLength: 254";

        OpenAPI swagger = (new OpenAPIV3Parser().readContents(yaml, null, null)).getOpenAPI();
        assertNotNull(swagger.getComponents().getSchemas().get("ID"));
    }

    @Test
    public void testResolveRelativePaths() {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        SwaggerParseResult parseResult = new OpenAPIV3Parser().readLocation("/relative-references-example/openapi.yaml", null, options);

        Assert.assertNotNull(parseResult.getOpenAPI());

        HashSet<String> validationMessages = new HashSet<>(null != parseResult.getMessages() ? parseResult.getMessages() : new ArrayList<>());

        Assert.assertTrue(validationMessages.isEmpty(), validationMessages.toString());
    }

    @Test
    public void testResolveRelativeSiblingPaths() {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        SwaggerParseResult parseResult = new OpenAPIV3Parser().readLocation("/relativeParent/root/root.yaml", null, options);

        Assert.assertNotNull(parseResult.getOpenAPI());

        HashSet<String> validationMessages = new HashSet<>(null != parseResult.getMessages() ? parseResult.getMessages() : new ArrayList<>());
        Assert.assertTrue(validationMessages.isEmpty(), validationMessages.toString());

    }
}
