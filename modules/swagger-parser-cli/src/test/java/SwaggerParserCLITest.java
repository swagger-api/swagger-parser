import io.swagger.v3.parser.SwaggerParser;
import org.testng.Assert;
import org.testng.annotations.Test;

public class SwaggerParserCLITest {
    @Test
    public void validateOKFromLocationTest(){
        String []args = new String[1];
        args[0] = "src/test/resources/fileWithNoErrorMessages.yaml";
        Assert.assertTrue(SwaggerParser.readFromLocation(args[0]).size() == 0);
    }

    @Test
    public void validateErrorFromLocationTest(){
        String []args = new String[1];
        args[0] = "src/test/resources/fileWithValidationErrorMessages.yaml";
        Assert.assertEquals(SwaggerParser.readFromLocation(args[0]).get(0), "attribute info.version is missing");
        Assert.assertEquals(SwaggerParser.readFromLocation(args[0]).get(1), "attribute paths.'/cu'(post).responses.200.description is missing");
    }

    @Test
    public void validateFileNotFoundInLocationTest(){
        String []args = new String[1];
        args[0] = "src/test/resources/WrongLocation.yaml";
        Assert.assertTrue(SwaggerParser.readFromLocation(args[0]).size() == 1);
        Assert.assertEquals(SwaggerParser.readFromLocation(args[0]).get(0), "Unable to read location `src/test/resources/WrongLocation.yaml`");
    }
}