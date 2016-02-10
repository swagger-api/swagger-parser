package io.swagger.parser.util;

import org.testng.annotations.Test;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertEqualsNoOrder;
import static org.testng.Assert.assertTrue;
import static io.swagger.parser.util.PathUtils.*;

public class PathUtilTest {

    @Test
    public void testGetParentDirectoryOfFile() throws Exception {

        final String actualResult = PathUtils.getParentDirectoryOfFile("src/test/resources/parent.json").toString();

        final String expectedResult = Paths.get("src/test/resources").toAbsolutePath().toString();

        assertEquals(actualResult, expectedResult);
    }

    @Test
    public void testDropWhileNoneMatch() {
        final String x              = "abcdefg";
        final String actualResult   = dropWhile(x, 'z', PathUtils.ComparisonOp.NOT_EQUALS);

        assertTrue(actualResult.isEmpty());
    }

    @Test
    public void testDropWhileAllMatch() {
        final String x              = "ffffffff";
        final String actualResult   = dropWhile(x, 'f', PathUtils.ComparisonOp.EQUALS);

        assertTrue(actualResult.isEmpty());
    }

    @Test
    public void testDropWhileFirstFewMatch() {

        final String x              = "AAAbbbccc";
        final String actualResult   = dropWhile(x, 'A', PathUtils.ComparisonOp.EQUALS);
        final String expectedResult = "bbbccc";

        assertEquals(actualResult, expectedResult);
    }

    @Test
    public void testDropWhilePath() {

        final String x              = "http://foo.bar.com/{id}";
        final String actualResult   = dropWhile(x, '{', PathUtils.ComparisonOp.NOT_EQUALS);
        final String expectedResult = "{id}";

        assertEquals(actualResult, expectedResult);
    }

    @Test
    public void testTakeWhilePath() {

        final String x              = "{id}";
        final String actualResult   = takeWhile(x, '}', PathUtils.ComparisonOp.NOT_EQUALS);
        final String expectedResult = "{id";

        assertEquals(actualResult, expectedResult);
    }

    @Test
    public void testDropWhileEmptyInputEquals() {

        final String x              = "";
        final String actualResult   = dropWhile(x, 'Z', PathUtils.ComparisonOp.EQUALS);

        assertTrue(actualResult.isEmpty());
    }

    @Test
    public void testDropWhileEmptyInputNotEquals() {

        final String x              = "";
        final String actualResult   = dropWhile(x, 'Z', PathUtils.ComparisonOp.NOT_EQUALS);

        assertTrue(actualResult.isEmpty());
    }

    @Test
    public void testTakeWhileTakeAllValuesEquals() {

        final String x            = "zzzzzz";
        final String actualResult = takeWhile(x, 'z', PathUtils.ComparisonOp.EQUALS);

        assertEquals(actualResult, x);
    }

    @Test
    public void testTakeWhileTakeAllValuesNotEquals() {

        final String x            = "zzzzzz";
        final String actualResult = takeWhile(x, 'z', PathUtils.ComparisonOp.NOT_EQUALS);

        assertTrue(actualResult.isEmpty());
    }


    @Test
    public void testTakeWhileTakeNoValuesCaseSensitiveEquals() {

        final String x            = "KAJSDLFKJASDKLFJ";
        final String actualResult = takeWhile(x, 'k', PathUtils.ComparisonOp.EQUALS);

        assertTrue(actualResult.isEmpty());
    }

    @Test
    public void testTakeWhileTakeNoValuesCaseSensitiveNotEquals() {

        final String x            = "KAJSDLFKJASDKLFJ";
        final String actualResult = takeWhile(x, 'k', PathUtils.ComparisonOp.NOT_EQUALS);

        assertEquals(x, actualResult);
    }

    @Test
    public void testTakeWhileTakeHeadCaseSensitive() {

        final String x              = "JjJjJ";
        final String actualResult   = takeWhile(x, 'J', PathUtils.ComparisonOp.EQUALS);
        final String expectedResult = "J";

        assertEquals(actualResult, expectedResult);
    }

    @Test
    public void testBreak() {
        final String x = "123";
        final char breakAt = '2';
        final List<String> expectedResult = new ArrayList<>();
        expectedResult.add("1");
        expectedResult.add("23");
        final List<String> actualResult = breakFn(x, breakAt);

        assertEquals(actualResult, expectedResult);
    }

    @Test
    public void testBreakOnPathParameter() {
        final String x = "http://foo.bar.com/{id}";
        final char breakAt = '{';
        final List<String> expectedResult = new ArrayList<>();
        expectedResult.add("http://foo.bar.com/");
        expectedResult.add("{id}");
        final List<String> actualResult = breakFn(x, breakAt);

        assertEquals(actualResult, expectedResult);
    }

    @Test
    public void testBreakOnNone() {
        final String x = "123";
        final char breakAt = '9';
        final List<String> expectedResult = new ArrayList<>();
        expectedResult.add("123");
        expectedResult.add("");
        final List<String> actualResult = breakFn(x, breakAt);

        assertEquals(actualResult, expectedResult);
    }

    @Test
    public void testBreakOnAll() {
        final String x = "123";
        final char breakAt = '1';
        final List<String> expectedResult = new ArrayList<>();
        expectedResult.add("");
        expectedResult.add("123");
        final List<String> actualResult = breakFn(x, breakAt);

        assertEquals(actualResult, expectedResult);
    }

    @Test
    public void testBreakOnWhitespaceWithNoWhitespaceMarker() {
        final String x = "           ";
        final char breakAt = '3';
        final List<String> expectedResult = new ArrayList<>();
        expectedResult.add(x);
        expectedResult.add("");
        final List<String> actualResult = breakFn(x, breakAt);

        assertEquals(actualResult, expectedResult);
    }

    @Test
    public void testBreakOnWhitespace() {
        final String x = "           ";
        final char breakAt = ' ';
        final List<String> expectedResult = new ArrayList<>();
        expectedResult.add("");
        expectedResult.add("           ");
        final List<String> actualResult = breakFn(x, breakAt);

        assertEquals(actualResult, expectedResult);
    }

    @Test
    public void testDrop0() {
        final String x            = "foobar";
        final String actualResult = drop(x, 0);
        assertEquals(actualResult, x);
    }

    @Test
    public void testDropNegative() {
        final String x            = "foobar";
        final String actualResult = drop(x, -555);
        assertEquals(actualResult, x);
    }

    @Test
    public void testDrop1() {
        final String x              = "foobar";
        final String actualResult   = drop(x, 1);
        final String expectedResult = "oobar";
        assertEquals(actualResult, expectedResult);
    }

    @Test
    public void testDrop3() {
        final String x              = "foobar";
        final String actualResult   = drop(x, 3);
        final String expectedResult = "bar";
        assertEquals(actualResult, expectedResult);
    }

    @Test
    public void testDropAll() {
        final String x              = "foobar";
        final String actualResult   = drop(x, x.length());
        assertTrue(actualResult.isEmpty());
    }

    @Test
    public void testDropMoreThanLengthX() {
        final String x              = "foobar";
        final String actualResult   = drop(x, 999);
        assertTrue(actualResult.isEmpty());
    }
}
