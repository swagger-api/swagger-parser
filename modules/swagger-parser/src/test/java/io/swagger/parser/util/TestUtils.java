package io.swagger.parser.util;

import java.io.File;
import java.net.URISyntaxException;

public class TestUtils {

    /**
     * @param resourceLocation The resource location relative to classpath root (ie: starts with '/')
     */
    public static String getResourceAbsolutePath(String resourceLocation) {
        if (!resourceLocation.startsWith("/")) {
            throw new RuntimeException("resourceLocation should be relative to classpath root (ie: starts with '/')");
        }
        try {
            // we use toURI
            return new File(TestUtils.class.getResource(resourceLocation).toURI()).getAbsolutePath();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
