package io.swagger.parser.processors;

import static org.testng.AssertJUnit.assertTrue;
import io.swagger.models.Model;
import io.swagger.models.ModelImpl;
import io.swagger.models.RefModel;
import io.swagger.models.Swagger;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.RefProperty;
import io.swagger.models.properties.StringProperty;
import io.swagger.models.refs.RefFormat;
import io.swagger.parser.ResolverCache;
import mockit.Expectations;
import mockit.Injectable;
import mockit.StrictExpectations;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;


public class ExternalRefProcessorTest {

    @Injectable
    ResolverCache cache;

    @Injectable
    Swagger swagger;

    @Test
    public void testProcessRefToExternalDefinition_NoNameConflict(
            @Injectable final Model mockedModel) throws Exception {

        final String ref = "http://my.company.com/path/to/file.json#/foo/bar";
        final RefFormat refFormat = RefFormat.URL;

        new StrictExpectations() {{
            cache.loadRef(ref, refFormat, Model.class);
            times = 1;
            result = mockedModel;

            swagger.getDefinitions();
            times = 1;
            result = null;

            cache.putRenamedRef(ref, "bar");
            swagger.addDefinition("bar", mockedModel); times=1;
        }};

        String newRef = new ExternalRefProcessor(cache, swagger).processRefToExternalDefinition(ref, refFormat);
        assertEquals(newRef, "bar");
    }


    @Test
    public void testProcessRefToExternalDefinition_NameConflict_FirstAppearance(
            @Injectable final Model mockedModel) throws Exception {

        final String ref = "http://my.company.com/path/to/file.json#/foo/bar";
        final RefFormat refFormat = RefFormat.URL;

        final Map<String, Model> definitionsMap = new HashMap<>();
        definitionsMap.put("bar", new ModelImpl());

        final String expectedNewRef = "bar1";

        new StrictExpectations() {{
            cache.loadRef(ref, refFormat, Model.class);
            times = 1;
            result = mockedModel;

            swagger.getDefinitions();
            times = 1;
            result = definitionsMap;

            cache.getRenamedRef(ref);
            times = 1;
            result = null;

            cache.putRenamedRef(ref, expectedNewRef); times=1;

            swagger.addDefinition(expectedNewRef, mockedModel);
        }};

        String newRef = new ExternalRefProcessor(cache, swagger).processRefToExternalDefinition(ref, refFormat);
        assertEquals(newRef, expectedNewRef);
    }

    @Test
    public void testProcessRefToExternalDefinition_NameConflict_SecondAppearance(@Injectable final Model mockedModel) throws Exception {
        final String ref = "http://my.company.com/path/to/file.json#/foo/bar";
        final RefFormat refFormat = RefFormat.URL;

        final Map<String, Model> definitionsMap = new HashMap<>();
        definitionsMap.put("bar", new ModelImpl());
        definitionsMap.put("bar1", mockedModel);

        final String expectedNewRef = "bar1";


        new StrictExpectations() {{
            cache.loadRef(ref, refFormat, Model.class);
            times = 1;
            result = mockedModel;

            swagger.getDefinitions();
            times = 1;
            result = definitionsMap;

            cache.getRenamedRef(ref);
            times = 1;
            result = expectedNewRef;
        }};

        String newRef = new ExternalRefProcessor(cache, swagger).processRefToExternalDefinition(ref, refFormat);
        assertEquals(newRef, expectedNewRef);
    }

    @Test
    public void testNestedExternalRefs(@Injectable final Model mockedModel){
    	final RefFormat refFormat = RefFormat.URL;

    	//Swagger test instance
    	Swagger testedSwagger = new Swagger();

    	//Start with customer, add address property to it
    	final String customerURL = "http://my.company.com/path/to/customer.json#/definitions/Customer";

    	final Model customerModel = new ModelImpl();
    	Map<String,Property> custProps = new HashMap<String, Property>();
    	RefProperty address = new RefProperty();
    	final String addressURL = "http://my.company.com/path/to/address.json#/definitions/Address";
    	address.set$ref(addressURL);
    	custProps.put("Address", address);
    	customerModel.setProperties(custProps);

    	//create address model, add Contact Ref Property to it
    	final Model addressModel = new ModelImpl();
    	Map<String, Property> addressProps = new HashMap<String, Property>();
    	RefProperty contact = new RefProperty();
    	final String contactURL = "http://my.company.com/path/to/Contact.json#/definitions/Contact";
    	contact.set$ref(contactURL);
    	addressProps.put("Contact", contact);
    	addressModel.setProperties(addressProps);


    	//Create contact model, with basic type property
    	final Model contactModel = new ModelImpl();
    	Property contactProp = new StringProperty();
    	contactProp.setName("PhoneNumber");
    	contactProp.setRequired(true);
    	Map<String, Property> contactProps = new HashMap<String, Property>();
    	contactProps.put("PhoneNumber", contactProp);
    	contactModel.setProperties(contactProps);

    	new Expectations(){{
    		cache.loadRef(customerURL, refFormat, Model.class);
    		result = customerModel;
    		times = 1;

    		cache.loadRef(addressURL, refFormat, Model.class);
    		result = addressModel;
    		times = 1;

    		cache.loadRef(contactURL, refFormat, Model.class);
    		result = contactModel;
    		times = 1;
 		}};

    	String actualRef = new ExternalRefProcessor(cache, testedSwagger).processRefToExternalDefinition(customerURL, refFormat);

    	assertTrue(testedSwagger.getDefinitions().get("Customer")!=null);
    	assertTrue(testedSwagger.getDefinitions().get("Contact")!=null);
    	assertTrue(testedSwagger.getDefinitions().get("Address")!=null);
    	assertTrue(testedSwagger.getDefinitions().size()==3);
    }
}
