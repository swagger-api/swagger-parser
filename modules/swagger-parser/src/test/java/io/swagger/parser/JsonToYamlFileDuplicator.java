package io.swagger.parser;


import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.parser.util.DeserializationUtils;
import io.swagger.util.Yaml;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;

public class JsonToYamlFileDuplicator {
    private static final Logger LOGGER = LoggerFactory.getLogger(JsonToYamlFileDuplicator.class);

    public static void duplicateFilesInYamlFormat(String inputDirectoryStr, String outputDirectoryStr) {
        Path outputDirectory = Paths.get(outputDirectoryStr);
        Path inputDirectory = Paths.get(inputDirectoryStr);

        deleteAndRecreateOutputDirectory(outputDirectory);

        final Iterator<File> fileIterator = FileUtils.iterateFiles(inputDirectory.toFile(), new String[]{"json"}, true);
        while (fileIterator.hasNext()) {
            File next = fileIterator.next();
            System.out.println("Processing " + next);

            processFile(next, inputDirectory, outputDirectory);
        }

    }

    private static void processFile(File next, Path inputDirectory, Path outputDirectory) {
        try {
            String fileContents = IOUtils.toString(new FileInputStream(next));
            fileContents = fileContents.replaceAll("\\.json", ".yaml");

            final JsonNode jsonNode = DeserializationUtils.deserializeIntoTree(fileContents, next.toString());

            final String yamlOutput = Yaml.mapper().writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode)
                    .replaceAll("\\n", System.getProperty("line.separator"));

            final String relativePath = "./" + next.toString().replace(inputDirectory.toString(), "").replace(".json", ".yaml");

            final Path outputFile = outputDirectory.resolve(relativePath).normalize();
            LOGGER.debug("output file: " + outputFile);

            final File file = outputFile.toAbsolutePath().toFile();
            FileUtils.forceMkdir(outputFile.getParent().toFile());
            FileUtils.write(file, yamlOutput);

        } catch (IOException e) {
            throw new RuntimeException("Could not process file " + next, e);
        }

    }

    private static void deleteAndRecreateOutputDirectory(Path outputDirectory) {
        try {
            FileUtils.deleteDirectory(outputDirectory.toFile());
            outputDirectory.toFile().mkdirs();
        } catch (IOException e) {
            throw new RuntimeException("Unable to delete directory: " + outputDirectory, e);
        }
    }
}
