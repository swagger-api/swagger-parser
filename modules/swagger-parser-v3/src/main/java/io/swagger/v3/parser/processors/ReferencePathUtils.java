package io.swagger.v3.parser.processors;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayDeque;
import java.util.Deque;

final class ReferencePathUtils {

    private ReferencePathUtils() {
    }

    static boolean isAbsolute(String ref) {
        if (ref == null) {
            return false;
        }
        if (isAbsoluteFilePath(ref)) {
            return true;
        }
        try {
            return new URI(ref).isAbsolute();
        } catch (URISyntaxException e) {
            return hasScheme(ref);
        }
    }

    static String resolve(String source, String ref) {
        if (source == null || ref == null) {
            return source;
        }
        try {
            URI sourceUri = new URI(source);
            if (!source.endsWith("/") && ref.startsWith("./") && "".equals(sourceUri.getPath())) {
                sourceUri = new URI(source + "/");
            } else if ("".equals(sourceUri.getPath()) && !ref.startsWith("/")) {
                sourceUri = new URI(source + "/");
            }

            URI resolved = sourceUri.resolve(new URI(ref)).normalize();
            String resolvedRef = resolved.toString();
            if (source.startsWith("./") && !resolved.isAbsolute() &&
                    !resolvedRef.startsWith(".") && !resolvedRef.startsWith("/")) {
                return "./" + resolvedRef;
            }
            return resolvedRef;
        } catch (URISyntaxException | IllegalArgumentException e) {
            return resolveRawPath(source, ref);
        }
    }

    private static String resolveRawPath(String source, String ref) {
        if (ref.isEmpty()) {
            return stripFragment(source);
        }
        if (isAbsolute(ref)) {
            return ref;
        }
        if (ref.startsWith("#")) {
            return stripFragment(source) + ref;
        }
        if (ref.startsWith("?")) {
            return stripQueryAndFragment(source) + ref;
        }

        String sourceFile = stripQueryAndFragment(source);
        int lastSeparator = Math.max(sourceFile.lastIndexOf('/'), sourceFile.lastIndexOf('\\'));
        if (lastSeparator == -1) {
            return normalizeRelativePath(ref);
        }

        String resolved = sourceFile.substring(0, lastSeparator + 1) + ref;
        if (hasScheme(sourceFile)) {
            return resolved;
        }
        return normalizeRelativePath(resolved);
    }

    private static String normalizeRelativePath(String ref) {
        int suffixStart = suffixStart(ref);
        String path = suffixStart == -1 ? ref : ref.substring(0, suffixStart);
        String suffix = suffixStart == -1 ? "" : ref.substring(suffixStart);
        boolean leadingDotSlash = path.startsWith("./");
        boolean leadingSlash = path.startsWith("/");
        Deque<String> segments = new ArrayDeque<>();

        for (String segment : path.split("/")) {
            if (segment.isEmpty() || ".".equals(segment)) {
                continue;
            }
            if ("..".equals(segment)) {
                if (!segments.isEmpty() && !"..".equals(segments.peekLast())) {
                    segments.removeLast();
                } else if (!leadingSlash) {
                    segments.addLast(segment);
                }
            } else {
                segments.addLast(segment);
            }
        }

        String normalized = String.join("/", segments);
        if (leadingSlash) {
            normalized = "/" + normalized;
        } else if (leadingDotSlash && !normalized.startsWith("..") && !normalized.isEmpty()) {
            normalized = "./" + normalized;
        }
        return normalized + suffix;
    }

    private static boolean isAbsoluteFilePath(String ref) {
        return ref.startsWith("/") || ref.startsWith("\\") ||
                (ref.length() >= 3 && Character.isLetter(ref.charAt(0)) && ref.charAt(1) == ':' &&
                        (ref.charAt(2) == '/' || ref.charAt(2) == '\\'));
    }

    private static boolean hasScheme(String ref) {
        int colon = ref.indexOf(':');
        if (colon <= 0 || !Character.isLetter(ref.charAt(0))) {
            return false;
        }
        for (int i = 1; i < colon; i++) {
            char character = ref.charAt(i);
            if (!Character.isLetterOrDigit(character) && character != '+' && character != '-' && character != '.') {
                return false;
            }
        }
        return true;
    }

    private static String stripFragment(String value) {
        int fragment = value.indexOf('#');
        return fragment == -1 ? value : value.substring(0, fragment);
    }

    private static String stripQueryAndFragment(String value) {
        int suffixStart = suffixStart(value);
        return suffixStart == -1 ? value : value.substring(0, suffixStart);
    }

    private static int suffixStart(String value) {
        int query = value.indexOf('?');
        int fragment = value.indexOf('#');
        if (query == -1) {
            return fragment;
        }
        if (fragment == -1) {
            return query;
        }
        return Math.min(query, fragment);
    }
}
