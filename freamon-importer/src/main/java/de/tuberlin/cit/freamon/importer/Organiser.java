package de.tuberlin.cit.freamon.importer;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class Organiser {

    private final static Logger log = Logger.getLogger(Organiser.class);
    static int firstLine, epoch, usr, sys, idl, wai, hiq, siq, mem_used, mem_buff, mem_cach, mem_free, net_recv, net_send, dsk_read, dsk_writ;
    static int numWorkers, coresWorker, memoryWorker;
    static double input_size;
    static String framework, signature, folder, appName, subfolder;
    static String[] jsonData;

    private List<String> folderEntries = new ArrayList<>();

    /**
     * Default Java method for starting programmes.
     * @param args - parameters passed to the programme.
     */
    public static void main(String[] args){
        new Organiser(args);
    }

    /**
     * Constructor of the Organiser object. The whole sequence of operations is controlled here.
     * @param args - parameters from the main method.
     */
    private Organiser(String[] args){
        log.info("Freamon Importer started.");
        CLIParser cli = new CLIParser();
        JobGenerator jobGenerator = new JobGenerator();
        cli.processCLIParameters(args);

        this.populateSubfolders();

        List<Master> masters = new ArrayList<>();
        List<Master> processedMasters = new ArrayList<>();

        for (String s : folderEntries){
            Master m = this.generateMastersWithSlaves(s);
            Master m_JID = jobGenerator.generateAndInsertJob(m);
            masters.add(m_JID);
            System.out.println("------------------------------------------");
        }

        ExecutionUnitGenerator executionUnitGenerator = new ExecutionUnitGenerator();

        for (Master master : masters){
            Master m_AfterMasterProcessed = executionUnitGenerator.generateAndInsertMasterWorker(master);
            Master m_AfterSlavesProcessed = executionUnitGenerator.generateAndInsertSlaveWorkers(m_AfterMasterProcessed);
            processedMasters.add(m_AfterSlavesProcessed);
        }
        log.debug("There are "+processedMasters.size()+" processed master entries.");

        EventGenerator eventGenerator = new EventGenerator();

        processedMasters.forEach(eventGenerator::generateEntries);
        log.info("Starting to populate the database with events. Please be patient.");
        eventGenerator.writeEventRecords();
        log.info("Finished.");
    }

    /**
     * Method for discovering the top-level folders of the jobs to be added to the database.
     * It also saves the folders in the folderEntries ArrayList.
     */
    private void populateSubfolders(){
        if (folder != null) {
            DirectoryStream.Filter<Path> filter = file -> (Files.isDirectory(file));

            Path directory = FileSystems.getDefault().getPath(folder);
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, filter)){
                for (Path path : stream){
                    if (!path.getFileName().toString().contains("fail"))
                        folderEntries.add(path.toAbsolutePath().toString());
                }
            }
            catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    /**
     * Method for locating the state.json file as well as the logs from the workers (including master) and storing them in a Master object.
     * @param path - location of a job folder.
     * @return - a Master object containing job details.
     */
    private Master generateMastersWithSlaves(String path){
        String state = path + "/state.json";
        JSONParser jsonParser = new JSONParser(state);
        jsonData = jsonParser.extractDataFromJSON();
        log.info("State: "+state);
        Map<String, Integer> files = this.getListOfFilesAndDirectories(path+"/"+subfolder);
        for (String s : files.keySet())
            log.info("files: "+s);
        String masterPath = null;
        String masterHostname = null;
        int noOfFiles = 0;
        for (String s : files.keySet()){
            if (files.get(s)==1)
                noOfFiles++;
        }
        String[] slavesPaths = new String[noOfFiles];
        String[] slavesHosts = new String[noOfFiles];
        int i=0;
        for (String s : files.keySet()){
            if (files.get(s)==1) {
                if (isMaster(s)) {
                    masterPath = s;
                    String[] splitPath = s.split("/");
                    masterHostname = splitPath[splitPath.length - 1].substring(0, splitPath[splitPath.length - 1].length() - 4);
                    log.debug("Added master. Hostname: " + masterHostname + ", Path: " + masterPath);
                } else {
                    slavesPaths[i] = s;
                    String[] splitPath = s.split("/");
                    String slaveHost = splitPath[splitPath.length - 1].substring(0, splitPath[splitPath.length - 1].length() - 4);
                    slaveHost += ".cit.tu-berlin.de";
                    slavesHosts[i] = slaveHost;
                    log.debug("Added slave. Hostname: " + slavesHosts[i] + ", Path: " + slavesPaths[i]);
                    i++;
                }
            }
        }
        return new Master(masterHostname, masterPath, slavesHosts, slavesPaths);

    }

    /**
     * Method for checking if a given machine is a master worker.
     * @param path - path to the log file.
     * @return - true if master; false otherwise
     */
    private boolean isMaster(String path){
        return path.contains("cit.tu-berlin.de");
    }

    /**
     * Helper method for getting a list of files and directories in a specific location.
     * @param path - location to be examined for files and folders.
     * @return - a {@link Map} object containing the full path to a folder or file and an integer to indicate whether it is a file (1)
     * or a folder (2).
     */
    private Map<String, Integer> getListOfFilesAndDirectories(String path){
        log.info("The path: "+path);
        Map<String, Integer> files = new HashMap<>();
        try(Stream<Path> paths = Files.walk(Paths.get(path))){
            paths.forEach(filePath -> {
                if (Files.isRegularFile(filePath)){
                    files.put(filePath.toString(), 1);
                }
                else if (Files.isDirectory(filePath)){
                    files.put(filePath.toString(), 2);
                }
            });
        }
        catch (IOException e){
            e.printStackTrace();
        }
        return files;
    }
}
