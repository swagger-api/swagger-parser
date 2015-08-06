package io.swagger.transform.migrate;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.transform.util.MutableJsonTree;
import io.swagger.transform.util.SwaggerMigrationException;
import io.swagger.transform.util.SwaggerMigrators;

import javax.annotation.Nonnull;

/**
 * Core interface for JSON migration
 *
 * <p>This takes a {@link JsonNode} as an input and outputs the transformed
 * {@link JsonNode}.</p>
 *
 * <p>Note that this interface makes <strong>no guarantee</strong> as to whether
 * a <em>new</em> {@code JsonNode} is returned, or the input argument is
 * returned (altered or not). If this distinction is important, please document
 * it in your implementation(s).</p>
 *
 * <p>You can either implement this interface directly or use one of the
 * predefined migrators in {@link SwaggerMigrators}.</p>
 *
 * @see SwaggerMigrators
 * @see MutableJsonTree#applyMigrator(SwaggerMigrator)
 * @see MutableJsonTree#applyMigratorToElements(SwaggerMigrator)
 */
public interface SwaggerMigrator {
    /**
     * Migrate a JSON input
     *
     * @param input the input node
     * @return the migrated node
     * @throws SwaggerMigrationException migration failed
     */
    @Nonnull
    JsonNode migrate(@Nonnull final JsonNode input)
            throws SwaggerMigrationException;
}
