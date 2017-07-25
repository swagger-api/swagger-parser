package io.swagger.parser;

import io.swagger.models.auth.AuthorizationValue;
import io.swagger.parser.util.SwaggerDeserializationResult;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.Collections;

@Mojo( name = "validate")
public class ValidateMojo extends AbstractMojo
{
    @Parameter(property = "swaggerSpecPath", required = true)
    private String swaggerSpecPath;

    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("Reading and validating spec at " +  swaggerSpecPath);

        SwaggerDeserializationResult result = new SwaggerParser().readWithInfo(swaggerSpecPath, Collections.<AuthorizationValue>emptyList(), false);
        if (!result.getMessages().isEmpty()) {
            for (String message : result.getMessages()) {
                getLog().error(message);
            }
            throw new MojoFailureException("There are validation failures.");
        }
    }
}