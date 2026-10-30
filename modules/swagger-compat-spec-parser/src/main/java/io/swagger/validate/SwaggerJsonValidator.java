package io.swagger.validate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.Error;
import com.networknt.schema.SchemaException;
import io.swagger.report.Message;
import io.swagger.report.MessageBuilder;
import io.swagger.report.Severity;
import io.swagger.transform.util.JacksonSupport;

import java.util.List;

public abstract class SwaggerJsonValidator {
    private final SwaggerSchemaValidator validator;


    protected SwaggerJsonValidator(final SwaggerSchemaValidator validator) {
        this.validator = validator;
    }

    private static boolean fillMessages(final List<Error> errors,
                                        final MessageBuilder builder) {
        final Severity severity = errors.isEmpty() ? Severity.OPTIONAL : Severity.ERROR;
        final ArrayNode node = JacksonSupport.nodeFactory().arrayNode();

        for (final Error error : errors) {
            final ObjectNode errorNode = node.addObject();
            errorNode.put("keyword", error.getKeyword());
            errorNode.put("instance", String.valueOf(error.getInstanceLocation()));
            errorNode.put("schema", String.valueOf(error.getSchemaLocation()));
            errorNode.put("message", error.getMessage());
        }

        final String reportAsString = JacksonSupport.prettyPrint(node);
        final Message message = new Message("", reportAsString, severity);
        builder.append(message);
        return errors.isEmpty();
    }

    public final void validate(final MessageBuilder builder,
                               final JsonNode input) {
        try {
            if (!fillMessages(validator.validate(input), builder)) {
                builder.append(new Message("", "JSON Schema validation failed",
                        Severity.ERROR));
            }
        } catch (SchemaException e) {
            builder.append(new Message("", e.getMessage(), Severity.ERROR));
        }
    }
}
