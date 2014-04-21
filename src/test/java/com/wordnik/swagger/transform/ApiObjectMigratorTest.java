package com.wordnik.swagger.transform;

import com.wordnik.swagger.transform.migrate.ApiObjectMigrator;

import java.io.IOException;

public final class ApiObjectMigratorTest
    extends SwaggerMigratorTest
{
    public ApiObjectMigratorTest()
        throws IOException
    {
        super("apiObject", new ApiObjectMigrator());
    }
}
