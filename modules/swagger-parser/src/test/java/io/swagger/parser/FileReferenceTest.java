package io.swagger.parser;

import io.swagger.models.*;

import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.parameters.RefParameter;
import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.RefProperty;
import io.swagger.models.refs.RefFormat;
import io.swagger.parser.util.SwaggerDeserializationResult;
import io.swagger.util.Yaml;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.*;
import java.util.Arrays;
import java.util.Map;


public class FileReferenceTest {
    @Test
    public void testIssue306() {
        SwaggerDeserializationResult result = new SwaggerParser().readWithInfo("./src/test/resources/nested-file-references/issue-306.yaml", null, true);
        assertNotNull(result.getSwagger());

        Swagger swagger = result.getSwagger();

        assertTrue(swagger.getDefinitions().size() == 6);
        // resolved from `$ref: './book.yaml'`
        assertNotNull(swagger.getDefinitions().get("Inventory"));
        // resolved from `$ref: 'book.yaml'`
        assertNotNull(swagger.getDefinitions().get("Orders"));

        // copied from `./book.yaml`
        assertNotNull(swagger.getDefinitions().get("book"));
    }

    @Test
    public void testIssue308() {
        SwaggerDeserializationResult result = new SwaggerParser().readWithInfo("./src/test/resources/nested-file-references/issue-308.yaml", null, true);
        assertNotNull(result.getSwagger());

        Swagger swagger = result.getSwagger();

        assertTrue(swagger.getDefinitions().size() == 2);
        assertTrue(swagger.getDefinitions().get("Paging").getProperties().size() == 1);
    }

    @Test
    public void testIssue310() {
        SwaggerDeserializationResult result = new SwaggerParser().readWithInfo("./src/test/resources/nested-file-references/issue-310.yaml", null, true);
        assertNotNull(result.getSwagger());

        Swagger swagger = result.getSwagger();

        assertTrue(swagger.getDefinitions().size() == 2);
        assertTrue(swagger.getDefinitions().get("Paging").getProperties().size() == 1);
    }

    @Test
    public void testIssue312() {
        SwaggerDeserializationResult result = new SwaggerParser().readWithInfo("./src/test/resources/nested-file-references/issue-312.yaml", null, true);
        assertNotNull(result.getSwagger());

        Swagger swagger = result.getSwagger();
        assertNotNull(swagger.getPath("/events"));
        Path path = swagger.getPath("/events");
        assertNotNull(path.getGet());

        Operation get = path.getGet();
        assertEquals(get.getOperationId(), "getEvents");
        assertTrue(swagger.getDefinitions().size() == 2);
        assertTrue(swagger.getDefinitions().get("Paging").getProperties().size() == 1);
    }

    @Test
    public void testIssue314() {
        SwaggerDeserializationResult result = new SwaggerParser().readWithInfo("./src/test/resources/nested-file-references/issue-314.yaml", null, true);
        assertNotNull(result.getSwagger());

        Swagger swagger = result.getSwagger();
        assertNotNull(swagger.getPath("/events"));
        Path path = swagger.getPath("/events");
        assertNotNull(path.getGet());

        Operation get = path.getGet();
        assertEquals(get.getOperationId(), "getEvents");
        assertTrue(swagger.getDefinitions().size() == 3);
        assertTrue(swagger.getDefinitions().get("Foobar").getProperties().size() == 1);
        assertTrue(swagger.getDefinitions().get("StatusResponse").getProperties().size() == 1);
        assertTrue(swagger.getDefinitions().get("Paging").getProperties().size() == 1);
    }

