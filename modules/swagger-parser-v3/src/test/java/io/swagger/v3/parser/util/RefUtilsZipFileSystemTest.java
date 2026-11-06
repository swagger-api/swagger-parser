package io.swagger.v3.parser.util;

import io.swagger.v3.parser.models.RefFormat;
import io.swagger.v3.parser.urlresolver.PermittedUrlsChecker;
import org.junit.Test;

import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

public class RefUtilsZipFileSystemTest {

    @Test
    public void testReadExternalRefFromZipFileSystem() throws Exception {
        Path archive = Files.createTempFile("ref-utils", ".zip");
        Files.delete(archive);

        Map<String, String> environment = Collections.singletonMap("create", "true");
        URI archiveUri = URI.create("jar:" + archive.toUri());
        String expectedContent = "openapi: 3.0.0\n";

        try (FileSystem zipFileSystem = FileSystems.newFileSystem(archiveUri, environment)) {
            Path parentDirectory = zipFileSystem.getPath("/specs");
            Path referencedFile = parentDirectory.resolve("error.openapi.yaml");
            Files.createDirectories(parentDirectory);
            Files.write(referencedFile, expectedContent.getBytes(UTF_8));

            assertNotSame(FileSystems.getDefault(), referencedFile.getFileSystem());

            String actualContent = RefUtils.readExternalRef(
                    "./error.openapi.yaml",
                    RefFormat.RELATIVE,
                    Collections.emptyList(),
                    parentDirectory,
                    new PermittedUrlsChecker());

            assertEquals(expectedContent, actualContent);
        } finally {
            Files.deleteIfExists(archive);
        }
    }
}
