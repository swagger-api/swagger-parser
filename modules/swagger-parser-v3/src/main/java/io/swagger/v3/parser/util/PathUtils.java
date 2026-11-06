package io.swagger.v3.parser.util;

import org.apache.commons.lang3.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PathUtils {

    final static String SCHEME_FILE = "file:";
    final static String SCHEME_HTTP = "http:";
    final static String SCHEME_HTTPS = "https:";

    public static Path getParentDirectoryOfFile(String location) {
        Path file = null;
        try {
            location = location.replaceAll("\\\\", "/");

            if (location.toLowerCase().startsWith(SCHEME_FILE)) {
                file = Paths.get(URI.create(location)).toAbsolutePath();
            } else {
                file = Paths.get(location).toAbsolutePath();
            }
            if (!Files.exists(file)) {
                return getParentDirectoryFromUrl(location);
            }

        } catch (Exception e) {
            e.getMessage();
        }

        return file.toAbsolutePath().getParent();
    }

    private static Path getParentDirectoryFromUrl(String location){
        try {
            Path file = getPathFromClasspath(location);
            return file.getParent();

        } catch (Exception e) {
            e.getMessage();
            return null;
        }
    }

    public static String getAbsoluteUrl(String location) {
        Path file = null;
        try {
            location = location.replaceAll("\\\\","/");
            if (location.toLowerCase().startsWith(SCHEME_HTTP) || location.toLowerCase().startsWith(SCHEME_HTTPS)) {
                return location;
            }

            if (location.toLowerCase().startsWith(SCHEME_FILE)) {
                file = Paths.get(URI.create(location)).toAbsolutePath();
            } else {
                file = Paths.get(location).toAbsolutePath();
            }
            if (!Files.exists(file)) {
                return getClasspathUrl(location);
            }

        } catch (Exception e) {
            if (file == null) return location;
        }

        return file.toAbsolutePath().toUri().toString();
    }

    private static String getClasspathUrl(String location){
        try {
            Path file = getPathFromClasspath(location);
            return file.toAbsolutePath().toUri().toString();

        } catch (Exception e) {
            return location;
        }
    }

    private static Path getPathFromClasspath(String location) {
        URL url = PathUtils.class.getResource(location);
        if (url == null) {
            url = PathUtils.class.getClassLoader().getResource(location);
        }
        if (url == null) {
            url = ClassLoader.getSystemResource(location);
        }
        return Paths.get(URI.create(url.toExternalForm()));
    }

    public static URI rootDocumentUri(String rootPath) {
        if (rootPath == null || rootPath.isEmpty()) {
            return null;
        }

        // Windows absolute path: C:/... or C:\... — must be classified before URI parsing
        // because new URI("C:/...") treats the drive letter as a URI scheme.
        if (rootPath.length() >= 3
                && Character.isLetter(rootPath.charAt(0))
                && rootPath.charAt(1) == ':'
                && (rootPath.charAt(2) == '/' || rootPath.charAt(2) == '\\')) {
            return existingFileUri(rootPath);
        }

        try {
            URI rootUri = new URI(rootPath);
            if (rootUri.getScheme() != null) {
                return normalizeRootDocumentUri(rootUri);
            }
        } catch (URISyntaxException ignored) {
            // It can still be an ordinary filesystem path, for example one containing spaces.
        }

        return existingFileUri(rootPath);
    }

    private static URI normalizeRootDocumentUri(URI rootUri) {
        boolean http = isHttpUri(rootUri);
        boolean file = "file".equalsIgnoreCase(rootUri.getScheme());
        if ((!http && !file)
                || rootUri.isOpaque()
                || (http && StringUtils.isBlank(rootUri.getRawAuthority()))) {
            return null;
        }
        return withoutFragment(rootUri.normalize());
    }

    private static URI existingFileUri(String location) {
        try {
            Path file = Paths.get(location);
            if (Files.isRegularFile(file)) {
                return file.toAbsolutePath().normalize().toUri();
            }
        } catch (InvalidPathException | SecurityException ignored) {
            // The root cannot be identified safely as a filesystem document.
        }
        return null;
    }

    public static Path parentDirectoryOfUri(URI fileUri) {
        try {
            URI pathOnly = new URI(fileUri.getScheme(), fileUri.getAuthority(),
                    fileUri.getPath(), null, null);
            return Paths.get(pathOnly).toAbsolutePath().getParent();
        } catch (IllegalArgumentException | URISyntaxException ignored) {
            return null;
        }
    }

    public static boolean isHttpUri(URI uri) {
        return uri != null && ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()));
    }

    public static URI withoutFragment(URI uri) {
        if (uri == null || uri.isOpaque()) {
            return null;
        }
        if (uri.getFragment() == null) {
            return uri;
        }
        try {
            String value = uri.toString();
            return new URI(value.substring(0, value.indexOf('#')));
        } catch (URISyntaxException | IllegalArgumentException ignored) {
            return null;
        }
    }
}
