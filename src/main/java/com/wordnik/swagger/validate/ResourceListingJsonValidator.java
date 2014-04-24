package com.wordnik.swagger.validate;

public final class ResourceListingJsonValidator
    extends SwaggerJsonValidator
{
    public ResourceListingJsonValidator()
    {
        super(new ResourceListingSchemaValidator());
    }
}
