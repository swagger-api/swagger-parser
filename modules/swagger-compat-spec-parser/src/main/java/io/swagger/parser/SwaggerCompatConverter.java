package io.swagger.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.models.ArrayModel;
import io.swagger.models.AuthorizationScope;
import io.swagger.models.Contact;
import io.swagger.models.Info;
import io.swagger.models.License;
import io.swagger.models.Method;
import io.swagger.models.Model;
import io.swagger.models.ModelImpl;
import io.swagger.models.Operation;
import io.swagger.models.ParamType;
import io.swagger.models.PassAs;
import io.swagger.models.Path;
import io.swagger.models.RefModel;
import io.swagger.models.Response;
import io.swagger.models.Scheme;
import io.swagger.models.Swagger;
import io.swagger.models.SwaggerVersion;
import io.swagger.models.apideclaration.Api;
import io.swagger.models.apideclaration.ApiDeclaration;
import io.swagger.models.apideclaration.ExtendedTypedObject;
import io.swagger.models.apideclaration.Items;
import io.swagger.models.apideclaration.ModelProperty;
import io.swagger.models.apideclaration.ResponseMessage;
import io.swagger.models.auth.ApiKeyAuthDefinition;
import io.swagger.models.auth.AuthorizationValue;
import io.swagger.models.auth.BasicAuthDefinition;
import io.swagger.models.auth.In;
import io.swagger.models.auth.OAuth2Definition;
import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.parameters.FormParameter;
import io.swagger.models.parameters.HeaderParameter;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.parameters.PathParameter;
import io.swagger.models.parameters.QueryParameter;
import io.swagger.models.parameters.SerializableParameter;
import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.PropertyBuilder;
import io.swagger.models.properties.RefProperty;
import io.swagger.models.properties.StringProperty;
import io.swagger.models.properties.UntypedProperty;
import io.swagger.models.refs.RefFormat;
import io.swagger.models.resourcelisting.ApiInfo;
import io.swagger.models.resourcelisting.ApiKeyAuthorization;
import io.swagger.models.resourcelisting.ApiListingReference;
import io.swagger.models.resourcelisting.Authorization;
import io.swagger.models.resourcelisting.AuthorizationCodeGrant;
import io.swagger.models.resourcelisting.BasicAuthorization;
import io.swagger.models.resourcelisting.ImplicitGrant;
import io.swagger.models.resourcelisting.OAuth2Authorization;
import io.swagger.models.resourcelisting.ResourceListing;
import io.swagger.parser.util.ClasspathHelper;
import io.swagger.parser.util.RemoteUrl;
import io.swagger.parser.util.SwaggerDeserializationResult;
import io.swagger.report.MessageBuilder;
import io.swagger.transform.migrate.ApiDeclarationMigrator;
import io.swagger.transform.migrate.ResourceListingMigrator;
import io.swagger.util.Json;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// legacy models

public class SwaggerCompatConverter implements SwaggerParserExtension {
    static Logger LOGGER = LoggerFactory.getLogger(SwaggerCompatConverter.class);

    @Override
    public SwaggerDeserializationResult readWithInfo(JsonNode node) {
        // not implemented
        return null;
    }

    @Override
    public SwaggerDeserializationResult readWithInfo(String location, List<AuthorizationValue> auths) {
        SwaggerDeserializationResult result = new SwaggerDeserializationResult();
        try {
            Swagger swagger = read(location, auths);
            if(swagger != null) {
                result.setSwagger(swagger);
                JsonNode jsonNode = Json.mapper().convertValue(swagger, JsonNode.class);
                return new Swagger20Parser().readWithInfo(jsonNode);
            }
        }
        catch (IOException e) {
            // TODO
        }
        return result;
    }

    @Override
    public Swagger read(JsonNode node) throws IOException {
        // TODO
        return null;
    }

    public Swagger read(String input) throws IOException {
        return read(input, null);
    }

