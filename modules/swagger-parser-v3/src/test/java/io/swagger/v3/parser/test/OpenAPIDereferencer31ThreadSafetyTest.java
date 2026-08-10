package io.swagger.v3.parser.test;

import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;

/**
 * Verifies that concurrent parsing of OpenAPI 3.1 specs with resolve=true
 * does not cause results to be swapped between threads.
 *
 * This is a regression test for https://github.com/swagger-api/swagger-parser/issues/2293
 * where OpenAPIDereferencer31 (a singleton) stored per-call state in instance fields,
 * causing concurrent dereference() calls to overwrite each other's OpenAPI/result objects.
 */
public class OpenAPIDereferencer31ThreadSafetyTest {

    private static String makeSpec(String title) {
        return "{\n" +
                "  \"openapi\": \"3.1.0\",\n" +
                "  \"info\": { \"title\": \"" + title + "\", \"version\": \"1.0.0\" },\n" +
                "  \"paths\": {},\n" +
                "  \"components\": {\n" +
                "    \"schemas\": {\n" +
                "      \"Item\": {\n" +
                "        \"type\": \"object\",\n" +
                "        \"properties\": { \"id\": { \"type\": \"string\" } }\n" +
                "      },\n" +
                "      \"ItemList\": {\n" +
                "        \"type\": \"object\",\n" +
                "        \"properties\": {\n" +
                "          \"items\": {\n" +
                "            \"type\": \"array\",\n" +
                "            \"items\": { \"$ref\": \"#/components/schemas/Item\" }\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}";
    }

    private static String parseAndGetTitle(String spec) {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setResolveFully(false);
        SwaggerParseResult result = new OpenAPIV3Parser().readContents(spec, null, options);
        return result.getOpenAPI().getInfo().getTitle();
    }

    @Test
    public void concurrentParsingOfDifferentSpecsShouldNotCorruptResults() throws InterruptedException, ExecutionException {
        String specA = makeSpec("SpecA");
        String specB = makeSpec("SpecB");

        int threadCount = 8;
        int iterations = 200;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);

        try {
            List<Future<String>> futures = new ArrayList<>();
            for (int i = 0; i < iterations; i++) {
                final String spec = (i % 2 == 0) ? specA : specB;
                final String expectedTitle = (i % 2 == 0) ? "SpecA" : "SpecB";
                futures.add(pool.submit(() -> {
                    String actual = parseAndGetTitle(spec);
                    assertEquals("Parsed title should match input spec (concurrent race detected)", expectedTitle, actual);
                    return actual;
                }));
            }

            for (Future<String> future : futures) {
                future.get();
            }
        } finally {
            pool.shutdown();
        }
    }
}
