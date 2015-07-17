package io.swagger.parser;

import io.swagger.models.*;
import io.swagger.models.auth.AuthorizationValue;
import io.swagger.parser.processors.DefinitionsProcessor;
import io.swagger.parser.processors.ModelProcessor;
import io.swagger.parser.processors.PathsProcessor;

import java.util.List;
import java.util.Map;

/**
 *
 */
public class SwaggerResolver {

    private final Swagger swagger;
    private final ResolverCache cache;
    private final PathsProcessor pathProcessor;
    private final DefinitionsProcessor definitionsProcessor;

    public SwaggerResolver(Swagger swagger, List<AuthorizationValue> auths) {
        this.swagger = swagger;
        this.cache = new ResolverCache(swagger, auths);
        definitionsProcessor = new DefinitionsProcessor(cache, swagger);
        pathProcessor = new PathsProcessor(cache, swagger);
    }


    public Swagger resolve() {
        if (swagger == null) {
            return null;
        }

        pathProcessor.processPaths();
        definitionsProcessor.processDefinitions();

        return swagger;
    }
}