    @Test
    public void testIssue316() {
        SwaggerDeserializationResult result = new SwaggerParser().readWithInfo("./src/test/resources/nested-file-references/issue-316.yaml", null, true);
        assertNotNull(result.getSwagger());

        Swagger swagger = result.getSwagger();
        assertNotNull(swagger.getPath("/events"));
        Path path = swagger.getPath("/events");
        assertNotNull(path.getGet());
        Operation get = path.getGet();
        assertEquals(get.getOperationId(), "getEvents");
        assertTrue(swagger.getDefinitions().size() == 3);
        assertTrue(swagger.getDefinitions().get("Foobar").getProperties().size() == 1);
        assertTrue(swagger.getDefinitions().get("StatusResponse").getProperties().size() == 1);
        assertTrue(swagger.getDefinitions().get("Paging2").getProperties().size() == 2);
        Model model = swagger.getDefinitions().get("Paging2");

        Property property = model.getProperties().get("foobar");
        assertTrue(property instanceof RefProperty);
        RefProperty ref = (RefProperty) property;
        assertEquals(ref.get$ref(), "#/definitions/Foobar");
    }

    @Test
    public void testIssue323() {
        SwaggerDeserializationResult result = new SwaggerParser().readWithInfo("./src/test/resources/nested-file-references/issue-323.yaml", null, true);
        assertNotNull(result.getSwagger());

        Swagger swagger = result.getSwagger();
        assertNotNull(swagger.getPath("/events"));

        assertNotNull(swagger.getDefinitions().get("StatusResponse"));
        assertNotNull(swagger.getDefinitions().get("Paging"));
        assertNotNull(swagger.getDefinitions().get("Foobar"));
    }

    @Test
    public void testIssue289() {
        SwaggerDeserializationResult result = new SwaggerParser().readWithInfo("./src/test/resources/issue-289.yaml", null, true);
        assertNotNull(result.getSwagger());

        Swagger swagger = result.getSwagger();
        assertNotNull(swagger.getPath("/foo").getGet());
    }

    @Test
    public void testIssue822() {
        SwaggerDeserializationResult result = new SwaggerParser().readWithInfo("./src/test/resources/issue-enc.yaml", null, true);
        assertNotNull(result.getSwagger());

        Swagger swagger = result.getSwagger();

        assertNotNull(swagger.getPath("/foo").getGet());
        assertNotNull(swagger.getPath("/bar/wtf").getGet());
        assertNull(swagger.getPath("/bar/haha").getGet());
        assertNotNull(swagger.getPath("/wtf/{bar}").getGet());
        assertNotNull(swagger.getPath("/haha/{bar}").getGet());
        assertNull(swagger.getPath("/haha2/{bar}").getGet());

    }

    @Test
    public void testIssue336() {
        SwaggerDeserializationResult result = new SwaggerParser().readWithInfo("./src/test/resources/nested-file-references/issue-336.json", null, true);
        assertNotNull(result.getSwagger());

        Swagger swagger = result.getSwagger();
        assertNotNull(swagger.getPaths());
    }

    @Test
    public void testIssue340() {
        SwaggerDeserializationResult result = new SwaggerParser().readWithInfo("./src/test/resources/nested-file-references/issue-340.json", null, true);
        assertNotNull(result.getSwagger());

        Swagger swagger = result.getSwagger();
        assertFalse(swagger.getDefinitions().get("BarData") instanceof RefModel);
    }

    @Test
    public void testIssue304() {
        SwaggerDeserializationResult result = new SwaggerParser().readWithInfo("./src/test/resources/nested-file-references/issue-304.json", null, true);
        assertNotNull(result.getSwagger().getDefinitions());
    }

    @Test
    public void testAllOfFlatAndNested() {
        for (String path : Arrays.asList("./src/test/resources/allOf-properties-ext-ref/models/swagger.json",
                "./src/test/resources/allOf-properties-ext-ref/swagger.json")) {
            Swagger swagger = new SwaggerParser().read(path);
            assertEquals(3, swagger.getDefinitions().size());
            ComposedModel composedModel = (ComposedModel)swagger.getDefinitions().get("record");
            assertEquals(((RefModel) composedModel.getParent()).getSimpleRef(), "pet");
            Map<String, Property> props = composedModel.getChild().getProperties();
            assertEquals(((RefProperty) props.get("mother")).getSimpleRef(), "pet");
            assertEquals(((RefProperty) ((ArrayProperty) props.get("siblings")).getItems()).getSimpleRef(), "pet");
        }
    }

