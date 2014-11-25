package com.wordnik.swagger.transform;

import com.wordnik.swagger.transform.migrate.V11ResourceListingMigrator;

import java.io.IOException;

public final class ResourceListingBasePathPushDownTest
    extends SwaggerMigratorTest
{
    public ResourceListingBasePathPushDownTest()
        throws IOException
    {
        super("basePathPushDown", new V11ResourceListingMigrator());
    }
}
