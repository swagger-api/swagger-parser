package io.swagger.v3.parser.reference;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.core.util.Json31;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.callbacks.Callback;
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
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

public class IdsTraverser implements Traverser {

    private DereferencerContext context;

    public IdsTraverser(DereferencerContext context) {
        this.context = context;
    }

    public DereferencerContext getContext() {
        return context;
    }

    public void setContext(DereferencerContext context) {
        this.context = context;
    }

    public IdsTraverser context(DereferencerContext context) {
        this.context = context;
        return this;
    }

    public Set<Object> visiting = new HashSet<>();
    protected HashMap<Object, Object> visitedMap = new HashMap<>();

    public OpenAPI traverse(OpenAPI openAPI, Visitor visitor) throws Exception {
        return traverseOpenApi(openAPI, visitor);
    }

    public OpenAPI traverseOpenApi(OpenAPI openAPI, Visitor visitor) {
        if (openAPI == null) {
            return null;
        }
        if (visiting.contains(openAPI)) {
            return openAPI;
        }
        if (visitedMap.containsKey(openAPI)) {
            return (OpenAPI)visitedMap.get(openAPI);
        }
        visiting.add(openAPI);

        OpenAPI resolved = openAPI;

        Paths paths = traversePaths(resolved.getPaths(), visitor);
        if (paths != null) {
            resolved.paths(paths);
        }
        Components components = traverseComponents(resolved.getComponents(), visitor);
        if (components != null) {
            resolved.components(components);
        }

        traverseMap(resolved.getWebhooks(), visitor, this::traversePathItem);

        visitedMap.put(openAPI, resolved);
        visiting.remove(openAPI);
        return resolved;
    }

    public Components traverseComponents(Components components, Visitor visitor) {
        if (components == null) {
            return null;
        }
        if (visiting.contains(components)) {
            return components;
        }

        if (visitedMap.containsKey(components)) {
            return (Components)visitedMap.get(components);
        }
        visiting.add(components);
        Components resolved = components;
        traverseMap(resolved.getPathItems(), visitor, this::traversePathItem);
        traverseMap(resolved.getParameters(), visitor, this::traverseParameter);
        traverseMap(resolved.getCallbacks(), visitor, this::traverseCallback);
        traverseMap(resolved.getRequestBodies(), visitor, this::traverseRequestBody);
        traverseMap(resolved.getSecuritySchemes(), visitor, this::traverseSecurityScheme);
        traverseSchemaMap(resolved.getSchemas(), visitor, new ArrayList<>());
        traverseMap(resolved.getHeaders(), visitor, this::traverseHeader);
        traverseMap(resolved.getLinks(), visitor, this::traverseLink);
        traverseMap(resolved.getResponses(), visitor, this::traverseResponse);

        traverseMap(resolved.getExamples(), visitor, this::traverseExample);
        visitedMap.put(components, resolved);
        visiting.remove(components);
        return resolved;
    }

    public Paths traversePaths(Paths paths, Visitor visitor) {
        if (paths == null) {
            return null;
        }
        if (visiting.contains(paths)) {
            return paths;
        }

        if (visitedMap.containsKey(paths)) {
            return (Paths)visitedMap.get(paths);
        }
        visiting.add(paths);
        Paths resolved = paths;
        traverseMap(resolved, visitor, this::traversePathItem);
        visitedMap.put(paths, resolved);
        visiting.remove(paths);
        return resolved;
    }

    public Operation traverseOperation(Operation operation, Visitor visitor) {
        if (operation == null) {
            return null;
        }
        if (visiting.contains(operation)) {
            return operation;
        }

        if (visitedMap.containsKey(operation)) {
            return (Operation)visitedMap.get(operation);
        }
        visiting.add(operation);
        Operation resolved = operation;

        if (resolved.getParameters() != null) {
            for (int i = 0; i < resolved.getParameters().size(); i++) {
                Parameter resolvedParameter = traverseParameter(resolved.getParameters().get(i), visitor);
                if (resolvedParameter != null) {
                    resolved.getParameters().set(i, resolvedParameter);
                }
            }
        }
        if (resolved.getRequestBody() != null) {
            RequestBody resolvedRequestBody = traverseRequestBody(resolved.getRequestBody(), visitor);
            if (resolvedRequestBody != null) {
                resolved.setRequestBody(resolvedRequestBody);
            }

        }

        if (resolved.getResponses() != null) {
            ApiResponses resolvedResponses = traverseResponses(resolved.getResponses(), visitor);
            if (resolvedResponses != null) {
                resolved.setResponses(resolvedResponses);
            }
        }

        traverseMap(resolved.getCallbacks(), visitor, this::traverseCallback);

        visitedMap.put(operation, resolved);
        visiting.remove(operation);
        return resolved;
    }

