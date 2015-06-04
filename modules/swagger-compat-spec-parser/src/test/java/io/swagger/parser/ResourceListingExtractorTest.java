package io.swagger.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.github.fge.jackson.JacksonUtils;

import io.swagger.models.resourcelisting.ApiListingReference;
import io.swagger.models.resourcelisting.ResourceListing;
import org.testng.annotations.Test;

import java.io.File;
import java.util.List;

public class ResourceListingExtractorTest {
  @Test
  public void readResourceListing() throws Exception {
    String resourceListingUri = "http://petstore.swagger.io/api/api-docs";
    ObjectMapper mapper = JacksonUtils.newMapper();
    mapper.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING);

    File resourceListingFile = new File("src/test/resources/specs/v1_2/petstore/api-docs");
    ResourceListing resourceListing = mapper.readValue(resourceListingFile, ResourceListing.class);


    List<ApiListingReference> apis = resourceListing.getApis();
    for(ApiListingReference ref : apis) {
      String path = ref.getPath();

      if(path.startsWith("http")) {
        // absolute listing, do some magic

      }
      else {
        // it is possible that the resource listing URL has a trailing slash and the path has a leading one
        path = (resourceListingUri + path).replace("//","/");
      }

      System.out.println("found path " + path);
    }
  }
}
