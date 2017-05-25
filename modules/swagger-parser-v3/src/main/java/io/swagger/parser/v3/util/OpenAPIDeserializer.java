package io.swagger.parser.v3.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import io.swagger.oas.models.ExternalDocumentation;
import io.swagger.oas.models.OpenAPI;
import io.swagger.oas.models.Operation;
import io.swagger.oas.models.PathItem;
import io.swagger.oas.models.Paths;
import io.swagger.oas.models.info.Contact;
import io.swagger.oas.models.info.Info;
import io.swagger.oas.models.info.License;
import io.swagger.oas.models.parameters.CookieParameter;
import io.swagger.oas.models.parameters.HeaderParameter;
import io.swagger.oas.models.parameters.Parameter;
import io.swagger.oas.models.parameters.PathParameter;
import io.swagger.oas.models.parameters.QueryParameter;
import io.swagger.oas.models.parameters.RequestBody;
import io.swagger.oas.models.responses.ApiResponse;
import io.swagger.oas.models.responses.ApiResponses;
import io.swagger.oas.models.security.SecurityRequirement;
import io.swagger.oas.models.servers.Server;
import io.swagger.oas.models.servers.ServerVariable;
import io.swagger.oas.models.servers.ServerVariables;
import io.swagger.parser.models.SwaggerParseResult;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

//import java.math.BigDecimal;


public class OpenAPIDeserializer {

    protected static Set<String> INFO_KEYS = new LinkedHashSet<>(Arrays.asList("title", "description", "termsOfService", "contact", "license", "version"));
    protected static Set<String> CONTACT_KEYS = new LinkedHashSet<>(Arrays.asList("name", "url", "email"));
    protected static Set<String> LICENSE_KEYS = new LinkedHashSet<>(Arrays.asList("name", "url"));
    protected static Set<String> OPERATION_KEYS = new LinkedHashSet<>(Arrays.asList("scheme", "tags", "summary", "description", "externalDocs", "operationId", "parameters", "responses", "schemes", "deprecated", "security"));
    protected static Set<String> PATH_KEYS = new LinkedHashSet<>(Arrays.asList("$ref", "get", "put", "post", "delete", "head", "patch", "options", "parameters"));
    protected static Set<String> EXTERNAL_DOCS_KEYS = new LinkedHashSet<>(Arrays.asList("description", "url"));
    protected static Set<String> RESPONSE_KEYS = new LinkedHashSet<>(Arrays.asList("description", "schema", "headers", "examples"));
    private static final String QUERY_PARAMETER = "query";
    private static final String COOKIE_PARAMETER = "cookie";
    private static final String PATH_PARAMETER = "path";
    private static final String HEADER_PARAMETER = "header";

    public SwaggerParseResult deserialize(JsonNode rootNode) {
        SwaggerParseResult result = new SwaggerParseResult();
        try {
            // TODO
            ParseResult rootParse = new ParseResult();
            OpenAPI api = parseRoot(rootNode, rootParse);
            result.setOpenAPI(api);
            result.setMessages(rootParse.getMessages());
        } catch (Exception e) {
            result.setMessages(Arrays.asList(e.getMessage()));

        }
        return result;
    }

    public OpenAPI parseRoot(JsonNode node, ParseResult result) {
        String location = "";
        OpenAPI openAPI = new OpenAPI();
        if (node.getNodeType().equals(JsonNodeType.OBJECT)) {
            ObjectNode on = (ObjectNode) node;

            // required
            String value = getString("openapi", on, true, location, result);
            openAPI.setOpenapi(value);

            ObjectNode obj = getObject("info", on, true, "", result);
            if (obj != null) {
                Info info = getInfo(obj, "info", result);
                openAPI.setInfo(info);
            }

            obj = getObject("paths", on, true, location, result);
            if (obj != null) {
                Paths paths = getPaths(obj, "paths", result);
                openAPI.setPaths(paths);
            }

            ArrayNode array = getArray("servers", on, false, location, result);
            if (obj != null) {
                openAPI.setServers(getServersList(array, location, result));
            }

            obj = getObject("externalDocs", on, false, location, result);
            if (obj != null) {
                ExternalDocumentation externalDocs = getExternalDocs(obj, "externalDocs", result);
                openAPI.setExternalDocs(externalDocs);
            }
        }

        return openAPI;
    }



