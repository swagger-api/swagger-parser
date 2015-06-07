package io.swagger.validate;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import io.swagger.transform.util.SwaggerJsonSchemaFactory;

public abstract class SwaggerSchemaValidator {
    protected static final JsonSchemaFactory FACTORY
            = SwaggerJsonSchemaFactory.getInstance().getFactory();

    private final JsonSchema schema;

    protected SwaggerSchemaValidator(final String schemaPath) {
        try {
            schema = FACTORY.getJsonSchema(schemaPath);
        } catch (ProcessingException e) {
            throw new RuntimeException("Unhandled exception", e);
        }
    }

    public final ProcessingReport validate(final JsonNode input)
            throws ProcessingException {
        return schema.validate(input, true);
    }
}
