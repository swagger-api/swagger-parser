package io.swagger.parser.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import io.swagger.models.*;
import io.swagger.models.auth.ApiKeyAuthDefinition;
import io.swagger.models.auth.In;
import io.swagger.models.auth.SecuritySchemeDefinition;
import io.swagger.models.parameters.Parameter;
import io.swagger.util.Json;

import java.util.*;

public class SwaggerDeserializer {
    static Set<String> ROOT_KEYS = new HashSet<String>(Arrays.asList("swagger", "info", "host", "basePath", "schemes", "consumes", "produces", "paths", "definitions", "parameters", "responses", "securityDefinitions", "security", "tags", "externalDocs"));
    static Set<String> EXTERNAL_DOCS_KEYS = new HashSet<String>(Arrays.asList("description", "url"));
    static Set<String> TAG_KEYS = new HashSet<String>(Arrays.asList("description", "name", "externalDocs"));

    public SwaggerDeserializationResult deserialize(JsonNode rootNode) {
        SwaggerDeserializationResult result = new SwaggerDeserializationResult();
        ParseResult rootParse = new ParseResult();
        Swagger swagger = parseRoot(rootNode, rootParse);

        Json.prettyPrint(rootParse);

        result.setSwagger(swagger);
        return result;
    }

    public Swagger parseRoot(JsonNode node, ParseResult result) {
        String location = "";
        Swagger swagger = new Swagger();
        if (node.getNodeType().equals(JsonNodeType.OBJECT)) {
            ObjectNode on = (ObjectNode)node;
            Iterator<JsonNode> it = null;

            Set<String> keys = getKeys(on);

            // required
            String value = getString("swagger", on, true, location, result);
            swagger.setSwagger(value);

            ObjectNode obj = getObject("info", on, true, location, result);
            // TODO parse
            Info info = Json.mapper().convertValue(obj, Info.class);
            swagger.info(info);

            // optional
            value = getString("host", on, false, location, result);
            swagger.setHost(value);

            value = getString("basePath", on, false, location, result);
            swagger.setBasePath(value);

            ArrayNode array = getArray("schemes", on, false, location, result);
            if(array != null) {
                it = array.iterator();
                while (it.hasNext()) {
                    JsonNode n = it.next();
                    String s = getString(n, location + ".schemes", result);
                    if (s != null) {
                        Scheme scheme = Scheme.forValue(s);
                        if (scheme != null) {
                            swagger.scheme(scheme);
                        }
                    }
                }
            }

            array = getArray("consumes", on, false, location, result);
            if(array != null) {
                it = array.iterator();
                while (it.hasNext()) {
                    JsonNode n = it.next();
                    String s = getString(n, location + ".consumes", result);
                    if (s != null) {
                        if (s != null) {
                            swagger.consumes(s);
                        }
                    }
                }
            }

            array = getArray("produces", on, false, location, result);
            if(array != null) {
                it = array.iterator();
                while (it.hasNext()) {
                    JsonNode n = it.next();
                    String s = getString(n, location + ".produces", result);
                    if (s != null) {
                        if (s != null) {
                            swagger.produces(s);
                        }
                    }
                }
            }

            obj = getObject("paths", on, true, location, result);
            // TODO: parse
            Map<String, Path> paths = Json.mapper().convertValue(obj, Json.mapper().getTypeFactory().constructMapType(Map.class, String.class, Path.class));
            swagger.paths(paths);

            obj = getObject("definitions", on, true, location, result);
            // TODO: parse
            Map<String, Model> definitions = Json.mapper().convertValue(obj, Json.mapper().getTypeFactory().constructMapType(Map.class, String.class, Model.class));
            swagger.setDefinitions(definitions);

            obj = getObject("parameters", on, true, location, result);
            // TODO: parse
            Map<String, Parameter> parameters = Json.mapper().convertValue(obj, Json.mapper().getTypeFactory().constructMapType(Map.class, String.class, Parameter.class));
            swagger.setParameters(parameters);

            obj = getObject("responses", on, true, location, result);
            // TODO: parse
            Map<String, Response> responses = Json.mapper().convertValue(obj, Json.mapper().getTypeFactory().constructMapType(Map.class, String.class, Response.class));
//            swagger.responses(responses);

            obj = getObject("securityDefinitions", on, true, location, result);
            Map<String, SecuritySchemeDefinition> securityDefinitions = securityDefinitions(obj, location, result);
            swagger.setSecurityDefinitions(securityDefinitions);

            obj = getObject("security", on, true, location, result);
            List<SecurityRequirement> security = securityRequirements(obj, location, result);
            swagger.setSecurity(security);

            array = getArray("tags", on, false, location, result);
            List<Tag> tags = tags(array, location, result);
            swagger.tags(tags);

            obj = getObject("externalDocs", on, true, location, result);
            ExternalDocs docs = externalDocs(obj, location, result);
            swagger.externalDocs(docs);

            // extra keys
            for(String key : keys) {
                if(!ROOT_KEYS.contains(key)) {
                    result.extra(location, key, node.get(key));
                }
            }
        }
        return swagger;
    }

