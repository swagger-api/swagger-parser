package io.swagger.v3.parser.reference;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.StringUtils;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.regex.Pattern;

public class ReferenceUtils {

    public static String toBaseURI(String uri) throws Exception
    {
        URI resolved = new URI(uri);
        return (resolved.getScheme() != null ? resolved.getScheme() + ":" : "") + resolved.getSchemeSpecificPart();
    }

    public static String getFragment(String uri) throws Exception
    {
        URI resolved = new URI(uri);
        return resolved.getFragment();
    }

    public static String resolve(String uri, String baseURI) throws Exception {
        if (StringUtils.isBlank(uri)) {
            return baseURI;
        }
        return new URI(baseURI).resolve(uri).toString();
    }

    public static boolean isLocalRef(String ref) {
        if (!StringUtils.isBlank(ref) && ref.startsWith("#")) {
            return true;
        }
        return false;
    }

    public static boolean isLocalRefToComponents(String ref) {
        if (!StringUtils.isBlank(ref) && ref.startsWith("#/components")) {
            return true;
        }
        return false;
    }

    public static boolean isAnchorRef(String ref) {
        if (!StringUtils.isBlank(ref) && ref.startsWith("#")) {
            return isAnchor(ref.substring(1));
        }
        return false;
    }

    public static boolean isAnchor(String ref) {
        if (!StringUtils.isBlank(ref) && Pattern.matches("^[A-Za-z_][A-Za-z_0-9.-]*$", ref)) {
            return true;
        }
        return false;
    }

    public static String unescapePointer(String jsonPathElement) {
        // URL decode the fragment
        try {
            jsonPathElement = URLDecoder.decode(jsonPathElement, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            //
        }
        // Unescape the JSON Pointer segment using the algorithm described in RFC 6901, section 4:
        // https://tools.ietf.org/html/rfc6901#section-4
        // First transform any occurrence of the sequence '~1' to '/'
        jsonPathElement = jsonPathElement.replaceAll("~1", "/");
        // Then transforming any occurrence of the sequence '~0' to '~'.
        return jsonPathElement.replaceAll("~0", "~");
    }

    public static JsonNode jsonPointerEvaluate(String fragment, JsonNode tree, String uri) {
        if (StringUtils.isBlank(fragment)) {
            return tree;
        }
        String[] tokens = fragment.split("/");
        JsonNode node = tree;
        for (String token : tokens) {
            if (StringUtils.isBlank(token)) {
                continue;
            }
            if (node.isArray()) {
                node = node.get(Integer.valueOf(token));
            } else {
                node = node.get(ReferenceUtils.unescapePointer(token));
            }
            //if at any point we do find an element we expect, print and error and abort
            if (node == null) {
                throw new RuntimeException("Could not find " + fragment + " in contents of " + uri);
            }
        }
        return node;
    }

    public static String getRefName(String uri) {
        if (uri.indexOf("/") == -1) {
            if (uri.startsWith("#")) {
                return uri.substring(1);
            }
        }
        String[] tokens = uri.split("/");
        return tokens[tokens.length -1];
    }
}
