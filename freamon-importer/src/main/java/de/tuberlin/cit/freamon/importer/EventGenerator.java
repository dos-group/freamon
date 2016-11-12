package de.tuberlin.cit.freamon.importer;

import de.tuberlin.cit.freamon.results.DB;
import de.tuberlin.cit.freamon.results.EventModel;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Object creating events from the log files.
 */
class EventGenerator {

    private Connection connection;
    private final static Logger log = Logger.getLogger(EventGenerator.class);

    /**
     * Constructor of the object. Connection to the database is established here.
     */
    EventGenerator(){
        log.debug("EventGenerator started.");
        connection = DB.getConnection("jdbc:monetdb://localhost/freamon", "monetdb", "monetdb");
    }

    /**
     * Method for creating the event table entries for a given job.
     * @param master - an object specifying the job to be processed.
     */
    void generateEntries(Master master){
        int jobID = master.getJobID();
        log.debug("jobID = "+jobID);

        //Master
        int masterWorkerID = master.getWorkerID(master.getMasterPath());
        log.debug("masterWorkerID = "+masterWorkerID);
        CSVParser csvParser = new CSVParser(Organiser.firstLine, master.getMasterPath());
        List<Entry> masterEntries = csvParser.parseStringToEntry();
        List<String[]> masterRecords = new ArrayList<>();
        if (tableExists()){
            for (Entry e : masterEntries){
                long epoch = e.getEpoch();
                double cpu = e.getUsr();
                double mem = e.getMem_used();
                double netRx = e.getNet_recv();
                double netTx = e.getNet_send();
                double blkio = -1;
                String[] record_cpu = {String.valueOf(masterWorkerID), String.valueOf(jobID), "cpu", String.valueOf(epoch), String.valueOf(cpu)};
                String[] record_mem = {String.valueOf(masterWorkerID), String.valueOf(jobID), "mem", String.valueOf(epoch), String.valueOf(mem)};
                String[] record_netRx = {String.valueOf(masterWorkerID), String.valueOf(jobID), "netRx", String.valueOf(epoch), String.valueOf(netRx)};
                String[] record_netTx = {String.valueOf(masterWorkerID), String.valueOf(jobID), "netTx", String.valueOf(epoch), String.valueOf(netTx)};
                String[] record_blkio = {String.valueOf(masterWorkerID), String.valueOf(jobID), "blkio", String.valueOf(epoch), String.valueOf(blkio)};
                masterRecords.add(record_cpu);
                masterRecords.add(record_mem);
                masterRecords.add(record_netRx);
                masterRecords.add(record_netTx);
                masterRecords.add(record_blkio);
            }
            log.info("Inserting entries for master worker...");
            masterRecords.forEach(this::insertEventRecord);
        }

        //Slaves
        for (int i=0;i<master.getSlavesPaths().length-1;i++){
            int slaveWorkerID = master.getWorkerID(master.getSlavesPaths()[i]);
            log.debug("slaveWorkerID = "+slaveWorkerID);
            csvParser = new CSVParser(Organiser.firstLine, master.getSlavesPaths()[i]);
            List<Entry> slaveEntries = csvParser.parseStringToEntry();
            List<String[]> slaveRecords = new ArrayList<>();
            if (tableExists()){
                for (Entry e : slaveEntries){
                    long epoch = e.getEpoch();
                    double cpu = e.getUsr();
                    double mem = e.getMem_used();
                    double netRx = e.getNet_recv();
                    double netTx = e.getNet_send();
                    double blkio = -1;
                    String[] record_cpu = {String.valueOf(slaveWorkerID), String.valueOf(jobID), "cpu", String.valueOf(epoch), String.valueOf(cpu)};
                    String[] record_mem = {String.valueOf(slaveWorkerID), String.valueOf(jobID), "mem", String.valueOf(epoch), String.valueOf(mem)};
                    String[] record_netRx = {String.valueOf(slaveWorkerID), String.valueOf(jobID), "netRx", String.valueOf(epoch), String.valueOf(netRx)};
                    String[] record_netTx = {String.valueOf(slaveWorkerID), String.valueOf(jobID), "netTx", String.valueOf(epoch), String.valueOf(netTx)};
                    String[] record_blkio = {String.valueOf(slaveWorkerID), String.valueOf(jobID), "blkio", String.valueOf(epoch), String.valueOf(blkio)};
                    slaveRecords.add(record_cpu);
                    slaveRecords.add(record_mem);
                    slaveRecords.add(record_netRx);
                    slaveRecords.add(record_netTx);
                    slaveRecords.add(record_blkio);
                }
                log.info("Inserting records of slave "+(i+1)+" of "+(master.getSlavesPaths().length-1));
                slaveRecords.forEach(this::insertEventRecord);
            }
        }

    }

    /**
     * Method for checking if the event table exists and if not creating it.
     * @return - true if table exists (or has just been created); false if does not exist and could not be created.
     */
    private boolean tableExists(){
        String sql = "SELECT name FROM tables WHERE name = '"+ EventModel.tableName()+"';";
        try {
            PreparedStatement pstmt = connection.prepareStatement(sql);
            ResultSet resultSet = pstmt.executeQuery();
            //Table not found - Create table
            if (!resultSet.next()) {
                EventModel.createTable(connection);
            }
            else {
                //Table found
                if (resultSet.getString(1).equalsIgnoreCase(EventModel.tableName()))
                    return true;
            }
        }
        catch (SQLException e){
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Helper method for insertion of a record into the database.
     * @param record - record as a {@link String[]} object to be inserted.
     */
    private void insertEventRecord(String[] record){
        String sql = "INSERT INTO "+EventModel.tableName() + "(worker_id, job_id, kind, millis, value) VALUES (?, ?, ?, ?, ?);";
        PreparedStatement pstmt = null;
        try {
            pstmt = connection.prepareStatement(sql);
            pstmt.setInt(1, Integer.parseInt(record[0]));
            pstmt.setInt(2, Integer.parseInt(record[1]));
            pstmt.setString(3, record[2]);
            pstmt.setLong(4, Long.valueOf(record[3]));
            pstmt.setDouble(5, Double.valueOf(record[4]));
            pstmt.execute();
        }
        catch (SQLException e){
            e.printStackTrace();
        }
        finally {
            if (pstmt!=null){
                try {
                    pstmt.close();
                }
                catch (SQLException e){
                    e.printStackTrace();
                }
            }
        }
    }
}
