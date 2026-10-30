package io.swagger.v3.parser.util;


import io.swagger.v3.parser.core.models.AuthorizationValue;
import io.swagger.v3.parser.models.RefFormat;
import io.swagger.v3.parser.processors.ExternalRefProcessor;
import io.swagger.v3.parser.urlresolver.PermittedUrlsChecker;
import org.apache.commons.io.IOUtils;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.MockedStatic;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public class RefUtilsTest {

    List<AuthorizationValue> auths;

    AuthorizationValue auth1 = mock(AuthorizationValue.class);
    AuthorizationValue auth2 = mock(AuthorizationValue.class);

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


    private MockedStatic<RemoteUrl> remoteUrl;
    private MockedStatic<IOUtils> ioUtils;
    private MockedStatic<Files> files;
    private MockedStatic<ClasspathHelper> classpathHelper;

    @Before
    public void setUpStaticMocks() {
        remoteUrl = mockStatic(RemoteUrl.class);
        ioUtils = mockStatic(IOUtils.class);
        files = mockStatic(Files.class);
        classpathHelper = mockStatic(ClasspathHelper.class);
    }

    @After
    public void closeStaticMocks() {
        classpathHelper.close();
        files.close();
        ioUtils.close();
        remoteUrl.close();
    }

    @Test
    public void testReadExternalRef_UrlFormat() throws Exception {

        final String url = "http://my.company.com/path/to/file.json";
        final String expectedResult = "really good json";

        remoteUrl.when(() -> RemoteUrl.urlToString(url, auths, permittedUrlsChecker)).thenReturn(expectedResult);

        String actualResult = RefUtils.readExternalRef(url, RefFormat.URL, auths, null, permittedUrlsChecker);
        assertEquals(actualResult, expectedResult);
        remoteUrl.verify(() -> RemoteUrl.urlToString(url, auths, permittedUrlsChecker), times(1));
    }

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Test
    public void testReadExternalRef_UrlFormat_ExceptionThrown() throws Exception {

        final String url = "http://my.company.com/path/to/file.json";

        remoteUrl.when(() -> RemoteUrl.urlToString(url, auths, permittedUrlsChecker)).thenThrow(new Exception());
        thrown.expect(RuntimeException.class);
        try {
            RefUtils.readExternalRef(url, RefFormat.URL, auths, null, permittedUrlsChecker);
        } finally {
            remoteUrl.verify(() -> RemoteUrl.urlToString(url, auths, permittedUrlsChecker), times(1));
        }
    }

    Path parentDirectory = mock(Path.class);
    Path pathToUse = mock(Path.class);

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

        ioUtils.when(() -> IOUtils.toString(any(InputStream.class), eq(UTF_8))).thenReturn(expectedResult);

        String actualResult = RefUtils.readExternalRef(filePath, RefFormat.RELATIVE, auths, parentDirectory, permittedUrlsChecker);
        assertEquals(actualResult, expectedResult);
        ioUtils.verify(() -> IOUtils.toString(any(InputStream.class), eq(UTF_8)), times(1));
        verifyRelativeFileLookup();

    }

    File file = mock(File.class);
    String filePath = "file.json";

    private void setupRelativeFileExpectations(final Path parentDirectory, final Path pathToUse, final File file, final String filePath) throws Exception {
        Path resolved = mock(Path.class);
        when(parentDirectory.resolve(filePath)).thenReturn(resolved);
        when(resolved.normalize()).thenReturn(pathToUse);
        files.when(() -> Files.exists(pathToUse)).thenReturn(true);
        files.when(() -> Files.newInputStream(pathToUse)).thenReturn(mock(InputStream.class));
    }

    private void verifyRelativeFileLookup() {
        files.verify(() -> Files.exists(pathToUse), times(1));
    }


    @Test
    public void testReadExternalRef_RelativeFileFormat_ExceptionThrown() throws Exception {
        final String filePath = "file.json";
        File file = tempFile(filePath);

        setupRelativeFileExpectations(parentDirectory, pathToUse, file, filePath);

        ioUtils.when(() -> IOUtils.toString(any(InputStream.class), eq(UTF_8))).thenThrow(new IOException());

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

    Path pathToUse2 = mock(Path.class);

    Path parentDirectory2 = mock(Path.class);

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