    public List<Server> getServersList(ArrayNode obj, String location, ParseResult result) {

        List<Server> servers = new ArrayList<>();
        if (obj == null) {
            return null;

        }
        for (JsonNode item : obj) {
            if (item.getNodeType().equals(JsonNodeType.OBJECT)) {
                Server server = getServer((ObjectNode) item, location, result);
                if (server != null) {
                    servers.add(server);
                }
            }
        }
        return servers;
    }

    public Server getServer(ObjectNode obj, String location, ParseResult result) {
        if (obj == null) {
            return null;
        }

        Server server = new Server();
        if (server == null) {
            result.invalidType(location, "servers", "string", obj);
            return null;
        }

        String value = getString("url", obj, true, location, result);
        server.setUrl(value);

        value = getString("description", obj, false, location, result);
        server.setDescription(value);



        server.setVariables(getServerVariables(obj,location,result));



        return server;
    }

    public ServerVariables getServerVariables(ObjectNode obj, String location, ParseResult result){
        ServerVariables serverVariables = new ServerVariables();
        if (obj == null) {
            return null;
        }

        Set<String> serverKeys = getKeys(obj);
        for (String serverName : serverKeys) {
            JsonNode serverValue = obj.get(serverName);
            if (!serverValue.getNodeType().equals(JsonNodeType.OBJECT)) {
                result.invalidType(location, serverName, "object", serverValue);
            } else {
                ObjectNode server = (ObjectNode) serverValue;
                //ServerVariable serverVariable = getServerVariable(server, location + ".'" + serverName + "'", result);
                serverVariables = getServerVariable(server, location + ".'" + serverName + "'", result);
            }
        }

        return serverVariables;
    }

    public ServerVariables getServerVariable(ObjectNode obj, String location, ParseResult result){
        if(obj == null){
            return null;
        }
        final ServerVariables serverVariables = new ServerVariables();
        ServerVariable serverVariable = null;

        Set<String> variableKeys = getKeys(obj);
        for (String variableName : variableKeys) {
            JsonNode variableValue = obj.get(variableName);
            if (!variableValue.getNodeType().equals(JsonNodeType.OBJECT)) {
                result.invalidType(location, variableName, "object", variableValue);
            } else {
                serverVariable = new ServerVariable();
                ObjectNode objNode = getObject(variableName, obj, false, location, result);

                ArrayNode an = getArray("enum",objNode,false,location,result);
                if (an != null) {
                    List<String> _enum = new ArrayList<>();
                    for(JsonNode n : an) {
                        if(n.isValueNode()) {
                            _enum.add(n.asText());
                            serverVariable.setEnum(_enum);
                        }
                        else {
                            result.invalidType(location, "enum", "value", n);
                        }
                    }

                }


                String value = getString("default", objNode, true, location + ".'" + variableName + "'", result);
                serverVariable.setDefault(value);

                value = getString("description", objNode, false, location + ".'" + variableName + "'", result);
                serverVariable.setDescription(value);
                serverVariables.addServerVariable(variableName,serverVariable);

            }
        }

        return serverVariables;
    }

    //PathsObject

    public Paths getPaths(ObjectNode obj, String location, ParseResult result) {
        final Paths paths = new Paths();
        if (obj == null) {
            return null;
        }

        Set<String> pathKeys = getKeys(obj);
        for (String pathName : pathKeys) {
            JsonNode pathValue = obj.get(pathName);
            if (!pathValue.getNodeType().equals(JsonNodeType.OBJECT)) {
                result.invalidType(location, pathName, "object", pathValue);
            } else {
                ObjectNode path = (ObjectNode) pathValue;
                PathItem pathObj = getPathItem(path, location + ".'" + pathName + "'", result);
                paths.put(pathName, pathObj);
            }
        }

        return paths;
    }

