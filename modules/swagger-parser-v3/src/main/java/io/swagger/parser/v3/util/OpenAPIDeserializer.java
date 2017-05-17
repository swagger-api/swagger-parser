package io.swagger.parser.v3.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import io.swagger.oas.models.OpenAPI;
import io.swagger.oas.models.info.Contact;
import io.swagger.oas.models.info.License;
import io.swagger.oas.models.info.Info;
import io.swagger.oas.models.PathItem;
import io.swagger.oas.models.Operation;
import io.swagger.oas.models.responses.ApiResponse;
import io.swagger.oas.models.responses.ApiResponses;
import io.swagger.oas.models.security.SecurityRequirement;
import io.swagger.oas.models.parameters.Parameter;
import io.swagger.oas.models.ExternalDocumentation;
import io.swagger.parser.models.SwaggerParseResult;
import io.swagger.util.Json;
/*import io.swagger.oas.models.parameters.QueryParameter;
import io.swagger.oas.models.parameters.HeaderParameter;
import io.swagger.oas.models.parameters.PathParameter;*/
import io.swagger.oas.models.media.EncodingProperty;



import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;


public class OpenAPIDeserializer {

    protected static Set<String> INFO_KEYS = new LinkedHashSet<String>(Arrays.asList("title", "description", "termsOfService", "contact", "license", "version"));
    protected static Set<String> CONTACT_KEYS = new LinkedHashSet<String>(Arrays.asList("name", "url", "email"));
    protected static Set<String> LICENSE_KEYS = new LinkedHashSet<String>(Arrays.asList("name", "url"));
    protected static Set<String> OPERATION_KEYS = new LinkedHashSet<String>(Arrays.asList("scheme", "tags", "summary", "description", "externalDocs", "operationId", "consumes", "produces", "parameters", "responses", "schemes", "deprecated", "security"));
    protected static Set<String> PATH_KEYS = new LinkedHashSet<String>(Arrays.asList("$ref", "get", "put", "post", "delete", "head", "patch", "options", "parameters"));
    protected static Set<String> EXTERNAL_DOCS_KEYS = new LinkedHashSet<String>(Arrays.asList("description", "url"));
    protected static Set<String> RESPONSE_KEYS = new LinkedHashSet<String>(Arrays.asList("description", "schema", "headers", "examples"));

    public SwaggerParseResult deserialize(JsonNode rootNode) {
        SwaggerParseResult result = new SwaggerParseResult();
        try {
            // TODO
            ParseResult rootParse = new ParseResult();
            OpenAPI api = parseRoot(rootNode, rootParse);
            result.setOpenAPI(api);
        }
        catch (Exception e) {
            result.setMessages(Arrays.asList(e.getMessage()));
        }
        return result;
    }

    public OpenAPI parseRoot(JsonNode node, ParseResult result) {
        String location = "";
        OpenAPI openAPI = new OpenAPI();
        if (node.getNodeType().equals(JsonNodeType.OBJECT)) {
            ObjectNode on = (ObjectNode)node;

            // required
            String value = getString("openapi", on, true, location, result);
            openAPI.setOpenapi(value);

            ObjectNode obj = getObject("info", on, true, "", result);
            if (obj != null) {
                Info  info = info(obj, "info", result);
                openAPI.info(info);
            }

            obj = getObject("paths", on, true, location, result);
            if (obj != null) {
                Map<String, PathItem> paths = paths(obj, "paths", result);


                openAPI.paths(paths);
            }

        }

        return openAPI;
    }

    //PathsObject

    public Map<String,Paths> paths(ObjectNode obj, String location, ParseResult result) {
        Map<String, Paths> output = new LinkedHashMap<>();
        if(obj == null) {
            return null;
        }

        Set<String> pathKeys = getKeys(obj);
        for(String pathName : pathKeys) {
            JsonNode pathValue = obj.get(pathName);
            if(pathName.startsWith("x-")) {
                result.unsupported(location, pathName, pathValue);
            }
            else {
                if (!pathValue.getNodeType().equals(JsonNodeType.OBJECT)) {
                    result.invalidType(location, pathName, "object", pathValue);
                } else {
                    ObjectNode path = (ObjectNode) pathValue;
                    PathItem pathObj = path(path, location + ".'" + pathName + "'", result);
                    output.put(pathName, pathObj);
                }
            }
        }
        return output;
    }