    @Override
    public Swagger read(String input, List<AuthorizationValue> auths) throws IOException {
        Swagger output = null;
        MessageBuilder migrationMessages = new MessageBuilder();
        SwaggerLegacyParser swaggerParser = new SwaggerLegacyParser();
        ResourceListing resourceListing = null;
        resourceListing = readResourceListing(input, migrationMessages, auths);

        List<ApiDeclaration> apis = new ArrayList<ApiDeclaration>();

        if (resourceListing != null) {
            List<ApiListingReference> refs = resourceListing.getApis();
            boolean readAsSingleFile = false;
            if (refs != null) {
                for (ApiListingReference ref : refs) {
                    ApiDeclaration apiDeclaration = null;
                    JsonNode node = ref.getExtraFields();
                    JsonNode operations = node.get("operations");
                    if (operations != null) {
                        if (!readAsSingleFile) {
                            // this is a single-file swagger definition
                            apiDeclaration = readDeclaration(input, migrationMessages, auths);
                            readAsSingleFile = true; // avoid doing this again
                        }
                    } else {
                        String location = null;
                        if (input.startsWith("http")) {
                            // look up as url
                            String pathLocation = ref.getPath();
                            if (pathLocation.startsWith("http")) {
                                // use as absolute url
                                location = pathLocation;
                            } else {
                                if (pathLocation.startsWith("/")) {
                                    // handle 1.1 specs
                                    if(resourceListing.getSwaggerVersion().equals(SwaggerVersion.V1_1) &&
                                            resourceListing.getExtraFields().get("basePath") != null) {
                                        String basePath = resourceListing.getExtraFields().get("basePath").textValue();
                                        location = basePath + pathLocation;
                                    }
                                    else {
                                        location = input + pathLocation;
                                    }
                                } else {
                                    location = input + "/" + pathLocation;
                                }
                            }
                        } else {
                            // file system
                            File fileLocation = new File(input);
                            if (ref.getPath().startsWith("/")) {
                                location = fileLocation.getParent() + ref.getPath();
                            } else {
                                location = fileLocation.getParent() + File.separator + ref.getPath();
                            }
                        }
                        if (location.indexOf(".{format}") != -1) {
                            location = location.replaceAll("\\.\\{format\\}", ".json");
                        }
                        apiDeclaration = readDeclaration(location, migrationMessages, auths);
                    }
                    if (apiDeclaration != null) {
                        apis.add(apiDeclaration);
                    }
                }
            }
            output = convert(resourceListing, apis);
        }
        return output;
    }

    public ResourceListing readResourceListing(String input, MessageBuilder messages, List<AuthorizationValue> auths) {
        ResourceListing output = null;
        JsonNode jsonNode = null;
        try {
            if (input.startsWith("http")) {
                String json = RemoteUrl.urlToString(input, auths);
                jsonNode = Json.mapper().readTree(json);
            } else {
                final String fileScheme = "file:";
                java.nio.file.Path path;
                if (input.toLowerCase().startsWith(fileScheme)) {
                    path = Paths.get(URI.create(input));
                } else {
                    path = Paths.get(input);
                }
                String json;
                if (Files.exists(path)) {
                    json= FileUtils.readFileToString(path.toFile(), "UTF-8");
                } else {
                    json = ClasspathHelper.loadFileFromClasspath(input  );
                }

                jsonNode = Json.mapper().readTree(json);

            }
            if (jsonNode == null || jsonNode.get("swaggerVersion") == null) {
                return null;
            }
            ResourceListingMigrator migrator = new ResourceListingMigrator();
            JsonNode transformed = migrator.migrate(messages, jsonNode);
            output = Json.mapper().convertValue(transformed, ResourceListing.class);
        } catch (java.lang.IllegalArgumentException e) {
            return null;
        } catch (Exception e) {
            LOGGER.error("failed to read resource listing", e);
        }
        return output;
    }

    public Model convertModel(io.swagger.models.apideclaration.Model model) {
        ModelImpl output = new ModelImpl();
        output.setName(model.getId());
        output.setDescription(model.getDescription());
        output.setDiscriminator(model.getDiscriminator());
        if (model.getRequired() != null) {
            output.setRequired(model.getRequired());
        }
        for (String key : model.getProperties().keySet()) {
            Property prop = convertProperty(model.getProperties().get(key));
            if (prop != null) {
                output.addProperty(key, prop);
            }
        }
        return output;
    }

    public Property convertProperty(ModelProperty property) {
        Property output = null;
        output = propertyFromTypedObject(property);
        output.setDescription(property.getDescription());
        return output;
    }

