package io.swagger.parser.util;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class ClasspathHelperTest {

    @Test
    public void testLoadFileFromClasspath() throws Exception {
        final String contents = ClasspathHelper.loadFileFromClasspath("classpathTest.txt");
        assertEquals(contents, "How now brown cow?");
    }

    @Test(expectedExceptions = {RuntimeException.class})
    public void testLoadFileFromClasspath_DoesntExist() throws Exception {
        ClasspathHelper.loadFileFromClasspath("nothing.txt");

    }
}
