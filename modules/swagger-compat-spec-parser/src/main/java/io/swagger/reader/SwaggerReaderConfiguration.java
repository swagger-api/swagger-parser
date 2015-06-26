package io.swagger.reader;

import javax.annotation.Nonnull;
import java.util.Objects;

public final class SwaggerReaderConfiguration {
    String proxy;

    public SwaggerReaderConfiguration useProxy(@Nonnull final String proxy) {
        this.proxy = Objects.requireNonNull(proxy);
        return this;
    }
}

