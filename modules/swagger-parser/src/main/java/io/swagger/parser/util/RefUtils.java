package io.swagger.parser.util;

import io.swagger.models.Model;
import io.swagger.models.refs.RefFormat;

import java.net.URL;
import java.util.Map;

/**
 * Created by russellb337 on 7/10/15.
 */
public class RefUtils {

    public static String computeDefinitionName(String ref) {

        final String[] refParts = ref.split("#/");

        if (refParts.length > 2) {
            throw new RuntimeException("Invalid ref format: " + ref);
        }

        final String file = refParts[0];
        final String definitionPath = refParts.length == 2 ? refParts[1] : null;


        String plausibleName;

        if(definitionPath != null) { //the name will come from the definition path
            final String[] jsonPathElements = definitionPath.split("/");
            plausibleName = jsonPathElements[jsonPathElements.length-1];
        } else { //no definition path, so we must come up with a name from the file
            final String[] filePathElements = file.split("/");
            plausibleName = filePathElements[filePathElements.length-1];

            final String[] split = plausibleName.split("\\.");
            plausibleName = split[0];
        }

        return plausibleName;
    }

    public static String deconflictName(String possiblyConflictingDefinitionName, Map<String, Model> definitions) {

        String result = possiblyConflictingDefinitionName;
        int count = 1;

        while(definitions.containsKey(result)) {
            result += "-" + count;
            count++;
        }

        return result;
    }

    public static boolean isAnExternalRefFormat(RefFormat refFormat) {
        return refFormat == RefFormat.URL || refFormat == RefFormat.RELATIVE;
    }


}
