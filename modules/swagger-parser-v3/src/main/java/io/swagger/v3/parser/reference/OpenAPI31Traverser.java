package io.swagger.v3.parser.reference;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import io.swagger.v3.parser.util.DeserializationUtils;
import io.swagger.v3.parser.util.OpenAPIDeserializer;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

public class OpenAPI31Traverser implements Traverser {

    private DereferencerContext context;

    public OpenAPI31Traverser(DereferencerContext context) {
        this.context = context;
    }

    public DereferencerContext getContext() {
        return context;
    }

    public void setContext(DereferencerContext context) {
        this.context = context;
    }

    public OpenAPI31Traverser context(DereferencerContext context) {
        this.context = context;
        return this;
    }

    public Set<Object> visiting = new HashSet<>();
    protected HashMap<Object, Object> visitedMap = new HashMap<>();

    public OpenAPI traverse(OpenAPI openAPI, Visitor visitor) throws Exception {
        if (!(visitor instanceof ReferenceVisitor)) {
            return openAPI;
        }
        return traverseOpenApi(openAPI, (ReferenceVisitor)visitor);
    }

    private OpenAPIDeserializer deserializer = new OpenAPIDeserializer();

    public <T> T deserializeFragment(JsonNode node, Class<T> expectedType, String uri, String fragment, Set<String> validationMessages) {

        String sanitizedFragment = fragment == null ? "" : fragment;
        OpenAPIDeserializer.ParseResult parseResult = new OpenAPIDeserializer.ParseResult().openapi31(true);
        T result = null;
        if (expectedType.equals(Schema.class)) {
            result = (T) deserializer.getSchema((ObjectNode) node, sanitizedFragment.replace("/", "."), parseResult);
        } else if (expectedType.equals(RequestBody.class)) {
            result = (T) deserializer.getRequestBody((ObjectNode) node, sanitizedFragment.replace("/", "."), parseResult);
        } else if (expectedType.equals(ApiResponse.class)) {
            result = (T) deserializer.getResponse((ObjectNode) node, sanitizedFragment.replace("/", "."), parseResult);
        }else if (expectedType.equals(Callback.class)) {
            result = (T) deserializer.getCallback((ObjectNode) node, sanitizedFragment.replace("/", "."), parseResult);
        }else if (expectedType.equals(Example.class)) {
            result = (T) deserializer.getExample((ObjectNode) node, sanitizedFragment.replace("/", "."), parseResult);
        }else if (expectedType.equals(Header.class)) {
            result = (T) deserializer.getHeader((ObjectNode) node, sanitizedFragment.replace("/", "."), parseResult);
        }else if (expectedType.equals(Link.class)) {
            result = (T) deserializer.getLink((ObjectNode) node, sanitizedFragment.replace("/", "."), parseResult);
        }else if (expectedType.equals(Parameter.class)) {
            result = (T) deserializer.getParameter((ObjectNode) node, sanitizedFragment.replace("/", "."), parseResult);
        }else if (expectedType.equals(SecurityScheme.class)) {
            result = (T) deserializer.getSecurityScheme((ObjectNode) node, sanitizedFragment.replace("/", "."), parseResult);
        }else if (expectedType.equals(PathItem.class)) {
            result = (T) deserializer.getPathItem((ObjectNode) node, sanitizedFragment.replace("/", "."), parseResult);
        }
        parseResult.getMessages().forEach((m) -> {
            validationMessages.add(m + " (" + uri + ")");
        });
        if (result != null) {
            return result;
        }
        // TODO ensure core deserialization exceptions get added to result messages resolveValidationMessages
        return DeserializationUtils.deserialize(node, uri, expectedType);
    }


    public OpenAPI traverseOpenApi(OpenAPI openAPI, ReferenceVisitor visitor) {
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

        OpenAPI resolved = visitor.visitOpenApi(openAPI);

        if (resolved == null) {
            resolved = openAPI;
        }
        Paths paths = traversePaths(resolved.getPaths(), visitor);
        if (paths != null) {
            resolved.paths(paths);
        }
        Components components = traverseComponents(resolved.getComponents(), visitor);
        if (components != null) {
            resolved.components(components);
        }

        traverseMap(resolved.getWebhooks(), visitor, this::traversePathItem);

        // resolved.getServers();

        // TODO ignored as we are not dereferencing URLs pointing to generic content atm
        // resolved.getInfo();
        // resolved.getExternalDocs();
        // resolved.getTags();
        // resolved.getSecurity()
        visitedMap.put(openAPI, resolved);
        visiting.remove(openAPI);
        return resolved;
    }