    public PathItem getPathItem(ObjectNode obj, String location, ParseResult result) {
        boolean hasRef = false;

        if (obj.get("$ref") != null) {
            JsonNode ref = obj.get("$ref");

            if (ref.getNodeType().equals(JsonNodeType.STRING)) {
                //return pathRef((TextNode)ref, location, result);
            } else if (ref.getNodeType().equals(JsonNodeType.OBJECT)) {
                ObjectNode on = (ObjectNode) ref;

                // extra keys
                Set<String> keys = getKeys(on);
                for (String key : keys) {
                    result.extra(location, key, on.get(key));
                }
            }
            return null;
        }
        PathItem pathItem = new PathItem();

        String value = getString("summary", obj, true, location, result);
        pathItem.setSummary(value);

        value = getString("description", obj, false, location, result);
        pathItem.setDescription(value);

        ArrayNode parameters = getArray("parameters", obj, false, location, result);
        pathItem.setParameters(getParameterList(parameters, location, result));

        ArrayNode servers = getArray("servers", obj, false, location, result);
        pathItem.setServers(getServersList(servers, location, result));

        ObjectNode on = getObject("get", obj, false, location, result);
        if (on != null) {
            Operation op = getOperation(on, location + "(get)", result);
            if (op != null) {
                pathItem.setGet(op);
            }
        }
        on = getObject("put", obj, false, location, result);
        if (on != null) {
            Operation op = getOperation(on, location + "(put)", result);
            if (op != null) {
                pathItem.setPut(op);
            }
        }
        on = getObject("post", obj, false, location, result);
        if (on != null) {
            Operation op = getOperation(on, location + "(post)", result);
            if (op != null) {
                pathItem.setPost(op);
            }
        }
        on = getObject("head", obj, false, location, result);
        if (on != null) {
            Operation op = getOperation(on, location + "(head)", result);
            if (op != null) {
                pathItem.setHead(op);
            }
        }
        on = getObject("delete", obj, false, location, result);
        if (on != null) {
            Operation op = getOperation(on, location + "(delete)", result);
            if (op != null) {
                pathItem.setDelete(op);
            }
        }
        on = getObject("patch", obj, false, location, result);
        if (on != null) {
            Operation op = getOperation(on, location + "(patch)", result);
            if (op != null) {
                pathItem.setPatch(op);
            }
        }
        on = getObject("options", obj, false, location, result);
        if (on != null) {
            Operation op = getOperation(on, location + "(options)", result);
            if (op != null) {
                pathItem.setOptions(op);
            }
        }

        // extra keys
        Set<String> keys = getKeys(obj);
        for (String key : keys) {
            if (key.startsWith("x-")) {
                //createPathItem.setVendorExtension(key, extension(obj.get(key)));
            } else if (!PATH_KEYS.contains(key)) {
                result.extra(location, key, obj.get(key));
            }
        }
        return pathItem;
    }


    public ExternalDocumentation getExternalDocs(ObjectNode node, String location, ParseResult result) {
        ExternalDocumentation externalDocs = null;

        if (node != null) {
            externalDocs = new ExternalDocumentation();
            Set<String> keys = getKeys(node);

            String value = getString("description", node, false, location, result);
            externalDocs.description(value);

            value = getString("url", node, true, location, result);
            externalDocs.url(value);

            // extra keys
            for (String key : keys) {
                if (key.startsWith("x-")) {
                    //output.setVendorExtension(key, extension(node.get(key)));
                } else if (!EXTERNAL_DOCS_KEYS.contains(key)) {
                    result.extra(location + ".externalDocs", key, node.get(key));
                }
            }
        }

        return externalDocs;
    }


    public String getString(JsonNode node, String location, ParseResult result) {
        String output = null;
        if (!node.getNodeType().equals(JsonNodeType.STRING)) {
            result.invalidType(location, "", "string", node);
        } else {
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
        } else if (!v.isValueNode()) {
            result.invalidType(location, key, "string", node);
        } else {
            value = v.asText();
        }
        return value;
    }

    public Set<String> getKeys(ObjectNode node) {
        Set<String> keys = new LinkedHashSet<>();
        if (node == null) {
            return keys;
        }

        Iterator<String> it = node.fieldNames();
        while (it.hasNext()) {
            keys.add(it.next());
        }

        return keys;
    }


    public ObjectNode getObject(String key, ObjectNode node, boolean required, String location, ParseResult result) {
        JsonNode value = node.get(key);
        //System.out.println(value);
        ObjectNode on = null;
        if (value == null) {
            if (required) {
                result.missing(location, key);
                result.invalid();
            }
        } else if (!value.getNodeType().equals(JsonNodeType.OBJECT)) {
            //System.out.println("nodeType"+value.getNodeType());
            result.invalidType(location, key, "object", value);
            if (required) {
                result.invalid();
            }
        } else {
            on = (ObjectNode) value;
        }
        return on;
    }

    //Info Object

