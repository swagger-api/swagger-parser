package com.wordnik.swagger.converter;

import com.wordnik.swagger.parser.SwaggerLegacyConverter;
import com.wordnik.swagger.util.Json;
import com.wordnik.swagger.models.Format;
import com.wordnik.swagger.models.apideclaration.ModelProperty;
import com.wordnik.swagger.models.apideclaration.Items;
import com.wordnik.swagger.models.properties.*;
import com.wordnik.swagger.models.Model;

import java.util.*;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class ModelConverterTest {
  SwaggerLegacyConverter converter = new SwaggerLegacyConverter();

  @Test
  public void convertModelWithPrimitives() throws Exception {
    com.wordnik.swagger.models.apideclaration.Model model = new com.wordnik.swagger.models.apideclaration.Model();
    Map<String, ModelProperty> properties = new LinkedHashMap<String, ModelProperty>();

    ModelProperty id = new ModelProperty();
    id.setType("integer");
    id.setFormat(Format.INT64);
    id.setDescription("the id property");
    properties.put("id", id);

    ModelProperty name = new ModelProperty();
    name.setType("string");
    name.setDescription("the name property");
    properties.put("name", name);
    model.setProperties(properties);

    ModelProperty age = new ModelProperty();
    age.setType("integer");
    age.setFormat(Format.INT32);
    age.setDescription("the age property");
    properties.put("age", age);
    model.setProperties(properties);

    List<String> required = new ArrayList<String>();
    required.add("id");
    required.add("name");
    model.setRequired(required);

    Model converted = converter.convertModel(model);

    Map<String, Property> convertedProperties = converted.getProperties();
    assertTrue(convertedProperties.size() == 3);
    assertTrue(convertedProperties.keySet().iterator().next().equals("id"));
  }
}