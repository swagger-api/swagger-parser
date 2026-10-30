package io.swagger.v3.parser;

import org.junit.Assert;
import org.junit.Test;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public class ObjectMapperTest {
    // Jackson 3 ships java.time support in databind, no JavaTimeModule to register.
    @Test
    public void testJavaTimeSupport() {
        ObjectMapper mapper = ObjectMapperFactory.createJson();
        Assert.assertEquals("\"2024-01-02\"", mapper.writeValueAsString(LocalDate.of(2024, 1, 2)));
        Assert.assertEquals("\"2024-01-02T03:04:05Z\"",
                mapper.writeValueAsString(OffsetDateTime.of(2024, 1, 2, 3, 4, 5, 0, ZoneOffset.UTC)));
    }
}
