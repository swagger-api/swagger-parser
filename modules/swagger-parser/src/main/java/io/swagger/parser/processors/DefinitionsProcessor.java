package io.swagger.parser.processors;

import io.swagger.models.Model;
import io.swagger.models.Swagger;
import io.swagger.parser.ResolverCache;

import java.util.Map;


public class DefinitionsProcessor {
    private final ResolverCache cache;
    private final Swagger swagger;
    private final ModelProcessor modelProcessor;

    public DefinitionsProcessor(ResolverCache cache, Swagger swagger) {

        this.cache = cache;
        this.swagger = swagger;
        modelProcessor = new ModelProcessor(cache, swagger);
    }

    public void processDefinitions() {
        final Map<String, Model> definitions = swagger.getDefinitions();

        if (definitions == null) {
            return;
        }

        for (Map.Entry<String, Model> definitionEntry : definitions.entrySet()) {
            final Model model = definitionEntry.getValue();
            //if we process a RefModel here, in the #/definitions table, we want to overwrite it with the referenced value
            modelProcessor.processModel(model);
        }
    }
}
