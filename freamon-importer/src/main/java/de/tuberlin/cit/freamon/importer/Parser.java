package de.tuberlin.cit.freamon.importer;

import org.apache.log4j.Logger;
public class Parser {

    private final static Logger log = Logger.getLogger(Parser.class);
    static String[] files;

    public static void main(String[] args){
        new Parser(args);
    }

    private Parser(String[] args){
        log.info("Freamon Import started.");
        CLIParser cli = new CLIParser();
        cli.processCLIParameters(args);
        if (files!= null) {
            for (String s : files)
                System.out.println("Received file: " + s);
        }
    }
}
