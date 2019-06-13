package io.swagger.parser.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import io.swagger.models.*;
import io.swagger.models.auth.*;
import io.swagger.models.parameters.*;
import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.PropertyBuilder;
import io.swagger.models.properties.RefProperty;
import io.swagger.util.Json;

import java.math.BigDecimal;
import java.util.*;

import static io.swagger.models.properties.PropertyBuilder.PropertyId.*;

public class SwaggerDeserializer {

    protected static Set<String> ROOT_KEYS = new LinkedHashSet<String>(Arrays.asList("swagger", "info", "host", "basePath", "schemes", "consumes", "produces", "paths", "definitions", "parameters", "responses", "securityDefinitions", "security", "tags", "externalDocs"));
    protected static Set<String> EXTERNAL_DOCS_KEYS = new LinkedHashSet<String>(Arrays.asList("description", "url"));
    protected static Set<String> SCHEMA_KEYS = new LinkedHashSet<String>(Arrays.asList("discriminator", "example", "$ref", "format", "title", "description", "default", "multipleOf", "maximum", "exclusiveMaximum", "minimum", "exclusiveMinimum", "maxLength", "minLength", "pattern", "maxItems", "minItems", "uniqueItems", "maxProperties", "minProperties", "required", "enum", "type", "items", "allOf", "properties", "additionalProperties", "xml", "readOnly", "allowEmptyValue"));
    protected static Set<String> INFO_KEYS = new LinkedHashSet<String>(Arrays.asList("title", "description", "termsOfService", "contact", "license", "version"));
    protected static Set<String> TAG_KEYS = new LinkedHashSet<String>(Arrays.asList("description", "name", "externalDocs"));
    protected static Set<String> RESPONSE_KEYS = new LinkedHashSet<String>(Arrays.asList("description", "schema", "headers", "examples"));
    protected static Set<String> CONTACT_KEYS = new LinkedHashSet<String>(Arrays.asList("name", "url", "email"));
    protected static Set<String> LICENSE_KEYS = new LinkedHashSet<String>(Arrays.asList("name", "url"));
    protected static Set<String> REF_MODEL_KEYS = new LinkedHashSet<String>(Arrays.asList("$ref"));
    protected static Set<String> PATH_KEYS = new LinkedHashSet<String>(Arrays.asList("$ref", "get", "put", "post", "delete", "head", "patch", "options", "parameters"));
    protected static Set<String> OPERATION_KEYS = new LinkedHashSet<String>(Arrays.asList("scheme", "tags", "summary", "description", "externalDocs", "operationId", "consumes", "produces", "parameters", "responses", "schemes", "deprecated", "security"));
    protected static Set<String> PARAMETER_KEYS = new LinkedHashSet<String>(Arrays.asList("name", "in", "description", "required", "type", "format", "allowEmptyValue", "items", "collectionFormat", "default", "maximum", "exclusiveMaximum", "minimum", "exclusiveMinimum", "maxLength", "minLength", "pattern", "maxItems", "minItems", "uniqueItems", "enum", "multipleOf", "readOnly", "allowEmptyValue"));
    protected static Set<String> BODY_PARAMETER_KEYS = new LinkedHashSet<String>(Arrays.asList("name", "in", "description", "required", "schema"));
    protected static Set<String> SECURITY_SCHEME_KEYS = new LinkedHashSet<String>(Arrays.asList("type", "name", "in", "description", "flow", "authorizationUrl", "tokenUrl" , "scopes"));

    private final Set<String> operationIDs = new HashSet<>();

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

            if(obj != null) {
                Map<String, Parameter> parameters = new LinkedHashMap<>();
                Set<String> keys = getKeys(obj);
                for(String key : keys) {
                    JsonNode paramNode = obj.get(key);
                    if(paramNode instanceof ObjectNode) {
                        Parameter parameter = this.parameter((ObjectNode)paramNode, location, result);
                        parameters.put(key, parameter);
                    }
                }
                swagger.setParameters(parameters);
            }

            obj = getObject("responses", on, false, location, result);
            Map<String, Response> responses = responses(obj, "responses", result);
            swagger.responses(responses);

            obj = getObject("securityDefinitions", on, false, location, result);
            Map<String, SecuritySchemeDefinition> securityDefinitions = securityDefinitions(obj, "securityDefinitions", result);
            swagger.setSecurityDefinitions(securityDefinitions);

