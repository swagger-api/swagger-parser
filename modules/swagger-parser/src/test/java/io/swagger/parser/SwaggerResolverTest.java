package io.swagger.parser;

import io.swagger.models.Swagger;
import io.swagger.models.auth.AuthorizationValue;
import io.swagger.parser.processors.DefinitionsProcessor;
import io.swagger.parser.processors.PathsProcessor;
import mockit.Injectable;
import mockit.Mocked;
import mockit.StrictExpectations;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

/**
 * Created by russellb337 on 7/15/15.
 */
public class SwaggerResolverTest {

    @Test
    public void testSwaggerResolver(@Injectable final Swagger swagger,
                                    @Injectable final List<AuthorizationValue> auths,
                                    @Mocked final ResolverCache cache,
                                    @Mocked final DefinitionsProcessor definitionsProcessor,
                                    @Mocked final PathsProcessor pathsProcessor) throws Exception {

        new StrictExpectations() {{
            new ResolverCache(swagger, auths, null);
            result = cache;
            times = 1;

            new DefinitionsProcessor(cache, swagger);
            result = definitionsProcessor;
            times=1;

            new PathsProcessor(cache, swagger);
            result = pathsProcessor;
            times=1;

            pathsProcessor.processPaths();
            times=1;

            definitionsProcessor.processDefinitions();
            times=1;

        }};

        assertEquals(new SwaggerResolver(swagger, auths, null).resolve(), swagger);
    }

    @Test
    public void testSwaggerResolver_NullSwagger() throws Exception {
        assertNull(new SwaggerResolver(null, null, null).resolve());
    }
}
