package io.swagger.v3.parser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.core.extensions.SwaggerParserExtension;
import io.swagger.v3.parser.core.models.AuthorizationValue;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import io.swagger.v3.parser.exception.EncodingNotSupportedException;
import io.swagger.v3.parser.exception.ReadContentException;
import io.swagger.v3.parser.util.ClasspathHelper;
import io.swagger.v3.parser.util.InlineModelResolver;
import io.swagger.v3.parser.util.OpenAPIDeserializer;
import io.swagger.v3.parser.util.RemoteUrl;
import io.swagger.v3.parser.util.ResolverFully;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
import javax.net.ssl.SSLHandshakeException;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenAPIV3Parser implements SwaggerParserExtension {
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenAPIV3Parser.class);
    private static ObjectMapper JSON_MAPPER, YAML_MAPPER;
    /**
     * Encoding of the resource content with OpenAPI spec to parse.
     */
    private static String encoding = StandardCharsets.UTF_8.displayName();

    static {
        JSON_MAPPER = ObjectMapperFactory.createJson();
        YAML_MAPPER = ObjectMapperFactory.createYaml();
    }

    /**
     * Locates extensions on the current thread class loader and then, if it differs from this class classloader (as in
     * OSGi), locates extensions from this class classloader as well.
     * @return a list of extensions
     */
    public static List<SwaggerParserExtension> getExtensions() {
        final ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        final List<SwaggerParserExtension> extensions = getExtensions(tccl);
        final ClassLoader cl = SwaggerParserExtension.class.getClassLoader();
        if (cl != tccl) {
            extensions.addAll(getExtensions(cl));
        }
        extensions.add(0, new OpenAPIV3Parser());
        return extensions;
    }

    protected static List<SwaggerParserExtension> getExtensions(ClassLoader cl) {
        final List<SwaggerParserExtension> extensions = new ArrayList<>();

        final ServiceLoader<SwaggerParserExtension> loader = ServiceLoader.load(SwaggerParserExtension.class, cl);
        for (SwaggerParserExtension extension : loader) {
            extensions.add(extension);
        }
        return extensions;
    }

    public static String getEncoding() {
        return encoding;
    }

    public static void setEncoding(String encoding) {
        if (!Charset.isSupported(encoding)) {
            throw new EncodingNotSupportedException(encoding);
        }
        OpenAPIV3Parser.encoding = encoding;
    }

    @Override
    public SwaggerParseResult readLocation(String url, List<AuthorizationValue> auth, ParseOptions options) {
        try {
            final String content = readContentFromLocation(url, emptyListIfNull(auth));
            LOGGER.debug("Loaded raw data: {}", content);
            return readContents(content, auth, options, url);
        } catch (ReadContentException e) {
            LOGGER.warn("Exception while reading:", e);
            return SwaggerParseResult.ofError(e.getMessage());
        }
    }

    @Override
    public SwaggerParseResult readContents(String swaggerAsString, List<AuthorizationValue> auth,
                                           ParseOptions options) {
        return readContents(swaggerAsString, auth, options, null);
    }

    public OpenAPI read(String location) {
        final ParseOptions options = new ParseOptions();
        options.setResolve(true);
        return read(location, null, options);
    }

    public OpenAPI read(String location, List<AuthorizationValue> auths, ParseOptions resolve) {
        if (location == null) {
            return null;
        }

        final List<SwaggerParserExtension> parserExtensions = getExtensions();
        SwaggerParseResult parsed;
        for (SwaggerParserExtension extension : parserExtensions) {
            parsed = extension.readLocation(location, auths, resolve);
            for (String message : parsed.getMessages()) {
                LOGGER.info("{}: {}", extension, message);
            }
            final OpenAPI result = parsed.getOpenAPI();
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    @Deprecated
    public SwaggerParseResult readWithInfo(String path, JsonNode node) {
        return parseJsonNode(path, node);
    }

    public SwaggerParseResult parseJsonNode(String path, JsonNode node) {
        return new OpenAPIDeserializer().deserialize(node, path,new ParseOptions());
    }
    public SwaggerParseResult parseJsonNode(String path, JsonNode node, ParseOptions options) {
        return new OpenAPIDeserializer().deserialize(node, path, options);
    }

    public SwaggerParseResult readContents(String yaml) {
        final ParseOptions options = new ParseOptions();
        options.setResolve(true);
        return readContents(yaml, null, options);
    }

    private SwaggerParseResult readContents(String swaggerAsString, List<AuthorizationValue> auth, ParseOptions options,
                                            String location) {
        if (swaggerAsString == null || swaggerAsString.trim().isEmpty()) {
            return SwaggerParseResult.ofError("Null or empty definition");
        }

        try {
            final ObjectMapper mapper = getRightMapper(swaggerAsString);
            final JsonNode rootNode = mapper.readTree(swaggerAsString);
            final SwaggerParseResult result;
            if (options != null) {
                result = parseJsonNode(location, rootNode, options);
            }else {
                result = parseJsonNode(location, rootNode);
            }
            if (result.getOpenAPI() != null) {
                return resolve(result, auth, options, location);
            }
            return result;
        } catch (JsonProcessingException e) {
            LOGGER.warn("Exception while parsing:", e);
            final String message = getParseErrorMessage(e.getOriginalMessage(), location);
            return SwaggerParseResult.ofError(message);
        }
    }

    @Deprecated
    public SwaggerParseResult readWithInfo(String location, List<AuthorizationValue> auths) {
        return readContents(readContentFromLocation(location, auths), auths, null);
    }

    private SwaggerParseResult resolve(SwaggerParseResult result, List<AuthorizationValue> auth, ParseOptions options,
            String location) {
        try {
            if (options != null) {
                if (options.isResolve() || options.isResolveFully()) {
                    result.setOpenAPI(new OpenAPIResolver(result.getOpenAPI(), emptyListIfNull(auth),
                            location).resolve());
                    if (options.isResolveFully()) {
                        new ResolverFully(options.isResolveCombinators()).resolveFully(result.getOpenAPI());
                    }
                }
                if (options.isFlatten()) {
                    final InlineModelResolver inlineModelResolver =
                            new InlineModelResolver(options.isFlattenComposedSchemas(),
                                    options.isCamelCaseFlattenNaming(), options.isSkipMatches(), options.isNameInlineResponsesBasedOnEndpoint());
                    if (result.getOpenAPI()!= null) {
                        inlineModelResolver.flatten(result.getOpenAPI());
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Exception while resolving:", e);
            result.setMessages(Collections.singletonList(e.getMessage()));
        }
        return result;
    }

    private String getParseErrorMessage(String originalMessage, String location) {
        if (Objects.isNull(originalMessage)) {
            return String.format("Unable to parse `%s`", location);
        }
        if (originalMessage.startsWith("Duplicate field")) {
            return String.format("%s in `%s`", originalMessage, location);
        }
        return originalMessage;
    }

    private <T> List<T> emptyListIfNull(List<T> list) {
        return Objects.isNull(list) ? new ArrayList<>() : list;
    }

    private ObjectMapper getRightMapper(String data) {
        if (data.trim().startsWith("{")) {
            return JSON_MAPPER;
        }
        return YAML_MAPPER;
    }

    private String readContentFromLocation(String location, List<AuthorizationValue> auth) {
        final String adjustedLocation = location.replaceAll("\\\\", "/");
        try {
            if (adjustedLocation.toLowerCase().startsWith("http")) {
                return RemoteUrl.urlToString(adjustedLocation, auth);
            } else {
                final String fileScheme = "file:";
                final Path path = adjustedLocation.toLowerCase().startsWith(fileScheme) ?
                        Paths.get(URI.create(adjustedLocation)) : Paths.get(adjustedLocation);
                if (Files.exists(path)) {
                    return FileUtils.readFileToString(path.toFile(), encoding);
                } else {
                    return ClasspathHelper.loadFileFromClasspath(adjustedLocation);
                }
            }
        } catch (SSLHandshakeException e) {
            final String message = String.format(
                    "Unable to read location `%s` due to a SSL configuration error. It is possible that the server SSL certificate is invalid, self-signed, or has an untrusted Certificate Authority.",
                    adjustedLocation);
            throw new ReadContentException(message, e);
        } catch (Exception e) {
            throw new ReadContentException(String.format("Unable to read location `%s`", adjustedLocation), e);
        }
    }

    /**
     * Transform the swagger-model version of AuthorizationValue into a parser-specific one, to avoid
     * dependencies across extensions
     *
     * @param input
     * @return
     */
    @Deprecated
    protected List<AuthorizationValue> transform(List<AuthorizationValue> input) {
        if(input == null) {
            return null;
        }

        List<AuthorizationValue> output = new ArrayList<>();

        for(AuthorizationValue value : input) {
            AuthorizationValue v = new AuthorizationValue();

            v.setKeyName(value.getKeyName());
            v.setValue(value.getValue());
            v.setType(value.getType());
            v.setUrlMatcher(value.getUrlMatcher());

            output.add(v);
        }

        return output;
    }


}
