package io.swagger.parser.processors;

import io.swagger.models.Swagger;
import io.swagger.models.properties.*;
import io.swagger.models.refs.RefFormat;
import io.swagger.models.refs.RefType;
import io.swagger.parser.ResolverCache;

import java.util.List;
import java.util.Map;

import static io.swagger.parser.util.RefUtils.isAnExternalRefFormat;

public class PropertyProcessor  {

    private final ExternalRefProcessor externalRefProcessor;

    public PropertyProcessor(ResolverCache cache, Swagger swagger) {
        externalRefProcessor = new ExternalRefProcessor(cache, swagger);
    }

    public void processProperty(Property property) {
        if (property instanceof RefProperty) {
            processRefProperty((RefProperty) property);
        } else if (property instanceof ArrayProperty) {
            processArrayProperty((ArrayProperty) property);
        } else if (property instanceof MapProperty) {
            processMapProperty((MapProperty) property);
        } else if (property instanceof ObjectProperty) {
            processObjectProperty((ObjectProperty) property);
        } else if (property instanceof ComposedProperty) {
            processComposedProperty((ComposedProperty) property);
        }
    }

    private void processRefProperty(RefProperty refProperty) {
        // TODO: if the parent document is remote, then this will be remote by definition.
        if (isAnExternalRefFormat(refProperty.getRefFormat())) {
            final String newRef = externalRefProcessor.processRefToExternalDefinition(refProperty.get$ref(), refProperty.getRefFormat());

            if (newRef != null) {
                refProperty.set$ref(RefType.DEFINITION.getInternalPrefix()+newRef);
            }
        }
    }

    private void processMapProperty(MapProperty property) {
        final Property additionalProperties = property.getAdditionalProperties();
        if (additionalProperties != null) {
            processProperty(additionalProperties);
        }
    }

    private void processArrayProperty(ArrayProperty property) {
        final Property items = property.getItems();
        if (items != null) {
            processProperty(items);
        }
    }

    private void processObjectProperty(ObjectProperty property) {
        final Map<String, Property> properties = property.getProperties();
        if (properties != null)
            for (Property p : properties.values())
                processProperty(p);
    }

    private void processComposedProperty(ComposedProperty property) {
        final List<Property> properties = property.getAllOf();
        if (properties != null)
            for (Property p : properties)
                processProperty(p);
    }
}