    @Test
    public void testIssue421() {
        SwaggerDeserializationResult result = new SwaggerParser().readWithInfo("./src/test/resources/nested-file-references/issue-421.yaml", null, true);
        assertNotNull(result.getSwagger());

        Swagger swagger = result.getSwagger();
        assertNotNull(swagger.getPath("/pet/{petId}"));
        assertNotNull(swagger.getPath("/pet/{petId}").getGet());
        assertNotNull(swagger.getPath("/pet/{petId}").getGet().getParameters());
        assertTrue(swagger.getPath("/pet/{petId}").getGet().getParameters().size() == 1);
        assertTrue(swagger.getPath("/pet/{petId}").getGet().getParameters().get(0).getName().equals("petId"));
        assertTrue(swagger.getDefinitions().get("Pet") instanceof ModelImpl);
        assertTrue(swagger.getDefinitions().get("Pet").getProperties().size() == 6);

        assertNotNull(swagger.getPath("/pet/{petId}").getPost());
        assertNotNull(swagger.getPath("/pet/{petId}").getPost().getParameters());
        assertTrue(swagger.getPath("/pet/{petId}").getPost().getParameters().size() == 3);
        assertTrue(swagger.getPath("/pet/{petId}").getPost().getParameters().get(1) instanceof RefParameter);
        assertTrue(((RefParameter)swagger.getPath("/pet/{petId}").getPost().getParameters().get(1)).getRefFormat() == RefFormat.INTERNAL);
        assertTrue(((RefParameter)swagger.getPath("/pet/{petId}").getPost().getParameters().get(1)).getSimpleRef().equals("name"));

        assertNotNull(swagger.getPath("/store/order"));
        assertNotNull(swagger.getPath("/store/order").getPost());
        assertNotNull(swagger.getPath("/store/order").getPost().getParameters());
        assertTrue(swagger.getPath("/store/order").getPost().getParameters().size() == 1);
        assertTrue(swagger.getPath("/store/order").getPost().getParameters().get(0) instanceof BodyParameter);
        assertNotNull(((BodyParameter)swagger.getPath("/store/order").getPost().getParameters().get(0)).getSchema());
        assertTrue(((BodyParameter)swagger.getPath("/store/order").getPost().getParameters().get(0)).getSchema() instanceof RefModel);
        assertTrue(((RefModel)((BodyParameter)swagger.getPath("/store/order").getPost().getParameters().get(0)).getSchema()).getSimpleRef().equals("Order"));

        assertTrue(swagger.getDefinitions().get("Order") instanceof ModelImpl);
        assertTrue(swagger.getDefinitions().get("Order").getProperties().size() == 6);
    }

    @Test
    public void testRelativeRefIssue421() {
        SwaggerDeserializationResult result = new SwaggerParser().readWithInfo("./src/test/resources/main.yaml", null, true);
        assertNotNull(result.getSwagger());

        Swagger swagger = result.getSwagger();
        assertNotNull(swagger);
        assertNotNull(swagger.getPath("pets"));
        assertNotNull(swagger.getPath("pets").getGet());
        assertNotNull(swagger.getPath("pets").getGet().getResponses());
        assertNotNull(swagger.getPath("pets").getGet().getResponses().get("200"));
        assertNotNull(swagger.getPath("pets").getGet().getResponses().get("200").getSchema());
        assertTrue(swagger.getPath("pets").getGet().getResponses().get("200").getSchema() instanceof RefProperty);

        assertEquals(((RefProperty)swagger.getPath("pets").getGet().getResponses().get("200").getSchema()).get$ref(), "#/definitions/Pet");

        assertTrue(swagger.getDefinitions().get("Pet") instanceof ModelImpl);
        assertTrue(swagger.getDefinitions().get("Pet").getProperties().size() == 2);
    }
}