    public PathItem path(ObjectNode obj, String location, ParseResult result) {
        boolean hasRef = false;
        PathItem output = null;
        if(obj.get("$ref") != null) {
            JsonNode ref = obj.get("$ref");
            if(ref.getNodeType().equals(JsonNodeType.STRING)) {
                //return pathRef((TextNode)ref, location, result);
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
        PathItem path = new PathItem();

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
                //path.setVendorExtension(key, extension(obj.get(key)));
            }
            else if(!PATH_KEYS.contains(key)) {
                result.extra(location, key, obj.get(key));
            }
        }
        return path;
    }

    public ExternalDocumentation externalDocs(ObjectNode node, String location, ParseResult result) {
        ExternalDocumentation output = null;

        if(node != null) {
            output = new ExternalDocumentation();
            Set<String> keys = getKeys(node);

            String value = getString("description", node, false, location, result);
            output.description(value);

            value = getString("url", node, true, location, result);
            output.url(value);

            // extra keys
            for(String key : keys) {
                if(key.startsWith("x-")) {
                    //output.setVendorExtension(key, extension(node.get(key)));
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

    public Object extension(JsonNode jsonNode) {
        return Json.mapper().convertValue(jsonNode, Object.class);
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

    private EncodingProperty schema(Map<String, Object> schemaItems, JsonNode obj, String location, ParseResult result) {
        return Json.mapper().convertValue(obj, EncodingProperty.class);
    }


    //Info Object

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

        value = getString("version", node, true, location, result);
        info.version(value);

        // extra keys
        Set<String> keys = getKeys(node);
        for(String key : keys) {
            if(key.startsWith("x-")) {
                //info.setVendorExtension(key, extension(node.get(key)));
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
                //license.setVendorExtension(key, extension(node.get(key)));
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



    /*public PathItem pathRef(TextNode ref, String location, ParseResult result) {
        RefPath output = new RefPath();
        output.set$ref(ref.textValue());
        return output;
    }*/




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
                //return refParameter((TextNode) ref, location, result);
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
        // Comentado por GRACE
        /*if(value != null) {
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
                Map<PropertyBuilder.PropertyId, Object> map = new LinkedHashMap<PropertyBuilder.PropertyId, Object>();

                map.put(TYPE, type);
                map.put(FORMAT, format);
                String defaultValue = getString("default", obj, false, location, result);
                map.put(DEFAULT, defaultValue);
                sp.setDefault(defaultValue);

                BigDecimal bd = getBigDecimal("maximum", obj, false, location, result);
                if(bd != null) {
                    map.put(MAXIMUM, bd);
                    sp.setMaximum(bd);
                }

                Boolean bl = getBoolean("exclusiveMaximum", obj, false, location, result);
                if(bl != null) {
                    map.put(EXCLUSIVE_MAXIMUM, bl);
                    sp.setExclusiveMaximum(bl);
                }

                bd = getBigDecimal("minimum", obj, false, location, result);
                if(bd != null) {
                    map.put(MINIMUM, bd);
                    sp.setMinimum(bd);
                }

                bl = getBoolean("exclusiveMinimum", obj, false, location, result);
                if(bl != null) {
                    map.put(EXCLUSIVE_MINIMUM, bl);
                    sp.setExclusiveMinimum(bl);
                }

                Integer maxLength = getInteger("maxLength", obj, false, location, result);
                map.put(MAX_LENGTH, maxLength);
                sp.setMaxLength(maxLength);

                Integer minLength = getInteger("minLength", obj, false, location, result);
                map.put(MIN_LENGTH, minLength);
                sp.setMinLength(minLength);

                String pat = getString("pattern", obj, false, location, result);
                map.put(PATTERN, pat);
                sp.setPattern(pat);

                Integer iv = getInteger("maxItems", obj, false, location, result);
                map.put(MAX_ITEMS, iv);
                sp.setMaxItems(iv);

                iv = getInteger("minItems", obj, false, location, result);
                map.put(MIN_ITEMS, iv);
                sp.setMinItems(iv);

                iv = getInteger("minLength", obj, false, location, result);
                map.put(MIN_LENGTH, iv);
                sp.setMinLength(iv);

                iv = getInteger("maxLength", obj, false, location, result);
                map.put(MAX_LENGTH, iv);
                sp.setMaxLength(iv);

                bd = getBigDecimal("multipleOf", obj, false, location, result);
                if(bd != null) {
                    map.put(MULTIPLE_OF, bd);
                    sp.setMultipleOf(bd.doubleValue());
                }

                Boolean uniqueItems = getBoolean("uniqueItems", obj, false, location, result);
                map.put(UNIQUE_ITEMS, uniqueItems);
                sp.setUniqueItems(uniqueItems);

                ArrayNode an = getArray("enum", obj, false, location, result);
                if(an != null) {
                    List<String> _enum = new ArrayList<String>();
                    for(JsonNode n : an) {
                        if(n.isValueNode()) {
                            _enum.add(n.asText());
                        }
                        else {
                            result.invalidType(location, "enum", "value", n);
                        }
                    }
                    sp.setEnum(_enum);
                    map.put(ENUM, _enum);
                }

                bl = getBoolean("readOnly", obj, false, location, result);
                if(bl != null) {
                    map.put(READ_ONLY, bl);
                    sp.setReadOnly(bl);
                }

                bl = getBoolean("allowEmptyValue", obj, false, location, result);
                if(bl != null) {
                    map.put(ALLOW_EMPTY_VALUE, bl);
                    sp.setAllowEmptyValue(bl);
                }

                EncodingProperty prop = PropertyBuilder.build(type, format, map);

                if(prop != null) {
                    sp.setProperty(prop);
                    ObjectNode items = getObject("items", obj, false, location, result);
                    if(items != null) {
                        EncodingProperty inner = schema(null, items, location, result);
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
                BodyParameter bp = new BodyParameter();

                JsonNode node = obj.get("schema");
                if(node != null && node instanceof ObjectNode) {
                    bp.setSchema(this.definition((ObjectNode)node, location, result));
                }

                // examples
                ObjectNode examplesNode = getObject("examples", obj, false, location, result);
                if(examplesNode != null) {
                    Map<String, String> examples = Json.mapper().convertValue(examplesNode, Json.mapper().getTypeFactory().constructMapType(Map.class, String.class, Object.class));
                    bp.setExamples(examples);
                }

                // pattern
                String pat = getString("pattern", obj, false, location, result);
                if(pat != null) {
                    bp.setPattern(pat);
                }

                // allowEmptyValue
                Boolean bl = getBoolean("allowEmptyValue", obj, false, location, result);
                if(bl != null) {
                    bp.setAllowEmptyValue(bl);
                }
                // readOnly
                bl = getBoolean("readOnly", obj, false, location, result);
                if(bl != null) {
                    bp.setReadOnly(bl);
                }

                // vendor extensions
                Set<String> keys = getKeys(obj);
                for(String key : keys) {
                    if(key.startsWith("x-")) {
                        bp.setVendorExtension(key, extension(obj.get(key)));
                    }
                    else if(!BODY_PARAMETER_KEYS.contains(key)) {
                        result.extra(location, key, obj.get(key));
                    }
                }
                output = bp;

//                output = Json.mapper().convertValue(obj, Parameter.class);
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
        }*/

        return output;
    }


    public Map<String, ApiResponse> responses(ObjectNode node, String location, ParseResult result) {
        if(node == null)
            return null;

        Map<String, ApiResponse> output = new TreeMap<String, ApiResponse>();

        Set<String> keys = getKeys(node);

        for(String key : keys) {
            if (key.startsWith("x-")) {

            }
            else {
                ObjectNode obj = getObject(key, node, false, location + ".responses", result);
                ApiResponse response = response(obj, location + "." + key, result);
                output.put(key, response);
            }
        }

        return output;
    }

    public ApiResponse response(ObjectNode node, String location, ParseResult result) {
        if(node == null)
            return null;

        ApiResponse output = new ApiResponse();
        JsonNode ref = node.get("$ref");
        if(ref != null) {
            if(ref.getNodeType().equals(JsonNodeType.STRING)) {
                //return refResponse((TextNode) ref, location, result);
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
            //output.schema(Json.mapper().convertValue(schema, EncodingProperty.class));
        }
        ObjectNode headersNode = getObject("headers", node, false, location, result);
        if(headersNode != null) {
            // TODO
            /*Map<String, EncodingProperty> headers = Json.mapper().convertValue(headersNode,
                    Json.mapper().getTypeFactory().constructMapType(Map.class, String.class, EncodingProperty.class));
            output.headers(headers);*/
        }

        ObjectNode examplesNode = getObject("examples", node, false, location, result);
        if(examplesNode != null) {
            Map<String, Object> examples = Json.mapper().convertValue(examplesNode, Json.mapper().getTypeFactory().constructMapType(Map.class, String.class, Object.class));
           // output.setExamples(examples);
        }

        // extra keys
        Set<String> keys = getKeys(node);
        for(String key : keys) {
            if(key.startsWith("x-")) {
                //output.setVendorExtension(key, extension(node.get(key)));
            }
            else if(!RESPONSE_KEYS.contains(key)) {
                result.extra(location, key, node.get(key));
            }
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
        ExternalDocumentation docs = externalDocs(externalDocs, location, result);
        output.setExternalDocs(docs);

        value = getString("operationId", obj, false, location, result);
        output.operationId(value);

        array = getArray("consumes", obj, false, location, result);
        if(array != null) {
            if (array.size() == 0) {
                //output.consumes(Collections.<String> emptyList());
            } else {
                Iterator<JsonNode> it = array.iterator();
                while (it.hasNext()) {
                    JsonNode n = it.next();
                    String s = getString(n, location + ".consumes", result);
                    if (s != null) {
                        //output.consumes(s);
                    }
                }
            }
        }
        array = getArray("produces", obj, false, location, result);
        if (array != null) {
            if (array.size() == 0) {
                //output.produces(Collections.<String> emptyList());
            } else {
                Iterator<JsonNode> it = array.iterator();
                while (it.hasNext()) {
                    JsonNode n = it.next();
                    String s = getString(n, location + ".produces", result);
                    if (s != null) {
                        //output.produces(s);
                    }
                }
            }
        }
        ArrayNode parameters = getArray("parameters", obj, false, location, result);
        output.setParameters(parameters(parameters, location, result));

        ObjectNode responses = getObject("responses", obj, true, location, result);
        Map<String, ApiResponse> responsesObject = responses(responses, location, result);
        if(responsesObject != null && responsesObject.size() == 0) {
            result.missing(location, "responses");
        }
        output.setResponses(responsesObject);

        array = getArray("schemes", obj, false, location, result);
        if(array != null) {
            Iterator<JsonNode> it = array.iterator();
            while (it.hasNext()) {
                JsonNode n = it.next();
                String s = getString(n, location + ".schemes", result);
                if (s != null) {
                    /*Schema scheme = Schema.forValue(s);
                    if (scheme != null) {
                        output.scheme(scheme);
                    }*/
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
            /*for(SecurityRequirement s : security) {
                if(s.getRequirements() != null && s.getRequirements().size() > 0) {
                    ss.add(s.getRequirements());
                }
            }*/
            output.setSecurity(ss);
        }

        // extra keys
        Set<String> keys = getKeys(obj);
        for(String key : keys) {
            if(key.startsWith("x-")) {
                //output.setVendorExtension(key, extension(obj.get(key)));
            }
            else if(!OPERATION_KEYS.contains(key)) {
                result.extra(location, key, obj.get(key));
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
                   // security.requirement(key, scopes);
                }
            }
            output.add(security);
        }

        return output;
    }


    protected static class ParseResult {
        private boolean valid = true;
        private Map<Location, JsonNode> extra = new LinkedHashMap<>();
        private Map<Location, JsonNode> unsupported = new LinkedHashMap<>();
        private Map<Location, String> invalidType = new LinkedHashMap<>();
        private List<Location> missing = new ArrayList<>();

        public ParseResult() {
        }

        public void unsupported(String location, String key, JsonNode value) {
            unsupported.put(new Location(location, key), value);
        }

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

        public Map<Location, JsonNode> getUnsupported() {
            return unsupported;
        }

        public void setUnsupported(Map<Location, JsonNode> unsupported) {
            this.unsupported = unsupported;
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
            for(Location l : unsupported.keySet()) {
                String location = l.location.equals("") ? "" : l.location + ".";
                String message = "attribute " + location + l.key + " is unsupported";
                messages.add(message);
            }
            return messages;
        }
    }


    public BigDecimal getBigDecimal(String key, ObjectNode node, boolean required, String location, ParseResult result) {
        BigDecimal value = null;
        JsonNode v = node.get(key);
        if (node == null || v == null) {
            if (required) {
                result.missing(location, key);
                result.invalid();
            }
        }
        else if(v.getNodeType().equals(JsonNodeType.NUMBER)) {
            value = new BigDecimal(v.asText());
        }
        else if(!v.isValueNode()) {
            result.invalidType(location, key, "double", node);
        }
        return value;
    }

    public Number getNumber(String key, ObjectNode node, boolean required, String location, ParseResult result) {
        Number value = null;
        JsonNode v = node.get(key);
        if (v == null) {
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

    /*public Object extension(JsonNode jsonNode) {
        return Json.mapper().convertValue(jsonNode, Object.class);
    }*/

    protected static class Location {
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