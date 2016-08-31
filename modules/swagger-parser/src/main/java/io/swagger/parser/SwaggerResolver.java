package io.swagger.parser;

import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.Response;
import io.swagger.models.Swagger;
import io.swagger.models.auth.AuthorizationValue;
import io.swagger.parser.processors.DefinitionsProcessor;
import io.swagger.parser.processors.OperationProcessor;
import io.swagger.parser.processors.PathsProcessor;
import io.swagger.parser.processors.PropertyProcessor;

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
    private final PropertyProcessor propertyProcessor;

    public SwaggerResolver(Swagger swagger, List<AuthorizationValue> auths, String parentFileLocation) {
        this.swagger = swagger;
        this.cache = new ResolverCache(swagger, auths, parentFileLocation);
        definitionsProcessor = new DefinitionsProcessor(cache, swagger);
        pathProcessor = new PathsProcessor(cache, swagger);
        operationsProcessor = new OperationProcessor(cache, swagger);
        propertyProcessor = new PropertyProcessor(cache, swagger);
    }

    public SwaggerResolver(Swagger swagger,  List<AuthorizationValue> auths) {
        this(swagger, auths, null);
    }

    public Swagger resolve() {
        if (swagger == null) {
            return null;
        }

        if(swagger.getResponses() != null) {
            for(String responseCode : swagger.getResponses().keySet()) {
                Response response = swagger.getResponses().get(responseCode);
                if(response.getSchema() != null) {
                    propertyProcessor.processProperty(response.getSchema());
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
}
