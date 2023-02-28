import io.swagger.v3.parser.SwaggerParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SwaggerParserCLITest {
    @Test
    public void validateOKFromLocationTest(){
        Map<String,Object> args = new HashMap();
        args.put("i","src/test/resources/fileWithNoErrorMessages.yaml");
        Namespace namespace = new Namespace(args);
        Assert.assertTrue(SwaggerParser.readFromLocation(namespace).size() == 0);
    }

    @Test
    public void validateErrorFromLocationTest(){
        Map<String,Object> args = new HashMap();
        args.put("i","src/test/resources/fileWithValidationErrorMessages.yaml");
        Namespace namespace = new Namespace(args);
        Assert.assertEquals(SwaggerParser.readFromLocation(namespace).toString(), "[attribute info.version is missing, attribute paths.'/cu'(post).responses.200.description is missing]");
    }

    @Test
    public void validateErrorTest(){
        Map<String,Object> args = new HashMap();
        args.put("i","src/test/resources/fileWithValidationErrorMessages.yaml");
        args.put("resolve", "true");
        args.put("resolvefully", "true");
        args.put("o", "target/test-classes/parsedSpec.yaml");
        args.put("l", "target/test-classes/errorLogs.yaml");
        Namespace namespace = new Namespace(args);
        Assert.assertEquals(SwaggerParser.readFromLocation(namespace).toString(), "[attribute info.version is missing, attribute paths.'/cu'(post).responses.200.description is missing]");
    }

    @Test
    public void validateFileNotFoundInLocationTest(){
        Map<String,Object> args = new HashMap();
        args.put("i","src/test/resources/WrongLocation.yaml");
        args.put("l", "target/test-classes/errorLogs.yaml");
        Namespace namespace = new Namespace(args);
        List<String> messages = new ArrayList<>();
        try {
            messages   = SwaggerParser.readFromLocation(namespace);
        }catch (Exception e){
            Assert.fail("error");
        }
        Assert.assertTrue( messages.size() == 1);
        Assert.assertEquals(messages.toString(), "[Unable to read location `src/test/resources/WrongLocation.yaml`]");
    }

    @Test
    public void validateOKFromLocationWithResolveOptionTest(){
        Map<String,Object> args = new HashMap();
        args.put("i","src/test/resources/internal-references-in-external-files/main.yaml");
        args.put("resolve", "true");
        Namespace namespace = new Namespace(args);
        Assert.assertTrue(SwaggerParser.readFromLocation(namespace).size() == 0);
    }

    @Test
    public void validateOKFromLocationWithResolveFullyOptionTestYaml(){
        Map<String,Object> args = new HashMap();
        args.put("i","src/test/resources/internal-references-in-external-files/main.yaml");
        args.put("resolve", "true");
        args.put("resolvefully", "true");
        args.put("yaml", "true");
        args.put("o", "target/test-classes/parsedSpec.yaml");
        Namespace namespace = new Namespace(args);
        Path path = Paths.get("target/test-classes/parsedSpec.yaml");

        Assert.assertTrue(SwaggerParser.readFromLocation(namespace).size() == 0);
        Assert.assertTrue(Files.exists(path));
    }

    @Test
    public void validateOKFromLocationWithResolveFullyOptionTestJson(){
        Map<String,Object> args = new HashMap();
        args.put("i","src/test/resources/internal-references-in-external-files/main.yaml");
        args.put("resolve", "true");
        args.put("resolvefully", "true");
        args.put("json", "true");
        args.put("o", "target/test-classes/parsedSpec.json");
        Namespace namespace = new Namespace(args);
        Path path = Paths.get("target/test-classes/parsedSpec.json");

        Assert.assertTrue(SwaggerParser.readFromLocation(namespace).size() == 0);
        Assert.assertTrue(Files.exists(path));
    }

    @Test
    public void validateOKFromLocationWithOnlyResolveFullyOptionTest(){
        Map<String,Object> args = new HashMap();
        args.put("i","src/test/resources/internal-references-in-external-files/main.yaml");
        args.put("resolvefully","true");
        Namespace namespace = new Namespace(args);
        Assert.assertTrue(SwaggerParser.readFromLocation(namespace).size() == 0);
    }

    @Test
    public void validateOKFromLocationWithFlattenOptionTest() throws ArgumentParserException {
        Map<String,Object> args = new HashMap();
        args.put("i","src/test/resources/internal-references-in-external-files/main.yaml");
        args.put("resolve", "true");
        args.put("resolvefully", "true");
        args.put("flatten", "true");
        args.put("o", "target/test-classes/parsedSpec.yaml");
        args.put("l", "target/test-classes/errorLogs.yaml");
        Namespace namespace = new Namespace(args);
        Assert.assertTrue(SwaggerParser.readFromLocation(namespace).size() == 0);
    }

    @Test
    public void validateOKFromLocationWithResolveFullyOptionTestYamlMain(){
        String[] args = new String[5];
        args[0]="-i=src/test/resources/internal-references-in-external-files/main.yaml";
        args[1]="-resolve";
        args[2]="-resolveFully";
        args[3]="-yaml";
        args[4]="-o=target/test-classes/parsedSpec.yaml";

        Path path = Paths.get("target/test-classes/parsedSpec.yaml");
        SwaggerParser.main(args);
        Assert.assertTrue(Files.exists(path));
    }

    @Test
    public void validateOKFromLocationWithResolveFullyOptionTestJSONMain(){
        String[] args = new String[5];
        args[0]="-i=src/test/resources/internal-references-in-external-files/main.yaml";
        args[1]="-resolve";
        args[2]="-resolveFully";
        args[3]="-json";
        args[4]="-o=target/test-classes/parsedSpec.json";

        Path path = Paths.get("target/test-classes/parsedSpec.json");
        SwaggerParser.main(args);
        Assert.assertTrue(Files.exists(path));
    }
}