    public ApiResponses traverseResponses(ApiResponses responses, Visitor visitor) {
        if (responses == null) {
            return null;
        }
        if (visiting.contains(responses)) {
            return responses;
        }

        if (visitedMap.containsKey(responses)) {
            return (ApiResponses)visitedMap.get(responses);
        }
        visiting.add(responses);
        ApiResponses resolved = responses;
        traverseMap(resolved, visitor, this::traverseResponse);
        visitedMap.put(responses, resolved);
        visiting.remove(responses);
        return resolved;
    }


    public ApiResponse traverseResponse(ApiResponse response, Visitor visitor) {
        if (response == null) {
            return null;
        }
        if (visiting.contains(response)) {
            return response;
        }

        if (visitedMap.containsKey(response)) {
            return (ApiResponse)visitedMap.get(response);
        }
        visiting.add(response);
        ApiResponse resolved = response;

        traverseMap(resolved.getContent(), visitor, this::traverseMediaType);

        traverseMap(resolved.getHeaders(), visitor, this::traverseHeader);

        traverseMap(resolved.getLinks(), visitor, this::traverseLink);

        // TODO ignored as no dereference for servers atm
        // resolved.getServers()

        visitedMap.put(response, deepcopy(resolved, ApiResponse.class));
        visiting.remove(response);
        return resolved;
    }

    public RequestBody traverseRequestBody(RequestBody requestBody, Visitor visitor) {
        if (requestBody == null) {
            return null;
        }
        if (visiting.contains(requestBody)) {
            return requestBody;
        }

        if (visitedMap.containsKey(requestBody)) {
            return (RequestBody)visitedMap.get(requestBody);
        }
        visiting.add(requestBody);
        RequestBody resolved = requestBody;

        traverseMap(resolved.getContent(), visitor, this::traverseMediaType);

        visitedMap.put(requestBody, deepcopy(resolved, RequestBody.class));
        visiting.remove(requestBody);
        return resolved;
    }

    /*
     *
     * constructs accepting $ref
     *
     */

    public PathItem traversePathItem(PathItem pathItem, Visitor visitor) {
        if (pathItem == null) {
            return null;
        }
        if (visiting.contains(pathItem)) {
            return pathItem;
        }

        if (visitedMap.containsKey(pathItem)) {
            return (PathItem) visitedMap.get(pathItem);
        }
        visiting.add(pathItem);
        PathItem resolved = pathItem;
        Operation getOp = resolved.getGet();
        Operation resolvedOperation = traverseOperation(getOp, visitor);
        if (resolvedOperation != null) {
            resolved.setGet(resolvedOperation);
        }
        Operation putOp = resolved.getPut();
        resolvedOperation = traverseOperation(putOp, visitor);
        if (resolvedOperation != null) {
            resolved.setPut(resolvedOperation);
        }
        Operation deleteOp = resolved.getDelete();
        resolvedOperation = traverseOperation(deleteOp, visitor);
        if (resolvedOperation != null) {
            resolved.setDelete(resolvedOperation);
        }
        Operation patchOp = resolved.getPatch();
        resolvedOperation = traverseOperation(patchOp, visitor);
        if (resolvedOperation != null) {
            resolved.setPatch(resolvedOperation);
        }
        Operation optionsOp = resolved.getOptions();
        resolvedOperation = traverseOperation(optionsOp, visitor);
        if (resolvedOperation != null) {
            resolved.setOptions(resolvedOperation);
        }
        Operation headOp = resolved.getHead();
        resolvedOperation = traverseOperation(headOp, visitor);
        if (resolvedOperation != null) {
            resolved.setHead(resolvedOperation);
        }
        Operation postOp = resolved.getPost();
        resolvedOperation = traverseOperation(postOp, visitor);
        if (resolvedOperation != null) {
            resolved.setPost(resolvedOperation);
        }
        Operation traceOp = resolved.getTrace();
        resolvedOperation = traverseOperation(traceOp, visitor);
        if (resolvedOperation != null) {
            resolved.setTrace(resolvedOperation);
        }

        if (resolved.getParameters() != null) {
            for (int i = 0; i < resolved.getParameters().size(); i++) {
                Parameter resolvedParameter = traverseParameter(resolved.getParameters().get(i), visitor);
                if (resolvedParameter != null) {
                    resolved.getParameters().set(i, resolvedParameter);
                }
            }
        }

        visitedMap.put(pathItem, deepcopy(resolved, PathItem.class));
        visiting.remove(pathItem);
        return resolved;
    }

