package io.swagger.v3.parser.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.SpecVersion;
import io.swagger.v3.oas.models.callbacks.Callback;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.links.Link;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ByteArraySchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.DateSchema;
import io.swagger.v3.oas.models.media.DateTimeSchema;
import io.swagger.v3.oas.models.media.Discriminator;
import io.swagger.v3.oas.models.media.Encoding;
import io.swagger.v3.oas.models.media.JsonSchema;
import io.swagger.v3.oas.models.media.MapSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.XML;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.parameters.CookieParameter;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.Parameter.StyleEnum;
import io.swagger.v3.oas.models.parameters.PathParameter;
import io.swagger.v3.oas.models.parameters.QueryParameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.servers.ServerVariable;
import io.swagger.v3.oas.models.servers.ServerVariables;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import io.swagger.v3.core.util.Json;

import io.swagger.v3.parser.extensions.JsonSchemaParserExtension;
import org.apache.commons.lang3.StringUtils;

import static io.swagger.v3.core.util.RefUtils.extractSimpleName;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import static java.util.Collections.emptySet;


public class OpenAPIDeserializer {

	protected static Set<String> JSON_SCHEMA_2020_12_TYPES = new LinkedHashSet<>(Arrays.asList("null", "boolean",
			"object", "array", "number", "string", "integer"));

	protected static Set<String> ROOT_KEYS = new LinkedHashSet<>(Arrays.asList("openapi", "info", "servers", "paths",
			"components", "security", "tags", "externalDocs"));
	protected static Set<String> INFO_KEYS = new LinkedHashSet<>(Arrays.asList("title", "description", "termsOfService"
			, "contact", "license", "version"));
	protected static Set<String> CONTACT_KEYS = new LinkedHashSet<>(Arrays.asList("name", "url", "email"));
	protected static Set<String> LICENSE_KEYS = new LinkedHashSet<>(Arrays.asList("name", "url"));
	protected static Set<String> TAG_KEYS = new LinkedHashSet<>(Arrays.asList("description", "name", "externalDocs"));
	protected static Set<String> RESPONSE_KEYS = new LinkedHashSet<>(Arrays.asList("$ref", "description", "headers",
			"content", "links"));
	protected static Set<String> SERVER_KEYS = new LinkedHashSet<>(Arrays.asList("url", "description", "variables"));
	protected static Set<String> SERVER_VARIABLE_KEYS = new LinkedHashSet<>(Arrays.asList("enum", "default",
			"description"));
	protected static Set<String> PATHITEM_KEYS = new LinkedHashSet<>(Arrays.asList("$ref", "summary", "description",
			"get", "put", "post", "delete", "head", "patch", "options", "trace", "servers", "parameters"));
	protected static Set<String> OPERATION_KEYS = new LinkedHashSet<>(Arrays.asList("tags", "summary", "description",
			"externalDocs", "operationId", "parameters", "requestBody", "responses", "callbacks", "deprecated",
			"security",
			"servers"));
	protected static Set<String> PARAMETER_KEYS = new LinkedHashSet<>(Arrays.asList("$ref", "name", "in", "description"
			, "required", "deprecated", "allowEmptyValue", "style", "explode", "allowReserved", "schema", "example",
			"examples"
			, "content"));
	protected static Set<String> REQUEST_BODY_KEYS = new LinkedHashSet<>(Arrays.asList("$ref", "description", "content"
			, "required"));
	protected static Set<String> SECURITY_SCHEME_KEYS = new LinkedHashSet<>(Arrays.asList("$ref", "type", "name", "in"
			, "description", "flows", "scheme", "bearerFormat", "openIdConnectUrl"));
	protected static Set<String> EXTERNAL_DOCS_KEYS = new LinkedHashSet<>(Arrays.asList("description", "url"));
	protected static Set<String> COMPONENTS_KEYS = new LinkedHashSet<>(Arrays.asList("schemas", "responses",
			"parameters", "examples", "requestBodies", "headers", "securitySchemes", "links", "callbacks"));
	protected static Set<String> SCHEMA_KEYS = new LinkedHashSet<>(Arrays.asList("$ref", "title", "multipleOf",
			"maximum", "format", "exclusiveMaximum", "minimum", "exclusiveMinimum", "maxLength", "minLength",
			"pattern", "maxItems", "minItems", "uniqueItems", "maxProperties", "minProperties", "required", "enum", "type",
			"allOf", "oneOf", "anyOf", "not", "items", "properties", "additionalProperties", "description", "default",
			"nullable", "discriminator", "readOnly", "writeOnly", "xml", "externalDocs", "example", "deprecated"));
	protected static Set<String> EXAMPLE_KEYS = new LinkedHashSet<>(Arrays.asList("$ref", "summary", "description",
			"value", "externalValue"));
	protected static Set<String> HEADER_KEYS = new LinkedHashSet<>(Arrays.asList("$ref", "name", "in", "description",
			"required", "deprecated", "allowEmptyValue", "style", "explode", "allowReserved", "schema", "example",
			"examples",
			"content"));
	protected static Set<String> LINK_KEYS = new LinkedHashSet<>(Arrays.asList("$ref", "operationRef", "operationId",
			"parameters", "requestBody", "description", "server"));
	protected static Set<String> MEDIATYPE_KEYS = new LinkedHashSet<>(Arrays.asList("schema", "example", "examples",
			"encoding"));
	protected static Set<String> XML_KEYS = new LinkedHashSet<>(Arrays.asList("name", "namespace", "prefix",
			"attribute", "wrapped"));
	protected static Set<String> OAUTHFLOW_KEYS = new LinkedHashSet<>(Arrays.asList("authorizationUrl", "tokenUrl",
			"refreshUrl", "scopes"));
	protected static Set<String> OAUTHFLOWS_KEYS = new LinkedHashSet<>(Arrays.asList("implicit", "password",
			"clientCredentials", "authorizationCode"));
	protected static Set<String> ENCODING_KEYS = new LinkedHashSet<>(Arrays.asList("contentType", "headers", "style",
			"explode", "allowReserved"));

	// 3.1
	protected static Set<String> ROOT_KEYS_31 = new LinkedHashSet<>(Arrays.asList("openapi", "info", "servers", "paths",
			"components", "security", "tags", "externalDocs", "webhooks", "jsonSchemaDialect"));
	protected static Set<String> RESERVED_KEYWORDS_31 = new LinkedHashSet<>(Arrays.asList("x-oai-","x-oas-"));
	protected static Set<String> INFO_KEYS_31 = new LinkedHashSet<>(Arrays.asList("title","summary", "description", "termsOfService"
			, "contact", "license", "version"));
	protected static Set<String> CONTACT_KEYS_31 = new LinkedHashSet<>(Arrays.asList("name", "url", "email"));
	protected static Set<String> LICENSE_KEYS_31 = new LinkedHashSet<>(Arrays.asList("name", "url", "identifier"));
	protected static Set<String> TAG_KEYS_31 = new LinkedHashSet<>(Arrays.asList("description", "name", "externalDocs"));
	protected static Set<String> RESPONSE_KEYS_31 = new LinkedHashSet<>(Arrays.asList("$ref", "description", "headers",
			"content", "links"));
	protected static Set<String> SERVER_KEYS_31 = new LinkedHashSet<>(Arrays.asList("url", "description", "variables"));
	protected static Set<String> SERVER_VARIABLE_KEYS_31 = new LinkedHashSet<>(Arrays.asList("enum", "default",
			"description"));
	protected static Set<String> PATHITEM_KEYS_31 = new LinkedHashSet<>(Arrays.asList("$ref", "summary", "description",
			"get", "put", "post", "delete", "head", "patch", "options", "trace", "servers", "parameters"));
	protected static Set<String> OPERATION_KEYS_31 = new LinkedHashSet<>(Arrays.asList("tags", "summary", "description",
			"externalDocs", "operationId", "parameters", "requestBody", "responses", "callbacks", "deprecated",
			"security",
			"servers"));
	protected static Set<String> PARAMETER_KEYS_31 = new LinkedHashSet<>(Arrays.asList("$ref", "name", "in", "description"
			, "required", "deprecated", "allowEmptyValue", "style", "explode", "allowReserved", "schema", "example",
			"examples"
			, "content"));
	protected static Set<String> REQUEST_BODY_KEYS_31 = new LinkedHashSet<>(Arrays.asList("$ref", "description", "content"
			, "required"));
	protected static Set<String> SECURITY_SCHEME_KEYS_31 = new LinkedHashSet<>(Arrays.asList("$ref", "type", "name", "in"
			, "description", "flows", "scheme", "bearerFormat", "openIdConnectUrl"));
	protected static Set<String> EXTERNAL_DOCS_KEYS_31 = new LinkedHashSet<>(Arrays.asList("description", "url"));
	protected static Set<String> COMPONENTS_KEYS_31 = new LinkedHashSet<>(Arrays.asList("schemas", "responses", "pathItems",
			"parameters", "examples", "requestBodies", "headers", "securitySchemes", "links", "callbacks"));

	protected static Set<String> SCHEMA_KEYS_31 = new LinkedHashSet<>(Arrays.asList("$ref", "title", "multipleOf",
			"maximum", "format", "exclusiveMaximum", "minimum", "exclusiveMinimum", "maxLength", "minLength",
			"pattern", "maxItems", "minItems", "uniqueItems", "maxProperties", "minProperties", "required", "enum", "type",
			"allOf", "oneOf", "anyOf", "not", "items", "properties", "additionalProperties", "description",
            "default", "discriminator", "readOnly", "writeOnly", "xml", "externalDocs", "example", "deprecated",
			"const", "examples", "$id", "$comment", "if", "then", "else", "unevaluatedProperties","unevaluatedItems", "prefixItems",
            "contains","contentEncoding","contentMediaType","$anchor","$schema","contentSchema","propertyNames",
            "dependentSchemas","dependentRequired","minContains","maxContains","patternProperties"));
	protected static Set<String> EXAMPLE_KEYS_31 = new LinkedHashSet<>(Arrays.asList("$ref", "summary", "description",
			"value", "externalValue"));
	protected static Set<String> HEADER_KEYS_31 = new LinkedHashSet<>(Arrays.asList("$ref", "name", "in", "description",
			"required", "deprecated", "allowEmptyValue", "style", "explode", "allowReserved", "schema", "example",
			"examples",
			"content"));
	protected static Set<String> LINK_KEYS_31 = new LinkedHashSet<>(Arrays.asList("$ref", "operationRef", "operationId",
			"parameters", "requestBody", "description", "server"));
	protected static Set<String> MEDIATYPE_KEYS_31 = new LinkedHashSet<>(Arrays.asList("schema", "example", "examples",
			"encoding"));
	protected static Set<String> XML_KEYS_31 = new LinkedHashSet<>(Arrays.asList("name", "namespace", "prefix",
			"attribute", "wrapped"));
	protected static Set<String> OAUTHFLOW_KEYS_31 = new LinkedHashSet<>(Arrays.asList("authorizationUrl", "tokenUrl",
			"refreshUrl", "scopes"));
	protected static Set<String> OAUTHFLOWS_KEYS_31 = new LinkedHashSet<>(Arrays.asList("implicit", "password",
			"clientCredentials", "authorizationCode"));
	protected static Set<String> ENCODING_KEYS_31 = new LinkedHashSet<>(Arrays.asList("contentType", "headers", "style",
			"explode", "allowReserved"));

	protected static Map<String, Map<String, Set<String>>> KEYS = new LinkedHashMap<>();

    protected static Set<JsonNodeType> validNodeTypes =  new LinkedHashSet<>(
            Arrays.asList(JsonNodeType.OBJECT, JsonNodeType.STRING));


	static {
		Map<String, Set<String>> keys30 = new LinkedHashMap<>();
		Map<String, Set<String>> keys31 = new LinkedHashMap<>();
		keys30.put("ROOT_KEYS", ROOT_KEYS);
		keys30.put("INFO_KEYS", INFO_KEYS);
		keys30.put("CONTACT_KEYS", CONTACT_KEYS);
		keys30.put("LICENSE_KEYS", LICENSE_KEYS);
		keys30.put("TAG_KEYS", TAG_KEYS);
		keys30.put("RESPONSE_KEYS", RESPONSE_KEYS);
		keys30.put("SERVER_KEYS", SERVER_KEYS);
		keys30.put("SERVER_VARIABLE_KEYS", SERVER_VARIABLE_KEYS);
		keys30.put("PATHITEM_KEYS", PATHITEM_KEYS);
		keys30.put("OPERATION_KEYS", OPERATION_KEYS);
		keys30.put("PARAMETER_KEYS", PARAMETER_KEYS);
		keys30.put("REQUEST_BODY_KEYS", REQUEST_BODY_KEYS);
		keys30.put("SECURITY_SCHEME_KEYS", SECURITY_SCHEME_KEYS);
		keys30.put("EXTERNAL_DOCS_KEYS", EXTERNAL_DOCS_KEYS);
		keys30.put("COMPONENTS_KEYS", COMPONENTS_KEYS);
		keys30.put("SCHEMA_KEYS", SCHEMA_KEYS);
		keys30.put("EXAMPLE_KEYS", EXAMPLE_KEYS);
		keys30.put("HEADER_KEYS", HEADER_KEYS);
		keys30.put("LINK_KEYS", LINK_KEYS);
		keys30.put("MEDIATYPE_KEYS", MEDIATYPE_KEYS);
		keys30.put("XML_KEYS", XML_KEYS);
		keys30.put("OAUTHFLOW_KEYS", OAUTHFLOW_KEYS);
		keys30.put("OAUTHFLOWS_KEYS", OAUTHFLOWS_KEYS);
		keys30.put("ENCODING_KEYS", ENCODING_KEYS);
		keys31.put("ROOT_KEYS", ROOT_KEYS_31);
		keys31.put("INFO_KEYS", INFO_KEYS_31);
		keys31.put("CONTACT_KEYS", CONTACT_KEYS_31);
		keys31.put("LICENSE_KEYS", LICENSE_KEYS_31);
		keys31.put("TAG_KEYS", TAG_KEYS_31);
		keys31.put("RESPONSE_KEYS", RESPONSE_KEYS_31);
		keys31.put("SERVER_KEYS", SERVER_KEYS_31);
		keys31.put("SERVER_VARIABLE_KEYS", SERVER_VARIABLE_KEYS_31);
		keys31.put("PATHITEM_KEYS", PATHITEM_KEYS_31);
		keys31.put("OPERATION_KEYS", OPERATION_KEYS_31);
		keys31.put("PARAMETER_KEYS", PARAMETER_KEYS_31);
		keys31.put("REQUEST_BODY_KEYS", REQUEST_BODY_KEYS_31);
		keys31.put("SECURITY_SCHEME_KEYS", SECURITY_SCHEME_KEYS_31);
		keys31.put("EXTERNAL_DOCS_KEYS", EXTERNAL_DOCS_KEYS_31);
		keys31.put("COMPONENTS_KEYS", COMPONENTS_KEYS_31);
		keys31.put("SCHEMA_KEYS", SCHEMA_KEYS_31);
		keys31.put("EXAMPLE_KEYS", EXAMPLE_KEYS_31);
		keys31.put("HEADER_KEYS", HEADER_KEYS_31);
		keys31.put("LINK_KEYS", LINK_KEYS_31);
		keys31.put("MEDIATYPE_KEYS", MEDIATYPE_KEYS_31);
		keys31.put("XML_KEYS", XML_KEYS_31);
		keys31.put("OAUTHFLOW_KEYS", OAUTHFLOW_KEYS_31);
		keys31.put("OAUTHFLOWS_KEYS", OAUTHFLOWS_KEYS_31);
		keys31.put("ENCODING_KEYS", ENCODING_KEYS_31);
		keys31.put("RESERVED_KEYWORDS", RESERVED_KEYWORDS_31);
		KEYS.put("openapi30", keys30);
		KEYS.put("openapi31", keys31);

	}

	private static final String QUERY_PARAMETER = "query";
	private static final String COOKIE_PARAMETER = "cookie";
	private static final String PATH_PARAMETER = "path";
	private static final String HEADER_PARAMETER = "header";
	private static final Pattern RFC3339_DATE_TIME_PATTERN = Pattern.compile("^(\\d{4})-(\\d{2})-(\\d{2})T(\\d{2}):" +
			"(\\d{2}):(\\d{2})(\\.\\d+)?((Z)|([+-]\\d{2}:\\d{2}))$");
	private static final Pattern RFC3339_DATE_PATTERN = Pattern.compile("^(\\d{4})-(\\d{2})-(\\d{2})$");
	private static final String REFERENCE_SEPARATOR = "#/";
	private Components components;
	private JsonNode rootNode;
	private Map<String, Object> rootMap;
	private String basePath;
	private final Set<String> operationIDs = new HashSet<>();
    private Map<String,String> localSchemaRefs = new HashMap<>();

	public SwaggerParseResult deserialize(JsonNode rootNode) {
		return deserialize(rootNode, null);
	}

    public SwaggerParseResult deserialize(JsonNode rootNode, String path) {
        return deserialize(rootNode,path, new ParseOptions());
    }

    public SwaggerParseResult deserialize(JsonNode rootNode, String path, ParseOptions options) {
        return deserialize(rootNode,path, new ParseOptions(), false);
    }

    public SwaggerParseResult deserialize(JsonNode rootNode, String path, ParseOptions options, boolean isOaiAuthor) {
        basePath = path;
        this.rootNode = rootNode;
        rootMap = new ObjectMapper().convertValue(rootNode, Map.class);
		SwaggerParseResult result = new SwaggerParseResult();
        try {
            ParseResult rootParse = new ParseResult();
            rootParse.setOaiAuthor(options.isOaiAuthor());
            rootParse.setInferSchemaType(options.isInferSchemaType());
            rootParse.setAllowEmptyStrings(options.isAllowEmptyString());
            rootParse.setValidateInternalRefs(options.isValidateInternalRefs());
            OpenAPI api = parseRoot(rootNode, rootParse, path);
            result.openapi31(rootParse.isOpenapi31());
            result.setOpenAPI(api);
            result.setMessages(rootParse.getMessages());
        } catch (Exception e) {
            result.setMessages(Arrays.asList(e.getMessage()));
        }
		return result;
	}

