package io.swagger.reader;

import javax.annotation.Nonnull;

public final class SwaggerReaderFactory {
    private final String proxy;

    public SwaggerReaderFactory(@Nonnull final SwaggerReaderConfiguration cfg) {
        proxy = cfg.proxy;
    }

    public SwaggerReader newReader() {
        return proxy == null
                ? new SimpleSwaggerReader()
                : new ProxySwaggerReader(proxy);
    }
}
