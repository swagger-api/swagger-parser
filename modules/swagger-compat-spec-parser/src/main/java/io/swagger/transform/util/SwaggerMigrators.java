package io.swagger.transform.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jackson.JacksonUtils;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import com.google.common.collect.ImmutableSet;
import io.swagger.transform.migrate.SwaggerMigrator;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.util.Objects;
import java.util.Set;

/**
 * Utility {@link SwaggerMigrator}s
 */
@ParametersAreNonnullByDefault
public final class SwaggerMigrators {
    private static final ObjectMapper MAPPER = JacksonUtils.newMapper();

    /**
     * Return a migrator converting object member values to JSON Strings
     *
     * <p>Note that this will only work if member values are
     * <strong>not</strong> containers (ie, JSON Arrays or Objects).</p>
     *
     * @param first  first member name
     * @param others one or more other member names
     * @return a migrator
     * @see MembersToString
     */
    public static SwaggerMigrator membersToString(
            final String first, final String... others) {
        return new MembersToString(first, others);
    }

    /**
     * Return a migrator applying a <a
     * href="tools.ietf.org/html/rfc6902">JSON Patch</a>
     *
     * <p>The JSON Patch must be deserialized at this point. You can also load
     * one from the classpath using {@link #patchFromResource(String)}.</p>
     *
     * @param patch the JSON patch to apply
     * @return a migrator
     * @see JsonPatch
     */
    public static SwaggerMigrator fromPatch(final JsonPatch patch) {
        Objects.requireNonNull(patch, "patch cannot be null");
        return new SwaggerMigrator() {
            @Nonnull
            @Override
            public JsonNode migrate(@Nonnull final JsonNode input)
                    throws SwaggerMigrationException {
                Objects.requireNonNull(input);
                try {
                    return patch.apply(input);
                } catch (JsonPatchException e) {
                    throw new SwaggerMigrationException(e);
                }
            }
        };
    }

    /**
     * Return a migrator renaming object members
     *
     * <p>Note that this migrator will not fail if the member to rename does not
     * exists; however it <strong>will</strong> fail if the <em>target</em>
     * member already exists in the target JSON Object.</p>
     *
     * @param from the member name to rename
     * @param to   the new name
     * @return a migrator
     */
    public static SwaggerMigrator renameMember(final String from,
                                               final String to) {
        Objects.requireNonNull(from);
        Objects.requireNonNull(to);

        return new SwaggerMigrator() {
            @Nonnull
            @Override
            public JsonNode migrate(@Nonnull final JsonNode input)
                    throws SwaggerMigrationException {
                Objects.requireNonNull(input);
                if (!input.has(from)) {
                    return input;
                }
                if (input.has(to)) {
                    throw new SwaggerMigrationException("object already has a "
                            + "member named \"" + to + '"');
                }
                final ObjectNode ret = input.deepCopy();
                ret.put(to, ret.get(from));
                ret.remove(from);
                return ret;
            }
        };
    }

    /**
     * Return a migrator applying a JSON Patch as read from the classpath
     *
     * @param resourcePath the resource path
     * @return a migrator
     * @see #fromPatch(JsonPatch)
     */
    public static SwaggerMigrator patchFromResource(final String resourcePath) {
        Objects.requireNonNull(resourcePath);

        final JsonNode node;
        final JsonPatch patch;

        try {
            node = JsonLoader.fromResource(resourcePath);
            patch = MAPPER.readValue(node.traverse(), JsonPatch.class);
        } catch (IOException e) {
            throw new RuntimeException("cannot load resource", e);
        }

        return fromPatch(patch);
    }

    /**
     * Migrator converting object member values to JSON Strings
     *
     * <p>Important notes:</p>
     *
     * <ul>
     * <li>this migrator will fail if at least one specified member has a
     * "container" value (ie, the value is a JSON Array or an Objet);</li>
     * <li>the {@link JsonNode} given as an input is unaltered; a modified
     * copy is returned.</li>
     * </ul>
     */
    public static final class MembersToString
            implements SwaggerMigrator {
        private final Set<String> memberNames;

        private MembersToString(final String first, final String... others) {
            memberNames = ImmutableSet.<String>builder()
                    .add(first).add(others).build();
        }

        @Nonnull
        @Override
        public JsonNode migrate(@Nonnull final JsonNode input)
                throws SwaggerMigrationException {
            Objects.requireNonNull(input, "input cannot be null");
            final ObjectNode ret = input.deepCopy();
            JsonNode node;

            for (final String memberName : memberNames) {
                node = input.path(memberName);
                if (node.isMissingNode()) {
                    continue;
                }
                if (node.isContainerNode()) {
                    throw new SwaggerMigrationException("operation does not "
                            + "apply to JSON arrays or objects");
                }
                if (!node.isTextual()) {
                    ret.put(memberName, node.asText());
                }
            }

            return ret;
        }
    }
}

