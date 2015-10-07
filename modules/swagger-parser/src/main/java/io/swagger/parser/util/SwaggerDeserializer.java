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
import io.swagger.models.properties.Property;
import io.swagger.util.Json;

import java.util.*;

public class SwaggerDeserializer {
    static Set<String> ROOT_KEYS = new HashSet<String>(Arrays.asList("swagger", "info", "host", "basePath", "schemes", "consumes", "produces", "paths", "definitions", "parameters", "responses", "securityDefinitions", "security", "tags", "externalDocs"));
    static Set<String> EXTERNAL_DOCS_KEYS = new HashSet<String>(Arrays.asList("description", "url"));
    static Set<String> SCHEMA_KEYS = new HashSet<String>(Arrays.asList("$ref", "format", "title", "description", "default", "multipleOf", "maximum", "exclusiveMaximum", "minimum", "exclusiveMinimum", "maxLength", "minLength", "pattern", "maxItems", "minItems", "uniqueItems", "maxProperties", "minProperties", "required", "enum", "type", "items", "allOf", "properties", "additionalProperties"));
    static Set<String> INFO_KEYS = new HashSet<String>(Arrays.asList("title", "description", "termsOfService", "contact", "license", "version"));
    static Set<String> TAG_KEYS = new HashSet<String>(Arrays.asList("description", "name", "externalDocs"));
    static Set<String> RESPONSE_KEYS = new HashSet<String>(Arrays.asList("description", "schema", "headers", "examples"));
    static Set<String> CONTACT_KEYS = new HashSet<String>(Arrays.asList("name", "url", "email"));
    static Set<String> LICENSE_KEYS = new HashSet<String>(Arrays.asList("name", "url"));
    static Set<String> REF_MODEL_KEYS = new HashSet<String>(Arrays.asList("$ref"));

    public SwaggerDeserializationResult deserialize(JsonNode rootNode) {
        SwaggerDeserializationResult result = new SwaggerDeserializationResult();
        ParseResult rootParse = new ParseResult();
        Swagger swagger = parseRoot(rootNode, rootParse);

        result.setSwagger(swagger);
        result.setMessages(rootParse.getMessages());
        return result;
    }

    public Swagger parseRoot(JsonNode node, ParseResult result) {
        String location = "";
        Swagger swagger = new Swagger();
        if (node.getNodeType().equals(JsonNodeType.OBJECT)) {
            ObjectNode on = (ObjectNode)node;
            Iterator<JsonNode> it = null;

            // required
            String value = getString("swagger", on, true, location, result);
            swagger.setSwagger(value);

            ObjectNode obj = getObject("info", on, true, "", result);
            if(obj != null) {
                Info info = info(obj, "info", result);
                swagger.info(info);
            }

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

            obj = getObject("definitions", on, false, location, result);
            Map<String, Model> definitions = definitions(obj, "definitions", result);
            swagger.setDefinitions(definitions);

            obj = getObject("parameters", on, false, location, result);
            // TODO: parse
            Map<String, Parameter> parameters = Json.mapper().convertValue(obj, Json.mapper().getTypeFactory().constructMapType(Map.class, String.class, Parameter.class));
            swagger.setParameters(parameters);

            obj = getObject("responses", on, false, location, result);
            Map<String, Response> responses = responses(obj, "responses", result);
            swagger.responses(responses);

            obj = getObject("securityDefinitions", on, false, location, result);
            Map<String, SecuritySchemeDefinition> securityDefinitions = securityDefinitions(obj, location, result);
            swagger.setSecurityDefinitions(securityDefinitions);

            obj = getObject("security", on, false, location, result);
            List<SecurityRequirement> security = securityRequirements(obj, location, result);
            swagger.setSecurity(security);

            array = getArray("tags", on, false, location, result);
            List<Tag> tags = tags(array, location, result);
            swagger.tags(tags);

            obj = getObject("externalDocs", on, false, location, result);
            ExternalDocs docs = externalDocs(obj, location, result);
            swagger.externalDocs(docs);

            // extra keys
            Set<String> keys = getKeys(on);
            for(String key : keys) {
                if(!ROOT_KEYS.contains(key)) {
                    result.extra(location, key, node.get(key));
                }
            }
        }
        return swagger;
    }

