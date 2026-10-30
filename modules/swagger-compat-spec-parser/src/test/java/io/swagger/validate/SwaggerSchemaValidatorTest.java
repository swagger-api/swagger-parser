package io.swagger.validate;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.Schema;
import io.swagger.transform.util.JacksonSupport;
import io.swagger.transform.util.SwaggerJsonSchemaFactory;

import java.io.IOException;

public abstract class SwaggerSchemaValidatorTest {
    protected final Schema schema;
    protected final JsonNode instance;

    protected SwaggerSchemaValidatorTest(final String schemaPath,
                                         final String instancePath)
            throws IOException {
        schema = SwaggerJsonSchemaFactory.getInstance().getSchema(schemaPath);
        instance = JacksonSupport.fromResource("/validate/" + instancePath);
    }
}
