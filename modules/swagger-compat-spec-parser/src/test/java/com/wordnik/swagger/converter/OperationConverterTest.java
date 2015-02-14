package com.wordnik.swagger.converter;

import io.swagger.parser.SwaggerCompatConverter;
import io.swagger.models.ParamType;
import io.swagger.models.Method;
import io.swagger.models.apideclaration.ResponseMessage;

import com.wordnik.swagger.models.Operation;
import com.wordnik.swagger.models.Response;
import com.wordnik.swagger.models.parameters.*;
import com.wordnik.swagger.models.properties.*;
import com.wordnik.swagger.util.Json;

import java.util.*;

import org.testng.annotations.Test;

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

    Operation converted = converter.convertOperation("tag", operation);

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

    Property property = response.getSchema();
    assertNotNull(property);
    assertTrue(property.getClass().equals(RefProperty.class));
    RefProperty ref = (RefProperty) property;
    assertEquals(ref.getSimpleRef(), "Cat");
  }
}