package io.swagger.v3.parser;


import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.parser.core.models.AuthorizationValue;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import io.swagger.v3.parser.processors.ComponentsProcessor;
import io.swagger.v3.parser.processors.OperationProcessor;
import io.swagger.v3.parser.processors.PathsProcessor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class OpenAPIResolver {

    private final OpenAPI openApi;
    private final ResolverCache cache;
    private final ComponentsProcessor componentsProcessor;
    private final PathsProcessor pathProcessor;
    private final OperationProcessor operationsProcessor;
    private Settings settings = new Settings();
    private Set<String> resolveValidationMessages = new HashSet<>();

    public ResolverCache getCache() {
        return cache;
    }

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
        this(openApi, auths, parentFileLocation, settings, new ParseOptions());
    }

    public OpenAPIResolver(OpenAPI openApi, List<AuthorizationValue> auths, String parentFileLocation, Settings settings, ParseOptions parseOptions) {
        this.openApi = openApi;
        this.settings = settings != null ? settings : new Settings();
        this.cache = new ResolverCache(openApi, auths, parentFileLocation, resolveValidationMessages, parseOptions);
        componentsProcessor = new ComponentsProcessor(openApi,this.cache);
        pathProcessor = new PathsProcessor(cache, openApi,this.settings);
        operationsProcessor = new OperationProcessor(cache, openApi);
    }

    public void resolve(SwaggerParseResult result) {

        OpenAPI resolved = resolve();
        if (resolved == null) {
            return;
        }
        result.setOpenAPI(resolved);
        result.getMessages().addAll(resolveValidationMessages);
    }

    public OpenAPI resolve() {
        if (openApi == null) {
            return null;
        }

        pathProcessor.processPaths();
        componentsProcessor.processComponents();


        if(openApi.getPaths() != null) {
            for(String pathname : openApi.getPaths().keySet()) {
                PathItem pathItem = openApi.getPaths().get(pathname);
                if(pathItem.readOperations() != null) {
                    for(Operation operation : pathItem.readOperations()) {
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
