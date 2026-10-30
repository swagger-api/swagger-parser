package io.swagger.transform.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public class JsonPatchTest {
    private static final ObjectMapper MAPPER = JacksonSupport.newMapper();

    private static JsonNode json(final String json) throws IOException {
        return MAPPER.readTree(json.replace('\'', '"'));
    }

    private static JsonPatch patch(final String json) throws IOException {
        return MAPPER.readValue(json.replace('\'', '"'), JsonPatch.class);
    }

    private static void assertJson(final JsonNode actual, final String expected) throws IOException {
        assertEquals(actual, json(expected));
    }

    private static List<String> fieldNames(final JsonNode node) {
        final List<String> names = new ArrayList<>();
        final Iterator<String> it = node.fieldNames();
        while (it.hasNext()) {
            names.add(it.next());
        }
        return names;
    }

    private static void assertFails(final JsonPatch patch, final String input) throws IOException {
        try {
            patch.apply(json(input));
            fail("patch should not apply");
        } catch (SwaggerMigrationException expected) {
            // expected
        }
    }

    @Test
    public void addToObjectAndArray() throws Exception {
        final JsonPatch patch = patch("[{'op':'add','path':'/a','value':1},"
                + "{'op':'add','path':'/arr/1','value':'x'},"
                + "{'op':'add','path':'/arr/-','value':'z'}]");
        assertJson(patch.apply(json("{'arr':['b','c']}")), "{'arr':['b','x','c','z'],'a':1}");
    }

    @Test
    public void addOnExistingMemberKeepsPosition() throws Exception {
        final JsonNode result = patch("[{'op':'add','path':'/a','value':2}]").apply(json("{'a':1,'b':1}"));
        assertJson(result, "{'a':2,'b':1}");
        assertEquals(fieldNames(result).get(0), "a");
    }

    @Test
    public void addToRootReplacesDocument() throws Exception {
        assertJson(patch("[{'op':'add','path':'','value':{'b':2}}]").apply(json("{'a':1}")), "{'b':2}");
    }

    @Test
    public void addFailsWithoutParent() throws Exception {
        assertFails(patch("[{'op':'add','path':'/missing/a','value':1}]"), "{}");
        assertFails(patch("[{'op':'add','path':'/a/b','value':1}]"), "{'a':1}");
        assertFails(patch("[{'op':'add','path':'/arr/5','value':1}]"), "{'arr':[]}");
    }

    @Test
    public void removeAndReplace() throws Exception {
        final JsonPatch patch = patch("[{'op':'remove','path':'/a'},"
                + "{'op':'replace','path':'/b','value':'new'},"
                + "{'op':'remove','path':'/arr/0'}]");
        final JsonNode result = patch.apply(json("{'a':1,'b':2,'c':3,'arr':[1,2]}"));
        assertJson(result, "{'b':'new','c':3,'arr':[2]}");
        assertEquals(fieldNames(result).get(0), "b");
    }

    @Test
    public void removeAndReplaceFailOnMissingPath() throws Exception {
        assertFails(patch("[{'op':'remove','path':'/a'}]"), "{}");
        assertFails(patch("[{'op':'replace','path':'/a','value':1}]"), "{}");
        assertFails(patch("[{'op':'remove','path':'/arr/3'}]"), "{'arr':[1]}");
    }

    @Test
    public void moveRenamesAndAppendsMember() throws Exception {
        final JsonNode result = patch("[{'op':'move','from':'/dataType','path':'/$ref'}]")
                .apply(json("{'dataType':'Pet','description':'d'}"));
        assertJson(result, "{'description':'d','$ref':'Pet'}");
        assertEquals(fieldNames(result).get(1), "$ref");
    }

    @Test
    public void moveOverwritesExistingTarget() throws Exception {
        assertJson(patch("[{'op':'move','from':'/a','path':'/b'}]").apply(json("{'a':1,'b':2}")), "{'b':1}");
    }

    @Test
    public void moveToSamePathIsNoop() throws Exception {
        assertJson(patch("[{'op':'move','from':'/a','path':'/a'}]").apply(json("{'a':1}")), "{'a':1}");
    }

    @Test
    public void moveFailures() throws Exception {
        assertFails(patch("[{'op':'move','from':'/missing','path':'/b'}]"), "{'a':1}");
        assertFails(patch("[{'op':'move','from':'/a','path':'/a/b'}]"), "{'a':{}}");
    }

    @Test
    public void escapedPointerTokens() throws Exception {
        assertJson(patch("[{'op':'add','path':'/a~1b','value':1},{'op':'remove','path':'/c~0d'}]")
                .apply(json("{'c~d':0}")), "{'a/b':1}");
    }

    @Test
    public void inputAndPatchValueAreNotModified() throws Exception {
        final JsonPatch patch = patch("[{'op':'add','path':'/v','value':{'x':1}}]");
        final JsonNode input = json("{'a':1}");
        final JsonNode first = patch.apply(input);
        ((ObjectNode) first.get("v")).put("x", 2);
        assertJson(input, "{'a':1}");
        assertJson(patch.apply(input), "{'a':1,'v':{'x':1}}");
    }

    @Test
    public void failedPatchLeavesInputUntouched() throws Exception {
        final JsonNode input = json("{'a':1}");
        try {
            patch("[{'op':'remove','path':'/a'},{'op':'remove','path':'/missing'}]").apply(input);
            fail("patch should not apply");
        } catch (SwaggerMigrationException expected) {
            // expected
        }
        assertJson(input, "{'a':1}");
    }

    @Test
    public void readsMapOfPatches() throws Exception {
        final Map<String, JsonPatch> patches = MAPPER.readValue(
                JacksonSupport.fromResource("/patches/v1.1/dataType.json").traverse(),
                new TypeReference<Map<String, JsonPatch>>() {
                });
        assertTrue(patches.size() > 0);
    }

    @Test
    public void invalidPatchesAreRejectedWhenRead() {
        final String[] invalid = {
                "{'op':'add','path':'/a','value':1}",
                "[{'op':'copy','from':'/a','path':'/b'}]",
                "[{'op':'test','path':'/a','value':1}]",
                "[{'op':'Add','path':'/a','value':1}]",
                "[{'op':'add','path':'/a'}]",
                "[{'op':'remove'}]",
                "[{'op':'move','path':'/a'}]",
                "[{'op':'remove','path':'a'}]"
        };
        for (final String json : invalid) {
            try {
                patch(json);
                fail("patch should be rejected: " + json);
            } catch (IOException expected) {
                // expected
            }
        }
    }
}
