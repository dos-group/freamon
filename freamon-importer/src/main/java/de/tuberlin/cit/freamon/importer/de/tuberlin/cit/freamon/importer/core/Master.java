package de.tuberlin.cit.freamon.importer.de.tuberlin.cit.freamon.importer.core;

import java.util.HashMap;
import java.util.Map;

/**
 * Object for storing job details such as the jobID, master hostname and log file location (and the same for the slaves).
 */
public class Master {

    private String masterHostname;
    private String masterPath;
    private String[] slavesHostnames;
    private String[] slavesPaths;
    private Map<String, Integer> workerIDMapping = new HashMap<>();

    private int jobID;

    /**
     * Constructor of the Master object.
     * @param masterHostname - hostname of the master worker.
     * @param masterPath - path to the master worker log file.
     * @param slavesHostnames - hostnames of the slave workers.
     * @param slavesPaths - paths to the log files of the slave workers.
     */
    Master(String masterHostname, String masterPath, String[] slavesHostnames, String[] slavesPaths){
        this.masterHostname = masterHostname;
        this.masterPath = masterPath;
        this.slavesHostnames = slavesHostnames;
        this.slavesPaths = slavesPaths;
    }

    /**
     * Method for saving the mapping of a path to a log file and the workerID.
     * @param path - path to the log file of a worker.
     * @param workerID - worker identifier.
     */
    public void addWorkerMapping(String path, int workerID){
        this.workerIDMapping.put(path, workerID);
    }


    /**
     * Method for retrieval of the worker identifier in exchange for a path to the log file of a worker.
     * @param path - location of the log file of a worker.
     * @return - worker identifier as a integer.
     */
    public int getWorkerID(String path){
        return this.workerIDMapping.get(path);
    }

    /**
     * Getter of the hostname of the master worker.
     * @return - hostname of the master worker.
     */
    public String getMasterHostname() {
        return masterHostname;
    }

    /**
     * Getter of the path to the log file of the master worker.
     * @return - path to the log file of the master worker.
     */
    public String getMasterPath() {
        return masterPath;
    }

    /**
     * Getter of the hostnames of slave workers.
     * @return - hostnames of slave workers as a {@link String[]} object.
     */
    public String[] getSlavesHostnames() {
        return slavesHostnames;
    }

    /**
     * Getter of the paths to the log files of slave workers.
     * @return - paths of log files of slave workers as a {@link String[]} object.
     */
    public String[] getSlavesPaths() {
        return slavesPaths;
    }

    /**
     * Getter of the job identifier
     * @return - job identifier as an integer.
     */
    public int getJobID() {
        return jobID;
    }

    /**
     * Setter of the job identifier
     * @param jobID - jobID as an integer.
     */
    public void setJobID(int jobID) {
        this.jobID = jobID;
    }


}
