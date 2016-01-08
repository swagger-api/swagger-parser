package io.swagger.parser.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import io.swagger.models.*;
import io.swagger.models.auth.*;
import io.swagger.models.parameters.*;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.PropertyBuilder;
import io.swagger.util.Json;

import java.util.*;

import static io.swagger.models.properties.PropertyBuilder.PropertyId.*;

public class SwaggerDeserializer {
    static Set<String> ROOT_KEYS = new HashSet<String>(Arrays.asList("swagger", "info", "host", "basePath", "schemes", "consumes", "produces", "paths", "definitions", "parameters", "responses", "securityDefinitions", "security", "tags", "externalDocs"));
    static Set<String> EXTERNAL_DOCS_KEYS = new HashSet<String>(Arrays.asList("description", "url"));
    static Set<String> SCHEMA_KEYS = new HashSet<String>(Arrays.asList("discriminator", "example", "$ref", "format", "title", "description", "default", "multipleOf", "maximum", "exclusiveMaximum", "minimum", "exclusiveMinimum", "maxLength", "minLength", "pattern", "maxItems", "minItems", "uniqueItems", "maxProperties", "minProperties", "required", "enum", "type", "items", "allOf", "properties", "additionalProperties", "xml"));
    static Set<String> INFO_KEYS = new HashSet<String>(Arrays.asList("title", "description", "termsOfService", "contact", "license", "version"));
    static Set<String> TAG_KEYS = new HashSet<String>(Arrays.asList("description", "name", "externalDocs"));
    static Set<String> RESPONSE_KEYS = new HashSet<String>(Arrays.asList("description", "schema", "headers", "examples"));
    static Set<String> CONTACT_KEYS = new HashSet<String>(Arrays.asList("name", "url", "email"));
    static Set<String> LICENSE_KEYS = new HashSet<String>(Arrays.asList("name", "url"));
    static Set<String> REF_MODEL_KEYS = new HashSet<String>(Arrays.asList("$ref"));
    static Set<String> PATH_KEYS = new HashSet<String>(Arrays.asList("$ref", "get", "put", "post", "delete", "head", "patch", "options", "parameters"));
    static Set<String> OPERATION_KEYS = new HashSet<String>(Arrays.asList("scheme", "tags", "summary", "description", "externalDocs", "operationId", "consumes", "produces", "parameters", "responses", "schemes", "deprecated", "security"));
    static Set<String> PARAMETER_KEYS = new HashSet<String>(Arrays.asList("name", "in", "description", "required", "type", "format", "allowEmptyValue", "items", "collectionFormat", "default", "maximum", "exclusiveMaximum", "minimum", "exclusiveMinimum", "maxLength", "minLength", "pattern", "maxItems", "minItems", "uniqueItems", "enum", "multipleOf"));
    static Set<String> BODY_PARAMETER_KEYS = new HashSet<String>(Arrays.asList("name", "in", "description", "required", "schema"));

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
                        swagger.consumes(s);
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
                        swagger.produces(s);
                    }
                }
            }

            obj = getObject("paths", on, true, location, result);
            Map<String, Path> paths = paths(obj, "paths", result);
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

            array = getArray("security", on, false, location, result);
            List<SecurityRequirement> security = securityRequirements(array, location, result);
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
                if(key.startsWith("x-")) {
                    swagger.vendorExtension(key, extension(on.get(key)));
                }
                else if(!ROOT_KEYS.contains(key)) {
                    result.extra(location, key, node.get(key));
                }
            }
        }
        else {
            result.invalidType("", "", "object", node);
            result.invalid();
            return null;
        }
        return swagger;
    }

    public Map<String,Path> paths(ObjectNode obj, String location, ParseResult result) {
        Map<String, Path> output = new LinkedHashMap<>();
        if(obj == null) {
            return null;
        }

        Set<String> pathKeys = getKeys(obj);
        for(String pathName : pathKeys) {
            JsonNode pathValue = obj.get(pathName);
            if(!pathValue.getNodeType().equals(JsonNodeType.OBJECT)) {
                result.invalidType(location, pathName, "object", pathValue);
            }
            else {
                ObjectNode path = (ObjectNode) pathValue;
                Path pathObj = path(path, location + ".'" + pathName + "'", result);
                output.put(pathName, pathObj);
            }
        }
        return output;
    }

    public Path path(ObjectNode obj, String location, ParseResult result) {
        boolean hasRef = false;
        Path output = null;
        if(obj.get("$ref") != null) {
            JsonNode ref = obj.get("$ref");
            if(ref.getNodeType().equals(JsonNodeType.STRING)) {
                return pathRef((TextNode)ref, location, result);
            }

            else if(ref.getNodeType().equals(JsonNodeType.OBJECT)){
                ObjectNode on = (ObjectNode) ref;

                // extra keys
                Set<String> keys = getKeys(on);
                for(String key : keys) {
                    result.extra(location, key, on.get(key));
                }
            }
            return null;
        }
        Path path = new Path();

        ArrayNode parameters = getArray("parameters", obj, false, location, result);
        path.setParameters(parameters(parameters, location, result));

        ObjectNode on = getObject("get", obj, false, location, result);
        if(on != null) {
            Operation op = operation(on, location + "(get)", result);
            if(op != null) {
                path.setGet(op);
            }
        }
        on = getObject("put", obj, false, location, result);
        if(on != null) {
            Operation op = operation(on, location + "(put)", result);
            if(op != null) {
                path.setPut(op);
            }
        }
        on = getObject("post", obj, false, location, result);
        if(on != null) {
            Operation op = operation(on, location + "(post)", result);
            if(op != null) {
                path.setPost(op);
            }
        }
        on = getObject("head", obj, false, location, result);
        if(on != null) {
            Operation op = operation(on, location + "(head)", result);
            if(op != null) {
                path.setHead(op);
            }
        }
        on = getObject("delete", obj, false, location, result);
        if(on != null) {
            Operation op = operation(on, location + "(delete)", result);
            if(op != null) {
                path.setDelete(op);
            }
        }
        on = getObject("patch", obj, false, location, result);
        if(on != null) {
            Operation op = operation(on, location + "(patch)", result);
            if(op != null) {
                path.setPatch(op);
            }
        }
        on = getObject("options", obj, false, location, result);
        if(on != null) {
            Operation op = operation(on, location + "(options)", result);
            if(op != null) {
                path.setOptions(op);
            }
        }

        // extra keys
        Set<String> keys = getKeys(obj);
        for(String key : keys) {
            if(key.startsWith("x-")) {
                path.setVendorExtension(key, extension(obj.get(key)));
            }
            else if(!PATH_KEYS.contains(key)) {
                result.extra(location, key, obj.get(key));
            }
        }
        return path;
    }

    public Operation operation(ObjectNode obj, String location, ParseResult result) {
        if(obj == null) {
            return null;
        }
        Operation output = new Operation();
        ArrayNode array = getArray("tags", obj, false, location, result);
        List<String> tags = tagStrings(array, location, result);
        if(tags != null) {
            output.tags(tags);
        }
        String value = getString("summary", obj, false, location, result);
        output.summary(value);

        value = getString("description", obj, false, location, result);
        output.description(value);

        ObjectNode externalDocs = getObject("externalDocs", obj, false, location, result);
        ExternalDocs docs = externalDocs(externalDocs, location, result);
        output.setExternalDocs(docs);

        value = getString("operationId", obj, false, location, result);
        output.operationId(value);

        array = getArray("consumes", obj, false, location, result);
        if(array != null) {
           	if (array.size() == 0) {
        		output.consumes(Collections.<String> emptyList());
        	} else {
	            Iterator<JsonNode> it = array.iterator();
	            while (it.hasNext()) {
	                JsonNode n = it.next();
	                String s = getString(n, location + ".consumes", result);
	                if (s != null) {
	                    output.consumes(s);
	                }
	            }
        	}
        }
        array = getArray("produces", obj, false, location, result);
        if (array != null) {
        	if (array.size() == 0) {
        		output.produces(Collections.<String> emptyList());
        	} else {
	            Iterator<JsonNode> it = array.iterator();
	            while (it.hasNext()) {
	                JsonNode n = it.next();
	                String s = getString(n, location + ".produces", result);
	                if (s != null) {
	                    output.produces(s);
	                }
	            }
        	}
        }
        ArrayNode parameters = getArray("parameters", obj, false, location, result);
        output.setParameters(parameters(parameters, location, result));

        ObjectNode responses = getObject("responses", obj, true, location, result);
        output.setResponses(responses(responses, location, result));

        array = getArray("schemes", obj, false, location, result);
        if(array != null) {
            Iterator<JsonNode> it = array.iterator();
            while (it.hasNext()) {
                JsonNode n = it.next();
                String s = getString(n, location + ".schemes", result);
                if (s != null) {
                    Scheme scheme = Scheme.forValue(s);
                    if (scheme != null) {
                        output.scheme(scheme);
                    }
                }
            }
        }
        Boolean deprecated = getBoolean("deprecated", obj, false, location, result);
        if(deprecated != null) {
            output.setDeprecated(deprecated);
        }
        array = getArray("security", obj, false, location, result);
        List<SecurityRequirement> security = securityRequirements(array, location, result);
        if(security != null) {
            List<Map<String, List<String>>> ss = new ArrayList<>();
            for(SecurityRequirement s : security) {
                if(s.getRequirements() != null && s.getRequirements().size() > 0) {
                    ss.add(s.getRequirements());
                }
            }
            output.setSecurity(ss);
        }

        // extra keys
        Set<String> keys = getKeys(obj);
        for(String key : keys) {
            if(key.startsWith("x-")) {
                output.setVendorExtension(key, extension(obj.get(key)));
            }
            else if(!OPERATION_KEYS.contains(key)) {
                result.extra(location, key, obj.get(key));
            }
        }

        return output;
    }

    public Boolean getBoolean(String key, ObjectNode node, boolean required, String location, ParseResult result) {
        Boolean value = null;
        JsonNode v = node.get(key);
        if (node == null || v == null) {
            if (required) {
                result.missing(location, key);
                result.invalid();
            }
        }
        else {
            if(v.getNodeType().equals(JsonNodeType.BOOLEAN)) {
                value = v.asBoolean();
            }
            else if(v.getNodeType().equals(JsonNodeType.STRING)) {
                String stringValue = v.textValue();
                return Boolean.parseBoolean(stringValue);
            }
        }
        return value;
    }

    public List<Parameter> parameters(ArrayNode obj, String location, ParseResult result) {
        List<Parameter> output = new ArrayList<Parameter>();
        if(obj == null) {
            return output;
        }
        for(JsonNode item : obj) {
            if(item.getNodeType().equals(JsonNodeType.OBJECT)) {
                Parameter param = parameter((ObjectNode) item, location, result);
                if(param != null) {
                    output.add(param);
                }
            }
        }
        return output;
    }

    public Parameter parameter(ObjectNode obj, String location, ParseResult result) {
        if(obj == null) {
            return null;
        }

        Parameter output = null;
        JsonNode ref = obj.get("$ref");
        if(ref != null) {
            if(ref.getNodeType().equals(JsonNodeType.STRING)) {
                return refParameter((TextNode) ref, location, result);
            }
            else {
                result.invalidType(location, "$ref", "string", obj);
                return null;
            }
        }

        String l = null;
        JsonNode ln = obj.get("name");
        if(ln != null) {
            l = ln.asText();
        }
        else {
            l = "['unknown']";
        }
        location += ".[" + l + "]";

        String value = getString("in", obj, true, location, result);
        if(value != null) {
            String type = getString("type", obj, false, location, result);
            String format = getString("format", obj, false, location, result);
            AbstractSerializableParameter<?> sp = null;
            if("query".equals(value)) {
                sp = new QueryParameter();
            }
            else if ("header".equals(value)) {
                sp = new HeaderParameter();
            }
            else if ("path".equals(value)) {
                sp = new PathParameter();
            }
            else if ("formData".equals(value)) {
                sp = new FormParameter();
            }

            if(sp != null) {
                // type is mandatory when sp != null
                getString("type", obj, true, location, result);
                Map<PropertyBuilder.PropertyId, Object> map = new HashMap<PropertyBuilder.PropertyId, Object>();

                map.put(TYPE, type);
                map.put(FORMAT, format);
                String defaultValue = getString("default", obj, false, location, result);
                map.put(DEFAULT, defaultValue);
                sp.setDefault(defaultValue);

                Double dbl = getDouble("maximum", obj, false, location, result);
                if(dbl != null) {
                    map.put(MAXIMUM, dbl);
                    sp.setMaximum(dbl);
                }

                Boolean bl = getBoolean("exclusiveMaximum", obj, false, location, result);
                if(bl != null) {
                    map.put(EXCLUSIVE_MAXIMUM, bl);
                    sp.setExclusiveMaximum(bl);
                }

                dbl = getDouble("minimum", obj, false, location, result);
                if(dbl != null) {
                    map.put(MINIMUM, dbl);
                    sp.setMinimum(dbl);
                }

                bl = getBoolean("exclusiveMinimum", obj, false, location, result);
                if(bl != null) {
                    map.put(EXCLUSIVE_MINIMUM, bl);
                    sp.setExclusiveMinimum(bl);
                }

                map.put(MAX_LENGTH, getInteger("maxLength", obj, false, location, result));
                map.put(MIN_LENGTH, getInteger("minLength", obj, false, location, result));

                String pat = getString("pattern", obj, false, location, result);
                map.put(PATTERN, pat);
                sp.setPattern(pat);

                Integer iv = getInteger("maxItems", obj, false, location, result);
                map.put(MAX_ITEMS, iv);
                sp.setMaxItems(iv);

                iv = getInteger("minItems", obj, false, location, result);
                map.put(MIN_ITEMS, iv);
                sp.setMinItems(iv);

                map.put(UNIQUE_ITEMS, getBoolean("uniqueItems", obj, false, location, result));

                ArrayNode an = getArray("enum", obj, false, location, result);
                if(an != null) {
                    List<String> _enum = new ArrayList<String>();
                    for(JsonNode n : an) {
                        _enum.add(n.textValue());
                    }
                    sp.setEnum(_enum);
                    map.put(ENUM, _enum);
                }

                Property prop = PropertyBuilder.build(type, format, map);

                if(prop != null) {
                    sp.setProperty(prop);
                    ObjectNode items = getObject("items", obj, false, location, result);
                    if(items != null) {
                        Property inner = schema(null, items, location, result);
                        sp.setItems(inner);
                    }
                }

                Set<String> keys = getKeys(obj);
                for(String key : keys) {
                    if(key.startsWith("x-")) {
                        sp.setVendorExtension(key, extension(obj.get(key)));
                    }
                    else if(!PARAMETER_KEYS.contains(key)) {
                        result.extra(location, key, obj.get(key));
                    }
                }

                String collectionFormat = getString("collectionFormat", obj, false, location, result);
                sp.setCollectionFormat(collectionFormat);

                output = sp;
            }
            else if ("body".equals(value)) {
                output = Json.mapper().convertValue(obj, Parameter.class);
            }
            if(output != null) {
                value = getString("name", obj, true, location, result);
                output.setName(value);

                value = getString("description", obj, false, location, result);
                output.setDescription(value);

                Boolean required = getBoolean("required", obj, false, location, result);
                if(required != null) {
                    output.setRequired(required);
                }
            }
        }

        return output;
    }

    private Property schema(Map<String, Object> schemaItems, JsonNode obj, String location, ParseResult result) {
        return Json.mapper().convertValue(obj, Property.class);
    }

    public RefParameter refParameter(TextNode obj, String location, ParseResult result) {
        return new RefParameter(obj.asText());
    }
    
    public RefResponse refResponse(TextNode obj, String location, ParseResult result) {
        return new RefResponse(obj.asText());
    }

    public Path pathRef(TextNode ref, String location, ParseResult result) {
        RefPath output = new RefPath();
        output.set$ref(ref.textValue());
        return output;
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

            model = am;
        }
        else {
            ModelImpl impl = new ModelImpl();
            impl.setType(type);


            JsonNode ap = node.get("additionalProperties");
            if(ap != null && ap.getNodeType().equals(JsonNodeType.OBJECT)) {
                impl.setAdditionalProperties(Json.mapper().convertValue(ap, Property.class));
            }

            value = getString("default", node, false, location, result);
            impl.setDefaultValue(value);

            value = getString("format", node, false, location, result);
            impl.setFormat(value);

            value = getString("discriminator", node, false, location, result);
            impl.setDiscriminator(value);

            ap = node.get("enum");
            if(ap != null) {
                ArrayNode arrayNode = getArray("enum", node, false, location, result);
                if(arrayNode != null) {
                    for(JsonNode n : arrayNode) {
                        impl._enum(n.textValue());
                    }
                }
            }

            JsonNode xml = node.get("xml");
            if(xml != null) {
                impl.setXml(Json.mapper().convertValue(xml, Xml.class));
            }

            ObjectNode externalDocs = getObject("externalDocs", node, false, location, result);
            ExternalDocs docs = externalDocs(externalDocs, location, result);
            impl.setExternalDocs(docs);

            ObjectNode properties = getObject("properties", node, false, location, result);
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
                    impl.setVendorExtension(key, extension(node.get(key)));
                }
                else if(!SCHEMA_KEYS.contains(key)) {
                    result.extra(location, key, node.get(key));
                }
            }
            if("{ }".equals(Json.pretty(impl)))
                return null;
            model = impl;
        }
        JsonNode exampleNode = node.get("example");
        if(exampleNode != null) {
            // we support text or object nodes
            if(exampleNode.getNodeType().equals(JsonNodeType.OBJECT)) {
                ObjectNode on = getObject("example", node, false, location, result);
                if(on != null) {
                    model.setExample(on);
                }
            }
            else {
                model.setExample(exampleNode.asText());
            }
        }

        if(model != null) {
            value = getString("description", node, false, location, result);
            model.setDescription(value);

            value = getString("title", node, false, location, result);
            model.setTitle(value);
        }

        return model;
    }

    public Object extension(JsonNode jsonNode) {
        if(jsonNode.getNodeType().equals(JsonNodeType.BOOLEAN)) {
            return jsonNode.asBoolean();
        }
        if(jsonNode.getNodeType().equals(JsonNodeType.STRING)) {
            return jsonNode.asText();
        }
        if(jsonNode.getNodeType().equals(JsonNodeType.NUMBER)) {
            NumericNode n = (NumericNode) jsonNode;
            if(n.isLong()) {
                return jsonNode.asLong();
            }
            if(n.isInt()) {
                return jsonNode.asInt();
            }
            if(n.isBigDecimal()) {
                return jsonNode.textValue();
            }
            if(n.isBoolean()) {
                return jsonNode.asBoolean();
            }
            if(n.isFloat()) {
                return jsonNode.floatValue();
            }
            if(n.isDouble()) {
                return jsonNode.doubleValue();
            }
            if(n.isShort()) {
                return jsonNode.intValue();
            }
            return jsonNode.asText();
        }
        if(jsonNode.getNodeType().equals(JsonNodeType.ARRAY)) {
            ArrayNode an = (ArrayNode) jsonNode;
            List<Object> o = new ArrayList<Object>();
            for(JsonNode i : an) {
                Object obj = extension(i);
                if(obj != null) {
                    o.add(obj);
                }
            }
            return o;
        }
        return jsonNode;
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
        JsonNode ref = node.get("$ref");
        if(ref != null) {
            if(ref.getNodeType().equals(JsonNodeType.STRING)) {
                return refResponse((TextNode) ref, location, result);
            }
            else {
                result.invalidType(location, "$ref", "string", node);
                return null;
            }
        }

        String value = getString("description", node, true, location, result);
        output.description(value);

        ObjectNode schema = getObject("schema", node, false, location, result);
        if(schema != null) {
            output.schema(Json.mapper().convertValue(schema, Property.class));
        }
        ObjectNode headersNode = getObject("headers", node, false, location, result);
        if(headersNode != null) {
            // TODO
            Map<String, Property> headers = Json.mapper().convertValue(headersNode,
                    Json.mapper().getTypeFactory().constructMapType(Map.class, String.class, Property.class));
            output.headers(headers);
        }

        ObjectNode examplesNode = getObject("examples", node, false, location, result);
        if(examplesNode != null) {
            Map<String, Object> examples = Json.mapper().convertValue(examplesNode, Json.mapper().getTypeFactory().constructMapType(Map.class, String.class, Object.class));
            output.setExamples(examples);
        }

        // extra keys
        Set<String> keys = getKeys(node);
        for(String key : keys) {
            if(key.startsWith("x-")) {
                output.setVendorExtension(key, extension(node.get(key)));
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
                info.setVendorExtension(key, extension(node.get(key)));
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
                license.setVendorExtension(key, extension(node.get(key)));
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

            if(def != null) {
                output.put(key, def);
            }
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
                // TODO: parse manually for better feedback
                output = Json.mapper().convertValue(node, BasicAuthDefinition.class);
            }
            else if (type.equals("apiKey")) {
                String position = getString("in", node, true, location, result);
                String name = getString("name", node, true, location, result);

                if(name != null && ("header".equals(position) || "query".equals(position))) {
                    In in = In.forValue(position);
                    if(in != null) {
                        ApiKeyAuthDefinition auth = new ApiKeyAuthDefinition()
                                .name(name)
                                .in(in);
                        output = auth;
                    }
                }
            }
            else if (type.equals("oauth2")) {
                // TODO: parse manually for better feedback
                output = Json.mapper().convertValue(node, OAuth2Definition.class);
            }
            else {
                result.invalidType(location + ".type", "type", "basic|apiKey|oauth2", node);
            }
        }

        return output;
    }

    public List<SecurityRequirement> securityRequirements(ArrayNode node, String location, ParseResult result) {
        if(node == null)
            return null;

        List<SecurityRequirement> output = new ArrayList<SecurityRequirement>();

        for(JsonNode item : node) {
            SecurityRequirement security = new SecurityRequirement();
            if(item.getNodeType().equals(JsonNodeType.OBJECT)) {
                ObjectNode on = (ObjectNode) item;
                Set<String> keys = getKeys(on);

                for (String key : keys) {
                    List<String> scopes = new ArrayList<>();
                    ArrayNode obj = getArray(key, on, false, location + ".security", result);
                    if (obj != null) {
                        for (JsonNode n : obj) {
                            if (n.getNodeType().equals(JsonNodeType.STRING)) {
                                scopes.add(n.asText());
                            } else {
                                result.invalidType(location, key, "string", n);
                            }
                        }
                    }
                    security.requirement(key, scopes);
                }
            }
            output.add(security);
        }

        return output;
    }


    public List<String> tagStrings(ArrayNode nodes, String location, ParseResult result) {
        if(nodes == null)
            return null;

        List<String> output = new ArrayList<String>();

        for(JsonNode node : nodes) {
            if(node.getNodeType().equals(JsonNodeType.STRING)) {
                output.add(node.textValue());
            }
        }
        return output;
    }

    public List<Tag> tags(ArrayNode nodes, String location, ParseResult result) {
        if(nodes == null)
            return null;

        List<Tag> output = new ArrayList<Tag>();

        for(JsonNode node : nodes) {
            if(node.getNodeType().equals(JsonNodeType.OBJECT)) {
                Tag tag = tag((ObjectNode) node, location + ".tags", result);
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

            value = getString("description", node, false, location, result);
            tag.description(value);

            ObjectNode externalDocs = getObject("externalDocs", node, false, location, result);
            ExternalDocs docs = externalDocs(externalDocs, location + "externalDocs", result);
            tag.externalDocs(docs);

            // extra keys
            for(String key : keys) {
                if(key.startsWith("x-")) {
                    tag.setVendorExtension(key, extension(node.get(key)));
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

            String value = getString("description", node, false, location, result);
            output.description(value);

            value = getString("url", node, true, location, result);
            output.url(value);

            // extra keys
            for(String key : keys) {
                if(key.startsWith("x-")) {
                    output.setVendorExtension(key, extension(node.get(key)));
                }
                else if(!EXTERNAL_DOCS_KEYS.contains(key)) {
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

    public Double getDouble(String key, ObjectNode node, boolean required, String location, ParseResult result) {
        Double value = null;
        JsonNode v = node.get(key);
        if (node == null || v == null) {
            if (required) {
                result.missing(location, key);
                result.invalid();
            }
        }
        else if(v.getNodeType().equals(JsonNodeType.NUMBER)) {
            value = v.asDouble();
        }
        else if(!v.isValueNode()) {
            result.invalidType(location, key, "double", node);
        }
        return value;
    }

    public Number getNumber(String key, ObjectNode node, boolean required, String location, ParseResult result) {
        Number value = null;
        JsonNode v = node.get(key);
        if (node == null || v == null) {
            if (required) {
                result.missing(location, key);
                result.invalid();
            }
        }
        else if(v.getNodeType().equals(JsonNodeType.NUMBER)) {
            value = v.numberValue();
        }
        else if(!v.isValueNode()) {
            result.invalidType(location, key, "number", node);
        }
        return value;
    }

    public Integer getInteger(String key, ObjectNode node, boolean required, String location, ParseResult result) {
        Integer value = null;
        JsonNode v = node.get(key);
        if (node == null || v == null) {
            if (required) {
                result.missing(location, key);
                result.invalid();
            }
        }
        else if(v.getNodeType().equals(JsonNodeType.NUMBER)) {
            value = v.intValue();
        }
        else if(!v.isValueNode()) {
            result.invalidType(location, key, "integer", node);
        }
        return value;
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
        else if(!v.isValueNode()) {
            result.invalidType(location, key, "string", node);
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
