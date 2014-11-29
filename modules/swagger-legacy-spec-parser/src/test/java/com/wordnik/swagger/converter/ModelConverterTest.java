package com.wordnik.swagger.converter;

import io.swagger.parser.SwaggerLegacyConverter;
import io.swagger.models.Format;
import io.swagger.models.apideclaration.ModelProperty;
import com.wordnik.swagger.models.properties.*;
import com.wordnik.swagger.models.Model;
import com.wordnik.swagger.util.Json;

import java.util.*;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class ModelConverterTest {
  SwaggerLegacyConverter converter = new SwaggerLegacyConverter();

  @Test
  public void convertModelWithPrimitives() throws Exception {
    io.swagger.models.apideclaration.Model model = new io.swagger.models.apideclaration.Model();

    model.setDescription("the model");
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
    assertEquals(model.getDescription(), converted.getDescription());

    Map<String, Property> convertedProperties = converted.getProperties();
    assertTrue(convertedProperties.size() == 3);
    assertTrue(convertedProperties.keySet().iterator().next().equals("id"));
  }

  @Test
  public void convertModelWithSubtypes() throws Exception {
    io.swagger.models.apideclaration.Model model = new io.swagger.models.apideclaration.Model();

    model.setDescription("the model");
    model.setDiscriminator("type");

    List<String> subtypes = new ArrayList<String>();
    subtypes.add("Cat");
    subtypes.add("Dog");

    Map<String, ModelProperty> properties = new LinkedHashMap<String, ModelProperty>();
    ModelProperty id = new ModelProperty();
    id.setType("integer");
    id.setFormat(Format.INT64);
    id.setDescription("the id property");
    properties.put("id", id);

    ModelProperty type = new ModelProperty();
    type.setType("string");
    type.setDescription("the type property, which is the discriminator");
    properties.put("type", type);
    model.setProperties(properties);

    Model converted = converter.convertModel(model);
    // TODO: subtypes are not translated 1:1
  }
}