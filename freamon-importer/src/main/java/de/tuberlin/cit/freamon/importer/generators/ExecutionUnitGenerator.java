package de.tuberlin.cit.freamon.importer.generators;

import de.tuberlin.cit.freamon.importer.core.Master;
import de.tuberlin.cit.freamon.results.DB;
import de.tuberlin.cit.freamon.results.ExecutionUnitModel;
import org.apache.log4j.Logger;

import java.sql.*;
import java.util.UUID;

/**
 * Object for creation of the workers table.
 */
public class ExecutionUnitGenerator {


    private Connection connection;
    private final static Logger log = Logger.getLogger(ExecutionUnitGenerator.class);

    /**
     * Constructor of the object. The connection to the database is also established here.
     */
    public ExecutionUnitGenerator(){
        connection = DB.getConnection("jdbc:monetdb://localhost/freamon", "monetdb", "monetdb");
    }

    /**
     * Method for generating a table entry for a master worker
     * @param master - an object specifying the job
     * @return - updated {@link Master} object with workerID for master being stored.
     */
    public Master generateAndInsertMasterWorker(Master master){
        int jobID = master.getJobID();
        int masterWorkerID = this.getWorkerID(master.getMasterHostname());
        if (masterWorkerID==0)
            masterWorkerID = UUID.randomUUID().hashCode();
        String masterHostname = master.getMasterHostname();

        String[] masterWorkerEntry = {String.valueOf(masterWorkerID), String.valueOf(jobID), masterHostname, "true"};
        this.insertWorker(masterWorkerEntry);
        master.addWorkerMapping(master.getMasterPath(), masterWorkerID);

        return master;
    }

    /**
     * Method for generating table entries for slave workers.
     * @param master - an object specifying the job.
     * @return - updated {@link Master} object with workerIDs of slave workers.
     */
    public Master generateAndInsertSlaveWorkers(Master master){
        int jobID = master.getJobID();
        int slaveWorkerID;
        String slaveHostname;
        for (int i=0;i<master.getSlavesHostnames().length;i++){
            if (master.getSlavesHostnames()[i]!=null){
                slaveWorkerID = this.getWorkerID(master.getSlavesHostnames()[i]);
                if (slaveWorkerID==0)
                    slaveWorkerID = UUID.randomUUID().hashCode();
                slaveHostname = master.getSlavesHostnames()[i];
                String[] slaveWorkerEntry = {String.valueOf(slaveWorkerID), String.valueOf(jobID), slaveHostname, "false"};
                this.insertWorker(slaveWorkerEntry);
                master.addWorkerMapping(master.getSlavesPaths()[i], slaveWorkerID);
            }
        }
        return master;
    }

    /**
     * Helper method for insertion of records to the database.
     * @param workerEntry - entry to be inserted as a {@link String[]} object.
     */
    private void insertWorker(String[] workerEntry){
        String sql = "INSERT INTO "+ExecutionUnitModel.tableName() + "(id, job_id, hostname, is_yarn_container, is_master) VALUES (?, ?, ?, ?, ?);";
        PreparedStatement pstmt = null;
        log.debug("ExecUnit Entry: id: "+workerEntry[0] + ", job_id: "+workerEntry[1] + ", hostname: "+workerEntry[2]+", is_master: "+workerEntry[3]);

        try {
            pstmt = connection.prepareStatement(sql);
            pstmt.setInt(1, Integer.parseInt(workerEntry[0]));
            pstmt.setInt(2, Integer.parseInt(workerEntry[1]));
            pstmt.setString(3, workerEntry[2]);
            pstmt.setBoolean(4, false);
            pstmt.setBoolean(5, Boolean.parseBoolean(workerEntry[3]));
            pstmt.execute();
        }
        catch (SQLException e){
            e.printStackTrace();
        }
        finally {
            if (pstmt!=null){
                try {
                    pstmt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Method for querying the database for a workerID based on the hostname of the machine
     * @param hostname - hostname to be queried
     * @return - workerID as an integer or 0 (zero).
     */
    private int getWorkerID(String hostname){
        int result = 0;
        if (tableExists()){
            String sql = "SELECT id FROM "+ ExecutionUnitModel.tableName()+" WHERE hostname = ?;";
            try {
                PreparedStatement pstmt = connection.prepareStatement(sql);
                pstmt.setString(1, hostname);
                ResultSet resultSet = pstmt.executeQuery();
                while (resultSet.next()){
                    result = resultSet.getInt(1);
                }
            }
            catch (SQLException e){
                e.printStackTrace();
            }
        }
        return result;
    }

    /**
     * Method for checking if the table exists and if not creating it.
     * @return - true if table exists or has successfully been created; else false.
     */
    private boolean tableExists(){
        String sql = "SELECT name FROM tables WHERE name = '"+ ExecutionUnitModel.tableName()+"';";
        try {
            PreparedStatement pstmt = connection.prepareStatement(sql);
            ResultSet resultSet = pstmt.executeQuery();
            //Table not found - Create table
            if (!resultSet.next()) {
                ExecutionUnitModel.createTable(connection);
            }
            else {
                //Table found
                if (resultSet.getString(1).equalsIgnoreCase(ExecutionUnitModel.tableName()))
                    return true;
            }
        }
        catch (SQLException e){
            e.printStackTrace();
        }

        return false;
    }
}
