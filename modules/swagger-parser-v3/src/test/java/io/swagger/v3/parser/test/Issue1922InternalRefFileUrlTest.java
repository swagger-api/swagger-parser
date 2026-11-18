package io.swagger.v3.parser.test;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import org.testng.annotations.Test;

import java.io.File;
import java.nio.file.Paths;

import static org.testng.Assert.assertEquals;

/**
 * Regression test for https://github.com/swagger-api/swagger-parser/issues/1922
 *
 * For an OAS 3.1 document that contains only internal ("#/components/...") references, parsing with
 * {@code resolve=true} and {@code resolveFully=false} must leave the internal {@code $ref}s intact —
 * regardless of whether the root document is loaded by a filesystem path or by a {@code file://} URL.
 *
 * Today the result diverges: loaded by PATH the {@code $ref} is preserved (correct); loaded by
 * {@code file://} URL the OAS 3.1 dereferencer inlines the referenced schema and the {@code $ref} is
 * lost. Downstream code generators (openapi-generator, quarkus-openapi-generator) that pass the spec
 * as a URL therefore see every reused component duplicated per use-site, with the canonical component
 * left generated-but-orphaned.
 */
public class Issue1922InternalRefFileUrlTest {

    private static final String SPEC = "src/test/resources/3.1.0/issue-1922-internal-ref-reuse.yaml";

    private static ParseOptions resolveButNotFully() {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);        // resolve references...
        options.setResolveFully(false);  // ...but do NOT inline them
        options.setFlatten(false);
        return options;
    }

    /** The internal $ref at FeeListResponse.fees.items, or null if it was inlined. */
    private static String feesItemsRef(String location) {
        OpenAPI api = new OpenAPIV3Parser().readLocation(location, null, resolveButNotFully()).getOpenAPI();
        Schema<?> fees = (Schema<?>) api.getComponents().getSchemas().get("FeeListResponse").getProperties().get("fees");
        return fees.getItems() == null ? null : fees.getItems().get$ref();
    }

    @Test(description = "internal $ref is preserved when loaded by filesystem path (baseline)")
    public void internalRefPreservedWhenLoadedByPath() {
        String path = new File(SPEC).getAbsolutePath();
        assertEquals(feesItemsRef(path), "#/components/schemas/FeeValues");
    }

    @Test(description = "internal $ref must also be preserved when loaded by file:// URL (issue #1922)")
    public void internalRefPreservedWhenLoadedByFileUrl() {
        String fileUrl = Paths.get(SPEC).toAbsolutePath().toUri().toString();
        // FAILS today: the OAS 3.1 dereferencer inlines the internal $ref on file:// load, so this is null.
        assertEquals(feesItemsRef(fileUrl), "#/components/schemas/FeeValues");
    }

    @Test(description = "path and file:// URL must produce identical handling of internal $refs")
    public void pathAndFileUrlAgree() {
        String path = new File(SPEC).getAbsolutePath();
        String fileUrl = Paths.get(SPEC).toAbsolutePath().toUri().toString();
        assertEquals(feesItemsRef(fileUrl), feesItemsRef(path));
    }
}
