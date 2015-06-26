package io.swagger.transform.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.github.fge.jackson.JacksonUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public final class APIFileDownload {
    private static final Path RESOURCE_PATH;
    private static final ObjectMapper MAPPER = JacksonUtils.newMapper();
    private static final ObjectWriter JSONWRITER
            = MAPPER.writerWithDefaultPrettyPrinter();

    private APIFileDownload() {
    }

    public static void main(final String... args)
            throws IOException {
        if (args.length != 2) {
            System.err.println("Requiring two arguments (URL, destination)");
            System.exit(2);
        }

        final URL source = new URL(args[0]);
        final Path dstFile = RESOURCE_PATH.resolve(args[1]).toAbsolutePath();

        if (!dstFile.startsWith(RESOURCE_PATH)) {
            System.err.println("Illegal destination path " + dstFile);
            System.exit(2);
        }

        Files.createDirectories(dstFile.getParent());

        try (
                final InputStream in = source.openStream();
                final BufferedWriter writer = Files.newBufferedWriter(dstFile,
                        StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        ) {
            final JsonNode node = MAPPER.readTree(in);
            JSONWRITER.writeValue(writer, node);
        }
    }

    static {
        try {
            RESOURCE_PATH = Paths.get("src/main/resources/samples")
                    .toRealPath();
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
}
