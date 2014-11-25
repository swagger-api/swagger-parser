package com.wordnik.swagger.transform.migrate;

import com.fasterxml.jackson.databind.JsonNode;
import com.wordnik.swagger.report.Message;
import com.wordnik.swagger.report.MessageBuilder;
import com.wordnik.swagger.report.Severity;
import com.wordnik.swagger.transform.util.SwaggerMigrationException;

public abstract class SwaggerJsonMigrator
{
    protected final SwaggerMigrator migrator;

    protected SwaggerJsonMigrator(final SwaggerMigrator migrator)
    {
        this.migrator = migrator;
    }

    public final JsonNode migrate(final MessageBuilder builder,
        final JsonNode node)
    {
        try {
            return migrator.migrate(node);
        } catch (SwaggerMigrationException e) {
            builder.append(new Message("", e.getMessage(), Severity.ERROR));
            return node;
        }
    }
}

