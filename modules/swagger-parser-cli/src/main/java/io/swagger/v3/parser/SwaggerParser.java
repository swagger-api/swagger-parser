package io.swagger.v3.parser;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Yaml;

import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;


import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;


public class SwaggerParser {

    public static final String RESOLVE = "resolve";
    public static final String RESOLVEFULLY = "resolvefully";
    public static final String FLATTEN = "flatten";

    public static void main(String[] args) {
        if (args.length > 0){
            ArgumentParser parser = ArgumentParsers.newFor("swagger-parser").build()
                    .defaultHelp(true);
            parser.addArgument("-i")
                    .dest("i")
                    .required(true)
                    .type(String.class)
                    .help("input file to be parsed");
            parser.addArgument("-resolve")
                    .dest(RESOLVE)
                    .type(Boolean.class)
                    .action(Arguments.storeTrue())
                    .setDefault(false)
                    .help("resolve remote or local references");
            parser.addArgument("-resolveFully")
                    .dest(RESOLVEFULLY)
                    .type(Boolean.class)
                    .action(Arguments.storeTrue())
                    .setDefault(false)
                    .help("");
            parser.addArgument("-flatten")
                    .dest(FLATTEN)
                    .type(Boolean.class)
                    .action(Arguments.storeTrue())
                    .setDefault(false)
                    .help("");
            parser.addArgument("-o")
                    .dest("o")
                    .type(String.class)
                    .help("output file parsed");
            parser.addArgument("-l")
                    .dest("l")
                    .type(String.class)
                    .help("output error logs");
            parser.addArgument("-json")
                    .dest("json")
                    .type(String.class)
                    .help("generate file as JSON");
            parser.addArgument("-yaml")
                    .dest("yaml")
                    .type(String.class)
                    .help("generate file as YAML");
            try{
                readFromLocation(parser.parseArgs(args));
            }catch (ArgumentParserException e) {
                parser.handleError(e);
                System.exit(1);
            }
        }
    }

    private static void generateMessagesFile(List<String> messages, Namespace arguments) {
        if ( messages != null && !messages.isEmpty() && arguments != null && arguments.getString("l") != null){
            if(arguments.getString("l") != null) {
                generateParsedFile(arguments, "l", messages.toString());
            }
        }
    }

    public static List<String> readFromLocation(Namespace args) {
        List<String> messages = new ArrayList<>();
        ParseOptions options;
        try {
            options = setOptions(args);
            final SwaggerParseResult result = new OpenAPIV3Parser().readLocation(args.get("i"), null, options);
            if(args.getString("o") != null) {
                if (result.getOpenAPI() != null){
                    String output;
                    if(args.getString("json") != null){
                        output = Json.pretty(result.getOpenAPI());
                    }else if(args.getString("yaml") != null){
                        output = Yaml.pretty(result.getOpenAPI());
                    }else{
                        output= Yaml.pretty(result.getOpenAPI());
                    }
                    generateParsedFile(args, "o", output );
                }
            }
            if(result.getOpenAPI() == null || !result.getMessages().isEmpty()){
                messages = result.getMessages();
                generateMessagesFile(messages, args);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return messages;
    }

    private static void generateParsedFile(Namespace args, String o, String result) {
        try {
            if(result != null) {
                OutputStream out = Files.newOutputStream(Paths.get(args.getString(o)));
                byte[] specBytes = result.getBytes();
                out.write(specBytes);
                out.close();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private static ParseOptions setOptions(Namespace parseOptions) {
        ParseOptions options  = new ParseOptions();

        if (parseOptions.getString(RESOLVE) !=null && parseOptions.getString(RESOLVE).equals("true")) {
            options.setResolve(true);
        }
        if (parseOptions.getString(RESOLVEFULLY) != null && parseOptions.getString(RESOLVEFULLY).equals("true")) {
            options.setResolveFully(true);
        }
        if (parseOptions.getString(FLATTEN) != null && parseOptions.getString(FLATTEN).equals("true")) {
            options.setFlatten(true);
        }
        return options;
    }
}