package io.swagger.v3.parser.test;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import io.swagger.v3.parser.util.DeserializationUtils;
import org.testng.annotations.Test;

import java.util.Arrays;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

public class AnchorTest {

    @Test
    public void testIssue146() {

        String yaml = "openapi: 3.0.0\n" +
                "servers: []\n" +
                "info:\n" +
                "  version: 0.0.1\n" +
                "  title: API\n" +
                "x-types:\n" +
                "  OperationType: &OperationType\n" +
                "    - registration\n" +
                "# Describe your paths here\n" +
                "paths:\n" +
                "  /checker:\n" +
                "    get:\n" +
                "      parameters:\n" +
                "        - name: operations\n" +
                "          in: query\n" +
                "          schema:\n" +
                "            type: array\n" +
                "            items:\n" +
                "              type: string\n" +
                "            enum: *OperationType\n" +
                "            default:\n" +
                "              - registration\n" +
                "      responses:\n" +
                "        '200':\n" +
                "          description: OK\n" +
                "          content:\n" +
                "            '*/*':\n" +
                "              schema:\n" +
                "                $ref: \"#/components/schemas/OperationType\"\n" +
                "components:\n" +
                "  schemas:\n" +
                "    OperationType:\n" +
                "      type: string\n" +
                "      enum: *OperationType";

        SwaggerParseResult result = new OpenAPIV3Parser().readContents(yaml,null,null);

        Schema model = result.getOpenAPI().getComponents().getSchemas().get("OperationType");
        assertEquals(model.getEnum(), Arrays.asList("registration"));
    }


    @Test
    public void testCycle() {

        String yaml = "a:\n" +
                "  a1: &a1\n" +
                "    a2: \n" +
                "    - a3\n" +
                "    - a4\n" +
                "    a5: \n" +
                "    - *a1";

        SwaggerParseResult result = new OpenAPIV3Parser().readContents(yaml,null,null);
        assertEquals(Json.pretty(result.getOpenAPI()), "null");

    }

    @Test
    public void testIssue998() throws Exception{

        //DeserializationUtils.getOptions().setMaxYamlDepth(5000);
        SwaggerParseResult result = new OpenAPIV3Parser().readLocation("./issue_998.yaml",null,null);
        assertNull(result.getOpenAPI());

    }

    @Test
    public void testIssue998Billion() throws Exception{
        DeserializationUtils.getOptions().setMaxYamlReferences(100000L);
        String yaml = "a: &a [\"lol\",\"lol\",\"lol\",\"lol\",\"lol\",\"lol\",\"lol\",\"lol\",\"lol\"]\n" +
                "b: &b [*a,*a,*a,*a,*a,*a,*a,*a,*a]\n" +
                "c: &c [*b,*b,*b,*b,*b,*b,*b,*b,*b]\n" +
                "d: &d [*c,*c,*c,*c,*c,*c,*c,*c,*c]\n" +
                "e: &e [*d,*d,*d,*d,*d,*d,*d,*d,*d]\n" +
                "f: &f [*e,*e,*e,*e,*e,*e,*e,*e,*e]\n" +
                "g: &g [*f,*f,*f,*f,*f,*f,*f,*f,*f]\n" +
                "h: &h [*g,*g,*g,*g,*g,*g,*g,*g,*g]\n" +
                "i: &i [*h,*h,*h,*h,*h,*h,*h,*h,*h]";

        String yaml2 = "a: &a [\"lol\",\"lol\",\"lol\",\"lol\",\"lol\",\"lol\",\"lol\",\"lol\",\"lol\"]\n" +
                "b: &b [*a,*a,*a,*a,*a,*a,*a,*a,*a]\n" +
                "c: &c [*b,*b,*b,*b,*b,*b,*b,*b,*b]\n" +
                "d: &d [*c,*c,*c,*c,*c,*c,*c,*c,*c]\n" +
                "e: &e [*d,*d,*d,*d,*d,*d,*d,*d,*d]\n" +
                "f: &f [*e,*e,*e,*e,*e,*e,*e,*e,*e]\n" +
                "g: &g [*f,*f,*f,*f,*f,*f,*f,*f,*f]\n" +
                "h: &h [*g,*g,*g,*g,*g,*g,*g,*g,*g]";

        String yaml3 = "a: &a [\"lol\"]\n" +
                "b: &b [*a,*a]\n" +
                "c: &c [*b,*b]";

        OpenAPI result = new OpenAPIV3Parser().readContents(yaml,null,null).getOpenAPI();
        assertEquals(Json.pretty(result), "null");
        result = new OpenAPIV3Parser().readContents(yaml2,null,null).getOpenAPI();
        assertEquals(Json.pretty(result), "null");
        DeserializationUtils.getOptions().setMaxYamlReferences(10000000L);

    }

    @Test
    public void testBillionLaughProtectionSnakeYaml() {
        ParseOptions opts = new ParseOptions();
        opts.setResolve(true);
        SwaggerParseResult result = new OpenAPIV3Parser().readLocation("billion_laughs_snake_yaml.yaml",null,opts);
        assertNotNull(result.getOpenAPI().getComponents().getSchemas().get("a1"));
        assertEquals(result.getOpenAPI().getComponents().getSchemas().get("a1").getEnum().get(0), "AA1");
        assertNotNull(result.getOpenAPI().getComponents().getSchemas().get("c1"));
        assertEquals(((Schema)result.getOpenAPI().getComponents().getSchemas().get("c1").getProperties().get("a")).getEnum().get(0), "AA1");


        DeserializationUtils.getOptions().setMaxYamlAliasesForCollections(50);
        DeserializationUtils.getOptions().setYamlAllowRecursiveKeys(false);

        result = new OpenAPIV3Parser().readLocation("billion_laughs_snake_yaml.yaml",null,opts);

        DeserializationUtils.getOptions().setMaxYamlAliasesForCollections(Integer.MAX_VALUE);
        DeserializationUtils.getOptions().setYamlAllowRecursiveKeys(true);

    }
}
