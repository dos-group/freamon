package de.tuberlin.cit.freamon.importer;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class Organiser {

    private final static Logger log = Logger.getLogger(Organiser.class);
    static String[] files;
    static String[] jsonData;
    static int firstLine, epoch, usr, sys, idl, wai, hiq, siq, mem_used, mem_buff, mem_cach, mem_free, net_recv, net_send, dsk_read, dsk_writ;
    static int jID, appID, datasetSize, numContainers, coresContainer, memoryContainer;
    static String framework, signature;

    public static void main(String[] args){
        new Organiser(args);
    }

    private Organiser(String[] args){
        log.info("Freamon Importer started.");
        CLIParser cli = new CLIParser();
        cli.processCLIParameters(args);
        Map<String, List<Entry>> fileEntries = new HashMap<>();
        if (files!= null) {
            if (files[0].substring(files[0].length()-4, files[0].length()).equalsIgnoreCase("json")){
                JSONParser jsonParser = new JSONParser(files[0]);
                jsonData = jsonParser.extractDataFromJSON();
                for (String d : jsonData)
                    System.out.println(d);
            }
            else
                log.error("Wrong file parameter provided. A json file was expected.");
            if (files[1].substring(files[1].length()-1,files[1].length()).equalsIgnoreCase("/")){
                try (Stream<Path> paths = Files.walk(Paths.get(files[1]))) {
                    paths.forEach(filePath -> {
                        if (Files.isRegularFile(filePath)){
                            System.out.println(filePath);
                            fileEntries.put(filePath.toString(), null);
                        }
                    });
                }
                catch (IOException e){
                    e.printStackTrace();
                }
            }
        }
        if (!fileEntries.keySet().isEmpty()){
            for (String file : fileEntries.keySet()){
                CSVParser csvParser = new CSVParser(firstLine, file);
                csvParser.readFileToStringBuilder();
                List<Entry> results = csvParser.parseStringToEntry();
                fileEntries.put(file, results);
            }
        }
    }
}
