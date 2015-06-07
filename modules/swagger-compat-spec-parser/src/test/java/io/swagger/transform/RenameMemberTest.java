package io.swagger.transform;

import io.swagger.transform.util.SwaggerMigrators;

import java.io.IOException;

public final class RenameMemberTest
        extends SwaggerMigratorTest {
    public RenameMemberTest()
            throws IOException {
        super("renameMember", SwaggerMigrators.renameMember("foo", "bar"));
    }
}