    public Parameter traverseParameter(Parameter parameter, Visitor visitor) {
        if (parameter == null) {
            return null;
        }
        if (visiting.contains(parameter)) {
            return parameter;
        }

        if (visitedMap.containsKey(parameter)) {
            return (Parameter)visitedMap.get(parameter);
        }
        visiting.add(parameter);
        Parameter resolved = parameter;

        traverseMap(resolved.getContent(), visitor, this::traverseMediaType);
        if (resolved.getSchema() != null) {
            Schema schema = traverseSchema(resolved.getSchema(), visitor, new ArrayList<>());
            if (schema != null) {
                resolved.setSchema(schema);
            }
        }

        traverseMap(resolved.getExamples(), visitor, this::traverseExample);

        visitedMap.put(parameter, deepcopy(resolved, Parameter.class));
        visiting.remove(parameter);
        return resolved;

    }

    public Example traverseExample(Example example, Visitor visitor) {
        if (example == null) {
            return null;
        }
        if (visiting.contains(example)) {
            return example;
        }

        if (visitedMap.containsKey(example)) {
            return (Example)visitedMap.get(example);
        }
        visiting.add(example);
        Example resolved = example;

        visitedMap.put(example, deepcopy(resolved, Example.class));
        visiting.remove(example);
        return resolved;

    }

    public Callback traverseCallback(Callback callback, Visitor visitor) {

        if (callback == null) {
            return null;
        }
        if (visiting.contains(callback)) {
            return callback;
        }

        if (visitedMap.containsKey(callback)) {
            return (Callback)visitedMap.get(callback);
        }
        visiting.add(callback);
        traverseMap(callback, visitor, this::traversePathItem);
        visitedMap.put(callback, callback);
        visiting.remove(callback);
        return callback;
    }

    public MediaType traverseMediaType(MediaType mediaType, Visitor visitor) {
        if (mediaType == null) {
            return null;
        }
        if (visiting.contains(mediaType)) {
            return mediaType;
        }

        if (visitedMap.containsKey(mediaType)) {
            return (MediaType)visitedMap.get(mediaType);
        }
        visiting.add(mediaType);
        MediaType resolved = mediaType;

        if (resolved.getSchema() != null) {
            traverseSchema(resolved.getSchema(), visitor, new ArrayList<>());
        }
        traverseMap(resolved.getEncoding(), visitor, this::traverseEncoding);
        traverseMap(resolved.getExamples(), visitor, this::traverseExample);
        visitedMap.put(mediaType, resolved);
        visiting.remove(mediaType);
        return resolved;
    }

    public Encoding traverseEncoding(Encoding encoding, Visitor visitor) {
        if (encoding == null) {
            return null;
        }
        if (visiting.contains(encoding)) {
            return encoding;
        }

        if (visitedMap.containsKey(encoding)) {
            return (Encoding)visitedMap.get(encoding);
        }
        visiting.add(encoding);
        Encoding resolved = encoding;

        traverseMap(resolved.getHeaders(), visitor, this::traverseHeader);
        visitedMap.put(encoding, resolved);
        visiting.remove(encoding);
        return resolved;

    }

