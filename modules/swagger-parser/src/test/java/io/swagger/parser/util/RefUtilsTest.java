package io.swagger.parser.util;

import io.swagger.models.Model;
import io.swagger.models.ModelImpl;
import io.swagger.models.auth.AuthorizationValue;
import io.swagger.models.refs.RefFormat;
import mockit.Injectable;
import mockit.Mocked;
import mockit.StrictExpectations;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.*;

/**
 * Created by russellb337 on 7/15/15.
 */
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

    @Test
    public void testDeconflictName() throws Exception {
        doDeconflictNameTestCase("foo", "foo");
        doDeconflictNameTestCase("foo", "foo1", "foo");
        doDeconflictNameTestCase("foo", "foo2", "foo", "foo1");
        doDeconflictNameTestCase("foo", "foo3", "foo", "foo1", "foo2");
    }

    private void doDeconflictNameTestCase(String input, String expectedOutput, String... existingKeys) {
        assertEquals(expectedOutput, RefUtils.deconflictName(input, createMap(existingKeys)));
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

        String actualResult = RefUtils.readExternalRef(url, RefFormat.URL, auths);
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
            RefUtils.readExternalRef(url, RefFormat.URL, auths);
            fail("Should have thrown an exception");
        } catch (RuntimeException e) {
            assertEquals(mockedException, e.getCause());
        }

    }

    @Test
    public void testReadExternalRef_RelativeFileFormat(@Injectable final List<AuthorizationValue> auths,
                                                       @Mocked IOUtils ioUtils,
                                                       @Mocked final FileInputStream fileInputStream
    ) throws Exception {

        final String file = "./path/to/file.json";
        final String expectedResult = "really good json";

        new StrictExpectations() {{
            new FileInputStream(file);
            times = 1;
            result = fileInputStream;

            IOUtils.toString(fileInputStream);
            times = 1;
            result = expectedResult;
        }};

        String actualResult = RefUtils.readExternalRef(file, RefFormat.RELATIVE, auths);
        assertEquals(expectedResult, actualResult);
    }

    @Test
    public void testReadExternalRef_RelativeFileFormat_ExceptionThrown(@Injectable final List<AuthorizationValue> auths,
                                                                       @Mocked IOUtils ioUtils,
                                                                       @Mocked final FileInputStream fileInputStream,
                                                                       @Injectable final IOException mockedException
                                                                       ) throws Exception {

        final String file = "./path/to/file.json";

        new StrictExpectations() {{
            new FileInputStream(file);
            times = 1;
            result = fileInputStream;

            IOUtils.toString(fileInputStream);
            times = 1;
            result = mockedException;
        }};

        try {
            RefUtils.readExternalRef(file, RefFormat.RELATIVE, auths);
            fail("Should have thrown an exception");
        } catch(RuntimeException e) {
            assertEquals(mockedException, e.getCause());
        }
    }

    @Test
    public void testReadExternalRef_InternalFormat(@Injectable final List<AuthorizationValue> auths) throws Exception {

        final String file = "#/defintiions/foo";

        try {
            RefUtils.readExternalRef(file, RefFormat.INTERNAL, auths);
            fail("Should have thrown an exception");
        } catch(RuntimeException e) {

        }
    }

}
