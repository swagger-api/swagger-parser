package io.swagger.parser.processors;


import io.swagger.models.Operation;
import io.swagger.models.PathImpl;
import io.swagger.models.RefPath;
import io.swagger.models.Swagger;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.refs.RefFormat;
import io.swagger.parser.ResolverCache;
import mockit.Expectations;
import mockit.FullVerifications;
import mockit.Injectable;
import mockit.Mocked;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;

public class PathsProcessorTest {

    @Injectable
    ResolverCache cache;

    @Mocked
    ParameterProcessor parameterProcessor;

    @Mocked
    OperationProcessor operationProcessor;

    @Test
    public void testProcessPaths(@Injectable final List<Parameter> nonRefPathParameters,
                                 @Injectable final List<Parameter> processedNonRefPathParameters,
                                 @Injectable final List<Parameter> resolvedPathParamters,
                                 @Injectable final List<Parameter> processedResolvedPathParameters,
                                 @Injectable final Operation nonRefPathOperation,
                                 @Injectable final Operation resolvedPathOperation) throws Exception {

        final Swagger swagger = new Swagger();

        final String ref = "http://my.company.com/path/to/file.json#/foo";
        swagger.path("/foo", new RefPath(ref));

        final PathImpl nonRefPath = new PathImpl();
        swagger.path("/bar", nonRefPath);
        nonRefPath.setParameters(nonRefPathParameters);
        nonRefPath.get(nonRefPathOperation);

        final PathImpl resolvedPath = new PathImpl();
        resolvedPath.setParameters(resolvedPathParamters);
        resolvedPath.get(resolvedPathOperation);

        new Expectations() {{
            new ParameterProcessor(cache, swagger);
            times = 1;
            result = parameterProcessor;
            new OperationProcessor(cache, swagger);
            times = 1;
            result = operationProcessor;

            cache.loadRef(ref, RefFormat.URL, PathImpl.class);
            times = 1;
            result = resolvedPath;

            parameterProcessor.processParameters(resolvedPathParamters);
            times = 1;
            result = processedResolvedPathParameters;

            parameterProcessor.processParameters(nonRefPathParameters);
            times=1;
            result = processedNonRefPathParameters;

            operationProcessor.processOperation(resolvedPathOperation);
            operationProcessor.processOperation(nonRefPathOperation);
        }};

        new PathsProcessor(cache, swagger).processPaths();

        new FullVerifications(){{}};

        assertEquals(nonRefPath.getParameters(), processedNonRefPathParameters);
        assertEquals(swagger.getPath("/foo"), resolvedPath);
        assertEquals(resolvedPath.getParameters(), processedResolvedPathParameters);
    }
}
