package io.swagger.v3.parser.extensions;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.ResolverCache;
import io.swagger.v3.parser.util.OpenAPIDeserializer;

import java.util.Map;

public interface JsonSchemaParserExtension {

    Schema getSchema(ObjectNode node, String location, OpenAPIDeserializer.ParseResult result, Map<String, Object> rootMap, String basePath);


    boolean resolveSchema(Schema schema, ResolverCache cache, OpenAPI openAPI, boolean openapi31);

}
