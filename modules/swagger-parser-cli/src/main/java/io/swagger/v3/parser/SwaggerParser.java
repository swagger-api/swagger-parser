package io.swagger.v3.parser;

import io.swagger.v3.parser.core.models.SwaggerParseResult;
import java.util.ArrayList;
import java.util.List;

public class SwaggerParser {
    public static void main(String[] args) {
        if (args.length > 0){
            List<String> messages = readFromLocation(args[0]);
            if ( messages.size() > 0){
                messages.forEach(System.out::println);
                System.exit(1);
            }
        }
    }

    public static List<String> readFromLocation(String location) {
        List<String> messages = new ArrayList<>();
        try {
            final SwaggerParseResult result = new OpenAPIV3Parser().readLocation(location, null, null);
            if(result.getOpenAPI() == null || !result.getMessages().isEmpty()){
                messages = result.getMessages();
            }
        }catch (Exception e){
            e.printStackTrace();
            System.exit(1);
        }
        return messages;
    }
}
