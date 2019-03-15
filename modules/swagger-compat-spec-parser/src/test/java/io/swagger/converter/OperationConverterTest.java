package io.swagger.converter;

import io.swagger.models.Method;
import io.swagger.models.Model;
import io.swagger.models.Operation;
import io.swagger.models.ParamType;
import io.swagger.models.RefModel;
import io.swagger.models.Response;
import io.swagger.models.apideclaration.ApiDeclaration;
import io.swagger.models.apideclaration.ResponseMessage;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.RefProperty;
import io.swagger.parser.SwaggerCompatConverter;
import org.testng.annotations.Test;

import java.util.*;

import static org.testng.Assert.*;

public class OperationConverterTest {
    SwaggerCompatConverter converter = new SwaggerCompatConverter();

    @Test
    public void convertOperation1() throws Exception {
        io.swagger.models.apideclaration.Operation operation = new io.swagger.models.apideclaration.Operation();

        operation.setMethod(Method.GET);
        operation.setSummary("the summary");
        operation.setNotes("the notes");
        operation.setNickname("getFun");
        List<String> produces = new ArrayList<String>();
        produces.add("application/json");
        operation.setProduces(produces);

        // response type
        operation.setRef("Cat");

        // response messages
        List<ResponseMessage> responses = new ArrayList<ResponseMessage>();
        ResponseMessage message400 = new ResponseMessage();
        message400.setCode(400);
        message400.setMessage("got a 400");
        responses.add(message400);

        operation.setResponseMessages(responses);

        // parameters
        io.swagger.models.apideclaration.Parameter param = new io.swagger.models.apideclaration.Parameter();
        param.setParamType(ParamType.QUERY);
        param.setDescription("a string query param");
        param.setRequired(false);
        param.setAllowMultiple(false);
        param.setType("string");

        List<io.swagger.models.apideclaration.Parameter> parameters = new ArrayList<io.swagger.models.apideclaration.Parameter>();
        parameters.add(param);
        operation.setParameters(parameters);

        Operation converted = converter.convertOperation("tag", operation, new ApiDeclaration());

        assertTrue(converted.getTags().size() == 1);
        assertEquals(converted.getTags().get(0), "tag");
        assertEquals(operation.getSummary(), converted.getSummary());
        assertEquals(operation.getNotes(), converted.getDescription());
        assertEquals(operation.getNickname(), converted.getOperationId());
        assertTrue(converted.getProduces().size() == 1);
        assertEquals(converted.getProduces().get(0), "application/json");
        assertTrue(converted.getParameters().size() == 1);
        assertTrue(converted.getResponses().size() == 2);

        Response response = converted.getResponses().get("200");
        assertNotNull(response);
        assertEquals(response.getDescription(), "success");

        Model schema = response.getResponseSchema();
        assertNotNull(schema);
        assertTrue(schema.getClass().equals(RefModel.class));
        RefModel ref = (RefModel) schema;
        assertEquals(ref.getSimpleRef(), "Cat");
    }

    @Test
    public void testConvertOperation_ConsumesAndProducesInheritedFromApiDeclaration() throws Exception {
        Set<String> expectedConsumes = new HashSet<>(Arrays.asList("application/json", "application/xml"));
        Set<String> expectedProduces = new HashSet<>(Arrays.asList("text/plain"));

        final ApiDeclaration apiDeclaration = new ApiDeclaration();
        apiDeclaration.setConsumes(new ArrayList<>(expectedConsumes));
        apiDeclaration.setProduces(new ArrayList<>(expectedProduces));

        io.swagger.models.apideclaration.Operation operation = new io.swagger.models.apideclaration.Operation();
        operation.setMethod(Method.GET);

        final SwaggerCompatConverter swaggerCompatConverter = new SwaggerCompatConverter();
        Operation converted = swaggerCompatConverter.convertOperation("tag", operation, apiDeclaration);

        assertSetsAreEqual(expectedConsumes, converted.getConsumes());
        assertSetsAreEqual(expectedProduces, converted.getProduces());
    }

    private void assertSetsAreEqual(Set<String> expectedConsumes, List<String> actualConsumes) {
        Set<String> actualConsumesSet = new HashSet<>();
        actualConsumesSet.addAll(actualConsumes);
        assertEquals(expectedConsumes.size(), actualConsumes.size());
        assertTrue(actualConsumesSet.containsAll(expectedConsumes));
    }
}