    public Components traverseComponents(Components components, ReferenceVisitor visitor) {
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
        Components resolved = visitor.visitComponents(components);
        if (resolved == null) {
            resolved = components;
        }

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

    public Paths traversePaths(Paths paths, ReferenceVisitor visitor) {
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
        Paths resolved = visitor.visitPaths(paths);
        if (resolved == null) {
            resolved = paths;
        }

        traverseMap(resolved, visitor, this::traversePathItem);
        visitedMap.put(paths, resolved);
        visiting.remove(paths);
        return resolved;
    }

    public Operation traverseOperation(Operation operation, ReferenceVisitor visitor) {
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
        Operation resolved = visitor.visitOperation(operation);
        if (resolved == null) {
            resolved = operation;
        }

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

        // ignored
        // resolved.getServers();
        // resolved.getSecurity()

        visitedMap.put(operation, resolved);
        visiting.remove(operation);
        return resolved;
    }

    public ApiResponses traverseResponses(ApiResponses responses, ReferenceVisitor visitor) {
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
        ApiResponses resolved = visitor.visitResponses(responses);
        if (resolved == null) {
            resolved = responses;
        }
        traverseMap(resolved, visitor, this::traverseResponse);
        visitedMap.put(responses, resolved);
        visiting.remove(responses);
        return resolved;
    }


    public ApiResponse traverseResponse(ApiResponse response, ReferenceVisitor visitor) {
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
        ApiResponse resolved = visitor.visitResponse(response);

        boolean resolvedNotNull = false;

        if (resolved == null) {
            resolved = response;
        } else {
            resolvedNotNull = true;
        }

        traverseMap(resolved.getContent(), visitor, this::traverseMediaType);

        traverseMap(resolved.getHeaders(), visitor, this::traverseHeader);

        traverseMap(resolved.getLinks(), visitor, this::traverseLink);

        // TODO ignored as no dereference for servers atm
        // resolved.getServers()

        // only if this is root and local ref
        if (shouldHandleRootLocalRefs(resolvedNotNull, response.get$ref(), visitor)) {
            ensureComponents(context.getOpenApi());
            if (context.getOpenApi().getComponents().getResponses() == null) context.getOpenApi().getComponents().responses(new LinkedHashMap<>());
            visitedMap.put(response, deepcopy(response, ApiResponse.class));
            visiting.remove(response);
            return handleRootLocalRefs(response.get$ref(), resolved, context.getOpenApi().getComponents().getResponses());
        }
        // merge stuff
        if (response.getDescription() != null) {
            resolved.description(response.getDescription());
        }
        visitedMap.put(response, deepcopy(resolved, ApiResponse.class));
        visiting.remove(response);
        return resolved;
    }

    public RequestBody traverseRequestBody(RequestBody requestBody, ReferenceVisitor visitor) {
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
        RequestBody resolved = visitor.visitRequestBody(requestBody);
        boolean resolvedNotNull = false;

        if (resolved == null) {
            resolved = requestBody;
        } else {
            resolvedNotNull = true;
        }

        traverseMap(resolved.getContent(), visitor, this::traverseMediaType);

        // only if this is root and local ref
        if (shouldHandleRootLocalRefs(resolvedNotNull, requestBody.get$ref(), visitor)) {
            ensureComponents(context.getOpenApi());
            if (context.getOpenApi().getComponents().getRequestBodies() == null) context.getOpenApi().getComponents().requestBodies(new LinkedHashMap<>());
            visitedMap.put(requestBody, deepcopy(requestBody, RequestBody.class));
            visiting.remove(requestBody);
            return handleRootLocalRefs(requestBody.get$ref(), resolved, context.getOpenApi().getComponents().getRequestBodies());
        }
        // merge stuff
        if (requestBody.getDescription() != null) {
            resolved.description(requestBody.getDescription());
        }
        visitedMap.put(requestBody, deepcopy(resolved, RequestBody.class));
        visiting.remove(requestBody);
        return resolved;
    }

    /*
     *
     * constructs accepting $ref
     *
     */

    public PathItem traversePathItem(PathItem pathItem, ReferenceVisitor visitor) {
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
        PathItem resolved = visitor.visitPathItem(pathItem);

        boolean resolvedNotNull = false;

        if (resolved == null) {
            resolved = pathItem;
        } else {
            resolvedNotNull = true;
        }

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

        // TODO ignored as no dereference for servers atm
        // resolved.getServers()

        // only if this is root and local ref
        if (shouldHandleRootLocalRefs(resolvedNotNull, pathItem.get$ref(), visitor)) {
            ensureComponents(context.getOpenApi());
            if (context.getOpenApi().getComponents().getPathItems() == null) context.getOpenApi().getComponents().pathItems(new LinkedHashMap<>());
            visitedMap.put(pathItem, deepcopy(pathItem, PathItem.class));
            visiting.remove(pathItem);
            return handleRootLocalRefs(pathItem.get$ref(), resolved, context.getOpenApi().getComponents().getPathItems());
        }

        // merge stuff
        if (pathItem.getParameters() != null) {
            resolved.parameters(pathItem.getParameters());
        }
        if (pathItem.getDescription() != null) {
            resolved.description(pathItem.getDescription());
        }
        if (pathItem.getSummary() != null) {
            resolved.summary(pathItem.getSummary());
        }
        // TODO additional undefined merge if other props are defined

        visitedMap.put(pathItem, deepcopy(resolved, PathItem.class));
        visiting.remove(pathItem);
        return resolved;
    }

    public Parameter traverseParameter(Parameter parameter, ReferenceVisitor visitor) {
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
        Parameter resolved = visitor.visitParameter(parameter);

        boolean resolvedNotNull = false;

        if (resolved == null) {
            resolved = parameter;
        } else {
            resolvedNotNull = true;
        }

        traverseMap(resolved.getContent(), visitor, this::traverseMediaType);
        if (resolved.getSchema() != null) {
            Schema schema = traverseSchema(resolved.getSchema(), visitor, new ArrayList<>());
            if (schema != null) {
                resolved.setSchema(schema);
            }
        }

        traverseMap(resolved.getExamples(), visitor, this::traverseExample);

        // only if this is root and local ref
        if (shouldHandleRootLocalRefs(resolvedNotNull, parameter.get$ref(), visitor)) {
            ensureComponents(context.getOpenApi());
            if (context.getOpenApi().getComponents().getParameters() == null) context.getOpenApi().getComponents().parameters(new LinkedHashMap<>());
            visitedMap.put(parameter, deepcopy(parameter, Parameter.class));
            visiting.remove(parameter);
            return handleRootLocalRefs(parameter.get$ref(), resolved, context.getOpenApi().getComponents().getParameters());
        }
        // merge stuff
        if (parameter.getDescription() != null) {
            resolved.description(parameter.getDescription());
        }
        visitedMap.put(parameter, deepcopy(resolved, Parameter.class));
        visiting.remove(parameter);
        return resolved;

    }

    public Example traverseExample(Example example, ReferenceVisitor visitor) {
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
        Example resolved = visitor.visitExample(example);

        boolean resolvedNotNull = false;

        if (resolved == null) {
            resolved = example;
        } else {
            resolvedNotNull = true;
        }

        // only if this is root and local ref
        if (shouldHandleRootLocalRefs(resolvedNotNull, example.get$ref(), visitor)) {
            ensureComponents(context.getOpenApi());
            if (context.getOpenApi().getComponents().getExamples() == null) context.getOpenApi().getComponents().examples(new LinkedHashMap<>());
            visitedMap.put(example, deepcopy(example, Example.class));
            visiting.remove(example);
            return handleRootLocalRefs(example.get$ref(), resolved, context.getOpenApi().getComponents().getExamples());
        }
        // merge stuff
        if (example.getDescription() != null) {
            resolved.description(example.getDescription());
        }
        if (example.getSummary() != null) {
            resolved.summary(example.getSummary());
        }
        visitedMap.put(example, deepcopy(resolved, Example.class));
        visiting.remove(example);
        return resolved;

    }

    public Callback traverseCallback(Callback callback, ReferenceVisitor visitor) {

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

    public MediaType traverseMediaType(MediaType mediaType, ReferenceVisitor visitor) {
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
        MediaType resolved = visitor.visitMediaType(mediaType);

        if (resolved == null) {
            resolved = mediaType;
        }

        if (resolved.getSchema() != null) {
            Schema schema = traverseSchema(resolved.getSchema(), visitor, new ArrayList<>());
            if (schema != null) {
                resolved.setSchema(schema);
            }
        }
        traverseMap(resolved.getEncoding(), visitor, this::traverseEncoding);
        traverseMap(resolved.getExamples(), visitor, this::traverseExample);
        visitedMap.put(mediaType, resolved);
        visiting.remove(mediaType);
        return resolved;
    }

    public Encoding traverseEncoding(Encoding encoding, ReferenceVisitor visitor) {
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
        Encoding resolved = visitor.visitEncoding(encoding);

        if (resolved == null) {
            resolved = encoding;
        }
        traverseMap(resolved.getHeaders(), visitor, this::traverseHeader);
        visitedMap.put(encoding, resolved);
        visiting.remove(encoding);
        return resolved;

    }

    public Header traverseHeader(Header header, ReferenceVisitor visitor) {
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
        Header resolved = visitor.visitHeader(header);

        boolean resolvedNotNull = false;

        if (resolved == null) {
            resolved = header;
        } else {
            resolvedNotNull = true;
        }
        traverseMap(resolved.getContent(), visitor, this::traverseMediaType);
        if (resolved.getSchema() != null) {
            Schema schema = traverseSchema(resolved.getSchema(), visitor, new ArrayList<>());
            if (schema != null) {
                resolved.setSchema(schema);
            }
        }

        // TODO ignored as we are not dereferencing URLs pointing to generic content atm
        // resolved.getExamples();
        //resolved.getExample();

        // only if this is root and local ref
        if (shouldHandleRootLocalRefs(resolvedNotNull, header.get$ref(), visitor)) {
            ensureComponents(context.getOpenApi());
            if (context.getOpenApi().getComponents().getHeaders() == null) context.getOpenApi().getComponents().headers(new LinkedHashMap<>());
            visitedMap.put(header, deepcopy(header, Header.class));
            visiting.remove(header);
            return handleRootLocalRefs(header.get$ref(), resolved, context.getOpenApi().getComponents().getHeaders());
        }
        // merge stuff
        if (header.getDescription() != null) {
            resolved.description(header.getDescription());
        }
        visitedMap.put(header, deepcopy(resolved, Header.class));
        visiting.remove(header);
        return resolved;
    }

    public SecurityScheme traverseSecurityScheme(SecurityScheme securityScheme, ReferenceVisitor visitor) {
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
        SecurityScheme resolved = visitor.visitSecurityScheme(securityScheme);
        boolean resolvedNotNull = false;

        if (resolved == null) {
            resolved = securityScheme;
        } else {
            resolvedNotNull = true;
        }

        // TODO ignored as we are not dereferencing URLs pointing to generic content atm
        // resolved.getExamples();
        //resolved.getExample();

        // only if this is root and local ref
        if (shouldHandleRootLocalRefs(resolvedNotNull, securityScheme.get$ref(), visitor)) {
            ensureComponents(context.getOpenApi());
            if (context.getOpenApi().getComponents().getSecuritySchemes() == null) context.getOpenApi().getComponents().securitySchemes(new LinkedHashMap<>());
            visitedMap.put(securityScheme, deepcopy(securityScheme, SecurityScheme.class));
            visiting.remove(securityScheme);
            return handleRootLocalRefs(securityScheme.get$ref(), resolved, context.getOpenApi().getComponents().getSecuritySchemes());
        }
        // merge stuff
        if (securityScheme.getDescription() != null) {
            resolved.description(securityScheme.getDescription());
        }
        visitedMap.put(securityScheme, deepcopy(resolved, SecurityScheme.class));
        visiting.remove(securityScheme);
        return resolved;
    }

    public Link traverseLink(Link link, ReferenceVisitor visitor) {
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
        Link resolved = visitor.visitLink(link);

        boolean resolvedNotNull = false;

        if (resolved == null) {
            resolved = link;
        } else {
            resolvedNotNull = true;
        }

        traverseMap(resolved.getHeaders(), visitor, this::traverseHeader);

        // TODO ignored as no dereference for servers atm
        // resolved.getServers()

        // only if this is root and local ref
        if (shouldHandleRootLocalRefs(resolvedNotNull, link.get$ref(), visitor)) {
            ensureComponents(context.getOpenApi());
            if (context.getOpenApi().getComponents().getLinks() == null) context.getOpenApi().getComponents().links(new LinkedHashMap<>());
            visitedMap.put(link, deepcopy(link, Link.class));
            visiting.remove(link);
            return handleRootLocalRefs(link.get$ref(), resolved, context.getOpenApi().getComponents().getLinks());
        }
        // merge stuff
        if (link.getDescription() != null) {
            resolved.description(link.getDescription());
        }
        visitedMap.put(link, deepcopy(resolved, Link.class));
        visiting.remove(link);
        return resolved;
    }

    public Schema traverseSchema(Schema schema, ReferenceVisitor visitor, List<String> inheritedIds) {
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
        }
        Schema resolved = visitor.visitSchema(schema, inheritedIds);

        boolean resolvedNotNull = false;

        if (resolved == null) {
            resolved = schema;
        } else {
            resolvedNotNull = true;
        }

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


        // only if this is root and local ref
        if (shouldHandleRootLocalRefs(resolvedNotNull, schema.get$ref(), visitor)) {
            ensureComponents(context.getOpenApi());
            if (context.getOpenApi().getComponents().getSchemas() == null) context.getOpenApi().getComponents().schemas(new LinkedHashMap<>());
            visitedMap.put(schema, deepcopy(schema, Schema.class));
            visiting.remove(schema);
            return handleRootLocalRefs(schema.get$ref(), resolved, context.getOpenApi().getComponents().getSchemas());
        }
        // merge ALL STUFF
        mergeSchemas(schema, resolved);
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

    public <T> void traverseMap(Map<String, T> map, ReferenceVisitor visitor, BiFunction<T, ReferenceVisitor, T> traverser) {
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

    public void traverseSchemaMap(Map<String, Schema> map, ReferenceVisitor visitor, List<String> inheritedIds) {
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
    public <T> T handleRootLocalRefs(String ref, T entity, Map<String, T> map) {
        if (!ReferenceUtils.isLocalRefToComponents(ref) && ReferenceUtils.isAnchorRef(ref)) {
            return null;
        }
        // replace components/map and return null
        String name = ReferenceUtils.getRefName(ref);
        map.put(name, entity);
        return null;
    }

    public boolean shouldHandleRootLocalRefs(boolean resolvedNotNull, String ref, ReferenceVisitor visitor) {

        return resolvedNotNull &&
                ReferenceUtils.isLocalRef(ref) &&
                !this.getContext().getParseOptions().isResolveFully() &&
                visitor.reference.getUri().equals(this.getContext().getRootUri()) &&
                (ReferenceUtils.isLocalRefToComponents(ref) || ReferenceUtils.isAnchorRef(ref));
    }

    public void ensureComponents(OpenAPI openAPI) {
        if (openAPI.getComponents() == null) {
            openAPI.setComponents(new Components());
        }
    }
    public void mergeSchemas(Schema source, Schema target) {
        if (source.getDescription() != null) {
            target.description(source.getDescription());
        }
        if (source.getOneOf() != null && !source.getOneOf().isEmpty()) {
            target.oneOf(source.getOneOf());
        }
        if (source.getNot() != null) {
            target.not(source.getNot());
        }
        if (source.getAnyOf() != null && !source.getAnyOf().isEmpty()) {
            target.anyOf(source.getAnyOf());
        }
        if (source.getType() != null) {
            target.type(source.getType());
        }
        if (source.getTypes() != null && !source.getTypes().isEmpty()) {
            target.types(source.getTypes());
        }
        if (source.getFormat() != null) {
            target.format(source.getFormat());
        }
        if (source.getRequired() != null) {
            target.required(source.getRequired());
        }
        if (source.getThen() != null) {
            target.then(source.getThen());
        }
        if (source.getIf() != null) {
            target._if(source.getIf());
        }
        if (source.getElse() != null) {
            target._else(source.getElse());
        }
        if (source.getContentSchema() != null) {
            target.contentSchema(source.getContentSchema());
        }
        if (source.getContains() != null) {
            target.contains(source.getContains());
        }
        if (source.getAdditionalProperties() != null) {
            target.additionalProperties(source.getAdditionalProperties());
        }
        if (source.getUnevaluatedProperties() != null) {
            target.unevaluatedProperties(source.getUnevaluatedProperties());
        }
        if (source.getUnevaluatedItems() != null) {
            target.unevaluatedItems(source.getUnevaluatedItems());
        }
        if (source.getPrefixItems() != null && !source.getPrefixItems().isEmpty()) {
            target.prefixItems(source.getPrefixItems());
        }
        if (source.getProperties() != null && !source.getProperties().isEmpty()) {
            target.properties(source.getProperties());
        }
        if (source.getPatternProperties() != null && !source.getPatternProperties().isEmpty()) {
            target.patternProperties(source.getPatternProperties());
        }
        if (source.getPattern() != null) {
            target.pattern(source.getPattern());
        }
        if (source.getDependentSchemas() != null && !source.getDependentSchemas().isEmpty()) {
            target.dependentSchemas(source.getDependentSchemas());
        }
        if (source.getConst() != null) {
            target._const(source.getConst());
        }
        if (source.getAdditionalItems() != null) {
            target.additionalItems(source.getAdditionalItems());
        }
        if (source.getEnum() != null && !source.getEnum().isEmpty()) {
            target._enum(source.getEnum());
        }
        if (source.getReadOnly() != null){
            target.setReadOnly(source.getReadOnly());
        }
        if (source.getWriteOnly() != null){
            target.setWriteOnly(source.getWriteOnly());
        }
        if (source.getMaxLength() != null){
            target.setMaxLength(source.getMaxLength());
        }
        if (source.get$anchor() != null){
            target.set$anchor(source.get$anchor());
        }
        if (source.get$comment() != null){
            target.set$comment(source.get$comment());
        }
        if (source.get$id() != null){
            target.set$id(source.get$id());
        }
        if (source.get$schema() != null){
            target.set$schema(source.get$schema());
        }
        if (source.getContentEncoding() != null){
            target.setContentEncoding(source.getContentEncoding());
        }
        if (source.getContentMediaType() != null){
            target.setContentMediaType(source.getContentMediaType());
        }
        if (source.getDefault() != null){
            target.setDefault(source.getDefault());
        }
        if (source.getDependentRequired() != null && !source.getDependentRequired().isEmpty()){
            target.setDependentRequired(source.getDependentRequired());
        }
        if (source.getDeprecated() != null){
            target.setDeprecated(source.getDeprecated());
        }
        if (source.getDiscriminator() != null){
            target.setDiscriminator(source.getDiscriminator());
        }
        if (source.getExample() != null){
            target.setExample(source.getExample());
        }
        if (source.getExamples() != null && !source.getExamples().isEmpty()){
            target.setExamples(source.getExamples());
        }
        if (source.getExclusiveMaximum() != null){
            target.setExclusiveMaximum(source.getExclusiveMaximum());
        }
        if (source.getExclusiveMaximumValue() != null){
            target.setExclusiveMaximumValue(source.getExclusiveMaximumValue());
        }
        if (source.getExclusiveMinimum() != null){
            target.setExclusiveMinimum(source.getExclusiveMinimum());
        }
        if (source.getExclusiveMinimumValue() != null){
            target.setExclusiveMinimumValue(source.getExclusiveMinimumValue());
        }
        if (source.getExtensions() != null && !source.getExtensions().isEmpty()){
            target.setExtensions(source.getExtensions());
        }
        if (source.getExternalDocs() != null){
            target.setExternalDocs(source.getExternalDocs());
        }
        if (source.getMaxContains() != null){
            target.setMaxContains(source.getMaxContains());
        }
        if (source.getMaximum() != null){
            target.setMaximum(source.getMaximum());
        }
        if (source.getMaxItems() != null){
            target.setMaxItems(source.getMaxItems());
        }
        if (source.getMaxProperties() != null){
            target.setMaxProperties(source.getMaxProperties());
        }
        if (source.getMinContains() != null){
            target.setMinContains(source.getMinContains());
        }
        if (source.getMinItems() != null){
            target.setMinItems(source.getMinItems());
        }
        if (source.getMinProperties() != null){
            target.setMinProperties(source.getMinProperties());
        }
        if (source.getMultipleOf() != null){
            target.setMultipleOf(source.getMultipleOf());
        }
        if (source.getNullable() != null){
            target.setNullable(source.getNullable());
        }
        if (source.getPropertyNames() != null){
            target.setPropertyNames(source.getPropertyNames());
        }
        if (source.getTitle() != null){
            target.setTitle(source.getTitle());
        }
        if (source.getUniqueItems() != null){
            target.setUniqueItems(source.getUniqueItems());
        }
    }

}
