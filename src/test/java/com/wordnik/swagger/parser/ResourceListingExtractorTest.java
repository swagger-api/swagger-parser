package com.wordnik.swagger.parser;

import com.wordnik.swagger.models.resourcelisting.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jackson.JacksonUtils;

import org.testng.annotations.Test;

import java.io.File;
import java.util.List;

import static org.testng.Assert.*;

public class ResourceListingExtractorTest {
  @Test
  public void readResourceListing() throws Exception {
    String resourceListingUri = "https://api.helloreverb.com/v2/api-docs";
    ObjectMapper mapper = JacksonUtils.newMapper();

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
    }
  }
}