    public Header traverseHeader(Header header, Visitor visitor) {
        if (header == null) {
            return null;
        }
        if (visiting.contains(header)) {
            return header;
        }

        if (visitedMap.containsKey(header)) {
            return (Header)visitedMap.get(header);
        }
        visiting.add(header);
        Header resolved = header;

        traverseMap(resolved.getContent(), visitor, this::traverseMediaType);
        if (resolved.getSchema() != null) {
            traverseSchema(resolved.getSchema(), visitor, new ArrayList<>());
        }

        visitedMap.put(header, deepcopy(resolved, Header.class));
        visiting.remove(header);
        return resolved;
    }

    public SecurityScheme traverseSecurityScheme(SecurityScheme securityScheme, Visitor visitor) {
        if (securityScheme == null) {
            return null;
        }
        if (visiting.contains(securityScheme)) {
            return securityScheme;
        }

        if (visitedMap.containsKey(securityScheme)) {
            return (SecurityScheme)visitedMap.get(securityScheme);
        }
        visiting.add(securityScheme);
        SecurityScheme resolved = securityScheme;
        visitedMap.put(securityScheme, deepcopy(resolved, SecurityScheme.class));
        visiting.remove(securityScheme);
        return resolved;
    }

    public Link traverseLink(Link link, Visitor visitor) {
        if (link == null) {
            return null;
        }
        if (visiting.contains(link)) {
            return link;
        }

        if (visitedMap.containsKey(link)) {
            return (Link)visitedMap.get(link);
        }
        visiting.add(link);
        Link resolved = link;

        traverseMap(resolved.getHeaders(), visitor, this::traverseHeader);
        visitedMap.put(link, deepcopy(resolved, Link.class));
        visiting.remove(link);
        return resolved;
    }

