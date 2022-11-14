package io.swagger.v3.parser.util;


import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.core.models.AuthorizationValue;
import io.swagger.v3.parser.models.RefFormat;
import io.swagger.v3.parser.processors.ExternalRefProcessor;
import mockit.Injectable;
import mockit.Mocked;
import mockit.StrictExpectations;
import org.apache.commons.io.IOUtils;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
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

    private Map<String, Schema> createMap(String... keys) {
        Map<String, Schema> definitionMap = new HashMap<>();

        for (String key : keys) {
            definitionMap.put(key, new Schema());
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

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Test
    public void testReadExternalRef_UrlFormat_ExceptionThrown(@Injectable final List<AuthorizationValue> auths,
                                                              @Mocked RemoteUrl remoteUrl
    ) throws Exception {

        final String url = "http://my.company.com/path/to/file.json";

        new StrictExpectations() {{
            RemoteUrl.urlToString(url, auths);
            times = 1;
            result = new Exception();
        }};
        thrown.expect(RuntimeException.class);
        RefUtils.readExternalRef(url, RefFormat.URL, auths, null);
    }

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testReadExternalRef_RelativeFileFormat(@Injectable final List<AuthorizationValue> auths,
                                                       @Mocked IOUtils ioUtils,
                                                       @Mocked Files files,
                                                       @Injectable final Path parentDirectory,
                                                       @Injectable final Path pathToUse
    ) throws Exception {
        final String filePath = "file.json";
        File file = tempFolder.newFile(filePath);
        final String expectedResult = "really good json";

        setupRelativeFileExpectations(parentDirectory, pathToUse, file, filePath);

        new StrictExpectations() {{
            IOUtils.toString((FileInputStream) any, UTF_8);
            times = 1;
            result = expectedResult;

        }};

        String actualResult = RefUtils.readExternalRef(filePath, RefFormat.RELATIVE, auths, parentDirectory);
        assertEquals(expectedResult, actualResult);

    }

    private void setupRelativeFileExpectations(@Injectable final Path parentDirectory, @Injectable final Path pathToUse, @Injectable final File file, final String filePath) throws Exception {
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

        }};
    }

    @Test
    public void testReadExternalRef_RelativeFileFormat_ExceptionThrown(@Injectable final List<AuthorizationValue> auths,
                                                                       @Mocked IOUtils ioUtils,
                                                                       @Mocked Files files,
                                                                       @Injectable final Path parentDirectory,
                                                                       @Injectable final Path pathToUse
    ) throws Exception {
        final String filePath = "file.json";
        File file = tempFolder.newFile(filePath);
        final String expectedResult = "really good json";

        setupRelativeFileExpectations(parentDirectory, pathToUse, file, filePath);

        new StrictExpectations() {{
            IOUtils.toString((FileInputStream) any, UTF_8);
            times = 1;
            result = new IOException();
        }};

        thrown.expect(new BaseMatcher<IOException>() {
            @Override
            public void describeTo(Description description) { }
            @Override
            public boolean matches(Object o) {
                return ((Exception)o).getCause().getClass().equals(IOException.class);
            }
        });
        RefUtils.readExternalRef(filePath, RefFormat.RELATIVE, auths, parentDirectory);
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

    
    @Test 
    public void testPathJoin2() {
        assertEquals(RefUtils.buildUrl("http://foo.bar.com/my/dir/file.yaml", "../newFile.yaml"), "http://foo.bar.com/my/newFile.yaml");
        assertEquals(RefUtils.buildUrl("http://foo.bar.com/my/dir/file.yaml", "../../newFile.yaml"), "http://foo.bar.com/newFile.yaml");
        assertEquals(RefUtils.buildUrl("http://foo.bar.com/my/dir/file.yaml", "./newFile.yaml"), "http://foo.bar.com/my/dir/newFile.yaml");
        assertEquals(RefUtils.buildUrl("http://foo.bar.com/my/dir/file.yaml", "../second/newFile.yaml"), "http://foo.bar.com/my/second/newFile.yaml");
        assertEquals(RefUtils.buildUrl("http://foo.bar.com/my/dir/file.yaml", "../../otherDir/newFile.yaml"), "http://foo.bar.com/otherDir/newFile.yaml");
        assertEquals(RefUtils.buildUrl("http://foo.bar.com/file.yaml", "./newFile.yaml"), "http://foo.bar.com/newFile.yaml");        
        assertEquals(RefUtils.buildUrl("http://foo.bar.com/my/dir/file.yaml", "/newFile.yaml"), "http://foo.bar.com/newFile.yaml");
        assertEquals(RefUtils.buildUrl("http://foo.bar.com/my/dir/file.yaml", "/my/newFile.yaml"), "http://foo.bar.com/my/newFile.yaml");
    }

    @Test
    public void testPathJoinIssue1745() {
        assertEquals(RefUtils.buildUrl("http://foo.bar.com/my/dir/file.yaml", "./second/../newFile.yaml"), "http://foo.bar.com/my/dir/newFile.yaml");
        assertEquals(RefUtils.buildUrl("http://foo.bar.com/my/dir/", "./second/../newFile.yaml"), "http://foo.bar.com/my/dir/newFile.yaml");
        // This is a little strange in the output (beacuse it has not completely eliminated the ..) but is still correct - paste a similar url into a browser and it resolves it correctly.
        assertEquals(RefUtils.buildUrl("http://foo.bar.com/my/dir/file.yaml", "/second/../newFile.yaml"), "http://foo.bar.com/second/../newFile.yaml");
    }

    @Test
    public void shouldReturnEmptyExternalPathForInternalReference() {
        // given
        String ref = "#/components/test";

        // when
        Optional<String> externalPath = RefUtils.getExternalPath(ref);

        // then
        assertThat(externalPath.isPresent(), is(false));
    }

    @Test
    public void shouldReturnEmptyExternalPathForNullReference() {
        // given
        String ref = null;

        // when
        Optional<String> externalPath = RefUtils.getExternalPath(ref);

        // then
        assertThat(externalPath.isPresent(), is(false));
    }

    @Test
    public void shouldReturnEmptyExternalPathForEmptyReference() {
        // given
        String ref = "";

        // when
        Optional<String> externalPath = RefUtils.getExternalPath(ref);

        // then
        assertThat(externalPath.isPresent(), is(false));
    }

    @Test
    public void shouldReturnEmptyExternalPathForInvalidReference() {
        // given
        String ref = "test";

        // when
        Optional<String> externalPath = RefUtils.getExternalPath(ref);

        // then
        assertThat(externalPath.isPresent(), is(false));
    }

    @Test
    public void shouldReturnExternalPathForFileReference() {
        // given
        String ref = "test.yaml#/components/test";

        // when
        Optional<String> externalPath = RefUtils.getExternalPath(ref);

        // then
        assertThat(externalPath.isPresent(), is(true));
        assertThat(externalPath.get(), equalTo("test.yaml"));
    }

    @Test
    public void shouldReturnExternalPathForHttpReference() {
        // given
        String ref = "http://localhost/schema.json#/components/test";

        // when
        Optional<String> externalPath = RefUtils.getExternalPath(ref);

        // then
        assertThat(externalPath.isPresent(), is(true));
        assertThat(externalPath.get(), equalTo("http://localhost/schema.json"));

    }
}
