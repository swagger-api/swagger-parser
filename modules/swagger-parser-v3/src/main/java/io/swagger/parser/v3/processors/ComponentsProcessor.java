package io.swagger.parser.v3.processors;

import io.swagger.oas.models.OpenAPI;
import io.swagger.oas.models.media.Schema;
import io.swagger.parser.ResolverCache;




/**
 * Created by gracekarina on 13/06/17.
 */
public class ComponentsProcessor {
    //private final ResolverCache cache;
    private final OpenAPI openApi;

    public ComponentsProcessor(OpenAPI openApi){
        this.openApi = openApi;
        System.out.println("Components");
    }


}
