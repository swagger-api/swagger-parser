package io.swagger.transform;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public final class MigrationErrorData {
    private final JsonNode original;
    private final String errorMessage;

    @JsonCreator
    public MigrationErrorData(@JsonProperty("original") final JsonNode original,
                              @JsonProperty("errorMessage") final String errorMessage) {
        this.original = original;
        this.errorMessage = errorMessage;
    }

    public JsonNode getOriginal() {
        return original;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
