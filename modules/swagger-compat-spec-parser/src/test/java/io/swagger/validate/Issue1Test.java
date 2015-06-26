package io.swagger.validate;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import org.testng.annotations.Test;

import java.io.IOException;

import static org.testng.Assert.assertFalse;

public final class Issue1Test
        extends SwaggerSchemaValidatorTest {
    public Issue1Test()
            throws IOException, ProcessingException {
        super("v1.2/parameterObject.json#", "issue1.json");
    }

    @Test
    public void parameterWithTypeFileMustHaveCorrectConsumesAndParamType()
            throws ProcessingException {
        final ProcessingReport report = schema.validate(instance);
        assertFalse(report.isSuccess());
    }
}
