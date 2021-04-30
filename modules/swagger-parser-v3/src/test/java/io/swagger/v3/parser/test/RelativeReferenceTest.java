package io.swagger.v3.parser.test;


import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.AuthorizationValue;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import io.swagger.v3.parser.util.RemoteUrl;
import mockit.Expectations;
import mockit.Mocked;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;


public class RelativeReferenceTest {
    @Mocked
    RemoteUrl remoteUrl;

    static final String spec =
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
            "          - v2\n"+
            "info:\n" +
            "  description: It works.\n" +
            "  version: 1.0.0\n" +
            "  title: My API\n" +
            "paths:\n" +
            "  /samplePath:\n" +
            "    $ref: './path/samplePath.yaml'";
    static final String samplePath =
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
            RemoteUrl.urlToString("http://foo.bar.com/swagger.json", Arrays.asList(new AuthorizationValue[]{}));
            times = 1;
            result = spec;
        }};

        SwaggerParseResult swaggerParseResult = new OpenAPIV3Parser().readLocation("http://foo.bar.com/swagger.json", null, new ParseOptions());
        assertNotNull(swaggerParseResult.getOpenAPI());
        assertTrue(swaggerParseResult.getMessages().isEmpty());
    }

    @Test
    public void testIssue213() throws Exception {
        new Expectations() {{
            RemoteUrl.urlToString("http://foo.bar.com/swagger.json", Arrays.asList(new AuthorizationValue[]{}));
            times = 1;
            result = spec;

            RemoteUrl.urlToString("http://foo.bar.com/path/samplePath.yaml", Arrays.asList(new AuthorizationValue[]{}));
            times = 1;
            result = samplePath;
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
                "          $ref: '#/components/schemas/ID'\n" +
                "        emailAddress:\n" +
                "          type: string\n" +
                "          format: email\n" +
                "          minLength: 6\n" +
                "          maxLength: 254";

        OpenAPI swagger = (new OpenAPIV3Parser().readContents(yaml,null, null)).getOpenAPI();
        assertNotNull(swagger.getComponents().getSchemas().get("ID"));
    }
        
    @Test
    public void testResolveRelativePaths() {
    	ParseOptions options = new ParseOptions();
        options.setResolve(true);
        SwaggerParseResult parseResult = new OpenAPIV3Parser().readLocation("/relative-references-example/openapi.yaml", null, options);
        
    	Assert.assertNotNull(parseResult.getOpenAPI());
    	
    	HashSet<String> validationMessages = new HashSet<>(null != parseResult.getMessages() ? parseResult.getMessages() : new ArrayList<>());
    	
    	
    	//validationMessages.forEach(msg->System.out.println(msg));
        //OpenAPI specification = parseResult.getOpenAPI();
    	Assert.assertTrue(validationMessages.isEmpty(), validationMessages.toString());
    	
    }
}
