package com.wordnik.swagger.validate;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.wordnik.swagger.transform.util.SwaggerJsonSchemaFactory;

import java.io.IOException;

public abstract class SwaggerSchemaValidatorTest
{
    private final JsonSchemaFactory FACTORY
        = SwaggerJsonSchemaFactory.getInstance().getFactory();

    protected final JsonSchema schema;
    protected final JsonNode instance;

    protected SwaggerSchemaValidatorTest(final String schemaURI,
        final String instancePath)
        throws IOException, ProcessingException
    {
        schema = FACTORY.getJsonSchema(schemaURI);
        instance = JsonLoader.fromResource("/validate/" + instancePath);
    }
}
