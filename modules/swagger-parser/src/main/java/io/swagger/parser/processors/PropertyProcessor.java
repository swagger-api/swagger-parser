package io.swagger.parser.processors;

import java.nio.file.Path;

import io.swagger.models.Swagger;
import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.MapProperty;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.RefProperty;
import io.swagger.parser.ResolverCache;

import static io.swagger.parser.util.RefUtils.isAnExternalRefFormat;

public class PropertyProcessor  {

    private final ExternalRefProcessor externalRefProcessor;

    public PropertyProcessor(ResolverCache cache, Swagger swagger) {
        externalRefProcessor = new ExternalRefProcessor(cache, swagger);
    }

    public void processProperty(Property property, Path propertyModelDirectory) {
        if (property instanceof RefProperty) {
            processRefProperty((RefProperty) property, propertyModelDirectory);
        } else if (property instanceof ArrayProperty) {
            processArrayProperty((ArrayProperty) property, propertyModelDirectory);
        } else if (property instanceof MapProperty) {
            processMapProperty((MapProperty) property, propertyModelDirectory);
        }
    }

    private void processRefProperty(RefProperty refProperty, Path propertyModelDirectory) {
        if (isAnExternalRefFormat(refProperty.getRefFormat())) {
            final String newRef = externalRefProcessor.processRefToExternalDefinition(refProperty.get$ref(), refProperty.getRefFormat(), propertyModelDirectory);

            if (newRef != null) {
                refProperty.set$ref(newRef);
            }
        }
    }

    private void processMapProperty(MapProperty property, Path propertyModelDirectory) {
        final Property additionalProperties = property.getAdditionalProperties();
        if (additionalProperties != null) {
            processProperty(additionalProperties, propertyModelDirectory);
        }
    }

    private void processArrayProperty(ArrayProperty property, Path propertyModelDirectory) {
        final Property items = property.getItems();
        if (items != null) {
            processProperty(items, propertyModelDirectory);
        }
    }
}