    public Parameter convertParameter(io.swagger.models.apideclaration.Parameter param) {
        Parameter output = null;
        List<String> _enum = param.getEnumValues();

        if (ParamType.PATH.equals(param.getParamType())) {
            PathParameter p = new PathParameter();
            p.setDefaultValue(param.getDefaultValue());
            p.setEnum(_enum);
            output = p;
        } else if (ParamType.QUERY.equals(param.getParamType())) {
            QueryParameter p = new QueryParameter();
            p.setDefaultValue(param.getDefaultValue());
            p.setEnum(_enum);
            output = p;
        } else if (ParamType.HEADER.equals(param.getParamType())) {
            HeaderParameter p = new HeaderParameter();
            p.setDefaultValue(param.getDefaultValue());
            p.setEnum(_enum);
            output = p;
        } else if (ParamType.BODY.equals(param.getParamType())) {
            BodyParameter p = new BodyParameter();
            output = p;
        } else if (ParamType.FORM.equals(param.getParamType())) {
            FormParameter p = new FormParameter();
            p.setDefaultValue(param.getDefaultValue());
            p.setEnum(_enum);
            output = p;
        }

        output.setName(param.getName());
        output.setDescription(param.getDescription());
        if (param.getRequired() != null) {
            output.setRequired(param.getRequired());
        }

        Property property = null;
        String type = param.getType() == null ? null : param.getType().toString();
        String format = param.getFormat() == null ? null : param.getFormat().toString();

        if (null == type) {
            LOGGER.warn("Empty type in Param: " + param);
        }

        if (output instanceof BodyParameter) {
            BodyParameter bp = (BodyParameter) output;
            bp.setSchema(modelFromExtendedTypedObject(param));
        } else if (output instanceof SerializableParameter) {
            SerializableParameter sp = (SerializableParameter) output;
            Property p = null;
            if (param.getAllowMultiple() != null && param.getAllowMultiple() == true) {
                ArrayProperty arrayProperty = new ArrayProperty();
                Property innerType = PropertyBuilder.build(type, format, null);
                arrayProperty.setItems(innerType);
                p = arrayProperty;
            } else {
                p = propertyFromTypedObject(param);
                if (p == null) {
                    LOGGER.warn(String.format(
                            "WARNING! No property detected for parameter '%s' (%s)! Falling back to string!",
                            param.getName(), param.getParamType()));
                    p = new StringProperty();
                }
            }
            if (p instanceof ArrayProperty) {
                ArrayProperty ap = (ArrayProperty) p;
                sp.setType("array");
                sp.setCollectionFormat("csv");
                sp.setItems(ap.getItems());
            } else {
                sp.setType(p.getType());
                sp.setFormat(p.getFormat());
            }
        }
        // all path parameters are required
        if (output instanceof PathParameter) {
            ((PathParameter) output).setRequired(true);
        }
        return output;
    }

    public Model modelFromExtendedTypedObject(ExtendedTypedObject obj) {
        String type = obj.getType() == null ? null : obj.getType().toString();
        String format = obj.getFormat() == null ? null : obj.getFormat().toString();

        Model output = null;
        if (obj.getRef() != null) {
            output = new RefModel().asDefault(obj.getRef());
        } else {
            if ("array".equals(type)) {
                ArrayModel am = new ArrayModel();
                Items items = obj.getItems();
                type = items.getType() == null ? null : items.getType().toString();
                format = items.getFormat() == null ? null : items.getFormat().toString();

                Property innerType = PropertyBuilder.build(type, format, null);
                if (innerType != null) {
                    am.setItems(innerType);
                }
                if (items.getRef() != null) {
                    am.setItems(new RefProperty(items.getRef(),RefFormat.INTERNAL));
                } else {
                    am.setItems(new RefProperty(type,RefFormat.INTERNAL));
                }
                output = am;
            } else {
                Property input = PropertyBuilder.build(type, format, null);
                if (input != null) {
                	output = PropertyBuilder.toModel(input);
                } else if (!"void".equals(type)) {
                    //use ref model
                    output = new RefModel().asDefault(type);
                }
            }
        }
        return output;
    }

