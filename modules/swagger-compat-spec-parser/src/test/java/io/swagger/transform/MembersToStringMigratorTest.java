package io.swagger.transform;

import io.swagger.transform.util.SwaggerMigrators;

import java.io.IOException;

public final class MembersToStringMigratorTest
        extends SwaggerMigratorTest {
    public MembersToStringMigratorTest()
            throws IOException {
        super("membersToString",
                SwaggerMigrators.membersToString("foo", "bar"));
    }
}
