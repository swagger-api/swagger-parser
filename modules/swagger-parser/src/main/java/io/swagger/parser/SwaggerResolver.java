package io.swagger.parser;

import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.Swagger;
import io.swagger.models.auth.AuthorizationValue;
import io.swagger.parser.processors.DefinitionsProcessor;
import io.swagger.parser.processors.OperationProcessor;
import io.swagger.parser.processors.PathsProcessor;
import io.swagger.parser.util.PathUtils;

import java.io.File;
import java.util.List;

/**
 *
 */
public class SwaggerResolver {

    private final Swagger              swagger;
    private final ResolverCache        cache;
    private final PathsProcessor       pathProcessor;
    private final DefinitionsProcessor definitionsProcessor;
    private final OperationProcessor   operationsProcessor;
    private final java.nio.file.Path   parentDirectory;

    public SwaggerResolver(Swagger swagger,
                           List<AuthorizationValue> auths,
                           String parentFileLocation)
    {
        this.swagger = swagger;
        this.cache = new ResolverCache(swagger, auths/*, parentFileLocation*/);
        definitionsProcessor = new DefinitionsProcessor(cache, swagger);
        pathProcessor = new PathsProcessor(cache, swagger);
        operationsProcessor = new OperationProcessor(cache, swagger);
        if(parentFileLocation != null) {
            parentDirectory = PathUtils.getParentDirectoryOfFile(parentFileLocation);
        } else {
            File file = new File(".");
            parentDirectory = file.toPath();
        }
    }

    public SwaggerResolver(Swagger swagger,  List<AuthorizationValue> auths) {
        this(swagger, auths, null);
    }

    public Swagger resolve() {
        if (swagger == null) {
            return null;
        }

        pathProcessor.processPaths(parentDirectory);
        definitionsProcessor.processDefinitions(parentDirectory);

        if(swagger.getPaths() != null) {
            for(String pathname : swagger.getPaths().keySet()) {
                Path path = swagger.getPaths().get(pathname);
                if(path.getOperations() != null) {
                    for(Operation operation : path.getOperations()) {
                        operationsProcessor.processOperation(operation, parentDirectory);
                    }
                }
            }
        }

        return swagger;
    }
}
