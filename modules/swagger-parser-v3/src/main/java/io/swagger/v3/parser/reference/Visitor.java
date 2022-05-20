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

import java.util.List;

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

}