    public Map<String, SecuritySchemeDefinition> securityDefinitions(ObjectNode node, String location, ParseResult result) {
        if(node == null)
            return null;

        Map<String, SecuritySchemeDefinition> output = new HashMap<String, SecuritySchemeDefinition>();
        Set<String> keys = getKeys(node);

        for(String key : keys) {
            ObjectNode obj = getObject(key, node, false, location, result);
            SecuritySchemeDefinition def = securityDefinition(obj, location, result);
        }

        return output;
    }

    public SecuritySchemeDefinition securityDefinition(ObjectNode node, String location, ParseResult result) {
        if(node == null)
            return null;

        SecuritySchemeDefinition output = null;

        String type = getString("type", node, true, location, result);
        if(type != null) {
            if(type.equals("basic")) {
                String position = getString("in", node, true, location, result);
                String name = getString("name", node, true, location, result);

                if(name != null && ("header".equals(position) || "query".equals(position))) {
                    In in = In.forValue(position);
                    if(in != null) {
                        ApiKeyAuthDefinition auth = new ApiKeyAuthDefinition()
                                .name(name)
                                .in(in);
                    }
                }
            }
            else if (type.equals("apiKey")) {

            }
            else if (type.equals("oauth2")) {

            }
            else {
                result.invalidType(location + ".type", "type", "basic|apiKey|oauth2", node);
            }
        }

        return output;
    }

    public List<SecurityRequirement> securityRequirements(ObjectNode node, String location, ParseResult result) {
        if(node == null)
            return null;

        List<SecurityRequirement> output = new ArrayList<SecurityRequirement>();
        Set<String> keys = getKeys(node);

        for(String key : keys) {
            SecurityRequirement security = new SecurityRequirement().requirement(key);

            ArrayNode obj = getArray(key, node, false, location + ".security", result);
            if(obj != null) {
                for(JsonNode n : obj) {
                    if(n.getNodeType().equals(JsonNodeType.STRING)) {
                        security.scope(n.asText());
                    }
                    else {
                        result.invalidType(location, key, "string", n);
                    }
                }
            }
            output.add(security);
        }
        return output;
    }

    public List<Tag> tags(ArrayNode nodes, String location, ParseResult result) {
        if(nodes == null)
            return null;

        List<Tag> output = new ArrayList<Tag>();

        for(JsonNode node : nodes) {
            if(node.getNodeType().equals(JsonNodeType.OBJECT)) {
                Tag tag = tag((ObjectNode)node, location + ".tags", result);
                if(tag != null) {
                    output.add(tag);
                }
            }
        }

        return output;
    }