	public OpenAPI parseRoot(JsonNode node, ParseResult result, String path) {
		String location = "";
		OpenAPI openAPI = new OpenAPI();
		if (node.getNodeType().equals(JsonNodeType.OBJECT)) {
			ObjectNode rootNode = (ObjectNode) node;

			// required
			String value = getString("openapi", rootNode, true, location, result);

			// we don't even try if the version isn't there
			if (value == null || (!value.startsWith("3.0") && !value.startsWith("3.1"))) {
				return null;
			} else if (value.startsWith("3.1")) {
				result.openapi31(true);
                openAPI.setSpecVersion(SpecVersion.V31);
			}
            if (!value.startsWith("3.0.") && !value.startsWith("3.1.")){
                result.warning(location, "The provided definition does not specify a valid version field");
            }
            openAPI.setOpenapi(value);


			ObjectNode obj = getObject("info", rootNode, true, location, result);
			if (obj != null) {
				Info info = getInfo(obj, "info", result);
				openAPI.setInfo(info);
			}

			obj = getObject("components", rootNode, false, location, result);
			if (obj != null) {
				Components components = getComponents(obj, "components", result);
				openAPI.setComponents(components);
				this.components = components;
                if(result.validateInternalRefs) {
                    /* TODO currently only capable of validating if ref is to root schema withing #/components/schemas
                     * need to evaluate json pointer instead to also allow validation of nested schemas
                     * e.g. #/components/schemas/foo/properties/bar
                     */
                    for (String schema : localSchemaRefs.keySet()) {
                        if (components.getSchemas().get(schema) == null) {
                            result.invalidType(localSchemaRefs.get(schema), schema, "schema", rootNode);
                        }
                    }
                }
			}

			boolean pathsRequired = true;
			if (result.isOpenapi31()) {
				pathsRequired = false;
			}
			obj = getObject("paths", rootNode, pathsRequired, location, result);
			if (obj != null) {
				Paths paths = getPaths(obj, "paths", result);
				openAPI.setPaths(paths);
			}


			ArrayNode array = getArray("servers", rootNode, false, location, result);
			if (array != null && array.size() > 0) {
				openAPI.setServers(getServersList(array, String.format("%s.%s", location, "servers"), result, path));
			} else {
				Server defaultServer = new Server();
				defaultServer.setUrl("/");
				List<Server> servers = new ArrayList<>();
				servers.add(defaultServer);
				openAPI.setServers(servers);
			}

			obj = getObject("externalDocs", rootNode, false, location, result);
			if (obj != null) {
				ExternalDocumentation externalDocs = getExternalDocs(obj, "externalDocs", result);
				openAPI.setExternalDocs(externalDocs);
			}

			array = getArray("tags", rootNode, false, location, result);
			if (array != null && array.size() > 0) {
				openAPI.setTags(getTagList(array, "tags", result));
			}

			array = getArray("security", rootNode, false, location, result);
			if (array != null && array.size() > 0) {
				List<SecurityRequirement> securityRequirements = getSecurityRequirementsList(array, "security",
						result);
				if (securityRequirements != null && securityRequirements.size() > 0) {
					openAPI.setSecurity(securityRequirements);
				}
			}

			if (result.isOpenapi31()) {
				obj = getObject("webhooks", rootNode, false, location, result);
				if (obj != null) {
					Map<String, PathItem> webhooks = getWebhooks(obj, "webhooks", result);
					openAPI.setWebhooks(webhooks);
				}
			}


			Map<String, Object> extensions = getExtensions(rootNode);
			if (extensions != null && extensions.size() > 0) {
				openAPI.setExtensions(extensions);
			}

			if (result.isOpenapi31()) {
				value = getString("jsonSchemaDialect", rootNode, false, location, result);
				if (value != null) {
					if (isValidURI(value)) {
						openAPI.setJsonSchemaDialect(value);
					}else{
						result.warning(location,"jsonSchemaDialect. Invalid url: " + value);
					}
				}
			}

			if(result.isOpenapi31() && openAPI.getComponents() == null && openAPI.getPaths() == null && openAPI.getWebhooks() == null){
				result.warning(location, "The OpenAPI document MUST contain at least one paths field, a components field or a webhooks field");
			}

			Set<String> keys = getKeys(rootNode);
			Map<String, Set<String>> specKeys = KEYS.get(result.isOpenapi31() ? "openapi31" : "openapi30");
			for (String key : keys) {
				if (!specKeys.get("ROOT_KEYS").contains(key) && !key.startsWith("x-")) {
					result.extra(location, key, node.get(key));
				}
				validateReservedKeywords(specKeys, key, location, result);

			}

		} else {
			result.invalidType(location, "openapi", "object", node);
			result.invalid();
			return null;
		}
		return openAPI;
	}

	boolean isValidURI(String uriString) {
		try {
			URI uri = new URI(uriString);
			return true;
		} catch (Exception exception) {
			return false;
		}
	}

	private void validateReservedKeywords(Map<String, Set<String>> specKeys, String key, String location, ParseResult result) {
		if(!result.isOaiAuthor() && result.isOpenapi31() && specKeys.get("RESERVED_KEYWORDS").stream()
				.filter(rk -> key.startsWith(rk))
				.findAny()
				.orElse(null) != null){
			result.reserved(location, key);
		}
	}

	public String mungedRef(String refString) {
		// Ref: IETF RFC 3966, Section 5.2.2
		if (!refString.contains(":") &&   // No scheme
				!refString.startsWith("#") && // Path is not empty
				!refString.startsWith("/") && // Path is not absolute
				refString.indexOf(".") > 0) { // Path does not start with dot but contains "." (file extension)
			return "./" + refString;
		}
		return null;
	}

	public Map<String, Object> getExtensions(ObjectNode node) {

		//It seems that the expanded content under the JSON format node is not parsed here,
		//Result in the expanded content of all nodes that are not resolved
		//So the expansion node is added here and the content under this node is parsed.
		Map<String, Object> extensions = tryDirectExtensions(node);
		if (extensions.isEmpty()) {
			extensions = tryUnwrapLookupExtensions(node);
		}
		return extensions;
	}

	private Map<String, Object> tryUnwrapLookupExtensions(ObjectNode node) {

		Map<String, Object> extensions = new LinkedHashMap<>();

		JsonNode extensionsNode = node.get("extensions");
		if (Objects.nonNull(extensionsNode) && JsonNodeType.OBJECT.equals(node.getNodeType())) {
			ObjectNode extensionsObjectNode = (ObjectNode) extensionsNode;
			extensions.putAll(tryDirectExtensions(extensionsObjectNode));
		}

		return extensions;
	}

	private Map<String, Object> tryDirectExtensions(ObjectNode node) {
		Map<String, Object> extensions = new LinkedHashMap<>();

		Set<String> keys = getKeys(node);
		for (String key : keys) {
			if (key.startsWith("x-")) {
				extensions.put(key, Json.mapper().convertValue(node.get(key), Object.class));
			}
		}

		return extensions;
	}

	public Components getComponents(ObjectNode obj, String location, ParseResult result) {
		if (obj == null) {
			return null;
		}
		Components components = new Components();

		ObjectNode node = getObject("schemas", obj, false, location, result);
		if (node != null) {
			components.setSchemas(getSchemas(node, String.format("%s.%s", location, "schemas"), result, true));
		}

		node = getObject("responses", obj, false, location, result);
		if (node != null) {
			components.setResponses(getResponses(node, String.format("%s.%s", location, "responses"), result,
					true));
		}
		if(result.isOpenapi31()){
			node = getObject("pathItems", obj, false, location, result);
			if (node != null) {
				components.setPathItems(getPathItems(node, String.format("%s.%s", location, "pathItems"), result, true));
			}
		}

		node = getObject("parameters", obj, false, location, result);
		if (node != null) {
			components.setParameters(getParameters(node, String.format("%s.%s", location, "parameters"), result,
					true));
		}
		node = getObject("examples", obj, false, location, result);
		if (node != null) {
			components.setExamples(getExamples(node, String.format("%s.%s", location, "examples"), result, true));
		}

		node = getObject("requestBodies", obj, false, location, result);
		if (node != null) {
			components.setRequestBodies(getRequestBodies(node, String.format("%s.%s", location, "requestBodies"),
					result, true));
		}

		node = getObject("headers", obj, false, location, result);
		if (node != null) {
			components.setHeaders(getHeaders(node, String.format("%s.%s", location, "headers"), result, true));
		}

		node = getObject("securitySchemes", obj, false, location, result);
		if (node != null) {
			components.setSecuritySchemes(getSecuritySchemes(node, String.format("%s.%s", location,
					"securitySchemes"), result, true));
		}

		node = getObject("links", obj, false, location, result);
		if (node != null) {
			components.setLinks(getLinks(node, String.format("%s.%s", location, "links"), result, true));
		}

		node = getObject("callbacks", obj, false, location, result);
		if (node != null) {
			components.setCallbacks(getCallbacks(node, String.format("%s.%s", location, "callbacks"), result,
					true));
		}
		components.setExtensions(new LinkedHashMap<>());

		Map<String, Object> extensions = getExtensions(obj);
		if (extensions != null && extensions.size() > 0) {
			components.setExtensions(extensions);
		}

		Set<String> keys = getKeys(obj);
		Map<String, Set<String>> specKeys = KEYS.get(result.isOpenapi31() ? "openapi31" : "openapi30");
		for (String key : keys) {
			if (!specKeys.get("COMPONENTS_KEYS").contains(key) && !key.startsWith("x-")) {
				result.extra(location, key, obj.get(key));
			}
			validateReservedKeywords(specKeys, key, location, result);

		}
		return components;
	}

	public List<Tag> getTagList(ArrayNode obj, String location, ParseResult result) {
		if (obj == null) {
			return null;
		}
		List<Tag> tags = new ArrayList<>();
		Set<String> tagsTracker = new HashSet<>();
		for (JsonNode item : obj) {
			if (item.getNodeType().equals(JsonNodeType.OBJECT)) {
				Tag tag = getTag((ObjectNode) item, location, result);
				if (tag != null) {
					tags.add(tag);

					if (tagsTracker.contains((String) tag.getName())) {
						result.uniqueTags(location, tag.getName());
					}

					tagsTracker.add(tag.getName());
				}
			}
		}
		return tags;
	}

	public Tag getTag(ObjectNode obj, String location, ParseResult result) {
		if (obj == null) {
			return null;
		}

		Tag tag = new Tag();

		String value = getString("name", obj, true, location, result);
		if ((result.isAllowEmptyStrings() && value != null) || (!result.isAllowEmptyStrings() && !StringUtils.isBlank(value))) {
			tag.setName(value);
		}

		value = getString("description", obj, false, location, result);
		if ((result.isAllowEmptyStrings() && value != null) || (!result.isAllowEmptyStrings() && !StringUtils.isBlank(value))) {
			tag.setDescription(value);
		}

		ObjectNode docs = getObject("externalDocs", obj, false, location, result);
		ExternalDocumentation externalDocs = getExternalDocs(docs, String.format("%s.%s", location,
				"externalDocs"), result);
		if (externalDocs != null) {
			tag.setExternalDocs(externalDocs);
		}

		Map<String, Object> extensions = getExtensions(obj);
		if (extensions != null && extensions.size() > 0) {
			tag.setExtensions(extensions);
		}

		Set<String> keys = getKeys(obj);
		Map<String, Set<String>> specKeys = KEYS.get(result.isOpenapi31() ? "openapi31" : "openapi30");
		for (String key : keys) {
			if (!specKeys.get("TAG_KEYS").contains(key) && !key.startsWith("x-")) {
				result.extra(location, key, obj.get(key));
			}
			validateReservedKeywords(specKeys, key, location, result);
		}

		return tag;
	}


	public List<Server> getServersList(ArrayNode obj, String location, ParseResult result, String path) {

		List<Server> servers = new ArrayList<>();
		if (obj == null) {
			return null;

		}
		for (JsonNode item : obj) {
			if (item.getNodeType().equals(JsonNodeType.OBJECT)) {
				Server server = getServer((ObjectNode) item, location, result, path);
				if (server != null) {
					servers.add(server);
				} else {
					Server defaultServer = new Server();
					defaultServer.setUrl("/");
					servers.add(defaultServer);
				}
			}
		}
		return servers;
	}

	public List<Server> getServersList(ArrayNode obj, String location, ParseResult result) {
		return getServersList(obj, location, result, null);
	}

	public Server getServer(ObjectNode obj, String location, ParseResult result) {
		return getServer(obj, location, result, null);
	}

	public Server getServer(ObjectNode obj, String location, ParseResult result, String path) {
		if (obj == null) {
			return null;
		}

		Server server = new Server();

		if (obj.get("variables") != null) {
			ObjectNode variables = getObject("variables", obj, false, location, result);
			ServerVariables serverVariables = getServerVariables(variables, String.format("%s.%s", location,
					"variables"), result);
			if (serverVariables != null && serverVariables.size() > 0) {
				server.setVariables(serverVariables);
			}
		}

		String value = getString("url", obj, true, location, result);
		if ((result.isAllowEmptyStrings() && value != null) || (!result.isAllowEmptyStrings() && !StringUtils.isBlank(value))) {
			if (!isValidURL(value) && path != null) {
				try {
					final URI absURI = new URI(path.replaceAll("\\\\", "/"));
					if ("http".equals(absURI.getScheme()) || "https".equals(absURI.getScheme())) {
						value = absURI.resolve(new URI(value)).toString();
					}
				} catch (URISyntaxException e) {
					final int openBrace = value.indexOf("{");
					final int closeBrace = value.indexOf("}");
					if (openBrace > -1 && closeBrace > -1) {
						String variable = value.substring(openBrace + 1, closeBrace);
						if (server.getVariables() != null) {
							if (!server.getVariables().containsKey(variable)) {
								result.warning(location, "invalid url : " + value);
							}
						}
					}
				}
			}
			server.setUrl(value);
		}

		value = getString("description", obj, false, location, result);
		if ((result.isAllowEmptyStrings() && value != null) || (!result.isAllowEmptyStrings() && !StringUtils.isBlank(value))) {
			server.setDescription(value);
		}

		Map<String, Object> extensions = getExtensions(obj);
		if (extensions != null && extensions.size() > 0) {
			server.setExtensions(extensions);
		}

		Set<String> keys = getKeys(obj);
		Map<String, Set<String>> specKeys = KEYS.get(result.isOpenapi31() ? "openapi31" : "openapi30");
		for (String key : keys) {
			if (!specKeys.get("SERVER_KEYS").contains(key) && !key.startsWith("x-")) {
				result.extra(location, key, obj.get(key));
			}
			validateReservedKeywords(specKeys, key, location, result);
		}


		return server;
	}

	boolean isValidURL(String urlString) {
		try {
			URL url = new URL(urlString);
			url.toURI();
			return true;
		} catch (Exception exception) {
			return false;
		}
	}

	public ServerVariables getServerVariables(ObjectNode obj, String location, ParseResult result) {
		ServerVariables serverVariables = new ServerVariables();
		if (obj == null) {
			return null;
		}

		Set<String> serverKeys = getKeys(obj);
		for (String serverName : serverKeys) {
			JsonNode serverValue = obj.get(serverName);
			ObjectNode server = (ObjectNode) serverValue;
			ServerVariable serverVariable = getServerVariable(server, String.format("%s.%s", location, serverName)
					, result);
			serverVariables.addServerVariable(serverName, serverVariable);
		}

		return serverVariables;
	}

	public ServerVariable getServerVariable(ObjectNode obj, String location, ParseResult result) {
		if (obj == null) {
			return null;
		}

		ServerVariable serverVariable = new ServerVariable();

		ArrayNode arrayNode = getArray("enum", obj, false, location, result);
		if (arrayNode != null) {
			if (arrayNode.size() == 0 && result.isOpenapi31()) {
				result.warning(location, "enum array MUST NOT be empty");
			} else {
				List<String> _enum = new ArrayList<>();
				for (JsonNode n : arrayNode) {
					if (n.isValueNode()) {
						_enum.add(n.asText());
						serverVariable.setEnum(_enum);
					} else {
						result.invalidType(location, "enum", "value", n);
					}
				}
			}
		}
		String value = getString("default", obj, true, String.format("%s.%s", location, "default"), result);
		if ((result.isAllowEmptyStrings() && value != null) || (!result.isAllowEmptyStrings() && !StringUtils.isBlank(value))) {
			serverVariable.setDefault(value);
		}

		value = getString("description", obj, false, String.format("%s.%s", location, "description"), result);
		if ((result.isAllowEmptyStrings() && value != null) || (!result.isAllowEmptyStrings() && !StringUtils.isBlank(value))) {
			serverVariable.setDescription(value);
		}

		Map<String, Object> extensions = getExtensions(obj);
		if (extensions != null && extensions.size() > 0) {
			serverVariable.setExtensions(extensions);
		}

		Set<String> keys = getKeys(obj);
		Map<String, Set<String>> specKeys = KEYS.get(result.isOpenapi31() ? "openapi31" : "openapi30");
		for (String key : keys) {
			if (!specKeys.get("SERVER_VARIABLE_KEYS").contains(key) && !key.startsWith("x-")) {
				result.extra(location, key, obj.get(key));
			}
			validateReservedKeywords(specKeys, key, location, result);
		}

		return serverVariable;
	}

	//PathsObject
	public Paths getPaths(ObjectNode obj, String location, ParseResult result) {
		final Paths paths = new Paths();
		if (getPathItems(obj, location, result, paths, false)) {
			return paths;
		}
		return null;
	}

		public Map<String, PathItem> getPathItems(ObjectNode node, String location, ParseResult result,
											  boolean underComponents) {
		if (node == null) {
			return null;
		}
		Map<String, PathItem> pathItems = new LinkedHashMap<>();
		Set<String> keys = getKeys(node);
		for (String key : keys) {
			if (underComponents) {
				if (!Pattern.matches("^[a-zA-Z0-9\\.\\-_]+$",
						key)) {
					result.warning(location, "PathItem key " + key + " doesn't adhere to regular expression " +
							"^[a-zA-Z0-9\\.\\-_]+$");
				}
			}
			PathItem pathItem = getPathItem((ObjectNode) node.get(key), location, result);
			if (pathItem != null) {
				pathItems.put(key, pathItem);
			}
		}
		return pathItems;
	}

	//Webhooks
	public Map<String, PathItem> getWebhooks(ObjectNode obj, String location, ParseResult result) {
		final Map<String, PathItem> webhooks = new LinkedHashMap<>();
		if (getPathItems(obj, location, result, webhooks, true)) {
			return webhooks;
		}
		return null;
	}

