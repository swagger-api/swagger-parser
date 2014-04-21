package com.github.fge.swagger.util;

import com.github.fge.jsonschema.core.load.configuration.LoadingConfiguration;
import com.github.fge.jsonschema.core.load.uri.URITranslatorConfiguration;
import com.github.fge.jsonschema.main.JsonSchemaFactory;

public final class SwaggerJsonSchemaFactory
{
    private static final String BASE_URI
        = "http://wordnik.github.io/schemas/";
    private static final String ROOT_CONTEXT = "resource:/schemas/";

    private static final SwaggerJsonSchemaFactory INSTANCE
        = new SwaggerJsonSchemaFactory();

    private final JsonSchemaFactory factory;

    public static SwaggerJsonSchemaFactory getInstance()
    {
        return INSTANCE;
    }

    private SwaggerJsonSchemaFactory()
    {
        final URITranslatorConfiguration translatorConfiguration
            = URITranslatorConfiguration.newBuilder()
            .setNamespace(ROOT_CONTEXT)
            .addPathRedirect(BASE_URI, ROOT_CONTEXT)
            .freeze();
        final LoadingConfiguration loadingConfiguration
            = LoadingConfiguration.newBuilder()
            .setURITranslatorConfiguration(translatorConfiguration)
            .freeze();
        factory = JsonSchemaFactory.newBuilder()
            .setLoadingConfiguration(loadingConfiguration).freeze();
    }

    public JsonSchemaFactory getFactory()
    {
        return factory;
    }
}
