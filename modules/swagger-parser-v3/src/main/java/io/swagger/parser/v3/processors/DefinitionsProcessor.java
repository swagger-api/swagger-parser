package io.swagger.parser.v3.processors;


import io.swagger.oas.models.OpenAPI;
import io.swagger.oas.models.media.Schema;
import io.swagger.parser.ResolverCache;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;


public class DefinitionsProcessor {
    private final ResolverCache cache;
    private final OpenAPI openApi;
    private final SchemaProcessor schemaProcessor;

    public DefinitionsProcessor(ResolverCache cache, OpenAPI openApi) {

        this.cache = cache;
        this.openApi = openApi;
        schemaProcessor = new SchemaProcessor(cache, openApi);
    }

    public void processDefinitions() {
        final Map<String, Schema> schemas = openApi.getComponents().getSchemas();

        if (schemas == null) {
            return;
        }

        Set<String> keySet = new LinkedHashSet<>();

        // the definitions can grow as we resolve references
        while(schemas.keySet().size() > keySet.size()) {
            processDefinitions(keySet, schemas);
        }
    }

    public void processDefinitions(Set<String> schemaKeys, Map<String, Schema> schemas) {
        schemaKeys.addAll(schemas.keySet());

        for (String schemaName : schemaKeys) {
            final Schema schema = schemas.get(schemaName);

            //String originalRef = schema instanceof RefModel ? ((RefModel) schema).get$ref() : null;

            schemaProcessor.processSchema(schema);

            //if we process a RefModel here, in the #/definitions table, we want to overwrite it with the referenced value
            if (schema.get$ref() != null) {
                //final String renamedRef = cache.getRenamedRef(originalRef);

                /*if (renamedRef != null) {
                    //we definitely resolved the referenced and shoved it in the definitions map
                    // because the referenced model may be in the definitions map, we need to remove old instances
                    final Schema resolvedSchema = schemas.get(renamedRef);

                    // ensure the reference isn't still in use
                    if(!cache.hasReferencedKey(renamedRef)) {
                        schemas.remove(renamedRef);
                    }

                    // add the new key
                    schemas.put(schemaName, resolvedSchema);
                }*/
            }
        }
    }
}