    public Schema traverseSchema(Schema schema, Visitor visitor, List<String> inheritedIds) {
        if (schema == null) {
            return null;
        }
        if (visiting.contains(schema)) {
            return schema;
        }
        if (visitedMap.containsKey(schema)) {
            return (Schema)visitedMap.get(schema);
        }
        visiting.add(schema);

        if (StringUtils.isNotBlank(schema.get$id())) {
            inheritedIds.add(schema.get$id());
            try {
                String resolvedURI = context.currentUri;
                for (String id : inheritedIds) {
                    String urlWithoutHash = ReferenceUtils.toBaseURI(id);
                    resolvedURI = ReferenceUtils.resolve(urlWithoutHash, resolvedURI);
                    resolvedURI = ReferenceUtils.toBaseURI(resolvedURI);
                }
                context.getIdsCache().put(resolvedURI, Json31.pretty(schema));
            } catch (Exception e) {
                //
            }
        }
        Schema resolved = schema;

        traverseSchemaMap(resolved.getProperties(), visitor, inheritedIds);

        if (resolved.getAdditionalItems() != null) {
            Schema traversedSchema = traverseSchema(resolved.getAdditionalItems(), visitor, inheritedIds);
            if (traversedSchema != null) {
                resolved.setAdditionalItems(traversedSchema);
            }
        }
        if (resolved.getAdditionalProperties() != null && resolved.getAdditionalProperties() instanceof Schema) {
            Schema traversedSchema = traverseSchema((Schema)resolved.getAdditionalProperties(), visitor, inheritedIds);
            if (traversedSchema != null) {
                resolved.setAdditionalProperties(traversedSchema);
            }
        }
        if (resolved.getAllOf() != null) {
            for (int i = 0; i < resolved.getAllOf().size(); i++) {
                Schema resolvedSchema = traverseSchema((Schema)resolved.getAllOf().get(i), visitor, inheritedIds);
                if (resolvedSchema != null) {
                    resolved.getAllOf().set(i, resolvedSchema);
                }
            }
        }
        if (resolved.getAnyOf() != null) {
            for (int i = 0; i < resolved.getAnyOf().size(); i++) {
                Schema resolvedSchema = traverseSchema((Schema)resolved.getAnyOf().get(i), visitor, inheritedIds);
                if (resolvedSchema != null) {
                    resolved.getAnyOf().set(i, resolvedSchema);
                }
            }
        }
        if (resolved.getContains() != null) {
            Schema traversedSchema = traverseSchema(resolved.getContains(), visitor, inheritedIds);
            if (traversedSchema != null) {
                resolved.setContains(traversedSchema);
            }
        }
        if (resolved.getContentSchema() != null) {
            Schema traversedSchema = traverseSchema(resolved.getContentSchema(), visitor, inheritedIds);
            if (traversedSchema != null) {
                resolved.setContentSchema(traversedSchema);
            }
        }
        traverseSchemaMap(resolved.getDependentSchemas(), visitor, inheritedIds);
        if (resolved.getElse() != null) {
            Schema traversedSchema = traverseSchema(resolved.getElse(), visitor, inheritedIds);
            if (traversedSchema != null) {
                resolved.setElse(traversedSchema);
            }
        }
        if (resolved.getIf() != null) {
            Schema traversedSchema = traverseSchema(resolved.getIf(), visitor, inheritedIds);
            if (traversedSchema != null) {
                resolved.setIf(traversedSchema);
            }
        }
        if (resolved.getItems() != null) {
            Schema traversedSchema = traverseSchema(resolved.getItems(), visitor, inheritedIds);
            if (traversedSchema != null) {
                resolved.setItems(traversedSchema);
            }
        }
        if (resolved.getNot() != null) {
            Schema traversedSchema = traverseSchema(resolved.getNot(), visitor, inheritedIds);
            if (traversedSchema != null) {
                resolved.setNot(traversedSchema);
            }
        }
        if (resolved.getOneOf() != null) {
            for (int i = 0; i < resolved.getOneOf().size(); i++) {
                Schema resolvedSchema = traverseSchema((Schema)resolved.getOneOf().get(i), visitor, inheritedIds);
                if (resolvedSchema != null) {
                    resolved.getOneOf().set(i, resolvedSchema);
                }
            }
        }
        traverseSchemaMap(resolved.getPatternProperties(), visitor, inheritedIds);
        if (resolved.getPrefixItems() != null) {
            for (int i = 0; i < resolved.getPrefixItems().size(); i++) {
                Schema resolvedSchema = traverseSchema((Schema)resolved.getPrefixItems().get(i), visitor, inheritedIds);
                if (resolvedSchema != null) {
                    resolved.getPrefixItems().set(i, resolvedSchema);
                }
            }
        }
        if (resolved.getThen() != null) {
            Schema traversedSchema = traverseSchema(resolved.getThen(), visitor, inheritedIds);
            if (traversedSchema != null) {
                resolved.setThen(traversedSchema);
            }
        }
        if (resolved.getUnevaluatedItems() != null) {
            Schema traversedSchema = traverseSchema(resolved.getUnevaluatedItems(), visitor, inheritedIds);
            if (traversedSchema != null) {
                resolved.setUnevaluatedItems(traversedSchema);
            }
        }
        if (resolved.getAdditionalProperties() != null && resolved.getUnevaluatedProperties() instanceof Schema) {
            Schema traversedSchema = traverseSchema((Schema)resolved.getUnevaluatedProperties(), visitor, inheritedIds);
            if (traversedSchema != null) {
                resolved.setUnevaluatedProperties(traversedSchema);
            }
        }

        if (StringUtils.isNotBlank(schema.get$id())) {
            inheritedIds.remove(schema.get$id());
        }
        visitedMap.put(schema, deepcopy(resolved, Schema.class));
        visiting.remove(schema);
        return resolved;

    }

    public <T> T deepcopy(T entity, Class<T> clazz) {
        try {
            return (T)Json31.mapper().readValue(Json31.mapper().writeValueAsString(entity), clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public <T> void traverseMap(Map<String, T> map, Visitor visitor, BiFunction<T, Visitor, T> traverser) {
        if (map != null) {
            Map<String, T> copy = new LinkedHashMap<>(map);
            for (String key : copy.keySet()) {
                T entity = copy.get(key);
                T resolved = traverser.apply(entity, visitor);
                if (resolved != null) {
                    map.put(key, resolved);
                }
            }
        }
    }

    public void traverseSchemaMap(Map<String, Schema> map, Visitor visitor, List<String> inheritedIds) {
        if (map != null) {
            Map<String, Schema> copy = new LinkedHashMap<>(map);
            for (String key : copy.keySet()) {
                Schema entity = copy.get(key);
                Schema resolved = traverseSchema(entity, visitor, inheritedIds);
                if (resolved != null) {
                    map.put(key, resolved);
                }
            }
        }
    }
}
