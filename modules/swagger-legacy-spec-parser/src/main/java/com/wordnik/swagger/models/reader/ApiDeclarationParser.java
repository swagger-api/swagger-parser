package com.wordnik.swagger.models.reader;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.annotation.JsonInclude.*;
import com.wordnik.swagger.models.apideclaration.*;
import com.wordnik.swagger.report.Message;
import com.wordnik.swagger.report.MessageBuilder;
import com.wordnik.swagger.report.Severity;

import java.util.*;

public class ApiDeclarationParser extends SwaggerParser {

    public ApiDeclaration read(String json, MessageBuilder messages) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.setSerializationInclusion(Include.NON_NULL);

        try {
            Map<String, Object> m = mapper.readValue(json, Map.class);
            ApiDeclaration api = new ApiDeclaration();

            String apiVersion = readString(m.get("apiVersion"));
            if (apiVersion != null)
                api.setApiVersion(apiVersion);
            else
                messages.append(new Message("ApiDeclaration.apiVersion", "apiVersion is missing", Severity.RECOMMENDED));

            String swaggerVersion = readString(m.get("swaggerVersion"));
            if (swaggerVersion != null)
                api.setSwaggerVersion(swaggerVersion);
            else
                messages.append(new Message("ApiDeclaration.swaggerVersion", "swaggerVersion is missing", Severity.ERROR));

            String basePath = readString(m.get("basePath"));
            if (basePath != null)
                api.setBasePath(basePath);
            else
                messages.append(new Message("ApiDeclaration.basePath", "basePath is missing", Severity.ERROR));

            String resourcePath = readString(m.get("resourcePath"));
            if (resourcePath != null)
                api.setResourcePath(resourcePath);
            else
                messages.append(new Message("ApiDeclaration.resourcePath", "resourcePath is missing", Severity.ERROR));

            String produces = readString(m.get("produces"));
            Object apis = m.get("apis");
            if (apis != null) {
                List<Api> o = readApis((List<Map<String, Object>>) apis, messages);
                if (o.size() > 0)
                    api.setApis(o);
            }

            Object models = m.get("models");
            if (models != null) {
                Map<String, Model> modelMap = readModels((Map<String, Object>) models, messages);
                api.setModels(modelMap);
            }

            return api;
        } catch (Exception e) {
            messages.append(new Message("ApiDeclaration", "invalid json", Severity.ERROR));
            return null;
        }
    }

    Map<String, Model> readModels(Map<String, Object> o, MessageBuilder messages) {
        Map<String, Model> output = new HashMap<String, Model>();

        for (String modelName : o.keySet()) {
            Model model = new Model();
            Map<String, Object> value = (Map<String, Object>) o.get(modelName);
            String id = readString(value.get("id"));
            model.setId(id);

            String description = readString(value.get("description"));
            model.setDescription(description);

//            String name = readString(value.get("name"));
//            model.setName(name);
//
//            String baseModel = readString(value.get("baseModel"));
//            model.setBaseModel(baseModel);
//
//            String qualifiedType = readString(value.get("qualifiedType"));
//            model.setQualifiedType(qualifiedType);

            String discriminator = readString(value.get("discriminator"));
            model.setDiscriminator(discriminator);

            Object properties = value.get("properties");
            if (properties != null) {
//                List<ModelProperty> props = readProperties((Map<String, Object>) properties, messages);
//                model.setProperties(props);
            }
            output.put(modelName, model);
        }

        return output;
    }

    List<ModelProperty> readProperties(Map<String, Object> properties, MessageBuilder messages) {
        List<ModelProperty> output = new ArrayList<ModelProperty>();

        for (String key : properties.keySet()) {
            Map<String, Object> value = (Map<String, Object>) properties.get(key);
            ModelProperty prop = new ModelProperty();

            String type = readString(value.get("type"));
            prop.setType(type);

            String qualifiedType = readString(value.get("qualifiedType"));
//            prop.setQualifiedType(qualifiedType);
//
//            Integer position = readInteger(value.get("position"));
//            prop.setPosition(position);
//
//            Boolean required = readBoolean(value.get("required"));
//            prop.setRequired(required);

            output.add(prop);
            // private List<AllowableValues> allowableValues = new ArrayList<AllowableValues>();
            // private ModelRef items = null;
        }

        return output;
    }

    List<Api> readApis(List<Map<String, Object>> om, MessageBuilder messages) {
        List<Api> output = new ArrayList<Api>();
        for (Map<String, Object> o : om) {
            Api op = new Api();
            String path = readString(o.get("path"));
            if (path != null)
                op.setPath(path);
            else
                messages.append(new Message("ApiDeclaration.apis", "path is missing", Severity.ERROR));

            Object operations = o.get("operations");

            if (operations instanceof List) {
                List<Operation> ops = readOperations((List<Map<String, Object>>) operations, messages);
                op.setOperations(ops);
            }

            output.add(op);
        }
        return output;
    }

    List<Operation> readOperations(List<Map<String, Object>> ops, MessageBuilder messages) {
        List<Operation> output = new ArrayList<Operation>();

//        for (Map<String, Object> o : ops) {
//            Operation op = new Operation();
//            String method = readString(o.get("method"));
////            if (method != null)
////                op.setMethod(method);
////            else
////                messages.append(new Message("ApiDeclaration.apis.operations.method", "missing method", Severity.ERROR));
//            String summary = readString(o.get("summary"));
//            if (summary != null)
//                op.setSummary(summary);
//            String notes = readString(o.get("notes"));
//            if (notes != null)
//                op.setNotes(notes);
////            String type = readString(o.get("type"));
////            if (type != null)
//////                op.setResponseClass(type);
////            else
////                messages.append(new Message("ApiDeclaration.apis.operations.type", "missing return type", Severity.ERROR));
//            String nickname = readString(o.get("nickname"));
//            if (nickname != null)
//                op.setNickname(nickname);
//            else
//                messages.append(new Message("ApiDeclaration.apis.operations.nickname", "missing nickname", Severity.ERROR));
//
////            Object authorizations = o.get("authorizations");
////            if (authorizations != null) {
////                Map<String, Authorization> auths = readAuthorizations((Map<String, List<Object>>) authorizations, messages);
////                if (auths.size() > 0)
////                    op.setAuthorizations(auths);
////            }
//
//            Object parameters = o.get("parameters");
//            if (parameters != null) {
//                List<Parameter> params = readParameters((List<Map<String, Object>>) parameters, messages);
//                op.setParameters(params);
//            }
//
//            output.add(op);
//        }
        return output;
    }

    List<Parameter> readParameters(List<Map<String, Object>> o, MessageBuilder messages) {
        List<Parameter> output = new ArrayList<Parameter>();
        for (Map<String, Object> p : o) {
            Parameter param = readParameter(p, messages);
            if (param != null)
                output.add(param);
        }
        return output;
    }

    Parameter readParameter(Map<String, Object> o, MessageBuilder messages) {
        Parameter param = new Parameter();

        String name = readString(o.get("name"));
        if (name != null)
            param.setName(name);
        else
            messages.append(new Message("ApiDeclaration.apis.operations.parameters.name", "missing name", Severity.ERROR));
        String description = readString(o.get("description"));
        param.setDescription(description);

        Boolean required = readBoolean(o.get("required"));
        param.setRequired(required);

        String type = readString(o.get("type"));
        param.setType(type);

//        String format = readString(o.get("format"));
//        param.setFormat(format);

        // TODO: items
//        String paramType = readString(o.get("paramType"));
//        param.setParamType(paramType);

        Boolean allowMultiple = readBoolean(o.get("allowMultiple"));
        param.setAllowMultiple(allowMultiple);

        return param;
    }

//    Map<String, Authorization> readAuthorizations(Map<String, List<Object>> ops, MessageBuilder messages) {
//        Map<String, Authorization> output = new HashMap<String, Authorization>();
//
//        for (String key : ops.keySet()) {
//            Authorization auth = new Authorization();
//            Object value = ops.get(key);
//            List<AuthorizationScope> scopes = readAuthorizationScopes((List<Map<String, Object>>) value, messages);
//            auth.setScopes(scopes);
//            output.put(key, auth);
//        }
//
//        return output;
//    }
}