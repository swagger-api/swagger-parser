package io.swagger.v3.parser.util;

import io.swagger.v3.parser.core.models.AuthorizationValue;
import io.swagger.v3.parser.models.RefFormat;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;



import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

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

    public static RefFormat computeRefFormat(String ref) {
        RefFormat result = RefFormat.INTERNAL;
        ref = mungedRef(ref);
        if(ref.startsWith("http")||ref.startsWith("https")) {
            result = RefFormat.URL;
        } else if(ref.startsWith("#/")) {
            result = RefFormat.INTERNAL;
        } else if(ref.startsWith(".") || ref.startsWith("/") || ref.indexOf("#/") > 0) {
            result = RefFormat.RELATIVE;
        }

        return result;
    }

    public static String mungedRef(String refString) {
        // Ref: IETF RFC 3966, Section 5.2.2
        if (!refString.contains(":") &&   // No scheme
                !refString.startsWith("#") && // Path is not empty
                !refString.startsWith("/") && // Path is not absolute
                !refString.contains("$") &&
                refString.indexOf(".") > 0) { // Path does not start with dot but contains "." (file extension)
            return "./" + refString;
        }
        return refString;
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
            throw new RuntimeException("Unable to load " + refFormat + " ref: " + file, e);
        }

        return result;

    }

    public static String buildUrl(String rootPath, String relativePath) {
        String[] rootPathParts = rootPath.split("/");
        String [] relPathParts = relativePath.split("/");

        if(rootPath == null || relativePath == null) {
            return null;
        }

        int trimRoot = 0;
        int trimRel = 0;

        if(!"".equals(rootPathParts[rootPathParts.length - 1])) {
            trimRoot = 1;
        }
        for(int i = 0; i < rootPathParts.length; i++) {
            if("".equals(rootPathParts[i])) {
                trimRel += 1;
            }
            else {
                break;
            }
        }
        for(int i = 0; i < relPathParts.length; i ++) {
            if(".".equals(relPathParts[i])) {
                trimRel += 1;
            }
            else if ("..".equals(relPathParts[i])) {
                trimRel += 1;
            }
        }

        String [] outputParts = new String[rootPathParts.length + relPathParts.length - trimRoot - trimRel];
        System.arraycopy(rootPathParts, 0, outputParts, 0, rootPathParts.length - trimRoot);
        System.arraycopy(relPathParts,
                trimRel,
                outputParts,
                rootPathParts.length - trimRoot + trimRel - 1,
                relPathParts.length - trimRel);

        return StringUtils.join(outputParts, "/");
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