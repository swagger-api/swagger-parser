package io.swagger.v3.parser.processors;


import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.parser.ResolverCache;
import io.swagger.v3.parser.models.RefFormat;
import mockit.Expectations;
import mockit.Injectable;
import mockit.StrictExpectations;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.AssertJUnit.assertTrue;


public class ExternalRefProcessorTest {

    @Injectable
	ResolverCache cache;

    @Injectable
	OpenAPI openAPI;

    @Test
    public void testProcessRefToExternalDefinition_NoNameConflict(
            @Injectable final Schema mockedModel) throws Exception {

        final String ref = "http://my.company.com/path/to/file.json#/foo/bar";
        final RefFormat refFormat = RefFormat.URL;

        new StrictExpectations() {{
			cache.getRenamedRef(ref);
			times = 1;
			result = null;

            cache.loadRef(ref, refFormat, Schema.class);
            times = 1;
            result = mockedModel;

			openAPI.getComponents();
			times = 1;
			result = new Components();

			openAPI.getComponents().getSchemas();
			times = 1;
			result = null;

            cache.putRenamedRef(ref, "bar");
            openAPI.getComponents().addSchemas("bar", mockedModel); times=1;

			cache.addReferencedKey("bar");
			times = 1;
			result = null;
        }};

        String newRef = new ExternalRefProcessor(cache, openAPI).processRefToExternalSchema(ref, refFormat);
        assertEquals(newRef, "bar");
    }



    @Test
    public void testNestedExternalRefs(@Injectable final Schema mockedModel){
    	final RefFormat refFormat = RefFormat.URL;
  	
    	//Swagger test instance
    	OpenAPI testedOpenAPI = new OpenAPI();
    	
    	//Start with customer, add address property to it
    	final String customerURL = "http://my.company.com/path/to/customer.json#/definitions/Customer";
    	
    	final Schema customerModel = new Schema();
    	Map<String,Schema> custProps = new HashMap<>();
    	Schema address = new Schema();
    	final String addressURL = "http://my.company.com/path/to/address.json#/definitions/Address";
    	address.set$ref(addressURL);
    	custProps.put("Address", address);
    	
    	//Create a 'local' reference to something in #/definitions, this should be ignored a no longer result in a null pointer exception
    	final String loyaltyURL = "#/definitions/LoyaltyScheme";
    	
    	Schema loyaltyProp = new Schema();
    	loyaltyProp.set$ref(loyaltyURL);
    	loyaltyProp.setName("LoyaltyCardNumber");
    	List<String> required = new ArrayList<>();
    	required.add("LoyaltyCardNumber");
    	loyaltyProp.setRequired(required);
    	
    	custProps.put("Loyalty", loyaltyProp);
    	customerModel.setProperties(custProps);
    	
    	//create address model, add Contact Ref Property to it
    	final Schema addressModel = new Schema();
    	Map<String, Schema> addressProps = new HashMap<>();
    	Schema contact = new Schema();
    	final String contactURL = "http://my.company.com/path/to/Contact.json#/definitions/Contact";
    	contact.set$ref(contactURL);
    	addressProps.put("Contact", contact);
    	addressModel.setProperties(addressProps);
    	
    	
    	//Create contact model, with basic type property
    	final Schema contactModel = new Schema();
    	Schema contactProp = new StringSchema();
    	contactProp.setName("PhoneNumber");
		List<String> requiredList = new ArrayList<>();
		requiredList.add("PhoneNumber");
    	contactProp.setRequired(requiredList);
    	Map<String, Schema> contactProps = new HashMap<>();
    	contactProps.put("PhoneNumber", contactProp);
    	contactModel.setProperties(contactProps);
    	
    	new Expectations(){{
    		cache.loadRef(customerURL, refFormat, Schema.class);
    		result = customerModel;
    		times = 1;
    				
    		cache.loadRef(addressURL, refFormat, Schema.class);
    		result = addressModel;
    		times = 1;
    		
    		cache.loadRef(contactURL, refFormat, Schema.class);
    		result = contactModel;
    		times = 1;
 		}};
    	
    	String actualRef = new ExternalRefProcessor(cache, testedOpenAPI).processRefToExternalSchema(customerURL, refFormat);

		assertTrue(testedOpenAPI.getComponents().getSchemas().get("Customer") != null);
    	assertTrue(testedOpenAPI.getComponents().getSchemas().get("Contact") != null);
    	assertTrue(testedOpenAPI.getComponents().getSchemas().get("Address") != null);
    }
}
