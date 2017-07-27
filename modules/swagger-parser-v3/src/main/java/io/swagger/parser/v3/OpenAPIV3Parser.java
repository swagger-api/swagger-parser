package io.swagger.parser.v3;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.parser.extensions.SwaggerParserExtension;
import io.swagger.parser.models.AuthorizationValue;
import io.swagger.parser.models.ParseOptions;
import io.swagger.parser.models.SwaggerParseResult;
import io.swagger.parser.v3.util.ClasspathHelper;
import io.swagger.parser.v3.util.DeserializationUtils;
import io.swagger.parser.v3.util.OpenAPIDeserializer;
import io.swagger.parser.v3.util.RemoteUrl;
import io.swagger.parser.v3.util.ResolverFully;
import io.swagger.util.Json;
import org.apache.commons.io.FileUtils;

import javax.net.ssl.SSLHandshakeException;
import java.net.URI;
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

    static {
        JSON_MAPPER = ObjectMapperFactory.createJson();
        YAML_MAPPER = ObjectMapperFactory.createYaml();
    }
    @Override
    public SwaggerParseResult readLocation(String url, List<AuthorizationValue> auth, ParseOptions options) {
        SwaggerParseResult result = new SwaggerParseResult();
        try {

            result = readWithInfo(url,auth);

            String version = result.getOpenAPI().getOpenapi();
            if(auth == null) {
                auth = new ArrayList<>();
            }
            if(version != null && version.startsWith("3.0")) {
                if (options != null) {
                    if (options.isResolve()) {
                        result.setOpenAPI(new OpenAPIResolver(result.getOpenAPI(), auth, null).resolve());
                    }if(options.isResolveFully()){
                        result.setOpenAPI(new OpenAPIResolver(result.getOpenAPI(), auth, null).resolve());
                        new ResolverFully().resolveFully(result.getOpenAPI());
                    }
                }
            }

        }

        catch (Exception e) {
            result.setMessages(Arrays.asList(e.getMessage()));
        }
        return result;
    }

    public SwaggerParseResult readWithInfo(JsonNode node) {
        OpenAPIDeserializer ser = new OpenAPIDeserializer();
        return ser.deserialize(node);
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
                    data = FileUtils.readFileToString(path.toFile(), "UTF-8");
                } else {
                    data = ClasspathHelper.loadFileFromClasspath(location);
                }
            }
            JsonNode rootNode;
            if (data.trim().startsWith("{")) {
                ObjectMapper mapper = Json.mapper();
                rootNode = mapper.readTree(data);
            } else {
                rootNode = DeserializationUtils.readYamlTree(data);
            }
            return readWithInfo(rootNode);
        }
        catch (SSLHandshakeException e) {
            SwaggerParseResult output = new SwaggerParseResult();
            output.setMessages(Arrays.asList("unable to read location `" + location + "` due to a SSL configuration error.  " +
                    "It is possible that the server SSL certificate is invalid, self-signed, or has an untrusted " +
                    "Certificate Authority."));
            return output;
        }
        catch (Exception e) {
            SwaggerParseResult output = new SwaggerParseResult();
            output.setMessages(Arrays.asList("unable to read location `" + location + "`"));
            return output;
        }
    }

    @Override
    public SwaggerParseResult readContents(String swaggerAsString, List<AuthorizationValue> auth, ParseOptions options) {
        SwaggerParseResult result = new SwaggerParseResult();
        if(swaggerAsString != null && !"".equals(swaggerAsString.trim())) {
            ObjectMapper mapper;
            if(swaggerAsString.trim().startsWith("{")) {
                mapper = JSON_MAPPER;
            }
            else {
                mapper = YAML_MAPPER;
            }
            if(auth == null) {
                auth = new ArrayList<>();
            }
            if(options != null) {
                if (options.isResolve()) {
                    try {
                        OpenAPIDeserializer deserializer = new OpenAPIDeserializer();
                        JsonNode rootNode = mapper.readTree(swaggerAsString.getBytes());
                        result = deserializer.deserialize(rootNode);
                        OpenAPIResolver resolver = new OpenAPIResolver(result.getOpenAPI(), auth, null);
                        result.setOpenAPI(resolver.resolve());

                    } catch (Exception e) {
                        result.setMessages(Arrays.asList(e.getMessage()));
                    }
                }else{
                    try {
                        JsonNode rootNode = mapper.readTree(swaggerAsString.getBytes());
                        result = new OpenAPIDeserializer().deserialize(rootNode);

                    } catch (Exception e) {
                        result.setMessages(Arrays.asList(e.getMessage()));
                    }
                }if (options.isResolveFully()){
                    result.setOpenAPI(new OpenAPIResolver(result.getOpenAPI(), auth, null).resolve());
                    new ResolverFully().resolveFully(result.getOpenAPI());
                }
            }else{
                try {
                    JsonNode rootNode = mapper.readTree(swaggerAsString.getBytes());
                    result = new OpenAPIDeserializer().deserialize(rootNode);

                } catch (Exception e) {
                    result.setMessages(Arrays.asList(e.getMessage()));
                }
            }
        }
        else {
            result.setMessages(Arrays.asList("No swagger supplied"));
        }
        return result;
    }

    protected List<io.swagger.parser.extensions.SwaggerParserExtension> getExtensions() {
        List<io.swagger.parser.extensions.SwaggerParserExtension> extensions = new ArrayList<>();

        ServiceLoader<SwaggerParserExtension> loader = ServiceLoader.load(io.swagger.parser.extensions.SwaggerParserExtension.class);
        Iterator<SwaggerParserExtension> itr = loader.iterator();
        while (itr.hasNext()) {
            extensions.add(itr.next());
        }
        extensions.add(0, new OpenAPIV3Parser());
        return extensions;
    }

    /**
     * Transform the swagger-model version of AuthorizationValue into a parser-specific one, to avoid
     * dependencies across extensions
     *
     * @param input
     * @return
     */
    protected List<io.swagger.parser.models.AuthorizationValue> transform(List<AuthorizationValue> input) {
        if(input == null) {
            return null;
        }

        List<io.swagger.parser.models.AuthorizationValue> output = new ArrayList<>();

        for(AuthorizationValue value : input) {
            io.swagger.parser.models.AuthorizationValue v = new io.swagger.parser.models.AuthorizationValue();

            v.setKeyName(value.getKeyName());
            v.setValue(value.getValue());
            v.setType(value.getType());

            output.add(v);
        }

        return output;
    }
}
