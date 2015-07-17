package io.swagger.parser.processors;

import io.swagger.models.Model;
import io.swagger.models.Swagger;
import io.swagger.parser.ResolverCache;
import mockit.*;
import org.testng.annotations.Test;

public class DefinitionsProcessorTest {

    @Injectable
    ResolverCache cache;

    @Mocked
    ModelProcessor modelProcessor;

    @Test
    public void testDefinitionsProcessor(@Injectable final Model model1, @Injectable final Model model2) throws Exception {

        final Swagger swagger = new Swagger();
        swagger.addDefinition("foo", model1);
        swagger.addDefinition("bar", model2);


        new Expectations() {{
            new ModelProcessor(cache, swagger); times=1; result=modelProcessor;
            modelProcessor.processModel((Model) any); times=2;
        }};

        new DefinitionsProcessor(cache, swagger).processDefinitions();

        new Verifications() {{
            modelProcessor.processModel(model1);
            modelProcessor.processModel(model2);
        }};
    }

    @Test
    public void testNoDefinitionsDefined(@Injectable final Swagger swagger) throws Exception {

        new StrictExpectations(){{
            swagger.getDefinitions(); times=1; result=null;
        }};

        new DefinitionsProcessor(cache, swagger).processDefinitions();
    }
}
