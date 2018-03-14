package io.swagger.converter;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

import io.swagger.models.Format;
import io.swagger.models.ModelImpl;
import io.swagger.models.ParamType;
import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.parameters.Parameter;
import io.swagger.parser.SwaggerCompatConverter;

public class BodyParameterConverterTest {

    SwaggerCompatConverter converter = new SwaggerCompatConverter();

    @Test
    public void convertInt32BodyParameter() throws Exception {
        io.swagger.models.apideclaration.Parameter param = new io.swagger.models.apideclaration.Parameter();
        param.setParamType(ParamType.BODY);
        param.setDescription("an int body param");
        param.setRequired(false);
        param.setAllowMultiple(false);
        param.setType("integer");
        param.setFormat(Format.INT32);

        Parameter converted = converter.convertParameter(param);

        assertTrue(converted.getClass().equals(BodyParameter.class));

        BodyParameter bp = (BodyParameter) converted;
        ModelImpl m = (ModelImpl) bp.getSchema();
        assertEquals(param.getType(), m.getType());
        assertEquals(param.getDescription(), bp.getDescription());
        assertEquals((Boolean) param.getRequired(), (Boolean) bp.getRequired());
    }
    
    
    @Test
    public void convertBooleanBodyParameter() throws Exception {
        io.swagger.models.apideclaration.Parameter param = new io.swagger.models.apideclaration.Parameter();
        param.setParamType(ParamType.BODY);
        param.setDescription("a boolean body param");
        param.setRequired(false);
        param.setAllowMultiple(false);
        param.setType("boolean");

        Parameter converted = converter.convertParameter(param);

        assertTrue(converted.getClass().equals(BodyParameter.class));

        BodyParameter bp = (BodyParameter) converted;
        ModelImpl m = (ModelImpl) bp.getSchema();
        assertEquals(param.getType(), m.getType());
        assertEquals(param.getDescription(), bp.getDescription());
        assertEquals((Boolean) param.getRequired(), (Boolean) bp.getRequired());
    }
}
