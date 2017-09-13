package io.swagger.parser.v2;

import v2.io.swagger.models.*;
import v2.io.swagger.models.parameters.AbstractSerializableParameter;
import v2.io.swagger.models.parameters.BodyParameter;
import v2.io.swagger.models.parameters.RefParameter;
import v2.io.swagger.models.parameters.SerializableParameter;
import v2.io.swagger.models.properties.Property;
import v2.io.swagger.models.properties.RefProperty;
import io.swagger.oas.models.*;
import io.swagger.oas.models.Operation;
import io.swagger.oas.models.info.Contact;
import io.swagger.oas.models.info.Info;
import io.swagger.oas.models.info.License;
import io.swagger.oas.models.media.*;
import io.swagger.oas.models.parameters.Parameter;
import io.swagger.oas.models.parameters.RequestBody;
import io.swagger.oas.models.responses.ApiResponse;
import io.swagger.oas.models.responses.ApiResponses;
import io.swagger.oas.models.servers.Server;
import io.swagger.oas.models.tags.Tag;
import v2.io.swagger.parser.SwaggerResolver;
import io.swagger.parser.extensions.SwaggerParserExtension;
import io.swagger.parser.models.AuthorizationValue;
import io.swagger.parser.models.ParseOptions;
import io.swagger.parser.models.SwaggerParseResult;
import v2.io.swagger.parser.util.SwaggerDeserializationResult;
import v2.io.swagger.util.Json;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class SwaggerConverter implements SwaggerParserExtension {
    private List<String> globalConsumes = new ArrayList<>();
    private List<String> globalProduces = new ArrayList<>();

    @Override
    public SwaggerParseResult readLocation(String url, List<AuthorizationValue> auths, ParseOptions options) {
        boolean resolve = false;
        if(options != null) {
            resolve = options.isResolve();
        }

        SwaggerDeserializationResult result = new v2.io.swagger.parser.SwaggerParser().readWithInfo(url, convert(auths), resolve);

        return convert(result);
    }

    @Override
    public SwaggerParseResult readContents(String swaggerAsString, List<io.swagger.parser.models.AuthorizationValue> auth, ParseOptions options) {
        SwaggerDeserializationResult result = new v2.io.swagger.parser.SwaggerParser().readWithInfo(swaggerAsString);

        if(options != null) {
            if(options.isResolve()) {
                Swagger resolved = new SwaggerResolver(result.getSwagger(), convert(auth)).resolve();
                result.setSwagger(resolved);
            }
        }
        return convert(result);
    }

    public List<v2.io.swagger.models.auth.AuthorizationValue> convert(List<AuthorizationValue> auths) {
        List<v2.io.swagger.models.auth.AuthorizationValue> convertedAuth = new ArrayList<>();
        if(auths != null) {
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
        if(parse == null) {
            return null;
        }

        if(parse.getSwagger() == null) {
            return new SwaggerParseResult()
                    .messages(parse.getMessages());
        }

        OpenAPI openAPI = new OpenAPI();
        SwaggerParseResult output = new SwaggerParseResult();

        SwaggerInventory inventory = new SwaggerInventory().process(parse.getSwagger());

        Swagger swagger = parse.getSwagger();

        openAPI.setInfo(convert(swagger.getInfo()));

        openAPI.setServers(convert(swagger.getSchemes(), swagger.getHost(), swagger.getBasePath()));

        if (swagger.getTags() != null) {
            openAPI.setTags(convertTags(swagger.getTags()));
        }

        if(swagger.getConsumes() != null) {
            this.globalConsumes.addAll(swagger.getConsumes());
        }
        if(swagger.getProduces() != null) {
            this.globalProduces.addAll(swagger.getProduces());
        }

        List<Model> models = inventory.getModels();

        // TODO until we have the example object working correctly in v3 pojos...
        for(Model model : models) {
            if(model.getExample() != null) {
                model.setExample(null);
            }
            if(model instanceof RefModel) {
                RefModel ref = (RefModel) model;
                if(ref.get$ref().indexOf("#/definitions") == 0) {
                    String updatedRef = "#/components/schemas" + ref.get$ref().substring("#/definitions".length());
                    ref.set$ref(updatedRef);
                }
            }
        }
        for(Property property : inventory.getProperties()) {
            if(property.getExample() != null) {
                property.setExample(null);
            }
            if(property instanceof RefProperty) {
                RefProperty ref = (RefProperty) property;
                if(ref.get$ref().indexOf("#/definitions") == 0) {
                    String updatedRef = "#/components/schemas" + ref.get$ref().substring("#/definitions".length());
                    ref.set$ref(updatedRef);
                }
            }
        }
        Paths v3Paths = new Paths();
        for(String pathname : swagger.getPaths().keySet()) {
            v2.io.swagger.models.Path v2Path = swagger.getPath(pathname);
            PathItem v3Path = convert(v2Path);
            v3Paths.put(pathname, v3Path);
        }
        openAPI.setPaths(v3Paths);
        Components components = new Components();
        if(swagger.getParameters() != null) {
            for(String name : swagger.getParameters().keySet()) {
                v2.io.swagger.models.parameters.Parameter param = swagger.getParameters().get(name);
                components.addParameters(name, convert(param));
            }
        }

        for(String key : swagger.getDefinitions().keySet()) {
            Model model = swagger.getDefinitions().get(key);
            Schema schema = Json.mapper().convertValue(model, Schema.class);

            components.addSchemas(key, schema);
        }

        openAPI.setComponents(components);

        output.setOpenAPI(openAPI);

        return output;
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
        externalDocumentation.setExtensions(externalDocs.getVendorExtensions());

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

        if (!StringUtils.startsWith(baseUrl, "/") && !schemes.isEmpty()) {
            for (Scheme scheme : schemes) {
                Server server = new Server();
                server.setUrl(scheme.toValue() + "://" + baseUrl);

                servers.add(server);
            }
        } else {
            Server server = new Server();
            server.setUrl(baseUrl);

            servers.add(server);
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

        if(v2Info.getVendorExtensions() != null && v2Info.getVendorExtensions().size() > 0) {
            info.setExtensions(v2Info.getVendorExtensions());
        }

        return info;
    }

    private License convert(v2.io.swagger.models.License v2License) {
        if(v2License == null) {
            return null;
        }

        License license = new License();

        if(v2License.getVendorExtensions() != null && v2License.getVendorExtensions().size() > 0) {
            license.setExtensions(v2License.getVendorExtensions());
        }
        license.setName(v2License.getName());
        license.setUrl(v2License.getUrl());

        return license;
    }

    public Contact convert(v2.io.swagger.models.Contact v2Contact) {
        if(v2Contact == null) {
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

        if(v2Path.getParameters() != null) {
            for(v2.io.swagger.models.parameters.Parameter param : v2Path.getParameters()) {
                v3Path.addParametersItem(convert(param));
            }
        }

        v2.io.swagger.models.Operation v2Operation;

        v2Operation = v2Path.getGet();
        if(v2Operation != null) {
            v3Path.setGet(convert(v2Operation));
        }
        v2Operation = v2Path.getPut();
        if(v2Operation != null) {
            v3Path.setPut(convert(v2Operation));
        }
        v2Operation = v2Path.getPost();
        if(v2Operation != null) {
            v3Path.setPost(convert(v2Operation));
        }
        v2Operation = v2Path.getPatch();
        if(v2Operation != null) {
            v3Path.setPatch(convert(v2Operation));
        }
        v2Operation = v2Path.getDelete();
        if(v2Operation != null) {
            v3Path.setDelete(convert(v2Operation));
        }

        if(v2Path.getVendorExtensions() != null && v2Path.getVendorExtensions().size() > 0) {
            v3Path.setExtensions(v2Path.getVendorExtensions());
        }
        return v3Path;
    }

    public Operation convert(v2.io.swagger.models.Operation v2Operation) {
        Operation operation = new Operation();
        if(StringUtils.isNotBlank(v2Operation.getDescription())) {
            operation.setDescription(v2Operation.getDescription());
        }
        if(StringUtils.isNotBlank(v2Operation.getSummary())) {
            operation.setSummary(v2Operation.getSummary());
        }
        operation.setDeprecated(v2Operation.isDeprecated());
        operation.setOperationId(v2Operation.getOperationId());

        operation.setTags(v2Operation.getTags());

        if(v2Operation.getParameters() != null) {
            List<v2.io.swagger.models.parameters.Parameter> formParams = new ArrayList<>();
            for(v2.io.swagger.models.parameters.Parameter param : v2Operation.getParameters()) {

                if("formData".equals(param.getIn())) {
                    formParams.add(param);
                }
                else if("body".equals(param.getIn())) {
                    RequestBody body = new RequestBody();
                    BodyParameter bp = (BodyParameter) param;

                    List<String> mediaTypes = new ArrayList<>(globalConsumes);
                    if(v2Operation.getConsumes() != null && v2Operation.getConsumes().size() > 0) {
                        mediaTypes.clear();
                        mediaTypes.addAll(v2Operation.getConsumes());
                    }

                    if(mediaTypes.size() == 0) {
                        mediaTypes.add("*/*");
                    }

                    if(StringUtils.isNotBlank(param.getDescription())) {
                        body.description(param.getDescription());
                    }
                    body.required(param.getRequired());

                    Content content = new Content();
                    for(String type: mediaTypes) {
                        content.addMediaType(type,
                            new MediaType().schema(
                                convert(bp.getSchema())));
                        if(StringUtils.isNotBlank(bp.getDescription())) {
                            body.setDescription(bp.getDescription());
                        }
                        operation.setRequestBody(body);
                    }
                    body.content(content);
                }
                else {
                    operation.addParametersItem(convert(param));
                }
            }

            if(formParams.size() > 0) {
                RequestBody body = new RequestBody();

                Schema formSchema = new Schema();

                for(v2.io.swagger.models.parameters.Parameter param : formParams) {
                    SerializableParameter sp = (SerializableParameter) param;

                    Schema schema = null;
                    if("file".equals(sp.getType())) {
                        schema = new FileSchema();
                    }
                    else if ("array".equals(sp.getType())) {
                        ArraySchema as = new ArraySchema();
                        if(sp.getItems() != null) {
                            as.setItems(convert(sp.getItems()));
                        }
                        schema = as;
                    }
                    else {
                        schema = new Schema();
                        schema.setType(sp.getType());
                        schema.setFormat(sp.getFormat());
                    }
                    schema.setDescription(sp.getDescription());
                    schema.setReadOnly(sp.isReadOnly());

                    formSchema.addProperties(param.getName(), schema);
                }
                List<String> mediaTypes = new ArrayList<>(globalConsumes);
                if(v2Operation.getConsumes() != null && v2Operation.getConsumes().size() > 0) {
                    mediaTypes.clear();
                    mediaTypes.addAll(v2Operation.getConsumes());
                }
                Content content = new Content();
                for(String type: mediaTypes) {
                    content.addMediaType(type, new MediaType().schema(formSchema));
                }
                body.content(content);
                operation.requestBody(body);
            }
        }

        if(v2Operation.getResponses() != null) {
            List<String> mediaTypes = new ArrayList<>(globalProduces);
            if(v2Operation.getProduces() != null) {
                // use this for media type
                mediaTypes.clear();
                mediaTypes.addAll(v2Operation.getProduces());
            }

            if(mediaTypes.size() == 0) {
                mediaTypes.add("*/*");
            }

            for(String responseCode : v2Operation.getResponses().keySet()) {
                v2.io.swagger.models.Response v2Response = v2Operation.getResponses().get(responseCode);
                ApiResponse response = convert(v2Response, mediaTypes);
                ApiResponses responses = operation.getResponses();
                if(responses == null) {
                    responses = new ApiResponses();
                }

                operation.responses(responses.addApiResponse(responseCode, response));
            }
        }

        return operation;
    }

    public ApiResponse convert(v2.io.swagger.models.Response v2Response, List<String> mediaTypes) {
        ApiResponse response = new ApiResponse();
        Content content = new Content();

        response.setDescription(v2Response.getDescription());
        if(v2Response.getSchema() != null) {
            Schema schema = convert(v2Response.getSchema());
            for(String type: mediaTypes) {
                // TODO: examples
                content.addMediaType(type, new MediaType().schema(schema));
            }
            response.content(content);
        }
        return response;
    }

    private Schema convert(Property schema) {
        return Json.mapper().convertValue(schema, Schema.class);
    }

    public Parameter convert(v2.io.swagger.models.parameters.Parameter v2Parameter) {
        Parameter v3Parameter = new Parameter();

        if(StringUtils.isNotBlank(v2Parameter.getDescription())) {
            v3Parameter.setDescription(v2Parameter.getDescription());
        }
        v3Parameter.setAllowEmptyValue(v2Parameter.getAllowEmptyValue());
        v3Parameter.setIn(v2Parameter.getIn());
        v3Parameter.setName(v2Parameter.getName());

        Schema schema = null;

        if(v2Parameter instanceof RefParameter) {
            schema = new Schema().$ref(((RefParameter) v2Parameter).get$ref());
        }
        else if(v2Parameter instanceof SerializableParameter) {
            SerializableParameter sp = (SerializableParameter) v2Parameter;

            if(sp.getVendorExtensions() != null && sp.getVendorExtensions().size() > 0) {
                schema.setExtensions(sp.getVendorExtensions());
            }
            if(sp.getEnum() != null) {
                for(String e : sp.getEnum()) {
                    // TODO: use the proper method for enum items on schema
                    //schema.addEnumItem(e);
                }
            }
            if ("array".equals(sp.getType())) {
                ArraySchema a = new ArraySchema();
                // TODO: convert arrays to proper template format
                sp.getCollectionFormat();
                Property items = sp.getItems();
                Schema itemsSchema = convert(items);
                a.setItems(itemsSchema);

                if (sp.getMaxItems() != null) {
                    a.setMaxItems(sp.getMaxItems());
                }
                if (sp.getMinItems() != null) {
                    a.setMinItems(sp.getMinItems());
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
                schema.setMinimum(sp.getMinimum());
                schema.setMinLength(sp.getMinLength());
                schema.setMaxLength(sp.getMaxLength());
                if (sp.getMultipleOf() != null) {
                    schema.setMultipleOf(new BigDecimal(sp.getMultipleOf().toString()));
                }
                schema.setPattern(sp.getPattern());
            }

            if(sp instanceof AbstractSerializableParameter) {
                AbstractSerializableParameter ap = (AbstractSerializableParameter) sp;
                schema.setDefault(ap.getDefault());
            }
        }

        if(v2Parameter.getRequired()) {
            v3Parameter.setRequired(v2Parameter.getRequired());
        }
        v3Parameter.setSchema(schema);
        if(v2Parameter.getVendorExtensions() != null && v2Parameter.getVendorExtensions().size() > 0) {
            v3Parameter.setExtensions(v2Parameter.getVendorExtensions());
        }
        return v3Parameter;
    }

    public Schema convert(v2.io.swagger.models.Model v2Model) {
        return Json.mapper().convertValue(v2Model, Schema.class);
    }
}
