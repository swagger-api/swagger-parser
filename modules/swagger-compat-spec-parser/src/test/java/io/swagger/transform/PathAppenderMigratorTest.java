package io.swagger.transform;

import io.swagger.transform.migrate.resourcelisting.PathAppenderMigrator;

import java.io.IOException;

public final class PathAppenderMigratorTest
        extends SwaggerMigratorTest {
    public PathAppenderMigratorTest()
            throws IOException {
        super("pathAppender", new PathAppenderMigrator("http://foo.bar"));
    }
}
