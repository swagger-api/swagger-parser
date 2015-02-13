package io.swagger.reader;


import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.io.Authentication;
import io.swagger.report.MessageBuilder;

import javax.annotation.Nonnull;
import java.util.Objects;

final class ProxySwaggerReader implements SwaggerReader {
    private final String proxy;

    ProxySwaggerReader(@Nonnull final String proxy) {
        this.proxy = Objects.requireNonNull(proxy);
    }


    @Override
    public JsonNode read(String url, Authentication authentication, MessageBuilder messageBuilder) {
        return null;
    }
}
