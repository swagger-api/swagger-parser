package io.swagger.parser;

import io.swagger.oas.models.Operation;
import io.swagger.oas.models.PathItem;
import io.swagger.oas.models.Paths;
import io.swagger.oas.models.OpenAPI;
import io.swagger.models.auth.AuthorizationValue;
import io.swagger.parser.processors.DefinitionsProcessor;
import io.swagger.parser.processors.OperationProcessor;
import io.swagger.parser.processors.PathsProcessor;

import java.util.List;
/**
 * Created by gracekarina on 8/06/17.
 */
public class OpenApiResolver {

        private final OpenAPI openApi;
        private final ResolverCache cache;
        private final PathsProcessor pathProcessor;
        private final DefinitionsProcessor definitionsProcessor;
        private final OperationProcessor operationsProcessor;
        private Settings settings = new Settings();

        public OpenApiResolver(OpenAPI openApi) {
            this(openApi, null, null, null);
        }

        public OpenApiResolver(OpenAPI openApi,  List<AuthorizationValue> auths) {
            this(openApi, auths, null, null);
        }

        public OpenApiResolver(OpenAPI openApi, List<AuthorizationValue> auths, String parentFileLocation) {
            this(openApi, auths, parentFileLocation, null);
        }

        public OpenApiResolver(OpenAPI openApi, List<AuthorizationValue> auths, String parentFileLocation, Settings settings) {
            this.openApi = openApi;
            this.settings = settings != null ? settings : new Settings();
            this.cache = new ResolverCache(openApi, auths, parentFileLocation);
            definitionsProcessor = new DefinitionsProcessor(cache, swagger);
            pathProcessor = new PathsProcessor(cache, swagger, this.settings);
            operationsProcessor = new OperationProcessor(cache, swagger);
        }

        public OpenAPI resolve() {
            if (openApi == null) {
                return null;
            }

            pathProcessor.processPaths();
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
            }

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
}
