package com.wordnik.swagger.transform;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jackson.JacksonUtils;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.wordnik.swagger.transform.migrate.SwaggerMigrator;
import com.wordnik.swagger.transform.util.SwaggerTransformException;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

import static org.testng.Assert.assertTrue;

@Test
public abstract class SwaggerMigratorTest
{
    private static final ObjectMapper MAPPER = JacksonUtils.newMapper();
    private static final TypeReference<List<MigrationTestData>> TYPE_REF
        = new TypeReference<List<MigrationTestData>>() {};
    private final Function<MigrationTestData, Object[]> TO_OBJECT
        = new Function<MigrationTestData, Object[]>()
    {
        @Nullable
        @Override
        public Object[] apply(@Nullable final MigrationTestData input)
        {
            return new Object[] { input };
        }
    };

    private final List<MigrationTestData> testData;
    private final SwaggerMigrator migrator;

    protected SwaggerMigratorTest(final String resource,
        final SwaggerMigrator migrator)
        throws IOException
    {
        this.migrator = migrator;

        try (
            final InputStream in = SwaggerMigratorTest.class
                .getResourceAsStream("/transform/" + resource + ".json");
        ) {
            testData = MAPPER.readValue(in, TYPE_REF);
        }
    }

    @DataProvider
    protected Iterator<Object[]> getTestData()
    {
        return Lists.transform(testData, TO_OBJECT).iterator();
    }

    @Test(dataProvider = "getTestData")
    public void migratorWorksAsExpected(final MigrationTestData data)
        throws SwaggerTransformException
    {
        /*
         * Unfortunately we cannot use assertEquals() directly :/ JsonNode
         * implements Iterable, as a result the default assertEquals() will
         * try and compare element by element, botching the test output.
         *
         * Use assertTrue() instead with a relevant failure message...
         */
        final JsonNode original = data.getOriginal();
        final JsonNode expected = data.getMigrated();
        final JsonNode actual = migrator.migrate(original);
        assertTrue(actual.equals(expected), errmsg(actual, expected));
    }

    private static String errmsg(final JsonNode actual, final JsonNode expected)
    {
        return new StringBuilder("migrator did not produce expected results!")
            .append("\nproduced:\n")
            .append(JacksonUtils.prettyPrint(actual))
            .append("\nexpected:\n")
            .append(JacksonUtils.prettyPrint(expected))
            .toString();
    }
}
