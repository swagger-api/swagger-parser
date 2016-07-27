package io.swagger.parser.processors;

import io.swagger.models.ArrayModel;
import io.swagger.models.ComposedModel;
import io.swagger.models.Model;
import io.swagger.models.ModelImpl;
import io.swagger.models.RefModel;
import io.swagger.models.Swagger;
import io.swagger.models.properties.Property;
import io.swagger.models.refs.GenericRef;
import io.swagger.models.refs.RefType;
import io.swagger.parser.ResolverCache;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static io.swagger.parser.util.RefUtils.isAnExternalRefFormat;


public class ModelProcessor {
    private final PropertyProcessor propertyProcessor;
    private final ExternalRefProcessor externalRefProcessor;

    public ModelProcessor(ResolverCache cache, Swagger swagger) {
        this.propertyProcessor = new PropertyProcessor(cache, swagger);
        this.externalRefProcessor = new ExternalRefProcessor(cache, swagger);
    }

    public void processModel(Model model) {
        if (model == null) {
            return;
        }

        if (model instanceof RefModel) {
            processRefModel((RefModel) model);
        } else if (model instanceof ArrayModel) {
            processArrayModel((ArrayModel) model);
        } else if (model instanceof ComposedModel) {
            processComposedModel((ComposedModel) model);
        } else if (model instanceof ModelImpl) {
            processModelImpl((ModelImpl) model);
        }
    }

    private void processModelImpl(ModelImpl modelImpl) {

        final Map<String, Property> properties = modelImpl.getProperties();

        if (properties != null) {
            for (Map.Entry<String, Property> propertyEntry : properties.entrySet()) {
                final Property property = propertyEntry.getValue();
                propertyProcessor.processProperty(property);
            }
        }

        Map<String, Object> vendorExtensions = modelImpl.getVendorExtensions();
        if (vendorExtensions != null) {
            if (vendorExtensions.containsKey("x-collection")) {
                ObjectNode xCollection = (ObjectNode) vendorExtensions.get("x-collection");
                String sub$ref = xCollection.get("schema").asText();
                GenericRef subRef = new GenericRef(RefType.DEFINITION, sub$ref);
                if (isAnExternalRefFormat(subRef.getFormat())) {
                    xCollection.put("schema", "#/definitions/" + externalRefProcessor.processRefToExternalDefinition(subRef.getRef(), subRef.getFormat()));
                }
            }
            if (vendorExtensions.containsKey("x-links")) {
                ObjectNode xLinks = (ObjectNode) vendorExtensions.get("x-links");
                Iterator<String> xLinksNames = xLinks.fieldNames();
                while (xLinksNames.hasNext()) {
                    String linkName = xLinksNames.next();
                    ObjectNode xLink = (ObjectNode) xLinks.get(linkName);
                    String sub$ref = xLink.get("schema").asText();
                    GenericRef subRef = new GenericRef(RefType.DEFINITION, sub$ref);
                    if (isAnExternalRefFormat(subRef.getFormat())) {
                        xLink.put("schema", "#/definitions/" + externalRefProcessor.processRefToExternalDefinition(subRef.getRef(), subRef.getFormat()));
                    }
                }
            }
        }
    }

    private void processComposedModel(ComposedModel composedModel) {

        processModel(composedModel.getParent());
        processModel(composedModel.getChild());

        final List<RefModel> interfaces = composedModel.getInterfaces();
        if (interfaces != null) {
            for (RefModel model : interfaces) {
                processRefModel(model);
            }
        }

    }

    private void processArrayModel(ArrayModel arrayModel) {

        final Property items = arrayModel.getItems();

        // ArrayModel has a properties map, but my reading of the swagger spec makes me think it should be ignored

        if (items != null) {
            propertyProcessor.processProperty(items);
        }
    }


    private void processRefModel(RefModel refModel) {
    /* if this is a URL or relative ref:
        1) we need to load it into memory.
        2) shove it into the #/definitions
        3) update the RefModel to point to its location in #/definitions
     */

        if (isAnExternalRefFormat(refModel.getRefFormat())) {
            final String newRef = externalRefProcessor.processRefToExternalDefinition(refModel.get$ref(), refModel.getRefFormat());

            if (newRef != null) {
                refModel.set$ref(newRef);
            }
        }
    }


}
