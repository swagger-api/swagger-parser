package io.swagger.parser.processors;

import io.swagger.models.ArrayModel;
import io.swagger.models.ComposedModel;
import io.swagger.models.Model;
import io.swagger.models.ModelImpl;
import io.swagger.models.RefModel;
import io.swagger.models.Swagger;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.RefProperty;
import io.swagger.models.refs.RefFormat;
import io.swagger.parser.ResolverCache;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.swagger.parser.util.RefUtils.computeDefinitionName;
import static io.swagger.parser.util.RefUtils.deconflictName;
import static io.swagger.parser.util.RefUtils.isAnExternalRefFormat;


public final class ExternalRefProcessor {

    private final ResolverCache cache;
    private final Swagger swagger;

    public ExternalRefProcessor(ResolverCache cache, Swagger swagger) {
        this.cache = cache;
        this.swagger = swagger;
    }

    public String processRefToExternalDefinition(String $ref, RefFormat refFormat) {
        final Model model = cache.loadRef($ref, refFormat, Model.class);

        String newRef;

        Map<String, Model> definitions = swagger.getDefinitions();

        if (definitions == null) {
            definitions = new HashMap<>();
        }

        final String possiblyConflictingDefinitionName = computeDefinitionName($ref);

        final Model existingModel = definitions.get(possiblyConflictingDefinitionName);

        if (existingModel != null) {
            //so this is either a conflict, or another reference to something we have previously renamed
            final String previouslyRenamedRef = cache.getRenamedRef($ref);
            if (previouslyRenamedRef != null) {
                //this is an additional reference to something we have renamed once
                newRef = previouslyRenamedRef;
            } else {
                //this is a conflict
                String deconflictedName = deconflictName(possiblyConflictingDefinitionName, definitions);
                cache.putRenamedRef($ref, deconflictedName);
                newRef = deconflictedName;
                swagger.addDefinition(deconflictedName, model);
            }

        } else {
            newRef = possiblyConflictingDefinitionName;
            cache.putRenamedRef($ref, newRef);


            //If this is a new model, then check it for other sub references
            processModel(model);
            swagger.addDefinition(newRef, model);
        }

        return newRef;
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

        //Loop the properties and recursively call this method;
        for(Map.Entry<String,Property> prop: properties.entrySet()){
            if(prop.getValue() instanceof RefProperty){
                RefProperty subRef = (RefProperty)prop.getValue();
                subRef.set$ref(processRefToExternalDefinition(subRef.get$ref(),subRef.getRefFormat()));
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
            if (items instanceof RefProperty) {
                RefProperty itemsRef = (RefProperty)items;
                final String newRef = processRefToExternalDefinition(itemsRef.get$ref(), itemsRef.getRefFormat());

                if (newRef != null) {
                    itemsRef.set$ref(newRef);
                }
            }
        }
    }


    private void processRefModel(RefModel refModel) {
        /* if this is a URL or relative ref:
            1) we need to load it into memory.
            2) shove it into the #/definitions
            3) update the RefModel to point to its location in #/definitions
         */

        if (isAnExternalRefFormat(refModel.getRefFormat())) {
            final String newRef = processRefToExternalDefinition(refModel.get$ref(),
                    refModel.getRefFormat());

            if (newRef != null) {
                refModel.set$ref(newRef);
            }
        }
    }
}
