package io.swagger.models.reader;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.models.apideclaration.ApiDeclaration;

import java.io.IOException;
import java.net.URL;

public class ApiDeclarationReader {
    public static void main(String[] args) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING);

        ApiDeclaration apiDeclaration = objectMapper.readValue(new URL("http://petstore.swagger.io/api/api-docs/store"), ApiDeclaration.class);

        System.out.println(apiDeclaration);
    }
}