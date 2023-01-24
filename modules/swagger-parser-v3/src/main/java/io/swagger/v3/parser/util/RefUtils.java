package io.swagger.v3.parser.util;

import io.swagger.v3.parser.core.models.AuthorizationValue;
import io.swagger.v3.parser.models.RefFormat;
import io.swagger.v3.parser.processors.ExternalRefProcessor;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class RefUtils {

    private static final String REFERENCE_SEPARATOR = "#/";

    private RefUtils() {
        // static access only
    }

    public static String computeDefinitionName(String ref) {

        final String[] refParts = ref.split(REFERENCE_SEPARATOR);

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
            // Fix for issue-1621 and issue-1865
            //validate number of dots
            if(split.length > 2) {
                //Remove dot so ref can be interpreted as internal and relative in Swagger-Core schema class 'set$ref'
                plausibleName = String.join("", Arrays.copyOf(split, split.length - 1));
            }else{
                plausibleName = split[0];
            }
        }

        return plausibleName;
    }

    public static Optional<String> getExternalPath(String ref) {
        if (ref == null) {
            return Optional.empty();
        }
        return Optional.of(ref.split(REFERENCE_SEPARATOR))
            .filter(it -> it.length == 2)
            .map(it -> it[0])
            .filter(it -> !it.isEmpty());
    }

    public static boolean isAnExternalRefFormat(RefFormat refFormat) {
        return refFormat == RefFormat.URL || refFormat == RefFormat.RELATIVE;
    }

    public static RefFormat computeRefFormat(String ref) {
        RefFormat result = RefFormat.INTERNAL;
        ref = mungedRef(ref);
        if(ref.startsWith("http")||ref.startsWith("https")) {
            result = RefFormat.URL;
        } else if(ref.startsWith(REFERENCE_SEPARATOR)) {
            result = RefFormat.INTERNAL;
        } else if(ref.startsWith(".") || ref.startsWith("/") || ref.indexOf(REFERENCE_SEPARATOR) > 0) {
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

    public static String readExternalClasspathRef(String file, RefFormat refFormat, List<AuthorizationValue> auths,
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
                String pathRef = ExternalRefProcessor.join(rootPath, file);

                result = ClasspathHelper.loadFileFromClasspath(pathRef);
            }
        } catch (Exception e) {
            throw new RuntimeException("Unable to load " + refFormat + " ref: " + file, e);
        }

        return result;

    }

    public static String buildUrl(String rootPath, String relativePath) {
      if(rootPath == null || relativePath == null) {
          return null;
      }

      try {
          int until = rootPath.lastIndexOf("/")+1;
          String root = rootPath.substring(0, until);
          URL rootUrl = new URL(root);
          URL finalUrl = new URL(rootUrl, relativePath);
          return finalUrl.toString();
      }
      catch(Exception e) {
          throw new RuntimeException(e);
      }
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
                    result = readAll(pathToUse);
                } else {
                    String url = file;
                    if (url.contains("..")) {
                        int parentCount = 0;
                        while (url.contains("..")) {
                            url = url.substring(url.indexOf(".") + 2);
                            parentCount++;
                        }
                        for (int i = 0; i < parentCount - 1; i++) {
                            parentDirectory = parentDirectory.getParent();
                        }
                        url = parentDirectory + url;
                    } else {
                        url = parentDirectory + url.substring(url.indexOf(".") + 1);
                    }
                    final Path pathToUse2 = parentDirectory.resolve(url).normalize();

                    if(Files.exists(pathToUse2)) {
                        result = readAll(pathToUse2);
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

    private static String readAll(Path path) throws IOException {
        try (InputStream inputStream = new FileInputStream(path.toFile())) {
            return IOUtils.toString(inputStream, UTF_8);
        }
    }
}
