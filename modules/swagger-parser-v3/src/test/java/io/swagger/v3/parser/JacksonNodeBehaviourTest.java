package io.swagger.v3.parser;

import org.testng.annotations.Test;
import tools.jackson.databind.*;
import tools.jackson.databind.json.JsonMapper;
import java.util.*;
import java.util.function.Function;

/**
 * Records what Jackson 3.2.4 really does for the text accessors of JsonNode. The expected table is
 * the evidence for the choices in OpenAPIDeserializer: textValue() became stringValue(null)
 * (same as Jackson 2: null for any node that is not a string), and asText() became
 * NodeTextUtils.asText() where the node can be null, missing, an object or an array.
 * If a Jackson upgrade changes one line, this test fails and the choices must be reviewed.
 */
public class JacksonNodeBehaviourTest {
    @Test
    public void recordedBehaviour() {
        ObjectMapper m = JsonMapper.builder().build();
        Map<String, JsonNode> nodes = new LinkedHashMap<>();
        nodes.put("string", m.readTree("\"abc\""));
        nodes.put("number", m.readTree("12"));
        nodes.put("double", m.readTree("1.5"));
        nodes.put("boolean", m.readTree("true"));
        nodes.put("object", m.readTree("{\"a\":1}"));
        nodes.put("array", m.readTree("[1]"));
        nodes.put("null", m.readTree("null"));
        nodes.put("missing", m.readTree("{}").path("x"));
        Map<String, Function<JsonNode, Object>> ops = new LinkedHashMap<>();
        ops.put("stringValue()", JsonNode::stringValue);
        ops.put("stringValue(null)", n -> n.stringValue(null));
        ops.put("asString()", JsonNode::asString);
        ops.put("asString(null)", n -> n.asString(null));
        ops.put("asString(def)", n -> n.asString("def"));
        ops.put("asText()", JsonNode::asText);
        ops.put("asText(null)", n -> n.asText(null));
        ops.put("asText(def)", n -> n.asText("def"));
        ops.put("textValue()", JsonNode::textValue);
        ops.put("asBoolean()", JsonNode::asBoolean);
        ops.put("asInt()", JsonNode::asInt);
        ops.put("asInt(7)", n -> n.asInt(7));
        StringBuilder sb = new StringBuilder("PROBE\n");
        for (var op : ops.entrySet()) for (var n : nodes.entrySet()) {
            String r;
            try { Object o = op.getValue().apply(n.getValue()); r = o == null ? "null" : "'" + o + "'"; }
            catch (Exception e) { r = "THROWS " + e.getClass().getSimpleName(); }
            sb.append(op.getKey()).append(" on ").append(n.getKey()).append(" -> ").append(r).append('\n');
        }
        String expected = """
            stringValue() on string -> 'abc'
            stringValue() on number -> THROWS JsonNodeException
            stringValue() on double -> THROWS JsonNodeException
            stringValue() on boolean -> THROWS JsonNodeException
            stringValue() on object -> THROWS JsonNodeException
            stringValue() on array -> THROWS JsonNodeException
            stringValue() on null -> null
            stringValue() on missing -> THROWS JsonNodeException
            stringValue(null) on string -> 'abc'
            stringValue(null) on number -> null
            stringValue(null) on double -> null
            stringValue(null) on boolean -> null
            stringValue(null) on object -> null
            stringValue(null) on array -> null
            stringValue(null) on null -> null
            stringValue(null) on missing -> null
            asString() on string -> 'abc'
            asString() on number -> '12'
            asString() on double -> '1.5'
            asString() on boolean -> 'true'
            asString() on object -> THROWS JsonNodeException
            asString() on array -> THROWS JsonNodeException
            asString() on null -> ''
            asString() on missing -> ''
            asString(null) on string -> 'abc'
            asString(null) on number -> '12'
            asString(null) on double -> '1.5'
            asString(null) on boolean -> 'true'
            asString(null) on object -> null
            asString(null) on array -> null
            asString(null) on null -> null
            asString(null) on missing -> null
            asString(def) on string -> 'abc'
            asString(def) on number -> '12'
            asString(def) on double -> '1.5'
            asString(def) on boolean -> 'true'
            asString(def) on object -> 'def'
            asString(def) on array -> 'def'
            asString(def) on null -> 'def'
            asString(def) on missing -> 'def'
            asText() on string -> 'abc'
            asText() on number -> '12'
            asText() on double -> '1.5'
            asText() on boolean -> 'true'
            asText() on object -> THROWS JsonNodeException
            asText() on array -> THROWS JsonNodeException
            asText() on null -> ''
            asText() on missing -> ''
            asText(null) on string -> 'abc'
            asText(null) on number -> '12'
            asText(null) on double -> '1.5'
            asText(null) on boolean -> 'true'
            asText(null) on object -> null
            asText(null) on array -> null
            asText(null) on null -> null
            asText(null) on missing -> null
            asText(def) on string -> 'abc'
            asText(def) on number -> '12'
            asText(def) on double -> '1.5'
            asText(def) on boolean -> 'true'
            asText(def) on object -> 'def'
            asText(def) on array -> 'def'
            asText(def) on null -> 'def'
            asText(def) on missing -> 'def'
            textValue() on string -> 'abc'
            textValue() on number -> THROWS JsonNodeException
            textValue() on double -> THROWS JsonNodeException
            textValue() on boolean -> THROWS JsonNodeException
            textValue() on object -> THROWS JsonNodeException
            textValue() on array -> THROWS JsonNodeException
            textValue() on null -> null
            textValue() on missing -> THROWS JsonNodeException
            asBoolean() on string -> THROWS JsonNodeException
            asBoolean() on number -> 'true'
            asBoolean() on double -> THROWS JsonNodeException
            asBoolean() on boolean -> 'true'
            asBoolean() on object -> THROWS JsonNodeException
            asBoolean() on array -> THROWS JsonNodeException
            asBoolean() on null -> 'false'
            asBoolean() on missing -> 'false'
            asInt() on string -> THROWS JsonNodeException
            asInt() on number -> '12'
            asInt() on double -> '1'
            asInt() on boolean -> THROWS JsonNodeException
            asInt() on object -> THROWS JsonNodeException
            asInt() on array -> THROWS JsonNodeException
            asInt() on null -> '0'
            asInt() on missing -> '0'
            asInt(7) on string -> '7'
            asInt(7) on number -> '12'
            asInt(7) on double -> '1'
            asInt(7) on boolean -> '7'
            asInt(7) on object -> '7'
            asInt(7) on array -> '7'
            asInt(7) on null -> '7'
            asInt(7) on missing -> '7'
            """;
        org.testng.Assert.assertEquals(sb.toString().replace("PROBE\n", "").trim(), expected.trim().replace("            ", ""));
    }
}
