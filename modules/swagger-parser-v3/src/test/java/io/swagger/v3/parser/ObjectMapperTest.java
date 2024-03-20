package io.swagger.v3.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.Assert;
import org.junit.Test;

public class ObjectMapperTest {
    @Test
    public void testJavaTimeModule() {
        ObjectMapper mapper = ObjectMapperFactory.createJson();
        Assert.assertTrue("JavaTimeModule found?",
                mapper.getRegisteredModuleIds().contains(new JavaTimeModule().getTypeId()));
    }
}
