package com.wordnik.swagger.converter;

import com.wordnik.swagger.parser.SwaggerLegacyConverter;
import com.wordnik.swagger.util.Json;
import com.wordnik.swagger.models.Format;
import com.wordnik.swagger.models.apideclaration.ModelProperty;
import com.wordnik.swagger.models.properties.*;

import java.util.*;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class ModelConverterTest {
  SwaggerLegacyConverter converter = new SwaggerLegacyConverter();

  @Test
  public void convertStringModelProperty() throws Exception {
    ModelProperty property = new ModelProperty();
    property.setType("string");
    property.setDescription("a simple string");

    Property converted = converter.convertProperty(property);
    assertEquals(converted.getClass(), StringProperty.class);
    assertEquals(converted.getType(), "string");
    assertEquals(converted.getDescription(), property.getDescription());
  }

  @Test
  public void convertStringModelPropertyWithEnum() throws Exception {
    ModelProperty property = new ModelProperty();
    property.setType("string");
    property.setDescription("a simple string");
    List<String> enumValues = new ArrayList<String>();
    enumValues.add("cat");
    enumValues.add("dog");
    property.setEnumValues(enumValues);

    Property converted = converter.convertProperty(property);
    assertEquals(converted.getClass(), StringProperty.class);
    assertEquals(converted.getType(), "string");
    assertEquals(converted.getDescription(), property.getDescription());
    StringProperty prop = (StringProperty) converted;
    assertNotNull(prop.getEnum());
    assertTrue(prop.getEnum().size() == 2);
  }

  @Test
  public void convertDateTimeModelProperty() throws Exception {
    ModelProperty property = new ModelProperty();
    property.setType("string");
    property.setFormat(Format.DATE_TIME);
    property.setDescription("a simple date-time");

    Property converted = converter.convertProperty(property);
    assertEquals(converted.getClass(), DateTimeProperty.class);
    assertEquals(converted.getDescription(), property.getDescription());
    assertEquals(converted.getType(), "string");
    assertEquals(converted.getFormat(), "date-time");
  }

  @Test
  public void convertIntegerModelProperty() throws Exception {
    ModelProperty property = new ModelProperty();
    property.setType("integer");
    property.setFormat(Format.INT32);
    property.setDescription("a simple int32 property");
    property.setMinimum("1.23");
    property.setMaximum("4.56");

    Property converted = converter.convertProperty(property);
    assertEquals(converted.getClass(), IntegerProperty.class);
    assertEquals(converted.getType(), "integer");
    assertEquals(converted.getFormat(), "int32");

    IntegerProperty prop = (IntegerProperty) converted;
    assertEquals(prop.getMinimum(), new Double(1.23));
    assertEquals(prop.getMaximum(), new Double(4.56));
  }
}