            array = getArray("security", on, false, location, result);
            List<SecurityRequirement> security = securityRequirements(array, "security", result);
            swagger.setSecurity(security);

            array = getArray("tags", on, false, location, result);
            List<Tag> tags = tags(array, "tags", result);
            swagger.tags(tags);

            obj = getObject("externalDocs", on, false, location, result);
            ExternalDocs docs = externalDocs(obj, "externalDocs", result);
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
            if(pathName.startsWith("x-")) {
                result.unsupported(location, pathName, pathValue);
            }
            else {
                if (!pathValue.getNodeType().equals(JsonNodeType.OBJECT)) {
                    result.invalidType(location, pathName, "object", pathValue);
                } else {
                    ObjectNode path = (ObjectNode) pathValue;
                    Path pathObj = path(path, location + ".'" + pathName + "'", result);
                    String[] eachPart = pathName.split("/");
                    for (String part : eachPart) {
                        if (part.startsWith("{") && part.endsWith("}") && part.length() > 2) {
                            String pathParam = part.substring(1, part.length() - 1);
                            boolean definedInPathLevel = isPathParamDefined(pathParam, pathObj.getParameters());
                            if (definedInPathLevel) {
                                continue;
                            }
                            List<Operation> operationsInAPath = getAllOperationsInAPath(pathObj);
                            for (Operation operation : operationsInAPath) {
                                if (!isPathParamDefined(pathParam, operation.getParameters())) {
                                    result.warning(location + ".'" + pathName + "'"," Declared path parameter " + pathParam + " needs to be defined as a path parameter in path or operation level");
                                    break;
                                }
                            }
                        }
                    }
                    output.put(pathName, pathObj);
                }
            }
        }
        return output;
    }

    private boolean isPathParamDefined(String pathParam, List<Parameter> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return false;
        } else {
            for (Parameter parameter : parameters) {
                if (pathParam.equals(parameter.getName()) && "path".equals(parameter.getIn())) {
                    return true;
                }
            }
        }
        return false;
    }

    private void addToOperationsList(List<Operation> operationsList, Operation operation) {
        if (operation == null) {
            return;
        }
        operationsList.add(operation);
    }

    private List<Operation> getAllOperationsInAPath(Path pathObj) {
        List<Operation> operations = new ArrayList<>();
        addToOperationsList(operations, pathObj.getGet());
        addToOperationsList(operations, pathObj.getPut());
        addToOperationsList(operations, pathObj.getPost());
        addToOperationsList(operations, pathObj.getPatch());
        addToOperationsList(operations, pathObj.getDelete());
        addToOperationsList(operations, pathObj.getOptions());
        addToOperationsList(operations, pathObj.getHead());
        return operations;
    }

    public Path path(ObjectNode obj, String location, ParseResult result) {
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

        value = getString("operationId", obj, false, location, result, operationIDs);
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
        Map<String, Response> responsesObject = responses(responses, location, result);
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
                String paramType = getString("type", obj, true, location, result);
                Map<PropertyBuilder.PropertyId, Object> map = new LinkedHashMap<PropertyBuilder.PropertyId, Object>();

                map.put(TYPE, type);
                map.put(FORMAT, format);
                String defaultValue = parameterDefault(obj, paramType, location, result);
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

                // readOnly
                Boolean bl = getBoolean("readOnly", obj, false, location, result);
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
        }

        return output;
    }

    private String parameterDefault(ObjectNode node, String type, String location, ParseResult result) {
        String key = "default";
        if (type != null && type.equals("array")) {
            ArrayNode array = getArray(key, node, false, location, result);
            return array != null ? array.toString() : null;
        }
        return getString(key, node, false, location, result);
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
        if(result == null) {
            // TODO, this shouldn't happen, but the `ResolverCache#loadRef` method is passing null
            result = new ParseResult();
        }
        if(node == null) {
            result.missing(location, "empty schema");
            return null;
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

            Integer maxItems = getInteger("maxItems", node, false, location, result);
            am.setMaxItems(maxItems);

            Integer minItems = getInteger("minItems", node, false, location, result);
            am.setMinItems(minItems);
            
            // add xml specific information if available 
            JsonNode xml = node.get("xml");
            if(xml != null) {
                am.setXml(Json.mapper().convertValue(xml, Xml.class));
            }
            
            // extra keys
            Set<String> keys = getKeys(node);
            for(String key : keys) {
                if(key.startsWith("x-")) {
                    am.setVendorExtension(key, extension(node.get(key)));
                }
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

            Boolean bp = getBoolean("uniqueItems", node, false, location, result);
            if(bp != null) {
                impl.setUniqueItems(bp);
            }


            BigDecimal bd = getBigDecimal("minimum", node, false, location, result);
            impl.setMinimum(bd);

            bd = getBigDecimal("maximum", node, false, location, result);
            impl.setMaximum(bd);

            bp = getBoolean("exclusiveMaximum", node, false, location, result);
            if(bp != null) {
                impl.setExclusiveMaximum(bp);
            }

            bp = getBoolean("exclusiveMinimum", node, false, location, result);
            if(bp != null) {
                impl.setExclusiveMinimum(bp);
            }

            value = getString("pattern", node, false, location, result);
            impl.setPattern(value);

            BigDecimal maximum = getBigDecimal("maximum", node, false, location, result);
            if(maximum != null) {
                impl.maximum(maximum);
            }

            BigDecimal minimum = getBigDecimal("minimum", node, false, location, result);
            if(minimum != null) {
                impl.minimum(minimum);
            }

            Integer minLength = getInteger("minLength", node, false, location, result);
            if(minLength != null) {
                impl.setMinLength(minLength);
            }

            Integer maxLength = getInteger("maxLength", node, false, location, result);
            if(maxLength != null) {
                impl.setMaxLength(maxLength);
            }

            BigDecimal multipleOf = getBigDecimal("multipleOf", node, false, location, result);
            if(multipleOf != null) {
                impl.setMultipleOf(multipleOf);
            }


            ap = node.get("enum");
            if(ap != null) {
                ArrayNode arrayNode = getArray("enum", node, false, location, result);
                if(arrayNode != null) {
                    for(JsonNode n : arrayNode) {
                        if(n.isValueNode()) {
                            impl._enum(n.asText());
                        }
                        else {
                            result.invalidType(location, "enum", "value", n);
                        }
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
                        if("array".equals( property.getType()) && !(property instanceof ArrayProperty && ((ArrayProperty) property).getItems() != null)) {
                            result.missing(location, "items");
                        }
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
            model = impl;
        }
        JsonNode exampleNode = node.get("example");
        if(exampleNode != null) {
            Object example = Json.mapper().convertValue(exampleNode, Object.class);
            model.setExample(example);
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
        return Json.mapper().convertValue(jsonNode, Object.class);
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

                Model child = null;
                List<RefModel> interfaces = new ArrayList<RefModel>();
                for (Model m : model.getAllOf()) {
                    if (m instanceof RefModel) {
                        interfaces.add((RefModel) m);
                    } else if (m instanceof ModelImpl) {
                        // NOTE: since ComposedModel.child allows only one inline child schema, the last one 'wins'.
                        child = m;
                    }
                }
                model.setInterfaces(interfaces);

                if(child != null) {
                    model.setChild(child);
                }

            }
            else {
                result.invalidType(location, "allOf", "array", allOf);
            }

            // extra keys
            Set<String> keys = getKeys(node);
            for(String key : keys) {
                if(key.startsWith("x-")) {
                    model.setVendorExtension(key, extension(node.get(key)));
                }
                else if(!SCHEMA_KEYS.contains(key)) {
                    result.extra(location, key, node.get(key));
                }
                else {
                    String value = getString("title", node, false, location, result);
                    model.setTitle(value);

                    value = getString("description", node, false, location, result);
                    model.setDescription(value);
                }
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
        if(node != null) {
            if(node.get("type") == null) {
                // may have an enum where type can be inferred
                JsonNode enumNode = node.get("enum");
                if(enumNode != null && enumNode.isArray()) {
                    String type = inferTypeFromArray((ArrayNode) enumNode);
                    node.put("type", type);
                }
            }
        }

        return Json.mapper().convertValue(node, Property.class);
    }

    public String inferTypeFromArray(ArrayNode an) {
        if(an.size() == 0) {
            return "string";
        }
        String type = null;
        for(int i = 0; i < an.size(); i++) {
            JsonNode element = an.get(0);
            if(element.isBoolean()) {
                if(type == null) {
                    type = "boolean";
                }
                else if(!"boolean".equals(type)) {
                    type = "string";
                }
            }
            else if(element.isNumber()) {
                if(type == null) {
                    type = "number";
                }
                else if(!"number".equals(type)) {
                    type = "string";
                }
            }
            else {
                type = "string";
            }
        }

        return type;
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
            if (key.startsWith("x-")) {

            }
            else {
                ObjectNode obj = getObject(key, node, false, location + ".responses", result);
                Response response = response(obj, location + "." + key, result);
                output.put(key, response);
            }
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
            JsonNode schemaRef = schema.get("$ref");
            if (schemaRef != null) {
                if (schemaRef.getNodeType().equals(JsonNodeType.STRING)) {
                    Model schemaProp = new RefModel(schemaRef.textValue());
                    output.responseSchema(schemaProp);
                } else {
                    result.invalidType(location, "$ref", "string", node);
                }
            } else {
                output.responseSchema(Json.mapper().convertValue(schema, Model.class));
            }

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

        Map<String, SecuritySchemeDefinition> output = new LinkedHashMap<>();
        Set<String> keys = getKeys(node);

        for(String key : keys) {
            ObjectNode obj = getObject(key, node, false, location, result);
            SecuritySchemeDefinition def = securityDefinition(obj, location + "." + key, result);

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
                        output = new ApiKeyAuthDefinition()
                                .name(name)
                                .in(in);
                        String description = getString("description", node, false, location, result);
                        output.setDescription(description);
                    }
                }
                JsonNode desc = node.get("description");
                if(desc != null) {
                    output.setDescription(desc.textValue());
                }
            }
            else if (type.equals("oauth2")) {
                // TODO: parse manually for better feedback
                output = Json.mapper().convertValue(node, OAuth2Definition.class);
                JsonNode desc = node.get("description");
                if(desc != null) {
                    output.setDescription(desc.textValue());
                }
            }
            else {
                result.invalidType(location + ".type", "type", "basic|apiKey|oauth2", node);
            }
            
            // extra keys
            Set<String> keys = getKeys(node);
            for(String key : keys) {
                if(key.startsWith("x-")) {
                    output.setVendorExtension(key, extension(node.get(key)));
                }
                else if(!SECURITY_SCHEME_KEYS.contains(key)) {
                    result.extra(location, key, node.get(key));
                }
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

    public String getString(String key, ObjectNode node, boolean required, String location, ParseResult result) {
        return getString(key, node, required, location, result, null);
    }

    public String getString(String key, ObjectNode node, boolean required, String location, ParseResult result, Set<String> uniqueValues) {
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
            if (uniqueValues != null && !uniqueValues.add(value)) {
                result.unique(location, "operationId");
                result.invalid();
            }
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

    protected static class ParseResult {
        private boolean valid = true;
        private Map<Location, JsonNode> extra = new LinkedHashMap<Location, JsonNode>();
        private Map<Location, JsonNode> unsupported = new LinkedHashMap<Location, JsonNode>();
        private Map<Location, String> invalidType = new LinkedHashMap<Location, String>();
        private List<Location> warnings = new ArrayList<>();
        private List<Location> missing = new ArrayList<Location>();
        private List<Location> unique = new ArrayList<>();

        public ParseResult() {
        }

        public void unsupported(String location, String key, JsonNode value) {
            unsupported.put(new Location(location, key), value);
        }

        public void extra(String location, String key, JsonNode value) {
            extra.put(new Location(location, key), value);
        }

        public void unique(String location, String key) {
            unique.add(new Location(location, key));
        }

        public void missing(String location, String key) {
            missing.add(new Location(location, key));
        }

        public void warning(String location, String key) {
            warnings.add(new Location(location, key));
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
            for (Location l : warnings) {
                String location = l.location.equals("") ? "" : l.location + ".";
                String message = "attribute " + location +l.key;
                messages.add(message);
            }
            for (Location l : unique) {
                String location = l.location.equals("") ? "" : l.location + ".";
                String message = "attribute " + location + l.key + " is repeated";
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
