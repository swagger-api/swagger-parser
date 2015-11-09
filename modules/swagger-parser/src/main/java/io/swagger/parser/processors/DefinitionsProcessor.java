package io.swagger.parser.processors;

import io.swagger.models.Model;
import io.swagger.models.RefModel;
import io.swagger.models.Swagger;
import io.swagger.parser.ResolverCache;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public class DefinitionsProcessor {
    private final ResolverCache cache;
    private final Swagger swagger;
    private final ModelProcessor modelProcessor;

    public DefinitionsProcessor(ResolverCache cache, Swagger swagger) {

        this.cache = cache;
        this.swagger = swagger;
        modelProcessor = new ModelProcessor(cache, swagger);
    }

    public void processDefinitions(Path swaggerParentDirectory) {
        final Map<String, Model> definitions = swagger.getDefinitions();

        if (definitions == null) {
            return;
        }

        Set<String> keySet = new HashSet<>();
        keySet.addAll(definitions.keySet());

        for (String modelName : keySet) {
            final Model model = definitions.get(modelName);

            String originalRef = model instanceof RefModel ? ((RefModel) model).get$ref() : null;

            modelProcessor.processModel(model, swaggerParentDirectory);

            //if we process a RefModel here, in the #/definitions table, we want to overwrite it with the referenced value
            if (model instanceof RefModel) {
                final String renamedRef = cache.getRenamedRef(originalRef);

                if (renamedRef != null) {
                    //we definitely resolved the referenced and shoved it in the definitions map
                    final Model resolvedModel = definitions.remove(renamedRef);
                    definitions.put(modelName, resolvedModel);
                }

            }
        }
    }
}