    public Info getInfo(ObjectNode node, String location, ParseResult result) {
        if (node == null)
            return null;

        Info info = new Info();
        String value = getString("title", node, true, location, result);
        info.setTitle(value);

        value = getString("description", node, false, location, result);
        info.setDescription(value);

        value = getString("termsOfService", node, false, location, result);
        info.setTermsOfService(value);

        ObjectNode obj = getObject("contact", node, false, "contact", result);
        Contact contact = getContact(obj, location, result);
        info.setContact(contact);

        obj = getObject("license", node, false, location, result);
        License license = getLicense(obj, location, result);
        info.setLicense(license);

        value = getString("version", node, true, location, result);
        info.setVersion(value);

        // extra keys
        Set<String> keys = getKeys(node);
        for (String key : keys) {
            if (key.startsWith("x-")) {
                //info.setVendorExtension(key, extension(node.get(key)));
            } else if (!INFO_KEYS.contains(key)) {
                result.extra(location, key, node.get(key));
            }
        }

        return info;
    }

    public License getLicense(ObjectNode node, String location, ParseResult result) {
        if (node == null)
            return null;

        License license = new License();

        String value = getString("name", node, true, location, result);
        license.setName(value);

        value = getString("url", node, false, location, result);
        license.setUrl(value);

        // extra keys
        Set<String> keys = getKeys(node);
        for (String key : keys) {
            if (key.startsWith("x-")) {
                //license.setVendorExtension(key, extension(node.get(key)));
            } else if (!LICENSE_KEYS.contains(key)) {
                result.extra(location + ".license", key, node.get(key));
            }
        }

        return license;
    }

    public Contact getContact(ObjectNode node, String location, ParseResult result) {
        if (node == null)
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
        for (String key : keys) {
            if (!CONTACT_KEYS.contains(key)) {
                result.extra(location + ".contact", key, node.get(key));
            }
        }

        return contact;
    }

    public ArrayNode getArray(String key, ObjectNode node, boolean required, String location, ParseResult result) {
        JsonNode value = node.get(key);
        ArrayNode an = null;
        if (value == null) {
            if (required) {
                result.missing(location, key);
                result.invalid();
            }
        } else if (!value.getNodeType().equals(JsonNodeType.ARRAY)) {
            result.invalidType(location, key, "array", value);
        } else {
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
        } else {
            if (v.getNodeType().equals(JsonNodeType.BOOLEAN)) {
                value = v.asBoolean();
            } else if (v.getNodeType().equals(JsonNodeType.STRING)) {
                String stringValue = v.textValue();
                return Boolean.parseBoolean(stringValue);
            }
        }
        return value;
    }


    public List<Parameter> getParameterList(ArrayNode obj, String location, ParseResult result) {
        List<Parameter> output = new ArrayList<>();
        if (obj == null) {
            return output;
        }
        for (JsonNode item : obj) {
            if (item.getNodeType().equals(JsonNodeType.OBJECT)) {
                Parameter param = getParameter((ObjectNode) item, location, result);
                if (param != null) {
                    output.add(param);
                }
            }
        }
        return output;
    }

    public Parameter getParameter(ObjectNode obj, String location, ParseResult result) {
        if (obj == null) {
            return null;
        }

        String value = getString("in", obj, true, location, result);

        if (StringUtils.isBlank(value)) {
            return null;
        }

        Parameter parameter = null;

        if (QUERY_PARAMETER.equals(value)) {
            parameter = new QueryParameter();
        } else if (HEADER_PARAMETER.equals(value)) {
            parameter = new HeaderParameter();
        } else if (PATH_PARAMETER.equals(value)) {
            parameter = new PathParameter();
        } else if (COOKIE_PARAMETER.equals(value)) {
            parameter = new CookieParameter();
        }

        if (parameter == null) {
            result.invalidType(location, "in", "string", obj);
            return null;
        }

        parameter.setIn(value);
        value = getString("name", obj, true, location, result);
        parameter.setName(value);

        value = getString("description", obj, false, location, result);
        parameter.setDescription(value);

        Boolean required = getBoolean("required", obj, false, location, result);
        if (required != null) {
            parameter.setRequired(required);
        }

        Boolean deprecated = getBoolean("deprecated", obj, false, location, result);
        if (deprecated != null) {
            parameter.setDeprecated(deprecated);
        }

        Boolean allowEmptyValue = getBoolean("allowEmptyValue", obj, false, location, result);
        if (allowEmptyValue != null) {
            parameter.setAllowEmptyValue(allowEmptyValue);
        }

        Boolean explode = getBoolean("explode", obj, false, location, result);
        if (explode != null) {
            parameter.setExplode(explode);
        } else {
            parameter.setExplode(Boolean.FALSE);
        }

        value = getString("style", obj, true, location, result);

        setStyle(value, parameter, location, obj, result);


        //TODO: schema
        //TODO: example
        //TODO: examples
        //TODO: content


        return parameter;
    }

