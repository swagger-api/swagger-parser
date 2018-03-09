package io.swagger.v3.parser.test;



import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.testng.annotations.Test;

import java.util.Arrays;

import static org.testng.Assert.assertEquals;

public class AnchorTest {
    @Test
    public void testIssue146() {
        String yaml = "openapi: 3.0.0\n" +
                "servers: []\n" +
                "info:\n" +
                "  version: 0.0.1\n" +
                "  title: API\n" +
                "x-types:\n" +
                "  OperationType:\n" +
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
                "              enum:\n" +
                "                - registration\n" +
                "            default:\n" +
                "              - registration\n" +
                "      responses:\n" +
                "        '200':\n" +
                "          description: OK\n" +
                "          content:\n" +
                "            '*/*':\n" +
                "              schema:\n" +
                "                $ref: '#/components/schemas/OperationType'\n" +
                "components:\n" +
                "  schemas:\n" +
                "    OperationType:\n" +
                "      type: string\n" +
                "      enum:\n" +
                "        - registration";

        SwaggerParseResult result = new OpenAPIV3Parser().readContents(yaml,null,null);

        Schema model = result.getOpenAPI().getComponents().getSchemas().get("OperationType");
        assertEquals(model.getEnum(), Arrays.asList("registration"));
    }
}