    public Map<String, Model> definitions (ObjectNode node, String location, ParseResult result) {
        if(node == null)
            return null;
        Set<String> schemas = getKeys(node);
        Map<String, Model> output = new LinkedHashMap<String, Model>();

        for(String schemaName : schemas) {
            JsonNode schema = node.get(schemaName);
            if(schema.getNodeType().equals(JsonNodeType.OBJECT)) {
                Model model = definition((ObjectNode) schema, location + "." + schemaName, result);
                if(model != null) {
                    output.put(schemaName, model);
                }
            }
            else {
                result.invalidType(location, schemaName, "object", schema);
            }
        }
        return output;
    }

    public Model definition(ObjectNode node, String location, ParseResult result) {
        if(node == null) {
            result.missing(location, "empty schema");
        }
        if(node.get("$ref") != null) {
            return refModel(node, location, result);
        }
        if(node.get("allOf") != null) {
            return allOfModel(node, location, result);
        }
        Model model = null;
        String value = null;

        value = getString("title", node, false, location, result);
//        model.title(value);
        // TODO: name?

        String type = getString("type", node, false, location, result);
        Model m = new ModelImpl();
        if("array".equals(type)) {
            ArrayModel am = new ArrayModel();
            ObjectNode propertyNode = getObject("properties", node, false, location, result);
            Map<String, Property> properties = properties(propertyNode, location, result);
            am.setProperties(properties);


            ObjectNode itemsNode = getObject("items", node, false, location, result);
            Property items = property(itemsNode, location, result);
            if(items != null) {
                am.items(items);
            }

//            am.setVendorExtension();

            model = am;
        }
        else {
            ModelImpl impl = new ModelImpl();
            impl.setType(value);
//            impl.setAdditionalProperties();
//            impl.setDefaultValue();

            value = getString("format", node, false, location, result);
            impl.setFormat(value);

//            impl.setDiscriminator();
//            impl.setXml();
//            impl.setExternalDocs();

            ObjectNode properties = getObject("properties", node, true, location, result);
            if(properties != null) {
                Set<String> propertyNames = getKeys(properties);
                for(String propertyName : propertyNames) {
                    JsonNode propertyNode = properties.get(propertyName);
                    if(propertyNode.getNodeType().equals(JsonNodeType.OBJECT)) {
                        ObjectNode on = (ObjectNode) propertyNode;
                        Property property = property(on, location, result);
                        impl.property(propertyName, property);
                    }
                    else {
                        result.invalidType(location, "properties", "object", propertyNode);
                    }
                }
            }

            // need to set properties first
            ArrayNode required = getArray("required", node, false, location, result);
            if(required != null) {
                List<String> requiredProperties = new ArrayList<String>();
                for (JsonNode n : required) {
                    if(n.getNodeType().equals(JsonNodeType.STRING)) {
                        requiredProperties.add(((TextNode) n).textValue());
                    }
                    else {
                        result.invalidType(location, "required", "string", n);
                    }
                }
                if(requiredProperties.size() > 0) {
                    impl.setRequired(requiredProperties);
                }
            }

            // extra keys
            Set<String> keys = getKeys(node);
            for(String key : keys) {
                if(key.startsWith("x-")) {
                    impl.setVendorExtension(key, node.get(key));
                }
                else if(!SCHEMA_KEYS.contains(key)) {
                    result.extra(location, key, node.get(key));
                }
            }
            if("{ }".equals(Json.pretty(impl)))
                return null;
            model = impl;
        }
        ObjectNode exampleNode = getObject("example", node, false, location, result);
        if(exampleNode != null) {
            model.setExample(exampleNode);
        }

        value = getString("description", node, false, location, result);
        model.setDescription(value);

//        m.setExample();

        // multipleOf

        // maximum
        // Boolean bool = getBoolean("exclusiveMaximum", node, false, location, result);
        // Boolean bool = getBoolean("exclusiveMinimum", node, false, location, result);

        // maxLength
        // minLength

        // pattern

        // maxItems
        // minItems
        // uniqueItems
        // maxProperties
        // minProperties
        // enum
        // items
        // additionalProperties

        return model;
    }

