package io.swagger.parser.util;

import org.testng.annotations.Test;

import java.nio.file.Paths;

import static org.testng.Assert.assertEquals;

public class PathUtilTest {

    @Test
    public void testGetParentDirectoryOfFile() throws Exception {

        final String actualResult = PathUtils.getParentDirectoryOfFile("src/test/resources/parent.json").toString();

        final String expectedResult = Paths.get("src/test/resources").toAbsolutePath().toString();

        assertEquals(actualResult, expectedResult);
    }
}
