package io.swagger.v3.parser.processors;


import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.parser.ResolverCache;
import io.swagger.v3.parser.core.models.AuthorizationValue;
import io.swagger.v3.parser.models.RefFormat;
import io.swagger.v3.parser.util.RemoteUrl;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Mocked;
import mockit.StrictExpectations;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
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


	@Mocked
	RemoteUrl remoteUrl;


	@Test
	public void testRelativeRefIncludingUrlRef(@Injectable final Schema mockedModel)
			throws Exception {
		final RefFormat refFormat = RefFormat.RELATIVE;

		final String url = "https://my.example.remote.url.com/globals.yaml";

		final String expectedResult = "components:\n" +
				"  schemas:\n" +
				"    link-object:\n" +
				"      type: object\n" +
				"      additionalProperties:\n" +
				"        \"$ref\": \"#/components/schemas/rel-data\"\n" +
				"    rel-data:\n" +
				"      type: object\n" +
				"      required:\n" +
				"      - href\n" +
				"      properties:\n" +
				"        href:\n" +
				"          type: string\n" +
				"        note:\n" +
				"          type: string\n" +
				"    result:\n" +
				"      type: object\n" +
				"      properties:\n" +
				"        name:\n" +
				"          type: string\n" +
				"        _links:\n" +
				"          \"$ref\": \"#/components/schemas/link-object\"\n" +
				"";
		List<AuthorizationValue> auths = null;

		new Expectations() {{
			RemoteUrl.urlToString(url, auths);
			times = 1;
			result = expectedResult;
		}};

		OpenAPI mockedOpenAPI = new OpenAPI();
		mockedOpenAPI.setComponents(new Components());
		mockedOpenAPI.getComponents().setSchemas(new HashMap<>());
		ResolverCache mockedResolverCache = new ResolverCache(mockedOpenAPI, null, null);

		ExternalRefProcessor processor = new ExternalRefProcessor(mockedResolverCache, mockedOpenAPI);

		processor.processRefToExternalSchema("./relative-with-url/relative-with-url.yaml#/relative-with-url", refFormat);
		assertThat(((Schema) mockedOpenAPI.getComponents().getSchemas().get("relative-with-url").getProperties().get("Foo")).get$ref(),
				is("https://my.example.remote.url.com/globals.yaml#/components/schemas/link-object")
		);
		assertThat(mockedOpenAPI.getComponents().getSchemas().keySet().contains("link-object"), is(true));
		assertThat(mockedOpenAPI.getComponents().getSchemas().keySet().contains("rel-data"), is(true));
		// assert that ref is relative ref is resolved. and the file path is from root yaml file.
		assertThat(((Schema) mockedOpenAPI.getComponents().getSchemas().get("relative-with-url").getProperties().get("Bar")).get$ref(),
        is("./relative-with-url/relative-with-local.yaml#/relative-same-file")
    );
	}
}
