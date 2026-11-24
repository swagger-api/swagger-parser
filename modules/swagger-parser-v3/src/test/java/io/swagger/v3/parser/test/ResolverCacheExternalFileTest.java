package io.swagger.v3.parser.test;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.ResolverCache;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.models.RefFormat;
import io.swagger.v3.parser.util.DeserializationUtils;
import mockit.Invocation;
import mockit.Mock;
import mockit.MockUp;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNotSame;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public class ResolverCacheExternalFileTest {

    private static final String DEFINITIONS = "openapi: 3.0.1\n"
            + "info:\n"
            + "  title: definitions\n"
            + "  version: '1'\n"
            + "paths: {}\n"
            + "components:\n"
            + "  schemas:\n"
            + "    First:\n"
            + "      type: object\n"
            + "      example:\n"
            + "        name: first\n"
            + "        tags: [a, b]\n"
            + "      properties:\n"
            + "        id:\n"
            + "          type: string\n"
            + "          default: one\n"
            + "    Holder:\n"
            + "      type: object\n"
            + "      properties:\n"
            + "        inner:\n"
            + "          type: object\n"
            + "          example:\n"
            + "            name: inner\n"
            + "    Second:\n"
            + "      type: object\n"
            + "      example:\n"
            + "        name: second\n"
            + "      properties:\n"
            + "        id:\n"
            + "          type: integer\n";

    private Path directory;
    private Path rootFile;
    private AtomicInteger treeParseCount;

    @BeforeMethod
    public void setUp() throws IOException {
        directory = Files.createTempDirectory("resolver-cache-external-file");
        rootFile = directory.resolve("root.yaml");
        Files.write(rootFile, "openapi: 3.0.1\n".getBytes(StandardCharsets.UTF_8));
        Files.write(directory.resolve("definitions.yaml"), DEFINITIONS.getBytes(StandardCharsets.UTF_8));
        treeParseCount = new AtomicInteger();
        new MockUp<DeserializationUtils>() {
            @Mock
            public JsonNode deserializeIntoTree(Invocation invocation, String contents, String fileOrHost,
                                                ParseOptions options,
                                                io.swagger.v3.parser.core.models.SwaggerParseResult result) {
                treeParseCount.incrementAndGet();
                return invocation.proceed(contents, fileOrHost, options, result);
            }
        };
    }

    @AfterMethod
    public void tearDown() throws IOException {
        Files.deleteIfExists(directory.resolve("definitions.yaml"));
        Files.deleteIfExists(rootFile);
        Files.deleteIfExists(directory);
    }

    private ResolverCache newCache(boolean validateExternalRefs) {
        ParseOptions options = new ParseOptions();
        options.setValidateExternalRefs(validateExternalRefs);
        return new ResolverCache(new OpenAPI(), new ArrayList<>(), rootFile.toString(), new HashSet<>(), options);
    }

    private Schema load(ResolverCache cache, String name) {
        return cache.loadRef("definitions.yaml#/components/schemas/" + name, RefFormat.RELATIVE, Schema.class);
    }

    @Test
    public void parsesExternalFileOnceForManyRefs() {
        ResolverCache cache = newCache(false);

        assertNotNull(load(cache, "First"));
        assertNotNull(load(cache, "Second"));
        assertNotNull(load(cache, "First"));

        assertEquals(treeParseCount.get(), 1);
    }

    @Test
    public void parsesExternalFileOnceForManyRefsWithValidation() {
        ResolverCache cache = newCache(true);

        assertNotNull(load(cache, "First"));
        assertNotNull(load(cache, "Second"));

        assertEquals(treeParseCount.get(), 1);
    }

    @Test
    public void sameRefIsStillResolvedToSameInstance() {
        ResolverCache cache = newCache(false);

        assertSame(load(cache, "First"), load(cache, "First"));
    }

    @Test
    public void resultsMatchResolutionWithFreshCache() {
        for (boolean validate : new boolean[]{false, true}) {
            ResolverCache shared = newCache(validate);
            Schema firstShared = load(shared, "First");
            Schema secondShared = load(shared, "Second");

            assertEquals(firstShared, load(newCache(validate), "First"));
            assertEquals(secondShared, load(newCache(validate), "Second"));
        }
    }

    @Test
    public void overlappingRefsDoNotShareMutableNodes() {
        for (boolean validate : new boolean[]{false, true}) {
            ResolverCache cache = newCache(validate);
            Schema holder = load(cache, "Holder");
            Schema inner = cache.loadRef("definitions.yaml#/components/schemas/Holder/properties/inner",
                    RefFormat.RELATIVE, Schema.class);
            Schema innerOfHolder = (Schema) holder.getProperties().get("inner");
            assertNotNull(innerOfHolder.getExample());
            assertNotNull(inner.getExample());
            assertNotSame(innerOfHolder.getExample(), inner.getExample());

            ((com.fasterxml.jackson.databind.node.ObjectNode) innerOfHolder.getExample()).put("name", "changed");

            assertEquals(((com.fasterxml.jackson.databind.JsonNode) inner.getExample()).get("name").asText(), "inner");
        }
    }

    @Test
    public void validationMessagesOfExternalFileAreStillReported() throws IOException {
        String invalid = "openapi: 3.0.1\n"
                + "components:\n"
                + "  schemas:\n"
                + "    Broken:\n"
                + "      type: object\n"
                + "      properties:\n"
                + "        id:\n"
                + "          type: string\n"
                + "          minLength: not-a-number\n";
        Files.write(directory.resolve("definitions.yaml"), invalid.getBytes(StandardCharsets.UTF_8));

        Set<String> messagesWithFreshCaches = new HashSet<>();
        List<ResolverCache> caches = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            ParseOptions options = new ParseOptions();
            options.setValidateExternalRefs(true);
            Set<String> messages = new HashSet<>();
            ResolverCache cache = new ResolverCache(new OpenAPI(), new ArrayList<>(), rootFile.toString(), messages, options);
            cache.loadRef("definitions.yaml#/components/schemas/Broken", RefFormat.RELATIVE, Schema.class);
            messagesWithFreshCaches.addAll(messages);
            caches.add(cache);
        }

        Set<String> messagesWithSharedCache = new HashSet<>();
        ParseOptions options = new ParseOptions();
        options.setValidateExternalRefs(true);
        ResolverCache shared = new ResolverCache(new OpenAPI(), new ArrayList<>(), rootFile.toString(), messagesWithSharedCache, options);
        shared.loadRef("definitions.yaml#/components/schemas/Broken", RefFormat.RELATIVE, Schema.class);
        shared.loadRef("definitions.yaml#/components/schemas/Broken", RefFormat.RELATIVE, Schema.class);

        assertEquals(messagesWithSharedCache, messagesWithFreshCaches);
    }

    @Test
    public void missingFragmentStillFails() {
        ResolverCache cache = newCache(false);
        assertNotNull(load(cache, "First"));

        try {
            load(cache, "Missing");
            fail("expected an exception for a missing fragment");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("Could not find components/schemas/Missing"), e.getMessage());
        }
    }
}
