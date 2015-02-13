package com.wordnik.swagger.converter;

import io.swagger.parser.SwaggerCompatConverter;
import io.swagger.models.ParamType;
import io.swagger.models.Format;

import com.wordnik.swagger.models.parameters.*;
import com.wordnik.swagger.models.properties.*;
import com.wordnik.swagger.util.Json;

import java.util.*;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class PathParameterConverterTest {
  SwaggerCompatConverter converter = new SwaggerCompatConverter();

  @Test
  public void convertStringPathParameter() throws Exception {
    io.swagger.models.apideclaration.Parameter param = new io.swagger.models.apideclaration.Parameter();
    param.setParamType(ParamType.PATH);
    param.setDescription("a string path param");
    param.setRequired(false);
    param.setAllowMultiple(false);
    param.setType("string");

    Parameter converted = converter.convertParameter(param);

    assertTrue(converted.getClass().equals(PathParameter.class));

    PathParameter pp = (PathParameter) converted;
    assertEquals(param.getType(), pp.getType());
    assertEquals(param.getDescription(), pp.getDescription());
    assertTrue(pp.getRequired());
    assertNull(pp.getCollectionFormat());
  }
}