	protected boolean getPathItems(ObjectNode obj, String location, ParseResult result, Map<String, PathItem> paths, boolean isWebhook) {
		if (obj == null) {
			return false;
		}
		Set<String> pathKeys = getKeys(obj);
		for (String pathName : pathKeys) {
			JsonNode pathValue = obj.get(pathName);
			if (paths instanceof Paths && pathName.startsWith("x-")) {
				Map<String, Object> extensions = getExtensions(obj);
				if (extensions != null && extensions.size() > 0) {
					((Paths)paths).setExtensions(extensions);
				}
			} else {
				if (!pathValue.getNodeType().equals(JsonNodeType.OBJECT)) {
					result.invalidType(location, pathName, "object", pathValue);
				} else {
					if (!pathName.startsWith("/") && !isWebhook ) {
						result.warning(location, " Resource " + pathName + " should start with /");
					}
					ObjectNode path = (ObjectNode) pathValue;
					PathItem pathObj = getPathItem(path, String.format("%s.'%s'", location, pathName), result);
                    List<String> eachPart = new ArrayList<>();
                    Matcher m = Pattern.compile("\\{(.+?)\\}").matcher(pathName);
                    while (m.find()) {
                        eachPart.add(m.group());
                    }
					eachPart.stream()
							.forEach(part -> {
								String pathParam = part.substring(1, part.length() - 1);
								boolean definedInPathLevel = isPathParamDefined(pathParam,
										pathObj.getParameters());
								if (!definedInPathLevel) {
									List<Operation> operationsInAPath = getAllOperationsInAPath(pathObj);
									operationsInAPath.forEach(operation -> {
										List<Parameter> operationParameters = operation.getParameters();
										if (operationParameters == null) {
											operationParameters = Collections.<Parameter>emptyList();
										}
										operationParameters.forEach(parameter -> {
											if (PATH_PARAMETER.equalsIgnoreCase(parameter.getIn()) && Boolean.FALSE.equals(parameter.getRequired())) {
												result.warning(location,
														"For path parameter " + parameter.getName() + " the required" +
																" " +
																"value should be true");
											}
										});
										if (!isPathParamDefined(pathParam, operationParameters)) {
											result.warning(location + ".'" + pathName + "'", " Declared path " +
													"parameter " + pathParam + " needs to be defined as a path " +
													"parameter " +
													"in path or operation level");
											return;
										}
									});
								}
							});
					paths.put(pathName, pathObj);
				}
			}
		}
		return true;
	}

	private boolean isPathParamDefined(String pathParam, List<Parameter> parameters) {
		if (parameters == null || parameters.isEmpty()) {
			return false;
		} else {
			Parameter pathParamDefined = parameters.stream()
					.filter(parameter -> (parameter.get$ref() != null) || (pathParam.equals(parameter.getName()) &&
							"path".equals(parameter.getIn())))
					.findFirst()
					.orElse(null);
			if (pathParamDefined == null) {
				return false;
			}
		}
		return true;
	}

	private void addToOperationsList(List<Operation> operationsList, Operation operation) {
		if (operation == null) {
			return;
		}
		operationsList.add(operation);
	}

	public List<Operation> getAllOperationsInAPath(PathItem pathObj) {
		List<Operation> operations = new ArrayList<>();
		addToOperationsList(operations, pathObj.getGet());
		addToOperationsList(operations, pathObj.getPut());
		addToOperationsList(operations, pathObj.getPost());
		addToOperationsList(operations, pathObj.getPatch());
		addToOperationsList(operations, pathObj.getDelete());
		addToOperationsList(operations, pathObj.getTrace());
		addToOperationsList(operations, pathObj.getOptions());
		addToOperationsList(operations, pathObj.getHead());
		return operations;
	}

	public PathItem getPathItem(ObjectNode obj, String location, ParseResult result) {


		PathItem pathItem = new PathItem();

		if (obj.get("$ref") != null) {
			JsonNode ref = obj.get("$ref");

			if (ref.getNodeType().equals(JsonNodeType.STRING)) {
				String mungedRef = mungedRef(ref.textValue());
				if (mungedRef != null) {
					pathItem.set$ref(mungedRef);
				} else {
					pathItem.set$ref(ref.textValue());
				}
                if(ref.textValue().startsWith("#/components") && !(ref.textValue().startsWith("#/components/pathItems"))) {
                    result.warning(location, "$ref target "+ref.textValue() +" is not of expected type PathItem");
                }
				if(result.isOpenapi31()){
					String value = getString("summary", obj, false, location, result);
					if (StringUtils.isNotBlank(value)) {
						pathItem.setSummary(value);
					}
					value = getString("description", obj, false, location, result);
					if (StringUtils.isNotBlank(value)) {
						pathItem.setDescription(value);
					}
				}
				return pathItem;
			} else if (ref.getNodeType().equals(JsonNodeType.OBJECT)) {
				ObjectNode node = (ObjectNode) ref;

				// extra keys
				Set<String> keys = getKeys(node);
				for (String key : keys) {
					result.extra(location, key, node.get(key));
				}
			}
			return null;
		}

		String value = getString("summary", obj, false, location, result);
		if ((result.isAllowEmptyStrings() && value != null) || (!result.isAllowEmptyStrings() && !StringUtils.isBlank(value))) {
			pathItem.setSummary(value);
		}

		value = getString("description", obj, false, location, result);
		if ((result.isAllowEmptyStrings() && value != null) || (!result.isAllowEmptyStrings() && !StringUtils.isBlank(value))) {
			pathItem.setDescription(value);
		}

		ArrayNode parameters = getArray("parameters", obj, false, location, result);
		if (parameters != null && parameters.size() > 0) {
			pathItem.setParameters(getParameterList(parameters, location, result));
		}

		ArrayNode servers = getArray("servers", obj, false, location, result);
		if (servers != null && servers.size() > 0) {
			pathItem.setServers(getServersList(servers, location, result));
		}

		ObjectNode node = getObject("get", obj, false, location, result);
		if (node != null) {
			Operation operation = getOperation(node, location + "(get)", result);
			if (operation != null) {
				pathItem.setGet(operation);
			}
		}
		node = getObject("put", obj, false, location, result);
		if (node != null) {
			Operation operation = getOperation(node, location + "(put)", result);
			if (operation != null) {
				pathItem.setPut(operation);
			}
		}
		node = getObject("post", obj, false, location, result);
		if (node != null) {
			Operation operation = getOperation(node, location + "(post)", result);
			if (operation != null) {
				pathItem.setPost(operation);
			}
		}
		node = getObject("head", obj, false, location, result);
		if (node != null) {
			Operation operation = getOperation(node, location + "(head)", result);
			if (operation != null) {
				pathItem.setHead(operation);
			}
		}
		node = getObject("delete", obj, false, location, result);
		if (node != null) {
			Operation operation = getOperation(node, location + "(delete)", result);
			if (operation != null) {
				pathItem.setDelete(operation);
			}
		}
		node = getObject("patch", obj, false, location, result);
		if (node != null) {
			Operation operation = getOperation(node, location + "(patch)", result);
			if (operation != null) {
				pathItem.setPatch(operation);
			}
		}
		node = getObject("options", obj, false, location, result);
		if (node != null) {
			Operation operation = getOperation(node, location + "(options)", result);
			if (operation != null) {
				pathItem.setOptions(operation);
			}
		}
		node = getObject("trace", obj, false, location, result);
		if (node != null) {
			Operation operation = getOperation(node, location + "(trace)", result);
			if (operation != null) {
				pathItem.setTrace(operation);
			}
		}

		Map<String, Object> extensions = getExtensions(obj);
		if (extensions != null && extensions.size() > 0) {
			pathItem.setExtensions(extensions);
		}

		Set<String> keys = getKeys(obj);
		Map<String, Set<String>> specKeys = KEYS.get(result.isOpenapi31() ? "openapi31" : "openapi30");
		for (String key : keys) {
			if (!specKeys.get("PATHITEM_KEYS").contains(key) && !key.startsWith("x-")) {
				result.extra(location, key, obj.get(key));
			}
			validateReservedKeywords(specKeys, key, location, result);
		}

		return pathItem;
	}


	public ExternalDocumentation getExternalDocs(ObjectNode node, String location, ParseResult result) {
		ExternalDocumentation externalDocs = null;

		if (node != null) {
			externalDocs = new ExternalDocumentation();

			String value = getString("description", node, false, location, result);
			if ((result.isAllowEmptyStrings() && value != null) || (!result.isAllowEmptyStrings() && !StringUtils.isBlank(value))) {
				externalDocs.description(value);
			}

			value = getString("url", node, true, location, result);
			if ((result.isAllowEmptyStrings() && value != null) || (!result.isAllowEmptyStrings() && !StringUtils.isBlank(value))) {
				externalDocs.url(value);
			}

			Map<String, Object> extensions = getExtensions(node);
			if (extensions != null && extensions.size() > 0) {
				externalDocs.setExtensions(extensions);
			}

			Set<String> keys = getKeys(node);
			Map<String, Set<String>> specKeys = KEYS.get(result.isOpenapi31() ? "openapi31" : "openapi30");
			for (String key : keys) {
				if (!specKeys.get("EXTERNAL_DOCS_KEYS").contains(key) && !key.startsWith("x-")) {
					result.extra(location, key, node.get(key));
				}
			}
		}

		return externalDocs;
	}


	public String getString(String key, ObjectNode node, boolean required, String location, ParseResult
			result, Set<String> uniqueValues, boolean noInvalidError) {
		String value = null;
		JsonNode v = node.get(key);
		if (node == null || v == null) {
			if (required) {
				result.missing(location, key);
				result.invalid();
			}
		} else if (!v.isValueNode()) {
			if (!noInvalidError) {
				result.invalidType(location, key, "string", node);
			}
        } else if (!v.isNull()) {
			value = v.asText();
			if (uniqueValues != null && !uniqueValues.add(value)) {
				result.unique(location, "operationId");
				result.invalid();
			}
		}
		return value;
	}

	public String getString(String key, ObjectNode node, boolean required, String location, ParseResult
			result, Set<String> uniqueValues) {
		return getString(key, node, required, location, result, uniqueValues, false);
	}

	public String getString(String key, ObjectNode node, boolean required, String location, ParseResult result) {
		return getString(key, node, required, location, result, null);
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
		ObjectNode object = null;
		if (value == null) {
			if (required) {
				result.missing(location, key);
				result.invalid();
			}
		} else if (!value.getNodeType().equals(JsonNodeType.OBJECT)) {
			result.invalidType(location, key, "object", value);
			if (required) {
				result.invalid();
			}
		} else {
			object = (ObjectNode) value;
		}
		return object;
	}

    public JsonNode getObjectOrBoolean(String key, ObjectNode node, boolean required, String location, ParseResult result) {
        JsonNode value = node.get(key);

        if (value == null) {
            if (required) {
                result.missing(location, key);
                result.invalid();
            }
            return null;
        }
        Boolean boolValue = null;
        if (value.getNodeType().equals(JsonNodeType.BOOLEAN)) {
            boolValue = value.asBoolean();
        } else if (value.getNodeType().equals(JsonNodeType.STRING)) {
            String stringValue = value.textValue();
            if ("true".equalsIgnoreCase(stringValue) || "false".equalsIgnoreCase(stringValue)) {
                boolValue = Boolean.parseBoolean(stringValue);
            } else {
                result.invalidType(location, key, "object", value);
                return null;
            }
        }
        if (boolValue != null) {
            return value;
        }
        if (!value.isObject()) {
            result.invalidType(location, key, "object", value);
            return null;
        }

        return value;
    }

	public Info getInfo(ObjectNode node, String location, ParseResult result) {
		if (node == null)
			return null;

		Info info = new Info();

		String value = getString("title", node, true, location, result);
		if ((result.isAllowEmptyStrings() && value != null) || (!result.isAllowEmptyStrings() && !StringUtils.isBlank(value))) {
			info.setTitle(value);
		}

		value = getString("description", node, false, location, result);
		if ((result.isAllowEmptyStrings() && value != null) || (!result.isAllowEmptyStrings() && !StringUtils.isBlank(value))) {
			info.setDescription(value);
		}

		if(result.isOpenapi31()) {
			value = getString("summary", node, false, location, result);
			if (StringUtils.isNotBlank(value)) {
				info.setSummary(value);
			}
		}

		value = getString("termsOfService", node, false, location, result);
		if ((result.isAllowEmptyStrings() && value != null) || (!result.isAllowEmptyStrings() && !StringUtils.isBlank(value))) {
			info.setTermsOfService(value);
		}

		ObjectNode obj = getObject("contact", node, false, "contact", result);
		Contact contact = getContact(obj, String.format("%s.%s", location, "contact"), result);
		if (obj != null) {
			info.setContact(contact);
		}
		obj = getObject("license", node, false, location, result);
		License license = getLicense(obj, String.format("%s.%s", location, "license"), result);
		if (obj != null) {
			info.setLicense(license);
		}

		value = getString("version", node, true, location, result);
		if ((result.isAllowEmptyStrings() && value != null) || (!result.isAllowEmptyStrings() && !StringUtils.isBlank(value))) {
			info.setVersion(value);
		}

		Map<String, Object> extensions = getExtensions(node);
		if (extensions != null && extensions.size() > 0) {
			info.setExtensions(extensions);
		}

		Set<String> keys = getKeys(node);
		Map<String, Set<String>> specKeys = KEYS.get(result.isOpenapi31() ? "openapi31" : "openapi30");
		for (String key : keys) {
			if (!specKeys.get("INFO_KEYS").contains(key) && !key.startsWith("x-")) {
				result.extra(location, key, node.get(key));
			}
			validateReservedKeywords(specKeys, key, location, result);
		}

		return info;
	}

	public License getLicense(ObjectNode node, String location, ParseResult result) {
		if (node == null)
			return null;

		License license = new License();

		String value = getString("name", node, true, location, result);
		if ((result.isAllowEmptyStrings() && value != null) || (!result.isAllowEmptyStrings() && !StringUtils.isBlank(value))) {
			license.setName(value);
		}

		value = getString("url", node, false, location, result);
		if ((result.isAllowEmptyStrings() && value != null) || (!result.isAllowEmptyStrings() && !StringUtils.isBlank(value))) {
			try {
				new URL(value);
			} catch (Exception e) {
				result.warning(location, value);
			}
			license.setUrl(value);
		}

		if (result.isOpenapi31()) {
            // either the url must be set or the identifier but not both
            boolean needsIdentifier = license.getUrl() == null;
			value = getString("identifier", node, needsIdentifier, location, result);

			if (StringUtils.isNotBlank(value)) {
                if (!needsIdentifier) {
                    result.extra(location, "identifier", node);
                    result.invalid();
                } else {
                    license.setIdentifier(value);
                }
			}
		}


		Map<String, Object> extensions = getExtensions(node);
		if (extensions != null && extensions.size() > 0) {
			license.setExtensions(extensions);
		}

		Set<String> keys = getKeys(node);
		Map<String, Set<String>> specKeys = KEYS.get(result.isOpenapi31() ? "openapi31" : "openapi30");
		for (String key : keys) {
			if (!specKeys.get("LICENSE_KEYS").contains(key) && !key.startsWith("x-")) {
				result.extra(location, key, node.get(key));
			}
			validateReservedKeywords(specKeys, key, location, result);
		}

		return license;
	}

	public Contact getContact(ObjectNode node, String location, ParseResult result) {
		if (node == null)
			return null;

		Contact contact = new Contact();

		String value = getString("name", node, false, location, result);
		if ((result.isAllowEmptyStrings() && value != null) || (!result.isAllowEmptyStrings() && !StringUtils.isBlank(value))) {
			contact.setName(value);
		}

		value = getString("url", node, false, location, result);
		if ((result.isAllowEmptyStrings() && value != null) || (!result.isAllowEmptyStrings() && !StringUtils.isBlank(value))) {
			try {
				new URL(value);
			} catch (Exception e) {
				result.warning(location, value);
			}
			contact.setUrl(value);
		}

		value = getString("email", node, false, location, result);
		if ((result.isAllowEmptyStrings() && value != null) || (!result.isAllowEmptyStrings() && !StringUtils.isBlank(value))) {
			contact.setEmail(value);
		}

		Map<String, Object> extensions = getExtensions(node);
		if (extensions != null && extensions.size() > 0) {
			contact.setExtensions(extensions);
		}

		Set<String> keys = getKeys(node);
		Map<String, Set<String>> specKeys = KEYS.get(result.isOpenapi31() ? "openapi31" : "openapi30");
		for (String key : keys) {
			if (!specKeys.get("CONTACT_KEYS").contains(key) && !key.startsWith("x-")) {
				result.extra(location, key, node.get(key));
			}
			validateReservedKeywords(specKeys, key, location, result);
		}

		return contact;
	}

	public Content getContent(ObjectNode node, String location, ParseResult result) {
		if (node == null) {
			return null;
		}
		Content content = new Content();

		Set<String> keys = getKeys(node);
		for (String key : keys) {
			MediaType mediaType = getMediaType((ObjectNode) node.get(key), String.format("%s.'%s'", location, key)
					, result);
			if (mediaType != null) {
				content.addMediaType(key, mediaType);
			}
		}

		return content;
	}

	public MediaType getMediaType(ObjectNode contentNode, String location, ParseResult result) {
		if (contentNode == null) {
			return null;
		}
		MediaType mediaType = new MediaType();

		ObjectNode schemaObject = getObject("schema", contentNode, false, location, result);
		if (schemaObject != null) {
			mediaType.setSchema(getSchema(schemaObject, String.format("%s.%s", location, "schema"), result));
		}

		ObjectNode encodingObject = getObject("encoding", contentNode, false, location, result);
		if (encodingObject != null) {
            String encodingLocation = String.format("%s.%s", location, "encoding");
			mediaType.setEncoding(getEncodingMap(encodingObject, encodingLocation, result));

            // Given all properties defined for this media type object...
            Set<String> mediaTypeProperties =
                mediaType.getSchema() == null?
                emptySet() :

                mediaType.getSchema().get$ref() == null?
                Optional.ofNullable( mediaType.getSchema().getProperties()).map( Map::keySet).orElse( emptySet()) :

                null;

            if( mediaTypeProperties != null) {
                // ... report an error if an encoding is specified for an undefined property
                mediaType.getEncoding().keySet().stream()
                    .filter( ep -> !mediaTypeProperties.contains( ep))
                    .forEach( ep -> result.extra( encodingLocation, ep, encodingObject));
            }
		}
		Map<String, Object> extensions = getExtensions(contentNode);
		if (extensions != null && extensions.size() > 0) {
			mediaType.setExtensions(extensions);
		}

		ObjectNode examplesObject = getObject("examples", contentNode, false, location, result);
		if (examplesObject != null) {
			mediaType.setExamples(getExamples(examplesObject, String.format("%s.%s", location, "examples"), result
					, false));
		}

		Object example = getAnyType("example", contentNode, location, result);
		if (example != null) {
			if (examplesObject != null) {
				result.warning(location, "examples already defined -- ignoring \"example\" field");
			} else {
				mediaType.setExample(example instanceof NullNode ? null : example);
			}
		}


		Set<String> keys = getKeys(contentNode);
		Map<String, Set<String>> specKeys = KEYS.get(result.isOpenapi31() ? "openapi31" : "openapi30");
		for (String key : keys) {
			if (!specKeys.get("MEDIATYPE_KEYS").contains(key) && !key.startsWith("x-")) {
				result.extra(location, key, contentNode.get(key));
			}
			validateReservedKeywords(specKeys, key, location, result);
		}

		return mediaType;
	}

