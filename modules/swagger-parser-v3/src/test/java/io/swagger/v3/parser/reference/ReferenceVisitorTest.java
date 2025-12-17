package io.swagger.v3.parser.reference;

import com.fasterxml.jackson.databind.JsonNode;
import org.testng.annotations.Test;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;

public class ReferenceVisitorTest {

    @Test
    public void largeFileShouldBeParsedByJacksonLibraryWhenCodeLimitIsSet() throws Exception {
        System.setProperty("maxYamlCodePoints", "10000000");
        ReferenceVisitor visitor = new ReferenceVisitor(null, null, null, null);
        String resourceName = "/issue2059/largeFile.yaml";
        String content = readResourceAsString(resourceName);

        JsonNode jsonNode = visitor.deserializeIntoTree(content);

        assertNotNull(content);
        assertFalse(content.isEmpty());
        assertNotNull(jsonNode);
        System.clearProperty("maxYamlCodePoints");
    }

    @Test
    public void largeFileShouldNotBeParsedByJacksonLibraryWhenCodeLimitIsNotSet() throws Exception {
        ReferenceVisitor visitor = new ReferenceVisitor(null, null, null, null);
        String resourceName = "/issue2059/largeFile.yaml";
        String content = readResourceAsString(resourceName);

        try {
            visitor.deserializeIntoTree(content);
            // Fail if no exception is thrown
            org.testng.Assert.fail("Expected YAMLException was not thrown");
        } catch (YAMLException ex) {
            org.testng.Assert.assertTrue(
                    ex.getMessage() != null &&
                            ex.getMessage().contains("The incoming YAML document exceeds the limit: 3145728 code points."),
                    "Unexpected YAMLException message: " + ex.getMessage()
            );
        }
    }

    private String readResourceAsString(String resourceName) throws IOException {
        try (InputStream is = this.getClass().getResourceAsStream(resourceName)) {
            if (is == null) {
                throw new IOException("Resource not found: " + resourceName);
            }
            java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
            byte[] data = new byte[4096];
            int nRead;
            while ((nRead = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}