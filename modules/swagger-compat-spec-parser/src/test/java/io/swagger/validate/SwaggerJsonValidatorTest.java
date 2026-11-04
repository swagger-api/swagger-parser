package io.swagger.validate;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.report.MessageBuilder;
import io.swagger.report.Severity;
import io.swagger.transform.util.JacksonSupport;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class SwaggerJsonValidatorTest {

    @DataProvider
    public Object[][] apiDeclarations() {
        return new Object[][]{{"store"}, {"user"}};
    }

    /*
     * The petstore "pet" declaration has a parameter of type File in the body,
     * which the 1.2 schema only allows as a form parameter (same rule as
     * Issue1Test). The former validator rejected it as well.
     */
    @Test
    public void fileParameterInBodyIsReported() throws IOException {
        final MessageBuilder builder = new MessageBuilder();
        new ApiDeclarationJsonValidator().validate(builder, petstore("pet"));
        assertEquals(builder.getHighestSeverity(), Severity.ERROR);
        assertTrue(builder.toString().contains("/apis/4/operations/0/parameters/1"), builder.toString());
    }

    @Test
    public void validResourceListingHasNoErrors() throws IOException {
        final MessageBuilder builder = new MessageBuilder();
        new ResourceListingJsonValidator().validate(builder, petstore("api-docs"));
        assertEquals(builder.getHighestSeverity(), Severity.OPTIONAL, builder.toString());
    }

    @Test(dataProvider = "apiDeclarations")
    public void validApiDeclarationHasNoErrors(final String name) throws IOException {
        final MessageBuilder builder = new MessageBuilder();
        new ApiDeclarationJsonValidator().validate(builder, petstore(name));
        assertEquals(builder.getHighestSeverity(), Severity.OPTIONAL, builder.toString());
    }

    @Test
    public void invalidDocumentIsReported() throws IOException {
        final MessageBuilder builder = new MessageBuilder();
        new ResourceListingJsonValidator().validate(builder,
                JacksonSupport.fromResource("/validate/issue1.json"));
        assertEquals(builder.getHighestSeverity(), Severity.ERROR);
        assertTrue(builder.toString().contains("JSON Schema validation failed"), builder.toString());
    }

    private static JsonNode petstore(final String name) throws IOException {
        return JacksonSupport.fromResource("/specs/v1_2/petstore/" + name);
    }
}