	public Map<String, Encoding> getEncodingMap(ObjectNode node, String location, ParseResult result) {
		if (node == null) {
			return null;
		}
		Map<String, Encoding> encodings = new LinkedHashMap<>();
		Set<String> keys = getKeys(node);
		for (String key : keys) {
			Encoding encoding = getEncoding((ObjectNode) node.get(key), location, result);
			if (encoding != null) {
				encodings.put(key, encoding);
			}
		}
		return encodings;
	}

	public Encoding getEncoding(ObjectNode node, String location, ParseResult result) {
		if (node == null) {
			return null;
		}

		Encoding encoding = new Encoding();

		String value = getString("contentType", node, false, location, result);
		if ((result.isAllowEmptyStrings() && value != null) || (!result.isAllowEmptyStrings() && !StringUtils.isBlank(value))) {
			encoding.setContentType(value);
		}

		value = getString("style", node, false, location, result);

		if (StringUtils.isBlank(value)) {
			encoding.setStyle(Encoding.StyleEnum.FORM);
		} else {
			if (value.equals(Encoding.StyleEnum.FORM.toString())) {
				encoding.setStyle(Encoding.StyleEnum.FORM);
			} else if (value.equals(Encoding.StyleEnum.DEEP_OBJECT.toString())) {
				encoding.setStyle(Encoding.StyleEnum.DEEP_OBJECT);
			} else if (value.equals(Encoding.StyleEnum.PIPE_DELIMITED.toString())) {
				encoding.setStyle(Encoding.StyleEnum.PIPE_DELIMITED);
			} else if (value.equals(Encoding.StyleEnum.SPACE_DELIMITED.toString())) {
				encoding.setStyle(Encoding.StyleEnum.SPACE_DELIMITED);
			} else {
				result.invalidType(location, "style", "string", node);
			}
		}

		Boolean explode = getBoolean("explode", node, false, location, result);
		if (explode != null) {
			encoding.setExplode(explode);
		}

		Boolean allowReserved = getBoolean("allowReserved", node, false, location, result);
		if (allowReserved != null) {
			encoding.setAllowReserved(allowReserved);
		}
		ObjectNode headersObject = getObject("headers", node, false, location, result);
		if (headersObject != null) {
			encoding.setHeaders(getHeaders(headersObject, location, result, false));
		}

		Map<String, Object> extensions = getExtensions(node);
		if (extensions != null && extensions.size() > 0) {
			encoding.setExtensions(extensions);
		}

		Set<String> keys = getKeys(node);
		Map<String, Set<String>> specKeys = KEYS.get(result.isOpenapi31() ? "openapi31" : "openapi30");
		for (String key : keys) {
			if (!specKeys.get("ENCODING_KEYS").contains(key) && !key.startsWith("x-")) {
				result.extra(location, key, node.get(key));
			}
			validateReservedKeywords(specKeys, key, location, result);
		}

		return encoding;
	}

	public Map<String, Link> getLinks(ObjectNode obj, String location, ParseResult result,
									  boolean underComponents) {
		if (obj == null) {
			return null;
		}
		Map<String, Link> links = new LinkedHashMap<>();


		Set<String> linkKeys = getKeys(obj);
		for (String linkName : linkKeys) {
			if (underComponents) {
				if (!Pattern.matches("^[a-zA-Z0-9\\.\\-_]+$",
						linkName)) {
					result.warning(location, "Link name " + linkName + " doesn't adhere to regular expression " +
							"^[a-zA-Z0-9\\.\\-_]+$");
				}
			}

			JsonNode linkValue = obj.get(linkName);
			if (!linkValue.getNodeType().equals(JsonNodeType.OBJECT)) {
				result.invalidType(location, linkName, "object", linkValue);
			} else {
				ObjectNode link = (ObjectNode) linkValue;
				Link linkObj = getLink(link, String.format("%s.%s", location, linkName), result);
				if (linkObj != null) {
					links.put(linkName, linkObj);
				}
			}

		}
		return links;
	}

	public Link getLink(ObjectNode linkNode, String location, ParseResult result) {
		if (linkNode == null) {
			return null;
		}

		Link link = new Link();

		JsonNode ref = linkNode.get("$ref");
		if (ref != null) {
			if (ref.getNodeType().equals(JsonNodeType.STRING)) {
				String mungedRef = mungedRef(ref.textValue());
				if (mungedRef != null) {
					link.set$ref(mungedRef);
				} else {
					link.set$ref(ref.textValue());
				}
                if(ref.textValue().startsWith("#/components") && !(ref.textValue().startsWith("#/components/links"))) {
                    result.warning(location, "$ref target "+ref.textValue() +" is not of expected type Link");
                }
				if (result.isOpenapi31()) {
					String desc = getString("description", linkNode, false, location, result);
					if (StringUtils.isNotBlank(desc)) {
						link.setDescription(desc);
					}
				}
				return link;
			} else {
				result.invalidType(location, "$ref", "string", linkNode);
				return null;
			}
		}

		String value = getString("operationRef", linkNode, false, location, result);
		if ((result.isAllowEmptyStrings() && value != null) || (!result.isAllowEmptyStrings() && !StringUtils.isBlank(value))) {
			link.setOperationRef(value);
		}

		value = getString("operationId", linkNode, false, location, result);
		if ((result.isAllowEmptyStrings() && value != null) || (!result.isAllowEmptyStrings() && !StringUtils.isBlank(value))) {
			link.setOperationId(value);
		}

		ObjectNode parametersObject = getObject("parameters", linkNode, false, location, result);
		if (parametersObject != null) {
			link.setParameters(getLinkParameters(parametersObject, location, result));
		}

		String requestBody = getString("requestBody", linkNode, false, location, result);
		if (result.isAllowEmptyStrings() && requestBody != null) {
			link.setRequestBody(requestBody);
		}

		ObjectNode headerObject = getObject("headers", linkNode, false, location, result);
		if (headerObject != null) {
			link.setHeaders(getHeaders(headerObject, location, result, false));
		}

		ObjectNode serverObject = getObject("server", linkNode, false, location, result);
		if (serverObject != null) {
			link.setServer(getServer(serverObject, location, result));
		}

		value = getString("description", linkNode, false, location, result);
		if ((result.isAllowEmptyStrings() && value != null) || (!result.isAllowEmptyStrings() && !StringUtils.isBlank(value))) {
			link.setDescription(value);
		}

		Map<String, Object> extensions = getExtensions(linkNode);
		if (extensions != null && extensions.size() > 0) {
			link.setExtensions(extensions);
		}

		Set<String> keys = getKeys(linkNode);
		Map<String, Set<String>> specKeys = KEYS.get(result.isOpenapi31() ? "openapi31" : "openapi30");
		for (String key : keys) {
			if (!specKeys.get("LINK_KEYS").contains(key) && !key.startsWith("x-")) {
				result.extra(location, key, linkNode.get(key));
			}
			validateReservedKeywords(specKeys, key, location, result);
		}

		return link;
	}

	private Map<String, String> getLinkParameters(ObjectNode parametersObject, String location, ParseResult result) {

		Map<String, String> linkParameters = new LinkedHashMap<>();

		Set<String> keys = getKeys(parametersObject);
		for (String name : keys) {
			JsonNode value = parametersObject.get(name);
			linkParameters.put(name, value.asText());
		}

		return linkParameters;
	}



	public Map<String, Callback> getCallbacks(ObjectNode node, String location, ParseResult result,
											  boolean underComponents) {
		if (node == null) {
			return null;
		}
		Map<String, Callback> callbacks = new LinkedHashMap<>();
		Set<String> keys = getKeys(node);
		for (String key : keys) {
			if (underComponents) {
				if (!Pattern.matches("^[a-zA-Z0-9\\.\\-_]+$",
						key)) {
					result.warning(location, "Callback key " + key + " doesn't adhere to regular expression " +
							"^[a-zA-Z0-9\\.\\-_]+$");
				}
			}
			Callback callback = getCallback((ObjectNode) node.get(key), location, result);
			if (callback != null) {
				callbacks.put(key, callback);
			}
		}
		return callbacks;
	}


	public Callback getCallback(ObjectNode node, String location, ParseResult result) {
		if (node == null) {
			return null;
		}

		Callback callback = new Callback();

		Set<String> keys = getKeys(node);
		for (String name : keys) {
			JsonNode value = node.get(name);
			if (node != null) {
				JsonNode ref = node.get("$ref");
				if (ref != null) {
					if (ref.getNodeType().equals(JsonNodeType.STRING)) {
						String mungedRef = mungedRef(ref.textValue());
						if (mungedRef != null) {
							callback.set$ref(mungedRef);
						} else {
							callback.set$ref(ref.textValue());
						}
                        if(ref.textValue().startsWith("#/components") && !(ref.textValue().startsWith("#/components/callbacks"))) {
                            result.warning(location, "$ref target "+ref.textValue() +" is not of expected type Callback");
                        }
						return callback;
					} else {
						result.invalidType(location, "$ref", "string", node);
						return null;
					}
				}
				if(value.isObject()) {
					callback.addPathItem(name, getPathItem((ObjectNode) value, location, result));
				}else{
					result.invalidType(location, name, "object", value);
				}

				Map<String, Object> extensions = getExtensions(node);
				if (extensions != null && extensions.size() > 0) {
					callback.setExtensions(extensions);
				}
			}
		}

		return callback;
	}

	public XML getXml(ObjectNode node, String location, ParseResult result) {
		if (node == null) {
			return null;
		}
		XML xml = new XML();

		String value = getString("name", node, false, String.format("%s.%s", location, "name"), result);
		if ((result.isAllowEmptyStrings() && value != null) || (!result.isAllowEmptyStrings() && !StringUtils.isBlank(value))) {
			xml.setName(value);
		}

		value = getString("namespace", node, false, String.format("%s.%s", location, "namespace"), result);
		if ((result.isAllowEmptyStrings() && value != null) || (!result.isAllowEmptyStrings() && !StringUtils.isBlank(value))) {
			xml.setNamespace(value);
		}

		value = getString("prefix", node, false, String.format("%s.%s", location, "prefix"), result);
		if ((result.isAllowEmptyStrings() && value != null) || (!result.isAllowEmptyStrings() && !StringUtils.isBlank(value))) {
			xml.setPrefix(value);
		}

		Boolean attribute = getBoolean("attribute", node, false, location, result);
		if (attribute != null) {
			xml.setAttribute(attribute);
		}

		Boolean wrapped = getBoolean("wrapped", node, false, location, result);
		if (wrapped != null) {
			xml.setWrapped(wrapped);
		}

		Map<String, Object> extensions = getExtensions(node);
		if (extensions != null && extensions.size() > 0) {
			xml.setExtensions(extensions);
		}


		Set<String> keys = getKeys(node);
		Map<String, Set<String>> specKeys = KEYS.get(result.isOpenapi31() ? "openapi31" : "openapi30");
		for (String key : keys) {
			if (!specKeys.get("XML_KEYS").contains(key) && !key.startsWith("x-")) {
				result.extra(location, key, node.get(key));
			}
			validateReservedKeywords(specKeys, key, location, result);
		}


		return xml;

	}

	public ArrayNode getArray(String key, ObjectNode node, boolean required, String location, ParseResult result, boolean noInvalidError) {
		JsonNode value = node.get(key);
		ArrayNode arrayNode = null;
		if (value == null) {
			if (required) {
				result.missing(location, key);
				result.invalid();
			}
		} else if (!value.getNodeType().equals(JsonNodeType.ARRAY)) {
			if (!noInvalidError) {
				result.invalidType(location, key, "array", value);
			}
		} else {
			arrayNode = (ArrayNode) value;
		}
		return arrayNode;
	}

	public ArrayNode getArray(String key, ObjectNode node, boolean required, String location, ParseResult result) {
		return getArray(key, node, required, location, result, false);
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

	public BigDecimal getBigDecimal(String key, ObjectNode node, boolean required, String location, ParseResult
			result) {
		BigDecimal value = null;
		JsonNode v = node.get(key);
		if (node == null || v == null) {
			if (required) {
				result.missing(location, key);
				result.invalid();
			}
		} else if (v.getNodeType().equals(JsonNodeType.NUMBER)) {
			value = new BigDecimal(v.asText());
		} else if (!v.isValueNode()) {
			result.invalidType(location, key, "double", node);
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
		} else if (v.getNodeType().equals(JsonNodeType.NUMBER)) {
			if (v.isInt()) {
				value = v.intValue();
			}
		} else if (!v.isValueNode()) {
			result.invalidType(location, key, "integer", node);
		}
		return value;
	}

	public Map<String, Parameter> getParameters(ObjectNode obj, String location, ParseResult result,
												boolean underComponents) {
		if (obj == null) {
			return null;
		}
		Map<String, Parameter> parameters = new LinkedHashMap<>();
		Set<String> filter = new HashSet<>();
		Parameter parameter = null;

		Set<String> parameterKeys = getKeys(obj);
		for (String parameterName : parameterKeys) {
			if (underComponents) {
				if (!Pattern.matches("^[a-zA-Z0-9\\.\\-_]+$",
						parameterName)) {
					result.warning(location, "Parameter name " + parameterName + " doesn't adhere to regular " +
							"expression ^[a-zA-Z0-9\\.\\-_]+$");
				}
			}

			JsonNode parameterValue = obj.get(parameterName);
			if (parameterValue.getNodeType().equals(JsonNodeType.OBJECT)) {
				ObjectNode parameterObj = (ObjectNode) parameterValue;
				if (parameterObj != null) {
					parameter = getParameter(parameterObj, String.format("%s.%s", location, parameterName),
							result);
					if (parameter != null) {
						if (PATH_PARAMETER.equalsIgnoreCase(parameter.getIn()) && Boolean.FALSE.equals(parameter.getRequired())) {
							result.warning(location, "For path parameter " + parameterName + " the required value " +
									"should be true");
						}
						parameters.put(parameterName, parameter);
					}
				}
			}

		}
		return parameters;
	}


	public List<Parameter> getParameterList(ArrayNode obj, String location, ParseResult result) {
		List<Parameter> parameters = new ArrayList<>();
		if (obj == null) {
			return parameters;
		}
		for (JsonNode item : obj) {
			if (item.getNodeType().equals(JsonNodeType.OBJECT)) {
				Parameter parameter = getParameter((ObjectNode) item, location, result);
				if (parameter != null) {
					parameters.add(parameter);
				}
			}
		}
		Set<String> filter = new HashSet<>();


		parameters.stream().map(this::getParameterDefinition).forEach(param -> {
			String ref = param.get$ref();
			if (!filter.add(param.getName() + "#" + param.getIn())) {
				if (ref != null) {
					if (ref.startsWith(REFERENCE_SEPARATOR)) {// validate if it's inline param also
						result.warning(location, " There are duplicate parameter values");
					}
				} else {
					result.warning(location, " There are duplicate parameter values");
				}
			}
		});
		return parameters;
	}

	private Parameter getParameterDefinition(Parameter parameter) {
		if (parameter.get$ref() == null) {
			return parameter;
		}
		Object parameterSchemaName = extractSimpleName(parameter.get$ref()).getLeft();
		return Optional.ofNullable(components)
				.map(Components::getParameters)
				.map(parameters -> parameters.get(parameterSchemaName))
				.orElse(parameter);

	}

	public Parameter getParameter(ObjectNode obj, String location, ParseResult result) {
		if (obj == null) {
			return null;
		}

		Parameter parameter = null;

		JsonNode ref = obj.get("$ref");
		if (ref != null) {
			if (ref.getNodeType().equals(JsonNodeType.STRING)) {
				parameter = new Parameter();
				String mungedRef = mungedRef(ref.textValue());
				if (mungedRef != null) {
                    parameter.set$ref(mungedRef);
                }else {
                    parameter.set$ref(ref.textValue());
                }
                if(ref.textValue().startsWith("#/components") && !(ref.textValue().startsWith("#/components/parameters") || ref.textValue().startsWith("#/components/headers"))) {
                    result.warning(location, "$ref target "+ref.textValue() +" is not of expected type Parameter/Header");
                }
				if (result.isOpenapi31()) {
					String desc = getString("description", obj, false, location, result);
					if (StringUtils.isNotBlank(desc)) {
						parameter.setDescription(desc);
					}
				}
				return parameter;
			} else {
				result.invalidType(location, "$ref", "string", obj);
				return null;
			}
		}

		String l = null;
		JsonNode ln = obj.get("name");
		if (ln != null) {
			l = ln.asText();
		} else {
			l = "['unknown']";
		}
		location += ".[" + l + "]";

		String value = getString("in", obj, true, location, result);

		if (!result.isAllowEmptyStrings() && StringUtils.isBlank(value) || result.isAllowEmptyStrings() && value == null) {
			return null;
		}

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
			result.invalidType(location, "in", "[query|header|path|cookie]", obj);
			return null;
		}

		parameter.setIn(value);

		value = getString("name", obj, true, location, result);
		if ((result.isAllowEmptyStrings() && value != null) || (!result.isAllowEmptyStrings() && !StringUtils.isBlank(value))) {
			parameter.setName(value);
		}

		value = getString("description", obj, false, location, result);
		if ((result.isAllowEmptyStrings() && value != null) || (!result.isAllowEmptyStrings() && !StringUtils.isBlank(value))) {
			parameter.setDescription(value);
		}

		Boolean required = getBoolean("required", obj, false, location, result);
		if (required != null) {
			parameter.setRequired(required);
		} else {
			parameter.setRequired(false);
		}


		Boolean deprecated = getBoolean("deprecated", obj, false, location, result);
		if (deprecated != null) {
			parameter.setDeprecated(deprecated);
		}

		if (parameter instanceof QueryParameter) {
			Boolean allowEmptyValue = getBoolean("allowEmptyValue", obj, false, location, result);
			if (allowEmptyValue != null) {
				parameter.setAllowEmptyValue(allowEmptyValue);
			}
		}




		ObjectNode parameterObject = getObject("schema", obj, false, location, result);
		if (parameterObject != null) {
			parameter.setSchema(getSchema(parameterObject, String.format("%s.%s", location, "schemas"), result));
		}

		ObjectNode examplesObject = getObject("examples", obj, false, location, result);
		if (examplesObject != null) {
			parameter.setExamples(getExamples(examplesObject, String.format("%s.%s", location, "examples"), result
					, false));
		}

		Object example = getAnyType("example", obj, location, result);
		if (example != null) {
			if (examplesObject != null) {
				result.warning(location, "examples already defined -- ignoring \"example\" field");
			} else {
				parameter.setExample(example instanceof NullNode ? null : example);
			}
		}

		Boolean allowReserved = getBoolean("allowReserved", obj, false, location, result);
		if (allowReserved != null) {
			parameter.setAllowReserved(allowReserved);
		}

		ObjectNode contentNode = getObject("content", obj, false, location, result);
		if (contentNode != null) {
            Content content = getContent(contentNode, String.format("%s.%s", location, "content"), result);
            if(content.size() == 0) {
                result.unsupported(location,"content with no media type",contentNode);
                result.invalid();
            }
            else if(content.size() > 1) {
                result.unsupported(location,"content with multiple media types",contentNode);
                result.invalid();
            }
            else if(parameter.getSchema() != null) {
                result.unsupported(location,"content when schema defined",contentNode);
                result.invalid();
            }
            else {
                parameter.setContent(content);
            }
		}
        else if(parameter.getSchema() == null) {
            result.missing(location,"content");
        }

        value = getString("style", obj, false, location, result);
        if (parameter.getContent() == null) {
            setStyle(value, parameter, location, obj, result);

            Boolean explode = getBoolean("explode", obj, false, location, result);
            if (explode != null) {
                parameter.setExplode(explode);
            } else if (StyleEnum.FORM.equals(parameter.getStyle())) {
                parameter.setExplode(Boolean.TRUE);
            } else {
                parameter.setExplode(Boolean.FALSE);
            }
        }

		Map<String, Object> extensions = getExtensions(obj);
		if (extensions != null && extensions.size() > 0) {
			parameter.setExtensions(extensions);
		}

		Set<String> keys = getKeys(obj);
		Map<String, Set<String>> specKeys = KEYS.get(result.isOpenapi31() ? "openapi31" : "openapi30");
		for (String key : keys) {
			if (!specKeys.get("PARAMETER_KEYS").contains(key) && !key.startsWith("x-")) {
				result.extra(location, key, obj.get(key));
			}
			validateReservedKeywords(specKeys, key, location, result);
		}

		return parameter;
	}


