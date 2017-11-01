package io.swagger.v3.parser.processors;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.parser.ResolverCache;
import io.swagger.v3.parser.models.RefFormat;


import static io.swagger.v3.parser.util.RefUtils.computeRefFormat;

/**
 * Created by gracekarina on 23/06/17.
 */
public class SecuritySchemeProcessor {
    private final ResolverCache cache;
    private final OpenAPI openAPI;
    private final ExternalRefProcessor externalRefProcessor;

    public SecuritySchemeProcessor(ResolverCache cache, OpenAPI openAPI) {
        this.cache = cache;
        this.openAPI = openAPI;
        this.externalRefProcessor = new ExternalRefProcessor(cache, openAPI);
    }

    public SecurityScheme processSecurityScheme(SecurityScheme securityScheme) {

        if (securityScheme.get$ref() != null){
            RefFormat refFormat = computeRefFormat(securityScheme.get$ref());
            String $ref = securityScheme.get$ref();
            SecurityScheme newSecurityScheme = cache.loadRef($ref, refFormat, SecurityScheme.class);
            if (newSecurityScheme != null) {
                return newSecurityScheme;
            }
        }
        return securityScheme;

    }
}
