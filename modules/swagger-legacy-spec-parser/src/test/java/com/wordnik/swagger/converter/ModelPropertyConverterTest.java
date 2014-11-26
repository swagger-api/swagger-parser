package com.wordnik.swagger.converter;

import io.swagger.parser.SwaggerLegacyConverter;
import io.swagger.models.Format;
import io.swagger.models.apideclaration.ModelProperty;
import io.swagger.models.apideclaration.Items;
import com.wordnik.swagger.models.properties.*;

import java.util.*;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class ModelPropertyConverterTest {
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
    property.setMinimum("1.00");
    property.setMaximum("4.00");

    Property converted = converter.convertProperty(property);
    assertEquals(converted.getClass(), IntegerProperty.class);
    assertEquals(converted.getType(), "integer");
    assertEquals(converted.getFormat(), "int32");

    IntegerProperty prop = (IntegerProperty) converted;
    assertEquals(prop.getMinimum(), new Double(1.00));
    assertEquals(prop.getMaximum(), new Double(4.00));
  }

  @Test
  public void convertLongModelProperty() throws Exception {
    ModelProperty property = new ModelProperty();
    property.setType("integer");
    property.setFormat(Format.INT64);
    property.setDescription("a simple int64 property");
    property.setMinimum("1.00");
    property.setMaximum("4.00");

    Property converted = converter.convertProperty(property);
    assertEquals(converted.getClass(), LongProperty.class);
    assertEquals(converted.getType(), "integer");
    assertEquals(converted.getFormat(), "int64");

    LongProperty prop = (LongProperty) converted;
    assertEquals(prop.getMinimum(), new Double(1.00));
    assertEquals(prop.getMaximum(), new Double(4.00));
  }

  @Test
  public void convertFloatModelProperty() throws Exception {
    ModelProperty property = new ModelProperty();
    property.setType("number");
    property.setFormat(Format.FLOAT);
    property.setDescription("a simple float property");
    property.setMinimum("1.23");
    property.setMaximum("4.56");

    Property converted = converter.convertProperty(property);
    assertEquals(converted.getClass(), FloatProperty.class);
    assertEquals(converted.getType(), "number");
    assertEquals(converted.getFormat(), "float");

    FloatProperty prop = (FloatProperty) converted;
    assertEquals(prop.getMinimum(), new Double(1.23));
    assertEquals(prop.getMaximum(), new Double(4.56));
  }

  @Test
  public void convertDoubleModelProperty() throws Exception {
    ModelProperty property = new ModelProperty();
    property.setType("number");
    property.setFormat(Format.DOUBLE);
    property.setDescription("a simple double property");
    property.setMinimum("1.23");
    property.setMaximum("4.56");

    Property converted = converter.convertProperty(property);
    assertEquals(converted.getClass(), DoubleProperty.class);
    assertEquals(converted.getType(), "number");
    assertEquals(converted.getFormat(), "double");

    DoubleProperty prop = (DoubleProperty) converted;
    assertEquals(prop.getMinimum(), new Double(1.23));
    assertEquals(prop.getMaximum(), new Double(4.56));
  }

  @Test
  public void convertRefModelProperty() throws Exception {
    ModelProperty property = new ModelProperty();
    property.setRef("Pet");

    Property converted = converter.convertProperty(property);
    assertEquals(converted.getClass(), RefProperty.class);

    RefProperty ref = (RefProperty) converted;
    assertEquals(ref.get$ref(), "Pet");
  }

  @Test
  public void convertStringArrayModelProperty() throws Exception {
    ModelProperty property = new ModelProperty();
    property.setType("array");

    Items items = new Items();
    items.setType("string");
    property.setItems(items);

    Property converted = converter.convertProperty(property);
    assertEquals(converted.getClass(), ArrayProperty.class);

    ArrayProperty prop = (ArrayProperty) converted;
    Property innerType = prop.getItems();
    assertEquals(innerType.getType(), "string");
  }

  @Test
  public void convertRefArrayModelProperty() throws Exception {
    ModelProperty property = new ModelProperty();
    property.setType("array");

    Items items = new Items();
    items.setRef("Pet");
    property.setItems(items);

    Property converted = converter.convertProperty(property);
    assertEquals(converted.getClass(), ArrayProperty.class);

    ArrayProperty prop = (ArrayProperty) converted;
    Property innerType = prop.getItems();
    RefProperty ref = (RefProperty) innerType;
    assertEquals(ref.get$ref(), "Pet");
  }
}
