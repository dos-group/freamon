package de.tuberlin.cit.freamon.importer;

import de.tuberlin.cit.freamon.results.DB;
import de.tuberlin.cit.freamon.results.JobModel;
import org.apache.log4j.Logger;

import java.sql.*;
import java.util.Random;

class JobGenerator {

    private Connection connection;
    private final static Logger log = Logger.getLogger(JobGenerator.class);

    JobGenerator(){
        connection = DB.getConnection("jdbc:monetdb://localhost/freamon", "monetdb", "monetdb");
    }

    Master generateAndInsertJob(Master master){
        //generate job
        Random random = new Random();
        long start = 0;
        long stop = Long.parseLong(Organiser.jsonData[4]);
        String appID = "application"+"_"+stop+"_"+random.nextInt();
        String framework = ((Organiser.framework!=null)? Organiser.framework : "unknown");
        String signature = ((Organiser.signature!=null)? Organiser.signature : "unknown_signature");
        double datasetSize = Organiser.datasetSize;
        int noOfContainers = Organiser.numWorkers;
        int noOfCoresContainer = Organiser.coresWorker;
        int memoryContainer = Organiser.memoryWorker;
        int id = this.getLastJobID() + 1;
        master.setJobID(id);


        //insert job into DB
        String sql = "INSERT INTO "+JobModel.tableName() + " (id, app_id, framework, signature, dataset_size, num_containers, " +
                "cores_per_container, memory_per_container, start, stop) "+
                "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
        try {
            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setInt(1, id);
            pstmt.setString(2, appID);
            pstmt.setString(3, framework);
            pstmt.setString(4, signature);
            pstmt.setDouble(5, datasetSize);
            pstmt.setInt(6, noOfContainers);
            pstmt.setInt(7, noOfCoresContainer);
            pstmt.setInt(8, memoryContainer);
            pstmt.setLong(9, start);
            pstmt.setLong(10, stop);
            pstmt.execute();
        }
        catch (SQLException e){
            e.printStackTrace();
        }

        return master;

    }

    private int getLastJobID(){
        String sql = "SELECT max (\"id\") from "+JobModel.tableName()+";";
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

    private boolean tableExists(){
        String sql = "SELECT name FROM tables WHERE name = '"+JobModel.tableName()+"';";
        try {
            PreparedStatement pstmt = connection.prepareStatement(sql);
            ResultSet resultSet = pstmt.executeQuery();
            //Table not found - Create table
            if (!resultSet.next()) {
                JobModel.createTable(connection);
            }
            else {
                //Table found
                if (resultSet.getString(1).equalsIgnoreCase(JobModel.tableName()))
                    return true;
            }
        }
        catch (SQLException e){
            e.printStackTrace();
        }

        return false;
    }
}
