package io.swagger.parser.util;

import com.jayway.jsonpath.JsonPath;
import io.swagger.models.Model;
import io.swagger.models.ModelImpl;
import io.swagger.models.auth.AuthorizationValue;
import io.swagger.models.refs.RefFormat;
import io.swagger.parser.JsonToYamlFileDuplicator;
import mockit.Injectable;
import mockit.Mocked;
import mockit.StrictExpectations;
import mockit.integration.junit4.JMockit;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

@RunWith(JMockit.class)
public class RefUtilsTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

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
        final String expectedResult = "{}";

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
        final String expectedResult = "{}";

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
                                                @Injectable final Path pathToUse) throws Exception {
        final String filePath = "./path/to/file.json";
        final String expectedResult = "{}";

        new StrictExpectations() {{

            parentDirectory.resolve(filePath).normalize();
            times = 1;
            result = pathToUse;

            Files.exists(pathToUse);
            times = 1;
            result = false;

            ClasspathHelper.loadFileFromClasspath(filePath); times=1; result=expectedResult;

        }};

        final String actualResult = RefUtils.readExternalRef(filePath, RefFormat.RELATIVE, null, parentDirectory);

        assertEquals(actualResult, expectedResult);

    }


    @Test
    public void testReadExternalRef_AppendPathToLocalRefs_Yaml(@Mocked Files files,
            @Mocked ClasspathHelper classpathHelper,
            @Injectable final Path parentDirectory,
            @Injectable final Path pathToUse) throws Exception {
        final String filePath = "./path/to/file.yaml";
        File tmpFolder = temporaryFolder.newFolder("testReadExternalRef_AppendPathToLocalRefs_Json");
        JsonToYamlFileDuplicator.duplicateFilesInYamlFormat("src/test/resources/relative-file-references/json",
                tmpFolder.getAbsolutePath());
        final String fileContent = FileUtils.readFileToString(Paths.get(tmpFolder.toURI()).resolve("models").resolve("localrefence.yaml").toFile());
        testReadExternalRef_AppendPathToLocalRefs(files,classpathHelper,parentDirectory,pathToUse,filePath,fileContent);
    }

    @Test
    public void testReadExternalRef_AppendPathToLocalRefs_Json(@Mocked Files files,
            @Mocked ClasspathHelper classpathHelper,
            @Injectable final Path parentDirectory,
            @Injectable final Path pathToUse) throws Exception {
        final String filePath = "./path/to/file.json";
        final String fileContent = IOUtils.toString(getClass().getResource("/relative-file-references/json/models/localrefence.json"));
        testReadExternalRef_AppendPathToLocalRefs(files,classpathHelper,parentDirectory,pathToUse,filePath,fileContent);
    }

    private void testReadExternalRef_AppendPathToLocalRefs(Files files,
            ClasspathHelper classpathHelper,
            final Path parentDirectory,
            final Path pathToUse,
            final String filePath, final String fileContent ) {
        new StrictExpectations() {{

            parentDirectory.resolve(filePath).normalize();
            times = 1;
            result = pathToUse;

            Files.exists(pathToUse);
            times = 1;
            result = false;

            ClasspathHelper.loadFileFromClasspath(filePath); times=1; result=fileContent;

        }};

        final String actualResult = RefUtils.readExternalRef(filePath, RefFormat.RELATIVE, null, parentDirectory);

        assertEquals(JsonPath.read(actualResult,"$.localArray.items.['$ref']"),filePath+"#/referencedByLocalArray");
        assertEquals(JsonPath.read(actualResult,"$.localObject.properties.hello1.['$ref']"),filePath+"#/referencedByLocalElement");
        assertEquals(JsonPath.read(actualResult,"$.localObject.properties.shareprefix.['$ref']"),filePath+"#/referencedBy");

    }
}
