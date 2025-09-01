package io.swagger.v3.parser.util;


import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.core.models.AuthorizationValue;
import io.swagger.v3.parser.models.RefFormat;
import io.swagger.v3.parser.processors.ExternalRefProcessor;
import io.swagger.v3.parser.urlresolver.PermittedUrlsChecker;
import mockit.Injectable;
import mockit.Mocked;
import mockit.Expectations;
import org.apache.commons.io.IOUtils;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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

    List<AuthorizationValue> auths;

    @Injectable
    AuthorizationValue auth1;
    @Injectable
    AuthorizationValue auth2;

    private PermittedUrlsChecker permittedUrlsChecker = new PermittedUrlsChecker();

    public RefUtilsTest() {
        List<AuthorizationValue> auths = new ArrayList<>();
        auths.add(auth1);
        auths.add(auth2);
    }
    @Test
    public void testComputeDefinitionName() {
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
    public void testIsAnExternalRefFormat() {
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


    @Mocked
    RemoteUrl remoteUrl;

    @Test
    public void testReadExternalRef_UrlFormat() throws Exception {

        final String url = "http://my.company.com/path/to/file.json";
        final String expectedResult = "really good json";

        new Expectations() {{
            RemoteUrl.urlToString(url, auths, permittedUrlsChecker);
            times = 1;
            result = expectedResult;
        }};

        String actualResult = RefUtils.readExternalRef(url, RefFormat.URL, auths, null, permittedUrlsChecker);
        assertEquals(actualResult, expectedResult);
    }

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Test
    public void testReadExternalRef_UrlFormat_ExceptionThrown() throws Exception {

        final String url = "http://my.company.com/path/to/file.json";

        new Expectations() {{
            RemoteUrl.urlToString(url, auths, permittedUrlsChecker);
            times = 1;
            result = new Exception();
        }};
        thrown.expect(RuntimeException.class);
        RefUtils.readExternalRef(url, RefFormat.URL, auths, null, permittedUrlsChecker);
    }

    @Mocked IOUtils ioUtils;
    @Mocked Files files;
    @Injectable Path parentDirectory;
    @Injectable Path pathToUse;

    public File tempFile(String name) throws Exception{
        String tmpDir = System.getProperty("java.io.tmpdir");
        File f = new File(tmpDir + File.separator + name);
        f.createNewFile();
        return f;
    }

    @Test
    public void testReadExternalRef_RelativeFileFormat(
    ) throws Exception {

        File file = tempFile(filePath);
        final String expectedResult = "really good json";

        setupRelativeFileExpectations(parentDirectory, pathToUse, file, filePath);

        new Expectations() {{
            IOUtils.toString((FileInputStream) any, UTF_8);
            times = 1;
            result = expectedResult;

        }};

        String actualResult = RefUtils.readExternalRef(filePath, RefFormat.RELATIVE, auths, parentDirectory, permittedUrlsChecker);
        assertEquals(actualResult, expectedResult);

    }

    @Injectable File file;
    @Injectable String filePath;

    private void setupRelativeFileExpectations(@Injectable final Path parentDirectory, @Injectable final Path pathToUse, @Injectable final File file, final String filePath) throws Exception {
        new Expectations() {{

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
    public void testReadExternalRef_RelativeFileFormat_ExceptionThrown() throws Exception {
        final String filePath = "file.json";
        File file = tempFile(filePath);

        setupRelativeFileExpectations(parentDirectory, pathToUse, file, filePath);

        new Expectations() {{
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
        RefUtils.readExternalRef(filePath, RefFormat.RELATIVE, auths, parentDirectory, permittedUrlsChecker);
    }

    @Test
    public void testReadExternalRef_InternalFormat() {

        final String file = "#/defintiions/foo";

        try {
            RefUtils.readExternalRef(file, RefFormat.INTERNAL, auths, null, permittedUrlsChecker);
            fail("Should have thrown an exception");
        } catch (RuntimeException e) {

        }
    }

    @Mocked ClasspathHelper classpathHelper;
    @Injectable Path pathToUse2;

    @Injectable Path parentDirectory2;

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
        String ref = "#/components/test";

        Optional<String> externalPath = RefUtils.getExternalPath(ref);

        assertFalse(externalPath.isPresent());
    }

    @Test
    public void shouldReturnEmptyExternalPathForNullReference() {
        String ref = null;

        Optional<String> externalPath = RefUtils.getExternalPath(ref);

        assertFalse(externalPath.isPresent());
    }

    @Test
    public void shouldReturnEmptyExternalPathForEmptyReference() {
        String ref = "";

        Optional<String> externalPath = RefUtils.getExternalPath(ref);

        assertFalse(externalPath.isPresent());
    }

    @Test
    public void shouldReturnEmptyExternalPathForInvalidReference() {
        String ref = "test";

        Optional<String> externalPath = RefUtils.getExternalPath(ref);

        assertFalse(externalPath.isPresent());
    }

    @Test
    public void shouldReturnExternalPathForFileReference() {
        // given
        String ref = "test.yaml#/components/test";

        // when
        Optional<String> externalPath = RefUtils.getExternalPath(ref);

        // then
        assertTrue(externalPath.isPresent());
        assertEquals(externalPath.get(), "test.yaml");
    }

    @Test
    public void shouldReturnExternalPathForHttpReference() {
        // given
        String ref = "http://localhost/schema.json#/components/test";

        // when
        Optional<String> externalPath = RefUtils.getExternalPath(ref);

        // then
        assertTrue(externalPath.isPresent());
        assertEquals(externalPath.get(), "http://localhost/schema.json");

    }
}
