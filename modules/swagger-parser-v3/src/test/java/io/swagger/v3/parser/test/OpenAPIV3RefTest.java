package io.swagger.v3.parser.test;


import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.junit.Before;
import org.junit.Test;
import org.testng.Assert;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;

import static io.swagger.v3.oas.models.Components.COMPONENTS_SCHEMAS_REF;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;


public class OpenAPIV3RefTest {
    private OpenAPI oas;
    @Before
    public void parseOASSpec() {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        oas = new OpenAPIV3Parser().read("oas3-refs-test/openapi.json", null, options);
    }

    @Test
    public void testRefContainingDot() {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        SwaggerParseResult result = new OpenAPIV3Parser().readLocation("resolve-dot-containing-ref/standaloneSpec.yaml", null, options);

        Assert.assertNotNull(result.getOpenAPI());
        Assert.assertTrue(result.getMessages().isEmpty(), "No error messages should be present");
        Assert.assertNotNull(result.getOpenAPI().getPaths().get("/endpoint").getGet().getParameters());
        Assert.assertEquals(result.getOpenAPI().getPaths().get("/endpoint").getGet().getParameters().get(0).getName(), "FbtPrincipalIdentity");
    }

    @Test
    public void testExternalRefContainingDot() {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        SwaggerParseResult result = new OpenAPIV3Parser().readLocation("resolve-dot-containing-ref/externalRefSpec.yaml", null, options);

        Assert.assertNotNull(result.getOpenAPI());
        Assert.assertTrue(result.getMessages().isEmpty(), "No error messages should be present");
        Assert.assertNotNull(result.getOpenAPI().getPaths().get("/endpoint").getGet().getParameters());
        Assert.assertEquals(result.getOpenAPI().getPaths().get("/endpoint").getGet().getParameters().get(0).getName(), "FbtPrincipalIdentity");
    }

    @Test
    public void testParameterExampleRefProcessed() {
        String paramName = "correlation_id";
        Map<String, Example> paramExamples = oas.getComponents().getParameters().get(paramName).getExamples();
        Assert.assertEquals(paramExamples.size(), 1, "Parameter has an example");
        Assert.assertTrue(paramExamples.values()
                .stream().allMatch(e -> e.get$ref().equalsIgnoreCase("#/components/examples/" + paramName)),
                "Examples are referenced");
        Assert.assertEquals(oas.getComponents().getExamples().get(paramName).getValue(), "7758b780aaaca",
                "Examples are processed");
    }

    @Test
    public void testDiscriminatorMappingRefsUpdated() {
         Schema reqSchema = oas.getPaths().values().stream()
                .findFirst()
                .map(path -> path.getPost().getRequestBody().getContent().getOrDefault("application/json", null))
                 .map(MediaType::getSchema)
                .orElseThrow(() -> new IllegalStateException("Path not processed!"));
        Collection<String> discriminator = reqSchema.getDiscriminator().getMapping().values();
        Set<String> allOfs = ((List<Schema>)reqSchema.getAnyOf())
                .stream().map(Schema::get$ref).collect(Collectors.toSet());
        assertTrue(allOfs.stream().allMatch(s -> s.contains(COMPONENTS_SCHEMAS_REF)),"Schema mappings are processed");
        assertTrue(allOfs.containsAll(discriminator),"Discriminator mappings are updated");

    }

    @Test
    public void testCallbackRef() {
        assertEquals(oas.getComponents().getCallbacks().size(), 1, "Callbacks processed");
        Operation cbOperation = oas.getComponents().getCallbacks()
                .get("vaccination_complete")
                .get("{$request.body#/callback_url}/pets/{$request.path.id}/vaccinations").getPut();
        assertNotNull(cbOperation);
        assertEquals(cbOperation.getRequestBody().getContent().get("application/json").getSchema().get$ref(),
                COMPONENTS_SCHEMAS_REF + "vaccination_record", "Callback Request body processed");
        assertEquals(cbOperation.getResponses().get("400").getContent().get("application/json").getSchema().get$ref(),
                COMPONENTS_SCHEMAS_REF + "error" , "Callback responses Processed");
    }

    @Test
    public void testComposedArrayItemsRef() {
        Schema adoptionRequest = oas.getComponents()
               .getSchemas().get("adoption_request");
        assertEquals(adoptionRequest.getTitle(), "Adoption Request", "Processed ref Schemas");
        assertEquals(((Schema) adoptionRequest.getProperties().get("adopter_alias")).get$ref(),
                COMPONENTS_SCHEMAS_REF + "alias_array", "Processed ref added to Schemas");
        Schema adopterAlias = ((Schema) oas.getComponents().getSchemas().get("alias_array"));
        assertEquals(adopterAlias.getTitle(),"AdopterAlias", "Processed Schemas");
        assertEquals(adopterAlias.getItems().getAllOf().size(), 2, "Processed schemas items");
    }
}

