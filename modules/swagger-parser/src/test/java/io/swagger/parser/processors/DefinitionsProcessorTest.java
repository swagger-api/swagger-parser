package io.swagger.parser.processors;

import io.swagger.models.Model;
import io.swagger.models.RefModel;
import io.swagger.models.Swagger;
import io.swagger.parser.ResolverCache;
import mockit.*;
import org.testng.annotations.Test;

import java.util.Map;

import static org.testng.Assert.assertEquals;

public class DefinitionsProcessorTest {


    @Mocked
    ModelProcessor modelProcessor;

    @Test
    public void testDefinitionsProcessor(@Injectable final Model model1,
                                         @Injectable final Model model2,
                                         @Injectable final ResolverCache cache) throws Exception {

        final Swagger swagger = new Swagger();
        swagger.addDefinition("foo", model1);
        swagger.addDefinition("bar", model2);


        new Expectations() {{
            new ModelProcessor(cache, swagger);
            times = 1;
            result = modelProcessor;
            modelProcessor.processModel((Model) any);
            times = 2;
        }};

        new DefinitionsProcessor(cache, swagger).processDefinitions();

        new Verifications() {{
            modelProcessor.processModel(model1);
            modelProcessor.processModel(model2);
        }};
    }

    @Test
    public void testNoDefinitionsDefined(@Injectable final Swagger swagger,
                                         @Injectable final ResolverCache cache) throws Exception {

        new StrictExpectations() {{
            new ModelProcessor(cache, swagger);
            times = 1;
            result = modelProcessor;
            swagger.getDefinitions();
            times = 1;
            result = null;
        }};

        new DefinitionsProcessor(cache, swagger).processDefinitions();

        new FullVerifications() {{
        }};
    }

    @Test
    public void testDefinitionsProcessor_RefModelInDefinitionsMap(
            @Injectable final Model resolvedModel) throws Exception {
        final Swagger swagger = new Swagger();
        final String ref = "http://my.company.com/path/to/file.json#/foo/bar";
        final RefModel refModel = new RefModel(ref);
        swagger.addDefinition("foo", refModel);

        final MockUp<ResolverCache> mockup = new MockUp<ResolverCache>() {
            @Mock(invocations = 1)
            String getRenamedRef(String ref) {
                swagger.getDefinitions().put("bar", resolvedModel);
                return "bar";
            }
        };

        final ResolverCache mockResolverCache = mockup.getMockInstance();


        new StrictExpectations() {{
            new ModelProcessor(mockResolverCache, swagger);
            times = 1;
            result = modelProcessor;

            modelProcessor.processModel(refModel);
            times = 1;
        }};

        new DefinitionsProcessor(mockResolverCache, swagger).processDefinitions();

        new FullVerifications(){{}};

        final Map<String, Model> definitions = swagger.getDefinitions();
        assertEquals(definitions.size(), 1);

        final Model foo = definitions.get("foo");
        assertEquals(foo, resolvedModel);
    }
}
