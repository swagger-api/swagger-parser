package io.swagger.parser.util;

import com.fasterxml.jackson.databind.JsonNode;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.testng.AssertJUnit.assertFalse;

public class DeserializationUtilsTest {

    @Test
    public void testEnumValuesAreNotConvertedToBooleans() throws IOException {
        String yaml = readFile("src/test/resources/EnumYesNoOnOff.yaml");

        JsonNode jsonNode = DeserializationUtils.readYamlTree(yaml, null);

        jsonNode.findPath("EnumNoQuotes").get("enum").elements()
                .forEachRemaining(element -> assertFalse(element.isBoolean()));
    }

    private String readFile(String name) throws IOException {
        return new String(Files.readAllBytes(new File(name).toPath()), StandardCharsets.UTF_8);
    }
}
