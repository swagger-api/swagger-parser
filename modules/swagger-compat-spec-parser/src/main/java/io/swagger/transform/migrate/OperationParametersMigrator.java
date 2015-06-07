package io.swagger.transform.migrate;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.transform.util.SwaggerMigrationException;

import javax.annotation.Nonnull;

/**
 * Migrator for one parameter object or an operation object
 */
public final class OperationParametersMigrator
        implements SwaggerMigrator {
    private final SwaggerMigrator typeMigrator = new V11TypeMigrator();
    private final SwaggerMigrator allowableValuesMigrator
            = new V11AllowableValuesMigrator();

    @Nonnull
    @Override
    public JsonNode migrate(@Nonnull final JsonNode input)
            throws SwaggerMigrationException {
        return typeMigrator.migrate(allowableValuesMigrator.migrate(input));
    }
}
