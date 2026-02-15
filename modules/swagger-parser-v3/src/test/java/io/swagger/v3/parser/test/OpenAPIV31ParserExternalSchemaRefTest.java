package io.swagger.v3.parser.test;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Map;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * Test for GitHub issue #2266: External schema resolution broken in OpenAPI 3.1 (works in 3.0)
 * 
 * When parsing an OpenAPI 3.1 specification with external schema references (using $ref to separate YAML files),
 * the getComponents().getSchemas() method should return the resolved schemas, not null.
 */
public class OpenAPIV31ParserExternalSchemaRefTest {

    @Test
    public void testExternalSchemaRefResolvedToComponents() throws Exception {
        ParseOptions parseOptions = new ParseOptions();
        parseOptions.setResolve(true);
        
        SwaggerParseResult result = new OpenAPIV3Parser().readLocation(
            new File("src/test/resources/3.1.0/dereference/external-schema-ref/swagger.yaml").getAbsolutePath(), 
            null, 
            parseOptions
        );
        
        OpenAPI openAPI = result.getOpenAPI();
        
        assertNotNull(openAPI, "OpenAPI should not be null");
        assertNotNull(openAPI.getComponents(), "Components should not be null");
        assertNotNull(openAPI.getComponents().getSchemas(), "Schemas should not be null");
        
        Map<String, Schema> schemas = openAPI.getComponents().getSchemas();
        assertTrue(schemas.size() > 0, "Schemas should contain at least one entry");
        assertTrue(schemas.containsKey("ProbeInfo"), "Schemas should contain 'ProbeInfo'");
        
        Schema probeInfoSchema = schemas.get("ProbeInfo");
        assertNotNull(probeInfoSchema, "ProbeInfo schema should not be null");
        assertNotNull(probeInfoSchema.getProperties(), "ProbeInfo properties should not be null");
        assertTrue(probeInfoSchema.getProperties().containsKey("status"), "ProbeInfo should have 'status' property");
        assertTrue(probeInfoSchema.getProperties().containsKey("version"), "ProbeInfo should have 'version' property");
    }
    
    @Test
    public void testExternalSchemaRefResolvedFullyToComponents() throws Exception {
        ParseOptions parseOptions = new ParseOptions();
        parseOptions.setResolve(true);
        parseOptions.setResolveFully(true);
        
        SwaggerParseResult result = new OpenAPIV3Parser().readLocation(
            new File("src/test/resources/3.1.0/dereference/external-schema-ref/swagger.yaml").getAbsolutePath(), 
            null, 
            parseOptions
        );
        
        OpenAPI openAPI = result.getOpenAPI();
        
        assertNotNull(openAPI, "OpenAPI should not be null");
        assertNotNull(openAPI.getComponents(), "Components should not be null");
        assertNotNull(openAPI.getComponents().getSchemas(), "Schemas should not be null with resolveFully");
        
        Map<String, Schema> schemas = openAPI.getComponents().getSchemas();
        assertTrue(schemas.size() > 0, "Schemas should contain at least one entry");
        assertTrue(schemas.containsKey("ProbeInfo"), "Schemas should contain 'ProbeInfo'");
    }
}