    public Tag tag(ObjectNode node, String location, ParseResult result) {
        Tag tag = null;

        if(node != null) {
            tag = new Tag();
            Set<String> keys = getKeys(node);

            String value = getString("name", node, true, location, result);
            tag.name(value);

            value = getString("description", node, true, location, result);
            tag.description(value);

            ExternalDocs docs = externalDocs(node, location, result);
            tag.externalDocs(docs);

            // extra keys
            for(String key : keys) {
                if(key.startsWith("x-")) {
                    tag.setVendorExtension(key, Json.pretty(node.get(key)));
                }
                else if(!TAG_KEYS.contains(key)) {
                    result.extra(location + ".externalDocs", key, node.get(key));
                }
            }
        }

        return tag;
    }

    public ExternalDocs externalDocs(ObjectNode node, String location, ParseResult result) {
        ExternalDocs output = null;

        if(node != null) {
            output = new ExternalDocs();
            Set<String> keys = getKeys(node);

            String value = getString("description", node, true, location, result);
            output.description(value);

            value = getString("url", node, true, location, result);
            output.url(value);

            // extra keys
            for(String key : keys) {
                if(key.startsWith("x-")) {
                    output.setVendorExtension(key, Json.pretty(node.get(key)));
                }
                else if(!ROOT_KEYS.contains(key)) {
                    result.extra(location + ".externalDocs", key, node.get(key));
                }
            }
        }

        return output;
    }

    public String getString(JsonNode node, String location, ParseResult result) {
        String output = null;
        if(!node.getNodeType().equals(JsonNodeType.STRING)) {
            result.invalidType(location, "", "string", node);
        }
        else {
            output = ((TextNode) node).asText();
        }
        return output;
    }

    public ArrayNode getArray(String key, ObjectNode node, boolean required, String location, ParseResult result) {
        JsonNode value = node.get(key);
        ArrayNode an = null;
        if(value == null) {
            if(required) {
                result.missing(location, key);
                result.invalid();
            }
        }
        else if(!value.getNodeType().equals(JsonNodeType.ARRAY)) {
            result.invalidType(location, key, "array", value);
        }
        else {
            an = (ArrayNode) value;
        }
        return an;
    }

    public ObjectNode getObject(String key, ObjectNode node, boolean required, String location, ParseResult result) {
        JsonNode value = node.get(key);
        ObjectNode on = null;
        if(value == null) {
            if(required) {
                result.missing(location, key);
                result.invalid();
            }
        }
        else if(!value.getNodeType().equals(JsonNodeType.OBJECT)) {
            result.invalidType(location, key, "object", value);
            if(required) {
                result.invalid();
            }
        }
        else {
            on = (ObjectNode) value;
        }
        return on;
    }

    public String getString(String key, ObjectNode node, boolean required, String location, ParseResult result) {
        String value = null;
        JsonNode v = node.get(key);
        if(v != null) {
            value = v.asText();
            if (value == null) {
                result.missing(location, key);
                if (required) {
                    result.invalid();
                }
            }
        }
        return value;
    }

    public Set<String> getKeys(ObjectNode node) {
        Set<String> keys = new TreeSet<String>();

        Iterator<String> it = node.fieldNames();
        while (it.hasNext()) {
            keys.add(it.next());
        }

        return keys;
    }

    static class ParseResult {
        private boolean valid = true;
        private Map<Location, JsonNode> extra = new HashMap<Location, JsonNode>();
        private Map<Location, String> invalidType = new HashMap<Location, String>();
        private List<Location> missing = new ArrayList<Location>();

        public void extra(String location, String key, JsonNode value) {
            extra.put(new Location(location, key), value);
        }

        public void missing(String location, String key) {
            missing.add(new Location(location, key));
        }

        public void invalidType(String location, String key, String expectedType, JsonNode value){
            invalidType.put(new Location(location, key), expectedType);
        }

        public void invalid() {
            this.valid = false;
        }
    }

    static class Location {
        private String location, key;
        public Location(String location, String key) {
            this.location = location;
            this.key = key;
        }
    }
}
