package io.swagger.parser.util;

import io.swagger.models.Model;
import io.swagger.models.ModelImpl;
import io.swagger.models.auth.AuthorizationValue;
import io.swagger.models.refs.RefFormat;
import io.swagger.parser.processors.ExternalRefProcessor;
import mockit.Injectable;
import mockit.Mocked;
import mockit.StrictExpectations;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public class RefUtilsTest {

    @Test
    public void testComputeDefinitionName() throws Exception {
        //URL refs
        doComputeDefinitionNameTestCase("http://my.company.com/path/to/file.json", "file");
        doComputeDefinitionNameTestCase("http://my.company.com/path/to/file.json#/foo", "foo");
        doComputeDefinitionNameTestCase("http://my.company.com/path/to/file.json#/foo/bar", "bar");
        doComputeDefinitionNameTestCase("http://my.company.com/path/to/file.json#/foo/bar/hello", "hello");

        doComputeDefinitionNameTestCase("http://my.company.com/path/to/file", "file");
        doComputeDefinitionNameTestCase("http://my.company.com/path/to/file#/foo", "foo");
        doComputeDefinitionNameTestCase("http://my.company.com/path/to/file#/foo/bar", "bar");
        doComputeDefinitionNameTestCase("http://my.company.com/path/to/file#/foo/bar/hello", "hello");

        doComputeDefinitionNameTestCase("http://my.company.com/path/to/file.yaml", "file");
        doComputeDefinitionNameTestCase("http://my.company.com/path/to/file.yaml#/foo", "foo");
        doComputeDefinitionNameTestCase("http://my.company.com/path/to/file.yaml#/foo/bar", "bar");
        doComputeDefinitionNameTestCase("http://my.company.com/path/to/file.yaml#/foo/bar/hello", "hello");

        //Relative file refs
        doComputeDefinitionNameTestCase("./path/to/file.json", "file");
        doComputeDefinitionNameTestCase("./path/to/file.json#/foo", "foo");
        doComputeDefinitionNameTestCase("./path/to/file.json#/foo/bar", "bar");
        doComputeDefinitionNameTestCase("./path/to/file.json#/foo/bar/hello", "hello");

        doComputeDefinitionNameTestCase("./path/to/file.yaml", "file");
        doComputeDefinitionNameTestCase("./path/to/file.yaml#/foo", "foo");
        doComputeDefinitionNameTestCase("./path/to/file.yaml#/foo/bar", "bar");
        doComputeDefinitionNameTestCase("./path/to/file.yaml#/foo/bar/hello", "hello");

        doComputeDefinitionNameTestCase("./path/to/file", "file");
        doComputeDefinitionNameTestCase("./path/to/file#/foo", "foo");
        doComputeDefinitionNameTestCase("./path/to/file#/foo/bar", "bar");
        doComputeDefinitionNameTestCase("./path/to/file#/foo/bar/hello", "hello");
    }

    private void doComputeDefinitionNameTestCase(String ref, String expectedDefinitionName) {
        assertEquals(expectedDefinitionName, RefUtils.computeDefinitionName(ref));
    }

    private Map<String, Model> createMap(String... keys) {
        Map<String, Model> definitionMap = new HashMap<>();

        for (String key : keys) {
            definitionMap.put(key, new ModelImpl());
        }

        return definitionMap;
    }

    @Test
    public void testIsAnExternalRefFormat() throws Exception {
        final RefFormat[] values = RefFormat.values();

        for (RefFormat value : values) {
            switch (value) {

                case URL:
                case RELATIVE:
                    assertTrue(RefUtils.isAnExternalRefFormat(value));
                    break;
                case INTERNAL:
                    assertFalse(RefUtils.isAnExternalRefFormat(value));
                    break;
                default:
                    fail("Unhandled RefFormat condition: " + value);
            }
        }
    }

    @Test
    public void testReadExternalRef_UrlFormat(@Injectable final List<AuthorizationValue> auths,
                                              @Mocked RemoteUrl remoteUrl
    ) throws Exception {

        final String url = "http://my.company.com/path/to/file.json";
        final String expectedResult = "really good json";

        new StrictExpectations() {{
            RemoteUrl.urlToString(url, auths);
            times = 1;
            result = expectedResult;
        }};

        String actualResult = RefUtils.readExternalRef(url, RefFormat.URL, auths, null);
        assertEquals(expectedResult, actualResult);
    }

    @Test
    public void testReadExternalRef_UrlFormat_ExceptionThrown(@Injectable final List<AuthorizationValue> auths,
                                                              @Injectable final Exception mockedException,
                                                              @Mocked RemoteUrl remoteUrl
    ) throws Exception {

        final String url = "http://my.company.com/path/to/file.json";

        new StrictExpectations() {{
            RemoteUrl.urlToString(url, auths);
            times = 1;
            result = mockedException;
        }};

        try {
            RefUtils.readExternalRef(url, RefFormat.URL, auths, null);
            fail("Should have thrown an exception");
        } catch (RuntimeException e) {
            assertEquals(mockedException, e.getCause());
        }

    }

    @Test
    public void testReadExternalRef_RelativeFileFormat(@Injectable final List<AuthorizationValue> auths,
                                                       @Mocked IOUtils ioUtils,
                                                       @Mocked final FileInputStream fileInputStream,
                                                       @Mocked Files files,
                                                       @Injectable final Path parentDirectory,
                                                       @Injectable final Path pathToUse,
                                                       @Injectable final File file
    ) throws Exception {

        final String filePath = "./path/to/file.json";
        final String expectedResult = "really good json";

        setupRelativeFileExpectations(fileInputStream, parentDirectory, pathToUse, file, filePath);

        new StrictExpectations() {{
            IOUtils.toString(fileInputStream, "UTF-8");
            times = 1;
            result = expectedResult;
        }};

        String actualResult = RefUtils.readExternalRef(filePath, RefFormat.RELATIVE, auths, parentDirectory);
        assertEquals(expectedResult, actualResult);
    }

    private void setupRelativeFileExpectations(@Mocked final FileInputStream fileInputStream, @Injectable final Path parentDirectory, @Injectable final Path pathToUse, @Injectable final File file, final String filePath) throws Exception {
        new StrictExpectations() {{

            parentDirectory.resolve(filePath).normalize();
            times = 1;
            result = pathToUse;

            Files.exists(pathToUse);
            times = 1;
            result = true;

            pathToUse.toFile();
            times = 1;
            result = file;

            new FileInputStream(file);
            times = 1;
            result = fileInputStream;
        }};
    }

    @Test
    public void testReadExternalRef_RelativeFileFormat_ExceptionThrown(@Injectable final List<AuthorizationValue> auths,
                                                                       @Mocked IOUtils ioUtils,
                                                                       @Mocked final FileInputStream fileInputStream,
                                                                       @Mocked Files files,
                                                                       @Injectable final IOException mockedException,
                                                                       @Injectable final Path parentDirectory,
                                                                       @Injectable final Path pathToUse,
                                                                       @Injectable final File file
    ) throws Exception {
        final String filePath = "./path/to/file.json";

        setupRelativeFileExpectations(fileInputStream, parentDirectory, pathToUse, file, filePath);

        new StrictExpectations() {{
            IOUtils.toString(fileInputStream, "UTF-8");
            times = 1;
            result = mockedException;
        }};

        try {
            RefUtils.readExternalRef(filePath, RefFormat.RELATIVE, auths, parentDirectory);
            fail("Should have thrown an exception");
        } catch (RuntimeException e) {
            assertEquals(mockedException, e.getCause());
        }
    }

    @Test
    public void testReadExternalRef_InternalFormat(@Injectable final List<AuthorizationValue> auths) throws Exception {

        final String file = "#/defintiions/foo";

        try {
            RefUtils.readExternalRef(file, RefFormat.INTERNAL, auths, null);
            fail("Should have thrown an exception");
        } catch (RuntimeException e) {

        }
    }

    @Test
    public void testReadExternalRef_OnClasspath(@Mocked Files files,
                                                @Mocked ClasspathHelper classpathHelper,
                                                @Injectable final Path parentDirectory,
                                                @Injectable final Path pathToUse,
                                                @Injectable final Path pathToUse2) throws Exception {
        final String filePath = "./path/to/file.json";
        final String url = parentDirectory + "/path/to/file.json";
        final String expectedResult = "really good json";

        new StrictExpectations() {{

            parentDirectory.resolve(filePath).normalize();
            times = 1;
            result = pathToUse;

            Files.exists(pathToUse);
            times = 1;
            result = false;

            parentDirectory.resolve(url).normalize();
            times = 1;
            result = pathToUse2;

            Files.exists(pathToUse2);
            times = 1;
            result = false;

            ClasspathHelper.loadFileFromClasspath(filePath); times=1; result=expectedResult;

        }};

        final String actualResult = RefUtils.readExternalRef(filePath, RefFormat.RELATIVE, null, parentDirectory);

        assertEquals(actualResult, expectedResult);

    }

    @Test
    public void testPathJoin1() {
        // simple
        assertEquals(ExternalRefProcessor.join("http://foo.bar.com", "fun"), "http://foo.bar.com/fun");
        assertEquals(ExternalRefProcessor.join("http://foo.bar.com/", "fun"), "http://foo.bar.com/fun");
        assertEquals(ExternalRefProcessor.join("http://foo.bar.com/", "/fun"), "http://foo.bar.com/fun");
        assertEquals(ExternalRefProcessor.join("http://foo.bar.com", "/fun"), "http://foo.bar.com/fun");

        // relative to host
        assertEquals(ExternalRefProcessor.join("http://foo.bar.com/foo/bar", "/fun"), "http://foo.bar.com/fun");
        assertEquals(ExternalRefProcessor.join("http://foo.bar.com/foo/bar#/baz/bat", "/fun"), "http://foo.bar.com/fun");
        assertEquals(ExternalRefProcessor.join("http://foo.bar.com/foo/bar#/baz/bat", "/fun#for/all"), "http://foo.bar.com/fun#for/all");

        // relative
        assertEquals(ExternalRefProcessor.join("http://foo.bar.com", "./fun"), "http://foo.bar.com/fun");
        assertEquals(ExternalRefProcessor.join("http://foo.bar.com/veryFun", "./fun"), "http://foo.bar.com/fun");
        assertEquals(ExternalRefProcessor.join("http://foo.bar.com/veryFun/", "../fun#nothing"), "http://foo.bar.com/fun#nothing");
        assertEquals(ExternalRefProcessor.join("http://foo.bar.com/veryFun/notFun", "../fun#/it/is/fun"), "http://foo.bar.com/fun#/it/is/fun");

        // with file extensions
        assertEquals(ExternalRefProcessor.join("http://foo.bar.com/baz/bat.yaml", "../fun/times.yaml"), "http://foo.bar.com/fun/times.yaml");

        // hashes
        assertEquals(ExternalRefProcessor.join("http://foo.bar.com/veryFun/", "../fun#/it/is/fun"), "http://foo.bar.com/fun#/it/is/fun");

        // relative locations
        assertEquals(ExternalRefProcessor.join("./foo#/definitions/Foo", "./bar#/definitions/Bar"), "./bar#/definitions/Bar");
    }
}
