package io.swagger.models.reader;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.models.apideclaration.ApiDeclaration;
import io.swagger.models.resourcelisting.ApiListingReference;
import io.swagger.models.resourcelisting.ResourceListing;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResourceListingReader {
    public static void main(String[] args) throws IOException, URISyntaxException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING);

        String baseUrl = "http://petstore.swagger.io/api/api-docs";

        ResourceListing resourceListing = objectMapper.readValue(new URL(baseUrl), ResourceListing.class);
        Map<String, ApiDeclaration> apiDeclarations = new HashMap<>();

        List<ApiListingReference> apis = resourceListing.getApis();

        if (apis != null) {
            for (ApiListingReference api : apis) {
                URL apiUrl;

                URI uri = new URI(api.getPath());
                if (uri.isAbsolute()) {
                    apiUrl = uri.toURL();
                } else {
                    apiUrl = new URL(baseUrl + api.getPath());
                }

                apiDeclarations.put(api.getPath(), objectMapper.readValue(apiUrl, ApiDeclaration.class));
            }
        }

        System.out.println("---=== Resource Listing (" + baseUrl + ") ==--");
        System.out.println(resourceListing);

        for (Map.Entry<String, ApiDeclaration> apiDeclarationEntry : apiDeclarations.entrySet()) {
            System.out.println("---=== API Declaration (" + apiDeclarationEntry.getKey() + ") ==--");
            System.out.println(apiDeclarationEntry.getValue());
        }

    }
}