package de.tuberlin.cit.freamon.importer;

import org.apache.commons.cli.*;
import org.apache.log4j.Logger;

class CLIParser {

    private final static Logger log = Logger.getLogger(CLIParser.class);

    void processCLIParameters(String[] args){
        Options options = new Options();

        //TODO add more options
        Option files = new Option("f", "file", true, "Path to JSON file");
        options.addOption(files);

        Option directory = new Option("d", "directory", true, "Path to directory containing the dstat files");
        options.addOption(directory);

        Option firstLine = new Option("fL", "firstLine", true, "First line containing the data (counting starts from 0)");
        options.addOption(firstLine);

        Option epoch = new Option("e", "epoch", true, "column number containing 'epoch' (counting starts from 0)");
        options.addOption(epoch);

        Option usr = new Option("u", "usr", true, "column number containing 'usr' (counting starts from 0)");
        options.addOption(usr);

        Option sys = new Option("s", "sys", true, "column number containing 'sys' (counting starts from 0)");
        options.addOption(sys);

        Option idl = new Option("i", "idl", true, "column number containing 'idl' (counting starts from 0)");
        options.addOption(idl);

        Option wai = new Option("w", "wai", true, "column number containing 'wai' (counting starts from 0)");
        options.addOption(wai);

        Option hiq = new Option("h", "hiq", true, "column number containing 'hiq' (counting starts from 0)");
        options.addOption(hiq);

        Option siq = new Option("S", "siq", true, "column number containing 'siq' (counting starts from 0)");
        options.addOption(siq);

        Option mem_used = new Option("mu", "mem-used", true, "column number containing 'used (memory)' (counting starts from 0)");
        options.addOption(mem_used);

        Option mem_buff = new Option("mb", "mem-buff", true, "column number containing 'buff (memory)' (counting starts from 0)");
        options.addOption(mem_buff);

        Option mem_cach = new Option("mc", "mem-cache", true, "column number containing 'cach (memory)' (counting starts from 0)");
        options.addOption(mem_cach);

        Option mem_free = new Option("mf", "mem-free", true, "column number containing 'free (memory)' (counting starts from 0)");
        options.addOption(mem_free);

        Option net_recv = new Option("nr", "net-rec", true, "column number containing 'recv (net)' (counting starts from 0)");
        options.addOption(net_recv);

        Option net_send = new Option("ns", "net-send", true, "column number containing 'send (net)' (counting starts from 0)");
        options.addOption(net_send);

        Option dsk_read = new Option("dr", "dsk-read", true, "column number containing 'read (dsk)' (counting starts from 0)");
        options.addOption(dsk_read);

        Option dsk_writ = new Option("dw", "dsk-writ", true, "column number containing 'writ (dsk)' (counting starts from 0)");
        options.addOption(dsk_writ);

        Option jID = new Option("jID", "job-id", true, "job ID");
        options.addOption(jID);

        Option appID = new Option("aID", "app-id", true, "app ID");
        options.addOption(appID);

        Option framework = new Option("fw", "framework", true, "framework");
        options.addOption(framework);

        Option signature = new Option("sign", "signature", true, "signature");
        options.addOption(signature);

        Option datasetSize = new Option("dS", "dataset-size", true, "dataset size");
        options.addOption(datasetSize);

        Option numWorkers = new Option("nC", "num-workers", true, "number of workers (e.g. containers)");
        options.addOption(numWorkers);

        Option workerCores = new Option("cC", "cores-worker", true, "number of cores per worker");
        options.addOption(workerCores);

        Option workerMemory = new Option("mC", "memory-worker", true, "amount of memory per worker");
        options.addOption(workerMemory);

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            log.error("Caught ParseException whilst trying to parse your parameters: "+e);
            e.printStackTrace();
        }
        if (cmd!=null && cmd.hasOption("f") && cmd.hasOption("d")){
            Organiser.files = new String[2];
            Organiser.files[0] = cmd.getOptionValue("f");
            Organiser.files[1] = cmd.getOptionValue("d");
        }
        if (cmd!=null && cmd.hasOption("fL"))
            Organiser.firstLine = Integer.parseInt(cmd.getOptionValue("fL", "0"));

