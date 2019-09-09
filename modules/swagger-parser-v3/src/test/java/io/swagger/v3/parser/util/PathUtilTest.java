package io.swagger.v3.parser.util;

import org.testng.annotations.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

public class PathUtilTest {

    @Test
    public void testGetParentDirectoryOfFile() throws Exception {

        final String actualResult = PathUtils.getParentDirectoryOfFile("src/test/resources/test.yaml").toString();

        final String expectedResult = Paths.get("src/test/resources").toAbsolutePath().toString();

        assertEquals(actualResult, expectedResult);
    }

    @Test
    public void testGetParentDirectoryOfNonExistentFile() throws Exception {

        final Path result = PathUtils.getParentDirectoryOfFile("src/test/resources/parent.json");

        assertNull(result);
    }
}