    public void setStyle(String value, Parameter parameter, String location, ObjectNode obj, ParseResult result) {
        if (StringUtils.isBlank(value)) {
            if (QUERY_PARAMETER.equals(parameter.getIn()) || COOKIE_PARAMETER.equals(parameter.getIn())) {
                parameter.setStyle(Parameter.StyleEnum.FORM);
            } else if (PATH_PARAMETER.equals(parameter.getIn()) || HEADER_PARAMETER.equals(parameter.getIn())) {
                parameter.setStyle(Parameter.StyleEnum.SIMPLE);
            }
        } else {
            if (value.equals(Parameter.StyleEnum.FORM.toString())) {
                parameter.setStyle(Parameter.StyleEnum.FORM);
            } else if (value.equals(Parameter.StyleEnum.DEEPOBJECT.toString())) {
                parameter.setStyle(Parameter.StyleEnum.DEEPOBJECT);
            } else if (value.equals(Parameter.StyleEnum.LABEL.toString())) {
                parameter.setStyle(Parameter.StyleEnum.LABEL);
            } else if (value.equals(Parameter.StyleEnum.MATRIX.toString())) {
                parameter.setStyle(Parameter.StyleEnum.MATRIX);
            } else if (value.equals(Parameter.StyleEnum.PIPEDELIMITED.toString())) {
                parameter.setStyle(Parameter.StyleEnum.PIPEDELIMITED);
            } else if (value.equals(Parameter.StyleEnum.SIMPLE.toString())) {
                parameter.setStyle(Parameter.StyleEnum.SIMPLE);
            } else if (value.equals(Parameter.StyleEnum.SPACEDELIMITED.toString())) {
                parameter.setStyle(Parameter.StyleEnum.SPACEDELIMITED);
            } else {
                result.invalidType(location, "style", "string", obj);
            }
        }
    }

    public ApiResponses getResponses(ObjectNode node, String location, ParseResult result) {
        if (node == null) {
            return null;
        }

        ApiResponses apiResponses = new ApiResponses();
        Set<String> keys = getKeys(node);

        for (String key : keys) {
            //S ystem.out.println("KEY: " + key);
            if (key.startsWith("x-")) {
                // TODO: check the extension for this object.
            } else {
                ObjectNode obj = getObject(key, node, false, location + "responses", result);
                ApiResponse response = getResponse(obj, location + "." + key, result);
                apiResponses.put(key, response);
            }
        }

        return apiResponses;
    }

    public ApiResponse getResponse(ObjectNode node, String location, ParseResult result) {
        if (node == null) {
            return null;
        }

        ApiResponse apiResponse = new ApiResponse();
        JsonNode ref = node.get("$ref");
        if (ref != null) {
            if (ref.getNodeType().equals(JsonNodeType.STRING)) {
                //return refResponse((TextNode) ref, location, result);
            } else {
                result.invalidType(location, "$ref", "string", node);
                return null;
            }
        }

        String value = getString("description", node, true, location, result);
        //System.out.println("VALUE DESC: " + value);
        apiResponse.description(value);


        ObjectNode headersNode = getObject("headers", node, false, location, result);
        if (headersNode != null) {
            // TODO
            /*Map<String, EncodingProperty> headers = Json.mapper().convertValue(headersNode,
                    Json.mapper().getTypeFactory().constructMapType(Map.class, String.class, EncodingProperty.class));
            output.headers(headers);*/
        }


        // extra keys
        Set<String> keys = getKeys(node);
        for (String key : keys) {
            if (key.startsWith("x-")) {
                //output.setVendorExtension(key, extension(node.get(key)));
            } else if (!RESPONSE_KEYS.contains(key)) {
                result.extra(location, key, node.get(key));
            }
        }
        return apiResponse;
    }

    public List<String> tagStrings(ArrayNode nodes, String location, ParseResult result) {
        if (nodes == null)
            return null;

        List<String> tags = new ArrayList<>();

        for (JsonNode node : nodes) {
            if (node.getNodeType().equals(JsonNodeType.STRING)) {
                tags.add(node.textValue());
            }
        }
        return tags;
    }


