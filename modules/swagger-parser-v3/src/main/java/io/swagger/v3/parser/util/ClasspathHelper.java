package io.swagger.v3.parser.util;


import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;

public class ClasspathHelper {

    public static String loadFileFromClasspath(String location) {
       
        InputStream inputStream = ClasspathHelper.class.getResourceAsStream(location);

        if(inputStream == null) {
            inputStream = ClasspathHelper.class.getClassLoader().getResourceAsStream(location);
        }

        if(inputStream == null) {
            inputStream = ClassLoader.getSystemResourceAsStream(location);
        }

        if(inputStream != null) {
            try {
                return IOUtils.toString(inputStream);
            } catch (IOException e) {
                throw new RuntimeException("Could not read " + location + " from the classpath", e);
            }
        }

        return null;

    }

    public static String loadFileFromClasspath(String location, String parentDirectory) {

        location = parentDirectory + location.substring(location.indexOf(".")+1);

        InputStream inputStream = ClasspathHelper.class.getResourceAsStream(location);

        if(inputStream == null) {
            inputStream = ClasspathHelper.class.getClassLoader().getResourceAsStream(location);
        }

        if(inputStream == null) {
            inputStream = ClassLoader.getSystemResourceAsStream(location);
        }




        if(inputStream != null) {
            try {
                return IOUtils.toString(inputStream);
            } catch (IOException e) {
                throw new RuntimeException("Could not read " + location + " from the classpath", e);
            }
        }





        throw new RuntimeException("Could not find " + location + " on the classpath");
    }
}
