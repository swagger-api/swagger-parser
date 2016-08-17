package io.swagger.parser.processors;

import io.swagger.models.Swagger;
import io.swagger.models.properties.*;
import io.swagger.parser.ResolverCache;

import java.util.Map;

import static io.swagger.parser.util.RefUtils.isAnExternalRefFormat;

public class PropertyProcessor  {

    private final ExternalRefProcessor externalRefProcessor;
    private final ResolverCache cache;

    public PropertyProcessor(ResolverCache cache, Swagger swagger) {
        externalRefProcessor = new ExternalRefProcessor(cache, swagger);
        this.cache = cache;
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
        }
    }

    private void processRefProperty(RefProperty refProperty) {
        if (isAnExternalRefFormat(refProperty.getRefFormat())) {
            final String newRef = externalRefProcessor.processRefToExternalDefinition(refProperty.get$ref(), refProperty.getRefFormat());

            if (newRef != null) {
                refProperty.set$ref(newRef);
            }
        } else {
        	cache.checkInternalRef(refProperty.get$ref());
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
}
