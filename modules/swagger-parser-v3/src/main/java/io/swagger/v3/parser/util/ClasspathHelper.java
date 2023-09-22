package io.swagger.v3.parser.util;


import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;

public class ClasspathHelper {

    public static String loadFileFromClasspath(String location) {
        String file = FilenameUtils.separatorsToUnix(location);

        InputStream inputStream = ClasspathHelper.class.getResourceAsStream(file);
        
        if(inputStream == null) {
            inputStream = ClasspathHelper.class.getClassLoader().getResourceAsStream(file);
        }

        if(inputStream == null) {
            inputStream = ClassLoader.getSystemResourceAsStream(file);
        }

        if(inputStream == null) {
            inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(file);
        }

        if(inputStream != null) {
            try {
                return IOUtils.toString(inputStream);
            } catch (IOException e) {
                throw new RuntimeException("Could not read " + file + " from the classpath", e);
            }
        }

        throw new RuntimeException("Could not find " + file + " on the classpath");
    }
}
