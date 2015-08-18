package io.swagger.parser.util;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;


public class ClasspathHelper {

    public static String loadFileFromClasspath(String location) {

        InputStream inputStream = ClasspathHelper.class.getResourceAsStream(location);

        if(inputStream == null) {
            inputStream = ClassLoader.getSystemResourceAsStream(location);
        }

        if(inputStream != null) {
            try {
                final String result = IOUtils.toString(inputStream);
                return result;
            } catch (IOException e) {
                throw new RuntimeException("Could not read " + location + " from the classpath", e);
            }
        }

        throw new RuntimeException("Could not find " + location + " on the classpath");
    }
}
