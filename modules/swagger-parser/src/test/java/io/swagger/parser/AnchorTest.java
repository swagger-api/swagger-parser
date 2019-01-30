package io.swagger.parser;

import io.swagger.models.ModelImpl;
import io.swagger.models.Swagger;
import io.swagger.parser.util.DeserializationUtils;
import io.swagger.parser.util.SwaggerDeserializationResult;
import io.swagger.util.Json;
import org.junit.Test;

import java.util.Arrays;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

public class AnchorTest {

    @Test
    public void testIssue146() {
        String yaml = "swagger: '2.0'\n" +
                "\n" +
                "info:\n" +
                "  version: \"0.0.1\"\n" +
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
                "          type: array\n" +
                "          items:\n" +
                "            type: string\n" +
                "            enum: *OperationType\n" +
                "          default: [registration]\n" +
                "      responses:\n" +
                "        200:\n" +
                "          description: OK\n" +
                "          schema:\n" +
                "            $ref: '#/definitions/OperationType'\n" +
                "definitions:\n" +
                "  OperationType:\n" +
                "    type: string\n" +
                "    enum: *OperationType";


        SwaggerDeserializationResult result = new SwaggerParser().readWithInfo(yaml);

        ModelImpl model = (ModelImpl) result.getSwagger().getDefinitions().get("OperationType");
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

        SwaggerDeserializationResult result = new SwaggerParser().readWithInfo(yaml);
        assertEquals(Json.pretty(result.getSwagger()), "{ }");

    }

    @org.junit.Test
    public void testIssue998() throws Exception{

        //DeserializationUtils.getOptions().setMaxYamlDepth(5000);
        Swagger result = new SwaggerParser().read("issue_998.yaml");
        assertNull(result);

    }

    @org.junit.Test
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

        Swagger result = new SwaggerParser().readWithInfo(yaml).getSwagger();
        assertEquals(Json.pretty(result), "{ }");
        result = new SwaggerParser().readWithInfo(yaml2).getSwagger();
        assertEquals(Json.pretty(result), "{ }");
        DeserializationUtils.getOptions().setMaxYamlReferences(10000000L);

    }
}
