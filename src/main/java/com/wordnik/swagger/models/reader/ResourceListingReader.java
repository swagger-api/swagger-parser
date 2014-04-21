package com.wordnik.swagger.models.reader;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wordnik.swagger.models.apideclaration.ApiDeclaration;
import com.wordnik.swagger.models.resourcelisting.ApiListingReference;
import com.wordnik.swagger.models.resourcelisting.ResourceListing;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResourceListingReader {
  public static void main(String[] args) throws IOException, URISyntaxException {
//    MessageBuilder messages = new MessageBuilder();
//
//    ResourceListingParser reader = new ResourceListingParser();
//    String json = "{\"apiVersion\":\"1.0.0\",\"swaggerVersion\":\"1.2\",\"apis\":[{\"path\":\"/pet\",\"description\":\"Operations about pets\"},{\"path\":\"/user\",\"description\":\"Operations about user\"},{\"path\":\"/store\",\"description\":\"Operations about store\"}],\"authorizations\":{\"oauth2\":{\"type\":\"oauth2\",\"scopes\":[{\"scope\":\"write:pets\",\"description\":\"Modify pets in your account\"},{\"scope\":\"read:pets\",\"description\":\"Read your pets\"}],\"grantTypes\":{\"implicit\":{\"loginEndpoint\":{\"url\":\"http://petstore.swagger.wordnik.com/oauth/dialog\"},\"tokenName\":\"access_token\"},\"authorization_code\":{\"tokenRequestEndpoint\":{\"url\":\"http://petstore.swagger.wordnik.com/oauth/requestToken\",\"clientIdName\":\"client_id\",\"clientSecretName\":\"client_secret\"},\"tokenEndpoint\":{\"url\":\"http://petstore.swagger.wordnik.com/oauth/token\",\"tokenName\":\"auth_code\"}}}}},\"info\":{\"title\":\"Swagger Sample App\",\"description\":\"This is a sample server Petstore server.  You can find out more about Swagger \\n    at <a href=\\\"http://swagger.wordnik.com\\\">http://swagger.wordnik.com</a> or on irc.freenode.net, #swagger.  For this sample,\\n    you can use the api key \\\"special-key\\\" to test the authorization filters\",\"termsOfServiceUrl\":\"http://helloreverb.com/terms/\",\"contact\":\"apiteam@wordnik.com\",\"license\":\"Apache 2.0\",\"licenseUrl\":\"http://www.apache.org/licenses/LICENSE-2.0.html\"}}";
//
//    ResourceListing resource = reader.read(json, messages);

//    System.out.println(resource);

      ObjectMapper objectMapper = new ObjectMapper();
      objectMapper.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING);

      String baseUrl = "http://petstore.swagger.wordnik.com/api/api-docs";

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