package io.swagger.validate;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.Error;
import com.networknt.schema.Schema;
import io.swagger.transform.util.SwaggerJsonSchemaFactory;

import java.util.List;

public abstract class SwaggerSchemaValidator {
    private final Schema schema;

    protected SwaggerSchemaValidator(final String schemaPath) {
        schema = SwaggerJsonSchemaFactory.getInstance().getSchema(schemaPath);
    }

    public final List<Error> validate(final JsonNode input) {
        return schema.validate(input);
    }
}
