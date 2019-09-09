package io.swagger.v3.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.core.extensions.SwaggerParserExtension;
import io.swagger.v3.parser.core.models.AuthorizationValue;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import io.swagger.v3.parser.util.ClasspathHelper;
import io.swagger.v3.parser.util.InlineModelResolver;
import io.swagger.v3.parser.util.OpenAPIDeserializer;
import io.swagger.v3.parser.util.RemoteUrl;
import io.swagger.v3.parser.util.ResolverFully;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLHandshakeException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

public class OpenAPIV3Parser implements SwaggerParserExtension {
    private static ObjectMapper JSON_MAPPER, YAML_MAPPER;
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenAPIV3Parser.class);
    private static String encoding = "UTF-8";

    static {
        JSON_MAPPER = ObjectMapperFactory.createJson();
        YAML_MAPPER = ObjectMapperFactory.createYaml();
    }

    public static String getEncoding() {
        return encoding;
    }

    public static void setEncoding(String encoding) {
        if (Charset.isSupported(encoding)) {
            OpenAPIV3Parser.encoding = encoding;
        }
    }

    @Override
    public SwaggerParseResult readLocation(String url, List<AuthorizationValue> auth, ParseOptions options) {
        SwaggerParseResult result = new SwaggerParseResult();
        try {
            if (auth == null) {
                auth = new ArrayList<>();
            }
            result = readWithInfo(url,auth);

            if (result.getOpenAPI() != null) {
                String version = result.getOpenAPI().getOpenapi();
                if (version != null && version.startsWith("3.0")) {
                    if (options != null) {
                        OpenAPIResolver resolver = new OpenAPIResolver(result.getOpenAPI(), auth, url);
                        if (options.isResolve()) {
                            result.setOpenAPI(resolver.resolve());
                        }
                        if (options.isResolveFully()) {
                            result.setOpenAPI(resolver.resolve());
                            new ResolverFully(options.isResolveCombinators()).resolveFully(result.getOpenAPI());
                        } else if (options.isFlatten()) {
                            InlineModelResolver inlineModelResolver = new InlineModelResolver();
                            inlineModelResolver.setSkipMatches(options.isSkipMatches());
                            inlineModelResolver.flatten(result.getOpenAPI());
                        }
                    }
                }
            }

        }

        catch (Exception e) {
            LOGGER.warn("Exception while reading:", e);
            result.setMessages(Arrays.asList(e.getMessage()));
        }
        return result;
    }

    public OpenAPI read(String location) {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        return read(location,  null, options);
    }
    public OpenAPI read(String location, List<AuthorizationValue> auths, ParseOptions resolve) {
        if (location == null) {
            return null;
        }
        location = location.replaceAll("\\\\","/");
        OpenAPI output;

        List<SwaggerParserExtension> parserExtensions = getExtensions();
        SwaggerParseResult parsed;
        for (SwaggerParserExtension extension : parserExtensions) {
            parsed = extension.readLocation(location, auths, resolve);
            for (String message : parsed.getMessages()) {
                LOGGER.info("{}: {}", extension, message);
            }
            output = parsed.getOpenAPI();
            if (output != null) {
                return output;
            }
        }
        return null;
    }

    public SwaggerParseResult readWithInfo(String path, JsonNode node) {
        OpenAPIDeserializer ser = new OpenAPIDeserializer();
        return ser.deserialize(node, path);
    }

    private ObjectMapper getRightMapper(String data) {
      ObjectMapper mapper;
      if(data.trim().startsWith("{")) {
          mapper = JSON_MAPPER;
      } else {
          mapper = YAML_MAPPER;
      }
      return mapper;
    }

    public SwaggerParseResult readWithInfo(String location, List<AuthorizationValue> auths) {
        String data;

        try {
            location = location.replaceAll("\\\\","/");
            if (location.toLowerCase().startsWith("http")) {
                data = RemoteUrl.urlToString(location, auths);
            } else {
                final String fileScheme = "file:";
                Path path;
                if (location.toLowerCase().startsWith(fileScheme)) {
                    path = Paths.get(URI.create(location));
                } else {
                    path = Paths.get(location);
                }
                if (Files.exists(path)) {
                    data = FileUtils.readFileToString(path.toFile(), encoding);
                } else {
                    data = ClasspathHelper.loadFileFromClasspath(location);
                }
            }
            LOGGER.debug("Loaded raw data: {}", data);
            ObjectMapper mapper = getRightMapper(data);
            JsonNode rootNode = mapper.readTree(data);
            LOGGER.debug("Parsed rootNode: {}", rootNode);
            return readWithInfo(location, rootNode);
        }
        catch (SSLHandshakeException e) {
            SwaggerParseResult output = new SwaggerParseResult();
            output.setMessages(Arrays.asList("unable to read location `" + location + "` due to a SSL configuration error.  " +
                    "It is possible that the server SSL certificate is invalid, self-signed, or has an untrusted " +
                    "Certificate Authority."));
            return output;
        }
        catch (Exception e) {
            LOGGER.warn("Exception while reading:", e);
            SwaggerParseResult output = new SwaggerParseResult();
            output.setMessages(Arrays.asList("unable to read location `" + location + "`"));
            return output;
        }
    }

    @Override
    public SwaggerParseResult readContents(String swaggerAsString, List<AuthorizationValue> auth, ParseOptions options) {
        SwaggerParseResult result = new SwaggerParseResult();
        if(swaggerAsString != null && !"".equals(swaggerAsString.trim())) {
            ObjectMapper mapper = getRightMapper(swaggerAsString);

            if(auth == null) {
                auth = new ArrayList<>();
            }
            try{
                if(options != null) {
                    if (options.isResolve()) {
                        OpenAPIDeserializer deserializer = new OpenAPIDeserializer();
                        JsonNode rootNode = mapper.readTree(swaggerAsString.getBytes());
                        result = deserializer.deserialize(rootNode);
                        OpenAPIResolver resolver = new OpenAPIResolver(result.getOpenAPI(), auth, null);
                        result.setOpenAPI(resolver.resolve());
                    }else{
                        JsonNode rootNode = mapper.readTree(swaggerAsString.getBytes());
                        result = new OpenAPIDeserializer().deserialize(rootNode);
                    }
                    if (options.isResolveFully()) {
                        result.setOpenAPI(new OpenAPIResolver(result.getOpenAPI(), auth, null).resolve());
                        new ResolverFully(options.isResolveCombinators()).resolveFully(result.getOpenAPI());
                    } else if (options.isFlatten()) {
                        InlineModelResolver inlineModelResolver = new InlineModelResolver();
                        inlineModelResolver.setSkipMatches(options.isSkipMatches());
                        inlineModelResolver.flatten(result.getOpenAPI());
                    }
                }else{
                    JsonNode rootNode = mapper.readTree(swaggerAsString.getBytes());
                    result = new OpenAPIDeserializer().deserialize(rootNode);
                }
            } catch (Exception e) {
                result.setMessages(Arrays.asList(e.getMessage()));
            }
        }
        else {
            result.setMessages(Arrays.asList("No swagger supplied"));
        }
        return result;
    }

    /**
     * Locates extensions on the current thread class loader and then, if it differs
     * from this class classloader (as in OSGi), locates extensions from this
     * class classloader as well.
     *
     * @return a list of extensions
     */
    public static List<SwaggerParserExtension> getExtensions() {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        List<SwaggerParserExtension> extensions = getExtensions(tccl);
        ClassLoader cl = SwaggerParserExtension.class.getClassLoader();
        if (cl != tccl) {
            extensions.addAll(getExtensions(cl));
        }
        extensions.add(0, new OpenAPIV3Parser());
        return extensions;
    }

    protected static List<SwaggerParserExtension> getExtensions(ClassLoader cl) {
        List<SwaggerParserExtension> extensions = new ArrayList<>();

        ServiceLoader<SwaggerParserExtension> loader = ServiceLoader.load(SwaggerParserExtension.class, cl);
        Iterator<SwaggerParserExtension> itr = loader.iterator();
        while (itr.hasNext()) {
            extensions.add(itr.next());
        }
        return extensions;
    }

   /**
     * Transform the swagger-model version of AuthorizationValue into a parser-specific one, to avoid
     * dependencies across extensions
     *
     * @param input
     * @return
     */
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

    public SwaggerParseResult readContents(String yaml) {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        return readContents(yaml,null, options);
    }
}
