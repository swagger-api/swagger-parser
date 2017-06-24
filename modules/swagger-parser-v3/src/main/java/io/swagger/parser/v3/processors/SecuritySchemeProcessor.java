package io.swagger.parser.v3.processors;

import io.swagger.oas.models.OpenAPI;
import io.swagger.oas.models.security.SecurityScheme;
import io.swagger.parser.v3.ResolverCache;
import io.swagger.parser.v3.models.RefFormat;

import java.util.Map;

import static io.swagger.parser.v3.util.RefUtils.computeRefFormat;

/**
 * Created by gracekarina on 23/06/17.
 */
public class SecuritySchemeProcessor {
    private final ResolverCache cache;
    private final OpenAPI openApi;

    public SecuritySchemeProcessor(ResolverCache cache, OpenAPI openApi) {
        this.cache = cache;
        this.openApi = openApi;
    }

    public void processSecurityScheme(String name, SecurityScheme securityScheme) {

        if (securityScheme.get$ref() != null){
            RefFormat refFormat = computeRefFormat(securityScheme.get$ref());
            String $ref = securityScheme.get$ref();
            SecurityScheme newSecurityScheme = cache.loadRef($ref, refFormat, SecurityScheme.class);
            //TODO what if the example is not in components?
            openApi.getComponents().getSecuritySchemes().replace(name,securityScheme,newSecurityScheme);
        }
    }
}
