package io.swagger.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.util.Json;
import io.swagger.util.Yaml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractParser {
    static Logger LOGGER = LoggerFactory.getLogger(AbstractParser.class);

    public JsonNode stringToNode(String contents) throws UnparseableContentException {
        if(contents == null) {
            return null;
        }
        try {
            ObjectMapper mapper;
            if(contents.trim().startsWith("{")) {
                mapper = Json.mapper();
            }
            else {
                mapper = Yaml.mapper();
            }

            return mapper.readTree(contents);
        }
        catch (Exception e) {
            LOGGER.debug("unable to parse ", e);
            throw new UnparseableContentException();
        }
    }
}
