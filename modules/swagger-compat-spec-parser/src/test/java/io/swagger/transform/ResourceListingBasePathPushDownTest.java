package io.swagger.transform;

import io.swagger.transform.migrate.V11ResourceListingMigrator;

import java.io.IOException;

public final class ResourceListingBasePathPushDownTest
        extends SwaggerMigratorTest {
    public ResourceListingBasePathPushDownTest()
            throws IOException {
        super("basePathPushDown", new V11ResourceListingMigrator());
    }
}
