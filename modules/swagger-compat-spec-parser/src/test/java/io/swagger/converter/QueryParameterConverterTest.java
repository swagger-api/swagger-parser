package io.swagger.converter;

import io.swagger.parser.SwaggerCompatConverter;
import io.swagger.models.ParamType;
import io.swagger.models.Format;

import com.wordnik.swagger.models.parameters.*;
import com.wordnik.swagger.models.properties.*;
import com.wordnik.swagger.util.Json;

import java.util.*;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class QueryParameterConverterTest {
  SwaggerCompatConverter converter = new SwaggerCompatConverter();

  @Test
  public void convertStringQueryParameter() throws Exception {
    io.swagger.models.apideclaration.Parameter param = new io.swagger.models.apideclaration.Parameter();
    param.setParamType(ParamType.QUERY);
    param.setDescription("a string query param");
    param.setRequired(false);
    param.setAllowMultiple(false);
    param.setType("string");

    Parameter converted = converter.convertParameter(param);

    assertTrue(converted.getClass().equals(QueryParameter.class));

    QueryParameter qp = (QueryParameter) converted;
    assertEquals(param.getType(), qp.getType());
    assertEquals(param.getDescription(), qp.getDescription());
    assertEquals((Boolean)param.getRequired(), (Boolean)qp.getRequired());
    assertNull(qp.getCollectionFormat());
  }

  @Test
  public void convertDateQueryParameter() throws Exception {
    io.swagger.models.apideclaration.Parameter param = new io.swagger.models.apideclaration.Parameter();
    param.setParamType(ParamType.QUERY);
    param.setDescription("a string query param");
    param.setRequired(false);
    param.setAllowMultiple(false);
    param.setType("string");
    param.setFormat(Format.DATE_TIME);

    Parameter converted = converter.convertParameter(param);

    assertTrue(converted.getClass().equals(QueryParameter.class));

    QueryParameter qp = (QueryParameter) converted;
    assertEquals(param.getType(), qp.getType());
    assertEquals(qp.getFormat(), "date-time");
    assertEquals(param.getDescription(), qp.getDescription());
    assertEquals((Boolean)param.getRequired(), (Boolean)qp.getRequired());
    assertNull(qp.getCollectionFormat());
  }

  @Test
  public void convertIntegerQueryParameter() throws Exception {
    io.swagger.models.apideclaration.Parameter param = new io.swagger.models.apideclaration.Parameter();
    param.setParamType(ParamType.QUERY);
    param.setDescription("a string query param");
    param.setRequired(false);
    param.setAllowMultiple(false);
    param.setType("integer");
    param.setFormat(Format.INT32);

    Parameter converted = converter.convertParameter(param);

    assertTrue(converted.getClass().equals(QueryParameter.class));

    QueryParameter qp = (QueryParameter) converted;
    assertEquals(param.getType(), qp.getType());
    assertEquals(qp.getFormat(), "int32");
    assertEquals(param.getDescription(), qp.getDescription());
    assertEquals((Boolean)param.getRequired(), (Boolean)qp.getRequired());
    assertNull(qp.getCollectionFormat());
  }

  @Test
  public void convertStringArrayQueryParameter() throws Exception {
    io.swagger.models.apideclaration.Parameter param = new io.swagger.models.apideclaration.Parameter();
    param.setParamType(ParamType.QUERY);
    param.setDescription("a string array query param");
    param.setRequired(false);
    param.setAllowMultiple(true);
    param.setType("string");

    Parameter converted = converter.convertParameter(param);

    assertTrue(converted.getClass().equals(QueryParameter.class));

    QueryParameter qp = (QueryParameter) converted;
    assertEquals(qp.getType(), "array");
    assertEquals(param.getDescription(), qp.getDescription());
    assertNotNull(qp.getItems());
    Property items = qp.getItems();
    assertEquals(items.getType(), "string");
    assertEquals((Boolean)param.getRequired(), (Boolean)qp.getRequired());
    assertEquals(qp.getCollectionFormat(), "csv");
  }
}