    public Model allOfModel(ObjectNode node, String location, ParseResult result) {
        JsonNode sub = node.get("$ref");
        JsonNode allOf = node.get("allOf");

        if (sub != null) {
            if(sub.getNodeType().equals(JsonNodeType.OBJECT)) {
                return refModel((ObjectNode)sub, location, result);
            }
            else {
                result.invalidType(location, "$ref", "object", sub);
                return null;
            }
        } else if (allOf != null) {
            ComposedModel model = null;
            // we only support one parent, no multiple inheritance or composition
            if(allOf.getNodeType().equals(JsonNodeType.ARRAY)) {
                model = new ComposedModel();
                int pos = 0;
                for(JsonNode part : allOf) {
                    if(part.getNodeType().equals(JsonNodeType.OBJECT)) {
                        Model segment = definition((ObjectNode) part, location, result);
                        if(segment != null) {
                            model.getAllOf().add(segment);
                        }
                    }
                    else {
                        result.invalidType(location, "allOf[" + pos + "]", "object", part);
                    }
                    pos++;
                }

                List<Model> allComponents = model.getAllOf();
                if (allComponents.size() >= 1) {
                    model.setParent(allComponents.get(0));
                    if (allComponents.size() >= 2) {
                        model.setChild(allComponents.get(allComponents.size() - 1));
                        List<RefModel> interfaces = new ArrayList<RefModel>();
                        int size = allComponents.size();
                        for (Model m : allComponents.subList(1, size - 1)) {
                            if (m instanceof RefModel) {
                                RefModel ref = (RefModel) m;
                                interfaces.add(ref);
                            }
                        }
                        model.setInterfaces(interfaces);
                    } else {
                        model.setChild(new ModelImpl());
                    }
                }
                return model;
            }
            else {
                result.invalidType(location, "allOf", "array", allOf);
            }

            return model;
        }
        return null;
    }

    public Map<String, Property> properties(ObjectNode node, String location, ParseResult result) {
        if(node == null) {
            return null;
        }
        Map<String, Property> output = new LinkedHashMap<String, Property>();

        Set<String> keys = getKeys(node);
        for(String propertyName : keys) {
            JsonNode propertyNode = node.get(propertyName);
            if(propertyNode.getNodeType().equals(JsonNodeType.OBJECT)) {
                Property property = property((ObjectNode)propertyNode, location, result);
                output.put(propertyName, property);
            }
            else {
                result.invalidType(location, propertyName, "object", propertyNode);
            }
        }
        return output;
    }

    public Property property(ObjectNode node, String location, ParseResult result) {
        Property output = Json.mapper().convertValue(node, Property.class);
        return output;
    }

    public RefModel refModel(ObjectNode node, String location, ParseResult result) {
        RefModel output = new RefModel();

        if(node.getNodeType().equals(JsonNodeType.OBJECT)) {
            String refValue = ((TextNode)node.get("$ref")).textValue();
            output.set$ref(refValue);
        }
        else {
            result.invalidType(location, "$ref", "object", node);
            return null;
        }

        // extra keys
        Set<String> keys = getKeys(node);
        for(String key : keys) {
            if(!REF_MODEL_KEYS.contains(key)) {
                result.extra(location, key, node.get(key));
            }
        }

        return output;
    }

    public Map<String, Response> responses(ObjectNode node, String location, ParseResult result) {
        if(node == null)
            return null;

        Map<String, Response> output = new TreeMap<String, Response>();

        Set<String> keys = getKeys(node);

        for(String key : keys) {
            ObjectNode obj = getObject(key, node, false, location + ".responses", result);
            Response response = response(obj, location + "." + key, result);
            output.put(key, response);
        }

        return output;
    }

    public Response response(ObjectNode node, String location, ParseResult result) {
        if(node == null)
            return null;

        Response output = new Response();

        String value = getString("description", node, true, location, result);
        output.description(value);

        ObjectNode schema = getObject("schema", node, false, location, result);
        if(schema != null) {
            output.schema(Json.mapper().convertValue(schema, Property.class));
        }
        ObjectNode headersNode = getObject("headers", node, false, location, result);
        if(headersNode != null) {
            // TODO
            Map<String, Property> headers = Json.mapper().convertValue(headersNode, Json.mapper().getTypeFactory().constructMapType(Map.class, String.class, Path.class));
            output.headers(headers);
        }

        ObjectNode examplesNode = getObject("examples", node, false, location, result);
        if(examplesNode != null) {
            // TODO
            Map<String, Object> examples = Json.mapper().convertValue(examplesNode, Json.mapper().getTypeFactory().constructMapType(Map.class, String.class, Object.class));
            output.setExamples(examples);
        }

        // extra keys
        Set<String> keys = getKeys(node);
        for(String key : keys) {
            if(key.startsWith("x-")) {
                output.setVendorExtension(key, node.get(key));
            }
            else if(!RESPONSE_KEYS.contains(key)) {
                result.extra(location, key, node.get(key));
            }
        }
        return output;
    }