	public Map<String, Header> getHeaders(ObjectNode obj, String location, ParseResult result,
										  boolean underComponents) {
		if (obj == null) {
			return null;
		}
		Map<String, Header> headers = new LinkedHashMap<>();

		Set<String> headerKeys = getKeys(obj);
		for (String headerName : headerKeys) {
			if (underComponents) {
				if (!Pattern.matches("^[a-zA-Z0-9\\.\\-_]+$",
						headerName)) {
					result.warning(location, "Header name " + headerName + " doesn't adhere to regular expression " +
							"^[a-zA-Z0-9\\.\\-_]+$");
				}
			}
			JsonNode headerValue = obj.get(headerName);
			if (!headerValue.getNodeType().equals(JsonNodeType.OBJECT)) {
				result.invalidType(location, headerName, "object", headerValue);
			} else {
				ObjectNode header = (ObjectNode) headerValue;
				Header headerObj = getHeader(header, String.format("%s.%s", location, headerName), result);
				if (headerObj != null) {
					headers.put(headerName, headerObj);
				}
			}

		}

		return headers;
	}

	public Header getHeader(ObjectNode headerNode, String location, ParseResult result) {
		if (headerNode == null) {
			return null;
		}

		Header header = new Header();

		JsonNode ref = headerNode.get("$ref");
		if (ref != null) {
			if (ref.getNodeType().equals(JsonNodeType.STRING)) {
				String mungedRef = mungedRef(ref.textValue());
				if (mungedRef != null) {
					header.set$ref(mungedRef);
				} else {
					header.set$ref(ref.textValue());
				}
                if(ref.textValue().startsWith("#/components") && !(ref.textValue().startsWith("#/components/parameters") || ref.textValue().startsWith("#/components/headers"))) {
                    result.warning(location, "$ref target "+ref.textValue() +" is not of expected type Header/Parameter");
                }

				if (result.isOpenapi31()) {
					String desc = getString("description", headerNode, false, location, result);
					if (StringUtils.isNotBlank(desc)) {
						header.setDescription(desc);
					}
				}
				return header;
			} else {
				result.invalidType(location, "$ref", "string", headerNode);
				return null;
			}
		}


		String value = getString("description", headerNode, false, location, result);
		if ((result.isAllowEmptyStrings() && value != null) || (!result.isAllowEmptyStrings() && !StringUtils.isBlank(value))) {
			header.setDescription(value);
		}

		Boolean required = getBoolean("required", headerNode, false, location, result);
		if (required != null) {
			header.setRequired(required);
		}

		Boolean deprecated = getBoolean("deprecated", headerNode, false, location, result);
		if (deprecated != null) {
			header.setDeprecated(deprecated);
		}

		Boolean explode = getBoolean("explode", headerNode, false, location, result);
		if (explode != null) {
			header.setExplode(explode);
		} else {
			header.setExplode(Boolean.FALSE);
		}

		header.setStyle(Header.StyleEnum.SIMPLE);

		ObjectNode headerObject = getObject("schema", headerNode, false, location, result);
		if (headerObject != null) {
			header.setSchema(getSchema(headerObject, location, result));
		}

		ObjectNode examplesObject = getObject("examples", headerNode, false, location, result);
		if (examplesObject != null) {
			header.setExamples(getExamples(examplesObject, location, result, false));
		}

		Object example = getAnyType("example", headerNode, location, result);
		if (example != null) {
			if (examplesObject != null) {
				result.warning(location, "examples already defined -- ignoring \"example\" field");
			} else {
				header.setExample(example instanceof NullNode ? null : example);
			}
		}

		ObjectNode contentNode = getObject("content", headerNode, false, location, result);
		if (contentNode != null) {
			header.setContent(getContent(contentNode, String.format("%s.%s", location, "content"), result));
		}

		Map<String, Object> extensions = getExtensions(headerNode);
		if (extensions != null && extensions.size() > 0) {
			header.setExtensions(extensions);
		}

		Set<String> oAuthFlowKeys = getKeys(headerNode);
		Map<String, Set<String>> specKeys = KEYS.get(result.isOpenapi31() ? "openapi31" : "openapi30");
		for (String key : oAuthFlowKeys) {
			if (!specKeys.get("HEADER_KEYS").contains(key) && !key.startsWith("x-")) {
				result.extra(location, key, headerNode.get(key));
			}
		}

		return header;
	}

	public Object getAnyType(String nodeKey, ObjectNode node, String location, ParseResult result) {
		JsonNode example = node.get(nodeKey);
		if (example != null) {
			if (example.getNodeType().equals(JsonNodeType.STRING)) {
				return getString(nodeKey, node, false, location, result);
			}
			if (example.getNodeType().equals(JsonNodeType.NUMBER)) {
				Integer integerExample = getInteger(nodeKey, node, false, location, result);
				if (integerExample != null) {
					return integerExample;
				} else {
					BigDecimal bigDecimalExample = getBigDecimal(nodeKey, node, false, location, result);
					if (bigDecimalExample != null) {
						return bigDecimalExample;
					}
				}
			} else if (example.getNodeType().equals(JsonNodeType.OBJECT)) {
				ObjectNode objectValue = getObject(nodeKey, node, false, location, result);
				if (objectValue != null) {
					return objectValue;
				}
			} else if (example.getNodeType().equals(JsonNodeType.ARRAY)) {
				ArrayNode arrayValue = getArray(nodeKey, node, false, location, result);
				if (arrayValue != null) {
					return arrayValue;
				}
			} else if (example.getNodeType().equals(JsonNodeType.BOOLEAN)) {
				Boolean bool = getBoolean(nodeKey, node, false, location, result);
				if (bool != null) {
					return bool;
				}
			} else if (example.getNodeType().equals(JsonNodeType.NULL)) {
				return example;
			}
		}
		return null;
	}

	public Map<String, SecurityScheme> getSecuritySchemes(ObjectNode obj, String location, ParseResult result,
														  boolean underComponents) {
		if (obj == null) {
			return null;
		}
		Map<String, SecurityScheme> securitySchemes = new LinkedHashMap<>();

		Set<String> securitySchemeKeys = getKeys(obj);
		for (String securitySchemeName : securitySchemeKeys) {
			if (underComponents) {
				if (!Pattern.matches("^[a-zA-Z0-9\\.\\-_]+$",
						securitySchemeName)) {
					result.warning(location, "SecurityScheme name " + securitySchemeName + " doesn't adhere to " +
							"regular expression ^[a-zA-Z0-9\\.\\-_]+$");
				}
			}
			JsonNode securitySchemeValue = obj.get(securitySchemeName);
			if (!securitySchemeValue.getNodeType().equals(JsonNodeType.OBJECT)) {
				result.invalidType(location, securitySchemeName, "object", securitySchemeValue);
			} else {
				ObjectNode securityScheme = (ObjectNode) securitySchemeValue;
				SecurityScheme securitySchemeObj = getSecurityScheme(securityScheme, String.format("%s.%s",
						location, securitySchemeName), result);
				if (securityScheme != null) {
					securitySchemes.put(securitySchemeName, securitySchemeObj);
				}
			}
		}
		return securitySchemes;
	}

	public SecurityScheme getSecurityScheme(ObjectNode node, String location, ParseResult result) {
		if (node == null) {
			return null;
		}
		SecurityScheme securityScheme = new SecurityScheme();

		JsonNode ref = node.get("$ref");
		if (ref != null) {
			if (ref.getNodeType().equals(JsonNodeType.STRING)) {
				String mungedRef = mungedRef(ref.textValue());
				if (mungedRef != null) {
					securityScheme.set$ref(mungedRef);
				} else {
					securityScheme.set$ref(ref.textValue());
				}
                if(ref.textValue().startsWith("#/components") && !(ref.textValue().startsWith("#/components/securitySchemes"))) {
                    result.warning(location, "$ref target "+ref.textValue() +" is not of expected type securitySchemes");
                }
				if (result.isOpenapi31()) {
					String desc = getString("description", node, false, location, result);
					if (StringUtils.isNotBlank(desc)) {
						securityScheme.setDescription(desc);
					}
				}
				return securityScheme;
			} else {
				result.invalidType(location, "$ref", "string", node);
				return null;
			}
		}

		boolean descriptionRequired, bearerFormatRequired, nameRequired, inRequired, schemeRequired, flowsRequired,
				openIdConnectRequired;
		descriptionRequired = bearerFormatRequired = nameRequired = inRequired = schemeRequired = flowsRequired =
				openIdConnectRequired = false;

		String value = getString("type", node, true, location, result);
		if ((result.isAllowEmptyStrings() && value != null) || (!result.isAllowEmptyStrings() && !StringUtils.isBlank(value))) {
			if (SecurityScheme.Type.APIKEY.toString().equals(value)) {
				securityScheme.setType(SecurityScheme.Type.APIKEY);
				nameRequired = inRequired = true;
			} else if (SecurityScheme.Type.HTTP.toString().equals(value)) {
				securityScheme.setType(SecurityScheme.Type.HTTP);
				schemeRequired = true;
			} else if (SecurityScheme.Type.OAUTH2.toString().equals(value)) {
				securityScheme.setType(SecurityScheme.Type.OAUTH2);
				flowsRequired = true;
			} else if (SecurityScheme.Type.OPENIDCONNECT.toString().equals(value)) {
				securityScheme.setType(SecurityScheme.Type.OPENIDCONNECT);
				openIdConnectRequired = true;
			}else if (result.isOpenapi31() && SecurityScheme.Type.MUTUALTLS.toString().equals(value)) {
				securityScheme.setType(SecurityScheme.Type.MUTUALTLS);
			} else {
				result.invalidType(location + ".type", "type", "http|apiKey|oauth2|openIdConnect|mutualTLS ", node);
			}
		}
		value = getString("description", node, descriptionRequired, location, result);
		if ((result.isAllowEmptyStrings() && value != null) || (!result.isAllowEmptyStrings() && !StringUtils.isBlank(value))) {
			securityScheme.setDescription(value);
		}

		value = getString("name", node, nameRequired, location, result);
		if ((result.isAllowEmptyStrings() && value != null) || (!result.isAllowEmptyStrings() && !StringUtils.isBlank(value))) {
			securityScheme.setName(value);
		}

		final String securitySchemeIn = getString("in", node, inRequired, location, result);
		final Optional<SecurityScheme.In> matchingIn = Arrays.stream(SecurityScheme.In.values())
				.filter(in -> in.toString().equals(securitySchemeIn))
				.findFirst();

		securityScheme.setIn(matchingIn.orElse(null));

		value = getString("scheme", node, schemeRequired, location, result);
		if ((result.isAllowEmptyStrings() && value != null) || (!result.isAllowEmptyStrings() && !StringUtils.isBlank(value))) {
			securityScheme.setScheme(value);
		}

		value = getString("bearerFormat", node, bearerFormatRequired, location, result);
		if ((result.isAllowEmptyStrings() && value != null) || (!result.isAllowEmptyStrings() && !StringUtils.isBlank(value))) {
			securityScheme.setBearerFormat(value);
		}

		ObjectNode flowsObject = getObject("flows", node, flowsRequired, location, result);
		if (flowsObject != null) {
			securityScheme.setFlows(getOAuthFlows(flowsObject, location, result));
		}

		value = getString("openIdConnectUrl", node, openIdConnectRequired, location, result);
		if ((result.isAllowEmptyStrings() && value != null) || (!result.isAllowEmptyStrings() && !StringUtils.isBlank(value))) {
			securityScheme.setOpenIdConnectUrl(value);
		}

		Map<String, Object> extensions = getExtensions(node);
		if (extensions != null && extensions.size() > 0) {
			securityScheme.setExtensions(extensions);
		}

		Set<String> securitySchemeKeys = getKeys(node);
		Map<String, Set<String>> specKeys = KEYS.get(result.isOpenapi31() ? "openapi31" : "openapi30");
		for (String key : securitySchemeKeys) {
			if (!specKeys.get("SECURITY_SCHEME_KEYS").contains(key) && !key.startsWith("x-")) {
				result.extra(location, key, node.get(key));
			}
		}

		return securityScheme;
	}

	public OAuthFlows getOAuthFlows(ObjectNode node, String location, ParseResult result) {
		if (node == null) {
			return null;
		}

		OAuthFlows oAuthFlows = new OAuthFlows();

		ObjectNode objectNode = getObject("implicit", node, false, location, result);
		if (objectNode != null) {
			oAuthFlows.setImplicit(getOAuthFlow("implicit", objectNode, location, result));
		}

		objectNode = getObject("password", node, false, location, result);
		if (objectNode != null) {
			oAuthFlows.setPassword(getOAuthFlow("password", objectNode, location, result));
		}

		objectNode = getObject("clientCredentials", node, false, location, result);
		if (objectNode != null) {
			oAuthFlows.setClientCredentials(getOAuthFlow("clientCredentials", objectNode, location, result));
		}

		objectNode = getObject("authorizationCode", node, false, location, result);
		if (objectNode != null) {
			oAuthFlows.setAuthorizationCode(getOAuthFlow("authorizationCode", objectNode, location, result));
		}

		Map<String, Object> extensions = getExtensions(node);
		if (extensions != null && extensions.size() > 0) {
			oAuthFlows.setExtensions(extensions);
		}

		Set<String> oAuthFlowKeys = getKeys(node);
		Map<String, Set<String>> specKeys = KEYS.get(result.isOpenapi31() ? "openapi31" : "openapi30");
		for (String key : oAuthFlowKeys) {
			if (!specKeys.get("OAUTHFLOWS_KEYS").contains(key) && !key.startsWith("x-")) {
				result.extra(location, key, node.get(key));
			}
		}


		return oAuthFlows;
	}

	public OAuthFlow getOAuthFlow(String oAuthFlowType, ObjectNode node, String location, ParseResult result) {
		if (node == null) {
			return null;
		}

		OAuthFlow oAuthFlow = new OAuthFlow();

		boolean authorizationUrlRequired, tokenUrlRequired, refreshUrlRequired, scopesRequired;
		authorizationUrlRequired = tokenUrlRequired = refreshUrlRequired = false;
		scopesRequired = true;
		switch (oAuthFlowType) {
			case "implicit":
				authorizationUrlRequired = true;
				break;
			case "password":
				tokenUrlRequired = true;
				break;
			case "clientCredentials":
				tokenUrlRequired = true;
				break;
			case "authorizationCode":
				authorizationUrlRequired = tokenUrlRequired = true;
				break;
		}

		String value = getString("authorizationUrl", node, authorizationUrlRequired, location, result);
		if ((result.isAllowEmptyStrings() && value != null) || (!result.isAllowEmptyStrings() && !StringUtils.isBlank(value))) {
			oAuthFlow.setAuthorizationUrl(value);
		}

		value = getString("tokenUrl", node, tokenUrlRequired, location, result);
		if ((result.isAllowEmptyStrings() && value != null) || (!result.isAllowEmptyStrings() && !StringUtils.isBlank(value))) {
			oAuthFlow.setTokenUrl(value);
		}

		value = getString("refreshUrl", node, refreshUrlRequired, location, result);
		if ((result.isAllowEmptyStrings() && value != null) || (!result.isAllowEmptyStrings() && !StringUtils.isBlank(value))) {
			oAuthFlow.setRefreshUrl(value);
		}

		ObjectNode scopesObject = getObject("scopes", node, scopesRequired, location, result);

		Scopes scope = new Scopes();
		Set<String> keys = getKeys(scopesObject);
		for (String name : keys) {
			JsonNode scopeValue = scopesObject.get(name);
			if (scopesObject != null) {
				scope.addString(name, scopeValue.asText());
			}
		}
		oAuthFlow.setScopes(scope);

		Map<String, Object> extensions = getExtensions(node);
		if (extensions != null && extensions.size() > 0) {
			oAuthFlow.setExtensions(extensions);
		}

		Set<String> oAuthFlowKeys = getKeys(node);
		Map<String, Set<String>> specKeys = KEYS.get(result.isOpenapi31() ? "openapi31" : "openapi30");
		for (String key : oAuthFlowKeys) {
			if (!specKeys.get("OAUTHFLOW_KEYS").contains(key) && !key.startsWith("x-")) {
				result.extra(location, key, node.get(key));
			}
		}

		return oAuthFlow;
	}

	public Map<String, Schema> getSchemas(ObjectNode obj, String location, ParseResult result,
										  boolean underComponents) {
		if (obj == null) {
			return null;
		}
		Map<String, Schema> schemas = new LinkedHashMap<>();

		Set<String> schemaKeys = getKeys(obj);
		for (String schemaName : schemaKeys) {
			if (underComponents) {
				if (!Pattern.matches("^[a-zA-Z0-9\\.\\-_]+$",
						schemaName)) {
					result.warning(location, "Schema name " + schemaName + " doesn't adhere to regular expression " +
							"^[a-zA-Z0-9\\.\\-_]+$");
				}
			}
			JsonNode schemaValue = obj.get(schemaName);
			if (!schemaValue.getNodeType().equals(JsonNodeType.OBJECT)) {
				result.invalidType(location, schemaName, "object", schemaValue);
			} else {
				ObjectNode schema = (ObjectNode) schemaValue;
				Schema schemaObj = getSchema(schema, String.format("%s.%s", location, schemaName), result);
				if (schemaObj != null) {
					schemas.put(schemaName, schemaObj);
				}
			}
		}

		return schemas;
	}

