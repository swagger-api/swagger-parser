package io.swagger.parser.util;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class PathUtils {

    public static Path getParentDirectoryOfFile(String fileStr) {
        final String fileScheme = "file://";
        Path file;
        fileStr = fileStr.replaceAll("\\\\","/");
        if (fileStr.toLowerCase().startsWith(fileScheme)) {
            file = Paths.get(URI.create(fileStr)).toAbsolutePath();
        } else {
            file = Paths.get(fileStr).toAbsolutePath();
        }
        return file.toAbsolutePath().getParent();
    }

    public enum ComparisonOp {
        EQUALS, NOT_EQUALS
    }

    // breakFn :: (a -> Bool) -> [a] -> ([a], [a])
    // λ: break (== 2) [1,2,3]
    // ([1],[2,3])
    // λ: break (== 5) [1,2,3]
    // ([1,2,3],[])
    public static List<String> breakFn(final String xs, final char matcher) {
        final List<String> result = new ArrayList<>();
        final String first = takeWhile(xs, matcher, ComparisonOp.NOT_EQUALS);
        final int firstLen = first.length();
        final String rest;
        if(firstLen == xs.length()) {
            rest = "";
        }
        else {
            rest = xs.substring(firstLen);
        }
        result.add(first);
        result.add(rest);
        return result;
    }

    public static String drop(final String x, final int n) {
        final String result;

        if(x.isEmpty() || n <= 0) {
            result = x;
        }
        else {
            final String tail = x.substring(1);
            result = drop(tail, n-1);
        }

        return result;
    }

    public static String dropWhile(final String x, final char matcher, final ComparisonOp op) {
        final String result;

        if(x.isEmpty()) {
            result = "";
        }
        else if(!x.isEmpty() && compare(x.charAt(0), matcher, op)) {
            final String tail = x.substring(1);
            result = dropWhile(tail, matcher, op); // drop head
        }
        else {
            // x matches the matcher - no more dropping
            result = x;
        }
        return result;
    }


    public static String takeWhile(final String x, final char matcher, final ComparisonOp op) {
        return takeWhileHelper(x, matcher, op, "");
    }

    private static <T> boolean compare(final T x, final T y, final ComparisonOp op) {
        if (op == ComparisonOp.EQUALS) {
            return x.equals(y);
        }
        else {
            return !(x.equals(y));
        }
    }

    private static String takeWhileHelper(final String x, final char matcher, final ComparisonOp op, final String acc) {
        final String result;

        if(x.isEmpty()) {
            result = acc;
        }
        else {
            final char head = x.charAt(0);
            final String tail = x.substring(1);

            if(compare(head, matcher, op)) {
                result = takeWhileHelper(tail, matcher, op, acc + head);
            }
            else {
                // x does not match the matcher - no more taking.
                result = acc;
            }
        }
        return result;
    }
}
