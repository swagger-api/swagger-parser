package io.swagger.transform.migrate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jackson.JacksonUtils;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import com.google.common.base.Optional;
import io.swagger.transform.util.SwaggerMigrationException;
import io.swagger.transform.util.SwaggerMigrators;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;

/**
 * Patch a 1.1 dataType into a 1.2 type
 *
 * <p>Several Swagger objects have the possibility of a data type; as such, JSON
 * Schema validation currently cannot really limit the object members present,
 * not without the "strictProperties" and "merge" proposals of draft v5.</p>
 *
 * <p>The schema validation performed is therefore extremely simple: we only
 * check that a {@code dataType} field is present, check that its value is one
 * of the allowed values, and patch the node.</p>
 *
 * <p>If {@code dataType} is not a known primitive, it is considered to be a
 * v1.2 {@code $ref}.</p>
 */
public final class V11TypeMigrator
        implements SwaggerMigrator {
    private static final ObjectMapper MAPPER = JacksonUtils.newMapper();
    private static final TypeReference<Map<String, JsonPatch>> TYPE_REF
            = new TypeReference<Map<String, JsonPatch>>() {
    };
    private static final JsonPatch DEFAULT_PATCH;
    private final Map<String, JsonPatch> patches;
    private final JsonPatch fileTypePatch;
    public V11TypeMigrator() {
        try {
            JsonNode node;

            node = JsonLoader.fromResource("/patches/v1.1/dataType.json");
            patches = MAPPER.readValue(node.traverse(), TYPE_REF);

            node = JsonLoader.fromResource("/patches/other/fileTypePatch.json");
            fileTypePatch = MAPPER.readValue(node.traverse(), JsonPatch.class);
        } catch (IOException e) {
            throw new RuntimeException("failed to load the necessary file", e);
        }
    }

    @Nonnull
    @Override
    public JsonNode migrate(@Nonnull final JsonNode input)
            throws SwaggerMigrationException {
        Objects.requireNonNull(input);
        final JsonNode node = input.path("dataType");
        if (node.isMissingNode()) // FIXME...
        {
            return postMigrate(input);
        }
        if (!node.isTextual()) {
            throw new SwaggerMigrationException("dataType is not a text field");
        }
        final String dataType = node.textValue();
        final JsonPatch patch = Optional.fromNullable(patches.get(dataType))
                .or(DEFAULT_PATCH);
        final JsonNode migrated = SwaggerMigrators.fromPatch(patch)
                .migrate(input);
        return postMigrate(migrated);
    }

    private JsonNode postMigrate(final JsonNode input)
            throws SwaggerMigrationException {
        final JsonNode typeNode = input.path("type");
        if (!typeNode.isTextual()) {
            return input;
        }
        if (!"File".equals(typeNode.textValue())) {
            return input;
        }
        try {
            return fileTypePatch.apply(input);
        } catch (JsonPatchException e) {
            throw new SwaggerMigrationException(e);
        }
    }

    static {
        final String op
                = "[{"
                + "\"op\":\"move\","
                + "\"from\":\"/dataType\","
                + "\"path\":\"/$ref\""
                + "}]";
        try {
            DEFAULT_PATCH = MAPPER.readValue(op, JsonPatch.class);
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
}