	public Discriminator getDiscriminator(ObjectNode node, String location, ParseResult result) {
		Discriminator discriminator = new Discriminator();

		String value = getString("propertyName", node, true, location, result);
		if ((result.isAllowEmptyStrings() && value != null) || (!result.isAllowEmptyStrings() && !StringUtils.isBlank(value))) {
			discriminator.setPropertyName(value);
		}

		ObjectNode mappingNode = getObject("mapping", node, false, location, result);
		if (mappingNode != null) {
			Map<String, String> mapping = new LinkedHashMap<>();
			Set<String> keys = getKeys(mappingNode);
			for (String key : keys) {
				mapping.put(key, mappingNode.get(key).asText());
			}
			discriminator.setMapping(mapping);
		}

		if(result.isOpenapi31()) {
			Set<String> keys = getKeys(node);
			for (String key : keys) {
				if (key.startsWith("x-")) {
					Map<String, Object> extensions = getExtensions(node);
					if (extensions != null && extensions.size() > 0) {
						discriminator.setExtensions(extensions);
					}
				}
			}
		}

		return discriminator;

	}

	public Schema getSchema(JsonNode jsonNode, String location, ParseResult result) {
		if (jsonNode == null) {
			return null;
		}
		//Added to handle NPE from ResolverCache when Trying to dereference a schema
		if (result == null){
			result = new ParseResult();
			result.setAllowEmptyStrings(true);
		}

		Schema schema = null;
		List<JsonSchemaParserExtension> jsonschemaExtensions = getJsonSchemaParserExtensions();

		/* TODO!! solve this
		at the moment path passed as string (basePath) from upper components can be both an absolute url or a relative one
		when it's relative, e.g. currently when parsing a file passing the location as relative ref
		 */

		for (JsonSchemaParserExtension jsonschemaExtension: jsonschemaExtensions) {
			schema = jsonschemaExtension.getSchema(jsonNode, location, result, rootMap, basePath);
			if (schema != null) {
				return schema;
			}
		}

		if (result.isOpenapi31()) {
			return getJsonSchema(jsonNode, location, result);
		}

        ObjectNode node = null;
        if (jsonNode.isObject()) {
            node = (ObjectNode) jsonNode;
        } else {
            result.invalidType(location, "", "object", jsonNode);
            return null;
        }
		ArrayNode oneOfArray = getArray("oneOf", node, false, location, result);
		ArrayNode allOfArray = getArray("allOf", node, false, location, result);
		ArrayNode anyOfArray = getArray("anyOf", node, false, location, result);
		ObjectNode itemsNode = getObject("items", node, false, location, result);

		if ((allOfArray != null) || (anyOfArray != null) || (oneOfArray != null)) {
			ComposedSchema composedSchema = new ComposedSchema();

			if (allOfArray != null) {

				for (JsonNode n : allOfArray) {
					if (n.isObject()) {
						schema = getSchema((ObjectNode) n, location, result);
						composedSchema.addAllOfItem(schema);
					}
				}
				schema = composedSchema;
			}
			if (anyOfArray != null) {

				for (JsonNode n : anyOfArray) {
					if (n.isObject()) {
						schema = getSchema((ObjectNode) n, location, result);
						composedSchema.addAnyOfItem(schema);
					}
				}
				schema = composedSchema;
			}
			if (oneOfArray != null) {

				for (JsonNode n : oneOfArray) {
					if (n.isObject()) {
						schema = getSchema((ObjectNode) n, location, result);
						composedSchema.addOneOfItem(schema);
					}
				}
				schema = composedSchema;
			}
		}

		if (itemsNode != null && result.isInferSchemaType()) {
			ArraySchema items = new ArraySchema();
			if (itemsNode.getNodeType().equals(JsonNodeType.OBJECT)) {
				items.setItems(getSchema(itemsNode, location, result));
			} else if (itemsNode.getNodeType().equals(JsonNodeType.ARRAY)) {
				for (JsonNode n : itemsNode) {
					if (n.isValueNode()) {
						items.setItems(getSchema(itemsNode, location, result));
					}
				}
			}
			schema = items;
		}else if (itemsNode != null){
			Schema items = new Schema();
			if (itemsNode.getNodeType().equals(JsonNodeType.OBJECT)) {
				items.setItems(getSchema(itemsNode, location, result));
			} else if (itemsNode.getNodeType().equals(JsonNodeType.ARRAY)) {
				for (JsonNode n : itemsNode) {
					if (n.isValueNode()) {
						items.setItems(getSchema(itemsNode, location, result));
					}
				}
			}
			schema = items;
		}

		Boolean additionalPropertiesBoolean = getBoolean("additionalProperties", node, false, location, result);

		ObjectNode additionalPropertiesObject =
				additionalPropertiesBoolean == null
						? getObject("additionalProperties", node, false, location, result)
						: null;

		Object additionalProperties =
				additionalPropertiesObject != null
						? getSchema(additionalPropertiesObject, location, result)
						: additionalPropertiesBoolean;

		if (additionalProperties != null && result.isInferSchemaType()) {
			if (schema == null) {
				schema =
						additionalProperties.equals(Boolean.FALSE)
								? new ObjectSchema()
								: new MapSchema();
			}
			schema.setAdditionalProperties(additionalProperties);
		} else if (additionalProperties != null) {
            if (schema == null) {
                schema = new Schema();
            }
            schema.setAdditionalProperties(additionalProperties);
        }

		if (schema == null) {
			schema = SchemaTypeUtil.createSchemaByType(node);
		}

		JsonNode ref = node.get("$ref");
		if (ref != null) {
			if (ref.getNodeType().equals(JsonNodeType.STRING)) {

				if (location.startsWith("paths")) {
					try {
						String components[] = ref.asText().split("#/components");
						if ((ref.asText().startsWith("#/components")) && (components.length > 1)) {
							String[] childComponents = components[1].split("/");
							String[] newChildComponents = Arrays.copyOfRange(childComponents, 1,
									childComponents.length);
							boolean isValidComponent = ReferenceValidator.valueOf(newChildComponents[0])
									.validateComponent(this.components,
											newChildComponents[1]);
							if (!isValidComponent) {
								result.missing(location, ref.asText());
							}
						}
					} catch (Exception e) {
						result.missing(location, ref.asText());
					}
				}

				String mungedRef = mungedRef(ref.textValue());
				if (mungedRef != null) {
					schema.set$ref(mungedRef);
				} else {
					schema.set$ref(ref.asText());
				}
                /* TODO currently only capable of validating if ref is to root schema withing #/components/schemas
                 * need to evaluate json pointer instead to also allow validation of nested schemas
                 * e.g. #/components/schemas/foo/properties/bar
                 */
                if(schema.get$ref().startsWith("#/components/schemas") && StringUtils.countMatches(schema.get$ref(), "/") == 3){
                    String refName  = schema.get$ref().substring(schema.get$ref().lastIndexOf("/")+1);
                    localSchemaRefs.put(refName,location);
                }
                if(ref.textValue().startsWith("#/components") && !(ref.textValue().startsWith("#/components/schemas"))) {
                    result.warning(location, "$ref target "+ref.textValue() +" is not of expected type Schema");
                }
				return schema;
			} else {
				result.invalidType(location, "$ref", "string", node);
				return null;
			}
		}


        getCommonSchemaFields(node, location, result, schema);
        String value;
        Boolean bool;

        bool = getBoolean("exclusiveMaximum", node, false, location, result);
        if (bool != null) {
            schema.setExclusiveMaximum(bool);
        }

        bool = getBoolean("exclusiveMinimum", node, false, location, result);
        if (bool != null) {
            schema.setExclusiveMinimum(bool);
        }


		ArrayNode enumArray = getArray("enum", node, false, location, result);
		if (enumArray != null) {
			for (JsonNode n : enumArray) {
				if (n.isNumber()) {
					schema.addEnumItemObject(n.numberValue());
				} else if (n.isBoolean()) {
					schema.addEnumItemObject(n.booleanValue());
				} else if (n.isValueNode()) {
					try {
						schema.addEnumItemObject(getDecodedObject(schema, n.asText(null)));
					} catch (ParseException e) {
						result.invalidType(location, String.format("enum=`%s`", e.getMessage()),
								schema.getFormat(), n);
					}
				} else if (n.isContainerNode()) {
					schema.addEnumItemObject(n.isNull() ? null : n);
				} else {
					result.invalidType(location, "enum", "value", n);
				}
			}
		}

		value = getString("type", node, false, location, result);
		if (StringUtils.isBlank(schema.getType())) {
			if ((result.isAllowEmptyStrings() && value != null) || (!result.isAllowEmptyStrings() && !StringUtils.isBlank(value))) {
				schema.setType(value);
			} else if (result.isInferSchemaType()){
				// may have an enum where type can be inferred
				JsonNode enumNode = node.get("enum");
				if (enumNode != null && enumNode.isArray()) {
					String type = inferTypeFromArray((ArrayNode) enumNode);
					schema.setType(type);
				}
			}
			if ("array".equals(schema.getType()) && schema.getItems() == null) {
				result.missing(location, "items");
			}
		}

		ObjectNode notObj = getObject("not", node, false, location, result);
		if (notObj != null) {
			Schema not = getSchema(notObj, location, result);
			if (not != null) {
				schema.setNot(not);
			}
		}


		Map<String, Schema> properties = new LinkedHashMap<>();
		ObjectNode propertiesObj = getObject("properties", node, false, location, result);
		Schema property = null;

		Set<String> keys = getKeys(propertiesObj);
		for (String name : keys) {
			JsonNode propertyValue = propertiesObj.get(name);
			if (!propertyValue.getNodeType().equals(JsonNodeType.OBJECT)) {
				result.invalidType(location, "properties", "object", propertyValue);
			} else {
				if (propertiesObj != null) {
					property = getSchema((ObjectNode) propertyValue, location, result);
					if (property != null) {
						properties.put(name, property);
					}
				}
			}
		}
		if (propertiesObj != null) {
			schema.setProperties(properties);
		}

		//sets default value according to the schema type
		if (node.get("default") != null && result.isInferSchemaType()) {
            if (!StringUtils.isBlank(schema.getType())) {
                if (schema.getType().equals("array")) {
                    ArrayNode array = getArray("default", node, false, location, result);
                    if (array != null) {
                        schema.setDefault(array);
                    }
                } else if (schema.getType().equals("string")) {
                    value = getString("default", node, false, location, result);
                    if ((result.isAllowEmptyStrings() && value != null) || (!result.isAllowEmptyStrings() && !StringUtils.isBlank(value))) {
                        try {
                            schema.setDefault(getDecodedObject(schema, value));
                        } catch (ParseException e) {
                            result.invalidType(location, String.format("default=`%s`", e.getMessage()),
                                    schema.getFormat(), node);
                        }
                    }
                } else if (schema.getType().equals("boolean")) {
                    bool = getBoolean("default", node, false, location, result);
                    if (bool != null) {
                        schema.setDefault(bool);
                    }
                } else if (schema.getType().equals("object")) {
                    Object object = getObject("default", node, false, location, result);
                    if (object != null) {
                        schema.setDefault(object);
                    }
                } else if (schema.getType().equals("integer")) {
                    Integer number = getInteger("default", node, false, location, result);
                    if (number != null) {
                        schema.setDefault(number);
                    }
                } else if (schema.getType().equals("number")) {
                    BigDecimal number = getBigDecimal("default", node, false, location, result);
                    if (number != null) {
                        schema.setDefault(number);
                    }
                }
            } else {
                Object defaultObject = getAnyType("default", node, location, result);
                if (defaultObject != null) {
                    schema.setDefault(defaultObject);
                }
            }
        } else if (node.get("default") != null) {
            Object defaultObject = getAnyType("default", node, location, result);
            if (defaultObject != null) {
                schema.setDefault(defaultObject);
            }
		}else{
			schema.setDefault(null);
		}

		bool = getBoolean("nullable", node, false, location, result);
		if (bool != null) {
			schema.setNullable(bool);
		}

		Map<String, Object> extensions = getExtensions(node);
		if (extensions != null && extensions.size() > 0) {
			schema.setExtensions(extensions);
		}

		Set<String> schemaKeys = getKeys(node);
		Map<String, Set<String>> specKeys = KEYS.get("openapi30");
		for (String key : schemaKeys) {
			if (!specKeys.get("SCHEMA_KEYS").contains(key) && !key.startsWith("x-")) {
				result.extra(location, key, node.get(key));
			}
		}
		return schema;
	}

    protected void getCommonSchemaFields(ObjectNode node, String location, ParseResult result, Schema schema) {
        String value = getString("title", node, false, location, result);
        if ((result.isAllowEmptyStrings() && value != null) || (!result.isAllowEmptyStrings() && !StringUtils.isBlank(value))) {
            schema.setTitle(value);
        }

        ObjectNode discriminatorNode = getObject("discriminator", node, false, location, result);
        if (discriminatorNode != null) {
            schema.setDiscriminator(getDiscriminator(discriminatorNode, location, result));
        }

        BigDecimal bigDecimal = getBigDecimal("multipleOf", node, false, location, result);
        if (bigDecimal != null) {
            if (bigDecimal.compareTo(BigDecimal.ZERO) > 0) {
                schema.setMultipleOf(bigDecimal);
            } else {
                result.warning(location, "multipleOf value must be > 0");
            }
        }

        bigDecimal = getBigDecimal("maximum", node, false, location, result);
        if (bigDecimal != null) {
            schema.setMaximum(bigDecimal);
        }

        bigDecimal = getBigDecimal("minimum", node, false, location, result);
        if (bigDecimal != null) {
            schema.setMinimum(bigDecimal);
        }

        Integer integer = getInteger("minLength", node, false, location, result);
        if (integer != null) {
            schema.setMinLength(integer);
        }

        integer = getInteger("maxLength", node, false, location, result);
        if (integer != null) {
            schema.setMaxLength(integer);
        }

        String pattern = getString("pattern", node, false, location, result);
        if (result.isAllowEmptyStrings() && pattern != null) {
            schema.setPattern(pattern);
        }

        integer = getInteger("maxItems", node, false, location, result);
        if (integer != null) {
            schema.setMaxItems(integer);
        }
        integer = getInteger("minItems", node, false, location, result);
        if (integer != null) {
            schema.setMinItems(integer);
        }

        Boolean bool = getBoolean("uniqueItems", node, false, location, result);
        if (bool != null) {
            schema.setUniqueItems(bool);
        }

        integer = getInteger("maxProperties", node, false, location, result);
        if (integer != null) {
            schema.setMaxProperties(integer);
        }

        integer = getInteger("minProperties", node, false, location, result);
        if (integer != null) {
            schema.setMinProperties(integer);
        }

        ArrayNode required = getArray("required", node, false, location, result);
        if (required != null) {
            List<String> requiredList = new ArrayList<>();
            for (JsonNode n : required) {
                if (n.getNodeType().equals(JsonNodeType.STRING)) {
                    requiredList.add(((TextNode) n).textValue());
                } else {
                    result.invalidType(location, "required", "string", n);
                }
            }
            if (requiredList.size() > 0) {
                schema.setRequired(requiredList);
            }
        }

        value = getString("description", node, false, location, result);
        if ((result.isAllowEmptyStrings() && value != null) || (!result.isAllowEmptyStrings() && !StringUtils.isBlank(value))) {
            schema.setDescription(value);
        }

        value = getString("format", node, false, location, result);
        if ((result.isAllowEmptyStrings() && value != null) || (!result.isAllowEmptyStrings() && !StringUtils.isBlank(value))) {
            schema.setFormat(value);
        }

        bool = getBoolean("readOnly", node, false, location, result);
        if (bool != null) {
            schema.setReadOnly(bool);
        }

        bool = getBoolean("writeOnly", node, false, location, result);
        if (bool != null) {
            schema.setWriteOnly(bool);
        }

        bool =
                Optional.ofNullable(getBoolean("writeOnly", node, false, location, result)).orElse(false) && Optional.ofNullable(getBoolean("readOnly", node, false, location, result)).orElse(false);
        if (bool == true) {
            result.warning(location, " writeOnly and readOnly are both present");

        }

        ObjectNode xmlNode = getObject("xml", node, false, location, result);
        if (xmlNode != null) {
            XML xml = getXml(xmlNode, location, result);
            if (xml != null) {
                schema.setXml(xml);
            }
        }

        ObjectNode externalDocs = getObject("externalDocs", node, false, location, result);
        if (externalDocs != null) {
            ExternalDocumentation docs = getExternalDocs(externalDocs, location, result);
            if (docs != null) {
                schema.setExternalDocs(docs);
            }
        }

        Object example = getAnyType("example", node, location, result);
        if (example != null) {
            schema.setExample(example instanceof NullNode ? null : example);
        }

        bool = getBoolean("deprecated", node, false, location, result);
        if (bool != null) {
            schema.setDeprecated(bool);
        }

    }


    /**
	 * Decodes the given string and returns an object applicable to the given schema.
	 * Throws a ParseException if no applicable object can be recognized.
	 */
	private Object getDecodedObject(Schema schema, String objectString) throws ParseException {
		Object object =
				objectString == null ?
						null :

						schema.getClass().equals(DateSchema.class) ?
								toDate(objectString) :

								schema.getClass().equals(DateTimeSchema.class) ?
										toDateTime(objectString) :

										schema.getClass().equals(ByteArraySchema.class) ?
												toBytes(objectString) :

												objectString;

		if (object == null && objectString != null) {
			throw new ParseException(objectString, 0);
		}

		return object;
	}

	/**
	 * Decodes the given string and returns an object applicable to the given schema.
	 * Throws a ParseException if no applicable object can be recognized.
	 */
	private Object getDecodedObject31(Schema schema, String objectString) throws ParseException {
		Object object =
				objectString == null ?
						null :

						"string".equals(schema.getType()) && "date".equals(schema.getFormat()) ?
								toDate(objectString) :

								"string".equals(schema.getType()) && "date-time".equals(schema.getFormat()) ?
										toDateTime(objectString) :

										"string".equals(schema.getType()) && "byte".equals(schema.getFormat()) ?
												toBytes(objectString) :

												objectString;

		if (object == null && objectString != null) {
			throw new ParseException(objectString, 0);
		}

		return object;
	}


	/**
	 * Returns the Date represented by the given RFC3339 date-time string.
	 * Returns null if this string can't be parsed as Date.
	 */
	private OffsetDateTime toDateTime(String dateString) {

		OffsetDateTime dateTime = null;
		try {
			dateTime = OffsetDateTime.parse(dateString);
		} catch (Exception ignore) {
		}

		return dateTime;
	}


	/**
	 * Returns the Date represented by the given RFC3339 full-date string.
	 * Returns null if this string can't be parsed as Date.
	 */
	private Date toDate(String dateString) {
		Matcher matcher = RFC3339_DATE_PATTERN.matcher(dateString);

		Date date = null;
		if (matcher.matches()) {
			String year = matcher.group(1);
			String month = matcher.group(2);
			String day = matcher.group(3);

			try {
				date =
						new Calendar.Builder()
								.setDate(Integer.parseInt(year), Integer.parseInt(month) - 1,
										Integer.parseInt(day))
								.build()
								.getTime();
			} catch (Exception ignore) {
			}
		}

		return date;
	}


