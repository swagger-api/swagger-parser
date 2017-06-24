package io.swagger.parser.v3.processors;

import io.swagger.oas.models.OpenAPI;
import io.swagger.oas.models.callbacks.Callback;
import io.swagger.parser.v3.ResolverCache;

/**
 * Created by gracekarina on 23/06/17.
 */
public class CallbackProcessor {
    private final ResolverCache cache;
    private final OpenAPI openApi;

    public CallbackProcessor(ResolverCache cache, OpenAPI openApi) {
        this.cache = cache;
        this.openApi = openApi;
    }

    public void processCallback(String callbackName, Callback callback) {
    }
}