    public Operation getOperation(ObjectNode obj, String location, ParseResult result) {
        if (obj == null) {
            return null;
        }
        Operation operation = new Operation();
        ArrayNode array = getArray("tags", obj, false, location, result);
        List<String> tags = tagStrings(array, location, result);
        if (tags != null) {
            operation.setTags(tags);
        }
        String value = getString("summary", obj, false, location, result);
        operation.setSummary(value);

        value = getString("description", obj, false, location, result);
        operation.setDescription(value);

        ObjectNode externalDocs = getObject("externalDocs", obj, false, location, result);
        ExternalDocumentation docs = getExternalDocs(externalDocs, location, result);
        operation.setExternalDocs(docs);

        value = getString("operationId", obj, false, location, result);
        operation.operationId(value);

        ArrayNode parameters = getArray("parameters", obj, false, location, result);
        operation.setParameters(getParameterList(parameters, location, result));

        ObjectNode responsesNode = getObject("responses", obj, false, location, result);

        ApiResponses responses = getResponses(responsesNode, "responses", result);
        operation.setResponses(responses);


        final ObjectNode requestObjectNode = getObject("requestBody", obj, false, location, result);
        operation.setRequestBody(getRequestBody(requestObjectNode, location, result));


        array = getArray("schemes", obj, false, location, result);
        if (array != null) {
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
        if (deprecated != null) {
            operation.setDeprecated(deprecated);
        }
        array = getArray("security", obj, false, location, result);
        List<SecurityRequirement> security = securityRequirements(array, location, result);
        if (security != null) {
            List<Map<String, List<String>>> ss = new ArrayList<>();
            /*for(SecurityRequirement s : security) {
                if(s.getRequirements() != null && s.getRequirements().size() > 0) {
                    ss.add(s.getRequirements());
                }
            }*/
            //output.setSecurity(ss);
        }

        // extra keys
        Set<String> keys = getKeys(obj);
        for (String key : keys) {
            if (key.startsWith("x-")) {
                //output.setVendorExtension(key, extension(obj.get(key)));
            } else if (!OPERATION_KEYS.contains(key)) {
                result.extra(location, key, obj.get(key));
            }
        }

        return operation;
    }

    public List<SecurityRequirement> securityRequirements(ArrayNode node, String location, ParseResult result) {
        if (node == null)
            return null;

        List<SecurityRequirement> output = new ArrayList<>();

        for (JsonNode item : node) {
            SecurityRequirement security = new SecurityRequirement();
            if (item.getNodeType().equals(JsonNodeType.OBJECT)) {
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

    protected RequestBody getRequestBody(ObjectNode node, String location, ParseResult result) {
        if (node == null){
            return null;
        }
        final RequestBody body = new RequestBody();

        final String description = getString("description", node, false, location, result);
        final Boolean required = getBoolean("required", node, false, location, result);

        body.setDescription(description);
        body.setRequired(required);

        final ObjectNode contentNode = getObject("content", node, false, location, result);
        // TODO parse content and media type objects.
        return body;
    }




    protected static class ParseResult {
        private boolean valid = true;
        private Map<Location, JsonNode> extra = new LinkedHashMap<>();
        private Map<Location, JsonNode> unsupported = new LinkedHashMap<>();
        private Map<Location, String> invalidType = new LinkedHashMap<>();
        private List<Location> missing = new ArrayList<>();

        public ParseResult() {
        }

        public void extra(String location, String key, JsonNode value) {
            extra.put(new Location(location, key), value);
        }

        public void missing(String location, String key) {
            missing.add(new Location(location, key));
        }

        public void invalidType(String location, String key, String expectedType, JsonNode value) {
            invalidType.put(new Location(location, key), expectedType);
        }

        public void invalid() {
            this.valid = false;
        }

        public List<String> getMessages() {
            List<String> messages = new ArrayList<String>();
            for (Location l : extra.keySet()) {
                String location = l.location.equals("") ? "" : l.location + ".";
                String message = "attribute " + location + l.key + " is unexpected";
                messages.add(message);
            }
            for (Location l : invalidType.keySet()) {
                String location = l.location.equals("") ? "" : l.location + ".";
                String message = "attribute " + location + l.key + " is not of type `" + invalidType.get(l) + "`";
                messages.add(message);
            }
            for (Location l : missing) {
                String location = l.location.equals("") ? "" : l.location + ".";
                String message = "attribute " + location + l.key + " is missing";
                messages.add(message);
            }
            for (Location l : unsupported.keySet()) {
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