package io.swagger.transform.util;

public final class SwaggerMigrationException
        extends Exception {
    public SwaggerMigrationException() {
    }

    public SwaggerMigrationException(final String message) {
        super(message);
    }

    public SwaggerMigrationException(final Throwable cause) {
        super(cause);
    }
}
