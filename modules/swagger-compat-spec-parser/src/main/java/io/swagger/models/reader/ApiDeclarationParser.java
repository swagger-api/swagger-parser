package io.swagger.models.reader;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.swagger.models.apideclaration.Api;
import io.swagger.models.apideclaration.ApiDeclaration;
import io.swagger.models.apideclaration.Model;
import io.swagger.models.apideclaration.ModelProperty;
import io.swagger.models.apideclaration.Operation;
import io.swagger.models.apideclaration.Parameter;
import io.swagger.report.Message;
import io.swagger.report.MessageBuilder;
import io.swagger.report.Severity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
            if (apiVersion != null) {
                api.setApiVersion(apiVersion);
            } else {
                messages.append(new Message("ApiDeclaration.apiVersion", "apiVersion is missing", Severity.RECOMMENDED));
            }

            String swaggerVersion = readString(m.get("swaggerVersion"));
            if (swaggerVersion != null) {
                api.setSwaggerVersion(swaggerVersion);
            } else {
                messages.append(new Message("ApiDeclaration.swaggerVersion", "swaggerVersion is missing", Severity.ERROR));
            }

            String basePath = readString(m.get("basePath"));
            if (basePath != null) {
                api.setBasePath(basePath);
            } else {
                messages.append(new Message("ApiDeclaration.basePath", "basePath is missing", Severity.ERROR));
            }

            String resourcePath = readString(m.get("resourcePath"));
            if (resourcePath != null) {
                api.setResourcePath(resourcePath);
            } else {
                messages.append(new Message("ApiDeclaration.resourcePath", "resourcePath is missing", Severity.ERROR));
            }

            String produces = readString(m.get("produces"));
            Object apis = m.get("apis");
            if (apis != null) {
                List<Api> o = readApis((List<Map<String, Object>>) apis, messages);
                if (o.size() > 0) {
                    api.setApis(o);
                }
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


            String discriminator = readString(value.get("discriminator"));
            model.setDiscriminator(discriminator);

            Object properties = value.get("properties");
            if (properties != null) {
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

            output.add(prop);
        }

        return output;
    }

    List<Api> readApis(List<Map<String, Object>> om, MessageBuilder messages) {
        List<Api> output = new ArrayList<Api>();
        for (Map<String, Object> o : om) {
            Api op = new Api();
            String path = readString(o.get("path"));
            if (path != null) {
                op.setPath(path);
            } else {
                messages.append(new Message("ApiDeclaration.apis", "path is missing", Severity.ERROR));
            }

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
        return new ArrayList<Operation>();
    }

    List<Parameter> readParameters(List<Map<String, Object>> o, MessageBuilder messages) {
        List<Parameter> output = new ArrayList<Parameter>();
        for (Map<String, Object> p : o) {
            Parameter param = readParameter(p, messages);
            if (param != null) {
                output.add(param);
            }
        }
        return output;
    }

    Parameter readParameter(Map<String, Object> o, MessageBuilder messages) {
        Parameter param = new Parameter();

        String name = readString(o.get("name"));
        if (name != null) {
            param.setName(name);
        } else {
            messages.append(new Message("ApiDeclaration.apis.operations.parameters.name", "missing name", Severity.ERROR));
        }
        String description = readString(o.get("description"));
        param.setDescription(description);

        Boolean required = readBoolean(o.get("required"));
        param.setRequired(required);

        String type = readString(o.get("type"));
        param.setType(type);

        Boolean allowMultiple = readBoolean(o.get("allowMultiple"));
        param.setAllowMultiple(allowMultiple);

        return param;
    }

}