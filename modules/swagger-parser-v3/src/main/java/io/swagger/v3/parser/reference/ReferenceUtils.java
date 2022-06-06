package io.swagger.v3.parser.reference;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.core.util.Json31;
import io.swagger.v3.core.util.Yaml31;
import io.swagger.v3.parser.core.models.AuthorizationValue;
import io.swagger.v3.parser.util.ClasspathHelper;
import io.swagger.v3.parser.util.RemoteUrl;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.List;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;

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

    public static String readURI(String absoluteUri, List<AuthorizationValue> auths) throws Exception {
        URI resolved = new URI(absoluteUri);
        if (StringUtils.isBlank(resolved.getScheme())) {
            // try file
            String content = null;
            try {
                content = readFile(absoluteUri);
            } catch (Exception e) {
                //
            }
            if (StringUtils.isBlank(content)) {
                content = readClasspath(absoluteUri);
            }
            return content;
        }  else if (resolved.getScheme().startsWith("http")) {
            return readHttp(absoluteUri, auths);
        } else if (resolved.getScheme().startsWith("file")) {
            return readFile(absoluteUri);
        } else if (resolved.getScheme().startsWith("classpath")) {
            return readClasspath(absoluteUri);
        }
        throw new RuntimeException("scheme not supported for uri: " + absoluteUri);
    }

    public static JsonNode deserializeIntoTree(String content) throws Exception {
        boolean isJson = content.trim().startsWith("{");
        return isJson ? Json31.mapper().readTree(content) : Yaml31.mapper().readTree(content);
    }

    public static JsonNode parse(String absoluteUri, List<AuthorizationValue> auths) throws Exception {
        return deserializeIntoTree(readURI(absoluteUri, auths));
    }

    public static String readFile(String uri) throws Exception {
        try (InputStream inputStream = new FileInputStream(uri)) {
            return IOUtils.toString(inputStream, UTF_8);
        }
    }

    public static String readClasspath(String uri) throws Exception {
        return ClasspathHelper.loadFileFromClasspath(uri);
    }
    public static String readHttp(String uri, List<AuthorizationValue> auths) throws Exception {
        return RemoteUrl.urlToString(uri, auths);
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
            if (StringUtils.isNotBlank(token)) {
                node = node.get(ReferenceUtils.unescapePointer(token));
                //if at any point we do find an element we expect, print and error and abort
                if (node == null) {
                    throw new RuntimeException("Could not find " + fragment + " in contents of " + uri);
                }
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