    public Property propertyFromTypedObject(ExtendedTypedObject obj) {
        String type = obj.getType() == null ? null : obj.getType().toString();
        String format = obj.getFormat() == null ? null : obj.getFormat().toString();

        Property output = null;
        if ("array".equals(type)) {
            ArrayProperty am = new ArrayProperty();
            Items items = obj.getItems();
            if (items == null) {
                LOGGER.error("Error! Missing array type for property!  Assuming `object` -- please fix your spec");
                items = new Items();
                items.setType("object");
            }
            type = items.getType() == null ? null : items.getType().toString();
            format = items.getFormat() == null ? null : items.getFormat().toString();

            Map<PropertyBuilder.PropertyId, Object> args = new HashMap<>();
            if(items.getExtraFields().get("enum") != null && items.getExtraFields().get("enum").isArray()) {
                ArrayNode an = (ArrayNode)items.getExtraFields().get("enum");

                List<String> enumValues = new ArrayList<>();
                for(JsonNode jn : an) {
                    enumValues.add(jn.textValue());
                }
                args.put(PropertyBuilder.PropertyId.ENUM, enumValues);
            }
            Property innerType = PropertyBuilder.build(type, format, args);
            if (innerType != null && !(innerType instanceof UntypedProperty)) {
                am.setItems(innerType);
            } else if (items.getRef() != null) {
                am.setItems(new RefProperty(items.getRef(),RefFormat.INTERNAL));
            } else {
                am.setItems(new RefProperty(type,RefFormat.INTERNAL));
            }
            output = am;
        } else {
            Map<PropertyBuilder.PropertyId, Object> args = new HashMap<PropertyBuilder.PropertyId, Object>();
            if (obj.getEnumValues() != null && obj.getEnumValues().size() > 0) {
                args.put(PropertyBuilder.PropertyId.ENUM, obj.getEnumValues());
            }
            if (obj.getMinimum() != null) {
                args.put(PropertyBuilder.PropertyId.MINIMUM, new BigDecimal(obj.getMinimum()));
            }
            if (obj.getMaximum() != null) {
                args.put(PropertyBuilder.PropertyId.MAXIMUM, new BigDecimal(obj.getMaximum()));
            }

            Property i = PropertyBuilder.build(type, format, args);
            if (i != null && !(i instanceof UntypedProperty)) {
                output = i;
            } else {
                if (obj.getRef() != null) {
                    output = new RefProperty(obj.getRef(),RefFormat.INTERNAL);
                } else if (type != null && !type.equals("void")) {
                    output = new RefProperty(type,RefFormat.INTERNAL);
                }
            }
        }

        return output;
    }

    public Operation convertOperation(String tag, io.swagger.models.apideclaration.Operation operation,
                                      ApiDeclaration apiDeclaration) {
        Method method;

        if (operation.getMethod() == null) {
            JsonNode node = (JsonNode) operation.getExtraFields().get("httpMethod");
            method = Method.forValue(node.asText());
            operation.setMethod(method);
        }

        Operation output = new Operation()
                .summary(operation.getSummary())
                .description(operation.getNotes())
                .operationId(operation.getNickname());

        if (tag != null) {
            output.tag(tag);
        }

        for (io.swagger.models.apideclaration.Parameter parameter : operation.getParameters()) {
            output.parameter(convertParameter(parameter));
        }

        if (operation.getConsumes() != null && !operation.getConsumes().isEmpty()) {
            for (String consumes : operation.getConsumes()) {
                output.consumes(consumes);
            }
        } else if (apiDeclaration.getConsumes() != null) {
            for (String consumes : apiDeclaration.getConsumes()) {
                output.consumes(consumes);
            }
        }
        if (operation.getProduces() != null && !operation.getProduces().isEmpty()) {
            for (String produces : operation.getProduces()) {
                output.produces(produces);
            }
        } else if (apiDeclaration.getProduces() != null) {
            for (String produces : apiDeclaration.getProduces()) {
                output.produces(produces);
            }
        }

        for (ResponseMessage message : operation.getResponseMessages()) {
            Response response = new Response().description(message.getMessage());

            Model responseModel = null;
            if (message.getResponseModel() != null) {
                response.setResponseSchema(new RefModel().asDefault(message.getResponseModel()));
            }
            output.response(message.getCode(), response);
        }

        // default response type
        Model responseProperty = modelFromExtendedTypedObject(operation);
        Response response = new Response()
                .description("success")
                .responseSchema(responseProperty);
        if (output.getResponses() == null) {
            output.defaultResponse(response);
        } else if (responseProperty != null) {
            output.response(200, response);
        }

        Map<String, List<AuthorizationScope>> auths = operation.getAuthorizations();

        for (String securityName : auths.keySet()) {
            List<AuthorizationScope> scopes = auths.get(securityName);
            List<String> updatedScopes = new ArrayList<String>();
            for (AuthorizationScope s : scopes) {
                updatedScopes.add(s.getScope());
            }
            output.addSecurity(securityName, updatedScopes);
        }

        return output;
    }

