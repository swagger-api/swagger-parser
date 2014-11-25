package com.wordnik.swagger.transform;

import com.wordnik.swagger.transform.migrate.V11TypeMigrator;

import java.io.IOException;

public final class V11TypeMigratorTest
    extends SwaggerMigratorTest
{
    public V11TypeMigratorTest()
        throws IOException
    {
        super("dataType", new V11TypeMigrator());
    }
}
