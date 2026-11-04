package io.swagger.transform.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Objects;

/**
 * Jackson utilities used by the migration and validation code
 *
 * <p>The mapper configuration matches the one previously provided by
 * jackson-coreutils, so that migrated documents are serialized the same way
 * (in particular, floating point numbers are read as {@link
 * java.math.BigDecimal}s and written in plain notation).</p>
 */
public final class JacksonSupport {
    private static final ObjectMapper MAPPER = newMapper();
    private static final ObjectWriter WRITER = MAPPER.writerWithDefaultPrettyPrinter();

    private JacksonSupport() {
    }

    /**
     * Return a newly configured {@link ObjectMapper}
     *
     * @return a new mapper
     */
    public static ObjectMapper newMapper() {
        return new ObjectMapper()
                .setNodeFactory(JsonNodeFactory.instance)
                .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
                .enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN)
                .enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * Return the node factory used by mappers created by this class
     *
     * @return the node factory
     */
    public static JsonNodeFactory nodeFactory() {
        return JsonNodeFactory.instance;
    }

    /**
     * Pretty print a JSON value
     *
     * @param node the value
     * @return the pretty printed value
     */
    public static String prettyPrint(final JsonNode node) {
        Objects.requireNonNull(node, "node must not be null");
        try {
            return WRITER.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("cannot serialize JSON value", e);
        }
    }

    /**
     * Read a JSON value from a classpath resource
     *
     * @param resource the resource path; must start with {@code /}
     * @return the JSON value
     * @throws IOException resource not found, or invalid JSON
     */
    public static JsonNode fromResource(final String resource)
            throws IOException {
        Objects.requireNonNull(resource, "resource must not be null");
        if (!resource.startsWith("/")) {
            throw new IllegalArgumentException("resource path does not start with a '/'");
        }
        URL url = JacksonSupport.class.getResource(resource);
        if (url == null) {
            final ClassLoader loader = Thread.currentThread().getContextClassLoader();
            if (loader != null) {
                url = loader.getResource(resource.substring(1));
            }
        }
        if (url == null) {
            throw new IOException("resource " + resource + " not found");
        }
        try (InputStream in = url.openStream()) {
            return MAPPER.readTree(in);
        }
    }
}