    public ApiDeclaration readDeclaration(String input, MessageBuilder messages, List<AuthorizationValue> auths) {
        ApiDeclaration output = null;
        try {
            JsonNode jsonNode = null;
            if (input.startsWith("http")) {
                String json = RemoteUrl.urlToString(input, auths);
                jsonNode = Json.mapper().readTree(json);
            } else {
                final String fileScheme = "file:";
                java.nio.file.Path path;
                if (input.toLowerCase().startsWith(fileScheme)) {
                    path = Paths.get(URI.create(input));
                } else {
                    path = Paths.get(input);
                }
                String json;
                if (Files.exists(path)) {
                     json= FileUtils.readFileToString(path.toFile(), "UTF-8");
                } else {
                    json = ClasspathHelper.loadFileFromClasspath(input  );
                }

                jsonNode = Json.mapper().readTree(json);
            }

            // this should be moved to a json patch
            if (jsonNode.isObject()) {
                ((ObjectNode) jsonNode).remove("authorizations");
            }

            ApiDeclarationMigrator migrator = new ApiDeclarationMigrator();
            JsonNode transformed = migrator.migrate(messages, jsonNode);
            output = Json.mapper().convertValue(transformed, ApiDeclaration.class);
        } catch (java.lang.IllegalArgumentException e) {
            return null;
        } catch (Exception e) {
            LOGGER.error("failed to read api declaration", e);
        }
        return output;
    }

