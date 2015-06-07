package io.swagger.transform.migrate;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.report.Message;
import io.swagger.report.MessageBuilder;
import io.swagger.report.Severity;
import io.swagger.transform.util.SwaggerMigrationException;

public abstract class SwaggerJsonMigrator {
    protected final SwaggerMigrator migrator;

    protected SwaggerJsonMigrator(final SwaggerMigrator migrator) {
        this.migrator = migrator;
    }

    public final JsonNode migrate(final MessageBuilder builder,
                                  final JsonNode node) {
        try {
            return migrator.migrate(node);
        } catch (SwaggerMigrationException e) {
            builder.append(new Message("", e.getMessage(), Severity.ERROR));
            return node;
        }
    }
}

