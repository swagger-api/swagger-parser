package io.swagger.converter;

import io.swagger.models.Format;
import io.swagger.models.apideclaration.Items;
import io.swagger.models.apideclaration.ModelProperty;
import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.DateTimeProperty;
import io.swagger.models.properties.DoubleProperty;
import io.swagger.models.properties.FloatProperty;
import io.swagger.models.properties.IntegerProperty;
import io.swagger.models.properties.LongProperty;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.RefProperty;
import io.swagger.models.properties.StringProperty;
import io.swagger.parser.SwaggerCompatConverter;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class ModelPropertyConverterTest {
    SwaggerCompatConverter converter = new SwaggerCompatConverter();

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
        property.setMinimum("1");
        property.setMaximum("4");

        Property converted = converter.convertProperty(property);
        assertEquals(converted.getClass(), IntegerProperty.class);
        assertEquals(converted.getType(), "integer");
        assertEquals(converted.getFormat(), "int32");

        IntegerProperty prop = (IntegerProperty) converted;
        assertEquals(prop.getMinimum(), new BigDecimal("1"));
        assertEquals(prop.getMaximum(), new BigDecimal("4"));
    }

    @Test
    public void convertLongModelProperty() throws Exception {
        ModelProperty property = new ModelProperty();
        property.setType("integer");
        property.setFormat(Format.INT64);
        property.setDescription("a simple int64 property");
        property.setMinimum("1");
        property.setMaximum("4");

        Property converted = converter.convertProperty(property);
        assertEquals(converted.getClass(), LongProperty.class);
        assertEquals(converted.getType(), "integer");
        assertEquals(converted.getFormat(), "int64");

        LongProperty prop = (LongProperty) converted;
        assertEquals(prop.getMinimum(), new BigDecimal("1"));
        assertEquals(prop.getMaximum(), new BigDecimal("4"));
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
        assertEquals(prop.getMinimum(), new BigDecimal("1.23"));
        assertEquals(prop.getMaximum(), new BigDecimal("4.56"));
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
        assertEquals(prop.getMinimum(), new BigDecimal("1.23"));
        assertEquals(prop.getMaximum(), new BigDecimal("4.56"));
    }

    @Test
    public void convertRefModelProperty() throws Exception {
        ModelProperty property = new ModelProperty();
        property.setRef("Pet");

        Property converted = converter.convertProperty(property);
        assertEquals(converted.getClass(), RefProperty.class);

        RefProperty ref = (RefProperty) converted;
        assertEquals(ref.getSimpleRef(), "Pet");
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
        assertEquals(ref.getSimpleRef(), "Pet");
    }
}