        if (cmd!=null && cmd.hasOption("e"))
            Organiser.epoch = Integer.parseInt(cmd.getOptionValue("e", "-1"));

        if (cmd!=null && cmd.hasOption("u"))
            Organiser.usr = Integer.parseInt(cmd.getOptionValue("u", "-1"));

        if (cmd!=null && cmd.hasOption("s"))
            Organiser.sys = Integer.parseInt(cmd.getOptionValue("s", "-1"));

        if (cmd!=null && cmd.hasOption("i"))
            Organiser.idl = Integer.parseInt(cmd.getOptionValue("i", "-1"));

        if (cmd!=null && cmd.hasOption("w"))
            Organiser.wai = Integer.parseInt(cmd.getOptionValue("w", "-1"));

        if (cmd!=null && cmd.hasOption("h"))
            Organiser.hiq = Integer.parseInt(cmd.getOptionValue("h", "-1"));

        if (cmd!=null && cmd.hasOption("S"))
            Organiser.siq = Integer.parseInt(cmd.getOptionValue("S", "-1"));

        if (cmd!=null && cmd.hasOption("mu"))
            Organiser.mem_used = Integer.parseInt(cmd.getOptionValue("mu", "-1"));

        if (cmd!=null && cmd.hasOption("mb"))
            Organiser.mem_buff = Integer.parseInt(cmd.getOptionValue("mb", "-1"));

        if (cmd!=null && cmd.hasOption("mc"))
            Organiser.mem_cach = Integer.parseInt(cmd.getOptionValue("mc", "-1"));

        if (cmd!=null && cmd.hasOption("mf"))
            Organiser.mem_free = Integer.parseInt(cmd.getOptionValue("mf", "-1"));

        if (cmd!=null && cmd.hasOption("nr"))
            Organiser.net_recv = Integer.parseInt(cmd.getOptionValue("nr", "-1"));

        if (cmd!=null && cmd.hasOption("ns"))
            Organiser.net_send = Integer.parseInt(cmd.getOptionValue("ns", "-1"));

        if (cmd!=null && cmd.hasOption("dr"))
            Organiser.dsk_read = Integer.parseInt(cmd.getOptionValue("dr", "-1"));

        if (cmd!=null && cmd.hasOption("dw"))
            Organiser.dsk_writ = Integer.parseInt(cmd.getOptionValue("dw", "-1"));

        if (cmd!=null && cmd.hasOption("jID"))
            Organiser.jID = Integer.parseInt(cmd.getOptionValue("jID", "-1"));

        if (cmd!=null && cmd.hasOption("appID"))
            Organiser.appID = Integer.parseInt(cmd.getOptionValue("appID", "-1"));

        if (cmd!=null && cmd.hasOption("dS"))
            Organiser.datasetSize = Integer.parseInt(cmd.getOptionValue("dS", "-1"));

        if (cmd!=null && cmd.hasOption("fw"))
            Organiser.framework = cmd.getOptionValue("fw");

        if (cmd!=null && cmd.hasOption("sign"))
            Organiser.signature = cmd.getOptionValue("sign");

        if (cmd!=null && cmd.hasOption("nC"))
            Organiser.numContainers = Integer.parseInt(cmd.getOptionValue("nC", "-1"));

        if (cmd!=null && cmd.hasOption("dS"))
            Organiser.datasetSize = Integer.parseInt(cmd.getOptionValue("dS", "-1"));

        if (cmd!=null && cmd.hasOption("cC"))
            Organiser.coresContainer = Integer.parseInt(cmd.getOptionValue("cC", "-1"));

        if (cmd!=null && cmd.hasOption("mC"))
            Organiser.memoryContainer = Integer.parseInt(cmd.getOptionValue("mC", "-1"));



        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java -jar freamon-importer.jar OPTIONS", options);
    }
}
