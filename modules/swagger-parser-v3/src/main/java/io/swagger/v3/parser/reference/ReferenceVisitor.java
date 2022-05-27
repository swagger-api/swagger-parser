package io.swagger.v3.parser.reference;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.links.Link;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

public class ReferenceVisitor extends AbstractVisitor {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ReferenceVisitor.class);
    protected HashSet<Object> visited;
    protected HashMap<Object, Object> visitedMap;
    protected OpenAPI31Traverser openAPITraverser;
    protected Reference reference;

    public ReferenceVisitor(
            Reference reference,
            OpenAPI31Traverser openAPITraverser,
            HashSet<Object> visited,
            HashMap<Object, Object> visitedMap) {
        this.reference = reference;
        this.openAPITraverser = openAPITraverser;
        this.visited = visited;
        this.visitedMap = visitedMap;
    }

    public String toBaseURI(String uri) throws Exception{
        return ReferenceUtils.resolve(ReferenceUtils.toBaseURI(uri), this.reference.getUri());
    }

    public Reference toReference(String uri) throws Exception{
        String baseUri = toBaseURI(uri);
        Map<String, Reference> referenceSet = this.reference.getReferenceSet();
        if (referenceSet.containsKey(baseUri)) {
            return referenceSet.get(baseUri);
        }
        JsonNode node = ReferenceUtils.parse(baseUri, this.reference.getAuths());

        Reference ref = new Reference()
                .auths(this.reference.getAuths())
                .jsonNode(node)
                .uri(baseUri)
                .referenceSet(referenceSet)
                .messages(this.reference.getMessages());
        referenceSet.put(baseUri, ref);
        return ref;
    }

    public Reference toSchemaReference(String baseUri, JsonNode node) throws Exception{
        Map<String, Reference> referenceSet = this.reference.getReferenceSet();
        if (referenceSet.containsKey(baseUri)) {
            return referenceSet.get(baseUri);
        }

        Reference ref = new Reference()
                .auths(this.reference.getAuths())
                .jsonNode(node)
                .uri(baseUri)
                .referenceSet(referenceSet)
                .messages(this.reference.getMessages());
        referenceSet.put(baseUri, ref);
        return ref;
    }

    @Override
    public PathItem visitPathItem(PathItem pathItem){

        if (StringUtils.isBlank(pathItem.get$ref())) {
            return null;
        }
        return resolveRef(pathItem, pathItem.get$ref(), PathItem.class, openAPITraverser::traversePathItem);

    }

    @Override
    public Parameter visitParameter(Parameter parameter){

        if (StringUtils.isBlank(parameter.get$ref())) {
            return null;
        }

        return resolveRef(parameter, parameter.get$ref(), Parameter.class, openAPITraverser::traverseParameter);

    }

    @Override
    public Example visitExample(Example example){

        if (StringUtils.isBlank(example.get$ref())) {
            return null;
        }

        return resolveRef(example, example.get$ref(), Example.class, openAPITraverser::traverseExample);

    }

    @Override
    public Schema visitSchema(Schema schema, List<String> inheritedIds){

        if (StringUtils.isBlank(schema.get$ref())) {
            return null;
        }

        return resolveSchemaRef(schema, schema.get$ref(), inheritedIds);

    }

    @Override
    public ApiResponse visitResponse(ApiResponse response){

        if (StringUtils.isBlank(response.get$ref())) {
            return null;
        }

        return resolveRef(response, response.get$ref(), ApiResponse.class, openAPITraverser::traverseResponse);

    }

    @Override
    public RequestBody visitRequestBody(RequestBody requestBody){

        if (StringUtils.isBlank(requestBody.get$ref())) {
            return null;
        }

        return resolveRef(requestBody, requestBody.get$ref(), RequestBody.class, openAPITraverser::traverseRequestBody);
    }

    @Override
    public Link visitLink(Link link){
        if (StringUtils.isBlank(link.get$ref())) {
            return null;
        }

        return resolveRef(link, link.get$ref(), Link.class, openAPITraverser::traverseLink);

    }

    @Override
    public SecurityScheme visitSecurityScheme(SecurityScheme securityScheme){
        if (StringUtils.isBlank(securityScheme.get$ref())) {
            return null;
        }

        return resolveRef(securityScheme, securityScheme.get$ref(), SecurityScheme.class, openAPITraverser::traverseSecurityScheme);

    }

    @Override
    public Header visitHeader(Header header){
        if (StringUtils.isBlank(header.get$ref())) {
            return null;
        }

        return resolveRef(header, header.get$ref(), Header.class, openAPITraverser::traverseHeader);
    }

    public<T> T resolveRef(T visiting, String ref, Class<T> clazz, BiFunction<T, ReferenceVisitor, T> traverseFunction){
        try {
            Reference reference = toReference(ref);
            String fragment = ReferenceUtils.getFragment(ref);
            JsonNode node = ReferenceUtils.jsonPointerEvaluate(fragment, reference.getJsonNode(), ref);
            T resolved = openAPITraverser.deserializeFragment(node, clazz, ref, fragment, reference.getMessages());
            ReferenceVisitor visitor = new ReferenceVisitor(reference, openAPITraverser, this.visited, this.visitedMap);
            return traverseFunction.apply(resolved, visitor);

        } catch (Exception e) {
            LOGGER.error("Error resolving " + ref, e);
            this.reference.getMessages().add(e.getMessage());
            return null;
        }
    }

    public Schema resolveSchemaRef(Schema visiting, String ref, List<String> inheritedIds){
        try {
            String baseURI = this.reference.getUri();
            for (String id: inheritedIds) {
                String urlWithoutHash = ReferenceUtils.toBaseURI(id);
                baseURI = ReferenceUtils.resolve(urlWithoutHash, baseURI);
                baseURI = ReferenceUtils.toBaseURI(baseURI);
            }
            baseURI = ReferenceUtils.resolve(ref, baseURI);
            baseURI = ReferenceUtils.toBaseURI(baseURI);
            Reference reference = null;
            boolean isAnchor = false;
            if (this.reference.getReferenceSet().containsKey(baseURI)) {
                reference = this.reference.getReferenceSet().get(baseURI);
            }
            else {
                JsonNode node = null;
                try {
                    node = ReferenceUtils.parse(baseURI, this.reference.getAuths());
                } catch (Exception e) {
                    // we can not parse, try ref
                    baseURI = toBaseURI(ref);
                    node = ReferenceUtils.parse(baseURI, this.reference.getAuths());
                }
                reference = toSchemaReference(baseURI, node);
            }
            String fragment = ReferenceUtils.getFragment(ref);
            JsonNode evaluatedNode = null;
            try {
                evaluatedNode = ReferenceUtils.jsonPointerEvaluate(fragment, reference.getJsonNode(), ref);
            } catch (RuntimeException e) {
                // maybe anchor
                evaluatedNode = findAnchor(reference.getJsonNode(), fragment);
                if (evaluatedNode == null) {
                    throw new RuntimeException("Could not find " + fragment + " in contents of " + ref);
                }
                isAnchor = true;
            }
            Schema resolved = openAPITraverser.deserializeFragment(evaluatedNode, Schema.class, ref, fragment, reference.getMessages());
            if (isAnchor) {
                resolved.$anchor(null);
            }
            ReferenceVisitor visitor = new ReferenceVisitor(reference, openAPITraverser, this.visited, this.visitedMap);
            return openAPITraverser.traverseSchema(resolved, visitor, inheritedIds);
        } catch (Exception e) {
            LOGGER.error("Error resolving schema " + ref, e);
            this.reference.getMessages().add(e.getMessage());
            return null;
        }
    }

    public JsonNode findAnchor(JsonNode root, String anchor) {
        if(root.isObject()){
            JsonNode anchorNode = root.get("$anchor");
            if (anchorNode != null && anchorNode.isValueNode() && anchor.equals(anchorNode.asText())) {
                return root;
            }
            Iterator<String> fieldNames = root.fieldNames();
            while(fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                JsonNode fieldValue = root.get(fieldName);
                JsonNode node = findAnchor(fieldValue, anchor);
                if (node != null) {
                    return node;
                }
            }
        } else if(root.isArray()){
            ArrayNode arrayNode = (ArrayNode) root;
            for(int i = 0; i < arrayNode.size(); i++) {
                JsonNode arrayElement = arrayNode.get(i);
                JsonNode node = findAnchor(arrayElement, anchor);
                if (node != null) {
                    return node;
                }
            }
        }

        return null;
    }
}
