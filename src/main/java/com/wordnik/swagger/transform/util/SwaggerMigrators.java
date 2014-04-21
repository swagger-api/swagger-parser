package com.wordnik.swagger.transform.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jackson.JacksonUtils;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import com.wordnik.swagger.transform.migrate.SwaggerMigrator;
import com.google.common.collect.ImmutableSet;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Objects;
import java.util.Set;

public final class SwaggerMigrators
{
    private static final ObjectMapper MAPPER = JacksonUtils.newMapper();

    private static final class MembersToString
        implements SwaggerMigrator
    {
        private final Set<String> memberNames;

        private MembersToString(final String first, final String... others)
        {
            memberNames = ImmutableSet.<String>builder()
                .add(first).add(others).build();
        }

        @Nonnull
        @Override
        public JsonNode migrate(@Nonnull final JsonNode input)
            throws SwaggerTransformException
        {
            Objects.requireNonNull(input, "input cannot be null");
            final ObjectNode ret = input.deepCopy();
            JsonNode node;

            for (final String memberName: memberNames) {
                node = input.path(memberName);
                if (node.isMissingNode())
                    continue;
                if (node.isContainerNode())
                    throw new UncheckedSwaggerTransformException(
                        "operation does not apply to container nodes");
                if (!node.isTextual())
                    ret.put(memberName, node.asText());
            }

            return ret;
        }
    }

    public static SwaggerMigrator membersToString(
        final String first, final String... others)
    {
        return new MembersToString(first, others);
    }

    public static SwaggerMigrator fromPatch(
        @Nonnull final JsonPatch patch)
    {
        Objects.requireNonNull(patch, "patch cannot be null");
        return new SwaggerMigrator()
        {
            @Nonnull
            @Override
            public JsonNode migrate(@Nonnull final JsonNode input)
                throws SwaggerTransformException
            {
                Objects.requireNonNull(input);
                try {
                    return patch.apply(input);
                } catch (JsonPatchException e) {
                    throw new SwaggerTransformException(e);
                }
            }
        };
    }

    public static SwaggerMigrator renameMember(@Nonnull final String from,
        @Nonnull final String to)
    {
        Objects.requireNonNull(from);
        Objects.requireNonNull(to);

        return new SwaggerMigrator()
        {
            @Nonnull
            @Override
            public JsonNode migrate(@Nonnull final JsonNode input)
            {
                Objects.requireNonNull(input);
                if (!input.has(from))
                    return input;
                if (input.has(to))
                    throw new UncheckedSwaggerTransformException();
                final ObjectNode ret = input.deepCopy();
                ret.put(to, ret.get(from));
                ret.remove(from);
                return ret;
            }
        };
    }

    public static SwaggerMigrator patchFromResource(
        @Nonnull final String resourcePath)
    {
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
}

