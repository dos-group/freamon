package de.tuberlin.cit.freamon.importer;

import org.apache.commons.cli.*;
import org.apache.log4j.Logger;

/**
 * Object handling the parsing of the arguments into the programme.
 */
class CLIParser {

    private final static Logger log = Logger.getLogger(CLIParser.class);

    /**
     * Method declaring and processing the parameters.
     * @param args - parameters passed into the programme.
     */
    void processCLIParameters(String[] args){
        Options options = new Options();

        Option directory = new Option("d", "directory", true, "REQUIRED: Path to directory containing all jobs.");
        directory.setRequired(true);
        options.addOption(directory);

        Option appName = new Option("an", "app-name", true, "REQUIRED: Application name");
        appName.setRequired(true);
        options.addOption(appName);

        Option firstLine = new Option("fL", "firstLine", true, "REQUIRED: First line containing the data (counting starts from 0)");
        firstLine.setRequired(true);
        options.addOption(firstLine);

        Option epoch = new Option("e", "epoch", true, "REQUIRED: column number containing 'epoch' (counting starts from 0)");
        epoch.setRequired(true);
        options.addOption(epoch);

        Option subfolder = new Option("sf", "subfolder", true, "REQUIRED: Subfolder structure within a job.");
        options.addOption(subfolder);

        Option usr = new Option("u", "usr", true, "REQUIRED: column number containing 'usr' (counting starts from 0)");
        usr.setRequired(true);
        options.addOption(usr);

        Option sys = new Option("s", "sys", true, "REQUIRED: column number containing 'sys' (counting starts from 0)");
        sys.setRequired(true);
        options.addOption(sys);

        Option idl = new Option("i", "idl", true, "REQUIRED: column number containing 'idl' (counting starts from 0)");
        sys.setRequired(true);
        options.addOption(idl);

        Option wai = new Option("w", "wai", true, "REQUIRED: column number containing 'wai' (counting starts from 0)");
        wai.setRequired(true);
        options.addOption(wai);

        Option hiq = new Option("h", "hiq", true, "column number containing 'hiq' (counting starts from 0)");
        options.addOption(hiq);

        Option siq = new Option("S", "siq", true, "column number containing 'siq' (counting starts from 0)");
        options.addOption(siq);

        Option mem_used = new Option("mu", "mem-used", true, "REQUIRED: column number containing 'used (memory)' (counting starts from 0)");
        mem_used.setRequired(true);
        options.addOption(mem_used);

        Option mem_buff = new Option("mb", "mem-buff", true, "column number containing 'buff (memory)' (counting starts from 0)");
        options.addOption(mem_buff);

        Option mem_cach = new Option("mc", "mem-cache", true, "column number containing 'cach (memory)' (counting starts from 0)");
        options.addOption(mem_cach);

        Option mem_free = new Option("mf", "mem-free", true, "column number containing 'free (memory)' (counting starts from 0)");
        options.addOption(mem_free);

        Option net_recv = new Option("nr", "net-rec", true, "REQUIRED: column number containing 'recv (net)' (counting starts from 0)");
        net_recv.setRequired(true);
        options.addOption(net_recv);

        Option net_send = new Option("ns", "net-send", true, "REQUIRED: column number containing 'send (net)' (counting starts from 0)");
        net_send.setRequired(true);
        options.addOption(net_send);

        Option dsk_read = new Option("dr", "dsk-read", true, "column number containing 'read (dsk)' (counting starts from 0)");
        dsk_read.setRequired(true);
        options.addOption(dsk_read);

        Option dsk_writ = new Option("dw", "dsk-writ", true, "column number containing 'writ (dsk)' (counting starts from 0)");
        dsk_writ.setRequired(true);
        options.addOption(dsk_writ);

        Option framework = new Option("fw", "framework", true, "framework");
        options.addOption(framework);

        Option signature = new Option("sign", "signature", true, "signature");
        options.addOption(signature);

        Option inputSize = new Option("iS", "input-size", true, "input size");
        options.addOption(inputSize);

        Option coresWorker = new Option("cw", "cores-worker", true, "number of cores per worker");
        options.addOption(coresWorker);

        Option memoryWorker = new Option("mw", "memory-worker", true, "amount of memory per worker");
        options.addOption(memoryWorker);

        Option numWorkers = new Option("nC", "num-workers", true, "number of workers (e.g. containers)");
        options.addOption(numWorkers);

        Option workerCores = new Option("cC", "cores-worker", true, "number of cores per worker");
        options.addOption(workerCores);

        Option workerMemory = new Option("mC", "memory-worker", true, "amount of memory per worker");
        options.addOption(workerMemory);

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;

        HelpFormatter formatter = new HelpFormatter();


        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            log.error("Caught ParseException whilst trying to parse your parameters: "+e);
            e.printStackTrace();
        }
        if (cmd!=null) {

            if (!cmd.hasOption("d")){
                log.error("The required parameter -d (--directory) has not been provided.");
                formatter.printHelp("java -jar freamon-importer.jar OPTIONS", options);
                System.exit(1);
            }
            if (!cmd.hasOption("an")){
                log.error("The required parameter -an (--app-name) has not been provided");
                formatter.printHelp("java -jar freamon-importer.jar OPTIONS", options);
                System.exit(1);
            }
            if (!cmd.hasOption("fL")){
                log.error("The required parameter -fl (--firstLine) has not been provided");
                formatter.printHelp("java -jar freamon-importer.jar OPTIONS", options);
                System.exit(1);
            }
            if (!cmd.hasOption("e")){
                log.error("The required parameter -e (--epoch) has not been provided.");
                formatter.printHelp("java -jar freamon-importer.jar OPTIONS", options);
                System.exit(1);
            }
            if (!cmd.hasOption("sf")){
                log.error("The required parameter -sf (--subfolder) has not been provided.");
                formatter.printHelp("java -jar freamon-importer.jar OPTIONS", options);
                System.exit(1);
            }
            if (!cmd.hasOption("mu")){
                log.error("The required parameter -mu (--mem-used) has not been provided.");
                formatter.printHelp("java -jar freamon-importer.jar OPTIONS", options);
                System.exit(1);
            }
            if (!cmd.hasOption("nr")){
                log.error("The required parameter -nr (--net-rec) has not been provided.");
                formatter.printHelp("java -jar freamon-importer.jar OPTIONS", options);
                System.exit(1);
            }
            if (!cmd.hasOption("ns")){
                log.error("The required parameter -ns (--net-send) has not been provided.");
                formatter.printHelp("java -jar freamon-importer.jar OPTIONS", options);
                System.exit(1);
            }
            if (!cmd.hasOption("u")){
                log.error("The required parameter -u (--usr) has not been provided.");
                formatter.printHelp("java -jar freamon-importer.jar OPTIONS", options);
                System.exit(1);
            }
            if (!cmd.hasOption("s")){
                log.error("The required parameter -s (--sys) has not been provided.");
                formatter.printHelp("java -jar freamon-importer.jar OPTIONS", options);
                System.exit(1);
            }
            if (!cmd.hasOption("i")){
                log.error("The required parameter -i (--idl) has not been provided.");
                formatter.printHelp("java - jar freamon-importer.jar OPTIONS", options);
                System.exit(1);
            }
            if (!cmd.hasOption("w")){
                log.error("The required parameter -w (--wai) has not been provided.");
                formatter.printHelp("java -jar freamon-importer.jar OPTIONS", options);
                System.exit(1);
            }
            if (!cmd.hasOption("dr")){
                log.error("The required parameter -dr (--dsk-read) has not been provided.");
                formatter.printHelp("java -jar freamon-importer.jar OPTIONS", options);
                System.exit(1);
            }
            if (!cmd.hasOption("dw")){
                log.error("The required parameter -dw (--disk-writ) has not been provided.");
                formatter.printHelp("java -jar freamon-importer.jar OPTIONS", options);
                System.exit(1);
            }


            Organiser.folder = cmd.getOptionValue("d");

            Organiser.firstLine = Integer.parseInt(cmd.getOptionValue("fL", "0"));

            Organiser.appName = cmd.getOptionValue("an", "application");

            Organiser.epoch = Integer.parseInt(cmd.getOptionValue("e", "-1"));

            Organiser.subfolder = cmd.getOptionValue("sf");

            Organiser.usr = Integer.parseInt(cmd.getOptionValue("u", "-1"));

            Organiser.sys = Integer.parseInt(cmd.getOptionValue("s", "-1"));

            Organiser.idl = Integer.parseInt(cmd.getOptionValue("i", "-1"));

            Organiser.wai = Integer.parseInt(cmd.getOptionValue("w", "-1"));

            Organiser.hiq = Integer.parseInt(cmd.getOptionValue("h", "-1"));

            Organiser.siq = Integer.parseInt(cmd.getOptionValue("S", "-1"));

            Organiser.mem_used = Integer.parseInt(cmd.getOptionValue("mu", "-1"));

            Organiser.mem_buff = Integer.parseInt(cmd.getOptionValue("mb", "-1"));

            Organiser.mem_cach = Integer.parseInt(cmd.getOptionValue("mc", "-1"));

            Organiser.mem_free = Integer.parseInt(cmd.getOptionValue("mf", "-1"));

            Organiser.net_recv = Integer.parseInt(cmd.getOptionValue("nr", "-1"));

            Organiser.net_send = Integer.parseInt(cmd.getOptionValue("ns", "-1"));

            Organiser.dsk_read = Integer.parseInt(cmd.getOptionValue("dr", "-1"));

            Organiser.dsk_writ = Integer.parseInt(cmd.getOptionValue("dw", "-1"));

            Organiser.input_size = Integer.parseInt(cmd.getOptionValue("iS", "-1"));

            Organiser.framework = cmd.getOptionValue("fw", "Freamon");

            Organiser.signature = cmd.getOptionValue("sign", "unknown_signature");

            Organiser.numWorkers = Integer.parseInt(cmd.getOptionValue("nw", "-1"));

            Organiser.input_size = Integer.parseInt(cmd.getOptionValue("iS", "-1"));

            Organiser.coresWorker = Integer.parseInt(cmd.getOptionValue("cw", "-1"));

            Organiser.memoryWorker = Integer.parseInt(cmd.getOptionValue("mw", "-1"));
        }


    }
}
