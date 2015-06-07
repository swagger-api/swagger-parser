package io.swagger.transform;

import io.swagger.transform.migrate.ApiObjectMigrator;

import java.io.IOException;

public final class ApiObjectMigratorTest
        extends SwaggerMigratorTest {
    public ApiObjectMigratorTest()
            throws IOException {
        super("apiObject", new ApiObjectMigrator());
    }
}
