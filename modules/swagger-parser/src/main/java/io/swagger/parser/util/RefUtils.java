package io.swagger.parser.util;

import io.swagger.models.Model;
import io.swagger.models.auth.AuthorizationValue;
import io.swagger.models.refs.RefFormat;
import org.apache.commons.io.IOUtils;

import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class RefUtils {

    private RefUtils() {
        throw new InstantiationError("Must not instantiate this class");
    }

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

    public static String deconflictName(String possiblyConflictingDefinitionName, Map<String, Model> definitions) {

        String result = possiblyConflictingDefinitionName;
        int count = 1;

        while (definitions.containsKey(result)) {
            result = possiblyConflictingDefinitionName + count;
            count++;
        }

        return result;
    }

    public static boolean isAnExternalRefFormat(RefFormat refFormat) {
        return refFormat == RefFormat.URL || refFormat == RefFormat.RELATIVE;
    }

    public static String readExternalRef(String file, RefFormat refFormat, List<AuthorizationValue> auths,
                                         Path parentDirectory) {

        if (!RefUtils.isAnExternalRefFormat(refFormat)) {
            throw new RuntimeException("Ref is not external");
        }

        String result;

        try {
            if (refFormat == RefFormat.URL) {
                result = RemoteUrl.urlToString(file, auths);
            } else {
                //its assumed to be a relative file ref
                final Path pathToUse = parentDirectory.resolve(file).normalize();

                if(Files.exists(pathToUse)) {
                    result = IOUtils.toString(new FileInputStream(pathToUse.toFile()));
                } else {
                    result = ClasspathHelper.loadFileFromClasspath(file);
                }

            }
        } catch (Exception e) {
            throw new RuntimeException("Unable to load " + refFormat + " ref: " + file, e);
        }

        return result;

    }
}
