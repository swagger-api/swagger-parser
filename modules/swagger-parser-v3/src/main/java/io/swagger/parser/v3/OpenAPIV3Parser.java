package io.swagger.parser.v3;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.parser.extensions.SwaggerParserExtension;
import io.swagger.parser.models.AuthorizationValue;
import io.swagger.parser.models.ParseOptions;
import io.swagger.parser.models.SwaggerParseResult;
import io.swagger.parser.v3.util.ClasspathHelper;
import io.swagger.parser.v3.util.OpenAPIDeserializer;
import io.swagger.parser.v3.util.RemoteUrl;
import io.swagger.parser.v3.util.ResolverFully;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

import javax.net.ssl.SSLHandshakeException;

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
            String data = getDataFromLocation(url, auth);
            result = readContents(data, auth, options);
        }
        catch (SSLHandshakeException e) {
            result.setMessages(Arrays.asList("unable to read location `" + url + "` due to a SSL configuration error.  " +
                  "It is possible that the server SSL certificate is invalid, self-signed, or has an untrusted " +
                  "Certificate Authority."));
        }
        catch (Exception e) {
            result.setMessages(Arrays.asList("unable to read location `" + url + "`"));
        }
        return result;
    }

    @Override
    public SwaggerParseResult readContents(String swaggerAsString, List<AuthorizationValue> auth, ParseOptions options) {
        SwaggerParseResult result = new SwaggerParseResult();
        if(swaggerAsString != null && !"".equals(swaggerAsString.trim())) {
            try {
                JsonNode rootNode = mapDataToJsonNode(swaggerAsString);
                result = readWithInfo(rootNode);
      
                if (result.getOpenAPI() != null) {
                    String version = result.getOpenAPI().getOpenapi();
                    if (version != null && version.startsWith("3.0")) {
                        if (options != null) {
                            if (auth == null) {
                                auth = new ArrayList<>();
                            }
                            OpenAPIResolver resolver = new OpenAPIResolver(result.getOpenAPI(), auth, null);
                            if (options.isResolve() || options.isResolveFully()) {
                                result.setOpenAPI(resolver.resolve());
                            }
                            if (options.isResolveFully()) {
                                new ResolverFully().resolveFully(result.getOpenAPI());
                            }
                        }
                    }
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

    private SwaggerParseResult readWithInfo(JsonNode node) {
        OpenAPIDeserializer ser = new OpenAPIDeserializer();
        return ser.deserialize(node);
    }

    private String getDataFromLocation(String location, List<AuthorizationValue> auths) throws Exception {
        String data;
  
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
        
        return data;
    }
    
    private JsonNode mapDataToJsonNode(String data) throws JsonProcessingException, IOException {
        JsonNode rootNode;
        if (data.trim().startsWith("{")) {
            rootNode = JSON_MAPPER.readTree(data);
        } else {
            rootNode = YAML_MAPPER.readTree(data);
        }
        return rootNode;
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
