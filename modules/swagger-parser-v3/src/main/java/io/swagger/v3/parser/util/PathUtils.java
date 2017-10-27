package io.swagger.v3.parser.util;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PathUtils {


    public static Path getParentDirectoryOfFile(String fileStr) {
        final String fileScheme = "file:";
        Path file;
        fileStr = fileStr.replaceAll("\\\\","/");
        if (fileStr.toLowerCase().startsWith(fileScheme)) {
            file = Paths.get(URI.create(fileStr)).toAbsolutePath();
        } else {
            file = Paths.get(fileStr).toAbsolutePath();
        }
        return file.toAbsolutePath().getParent();
    }
}