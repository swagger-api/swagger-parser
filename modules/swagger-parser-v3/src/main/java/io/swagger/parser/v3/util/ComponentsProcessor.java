package io.swagger.parser.v3.util;

import io.swagger.oas.models.OpenAPI;

/**
 * Created by gracekarina on 14/06/17.
 */
public class ComponentsProcessor {
        OpenAPI openApi;
    public ComponentsProcessor(OpenAPI openApi){
        this.openApi = openApi;
        System.out.println("En Components");
    }
}
