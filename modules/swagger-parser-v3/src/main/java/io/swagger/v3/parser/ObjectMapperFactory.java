package io.swagger.v3.parser;

import com.fasterxml.jackson.annotation.JsonInclude;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.cfg.MapperBuilder;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.yaml.YAMLFactory;
import tools.jackson.dataformat.yaml.YAMLMapper;

public class ObjectMapperFactory {
    public static ObjectMapper createJson() {
        return createJson(true, true);
    }

    protected static ObjectMapper createJson(boolean includePathDeserializer, boolean includeResponseDeserializer) {
        return create(JsonMapper.builder(createJsonFactory()), includePathDeserializer, includeResponseDeserializer);
    }

    public static ObjectMapper createYaml() {
        return createYaml(true, true);
    }

    protected static ObjectMapper createYaml(boolean includePathDeserializer, boolean includeResponseDeserializer) {
        return create(YAMLMapper.builder(createYamlFactory()), includePathDeserializer, includeResponseDeserializer);
    }

    private static ObjectMapper create(MapperBuilder<?, ?> builder, boolean includePathDeserializer, boolean includeResponseDeserializer) {
        builder.changeDefaultPropertyInclusion(i -> i.withValueInclusion(JsonInclude.Include.NON_NULL));
        builder.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        builder.configure(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        builder.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return builder.build();
    }

    private static JsonFactory createJsonFactory() {
        return JsonFactory.builder()
          .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
          .build();
    }

    private static YAMLFactory createYamlFactory() {
        return YAMLFactory.builder()
          .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
          .build();
    }
}
