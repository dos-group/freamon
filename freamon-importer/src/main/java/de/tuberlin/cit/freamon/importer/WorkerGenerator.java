package de.tuberlin.cit.freamon.importer;

import de.tuberlin.cit.freamon.results.DB;
import de.tuberlin.cit.freamon.results.WorkerModel;
import org.apache.log4j.Logger;

import java.sql.*;

class WorkerGenerator {


    private Connection connection;
    private final static Logger log = Logger.getLogger(WorkerGenerator.class);

    WorkerGenerator(){
        connection = DB.getConnection("jdbc:monetdb://localhost/freamon", "monetdb", "monetdb");
    }

    Master generateAndInsertMasterWorker(Master master){
        int jobID = master.getJobID();
        int masterWorkerID = this.getWorkerID(master.getMasterHostname());
        if (masterWorkerID==0)
            masterWorkerID = this.getLastWorkerID() + 1;
        String masterHostname = master.getMasterHostname();

        String[] masterWorkerEntry = {String.valueOf(masterWorkerID), String.valueOf(jobID), masterHostname};
        this.insertWorker(masterWorkerEntry);
        master.addWorkerMapping(master.getMasterPath(), masterWorkerID);

        return master;
    }

    Master generateAndInsertSlaveWorkers(Master master){
        int jobID = master.getJobID();
        int slaveWorkerID;
        String slaveHostname;
        for (int i=0;i<master.getSlavesHostnames().length;i++){
            if (master.getSlavesHostnames()[i]!=null){
                slaveWorkerID = this.getWorkerID(master.getSlavesHostnames()[i]);
                if (slaveWorkerID==0)
                    slaveWorkerID = this.getLastWorkerID() + 1;
                slaveHostname = master.getSlavesHostnames()[i];
                String[] slaveWorkerEntry = {String.valueOf(slaveWorkerID), String.valueOf(jobID), slaveHostname};
                this.insertWorker(slaveWorkerEntry);
                master.addWorkerMapping(master.getSlavesPaths()[i], slaveWorkerID);
            }
        }
        return master;
    }

    private void insertWorker(String[] workerEntry){
        String sql = "INSERT INTO "+WorkerModel.tableName() + "(id, job_id, hostname, is_yarn) VALUES (?, ?, ?, ?);";
        PreparedStatement pstmt = null;

        try {
            pstmt = connection.prepareStatement(sql);
            pstmt.setInt(1, Integer.parseInt(workerEntry[0]));
            pstmt.setInt(2, Integer.parseInt(workerEntry[1]));
            pstmt.setString(3, workerEntry[2]);
            pstmt.setBoolean(4, false);
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





    private int getLastWorkerID(){
        String sql = "SELECT max (\"id\") from "+ WorkerModel.tableName()+";";
        int result = 0;
        if (tableExists()){
            log.debug("Table exists.");
            Statement stmt = null;

            try {
                stmt = connection.createStatement();
                ResultSet resultSet = stmt.executeQuery(sql);
                while (resultSet.next())
                    result = resultSet.getInt(1);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            finally {
                if (stmt != null)
                    try {
                        stmt.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
            }
        }
        return result;
    }

    private int getWorkerID(String hostname){
        int result = 0;
        if (tableExists()){
            String sql = "SELECT id FROM "+WorkerModel.tableName()+" WHERE hostname = ?;";
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

    private boolean tableExists(){
        String sql = "SELECT name FROM tables WHERE name = '"+ WorkerModel.tableName()+"';";
        try {
            PreparedStatement pstmt = connection.prepareStatement(sql);
            ResultSet resultSet = pstmt.executeQuery();
            //Table not found - Create table
            if (!resultSet.next()) {
                WorkerModel.createTable(connection);
            }
            else {
                //Table found
                if (resultSet.getString(1).equalsIgnoreCase(WorkerModel.tableName()))
                    return true;
            }
        }
        catch (SQLException e){
            e.printStackTrace();
        }

        return false;
    }
}
