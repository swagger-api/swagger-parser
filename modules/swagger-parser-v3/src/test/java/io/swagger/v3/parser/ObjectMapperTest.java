package io.swagger.v3.parser;

import tools.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import java.time.LocalDate;

public class ObjectMapperTest {
    @Test
    public void testJavaTimeSupport() {
        // Jackson 3 has java.time support built in (no JavaTimeModule needed);
        // WRITE_DATES_AS_TIMESTAMPS is disabled so dates serialize as ISO strings
        ObjectMapper mapper = ObjectMapperFactory.createJson();
        Assert.assertEquals("\"2024-01-15\"", mapper.writeValueAsString(LocalDate.of(2024, 1, 15)));
    }
}