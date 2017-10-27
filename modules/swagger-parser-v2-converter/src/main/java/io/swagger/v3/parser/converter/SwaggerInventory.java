package io.swagger.v3.parser.converter;

import io.swagger.models.ArrayModel;
import io.swagger.models.ComposedModel;
import io.swagger.models.Model;
import io.swagger.models.ModelImpl;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.Response;
import io.swagger.models.Swagger;
import io.swagger.models.Tag;
import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.MapProperty;
import io.swagger.models.properties.ObjectProperty;
import io.swagger.models.properties.Property;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SwaggerInventory {
    private List<Path> paths = new ArrayList();
    private List<Property> properties = new ArrayList();
    private List<Parameter> parameters = new ArrayList();
    private List<Operation> operations = new ArrayList();
    private List<Response> responses = new ArrayList();
    private List<Model> models = new ArrayList();
    private List<Tag> tags = new ArrayList();

    public SwaggerInventory() {
    }

    public List<Path> getPaths() {
        return this.paths;
    }

    public void setPaths(List<Path> paths) {
        this.paths = paths;
    }

    public List<Property> getProperties() {
        return this.properties;
    }

    public void setProperties(List<Property> properties) {
        this.properties = properties;
    }

    public List<Parameter> getParameters() {
        return this.parameters;
    }

    public void setParameters(List<Parameter> parameters) {
        this.parameters = parameters;
    }

    public List<Operation> getOperations() {
        return this.operations;
    }

    public void setOperations(List<Operation> operations) {
        this.operations = operations;
    }

    public List<Response> getResponses() {
        return this.responses;
    }

    public void setResponses(List<Response> responses) {
        this.responses = responses;
    }

    public List<Model> getModels() {
        return this.models;
    }

    public void setModels(List<Model> models) {
        this.models = models;
    }

    public List<Tag> getTags() {
        return this.tags;
    }

    public void setTags(List<Tag> tags) {
        this.tags = tags;
    }

    public SwaggerInventory process(Swagger swagger) {
        Iterator var2;
        if(swagger.getTags() != null) {
            var2 = swagger.getTags().iterator();

            while(var2.hasNext()) {
                Tag key = (Tag)var2.next();
                this.process(key);
            }
        }

        String key1;
        if(swagger.getPaths() != null) {
            var2 = swagger.getPaths().keySet().iterator();

            while(var2.hasNext()) {
                key1 = (String)var2.next();
                Path model = swagger.getPath(key1);
                this.process(model);
            }
        }

        if(swagger.getDefinitions() != null) {
            var2 = swagger.getDefinitions().keySet().iterator();

            while(var2.hasNext()) {
                key1 = (String)var2.next();
                Model model1 = (Model)swagger.getDefinitions().get(key1);
                this.process(model1);
            }
        }

        return this;
    }

    public void process(Tag tag) {
        this.tags.add(tag);
    }

    public void process(Path path) {
        this.paths.add(path);
        Iterator var2;
        if(path.getParameters() != null) {
            var2 = path.getParameters().iterator();

            while(var2.hasNext()) {
                Parameter operation = (Parameter)var2.next();
                this.process(operation);
            }
        }

        if(path.getOperations() != null) {
            var2 = path.getOperations().iterator();

            while(var2.hasNext()) {
                Operation operation1 = (Operation)var2.next();
                this.process(operation1);
            }
        }

    }

    public void process(Operation operation) {
        this.operations.add(operation);
        Iterator var2;
        if(operation.getParameters() != null) {
            var2 = operation.getParameters().iterator();

            while(var2.hasNext()) {
                Parameter key = (Parameter)var2.next();
                this.process(key);
            }
        }

        if(operation.getResponses() != null) {
            var2 = operation.getResponses().keySet().iterator();

            while(var2.hasNext()) {
                String key1 = (String)var2.next();
                Response response = (Response)operation.getResponses().get(key1);
                this.process(response);
            }
        }

    }

    public void process(Response response) {
        this.responses.add(response);
        if(response.getSchema() != null) {
            this.process(response.getSchema());
        }

    }

    public void process(Parameter parameter) {
        this.parameters.add(parameter);
        if(parameter instanceof BodyParameter) {
            BodyParameter p = (BodyParameter)parameter;
            if(p.getSchema() != null) {
                Model model = p.getSchema();
                if(model != null) {
                    this.process(model);
                }
            }
        }

    }

    public void process(Model model) {
        this.models.add(model);
        Iterator var3;
        String name;
        Property ip;
        if(model instanceof ModelImpl) {
            ModelImpl m = (ModelImpl)model;
            if(m.getProperties() != null) {
                var3 = m.getProperties().keySet().iterator();

                while(var3.hasNext()) {
                    name = (String)var3.next();
                    ip = (Property)m.getProperties().get(name);
                    this.process(ip);
                }
            }
        } else if(model instanceof ComposedModel) {
            ComposedModel m1 = (ComposedModel)model;
            if(m1.getAllOf() != null) {
                var3 = m1.getAllOf().iterator();

                while(var3.hasNext()) {
                    Model name1 = (Model)var3.next();
                    this.process(name1);
                }
            }
        } else if(model instanceof ArrayModel) {
            ArrayModel m2 = (ArrayModel)model;
            if(m2.getProperties() != null) {
                var3 = m2.getProperties().keySet().iterator();

                while(var3.hasNext()) {
                    name = (String)var3.next();
                    ip = (Property)m2.getProperties().get(name);
                    this.process(ip);
                }
            }
        }
    }

    public void process(Property property) {
        this.properties.add(property);
        if(property instanceof ArrayProperty) {
            ArrayProperty p = (ArrayProperty)property;
            Property ap = p.getItems();
            this.process(ap);
        } else if(property instanceof MapProperty) {
            MapProperty p1 = (MapProperty)property;
        } else if(property instanceof ObjectProperty) {
            ObjectProperty p2 = (ObjectProperty)property;
            if(p2.getProperties() != null) {
                Iterator ap1 = p2.getProperties().keySet().iterator();

                while(ap1.hasNext()) {
                    String name = (String)ap1.next();
                    Property ip = (Property)p2.getProperties().get(name);
                    this.process(ip);
                }
            }
        }

    }
}
