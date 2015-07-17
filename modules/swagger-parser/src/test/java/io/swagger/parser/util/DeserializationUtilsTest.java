package io.swagger.parser.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.models.Model;
import io.swagger.util.Json;
import io.swagger.util.Yaml;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Mocked;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;


/**
 * Created by russellb337 on 7/14/15.
 */
public class DeserializationUtilsTest {


    @Test
    public void testDeserializeYamlIntoObject(@Mocked Yaml yaml,
                                              @Injectable final ObjectMapper objectMapper,
                                              @Injectable final Model model) throws Exception {

        String jsonStr = "really good yaml";


        new Expectations() {{
            Yaml.mapper();
            result = objectMapper;
            times = 1;
            objectMapper.convertValue(anyString, withAny(Object.class));
            times = 1;
            result = model;
        }};

        final Model result = DeserializationUtils.deserialize(jsonStr, "foo.yaml", Model.class);
        assertEquals(model, result);
    }

    @Test
    public void testDeserializeJsonIntoObject(@Mocked Json json,
                                              @Injectable final ObjectMapper objectMapper,
                                              @Injectable final Model model) throws Exception {

        String jsonStr = "really good json";


        new Expectations() {{
            Json.mapper();
            result = objectMapper;
            times = 2;
            objectMapper.convertValue(anyString, withAny(Object.class));
            times = 2;
            result = model;
        }};

        Model result = DeserializationUtils.deserialize(jsonStr, "foo.json", Model.class);
        assertEquals(model, result);

        result = DeserializationUtils.deserialize(jsonStr, "foo", Model.class);
        assertEquals(model, result);
    }

    @Test
    public void testDeserializeYamlIntoTree(@Mocked Yaml yaml,
                                            @Injectable final ObjectMapper objectMapper,
                                            @Injectable final JsonNode jsonNode) throws Exception {

        String jsonStr = "really good yaml";

        new Expectations() {{
            Yaml.mapper();
            result = objectMapper;
            times = 1;
            objectMapper.readTree(anyString);
            result = jsonNode;
            times = 1;
        }};

        final JsonNode result = DeserializationUtils.deserializeIntoTree(jsonStr, "foo.yaml");
        assertEquals(jsonNode, result);
    }

    @Test
    public void testDeserializeJsonIntoTree(@Mocked Json json,
                                            @Injectable final ObjectMapper objectMapper,
                                            @Injectable final JsonNode jsonNode) throws Exception {

        String jsonStr = "really good json";

        new Expectations() {{
            Json.mapper();
            result = objectMapper;
            times = 2;
            objectMapper.readTree(anyString);
            result = jsonNode;
            times = 2;
        }};

        JsonNode result = DeserializationUtils.deserializeIntoTree(jsonStr, "foo.json");
        assertTrue(jsonNode == result);

        result = DeserializationUtils.deserializeIntoTree(jsonStr, "foo");
        assertTrue(jsonNode == result);
    }


}
