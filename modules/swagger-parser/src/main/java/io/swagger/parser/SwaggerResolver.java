package io.swagger.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.models.ArrayModel;
import io.swagger.models.Model;
import io.swagger.models.ModelImpl;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.RefModel;
import io.swagger.models.Response;
import io.swagger.models.Swagger;
import io.swagger.models.auth.AuthorizationValue;
import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.parameters.RefParameter;
import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.MapProperty;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.RefProperty;
import io.swagger.parser.util.RemoteUrl;
import io.swagger.util.Json;
import io.swagger.util.Yaml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SwaggerResolver {
    protected Swagger swagger;
    protected Map<String, List<ResolutionContext>> resolutionMap = new HashMap<String, List<ResolutionContext>>();
    protected ResolverOptions opts;
    Logger LOGGER = LoggerFactory.getLogger(SwaggerResolver.class);

    public SwaggerResolver() {
    }

    public SwaggerResolver(ResolverOptions opts) {
        this.opts = opts;
    }

    public Swagger resolve(Swagger swagger, List<AuthorizationValue> auths) {
        if (swagger == null) {
            return null;
        }

        this.swagger = swagger;

        // models
        detectModelRefs();

        // operations
        detectOperationRefs();

        applyResolutions(auths);
        return this.swagger;
    }

    public void applyResolutions(List<AuthorizationValue> auths) {
        // hosts to call
        Map<String, List<Object>> hostToObjectMap = new HashMap<String, List<Object>>();

        for (String path : resolutionMap.keySet()) {
            String[] parts = path.split("#");
            if (parts.length == 2) {
                String host = parts[0];
                String definitionPath = parts[1];
                List<Object> objectList = hostToObjectMap.get(host);
                if (objectList == null) {
                    objectList = new ArrayList<Object>();
                    hostToObjectMap.put(host, objectList);
                }
                List<ResolutionContext> contexts = resolutionMap.get(path);
                for (ResolutionContext ctx : contexts) {
                    Object mapping = ctx.object;
                    Object target = ctx.parent;
                    try {
                        String contents = null;
                        if (host.startsWith("http")) {
                            contents = new RemoteUrl().urlToString(host, auths);
                        } else {
                            contents = Json.mapper().writeValueAsString(swagger);
                        }
                        JsonNode location = null;
                        String locationName = null;
                        if (contents != null) {
                            ObjectMapper mapper;
                            if (contents.trim().startsWith("{")) {
                                mapper = Json.mapper();
                            } else {
                                mapper = Yaml.mapper();
                            }
                            location = mapper.readTree(contents);
                            String[] objectPath = definitionPath.split("/");
                            for (String objectPathPart : objectPath) {
                                LOGGER.debug("getting part " + objectPathPart);
                                if (objectPathPart.length() > 0 && location != null) {
                                    location = location.get(objectPathPart);
                                    locationName = objectPathPart;
                                }
                            }
                        }
                        if (location != null) {
                            // convert the node to the proper type
                            if (mapping instanceof Property) {
                                Model model = Json.mapper().convertValue(location, Model.class);
                                if (mapping instanceof RefProperty) {
                                    RefProperty ref = (RefProperty) mapping;
                                    ref.set$ref(locationName);
                                    swagger.addDefinition(locationName, model);
                                }
                            } else if (target instanceof Parameter) {
                                if (mapping instanceof RefModel) {
                                    Model model = Json.mapper().convertValue(location, Model.class);
                                    RefModel ref = (RefModel) mapping;
                                    ref.set$ref(locationName);
                                    swagger.addDefinition(locationName, model);
                                }
                            } else if (target instanceof Operation) {

                                // get the operation position
                                Operation operation = (Operation) target;
                                int position = 0;
                                for (Parameter param : operation.getParameters()) {

                                    if (param instanceof RefParameter) {
                                        RefParameter ref = (RefParameter) param;
                                        if (ref.getSimpleRef().equals(locationName)) {
                                            // found a match!
                                            Parameter remoteParam = Json.mapper().convertValue(location, Parameter.class);
                                            operation.getParameters().set(position, remoteParam);
                                        }
                                    }
                                    position += 1;
                                }
                            }
                        }
                    } catch (Exception e) {
                        // failed to get it
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public void detectOperationRefs() {
        Map<String, Path> paths = swagger.getPaths();
        if (paths == null) {
            return;
        }

        for (String pathName : paths.keySet()) {
            Path path = paths.get(pathName);
            List<Operation> operations = path.getOperations();
            for (Operation operation : operations) {
                if (operation.getParameters() != null) {
                    for (Parameter parameter : operation.getParameters()) {
                        if (parameter instanceof BodyParameter) {
                            BodyParameter bp = (BodyParameter) parameter;
                            if (bp.getSchema() != null && bp.getSchema() instanceof RefModel) {
                                RefModel ref = (RefModel) bp.getSchema();
                                String key = ref.get$ref();
                                if (key.startsWith("http")) {
                                    LOGGER.debug("added reference to " + key);
                                    List<ResolutionContext> m = resolutionMap.get(key);
                                    if (m == null) {
                                        m = new ArrayList<ResolutionContext>();
                                    }
                                    m.add(new ResolutionContext(ref, bp, "ref"));
                                    resolutionMap.put(key, m);
                                }
                            }
                        } else if (parameter instanceof RefParameter) {
                            RefParameter ref = (RefParameter) parameter;
                            String key = ref.get$ref();
                            LOGGER.debug("added reference to " + ref);

                            List<ResolutionContext> m = resolutionMap.get(key);
                            if (m == null) {
                                m = new ArrayList<ResolutionContext>();
                            }
                            m.add(new ResolutionContext(ref, operation, "inline"));
                            resolutionMap.put(key, m);
                        }
                    }
                }
                if (operation.getResponses() != null) {
                    for (String responseCode : operation.getResponses().keySet()) {
                        Response response = operation.getResponses().get(responseCode);
                        if (response.getSchema() != null) {
                            Property schema = response.getSchema();
                            if (schema instanceof RefProperty) {
                                RefProperty ref = (RefProperty) schema;
                                String key = ref.get$ref();

                                if (key != null && key.startsWith("http")) {
                                    List<ResolutionContext> m = resolutionMap.get(key);
                                    if (m == null) {
                                        m = new ArrayList<ResolutionContext>();
                                    }
                                    m.add(new ResolutionContext(ref, response, "ref"));
                                    resolutionMap.put(key, m);
                                }
                            }
                            else if (schema instanceof ArrayProperty) {
                              Property item = ((ArrayProperty)schema).getItems();
                              if (item instanceof RefProperty) {
                                RefProperty ref = (RefProperty) item;
                                String key = ref.get$ref();

                                if (key != null && key.startsWith("http")) {
                                    List<ResolutionContext> m = resolutionMap.get(key);
                                    if (m == null) {
                                        m = new ArrayList<ResolutionContext>();
                                    }
                                    m.add(new ResolutionContext(ref, schema, "ref"));
                                    resolutionMap.put(key, m);
                                }
                              }
                            }
                        }
                    }
                }
            }
        }
    }

    public void detectModelRefs() {
        Map<String, Model> models = swagger.getDefinitions();
        if (models != null) {
            for (String modelName : models.keySet()) {
                LOGGER.debug("looking at " + modelName);
                Model model = models.get(modelName);
                if (model instanceof RefModel) {
                    RefModel ref = (RefModel) model;
                    String key = ref.get$ref();
                    if (key != null && key.startsWith("http")) {
                        LOGGER.debug("added reference to " + key);
                        List<ResolutionContext> m = resolutionMap.get(key);
                        if (m == null) {
                            m = new ArrayList<ResolutionContext>();
                        }
                        m.add(new ResolutionContext(ref, swagger.getDefinitions(), "ref"));
                        resolutionMap.put(key, m);
                    }
                } else if (model instanceof ArrayModel) {
                    ArrayModel arrayModel = (ArrayModel) model;
                    if (arrayModel.getItems() != null && arrayModel.getItems() instanceof RefProperty) {
                        RefProperty ref = (RefProperty) arrayModel.getItems();
                        String key = ref.get$ref();
                        if (key != null && key.startsWith("http")) {
                            LOGGER.debug("added reference to " + key);
                            List<ResolutionContext> m = resolutionMap.get(key);
                            if (m == null) {
                                m = new ArrayList<ResolutionContext>();
                            }
                            m.add(new ResolutionContext(ref, swagger.getDefinitions(), "ref"));
                            resolutionMap.put(key, m);
                        }
                    }
                } else if (model instanceof ModelImpl) {
                    ModelImpl impl = (ModelImpl) model;
                    Map<String, Property> properties = impl.getProperties();
                    if (properties != null) {
                        for (String propertyName : properties.keySet()) {
                            Property property = properties.get(propertyName);
                            if (property instanceof RefProperty) {
                                RefProperty ref = (RefProperty) property;
                                String key = ref.get$ref();
                                if (key != null && key.startsWith("http")) {
                                    LOGGER.debug("added reference to " + key);
                                    List<ResolutionContext> m = resolutionMap.get(key);
                                    if (m == null) {
                                        m = new ArrayList<ResolutionContext>();
                                    }
                                    m.add(new ResolutionContext(ref, impl, "ref"));
                                    resolutionMap.put(key, m);
                                }
                            } else if (property instanceof ArrayProperty) {
                                ArrayProperty arrayProperty = (ArrayProperty) property;
                                if (arrayProperty.getItems() != null && arrayProperty.getItems() instanceof RefProperty) {
                                    RefProperty ref = (RefProperty) arrayProperty.getItems();
                                    String key = ref.get$ref();
                                    if (key != null && key.startsWith("http")) {
                                        LOGGER.debug("added reference to " + key);
                                        List<ResolutionContext> m = resolutionMap.get(key);
                                        if (m == null) {
                                            m = new ArrayList<ResolutionContext>();
                                        }
                                        m.add(new ResolutionContext(ref, arrayProperty, "ref"));
                                        resolutionMap.put(key, m);
                                    }
                                }
                            } else if (property instanceof MapProperty) {
                                MapProperty mp = (MapProperty) property;
                                if (mp.getAdditionalProperties() != null && mp.getAdditionalProperties() instanceof RefProperty) {
                                    RefProperty ref = (RefProperty) mp.getAdditionalProperties();
                                    String key = ref.get$ref();
                                    if (key != null && key.startsWith("http")) {
                                        LOGGER.debug("added reference to " + key);
                                        List<ResolutionContext> m = resolutionMap.get(key);
                                        if (m == null) {
                                            m = new ArrayList<ResolutionContext>();
                                        }
                                        m.add(new ResolutionContext(ref, mp, "ref"));
                                        resolutionMap.put(key, m);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    static class ResolutionContext {
        private Object object, parent;
        private String scope;

        public ResolutionContext(Object object, Object parent, String scope) {
            this.object = object;
            this.parent = parent;
            this.scope = scope;
        }
    }
}