	/**
	 * Returns the byte array represented by the given base64-encoded string.
	 * Returns null if this string is not a valid base64 encoding.
	 */
	private byte[] toBytes(String byteString) {
		byte[] bytes;

		try {
			bytes = Base64.getDecoder().decode(byteString);
		} catch (Exception e) {
			bytes = null;
		}

		return bytes;
	}


	public Map<String, Example> getExamples(ObjectNode obj, String location, ParseResult result,
											boolean underComponents) {
		if (obj == null) {
			return null;
		}
		Map<String, Example> examples = new LinkedHashMap<>();

		Set<String> exampleKeys = getKeys(obj);
		for (String exampleName : exampleKeys) {
			if (underComponents) {
				if (!Pattern.matches("^[a-zA-Z0-9\\.\\-_]+$",
						exampleName)) {
					result.warning(location, "Example name " + exampleName + " doesn't adhere to regular " +
							"expression ^[a-zA-Z0-9\\.\\-_]+$");
				}
			}

			JsonNode exampleValue = obj.get(exampleName);
			if (!validNodeTypes.contains(exampleValue.getNodeType())) {
				result.invalidType(location, exampleName, "object", exampleValue);
			} else if (exampleValue.getNodeType().equals(JsonNodeType.STRING)) {
                TextNode stringExample = (TextNode) exampleValue;
                if (stringExample != null) {
                    Example exampleObj = getTextExample(stringExample);
                    if (exampleObj != null) {
                        examples.put(exampleName, exampleObj);
                    }
                }
            } else {
				ObjectNode example = (ObjectNode) exampleValue;
				if (example != null) {
					Example exampleObj = getExample(example, String.format("%s.%s", location, exampleName),
							result);
					if (exampleObj != null) {
						examples.put(exampleName, exampleObj);
					}
				}
			}
		}
		return examples;
	}

    private Example getTextExample(TextNode textNode) {
        if (textNode == null) return null;
        Example example = new Example();
        example.setValue(textNode.textValue());
        return example;
    }

	public Example getExample(ObjectNode node, String location, ParseResult result) {
		if (node == null)
			return null;

		Example example = new Example();

		JsonNode ref = node.get("$ref");
		if (ref != null) {
			if (ref.getNodeType().equals(JsonNodeType.STRING)) {
				String mungedRef = mungedRef(ref.textValue());
				if (mungedRef != null) {
					example.set$ref(mungedRef);
				} else {
					example.set$ref(ref.textValue());
				}
                if(ref.textValue().startsWith("#/components") && !(ref.textValue().startsWith("#/components/examples"))) {
                    result.warning(location, "$ref target "+ref.textValue() +" is not of expected type Examples");
                }
                if(result.isOpenapi31()){
                    String value = getString("summary", node, false, location, result);
                    if (StringUtils.isNotBlank(value)) {
                        example.setSummary(value);
                    }
                    value = getString("description", node, false, location, result);
                    if (StringUtils.isNotBlank(value)) {
                        example.setDescription(value);
                    }
                }
				return example;
			} else {
				result.invalidType(location, "$ref", "string", node);
				return null;
			}
		}

		String value = getString("summary", node, false, location, result);
		if ((result.isAllowEmptyStrings() && value != null) || (!result.isAllowEmptyStrings() && !StringUtils.isBlank(value))) {
			example.setSummary(value);
		}

		value = getString("description", node, false, location, result);
		if ((result.isAllowEmptyStrings() && value != null) || (!result.isAllowEmptyStrings() && !StringUtils.isBlank(value))) {
			example.setDescription(value);
		}

		Object sample = getAnyType("value", node, location, result);
		if (sample != null) {
			example.setValue(sample instanceof NullNode ? null : sample);
		}

		value = getString("externalValue", node, false, location, result);
		if ((result.isAllowEmptyStrings() && value != null) || (!result.isAllowEmptyStrings() && !StringUtils.isBlank(value))) {
			example.setExternalValue(value);
		}

		if (sample != null && value != null) {
			result.warning(location, " value and externalValue are both present");
		}

		Map<String, Object> extensions = getExtensions(node);
		if (extensions != null && extensions.size() > 0) {
			example.setExtensions(extensions);
		}

		Set<String> keys = getKeys(node);
		Map<String, Set<String>> specKeys = KEYS.get(result.isOpenapi31() ? "openapi31" : "openapi30");
		for (String key : keys) {
			if (!specKeys.get("EXAMPLE_KEYS").contains(key) && !key.startsWith("x-")) {
				result.extra(location, key, node.get(key));
			}
			validateReservedKeywords(specKeys, key, location, result);

		}
		return example;
	}


	public void setStyle(String value, Parameter parameter, String location, ObjectNode obj, ParseResult result) {
		if (StringUtils.isBlank(value)) {
			if (QUERY_PARAMETER.equals(parameter.getIn()) || COOKIE_PARAMETER.equals(parameter.getIn())) {
				parameter.setStyle(StyleEnum.FORM);
			} else if (PATH_PARAMETER.equals(parameter.getIn()) || HEADER_PARAMETER.equals(parameter.getIn())) {
				parameter.setStyle(StyleEnum.SIMPLE);
			}
		} else {
			if (value.equals(StyleEnum.FORM.toString())) {
				parameter.setStyle(StyleEnum.FORM);
			} else if (value.equals(StyleEnum.DEEPOBJECT.toString())) {
				parameter.setStyle(StyleEnum.DEEPOBJECT);
			} else if (value.equals(StyleEnum.LABEL.toString())) {
				parameter.setStyle(StyleEnum.LABEL);
			} else if (value.equals(StyleEnum.MATRIX.toString())) {
				parameter.setStyle(StyleEnum.MATRIX);
			} else if (value.equals(StyleEnum.PIPEDELIMITED.toString())) {
				parameter.setStyle(StyleEnum.PIPEDELIMITED);
			} else if (value.equals(StyleEnum.SIMPLE.toString())) {
				parameter.setStyle(StyleEnum.SIMPLE);
			} else if (value.equals(StyleEnum.SPACEDELIMITED.toString())) {
				parameter.setStyle(StyleEnum.SPACEDELIMITED);
			} else {
				result.invalidType(location, "style", "StyleEnum", obj);
			}
		}
	}

	public ApiResponses getResponses(ObjectNode node, String location, ParseResult result,
									 boolean underComponents) {
		if (node == null) {
			return null;
		}

		ApiResponses apiResponses = new ApiResponses();
		Set<String> keys = getKeys(node);

		for (String key : keys) {
			if (underComponents) {
				if (!Pattern.matches("^[a-zA-Z0-9\\.\\-_]+$",
						key)) {
					result.warning(location, "Response key " + key + " doesn't adhere to regular expression " +
							"^[a-zA-Z0-9\\.\\-_]+$");
				}
			}

			if (key.startsWith("x-")) {
				Map<String, Object> extensions = getExtensions(node);
				if (extensions != null && extensions.size() > 0) {
					apiResponses.setExtensions(extensions);
				}
			} else {
				ObjectNode obj = getObject(key, node, false, location, result);
				if (obj != null) {
					ApiResponse response = getResponse(obj, String.format("%s.%s", location, key), result);
					if (response != null) {
						apiResponses.put(key, response);
					}
				}
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
				String mungedRef = mungedRef(ref.textValue());
				if (mungedRef != null) {
					apiResponse.set$ref(mungedRef);
				} else {
					apiResponse.set$ref(ref.textValue());
				}
                if(ref.textValue().startsWith("#/components") && !(ref.textValue().startsWith("#/components/responses"))) {
                    result.warning(location, "$ref target "+ref.textValue() +" is not of expected type Response");
                }
				if(result.isOpenapi31()){
					String value = getString("description", node, false, location, result);
					if (StringUtils.isNotBlank(value)) {
						apiResponse.setDescription(value);
					}
				}
				return apiResponse;
			} else {
				result.invalidType(location, "$ref", "string", node);
				return null;
			}
		}

		String value = getString("description", node, true, location, result);
		if ((result.isAllowEmptyStrings() && value != null) || (!result.isAllowEmptyStrings() && !StringUtils.isBlank(value))) {
			apiResponse.description(value);
		}


		ObjectNode headerObject = getObject("headers", node, false, location, result);
		if (headerObject != null) {
			Map<String, Header> headers = getHeaders(headerObject, String.format("%s.%s", location, "headers"), result, false);
			if (headers != null && headers.size() > 0) {
				apiResponse.setHeaders(headers);
			}
		}

		ObjectNode linksObj = getObject("links", node, false, location, result);
		if (linksObj != null) {
			Map<String, Link> links = getLinks(linksObj, String.format("%s.%s", location, "links"), result, false);
			if (links != null && links.size() > 0) {
				apiResponse.setLinks(links);
			}
		}

		ObjectNode contentObject = getObject("content", node, false, location, result);
		if (contentObject != null) {
			apiResponse.setContent(getContent(contentObject, String.format("%s.%s", location, "content"), result));
		}

		Map<String, Object> extensions = getExtensions(node);
		if (extensions != null && extensions.size() > 0) {
			apiResponse.setExtensions(extensions);
		}

		Set<String> keys = getKeys(node);
		Map<String, Set<String>> specKeys = KEYS.get(result.isOpenapi31() ? "openapi31" : "openapi30");
		for (String key : keys) {
			if (!specKeys.get("RESPONSE_KEYS").contains(key) && !key.startsWith("x-")) {
				result.extra(location, key, node.get(key));
			}
			validateReservedKeywords(specKeys, key, location, result);
		}


		return apiResponse;
	}

	public List<String> getTagsStrings(ArrayNode nodes, String location, ParseResult result) {
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
		List<String> tags = getTagsStrings(array, String.format("%s.%s", location, "tags"), result);
		if (tags != null) {
			operation.setTags(tags);
		}
		String value = getString("summary", obj, false, location, result);
		if ((result.isAllowEmptyStrings() && value != null) || (!result.isAllowEmptyStrings() && !StringUtils.isBlank(value))) {
			operation.setSummary(value);
		}

		value = getString("description", obj, false, location, result);
		if ((result.isAllowEmptyStrings() && value != null) || (!result.isAllowEmptyStrings() && !StringUtils.isBlank(value))) {
			operation.setDescription(value);
		}

		ObjectNode externalDocs = getObject("externalDocs", obj, false, location, result);
		ExternalDocumentation docs = getExternalDocs(externalDocs, String.format("%s.%s", location,
				"externalDocs"), result);
		if (docs != null) {
			operation.setExternalDocs(docs);
		}
		value = getString("operationId", obj, false, location, result, operationIDs);
		if ((result.isAllowEmptyStrings() && value != null) || (!result.isAllowEmptyStrings() && !StringUtils.isBlank(value))) {
			operation.operationId(value);
		}

		ArrayNode parameters = getArray("parameters", obj, false, location, result);
		if (parameters != null) {
			operation.setParameters(getParameterList(parameters, String.format("%s.%s", location, "parameters"),
					result));
		}

		final ObjectNode requestObjectNode = getObject("requestBody", obj, false, location, result);
		if (requestObjectNode != null) {
			operation.setRequestBody(getRequestBody(requestObjectNode, String.format("%s.%s", location,
					"requestBody"), result));
		}

		ObjectNode responsesNode = getObject("responses", obj, true, location, result);
		ApiResponses responses = getResponses(responsesNode, String.format("%s.%s", location, "responses"), result
				, false);
		if (responses != null) {
			operation.setResponses(responses);
		}

		ObjectNode callbacksNode = getObject("callbacks", obj, false, location, result);
		Map<String, Callback> callbacks = getCallbacks(callbacksNode, String.format("%s.%s", location,
				"callbacks"), result, false);
		if (callbacks != null) {
			operation.setCallbacks(callbacks);
		}

		Boolean deprecated = getBoolean("deprecated", obj, false, location, result);
		if (deprecated != null) {
			operation.setDeprecated(deprecated);
		}

		array = getArray("servers", obj, false, location, result);
		if (array != null && array.size() > 0) {
			operation.setServers(getServersList(array, String.format("%s.%s", location, "servers"), result));
		}


		array = getArray("security", obj, false, location, result);
		if (array != null) {
			operation.setSecurity(getSecurityRequirementsList(array, String.format("%s.%s", location, "security"),
					result));
		}


		Map<String, Object> extensions = getExtensions(obj);
		if (extensions != null && extensions.size() > 0) {
			operation.setExtensions(extensions);
		}

		Set<String> keys = getKeys(obj);
		Map<String, Set<String>> specKeys = KEYS.get(result.isOpenapi31() ? "openapi31" : "openapi30");
		for (String key : keys) {
			if (!specKeys.get("OPERATION_KEYS").contains(key) && !key.startsWith("x-")) {
				result.extra(location, key, obj.get(key));
			}
			validateReservedKeywords(specKeys, key, location, result);
		}
		return operation;
	}

	public List<SecurityRequirement> getSecurityRequirementsList(ArrayNode nodes, String location, ParseResult
			result) {
		if (nodes == null)
			return null;

		List<SecurityRequirement> securityRequirements = new ArrayList<>();

		for (JsonNode node : nodes) {
			if (node.getNodeType().equals(JsonNodeType.OBJECT)) {
				SecurityRequirement securityRequirement = new SecurityRequirement();
				Set<String> keys = getKeys((ObjectNode) node);
				if (keys.size() == 0) {
					securityRequirements.add(securityRequirement);
				} else {
					for (String key : keys) {
						if (key != null) {
							JsonNode value = node.get(key);
							if (key != null && JsonNodeType.ARRAY.equals(value.getNodeType())) {
								ArrayNode arrayNode = (ArrayNode) value;
								List<String> scopes = Stream
										.generate(arrayNode.elements()::next)
										.map((n) -> n.asText())
										.limit(arrayNode.size())
										.collect(Collectors.toList());
								securityRequirement.addList(key, scopes);
							}
						}
					}
					if (securityRequirement.size() > 0) {
						securityRequirements.add(securityRequirement);
					}
				}
			}
		}

		return securityRequirements;

	}

	public Map<String, RequestBody> getRequestBodies(ObjectNode obj, String location, ParseResult result,
													 boolean underComponents) {
		if (obj == null) {
			return null;
		}
		Map<String, RequestBody> bodies = new LinkedHashMap<>();

		Set<String> bodyKeys = getKeys(obj);
		for (String bodyName : bodyKeys) {
			if (underComponents) {
				if (!Pattern.matches("^[a-zA-Z0-9\\.\\-_]+$",
						bodyName)) {
					result.warning(location, "RequestBody name " + bodyName + " doesn't adhere to regular " +
							"expression ^[a-zA-Z0-9\\.\\-_]+$");
				}
			}
			JsonNode bodyValue = obj.get(bodyName);
			if (!bodyValue.getNodeType().equals(JsonNodeType.OBJECT)) {
				result.invalidType(location, bodyName, "object", bodyValue);
			} else {
				ObjectNode bodyObj = (ObjectNode) bodyValue;
				RequestBody body = getRequestBody(bodyObj, String.format("%s.%s", location, bodyName), result);
				if (body != null) {
					bodies.put(bodyName, body);
				}
			}
		}
		return bodies;
	}


	public RequestBody getRequestBody(ObjectNode node, String location, ParseResult result) {
		if (node == null) {
			return null;
		}
		final RequestBody body = new RequestBody();


		JsonNode ref = node.get("$ref");
		if (ref != null) {
			if (ref.getNodeType().equals(JsonNodeType.STRING)) {
				String mungedRef = mungedRef(ref.textValue());
				if (mungedRef != null) {
					body.set$ref(mungedRef);
				} else {
					body.set$ref(ref.textValue());
				}
                if(ref.textValue().startsWith("#/components") && !(ref.textValue().startsWith("#/components/requestBodies"))) {
                    result.warning(location, "$ref target "+ref.textValue() +" is not of expected type RequestBody");
                }
				if (result.isOpenapi31()) {
					String desc = getString("description", node, false, location, result);
					if (StringUtils.isNotBlank(desc)) {
						body.setDescription(desc);
					}
				}
				return body;
			} else {
				result.invalidType(location, "$ref", "string", node);
				return null;
			}
		}


		final String description = getString("description", node, false, location, result);
		if (result.isAllowEmptyStrings() && description != null) {
			body.setDescription(description);
		}

		final Boolean required = getBoolean("required", node, false, location, result);
		if (required != null) {
			body.setRequired(required);
		}

		final ObjectNode contentNode = getObject("content", node, true, location, result);
        Content content = getContent(contentNode, location + ".content", result);
		if(content != null && content.isEmpty()) {
            result.unsupported(location,"content with no media type",contentNode);
            result.invalid();
        }
        else {
            body.setContent(content);
        }

		Map<String, Object> extensions = getExtensions(node);
		if (extensions != null && extensions.size() > 0) {
			body.setExtensions(extensions);
		}

		Set<String> keys = getKeys(node);
		Map<String, Set<String>> specKeys = KEYS.get(result.isOpenapi31() ? "openapi31" : "openapi30");
		for (String key : keys) {
			if (!specKeys.get("REQUEST_BODY_KEYS").contains(key) && !key.startsWith("x-")) {
				result.extra(location, key, node.get(key));
			}
			validateReservedKeywords(specKeys, key, location, result);
		}
		return body;
	}

	public String inferTypeFromArray(ArrayNode an) {
		if (an.size() == 0) {
			return "string";
		}
		String type = null;
		for (int i = 0; i < an.size(); i++) {
			JsonNode element = an.get(0);
			if (element.isBoolean()) {
				if (type == null) {
					type = "boolean";
				} else if (!"boolean".equals(type)) {
					type = "string";
				}
			} else if (element.isNumber()) {
				if (type == null) {
					type = "number";
				} else if (!"number".equals(type)) {
					type = "string";
				}
			} else {
				type = "string";
			}
		}

		return type;
	}

	/**
	 * Locates extensions on the current thread class loader and then, if it differs from this class classloader (as in
	 * OSGi), locates extensions from this class classloader as well.
	 * @return a list of extensions
	 */
	public static List<JsonSchemaParserExtension> getJsonSchemaParserExtensions() {
		final ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		final List<JsonSchemaParserExtension> extensions = getJsonSchemaParserExtensions(tccl);
		final ClassLoader cl = JsonSchemaParserExtension.class.getClassLoader();
		if (cl != tccl) {
			extensions.addAll(getJsonSchemaParserExtensions(cl));
		}
		return extensions;
	}

