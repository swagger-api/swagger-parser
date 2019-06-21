package io.swagger.parser;

import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.Swagger;
import io.swagger.models.auth.AuthorizationValue;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.parameters.RefParameter;
import io.swagger.models.refs.RefFormat;
import io.swagger.parser.processors.DefinitionsProcessor;
import io.swagger.parser.processors.OperationProcessor;
import io.swagger.parser.processors.ParameterProcessor;
import io.swagger.parser.processors.PathsProcessor;

import java.util.Arrays;
import java.util.List;

/**
 *
 */
public class SwaggerResolver {
    private final Swagger swagger;
    private final ResolverCache cache;
    private final PathsProcessor pathProcessor;
    private final DefinitionsProcessor definitionsProcessor;
    private final OperationProcessor operationsProcessor;
    private final ParameterProcessor parametersProcessor;
    private Settings settings = new Settings();

    public SwaggerResolver(Swagger swagger) {
        this(swagger, null, null, null);
    }

    public SwaggerResolver(Swagger swagger,  List<AuthorizationValue> auths) {
        this(swagger, auths, null, null);
    }

    public SwaggerResolver(Swagger swagger, List<AuthorizationValue> auths, String parentFileLocation) {
        this(swagger, auths, parentFileLocation, null);
    }

    public SwaggerResolver(Swagger swagger, List<AuthorizationValue> auths, String parentFileLocation, Settings settings) {
        this.swagger = swagger;
        this.settings = settings != null ? settings : new Settings();
        this.cache = new ResolverCache(swagger, auths, parentFileLocation);
        definitionsProcessor = new DefinitionsProcessor(cache, swagger);
        pathProcessor = new PathsProcessor(cache, swagger, this.settings);
        operationsProcessor = new OperationProcessor(cache, swagger);
        parametersProcessor = new ParameterProcessor(cache, swagger);
    }

    public Swagger resolve() {
        if (swagger == null) {
            return null;
        }

        if (swagger.getParameters() != null) {
            for(String paramname : swagger.getParameters().keySet()) {
                Parameter param = swagger.getParameters().get(paramname);
                if (param instanceof RefParameter && ((RefParameter) param).getRefFormat() == RefFormat.RELATIVE) {
                    swagger.getParameters().put(paramname, parametersProcessor.processParameters(Arrays.asList(param)).get(0));
                }
            }
        }

        pathProcessor.processPaths();
        definitionsProcessor.processDefinitions();

        if(swagger.getPaths() != null) {
            for(String pathname : swagger.getPaths().keySet()) {
                Path path = swagger.getPaths().get(pathname);
                if(path.getOperations() != null) {
                    for(Operation operation : path.getOperations()) {
                        operationsProcessor.processOperation(operation);
                    }
                }
            }
        }

        return swagger;
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
