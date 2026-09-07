package io.swagger.v3.parser;

import com.fasterxml.jackson.annotation.JsonInclude;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.cfg.MapperBuilder;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.yaml.YAMLFactory;
import tools.jackson.dataformat.yaml.YAMLMapper;

public class ObjectMapperFactory {
    public static ObjectMapper createJson() {
        return createJson(true, true);
    }

    protected static ObjectMapper createJson(boolean includePathDeserializer, boolean includeResponseDeserializer) {
        return configure(JsonMapper.builder(createJsonFactory())).build();
    }

    public static ObjectMapper createYaml() {
        return createYaml(true, true);
    }

    protected static ObjectMapper createYaml(boolean includePathDeserializer, boolean includeResponseDeserializer) {
        return configure(YAMLMapper.builder(createYamlFactory())).build();
    }

    private static <M extends ObjectMapper, B extends MapperBuilder<M, B>> B configure(B builder) {
        return builder
          .changeDefaultPropertyInclusion(incl -> incl
            .withValueInclusion(JsonInclude.Include.NON_NULL)
            .withContentInclusion(JsonInclude.Include.NON_NULL))
          .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
          .configure(tools.jackson.databind.cfg.DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS, false)
          .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
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
