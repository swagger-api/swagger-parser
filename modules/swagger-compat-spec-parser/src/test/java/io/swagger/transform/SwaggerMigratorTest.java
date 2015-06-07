package io.swagger.transform;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jackson.JacksonUtils;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import io.swagger.transform.migrate.SwaggerMigrator;
import io.swagger.transform.util.SwaggerMigrationException;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

@Test
public abstract class SwaggerMigratorTest {
    private static final ObjectMapper MAPPER = JacksonUtils.newMapper();
    private static final TypeReference<List<MigrationTestData>> TESTDATA_TYPEREF
            = new TypeReference<List<MigrationTestData>>() {
    };
    private static final TypeReference<List<MigrationErrorData>> ERRDATA_TYPEREF
            = new TypeReference<List<MigrationErrorData>>() {
    };

    private final List<MigrationTestData> testData;
    private final List<MigrationErrorData> errorData;

    private final SwaggerMigrator migrator;

    protected SwaggerMigratorTest(final String resource,
                                  final SwaggerMigrator migrator)
            throws IOException {
        this.migrator = migrator;

        try (
                final InputStream in = SwaggerMigratorTest.class
                        .getResourceAsStream("/transform/" + resource + ".json");
                final InputStream in2 = SwaggerMigratorTest.class
                        .getResourceAsStream("/transform/" + resource + "-errs.json");
        ) {
            testData = MAPPER.readValue(in, TESTDATA_TYPEREF);
            if (in2 == null) {
                errorData = ImmutableList.of();
            } else {
                errorData = MAPPER.readValue(in2, ERRDATA_TYPEREF);
            }
        }
    }

    private static String errmsg(final JsonNode actual, final JsonNode expected) {
        return new StringBuilder("migrator did not produce expected results!")
                .append("\nproduced:\n")
                .append(JacksonUtils.prettyPrint(actual))
                .append("\nexpected:\n")
                .append(JacksonUtils.prettyPrint(expected))
                .toString();
    }

    private static <T> Function<T, Object[]> toObject() {
        return new Function<T, Object[]>() {
            @Nullable
            @Override
            public Object[] apply(@Nullable final T input) {
                return new Object[]{input};
            }
        };
    }

    @DataProvider
    protected Iterator<Object[]> getTestData() {
        return Lists.transform(testData, toObject()).iterator();
    }

    @Test(dataProvider = "getTestData")
    public void migratorWorksAsExpected(final MigrationTestData data)
            throws SwaggerMigrationException {
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

    @DataProvider
    protected Iterator<Object[]> getErrorData() {
        return Lists.transform(errorData, toObject()).iterator();
    }

    @Test(dataProvider = "getErrorData")
    public void errorsAreCorrectlyIdentifiedAndReported(
            final MigrationErrorData data) {
        final JsonNode node = data.getOriginal();
        final String errmsg = data.getErrorMessage();

        try {
            migrator.migrate(node);
            fail("No exception thrown!!");
        } catch (SwaggerMigrationException e) {
            assertEquals(e.getMessage(), errmsg,
                    "error message differs from expectations");
        }
    }
}
