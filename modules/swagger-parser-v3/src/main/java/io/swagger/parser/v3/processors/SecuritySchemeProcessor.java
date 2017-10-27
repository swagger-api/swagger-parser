package io.swagger.parser.v3.processors;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.parser.v3.ResolverCache;
import io.swagger.parser.v3.models.RefFormat;


import static io.swagger.parser.v3.util.RefUtils.computeRefFormat;
import static io.swagger.parser.v3.util.RefUtils.isAnExternalRefFormat;

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
