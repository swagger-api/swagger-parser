package io.swagger.v3.parser.test;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.reference.ReferenceUtils;
import org.testng.annotations.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class ParserDeduplicationParameterTest {

    @Test
    public void testIssue2102TwoRefsSameNameDifferentIn31() {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setResolveCombinators(true);
        options.setResolveFully(true);

        OpenAPI openAPI = new OpenAPIV3Parser()
                .read("issue-2102/openapi31.json", null, options);

        List<Parameter> params =
                openAPI.getPaths()
                        .get("/myoperation")
                        .getGet()
                        .getParameters();

        assertEquals(params.size(), 2);
        List<String> paramIns = params.stream().map(Parameter::getIn).collect(Collectors.toList());
        assertTrue(paramIns.contains("query"));
        assertTrue(paramIns.contains("header"));
    }


    @Test(description = "Duplicated parameter name with different locations")
    public void testDuplicatedParameterNameFromRef() {
        OpenAPIV3Parser openApiParser = new OpenAPIV3Parser();
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setFlatten(true);
        OpenAPI openAPI = openApiParser.read("issue-2102/openapi30.json", null, options);

        List<Parameter> parameters = openAPI.getPaths().get("/myoperation").getGet().getParameters();
        assertEquals(parameters.size(), 2);

        long namedParameterCount = parameters.stream()
                .map(param -> {
                    String refName = ReferenceUtils.getRefName(param.get$ref());
                    Components components = openAPI.getComponents();
                    return components.getParameters().get(refName);
                })
                .filter(param -> param.getName().equals("myParam"))
                .count();

        assertEquals((int)namedParameterCount, 2);
    }

    @Test(description = "Duplicated parameter name with different locations")
    public void testDuplicatedParameterNameFromRefResolveFullyResolveCombinators() {
        OpenAPIV3Parser openApiParser = new OpenAPIV3Parser();
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setResolveCombinators(true);
        options.setResolveFully(true);
        OpenAPI openAPI = openApiParser.read("issue-2102/openapi30.json", null, options);

        assertEquals(openAPI.getPaths().get("/myoperation").getGet().getParameters().size(), 2);
        assertEquals((int) openAPI.getPaths().get("/myoperation").getGet().getParameters().stream().filter(param -> param.getName().equals("myParam")).count(), 2);
    }

    @Test
    public void testInlineAndRefSameNameDifferentIn() {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setResolveFully(true);

        OpenAPI openAPI = new OpenAPIV3Parser()
                .read("issue-2102/openapi30mixedRef.yaml", null, options);

        List<Parameter> params =
                openAPI.getPaths()
                        .get("/mixed")
                        .getGet()
                        .getParameters();

        assertEquals(params.size(), 2);
        List<String> paramDescriptions = params.stream()
                .map(p -> p.getIn() + ":" + p.getName())
                .collect(Collectors.toList());
        assertTrue(paramDescriptions.contains("query:myParam"));
        assertTrue(paramDescriptions.contains("header:myParam"));
    }

    @Test
    public void testInlineAndRefSameNameSameIn() {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setResolveFully(true);

        OpenAPI openAPI = new OpenAPIV3Parser()
                .read("issue-2102/openapi30mixedRef.yaml", null, options);

        List<Parameter> params =
                openAPI.getPaths()
                        .get("/mixed")
                        .getGet()
                        .getParameters();

        assertEquals(params.size(), 2);
        List<String> paramDescriptions = params.stream()
                .map(p -> p.getIn() + ":" + p.getName())
                .collect(Collectors.toList());
        assertTrue(paramDescriptions.contains("query:myParam"));
        assertTrue(paramDescriptions.contains("header:myParam"));
    }

    @Test
    public void testOperationLevel_sameNameSameIn_refOverridesInline() {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setResolveFully(true);

        OpenAPI openAPI = new OpenAPIV3Parser()
                .read("issue-2102/openapi30sameNameSameLocation.yaml", null, options);

        List<Parameter> params =
                openAPI.getPaths()
                        .get("/duplicate")
                        .getGet()
                        .getParameters();

        assertEquals(params.size(), 1);

        Parameter p = params.get(0);
        assertEquals(p.getName(), "myParam");
        assertEquals(p.getIn(), "query");
        assertEquals(p.getSchema().getFormat(), "date-time");
    }
}
