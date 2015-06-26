package io.swagger.validate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.github.fge.jackson.JacksonUtils;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.LogLevel;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import io.swagger.report.Message;
import io.swagger.report.MessageBuilder;
import io.swagger.report.Severity;

import java.util.EnumMap;

public abstract class SwaggerJsonValidator {
    private static final EnumMap<LogLevel, Severity> LEVEL_MAP
            = new EnumMap<>(LogLevel.class);
    private final SwaggerSchemaValidator validator;


    protected SwaggerJsonValidator(final SwaggerSchemaValidator validator) {
        this.validator = validator;
    }

    private static boolean fillMessages(final ProcessingReport report,
                                        final MessageBuilder builder) {
        final Severity severity = LEVEL_MAP.get(report.getLogLevel());
        final ArrayNode node = JacksonUtils.nodeFactory().arrayNode();

        for (final ProcessingMessage processingMessage : report) {
            node.add(processingMessage.asJson());
        }

        final String reportAsString = JacksonUtils.prettyPrint(node);
        final Message message = new Message("", reportAsString, severity);
        builder.append(message);
        return report.isSuccess();
    }

    public final void validate(final MessageBuilder builder,
                               final JsonNode input) {
        final ProcessingReport report;
        try {
            report = validator.validate(input);
            if (!fillMessages(report, builder)) {
                builder.append(new Message("", "JSON Schema validation failed",
                        Severity.ERROR));
            }
        } catch (ProcessingException e) {
            builder.append(new Message("", e.getMessage(), Severity.ERROR));
        }
    }

    static {
        LEVEL_MAP.put(LogLevel.DEBUG, Severity.OPTIONAL);
        LEVEL_MAP.put(LogLevel.INFO, Severity.OPTIONAL);
        LEVEL_MAP.put(LogLevel.ERROR, Severity.ERROR);
        LEVEL_MAP.put(LogLevel.FATAL, Severity.ERROR);
        LEVEL_MAP.put(LogLevel.WARNING, Severity.WARNING);
        LEVEL_MAP.put(LogLevel.NONE, Severity.OPTIONAL);
    }
}