    public Info info(ObjectNode node, String location, ParseResult result) {
        if(node == null)
            return null;

        Info info = new Info();
        String value = getString("title", node, true, location, result);
        info.title(value);

        value = getString("description", node, false, location, result);
        info.description(value);

        value = getString("termsOfService", node, false, location, result);
        info.termsOfService(value);

        ObjectNode obj = getObject("contact", node, false, "contact", result);
        Contact contact = contact(obj, location, result);
        info.contact(contact);

        obj = getObject("license", node, false, location, result);
        License license = license(obj, location, result);
        info.license(license);

        value = getString("version", node, false, location, result);
        info.version(value);

        // extra keys
        Set<String> keys = getKeys(node);
        for(String key : keys) {
            if(key.startsWith("x-")) {
                info.setVendorExtension(key, node.get(key));
            }
            else if(!INFO_KEYS.contains(key)) {
                result.extra(location, key, node.get(key));
            }
        }

        return info;
    }

    public License license(ObjectNode node, String location, ParseResult result) {
        if(node == null)
            return null;

        License license = new License();

        String value = getString("name", node, true, location, result);
        license.name(value);

        value = getString("url", node, false, location, result);
        license.url(value);

        // extra keys
        Set<String> keys = getKeys(node);
        for(String key : keys) {
            if(key.startsWith("x-")) {
                license.setVendorExtension(key, node.get(key));
            }
            else if(!LICENSE_KEYS.contains(key)) {
                result.extra(location + ".license", key, node.get(key));
            }
        }

        return license;
    }

    public Contact contact(ObjectNode node, String location, ParseResult result) {
        if(node == null)
            return null;

        Contact contact = new Contact();

        String value = getString("name", node, false, location + ".name", result);
        contact.name(value);

        value = getString("url", node, false, location + ".url", result);
        contact.url(value);

        value = getString("email", node, false, location + ".email", result);
        contact.email(value);

        // extra keys
        Set<String> keys = getKeys(node);
        for(String key : keys) {
            if(!CONTACT_KEYS.contains(key)) {
                result.extra(location + ".contact", key, node.get(key));
            }
        }

        return contact;
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
                    tag.setVendorExtension(key, node.get(key));
                }
                else if(!EXTERNAL_DOCS_KEYS.contains(key)) {
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
                    output.setVendorExtension(key, node.get(key));
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
        if (node == null || v == null) {
            if (required) {
                result.missing(location, key);
                result.invalid();
            }
        }
        else {
            value = v.asText();
        }
        return value;
    }

    public Set<String> getKeys(ObjectNode node) {
        Set<String> keys = new LinkedHashSet<>();
        if(node == null) {
            return keys;
        }

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

        public boolean isValid() {
            return valid;
        }

        public void setValid(boolean valid) {
            this.valid = valid;
        }

        public Map<Location, JsonNode> getExtra() {
            return extra;
        }

        public void setExtra(Map<Location, JsonNode> extra) {
            this.extra = extra;
        }

        public Map<Location, String> getInvalidType() {
            return invalidType;
        }

        public void setInvalidType(Map<Location, String> invalidType) {
            this.invalidType = invalidType;
        }

        public List<Location> getMissing() {
            return missing;
        }

        public void setMissing(List<Location> missing) {
            this.missing = missing;
        }

        public List<String> getMessages() {
            List<String> messages = new ArrayList<String>();
            for(Location l : extra.keySet()) {
                String location = l.location.equals("") ? "" : l.location + ".";
                String message = "attribute " + location + l.key + " is unexpected";
                messages.add(message);
            }
            for(Location l : invalidType.keySet()) {
                String location = l.location.equals("") ? "" : l.location + ".";
                String message = "attribute " + location + l.key + " is not of type `" + invalidType.get(l) + "`";
                messages.add(message);
            }
            for(Location l : missing) {
                String location = l.location.equals("") ? "" : l.location + ".";
                String message = "attribute " + location + l.key + " is missing";
                messages.add(message);
            }
            return messages;
        }
    }

    static class Location {
        private String location;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Location)) return false;

            Location location1 = (Location) o;

            if (location != null ? !location.equals(location1.location) : location1.location != null) return false;
            return !(key != null ? !key.equals(location1.key) : location1.key != null);

        }

        @Override
        public int hashCode() {
            int result = location != null ? location.hashCode() : 0;
            result = 31 * result + (key != null ? key.hashCode() : 0);
            return result;
        }

        private String key;
        public Location(String location, String key) {
            this.location = location;
            this.key = key;
        }
    }
}
