package io.swagger.parser.util;

import io.swagger.models.auth.AuthorizationValue;
import io.swagger.models.refs.RefFormat;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.net.URI;
import java.net.URISyntaxException;

public class RefUtils {

    public static String computeDefinitionName(String ref) {


        final String[] refParts = ref.split("#/");

        if (refParts.length > 2) {
            throw new RuntimeException("Invalid ref format: " + ref);
        }

        final String file = refParts[0];
        final String definitionPath = refParts.length == 2 ? refParts[1] : null;


        String plausibleName;

        if (definitionPath != null) { //the name will come from the last element of the definition path
            final String[] jsonPathElements = definitionPath.split("/");
            plausibleName = jsonPathElements[jsonPathElements.length - 1];
        } else { //no definition path, so we must come up with a name from the file
            final String[] filePathElements = file.split("/");
            plausibleName = filePathElements[filePathElements.length - 1];

            final String[] split = plausibleName.split("\\.");
            plausibleName = split[0];
        }

        return plausibleName;
    }
    public static boolean isAnExternalRefFormat(RefFormat refFormat) {
        return refFormat == RefFormat.URL || refFormat == RefFormat.RELATIVE;
    }


    public static String readExternalUrlRef(String file, RefFormat refFormat, List<AuthorizationValue> auths,
                                         String rootPath) {

        if (!RefUtils.isAnExternalRefFormat(refFormat)) {
            throw new RuntimeException("Ref is not external");
        }

        String result;

        try {
            if (refFormat == RefFormat.URL) {
                result = RemoteUrl.urlToString(file, auths);
            } else {
                //its assumed to be a relative ref
                String url = buildUrl(rootPath, file);

                return readExternalRef(url, RefFormat.URL, auths, null);
            }
        } catch (Exception e) {
            throw new RuntimeException("Unable to load " + refFormat + " ref: " + file + " path:" + rootPath, e);
        }

        return result;

    }

    public static String buildUrl(String rootPath, String relativePath) throws URISyntaxException {
        return new URI(rootPath).resolve(new URI(relativePath)).toString();
    }



    public static String readExternalRef(String file, RefFormat refFormat, List<AuthorizationValue> auths,
                                         Path parentDirectory) {

        if (!RefUtils.isAnExternalRefFormat(refFormat)) {
            throw new RuntimeException("Ref is not external");
        }

        String result = null;

        try {
            if (refFormat == RefFormat.URL) {
                result = RemoteUrl.urlToString(file, auths);
            } else {
                //its assumed to be a relative file ref
                final Path pathToUse = parentDirectory.resolve(file).normalize();

                if(Files.exists(pathToUse)) {
                    result = IOUtils.toString(new FileInputStream(pathToUse.toFile()), "UTF-8");
                } else {
                    String url = file;
                    if(url.contains("..")) {
                        url = parentDirectory + url.substring(url.indexOf(".") + 2);
                    }else{
                        url = parentDirectory + url.substring(url.indexOf(".") + 1);
                    }
                    final Path pathToUse2 = parentDirectory.resolve(url).normalize();

                    if(Files.exists(pathToUse2)) {
                        result = IOUtils.toString(new FileInputStream(pathToUse2.toFile()), "UTF-8");
                    }
                }
                if (result == null){
                    result = ClasspathHelper.loadFileFromClasspath(file);
                }


            }
        } catch (Exception e) {
            throw new RuntimeException("Unable to load " + refFormat + " ref: " + file + " path: "+parentDirectory, e);
        }

        return result;

    }
}