	protected static List<JsonSchemaParserExtension> getJsonSchemaParserExtensions(ClassLoader cl) {
		final List<JsonSchemaParserExtension> extensions = new ArrayList<>();

		final ServiceLoader<JsonSchemaParserExtension> loader = ServiceLoader.load(JsonSchemaParserExtension.class, cl);
		for (JsonSchemaParserExtension extension : loader) {
			extensions.add(extension);
		}
		return extensions;
	}

	public Schema getJsonSchema(JsonNode jsonNode, String location, ParseResult result) {
		if (jsonNode == null) {
			return null;
		}
		Schema schema = null;
        Boolean boolValue = null;
        if (jsonNode.getNodeType().equals(JsonNodeType.BOOLEAN)) {
            boolValue = jsonNode.asBoolean();
        } else if (jsonNode.getNodeType().equals(JsonNodeType.STRING)) {
            String stringValue = jsonNode.textValue();
            if ("true".equalsIgnoreCase(stringValue) || "false".equalsIgnoreCase(stringValue)) {
                boolValue = Boolean.parseBoolean(stringValue);
            } else {
                result.invalidType(location, "", "object", jsonNode);
                return null;
            }
        }
        if (boolValue != null) {
            return new JsonSchema().booleanSchemaValue(boolValue);
        }
        ObjectNode node = null;
        if (jsonNode.isObject()) {
            node = (ObjectNode) jsonNode;
        } else {
            result.invalidType(location, "", "object", jsonNode);
            return null;
        }
		ArrayNode oneOfArray = getArray("oneOf", node, false, location, result);
		ArrayNode allOfArray = getArray("allOf", node, false, location, result);
		ArrayNode anyOfArray = getArray("anyOf", node, false, location, result);
		JsonNode itemsNode = getObjectOrBoolean("items", node, false, location, result);

		if ((allOfArray != null) || (anyOfArray != null) || (oneOfArray != null)) {
            JsonSchema composedSchema = new JsonSchema();

			if (allOfArray != null) {

				for (JsonNode n : allOfArray) {
                    schema = getJsonSchema(n, location, result);
                    composedSchema.addAllOfItem(schema);
				}
				schema = composedSchema;
			}
			if (anyOfArray != null) {

				for (JsonNode n : anyOfArray) {
                    schema = getJsonSchema(n, location, result);
                    composedSchema.addAnyOfItem(schema);
				}
				schema = composedSchema;
			}
			if (oneOfArray != null) {

				for (JsonNode n : oneOfArray) {
                    schema = getJsonSchema(n, location, result);
                    composedSchema.addOneOfItem(schema);
				}
				schema = composedSchema;
			}
		}
        if (itemsNode != null) {
            Schema items = new JsonSchema();
            items.setItems(getJsonSchema(itemsNode, location, result));
            schema = items;
        }
		JsonNode additionalProperties = getObjectOrBoolean("additionalProperties", node, false, location, result);
        if (additionalProperties != null) {
            Schema additionalPropertiesSchema = getJsonSchema(additionalProperties, location, result);
            if (schema == null) {
                schema = new JsonSchema();
            }
            schema.setAdditionalProperties(additionalPropertiesSchema);
        }

        JsonNode unevaluatedProperties = getObjectOrBoolean("unevaluatedProperties", node, false, location, result);
        if (unevaluatedProperties != null) {
            Schema unevaluatedPropertiesSchema = getJsonSchema(unevaluatedProperties, location, result);
            if (schema == null) {
                schema = new JsonSchema();
            }
            schema.setUnevaluatedProperties(unevaluatedPropertiesSchema);
        }

		if (schema == null) {
			schema = new JsonSchema();
		}

		JsonNode ref = node.get("$ref");
		if (ref != null) {
			if (ref.getNodeType().equals(JsonNodeType.STRING)) {

				if (location.startsWith("paths")) {
					try {
						String components[] = ref.asText().split("#/components");
						if ((ref.asText().startsWith("#/components")) && (components.length > 1)) {
							String[] childComponents = components[1].split("/");
							String[] newChildComponents = Arrays.copyOfRange(childComponents, 1,
									childComponents.length);
							boolean isValidComponent = ReferenceValidator.valueOf(newChildComponents[0])
									.validateComponent(this.components,
											newChildComponents[1]);
							if (!isValidComponent) {
								result.missing(location, ref.asText());
							}
						}
					} catch (Exception e) {
						result.missing(location, ref.asText());
					}
				}

				String mungedRef = mungedRef(ref.textValue());
				if (mungedRef != null) {
					schema.set$ref(mungedRef);
				} else {
					schema.set$ref(ref.asText());
				}
                if(ref.textValue().startsWith("#/components") && !(ref.textValue().startsWith("#/components/schemas"))) {
                    result.warning(location, "$ref target "+ref.textValue() +" is not of expected type Schema");
                }
			} else {
				result.invalidType(location, "$ref", "string", node);
			}
		}

        getCommonSchemaFields(node, location, result, schema);
        String value;
        Boolean bool;
        Integer integer;

        if (node.get("default") != null) {
            schema.setDefault(getAnyType("default", node, location, result));
        }

        BigDecimal bigDecimal = getBigDecimal("exclusiveMaximum", node, false, location, result);
        if (bigDecimal != null) {
            schema.setExclusiveMaximumValue(bigDecimal);
        }

        bigDecimal = getBigDecimal("exclusiveMinimum", node, false, location, result);
        if (bigDecimal != null) {
            schema.setExclusiveMinimumValue(bigDecimal);
        }

		integer = getInteger("minContains", node, false, location, result);
		if (integer != null) {
			schema.setMinContains(integer);
		}

		integer = getInteger("maxContains", node, false, location, result);
		if (integer != null) {
			schema.setMaxContains(integer);
		}

		String typeString = getString("type", node, false, location, result, null, true);
		ArrayNode typeArray = getArray("type", node, false, location, result, true);

        if ((result.isAllowEmptyStrings() && typeString != null) || (!result.isAllowEmptyStrings() && !StringUtils.isBlank(typeString))) {
            schema.addType(typeString);
		} else if (typeArray != null) {
			for (JsonNode n : typeArray) {
				if (n.isValueNode()) {
					if (!JSON_SCHEMA_2020_12_TYPES.contains(n.asText())) {
						result.warning(location, " invalid type " + n.asText());
					}
					if (!schema.addType(n.asText())) {
						result.warning(location, " duplicated type " + n.asText());
					}
				} else {
					result.invalidType(location, "type", "value", n);
				}
			}

		} else {
			// may have an enum where type can be inferred
			JsonNode enumNode = node.get("enum");
			if (enumNode != null && enumNode.isArray()) {
				String type = inferTypeFromArray((ArrayNode) enumNode);
				schema.addType(type);
			}
		}

		ArrayNode enumArray = getArray("enum", node, false, location, result);
		if (enumArray != null) {
			for (JsonNode n : enumArray) {
				if (n.isNumber()) {
					schema.addEnumItemObject(n.numberValue());
				} else if (n.isBoolean()) {
					schema.addEnumItemObject(n.booleanValue());
				} else if (n.isValueNode()) {
					try {
						schema.addEnumItemObject(getDecodedObject31(schema, n.asText(null)));
					} catch (ParseException e) {
						result.invalidType(location, String.format("enum=`%s`", e.getMessage()),
								schema.getFormat(), n);
					}
				} else if (n.isContainerNode()) {
					schema.addEnumItemObject(n.isNull() ? null : n);
				} else {
					result.invalidType(location, "enum", "value", n);
				}
			}
		}

		JsonNode notObj = getObjectOrBoolean("not", node, false, location, result);
		if (notObj != null) {
			Schema not = getJsonSchema(notObj, location, result);
			if (not != null) {
				schema.setNot(not);
			}
		}

        JsonNode contentSchemaObj = getObjectOrBoolean("contentSchema", node, false, location, result);
		if (contentSchemaObj != null) {
			Schema contentSchema = getJsonSchema(contentSchemaObj, location, result);
			if (contentSchema != null) {
				schema.setContentSchema(contentSchema);
			}
		}

        JsonNode propertyNamesObj = getObjectOrBoolean("propertyNames", node, false, location, result);
		if (propertyNamesObj != null) {
			Schema propertyNames = getJsonSchema(propertyNamesObj, location, result);
			if (propertyNames != null) {
				schema.setPropertyNames(propertyNames);
			}
		}

        JsonNode ifObj = getObjectOrBoolean("if", node, false, location, result);
		if (ifObj != null) {
			Schema _if = getJsonSchema(ifObj, location, result);
			if (_if != null) {
				schema.setIf(_if);
			}
		}

        JsonNode thenObj = getObjectOrBoolean("then", node, false, location, result);
		if (thenObj != null) {
			Schema _then = getJsonSchema(thenObj, location, result);
			if (_then != null) {
				schema.setThen(_then);
			}
		}

        JsonNode elseObj = getObjectOrBoolean("else", node, false, location, result);
		if (elseObj != null) {
			Schema _else = getJsonSchema(elseObj, location, result);
			if (_else != null) {
				schema.setElse(_else);
			}
		}

        JsonNode unevaluatedItems = getObjectOrBoolean("unevaluatedItems", node, false, location, result);
		if (unevaluatedItems != null) {
			Schema unevaluatedItemsSchema = getJsonSchema(unevaluatedItems, location, result);
			if (unevaluatedItemsSchema != null) {
				schema.setUnevaluatedItems(unevaluatedItemsSchema);
			}
		}


		Map<String, List<String>> dependentRequiredList = new LinkedHashMap<>();
		ObjectNode dependentRequiredObj = getObject("dependentRequired", node, false, location, result);
		List<String> dependentRequired = new ArrayList<>();

		Set<String> dependentRequiredKeys = getKeys(dependentRequiredObj);
		for (String name : dependentRequiredKeys) {
			JsonNode dependentRequiredValue = dependentRequiredObj.get(name);
			if (!dependentRequiredValue.getNodeType().equals(JsonNodeType.ARRAY)) {
				result.invalidType(location, "dependentRequired", "object", dependentRequiredValue);
			} else {
				if (dependentRequiredObj != null) {
					for (JsonNode n : dependentRequiredValue){
						if (n.getNodeType().equals(JsonNodeType.STRING)) {
							dependentRequired.add(n.textValue());
						}
					}
					if (dependentRequired != null) {
						dependentRequiredList.put(name, dependentRequired);
					}
				}
			}
		}
		if (dependentRequiredObj != null) {
			schema.setDependentRequired(dependentRequiredList);
		}

		Map<String, Schema> dependentSchemasList = new LinkedHashMap<>();
		ObjectNode dependentSchemasObj = getObject("dependentSchemas", node, false, location, result);
        if (dependentSchemasObj != null) {
            Schema dependentSchemas = null;

            Set<String> dependentSchemasKeys = getKeys(dependentSchemasObj);
            for (String name : dependentSchemasKeys) {
                JsonNode dependentSchemasValue = dependentSchemasObj.get(name);
                dependentSchemas = getJsonSchema(dependentSchemasValue, location, result);
                if (dependentSchemas != null) {
                    dependentSchemasList.put(name, dependentSchemas);
                }
            }
            if (dependentSchemasObj != null) {
                schema.setDependentSchemas(dependentSchemasList);
            }
        }

		//prefixItems
		ArrayNode prefixItemsArray = getArray("prefixItems", node, false, location, result);
		if(prefixItemsArray != null) {
			Schema prefixItems = new JsonSchema();

			List<Schema> prefixItemsList = new ArrayList<>();
			for (JsonNode n : prefixItemsArray) {
                prefixItems = getJsonSchema(n, location, result);
                if (prefixItems != null) {
                    prefixItemsList.add(prefixItems);
                }
			}
			if (prefixItemsList.size() > 0) {
				schema.setPrefixItems(prefixItemsList);
			}
		}

		JsonNode containsObj = getObjectOrBoolean("contains", node, false, location, result);
		if (containsObj != null) {
			Schema contains = getJsonSchema(containsObj, location, result);
			if (contains != null) {
				schema.setContains(contains);
			}
		}

		Map<String, Schema> properties = new LinkedHashMap<>();
		ObjectNode propertiesObj = getObject("properties", node, false, location, result);
		Schema property = null;

		Set<String> keys = getKeys(propertiesObj);
		for (String name : keys) {
			JsonNode propertyValue = propertiesObj.get(name);
            if (propertiesObj != null) {
                property = getJsonSchema(propertyValue, location, result);
                if (property != null) {
                    properties.put(name, property);
                }
            }
		}
		if (propertiesObj != null) {
			schema.setProperties(properties);
		}

		Map<String, Schema> patternProperties = new LinkedHashMap<>();
		ObjectNode patternPropertiesObj = getObject("patternProperties", node, false, location, result);
		Schema patternProperty = null;

		Set<String> patternKeys = getKeys(patternPropertiesObj);
		for (String name : patternKeys) {
			JsonNode propertyValue = patternPropertiesObj.get(name);
            if (patternPropertiesObj != null) {
                patternProperty = getJsonSchema(propertyValue, location, result);
                if (patternProperty != null) {
                    patternProperties.put(name, patternProperty);
                }
            }
		}
		if (patternPropertiesObj != null) {
			schema.setPatternProperties(patternProperties);
		}

		//const is a String
		value = getString("const", node, false, location, result);
		if (value != null) {
			schema.setConst(value);
		}

		value = getString("contentEncoding", node, false, location, result);
		if (value != null) {
			schema.setContentEncoding(value);
		}

		value = getString("contentMediaType", node, false, location, result);
		if (value != null) {
			schema.setContentMediaType(value);
		}

		ArrayNode examples = getArray("examples", node,false, location, result);
		List<Object> exampleList = new ArrayList<>();
		if (examples != null) {
			for (JsonNode item : examples) {
				exampleList.add(item);
			}
		}
		if(exampleList.size() > 0){
			schema.setExamples(exampleList);
		}

		value = getString("$anchor", node, false, location, result);
		if (value != null) {
			schema.set$anchor(value);
		}

		value = getString("$id", node, false, location, result);
		if (value != null) {
			schema.set$id(value);
		}

		value = getString("$schema", node, false, location, result);
		if (value != null) {
			schema.set$schema(value);
		}

		value = getString("$comment", node, false, location, result);
		if (value != null) {
			schema.set$comment(value);
		}

		Map<String, Object> extensions = getExtensions(node);
		if (extensions != null && extensions.size() > 0) {
			schema.setExtensions(extensions);
		}

		Set<String> schemaKeys = getKeys(node);
		Map<String, Set<String>> specKeys = KEYS.get("openapi31");
		for (String key : schemaKeys) {
			validateReservedKeywords(specKeys, key, location, result);
			if (!specKeys.get("SCHEMA_KEYS").contains(key) && !key.startsWith("x-")) {
				extensions.put(key, Json.mapper().convertValue(node.get(key), Object.class));
				schema.setExtensions(extensions);
			}
		}
		return schema;
	}

	public static class ParseResult {
		private boolean valid = true;
		private Map<Location, JsonNode> extra = new LinkedHashMap<>();
		private Map<Location, JsonNode> unsupported = new LinkedHashMap<>();
		private Map<Location, String> invalidType = new LinkedHashMap<>();
		private List<Location> missing = new ArrayList<>();
		private List<Location> warnings = new ArrayList<>();
		private List<Location> unique = new ArrayList<>();
		private List<Location> uniqueTags = new ArrayList<>();
		private boolean allowEmptyStrings = true;
        private List<Location> reserved = new ArrayList<>();
        private boolean validateInternalRefs;

        private boolean inferSchemaType = true;
		private boolean openapi31 = false;
		private boolean oaiAuthor = false;

		public boolean isInferSchemaType() {
			return inferSchemaType;
		}

		public void setInferSchemaType(boolean inferSchemaType) {
			this.inferSchemaType = inferSchemaType;
		}

		public ParseResult inferSchemaType(boolean inferSchemaType) {
			this.inferSchemaType = inferSchemaType;
			return this;
		}

		public ParseResult() {
		}

		public boolean isAllowEmptyStrings() {
			return this.allowEmptyStrings;
		}

		public void setAllowEmptyStrings(boolean allowEmptyStrings) {
			this.allowEmptyStrings = allowEmptyStrings;
		}

		public ParseResult allowEmptyStrings(boolean allowEmptyStrings) {
			this.allowEmptyStrings = allowEmptyStrings;
			return this;
		}

		public void unsupported(String location, String key, JsonNode value) {
			unsupported.put(new Location(location, key), value);
		}

		public void reserved(String location, String key) {
			reserved.add(new Location(location, key));
		}

		public void extra(String location, String key, JsonNode value) {
			extra.put(new Location(location, key), value);
		}

		public void missing(String location, String key) {
			missing.add(new Location(location, key));
		}

		public void warning(String location, String key) {
			warnings.add(new Location(location, key));
		}

		public void unique(String location, String key) {
			unique.add(new Location(location, key));

		}

		public void uniqueTags(String location, String key) {
			uniqueTags.add(new Location(location, key));
		}

		public void invalidType(String location, String key, String expectedType, JsonNode value) {
			invalidType.put(new Location(location, key), expectedType);
		}

		public void invalid() {
			this.valid = false;
		}

		public boolean isValid() {
			return this.valid;
		}

		public void setOpenapi31(boolean openapi31) {
			this.openapi31 = openapi31;
		}

		public ParseResult openapi31(boolean openapi31) {
			this.openapi31 = openapi31;
			return this;
		}
		public boolean isOpenapi31() {
			return this.openapi31;
		}

		public boolean isOaiAuthor() {
			return this.oaiAuthor;
		}

		public void setOaiAuthor(boolean oaiAuthor) {
			this.oaiAuthor = oaiAuthor;
		}

		public ParseResult oaiAuthor(boolean oaiAuthor) {
			this.oaiAuthor = oaiAuthor;
			return this;
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
			for (Location l : warnings) {
				String location = l.location.equals("") ? "" : l.location + ".";
				String message = location + l.key;
				messages.add(message);
			}
			for (Location l : unsupported.keySet()) {
				String location = l.location.equals("") ? "" : l.location + ".";
				String message = "attribute " + location + l.key + " is unsupported";
				messages.add(message);
			}
			for (Location l : unique) {
				String location = l.location.equals("") ? "" : l.location + ".";
				String message = "attribute " + location + l.key + " is repeated";
				messages.add(message);
			}
			for (Location l : uniqueTags) {
				String location = l.location.equals("") ? "" : l.location + ".";
				String message = "attribute " + location + l.key + " is repeated";
				messages.add(message);
			}
			for (Location l : reserved) {
				String location = l.location.equals("") ? "" : l.location + ".";
				String message = "attribute " + location + l.key + " is reserved by The OpenAPI Initiative";
				messages.add(message);
			}
			return messages;
		}

        public void setValidateInternalRefs(boolean validateInternalRefs) {
            this.validateInternalRefs = validateInternalRefs;
        }

        public boolean isValidateInternalRefs() {
            return validateInternalRefs;
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
