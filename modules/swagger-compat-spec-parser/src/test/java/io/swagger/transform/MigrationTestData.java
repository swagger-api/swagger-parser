package io.swagger.transform;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public final class MigrationTestData {
    private final JsonNode original;
    private final JsonNode migrated;

    @JsonCreator
    public MigrationTestData(@JsonProperty("original") final JsonNode original,
                             @JsonProperty("migrated") final JsonNode migrated) {
        this.original = original;
        this.migrated = migrated;
    }

    public JsonNode getOriginal() {
        return original;
    }

    public JsonNode getMigrated() {
        return migrated;
    }
}
