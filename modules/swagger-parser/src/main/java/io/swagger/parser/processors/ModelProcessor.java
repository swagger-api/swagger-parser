package io.swagger.parser.processors;

import io.swagger.models.ArrayModel;
import io.swagger.models.ComposedModel;
import io.swagger.models.Model;
import io.swagger.models.ModelImpl;
import io.swagger.models.RefModel;
import io.swagger.models.Swagger;
import io.swagger.models.properties.Property;
import io.swagger.models.refs.RefType;
import io.swagger.parser.ResolverCache;

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

        if (properties == null) {
            return;
        }

        for (Map.Entry<String, Property> propertyEntry : properties.entrySet()) {
            final Property property = propertyEntry.getValue();
            propertyProcessor.processProperty(property);
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
        String newRef = null;

        if (isAnExternalRefFormat(refModel.getRefFormat())) {

            newRef    = externalRefProcessor.processRefToExternalDefinition(refModel.get$ref(), refModel.getRefFormat());

        }

        if (newRef != null) {
            refModel.set$ref(RefType.DEFINITION.getInternalPrefix() + newRef);

        }

    }




}
