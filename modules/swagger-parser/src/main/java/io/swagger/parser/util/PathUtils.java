package io.swagger.parser.util;

import java.nio.file.Path;
import java.nio.file.Paths;

public class PathUtils {

    public static Path getParentDirectoryOfFile(String fileStr) {
        Path file = Paths.get(fileStr).toAbsolutePath();
        return file.toAbsolutePath().getParent();
    }
}
