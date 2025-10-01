package io.swagger.v3.parser.reference;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.links.Link;
import io.swagger.v3.oas.models.media.Encoding;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.parser.core.models.AuthorizationValue;
import io.swagger.v3.parser.urlresolver.PermittedUrlsChecker;
import io.swagger.v3.parser.util.ClasspathHelper;
import io.swagger.v3.parser.util.RemoteUrl;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

public interface Visitor {
    OpenAPI visitOpenApi(OpenAPI openAPI);

    Paths visitPaths(Paths paths);

    Components visitComponents(Components components);

    PathItem visitPathItem(PathItem pathItem);

    Parameter visitParameter(Parameter parameter);

    Operation visitOperation(Operation operation);

    Schema visitSchema(Schema schema, List<String> inheritedIds);

    ApiResponse visitResponse(ApiResponse response);

    RequestBody visitRequestBody(RequestBody requestBody);

    Link visitLink(Link link);

    SecurityScheme visitSecurityScheme(SecurityScheme securityScheme);

    ApiResponses visitResponses(ApiResponses responses);

    MediaType visitMediaType(MediaType mediaType);

    Encoding visitEncoding(Encoding encoding);

    Header visitHeader(Header header);

    Example visitExample(Example example);

    default String readFile(String path) throws Exception {
        try (InputStream inputStream = new FileInputStream(path)) {
            return IOUtils.toString(inputStream, UTF_8);
        }
    }

    default String readClasspath(String classPath) {
        return ClasspathHelper.loadFileFromClasspath(classPath);
    }
    default String readHttp(String uri, List<AuthorizationValue> auths, PermittedUrlsChecker permittedUrlsChecker) throws Exception {
        return RemoteUrl.urlToString(uri, auths, permittedUrlsChecker);
    }

    default String readURI(String absoluteUri, List<AuthorizationValue> auths, PermittedUrlsChecker permittedUrlsChecker) throws Exception {
        URI resolved = new URI(absoluteUri);
        if (StringUtils.isNotBlank(resolved.getScheme())) {
            if (resolved.getScheme().startsWith("http")) {
                return readHttp(absoluteUri, auths, permittedUrlsChecker);
            } else if (resolved.getScheme().startsWith("file")) {
                return readFile(resolved.getPath());
            } else if (resolved.getScheme().startsWith("classpath")) {
                return readClasspath(resolved.getPath());
            }
        }
        // If no matches exists, try file
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
    }

}
