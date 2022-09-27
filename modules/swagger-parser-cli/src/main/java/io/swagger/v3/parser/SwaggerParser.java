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
    public static final String JSON = "json";
    public static final String YAML = "yaml";
    public static final String LOG_ERRORS = "l";
    public static final String OUTPUT_FILE = "o";
    public static final String TRUE = "true";
    public static final String INPUT_FILE = "i";

    public static void main(String[] args) {
        if (args.length > 0){
            ArgumentParser parser = ArgumentParsers.newFor("swagger-parser").build()
                    .defaultHelp(true);
            parser.addArgument("-i")
                    .dest(INPUT_FILE)
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
                    .dest(OUTPUT_FILE)
                    .type(String.class)
                    .help("output file parsed");
            parser.addArgument("-l")
                    .dest(LOG_ERRORS)
                    .type(String.class)
                    .help("output error logs");
            parser.addArgument("-json")
                    .dest(JSON)
                    .type(Boolean.class)
                    .action(Arguments.storeTrue())
                    .setDefault(false)
                    .help("generate file as JSON");
            parser.addArgument("-yaml")
                    .dest(YAML)
                    .type(Boolean.class)
                    .action(Arguments.storeTrue())
                    .setDefault(false)
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
        if ( messages != null && !messages.isEmpty() && arguments != null && arguments.getString(LOG_ERRORS) != null){
            if(arguments.getString(LOG_ERRORS) != null) {
                generateParsedFile(arguments, LOG_ERRORS, messages.toString());
            }
        }
    }

    public static List<String> readFromLocation(Namespace args) {
        List<String> messages = new ArrayList<>();
        ParseOptions options;
        try {
            options = setOptions(args);
            final SwaggerParseResult result = new OpenAPIV3Parser().readLocation(args.get(INPUT_FILE), null, options);
            if(args.getString(OUTPUT_FILE) != null) {
                if (result.getOpenAPI() != null){
                    String output;
                    if(args.getString(JSON) != null && args.getString(JSON).equals(TRUE)){
                        output = Json.pretty(result.getOpenAPI());
                    }else if(args.getString(YAML) != null && args.getString(YAML).equals(TRUE)){
                        output = Yaml.pretty(result.getOpenAPI());
                    }else{
                        output= Yaml.pretty(result.getOpenAPI());
                    }
                    generateParsedFile(args, OUTPUT_FILE, output );
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

        if (parseOptions.getString(RESOLVE) !=null && parseOptions.getString(RESOLVE).equals(TRUE)) {
            options.setResolve(true);
        }
        if (parseOptions.getString(RESOLVEFULLY) != null && parseOptions.getString(RESOLVEFULLY).equals(TRUE)) {
            options.setResolveFully(true);
        }
        if (parseOptions.getString(FLATTEN) != null && parseOptions.getString(FLATTEN).equals(TRUE)) {
            options.setFlatten(true);
        }
        return options;
    }
}