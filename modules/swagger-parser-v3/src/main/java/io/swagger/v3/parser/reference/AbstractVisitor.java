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

public abstract class AbstractVisitor implements Visitor {

    @Override
    public OpenAPI visitOpenApi(OpenAPI openAPI){
        return null;
    }
    @Override
    public Paths visitPaths(Paths paths){
        return null;
    }

    @Override
    public Components visitComponents(Components components){
        return null;
    }

    @Override
    public PathItem visitPathItem(PathItem pathItem){
        return null;
    }

    @Override
    public Parameter visitParameter(Parameter parameter){
        return null;
    }

    @Override
    public Operation visitOperation(Operation operation){
        return null;
    }

    @Override
    public Schema visitSchema(Schema schema, List<String> inheritedIds){
        return null;
    }

    @Override
    public ApiResponse visitResponse(ApiResponse response){
        return null;
    }

    @Override
    public RequestBody visitRequestBody(RequestBody requestBody){
        return null;
    }

    @Override
    public Link visitLink(Link link){
        return null;
    }

    @Override
    public SecurityScheme visitSecurityScheme(SecurityScheme securityScheme){
        return null;
    }

    @Override
    public ApiResponses visitResponses(ApiResponses responses){
        return null;
    }

    @Override
    public MediaType visitMediaType(MediaType mediaType){
        return null;
    }

    @Override
    public Encoding visitEncoding(Encoding encoding){
        return null;
    }

    @Override
    public Header visitHeader(Header header){
        return null;
    }

    @Override
    public Example visitExample(Example example){
        return null;
    }
}
