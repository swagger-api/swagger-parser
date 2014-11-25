package com.wordnik.swagger.transform;

import com.wordnik.swagger.transform.util.SwaggerMigrators;

import java.io.IOException;

public final class MembersToStringMigratorTest
    extends SwaggerMigratorTest
{
    public MembersToStringMigratorTest()
        throws IOException
    {
        super("membersToString",
            SwaggerMigrators.membersToString("foo", "bar"));
    }
}
