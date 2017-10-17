package io.swagger.parser.v2;

import io.swagger.oas.models.Components;
import io.swagger.oas.models.ExternalDocumentation;
import io.swagger.oas.models.OpenAPI;
import io.swagger.oas.models.Operation;
import io.swagger.oas.models.PathItem;
import io.swagger.oas.models.Paths;
import io.swagger.oas.models.headers.Header;
import io.swagger.oas.models.info.Contact;
import io.swagger.oas.models.info.Info;
import io.swagger.oas.models.info.License;
import io.swagger.oas.models.media.ArraySchema;
import io.swagger.oas.models.media.ComposedSchema;
import io.swagger.oas.models.media.Content;
import io.swagger.oas.models.media.Discriminator;
import io.swagger.oas.models.media.FileSchema;
import io.swagger.oas.models.media.MediaType;
import io.swagger.oas.models.media.Schema;
import io.swagger.oas.models.parameters.Parameter;
import io.swagger.oas.models.parameters.RequestBody;
import io.swagger.oas.models.responses.ApiResponse;
import io.swagger.oas.models.responses.ApiResponses;
import io.swagger.oas.models.security.OAuthFlow;
import io.swagger.oas.models.security.OAuthFlows;
import io.swagger.oas.models.security.Scopes;
import io.swagger.oas.models.security.SecurityScheme;
import io.swagger.oas.models.servers.Server;
import io.swagger.oas.models.tags.Tag;
import io.swagger.parser.extensions.SwaggerParserExtension;
import io.swagger.parser.models.AuthorizationValue;
import io.swagger.parser.models.ParseOptions;
import io.swagger.parser.models.SwaggerParseResult;
import org.apache.commons.lang3.StringUtils;
import v2.io.swagger.models.ArrayModel;
import v2.io.swagger.models.ComposedModel;
import v2.io.swagger.models.ExternalDocs;
import v2.io.swagger.models.Model;
import v2.io.swagger.models.ModelImpl;
import v2.io.swagger.models.Path;
import v2.io.swagger.models.RefModel;
import v2.io.swagger.models.RefPath;
import v2.io.swagger.models.RefResponse;
import v2.io.swagger.models.Response;
import v2.io.swagger.models.Scheme;
import v2.io.swagger.models.SecurityRequirement;
import v2.io.swagger.models.Swagger;
import v2.io.swagger.models.auth.ApiKeyAuthDefinition;
import v2.io.swagger.models.auth.OAuth2Definition;
import v2.io.swagger.models.auth.SecuritySchemeDefinition;
import v2.io.swagger.models.parameters.AbstractSerializableParameter;
import v2.io.swagger.models.parameters.BodyParameter;
import v2.io.swagger.models.parameters.RefParameter;
import v2.io.swagger.models.parameters.SerializableParameter;
import v2.io.swagger.models.properties.AbstractNumericProperty;
import v2.io.swagger.models.properties.ArrayProperty;
import v2.io.swagger.models.properties.MapProperty;
import v2.io.swagger.models.properties.ObjectProperty;
import v2.io.swagger.models.properties.Property;
import v2.io.swagger.models.properties.RefProperty;
import v2.io.swagger.parser.SwaggerResolver;
import v2.io.swagger.parser.util.SwaggerDeserializationResult;
import v2.io.swagger.util.Json;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SwaggerConverter implements SwaggerParserExtension {
    private List<String> globalConsumes = new ArrayList<>();
    private List<String> globalProduces = new ArrayList<>();

    @Override
    public SwaggerParseResult readLocation(String url, List<AuthorizationValue> auths, ParseOptions options) {
        boolean resolve = false;
        if (options != null) {
            resolve = options.isResolve();
        }

        SwaggerDeserializationResult result = new v2.io.swagger.parser.SwaggerParser().readWithInfo(url, convert(auths), resolve);

        return convert(result);
    }

    @Override
    public SwaggerParseResult readContents(String swaggerAsString, List<io.swagger.parser.models.AuthorizationValue> auth, ParseOptions options) {
        SwaggerDeserializationResult result = new v2.io.swagger.parser.SwaggerParser().readWithInfo(swaggerAsString);

        if (options != null) {
            if (options.isResolve()) {
                Swagger resolved = new SwaggerResolver(result.getSwagger(), convert(auth)).resolve();
                result.setSwagger(resolved);
            }
        }
        return convert(result);
    }

    public List<v2.io.swagger.models.auth.AuthorizationValue> convert(List<AuthorizationValue> auths) {
        List<v2.io.swagger.models.auth.AuthorizationValue> convertedAuth = new ArrayList<>();
        if (auths != null) {
            for (AuthorizationValue auth : auths) {
                v2.io.swagger.models.auth.AuthorizationValue v = new v2.io.swagger.models.auth.AuthorizationValue();
                v.setType(auth.getType());
                v.setValue(auth.getValue());
                v.setKeyName(auth.getKeyName());
            }
        }

        return convertedAuth;
    }

    public SwaggerParseResult convert(SwaggerDeserializationResult parse) {
        if (parse == null) {
            return null;
        }

        if (parse.getSwagger() == null) {
            return new SwaggerParseResult()
                    .messages(parse.getMessages());
        }

        OpenAPI openAPI = new OpenAPI();
        SwaggerParseResult output = new SwaggerParseResult();

        SwaggerInventory inventory = new SwaggerInventory().process(parse.getSwagger());

        Swagger swagger = parse.getSwagger();

        if (swagger.getExternalDocs() != null) {
            openAPI.setExternalDocs(convert(swagger.getExternalDocs()));
        }

        openAPI.setInfo(convert(swagger.getInfo()));

        openAPI.setServers(convert(swagger.getSchemes(), swagger.getHost(), swagger.getBasePath()));

        if (swagger.getTags() != null) {
            openAPI.setTags(convertTags(swagger.getTags()));
        }

        if (swagger.getConsumes() != null) {
            this.globalConsumes.addAll(swagger.getConsumes());
        }

        if (swagger.getProduces() != null) {
            this.globalProduces.addAll(swagger.getProduces());
        }

        if (swagger.getSecurity() != null && swagger.getSecurity().size() > 0) {
            openAPI.setSecurity(convertSecurityRequirements(swagger.getSecurity()));
        }

        List<Model> models = inventory.getModels();

        // TODO until we have the example object working correctly in v3 pojos...
        for (Model model : models) {
            if (model instanceof RefModel) {
                RefModel ref = (RefModel) model;
                if (ref.get$ref().indexOf("#/definitions") == 0) {
                    String updatedRef = "#/components/schemas" + ref.get$ref().substring("#/definitions".length());
                    ref.set$ref(updatedRef);
                }
            }
        }

        for (Property property : inventory.getProperties()) {
            if (property instanceof RefProperty) {
                RefProperty ref = (RefProperty) property;
                if (ref.get$ref().indexOf("#/definitions") == 0) {
                    String updatedRef = "#/components/schemas" + ref.get$ref().substring("#/definitions".length());
                    ref.set$ref(updatedRef);
                }
            }
        }

        Paths v3Paths = new Paths();
        for (String pathname : swagger.getPaths().keySet()) {
            v2.io.swagger.models.Path v2Path = swagger.getPath(pathname);
            PathItem v3Path = convert(v2Path);
            v3Paths.put(pathname, v3Path);
        }
        openAPI.setPaths(v3Paths);

        Components components = new Components();

        if (swagger.getParameters() != null) {
            swagger.getParameters().forEach((k, v) -> {
                if ("body".equals(v.getIn())) {
                    components.addRequestBodies(k, convertParameterToRequestBody(v));
                } else if ("formData".equals(v.getIn())) {
                    components.addRequestBodies(k, convertFormDataToRequestBody(v));
                } else {
                    components.addParameters(k, convert(v));
                }
            });
        }

        if (swagger.getResponses() != null) {
            swagger.getResponses().forEach((k, v) -> components.addResponses(k, convert(v)));
        }

        if (swagger.getDefinitions() != null) {
            for (String key : swagger.getDefinitions().keySet()) {
                Model model = swagger.getDefinitions().get(key);
                Schema schema = convert(model);

                components.addSchemas(key, schema);
            }
        }

        if (swagger.getSecurityDefinitions() != null) {
            swagger.getSecurityDefinitions().forEach((k, v) -> components.addSecuritySchemes(k, convert(v)));
        }

        openAPI.setComponents(components);

        output.setOpenAPI(openAPI);

        return output;
    }

    private List<io.swagger.oas.models.security.SecurityRequirement> convertSecurityRequirements(List<SecurityRequirement> security) {
        List<io.swagger.oas.models.security.SecurityRequirement> securityRequirements = new ArrayList<>();

        for (SecurityRequirement requirement : security) {
            io.swagger.oas.models.security.SecurityRequirement securityRequirement = new io.swagger.oas.models.security.SecurityRequirement();

            requirement.getRequirements().forEach((k, v) -> securityRequirement.addList(k, v));

            securityRequirements.add(securityRequirement);
        }

        return securityRequirements;
    }

    private List<io.swagger.oas.models.security.SecurityRequirement> convertSecurityRequirementsMap(List<Map<String, List<String>>> security) {
        List<io.swagger.oas.models.security.SecurityRequirement> securityRequirements = new ArrayList<>();

//        for (SecurityRequirement requirement : security) {
//            io.swagger.oas.models.security.SecurityRequirement securityRequirement = new io.swagger.oas.models.security.SecurityRequirement();
//
//            requirement.getRequirements().forEach((k,v) -> securityRequirement.addList(k, v));
//
//            securityRequirements.add(securityRequirement);
//        }

        for (Map<String, List<String>> map : security) {
            io.swagger.oas.models.security.SecurityRequirement securityRequirement = new io.swagger.oas.models.security.SecurityRequirement();

            map.forEach((k, v) -> securityRequirement.addList(k, v));

            securityRequirements.add(securityRequirement);
        }

        return securityRequirements;
    }

    private SecurityScheme convert(SecuritySchemeDefinition definition) {
        SecurityScheme securityScheme;

        switch (definition.getType()) {
            case "basic":
                securityScheme = createBasicSecurityScheme();
                break;
            case "apiKey":
                securityScheme = convertApiKeySecurityScheme(definition);
                break;
            case "oauth2":
                securityScheme = convertOauth2SecurityScheme(definition);
                break;
            default:
                securityScheme = new SecurityScheme();
        }

        securityScheme.setDescription(definition.getDescription());

        if (definition.getVendorExtensions() != null && definition.getVendorExtensions().size() > 0) {
            securityScheme.setExtensions(definition.getVendorExtensions());
        }

        return securityScheme;
    }

    private SecurityScheme convertOauth2SecurityScheme(SecuritySchemeDefinition definition) {
        SecurityScheme securityScheme = new SecurityScheme();
        OAuth2Definition oAuth2Definition = (OAuth2Definition) definition;
        OAuthFlows oAuthFlows = new OAuthFlows();
        OAuthFlow oAuthFlow = new OAuthFlow();

        securityScheme.setType(SecurityScheme.Type.OAUTH2);

        switch (oAuth2Definition.getFlow()) {
            case "implicit":
                oAuthFlow.setAuthorizationUrl(oAuth2Definition.getAuthorizationUrl());
                oAuthFlows.setImplicit(oAuthFlow);
                break;
            case "password":
                oAuthFlow.setTokenUrl(oAuth2Definition.getTokenUrl());
                oAuthFlows.setPassword(oAuthFlow);
                break;
            case "application":
                oAuthFlow.setTokenUrl(oAuth2Definition.getTokenUrl());
                oAuthFlows.setClientCredentials(oAuthFlow);
                break;
            case "accessCode":
                oAuthFlow.setAuthorizationUrl(oAuth2Definition.getAuthorizationUrl());
                oAuthFlow.setTokenUrl(oAuth2Definition.getTokenUrl());
                oAuthFlows.setAuthorizationCode(oAuthFlow);
                break;
        }

        Scopes scopes = new Scopes();
        oAuth2Definition.getScopes().forEach((k, v) -> scopes.addString(k, v));
        oAuthFlow.setScopes(scopes);

        securityScheme.setFlows(oAuthFlows);

        return securityScheme;
    }

    private SecurityScheme convertApiKeySecurityScheme(SecuritySchemeDefinition definition) {
        SecurityScheme securityScheme = new SecurityScheme();
        ApiKeyAuthDefinition apiKeyAuthDefinition = (ApiKeyAuthDefinition) definition;

        securityScheme.setType(SecurityScheme.Type.APIKEY);
        securityScheme.setName(apiKeyAuthDefinition.getName());
        securityScheme.setIn(SecurityScheme.In.valueOf(apiKeyAuthDefinition.getIn().toString()));

        return securityScheme;
    }

    private SecurityScheme createBasicSecurityScheme() {
        SecurityScheme securityScheme = new SecurityScheme();

        securityScheme.setType(SecurityScheme.Type.HTTP);
        securityScheme.setScheme("basic");

        return securityScheme;
    }

    private List<Tag> convertTags(List<v2.io.swagger.models.Tag> v2tags) {
        List<Tag> v3tags = new ArrayList<>();

        for (v2.io.swagger.models.Tag v2tag : v2tags) {
            Tag v3tag = new Tag();

            v3tag.setDescription(v2tag.getDescription());
            v3tag.setName(v2tag.getName());

            if (v2tag.getExternalDocs() != null) {
                v3tag.setExternalDocs(convert(v2tag.getExternalDocs()));
            }

            if (v2tag.getVendorExtensions() != null && v2tag.getVendorExtensions().size() > 0) {
                v3tag.setExtensions(v2tag.getVendorExtensions());
            }

            v3tags.add(v3tag);
        }

        return v3tags;
    }

    private ExternalDocumentation convert(ExternalDocs externalDocs) {
        ExternalDocumentation externalDocumentation = new ExternalDocumentation();

        externalDocumentation.setUrl(externalDocs.getUrl());
        externalDocumentation.setDescription(externalDocs.getDescription());
        if (externalDocs.getVendorExtensions() != null && externalDocs.getVendorExtensions().size() > 0) {
            externalDocumentation.setExtensions(externalDocs.getVendorExtensions());
        }

        return externalDocumentation;
    }

    private List<Server> convert(List<Scheme> schemes, String host, String basePath) {
        List<Server> servers = new ArrayList<>();
        String baseUrl;

        if (StringUtils.isNotEmpty(basePath)) {
            baseUrl = basePath;
        } else {
            baseUrl = "/";
        }

        if (StringUtils.isNotEmpty(host)) {
            baseUrl = host + baseUrl;
        }

        if (!StringUtils.startsWith(baseUrl, "/") && schemes != null && !schemes.isEmpty()) {
            for (Scheme scheme : schemes) {
                Server server = new Server();
                server.setUrl(scheme.toValue() + "://" + baseUrl);

                servers.add(server);
            }
        } else {
            if (!"/".equals(baseUrl)) {
                Server server = new Server();
                server.setUrl(baseUrl);

                servers.add(server);
            } else {
                return null;
            }
        }

        return servers;
    }

    public Info convert(v2.io.swagger.models.Info v2Info) {
        Info info = new Info();

        info.setContact(convert(v2Info.getContact()));
        info.setDescription(v2Info.getDescription());
        info.setLicense(convert(v2Info.getLicense()));
        info.setTermsOfService(v2Info.getTermsOfService());
        info.setTitle(v2Info.getTitle());
        info.setVersion(v2Info.getVersion());

        if (v2Info.getVendorExtensions() != null && v2Info.getVendorExtensions().size() > 0) {
            info.setExtensions(v2Info.getVendorExtensions());
        }

        return info;
    }

    private License convert(v2.io.swagger.models.License v2License) {
        if (v2License == null) {
            return null;
        }

        License license = new License();

        if (v2License.getVendorExtensions() != null && v2License.getVendorExtensions().size() > 0) {
            license.setExtensions(v2License.getVendorExtensions());
        }
        license.setName(v2License.getName());
        license.setUrl(v2License.getUrl());

        return license;
    }

    public Contact convert(v2.io.swagger.models.Contact v2Contact) {
        if (v2Contact == null) {
            return null;
        }

        Contact contact = new Contact();

        contact.setUrl(v2Contact.getUrl());
        contact.setName(v2Contact.getName());
        contact.setEmail(v2Contact.getEmail());

        return contact;
    }

    public PathItem convert(Path v2Path) {
        PathItem v3Path = new PathItem();

        if (v2Path instanceof RefPath) {

            v3Path.set$ref(((RefPath) v2Path).get$ref());
        } else {

            if (v2Path.getParameters() != null) {
                for (v2.io.swagger.models.parameters.Parameter param : v2Path.getParameters()) {
                    v3Path.addParametersItem(convert(param));
                }
            }

            v2.io.swagger.models.Operation v2Operation;

            v2Operation = v2Path.getGet();
            if (v2Operation != null) {
                v3Path.setGet(convert(v2Operation));
            }
            v2Operation = v2Path.getPut();
            if (v2Operation != null) {
                v3Path.setPut(convert(v2Operation));
            }
            v2Operation = v2Path.getPost();
            if (v2Operation != null) {
                v3Path.setPost(convert(v2Operation));
            }
            v2Operation = v2Path.getPatch();
            if (v2Operation != null) {
                v3Path.setPatch(convert(v2Operation));
            }
            v2Operation = v2Path.getDelete();
            if (v2Operation != null) {
                v3Path.setDelete(convert(v2Operation));
            }

            if (v2Path.getVendorExtensions() != null && v2Path.getVendorExtensions().size() > 0) {
                v3Path.setExtensions(v2Path.getVendorExtensions());
            }
        }

        return v3Path;
    }

    public Operation convert(v2.io.swagger.models.Operation v2Operation) {
        Operation operation = new Operation();
        if (StringUtils.isNotBlank(v2Operation.getDescription())) {
            operation.setDescription(v2Operation.getDescription());
        }
        if (StringUtils.isNotBlank(v2Operation.getSummary())) {
            operation.setSummary(v2Operation.getSummary());
        }
        operation.setDeprecated(v2Operation.isDeprecated());
        operation.setOperationId(v2Operation.getOperationId());

        operation.setTags(v2Operation.getTags());

        if (v2Operation.getParameters() != null) {
            List<v2.io.swagger.models.parameters.Parameter> formParams = new ArrayList<>();
            for (v2.io.swagger.models.parameters.Parameter param : v2Operation.getParameters()) {

                if ("formData".equals(param.getIn())) {
                    formParams.add(param);
                } else if ("body".equals(param.getIn())) {
                    operation.setRequestBody(convertParameterToRequestBody(param, v2Operation.getConsumes()));
                } else {
                    operation.addParametersItem(convert(param));
                }
            }

            if (formParams.size() > 0) {
                RequestBody body = convertFormDataToRequestBody(formParams, v2Operation.getConsumes());
                operation.requestBody(body);
            }
        }

        if (v2Operation.getResponses() != null) {
            for (String responseCode : v2Operation.getResponses().keySet()) {
                v2.io.swagger.models.Response v2Response = v2Operation.getResponses().get(responseCode);
                ApiResponse response = convert(v2Response, v2Operation.getProduces());
                ApiResponses responses = operation.getResponses();
                if (responses == null) {
                    responses = new ApiResponses();
                }

                operation.responses(responses.addApiResponse(responseCode, response));
            }
        }

        if (v2Operation.getExternalDocs() != null) {
            operation.setExternalDocs(convert(v2Operation.getExternalDocs()));
        }

        if (v2Operation.getSecurity() != null && v2Operation.getSecurity().size() > 0) {
            operation.setSecurity(convertSecurityRequirementsMap(v2Operation.getSecurity()));
        }

        return operation;
    }

    private RequestBody convertFormDataToRequestBody(v2.io.swagger.models.parameters.Parameter formParam) {
        return convertFormDataToRequestBody(Arrays.asList(formParam), null);
    }

    private RequestBody convertFormDataToRequestBody(List<v2.io.swagger.models.parameters.Parameter> formParams, List<String> consumes) {
        RequestBody body = new RequestBody();

        Schema formSchema = new Schema();

        for (v2.io.swagger.models.parameters.Parameter param : formParams) {
            SerializableParameter sp = (SerializableParameter) param;

            Schema schema;
            if ("file".equals(sp.getType())) {
                schema = new FileSchema();
            } else if ("array".equals(sp.getType())) {
                ArraySchema as = new ArraySchema();
                if (sp.getItems() != null) {
                    as.setItems(convert(sp.getItems()));
                }
                schema = as;
            } else {
                schema = new Schema();
                schema.setType(sp.getType());
                schema.setFormat(sp.getFormat());
            }

            schema.setDescription(sp.getDescription());
            schema.setReadOnly(sp.isReadOnly());
            schema.setEnum(sp.getEnum());

            if (sp.getMaxItems() != null) {
                schema.setMaxItems(sp.getMaxItems());
            }
            if (sp.getMinItems() != null) {
                schema.setMinItems(sp.getMinItems());
            }
            if (sp.isUniqueItems() != null) {
                schema.setUniqueItems(sp.isUniqueItems());
            }

            if (sp.getVendorExtensions() != null && sp.getVendorExtensions().size() > 0) {
                schema.setExtensions(sp.getVendorExtensions());
            }

            schema.setMaximum(sp.getMaximum());
            schema.setExclusiveMaximum(sp.isExclusiveMaximum());
            schema.setMinimum(sp.getMinimum());
            schema.setExclusiveMinimum(sp.isExclusiveMinimum());
            schema.setMinLength(sp.getMinLength());
            schema.setMaxLength(sp.getMaxLength());

            Object exampleExtension = sp.getVendorExtensions().get("x-example");
            if (exampleExtension != null) {
                schema.setExample(exampleExtension);
            }

            if (sp.getMultipleOf() != null) {
                schema.setMultipleOf(new BigDecimal(sp.getMultipleOf().toString()));
            }

            schema.setPattern(sp.getPattern());

            if (sp.getVendorExtensions() != null && sp.getVendorExtensions().size() > 0) {
                schema.setExtensions(sp.getVendorExtensions());
            }

            if (sp instanceof AbstractSerializableParameter) {
                AbstractSerializableParameter ap = (AbstractSerializableParameter) sp;
                schema.setDefault(ap.getDefault());
            }

            formSchema.addProperties(param.getName(), schema);
        }

        List<String> mediaTypes = new ArrayList<>(globalConsumes);
        if (consumes != null && consumes.size() > 0) {
            mediaTypes.clear();
            mediaTypes.addAll(consumes);
        }

        // Assume multipart/form-data if nothing is specified
        if (mediaTypes.size() == 0) {
            mediaTypes.add("multipart/form-data");
        }

        Content content = new Content();
        for (String type : mediaTypes) {
            content.addMediaType(type, new MediaType().schema(formSchema));
        }
        body.content(content);
        return body;
    }

    private RequestBody convertParameterToRequestBody(v2.io.swagger.models.parameters.Parameter param) {
        return convertParameterToRequestBody(param, null);
    }

    private RequestBody convertParameterToRequestBody(v2.io.swagger.models.parameters.Parameter param, List<String> consumes) {
        RequestBody body = new RequestBody();
        BodyParameter bp = (BodyParameter) param;

        List<String> mediaTypes = new ArrayList<>(globalConsumes);
        if (consumes != null && consumes.size() > 0) {
            mediaTypes.clear();
            mediaTypes.addAll(consumes);
        }

        if (mediaTypes.size() == 0) {
            mediaTypes.add("*/*");
        }

        if (StringUtils.isNotBlank(param.getDescription())) {
            body.description(param.getDescription());
        }
        body.required(param.getRequired());

        Content content = new Content();
        for (String type : mediaTypes) {
            content.addMediaType(type,
                    new MediaType().schema(
                            convert(bp.getSchema())));
            if (StringUtils.isNotBlank(bp.getDescription())) {
                body.setDescription(bp.getDescription());
            }
        }
        body.content(content);
        return body;
    }

    public ApiResponse convert(Response response) {
        return convert(response, null);
    }

    public ApiResponse convert(v2.io.swagger.models.Response v2Response, List<String> produces) {
        ApiResponse response = new ApiResponse();
        Content content = new Content();

        if (v2Response instanceof RefResponse) {

            RefResponse ref = (RefResponse) v2Response;
            if (ref.get$ref().indexOf("#/responses") == 0) {
                String updatedRef = "#/components/responses" + ref.get$ref().substring("#/responses".length());
                ref.set$ref(updatedRef);
            }

            response.set$ref(ref.get$ref());
        } else {

            List<String> mediaTypes = new ArrayList<>(globalProduces);
            if (produces != null) {
                // use this for media type
                mediaTypes.clear();
                mediaTypes.addAll(produces);
            }

            if (mediaTypes.size() == 0) {
                mediaTypes.add("*/*");
            }

            response.setDescription(v2Response.getDescription());

            if (v2Response.getSchema() != null) {
                Schema schema = convertFileSchema(convert(v2Response.getSchema()));
                for (String type : mediaTypes) {
                    // TODO: examples
                    content.addMediaType(type, new MediaType().schema(schema));
                }
                response.content(content);
            }

            if (v2Response.getHeaders() != null && v2Response.getHeaders().size() > 0) {
                response.setHeaders(convertHeaders(v2Response.getHeaders()));
            }
        }

        return response;
    }

    private Schema convertFileSchema(Schema schema) {
        if ("file".equals(schema.getType())) {
            schema.setType("string");
            schema.setFormat("binary");
        }

        return schema;
    }

    private Map<String, Header> convertHeaders(Map<String, Property> headers) {
        Map<String, Header> result = new HashMap<>();

        headers.forEach((k, v) -> {
            result.put(k, convertHeader(v));
        });

        return result;
    }

    private Header convertHeader(Property property) {
        Schema schema = convert(property);
        schema.setDescription(null);

        Header header = new Header();
        header.setDescription(property.getDescription());
        header.setSchema(schema);

        return header;
    }

    private Schema convert(Property schema) {
        Schema result;

        if (schema instanceof RefProperty) {
            RefProperty ref = (RefProperty) schema;
            if (ref.get$ref().indexOf("#/definitions") == 0) {
                String updatedRef = "#/components/schemas" + ref.get$ref().substring("#/definitions".length());
                ref.set$ref(updatedRef);
            }

            result = new Schema();
            result.set$ref(ref.get$ref());
        } else if (schema instanceof ArrayProperty) {
            ArraySchema arraySchema = Json.mapper().convertValue(schema, ArraySchema.class);

            Property items = ((ArrayProperty) schema).getItems();
            Schema itemsSchema = convert(items);
            arraySchema.setItems(itemsSchema);

            if (((ArrayProperty) schema).getMaxItems() != null) {
                arraySchema.setMaxItems(((ArrayProperty) schema).getMaxItems());
            }
            if (((ArrayProperty) schema).getMinItems() != null) {
                arraySchema.setMinItems(((ArrayProperty) schema).getMinItems());
            }
            if (((ArrayProperty) schema).getUniqueItems() != null && ((ArrayProperty) schema).getUniqueItems()) {
                arraySchema.setUniqueItems(((ArrayProperty) schema).getUniqueItems());
            }

            result = arraySchema;

        } else {

            result = Json.mapper().convertValue(schema, Schema.class);
            result.setExample(schema.getExample());

            if ("object".equals(schema.getType()) && (result.getProperties() != null) && (result.getProperties().size() > 0)) {
                Map<String, Schema> properties = new HashMap<>();

                ((ObjectProperty) schema).getProperties().forEach((k, v) -> properties.put(k, convert(v)));

                result.setProperties(properties);
            }

            if (schema instanceof MapProperty) {
                MapProperty map = (MapProperty) schema;

                result.setAdditionalProperties(convert(map.getAdditionalProperties()));
                result.setMinProperties(map.getMinProperties());
                result.setMaxProperties(map.getMaxProperties());
            }

            if (schema instanceof AbstractNumericProperty) {
                AbstractNumericProperty np = (AbstractNumericProperty) schema;

                result.setExclusiveMaximum(np.getExclusiveMaximum());
                result.setExclusiveMinimum(np.getExclusiveMinimum());
            }
        }

        return result;
    }

    public Parameter convert(v2.io.swagger.models.parameters.Parameter v2Parameter) {
        Parameter v3Parameter = new Parameter();

        if (StringUtils.isNotBlank(v2Parameter.getDescription())) {
            v3Parameter.setDescription(v2Parameter.getDescription());
        }
        v3Parameter.setAllowEmptyValue(v2Parameter.getAllowEmptyValue());
        v3Parameter.setIn(v2Parameter.getIn());
        v3Parameter.setName(v2Parameter.getName());

        Object exampleExtension = v2Parameter.getVendorExtensions().get("x-example");
        if (exampleExtension != null) {
            v3Parameter.setExample(exampleExtension);
        }

        Schema schema = null;

        if (v2Parameter instanceof RefParameter) {

            RefParameter ref = (RefParameter) v2Parameter;
            if (ref.get$ref().indexOf("#/parameters") == 0) {

                String updatedRef = "#/components/parameters" + ref.get$ref().substring("#/parameters".length());
                ref.set$ref(updatedRef);
            }

            v3Parameter.set$ref(ref.get$ref());
        } else if (v2Parameter instanceof SerializableParameter) {
            SerializableParameter sp = (SerializableParameter) v2Parameter;

            if ("array".equals(sp.getType())) {
                ArraySchema a = new ArraySchema();
                // TODO: convert arrays to proper template format

                String cf = sp.getCollectionFormat();

                if (StringUtils.isEmpty(cf)) {
                    cf = "csv";
                }

                switch (cf) {
                    case "ssv":
                        if ("query".equals(v2Parameter.getIn())) {
                            v3Parameter.setStyle(Parameter.StyleEnum.SPACEDELIMITED);
                        }
                        break;
                    case "pipes":
                        if ("query".equals(v2Parameter.getIn())) {
                            v3Parameter.setStyle((Parameter.StyleEnum.PIPEDELIMITED));
                        }
                        break;
                    case "tsv":
                        break;
                    case "multi":
                        break;
                    case "csv":
                    default:
                        if ("query".equals(v2Parameter.getIn())) {
                            v3Parameter.setExplode(false);
                        }
                }

                Property items = sp.getItems();
                Schema itemsSchema = convert(items);
                a.setItems(itemsSchema);

                if (sp.getMaxItems() != null) {
                    a.setMaxItems(sp.getMaxItems());
                }
                if (sp.getMinItems() != null) {
                    a.setMinItems(sp.getMinItems());
                }
                if (sp.isUniqueItems() != null) {
                    a.setUniqueItems(sp.isUniqueItems());
                }

                schema = a;
            } else {
                schema = new Schema();
                schema.setType(sp.getType());
                schema.setFormat(sp.getFormat());

                if (sp.getVendorExtensions() != null && sp.getVendorExtensions().size() > 0) {
                    schema.setExtensions(sp.getVendorExtensions());
                }
                if (sp.getEnum() != null) {
                    for (String e : sp.getEnum()) {
                        schema.addEnumItemObject(e);
                    }
                }

                schema.setMaximum(sp.getMaximum());
                schema.setExclusiveMaximum(sp.isExclusiveMaximum());
                schema.setMinimum(sp.getMinimum());
                schema.setExclusiveMinimum(sp.isExclusiveMinimum());
                schema.setMinLength(sp.getMinLength());
                schema.setMaxLength(sp.getMaxLength());
                if (sp.getMultipleOf() != null) {
                    schema.setMultipleOf(new BigDecimal(sp.getMultipleOf().toString()));
                }
                schema.setPattern(sp.getPattern());
            }

            if (sp.getVendorExtensions() != null && sp.getVendorExtensions().size() > 0) {
                schema.setExtensions(sp.getVendorExtensions());
            }

            if (sp instanceof AbstractSerializableParameter) {
                AbstractSerializableParameter ap = (AbstractSerializableParameter) sp;
                schema.setDefault(ap.getDefault());
            }
        }

        if (v2Parameter.getRequired()) {
            v3Parameter.setRequired(v2Parameter.getRequired());
        }
        v3Parameter.setSchema(schema);
        if (v2Parameter.getVendorExtensions() != null && v2Parameter.getVendorExtensions().size() > 0) {
            v3Parameter.setExtensions(v2Parameter.getVendorExtensions());
        }
        return v3Parameter;
    }

    public Schema convert(v2.io.swagger.models.Model v2Model) {
        Schema result;

        if (v2Model instanceof ArrayModel) {
            ArraySchema arraySchema = Json.mapper().convertValue(v2Model, ArraySchema.class);

            arraySchema.setItems(convert(((ArrayModel) v2Model).getItems()));

            result = arraySchema;
        } else if (v2Model instanceof ComposedModel) {
            ComposedModel composedModel = (ComposedModel) v2Model;

            ComposedSchema composed = Json.mapper().convertValue(v2Model, ComposedSchema.class);

            composed.setAllOf(composedModel.getAllOf().stream().map(this::convert).collect(Collectors.toList()));

            result = composed;
        } else {
            String v2discriminator = null;

            if (v2Model instanceof ModelImpl) {
                ModelImpl model = (ModelImpl) v2Model;

                v2discriminator = model.getDiscriminator();
                model.setDiscriminator(null);
            }

            result = Json.mapper().convertValue(v2Model, Schema.class);

            if ((v2Model.getProperties() != null) && (v2Model.getProperties().size() > 0)) {
                Map<String, Property> properties = v2Model.getProperties();

                properties.forEach((k, v) -> {
                    result.addProperties(k, convert(v));
                });

            }

            if (v2Model instanceof ModelImpl) {
                ModelImpl model = (ModelImpl) v2Model;

                if (model.getAdditionalProperties() != null) {
                    result.setAdditionalProperties(convert(model.getAdditionalProperties()));
                }
            }

            if (v2discriminator != null) {
                Discriminator discriminator = new Discriminator();

                discriminator.setPropertyName(v2discriminator);
                result.setDiscriminator(discriminator);
            }
        }

        return result;
    }
}
