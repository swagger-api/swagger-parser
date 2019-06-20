package io.swagger.parser.util;

import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PathUtils {

    public static Path getParentDirectoryOfFile(String location) {
        Path file = null;
        try {
            location = location.replaceAll("\\\\","/");
            final String fileScheme = "file:";
            if (location.toLowerCase().startsWith(fileScheme)) {
                file = Paths.get(URI.create(location)).toAbsolutePath();
            } else {
                file = Paths.get(location).toAbsolutePath();
            }
            if (!Files.exists(file)) {
                URL url = PathUtils.class.getClassLoader().getResource(location);
                file = Paths.get((URI.create(url.toExternalForm())));
                return file.getParent();
            }



        } catch (Exception e) {
            e.getMessage();
        }

        return file.toAbsolutePath().getParent();
    }
}
