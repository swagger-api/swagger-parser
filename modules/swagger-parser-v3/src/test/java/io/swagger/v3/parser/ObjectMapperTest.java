package io.swagger.v3.parser;

import org.junit.Assert;
import org.junit.Test;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;

public class ObjectMapperTest {

    /**
     * Jackson 3 has java.time support built in, so there is no JavaTimeModule to look for.
     * Check the behaviour instead: dates are written as ISO-8601 text, not as numbers.
     */
    @Test
    public void testJavaTimeSupportWithoutModule() {
        for (ObjectMapper mapper : new ObjectMapper[]{ObjectMapperFactory.createJson(), ObjectMapperFactory.createYaml()}) {
            Assert.assertEquals("\"2020-01-02\"", asJson(mapper, LocalDate.of(2020, 1, 2)));
            Assert.assertEquals("\"2020-01-02T03:04:05Z\"",
                    asJson(mapper, OffsetDateTime.of(2020, 1, 2, 3, 4, 5, 0, ZoneOffset.UTC)));
            Assert.assertEquals("\"1970-01-01T00:00:00Z\"", asJson(mapper, Instant.EPOCH));
            // Jackson 3 writes java.util.Date with Z; Jackson 2 wrote +00:00. Same instant, harmless.
            Assert.assertEquals("\"1970-01-01T00:00:00.000Z\"", asJson(mapper, new Date(0)));
        }
    }

    private static String asJson(ObjectMapper mapper, Object value) {
        // The YAML mapper writes YAML; use its tree to compare the scalar value in a format-neutral way.
        String s = mapper.writeValueAsString(value).trim();
        if (s.startsWith("---")) {
            s = s.substring(3).trim();
        }
        return s.startsWith("\"") ? s : "\"" + s + "\"";
    }
}
