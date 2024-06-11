package io.swagger.parser.util;

import com.fasterxml.jackson.databind.JsonNode;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotSame;

public class DeserializationUtilsTest {

    @Test
    public void testEnumValuesAreNotConvertedToBooleansOAS2() throws IOException {
        String yaml = readFile("src/test/resources/EnumYesNoOnOffOAS2.yaml");

        JsonNode jsonNode = DeserializationUtils.readYamlTree(yaml, null);

        jsonNode.findPath("EnumWithQuotes").get("enum").elements()
                .forEachRemaining(element -> {
                    assertFalse(element.isBoolean());
                    assertNotSame(element.textValue(), "true");
                    assertNotSame(element.textValue(), "false");
                });
        jsonNode.findPath("EnumNoQuotes").get("enum").elements()
                .forEachRemaining(element -> {
                    assertFalse(element.isBoolean());
                    assertNotSame(element.textValue(), "true");
                    assertNotSame(element.textValue(), "false");
                });
    }

    @Test
    public void testEnumValuesAreNotConvertedToBooleansOAS3() throws IOException {
        String yaml = readFile("src/test/resources/EnumYesNoOnOffOAS3.yaml");

        JsonNode jsonNode = DeserializationUtils.readYamlTree(yaml, null);

        jsonNode.findPath("EnumWithQuotes").get("enum").elements()
                .forEachRemaining(element -> {
                    assertFalse(element.isBoolean());
                    assertNotSame(element.textValue(), "true");
                    assertNotSame(element.textValue(), "false");
                });
        jsonNode.findPath("EnumNoQuotes").get("enum").elements()
                .forEachRemaining(element -> {
                    assertFalse(element.isBoolean());
                    assertNotSame(element.textValue(), "true");
                    assertNotSame(element.textValue(), "false");
                });
    }


    private String readFile(String name) throws IOException {
        return new String(Files.readAllBytes(new File(name).toPath()), StandardCharsets.UTF_8);
    }
}