    public Swagger convert(ResourceListing resourceListing, List<ApiDeclaration> apiDeclarations) {
        Info info = new Info();
        if (resourceListing.getInfo() != null) {
            ApiInfo apiInfo = resourceListing.getInfo();
            Contact contact = null;
            if (apiInfo.getContact() != null) {
                contact = new Contact()
                        .url(apiInfo.getContact());
            }
            License license = null;
            if (apiInfo.getLicense() != null) {
                license = new License()
                        .name(apiInfo.getLicense())
                        .url(apiInfo.getLicenseUrl());
            }
            info = new Info()
                    .description(apiInfo.getDescription())
                    .version(resourceListing.getApiVersion())
                    .title(apiInfo.getTitle())
                    .termsOfService(apiInfo.getTermsOfServiceUrl())
                    .contact(contact)
                    .license(license);
        } else if (resourceListing.getApiVersion() != null) {
            info = new Info()
                    .version(resourceListing.getApiVersion());
        }

        Map<String, Path> paths = new HashMap<String, Path>();
        Map<String, Model> definitions = new HashMap<String, Model>();
        String basePath = null;

        for (ApiDeclaration apiDeclaration : apiDeclarations) {
            String tag;
            if (apiDeclaration.getApiListingRef() != null) {
                String refPath = apiDeclaration.getApiListingRef().getPath();
                tag = refPath.substring(refPath.lastIndexOf("/") + 1);
            } else {
                tag = apiDeclaration.getResourcePath();
            }
            if (tag != null) {
                tag = tag.replaceAll("/", "");
            }
            if (basePath != null) {
                if (!basePath.equals(apiDeclaration.getBasePath()) && apiDeclaration.getBasePath() != null) {
                    LOGGER.warn("warning!  multiple basePath values not supported!");
                }
            } else {
                basePath = apiDeclaration.getBasePath();
            }

            List<Api> apis = apiDeclaration.getApis();
            for (Api api : apis) {
                String apiPath = api.getPath();
                String description = api.getDescription();
                List<io.swagger.models.apideclaration.Operation> ops = api.getOperations();

                Path path = paths.get(apiPath);
                if (path == null) {
                    path = new Path();
                    paths.put(apiPath, path);
                }
                for (io.swagger.models.apideclaration.Operation op : ops) {
                    Operation operation = convertOperation(tag, op, apiDeclaration);

                    if (op.getMethod() != null) {
                        path.set(op.getMethod().toString().toLowerCase(), operation);
                    } else {
                        LOGGER.info("skipping operation with missing method:\n" + Json.pretty(op));
                    }
                }
            }

            // model definitions
            Map<String, io.swagger.models.apideclaration.Model> apiModels = apiDeclaration.getModels();
            for (String key : apiModels.keySet()) {
                Model model = convertModel(apiModels.get(key));
                definitions.put(key, model);
            }
        }

        String host = null;
        String scheme = "http";

        if (basePath != null) {
            String[] parts = basePath.split("://");
            if (parts.length == 2) {
                scheme = parts[0];
                int pos = parts[1].indexOf("/");
                if (pos != -1) {
                    host = parts[1].substring(0, pos);
                    basePath = parts[1].substring(pos);
                } else {
                    host = parts[1];
                    basePath = "/";
                }
            }
            if (!basePath.startsWith("/")) {
                basePath = "/" + basePath;
            }
        }


        Swagger swagger = new Swagger()
                .host(host)
                .scheme(Scheme.forValue(scheme))
                .basePath(basePath)
                .info(info)
                        // .securityRequirement(securityRequirement)
                .paths(paths)
                        // .securityDefinitions(name, securityScheme)
                .basePath(basePath);
        swagger.setDefinitions(definitions);
        // host is read from the api declarations


        Map<String, Authorization> authorizations = resourceListing.getAuthorizations();
        if (authorizations != null) {
            for (String authNickname : authorizations.keySet()) {
                Authorization auth = authorizations.get(authNickname);
                if (auth instanceof OAuth2Authorization) {
                    OAuth2Authorization o2 = (OAuth2Authorization) auth;
                    List<AuthorizationScope> scopes = o2.getScopes();

                    if (o2.getGrantTypes().getImplicit() != null) {
                        ImplicitGrant ig = o2.getGrantTypes().getImplicit();
                        OAuth2Definition oauth2 = new OAuth2Definition()
                                .implicit(ig.getLoginEndpoint().getUrl());
                        if (swagger.getSecurityDefinitions() != null && swagger.getSecurityDefinitions().keySet().contains(authNickname)) {
                            System.err.println("Warning!  Authorization nickname already in use!");
                        } else {
                            swagger.securityDefinition(authNickname, oauth2);
                        }
                        for (AuthorizationScope scope : scopes) {
                            oauth2.scope(scope.getScope(), scope.getDescription());
                        }
                    } else if (o2.getGrantTypes().getAuthorization_code() != null) {
                        AuthorizationCodeGrant ac = (AuthorizationCodeGrant) o2.getGrantTypes().getAuthorization_code();
                        OAuth2Definition oauth2 = new OAuth2Definition()
                                .accessCode(ac.getTokenRequestEndpoint().getUrl(), ac.getTokenEndpoint().getUrl());
                        if (swagger.getSecurityDefinitions() != null && swagger.getSecurityDefinitions().keySet().contains(authNickname)) {
                            System.err.println("Warning!  Authorization nickname already in use!");
                        } else {
                            swagger.securityDefinition(authNickname, oauth2);
                        }
                        for (AuthorizationScope scope : scopes) {
                            oauth2.scope(scope.getScope(), scope.getDescription());
                        }
                    }
                } else if (auth instanceof ApiKeyAuthorization) {
                    ApiKeyAuthorization o2 = (ApiKeyAuthorization) auth;
                    ApiKeyAuthDefinition def = new ApiKeyAuthDefinition();

                    PassAs passAs = o2.getPassAs();
                    if (PassAs.HEADER.equals(passAs)) {
                        def.in(In.HEADER);
                    } else {
                        def.in(In.QUERY);
                    }

                    def.setName(o2.getKeyname());

                    swagger.securityDefinition(authNickname, def);
                } else if (auth instanceof BasicAuthorization) {
                    BasicAuthDefinition def = new BasicAuthDefinition();

                    swagger.securityDefinition(authNickname, def);
                }
            }
        }
        return swagger;
    }
}
