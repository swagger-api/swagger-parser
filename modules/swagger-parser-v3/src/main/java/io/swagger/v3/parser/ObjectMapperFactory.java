package io.swagger.v3.parser;

import com.fasterxml.jackson.annotation.JsonInclude;
import tools.jackson.core.StreamReadConstraints;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.core.StreamWriteConstraints;
import tools.jackson.core.StreamWriteFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.cfg.EnumFeature;
import tools.jackson.databind.cfg.JsonNodeFeature;
import tools.jackson.databind.cfg.MapperBuilder;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.yaml.YAMLFactory;
import tools.jackson.dataformat.yaml.YAMLMapper;

/**
 * Builds the mappers that the parser uses to read JSON and YAML into trees.
 * Every setting that has a different default in Jackson 2 and Jackson 3 is set here on purpose.
 * See .claude/jackson-defaults-table.md for the full table.
 */
public class ObjectMapperFactory {

    /** Jackson 2 default nesting depth. Jackson 3 lowers it to 500. */
    static final int MAX_NESTING_DEPTH = 1000;

    public static ObjectMapper createJson() {
        return createJson(true, true);
    }

    protected static ObjectMapper createJson(boolean includePathDeserializer, boolean includeResponseDeserializer) {
        JsonMapper.Builder builder = JsonMapper.builder(
                tools.jackson.core.json.JsonFactory.builder()
                        // Jackson 2 and Jackson 3 both have this off by default. We need it on.
                        .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
                        .streamReadConstraints(StreamReadConstraints.builder().maxNestingDepth(MAX_NESTING_DEPTH).build())
                        .streamWriteConstraints(StreamWriteConstraints.builder().maxNestingDepth(MAX_NESTING_DEPTH).build())
                        .build());
        return configure(builder).build();
    }

    public static ObjectMapper createYaml() {
        return createYaml(true, true);
    }

    protected static ObjectMapper createYaml(boolean includePathDeserializer, boolean includeResponseDeserializer) {
        YAMLMapper.Builder builder = YAMLMapper.builder(
                YAMLFactory.builder()
                        .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
                        .streamReadConstraints(StreamReadConstraints.builder().maxNestingDepth(MAX_NESTING_DEPTH).build())
                        .streamWriteConstraints(StreamWriteConstraints.builder().maxNestingDepth(MAX_NESTING_DEPTH).build())
                        .build());
        return configure(builder).build();
    }

    private static <M extends ObjectMapper, B extends MapperBuilder<M, B>> B configure(B builder) {
        return applyJackson2Defaults(builder)
                .changeDefaultPropertyInclusion(i -> i.withValueInclusion(JsonInclude.Include.NON_NULL))
                // Settings the Jackson 2 version of this factory changed from the Jackson 2 default.
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                .configure(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * Sets every feature whose default is different in Jackson 3 back to the Jackson 2 default.
     * Settings that the factory changes on purpose are applied after this call.
     */
    public static <M extends ObjectMapper, B extends MapperBuilder<M, B>> B applyJackson2Defaults(B builder) {
        return builder
                .configure(DeserializationFeature.FAIL_ON_TRAILING_TOKENS, false)
                .configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false)
                .configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, false)
                .configure(JsonNodeFeature.STRIP_TRAILING_BIGDECIMAL_ZEROES, true)
                .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, false)
                .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, false)
                .configure(EnumFeature.WRITE_ENUMS_USING_TO_STRING, false)
                .configure(StreamWriteFeature.WRITE_BIGDECIMAL_AS_PLAIN, false);
    }
}
