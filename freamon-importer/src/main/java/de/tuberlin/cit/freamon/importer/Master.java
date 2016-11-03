package de.tuberlin.cit.freamon.importer;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

class Master {

    private String masterHostname;
    private String masterPath;
    private String statePath;
    private String[] slavesHostnames;
    private String[] slavesPaths;
    private Map<String, Integer> workerIDMapping = new HashMap<>();

    private int jobID;

    Master(String masterHostname, String masterPath, String statePath, String[] slavesHostnames, String[] slavesPaths){
        this.masterHostname = masterHostname;
        this.masterPath = masterPath;
        this.statePath = statePath;
        this.slavesHostnames = slavesHostnames;
        this.slavesPaths = slavesPaths;
    }

    void addWorkerMapping(String path, int workerID){
        this.workerIDMapping.put(path, workerID);
    }

    Set<String> getMappingHostnames(){
        return this.workerIDMapping.keySet();
    }

    int getWorkerID(String path){
        return this.workerIDMapping.get(path);
    }

    String getMasterHostname() {
        return masterHostname;
    }

    String getMasterPath() {
        return masterPath;
    }

    String getStatePath() {
        return statePath;
    }

    String[] getSlavesHostnames() {
        return slavesHostnames;
    }

    String[] getSlavesPaths() {
        return slavesPaths;
    }

    public int getJobID() {
        return jobID;
    }

    public void setJobID(int jobID) {
        this.jobID = jobID;
    }


}
