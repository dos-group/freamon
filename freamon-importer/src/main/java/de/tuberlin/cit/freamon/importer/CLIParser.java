package de.tuberlin.cit.freamon.importer;

import org.apache.commons.cli.*;
import org.apache.log4j.Logger;

public class CLIParser {

    private final static Logger log = Logger.getLogger(CLIParser.class);

    void processCLIParameters(String[] args){
        Options options = new Options();

        //TODO add more options
        Option option = new Option("f", "files", true, "One or more files to be parsed");
        option.setArgs(Option.UNLIMITED_VALUES);
        options.addOption(option);

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            log.error("Caught ParseException whilst trying to parse your parameters: "+e);
            e.printStackTrace();
        }
        if (cmd.hasOption("f")){
            Parser.files = cmd.getOptionValues("f");
        }
    }
}
