package io.swagger.v3.parser.util;

import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PathUtils {

    // TODO use properly java URL to idenfiy and get absolute URL
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
            URL url = PathUtils.class.getResource(location);
            if (url == null){
                url = PathUtils.class.getClassLoader().getResource(location);
            }
            if(url == null) {
                url = ClassLoader.getSystemResource(location);
            }

            Path file = Paths.get((URI.create(url.toExternalForm())));
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
            URL url = PathUtils.class.getResource(location);
            if (url == null){
                url = PathUtils.class.getClassLoader().getResource(location);
            }
            if(url == null) {
                url = ClassLoader.getSystemResource(location);
            }

            Path file = Paths.get((URI.create(url.toExternalForm())));
            return file.toAbsolutePath().toUri().toString();

        } catch (Exception e) {
            return location;
        }
    }
}
