package io.swagger.parser;

import io.swagger.models.ModelImpl;
import io.swagger.parser.util.SwaggerDeserializationResult;
import org.testng.annotations.Test;

import java.util.Arrays;

import static org.testng.Assert.assertEquals;

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
}
