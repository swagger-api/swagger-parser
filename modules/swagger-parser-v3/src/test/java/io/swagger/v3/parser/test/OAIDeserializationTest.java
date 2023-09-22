package io.swagger.v3.parser.test;


import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.SpecVersion;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import io.swagger.v3.parser.OpenAPIV3Parser;
import org.testng.annotations.Test;

import java.util.HashMap;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.AssertJUnit.assertEquals;

public class OAIDeserializationTest {
    @Test
    public void testDeserializeSimpleDefinition() throws Exception {
        String json =
                "{\n" +
                        "  \"openapi\": \"3.0.1\",\n" +
                        "  \"info\": {\n" +
                        "    \"title\": \"Swagger Petstore\",\n" +
                        "    \"description\": \"This is a sample server Petstore server. You can find out more about Swagger at [http://swagger.io](http://swagger.io) or on [irc.freenode.net, #swagger](http://swagger.io/irc/). For this sample, you can use the api key `special-key` to test the authorization filters.\",\n" +
                        "    \"termsOfService\": \"http://swagger.io/terms/\",\n" +
                        "    \"contact\": {\n" +
                        "      \"email\": \"apiteam@swagger.io\"\n" +
                        "    },\n" +
                        "    \"license\": {\n" +
                        "      \"name\": \"Apache 2.0\",\n" +
                        "      \"url\": \"http://www.apache.org/licenses/LICENSE-2.0.html\"\n" +
                        "    },\n" +
                        "    \"version\": \"1.0.0\"\n" +
                        "  }\n" +
                        "}";
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        SwaggerParseResult result = new OpenAPIV3Parser().readContents(json, null, options);

        assertNotNull(result.getOpenAPI());
    }

    @Test
    public void testIssue911() {
        SwaggerParseResult result = new OpenAPIV3Parser().readLocation("issue_911.yaml", null, null);
        assertEquals(result.getMessages().size(),1);
        assertNotNull(result.getOpenAPI());
    }

    @Test
    public void testDeserializeYamlDefinitionMissingSchema_Issue1951() throws Exception {
        // Create Spec missing schema but with schema items referenced in responses
        OpenAPI api = new OpenAPI();
        api.setSpecVersion(SpecVersion.V30);
        api.setComponents(new Components()); // no schema
        ApiResponse errorResponse = new ApiResponse().content(new Content());
        errorResponse
                .description("My error type")
                .getContent()
                .addMediaType("application/json", new MediaType()
                        .schema(new ComposedSchema().
                                $ref("NotAddedYet")
                                .type("schema")));
        api.getComponents().setResponses(new HashMap<>());
        api.getComponents().getResponses().put("ErrorObj", errorResponse);
        ComposedSchema schema = new ComposedSchema();
        schema.set$ref("ThingRequest");
        schema.setType("schema");
        Content content = new Content().addMediaType("application/json", new MediaType().schema(schema));
        ApiResponses resp = new ApiResponses();
        resp.addApiResponse("401", new ApiResponse().$ref("#/components/responses/ErrorObj"));
        resp.setDefault(new ApiResponse()
                .description("Default response is just a string")
                .content(new Content()
                        .addMediaType("application/json", new MediaType().schema(new StringSchema()))
                )
        );
        api.setPaths(new Paths()
                .addPathItem("/thingy", new PathItem()
                        .post(new Operation()
                                .responses(resp)
                                .requestBody(new RequestBody().content(content))
                        )
                )
        );

        String yaml = Yaml.mapper().writeValueAsString(api);

        // Generated YAML.  Intentionally missing schemas
        // openapi: 3.0.1
        // paths:
        //   /thingy:
        //     post:
        //       requestBody:
        //         content:
        //           application/json:
        //             schema:
        //               $ref: '#/components/schemas/ThingRequest'
        //       responses:
        //         "401":
        //           $ref: '#/components/responses/ErrorObj'
        //         default:
        //           description: Default response is just a string
        //           content:
        //             application/json:
        //               schema:
        //                 type: string
        // components:
        //   responses:
        //     ErrorObj:
        //       description: My error type
        //       content:
        //         application/json:
        //           schema:
        //             $ref: '#/components/schemas/NotAddedYet'

        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        SwaggerParseResult result = new OpenAPIV3Parser().readContents(yaml, null, options);

        assertNotNull(result.getOpenAPI());
        assertTrue(result.getMessages().contains("attribute info is missing"));
        assertTrue(result.getMessages().contains("attribute components.responses.ErrorObj.content.'application/json'.schema.NotAddedYet is missing"));
        assertTrue(result.getMessages().contains("attribute paths.'/thingy'(post).requestBody.content.'application/json'.schema.#/components/schemas/ThingRequest is missing"));
    }
}
