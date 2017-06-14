package io.swagger.parser.v3;

//package io.swagger.parser;

import io.swagger.oas.models.OpenAPI;
import io.swagger.parser.models.AuthorizationValue;
import io.swagger.parser.v3.processors.ComponentsProcessor;
/*import io.swagger.parser.processors.DefinitionsProcessor;
import io.swagger.parser.processors.OperationProcessor;
import io.swagger.parser.processors.PathsProcessor;*/

import java.util.List;

public class OpenAPIResolver {

    private final OpenAPI openApi;
    //private final ResolverCache cache;
    private final ComponentsProcessor componentsProcessor;
    /*private final PathsProcessor pathProcessor;
    private final DefinitionsProcessor definitionsProcessor;
    private final OperationProcessor operationsProcessor;*/
    private Settings settings = new Settings();

    public OpenAPIResolver(OpenAPI openApi) {
        this(openApi, null, null, null);
    }

    public OpenAPIResolver(OpenAPI openApi,  List<AuthorizationValue> auths) {
        this(openApi, auths, null, null);
    }

    public OpenAPIResolver(OpenAPI openApi, List<AuthorizationValue> auths, String parentFileLocation) {
        this(openApi, auths, parentFileLocation, null);
    }

    public OpenAPIResolver(OpenAPI openApi, List<AuthorizationValue> auths, String parentFileLocation, Settings settings) {
        this.openApi = openApi;
        this.settings = settings != null ? settings : new Settings();
        //this.cache = new ResolverCache(openApi, auths, parentFileLocation);
        componentsProcessor = new ComponentsProcessor(openApi);
        /*definitionsProcessor = new DefinitionsProcessor(cache, swagger);
        pathProcessor = new PathsProcessor(cache, swagger, this.settings);
        operationsProcessor = new OperationProcessor(cache, swagger);*/
    }

    public OpenAPI resolve() {
        if (openApi == null) {
            return null;
        }

        /*pathProcessor.processPaths();
        definitionsProcessor.processDefinitions();

        if(openApi.getPaths() != null) {
            for(String pathname : openApi.getPaths().keySet()) {
                PathItem pathItem = openApi.getPaths().get(pathname);
                if(pathItem.getOperations() != null) {
                    for(Operation operation : paths.getOperations()) {
                        operationsProcessor.processOperation(operation);
                    }
                }
            }
        }*/

        return openApi;
    }

    public static class Settings {

        private boolean addParametersToEachOperation = true;

        /**
         * If true, resource parameters are added to each operation
         */
        public boolean addParametersToEachOperation() {
            return this.addParametersToEachOperation;
        }

        /**
         * If true, resource parameters are added to each operation
         */
        public Settings addParametersToEachOperation(final boolean addParametersToEachOperation) {
            this.addParametersToEachOperation = addParametersToEachOperation;
            return this;
        }


    }
}
