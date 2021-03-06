package de.tuberlin.cit.freamon.importer.generators;

import de.tuberlin.cit.freamon.importer.core.DstatImporter;
import de.tuberlin.cit.freamon.importer.core.Master;
import de.tuberlin.cit.freamon.results.DB;
import de.tuberlin.cit.freamon.results.JobModel;
import org.apache.log4j.Logger;

import java.sql.*;
import java.util.Random;
import java.util.UUID;

/**
 * Object responsible for inserting the records into job table.
 */
public class JobGenerator {

    private Connection connection;
    private final static Logger log = Logger.getLogger(JobGenerator.class);

    /**
     * Constructor of the object. The connection to the database is also established here.
     */
    public JobGenerator(){
        connection = DB.getConnection("jdbc:monetdb://localhost/freamon", "monetdb", "monetdb");
    }

    /**
     * Method for job generation and insertion into the database.
     * @param master - object specifying the job to be processed.
     * @return - updated {@link Master} object with jobID.
     */
    public Master generateAndInsertJob(Master master){
        log.debug("Received a job to be generated.");
        //generate job
        Random random = new Random();
        long start = 0;
        long stop = Long.parseLong(DstatImporter.jsonData[4]);
        String appID = DstatImporter.appName+"_"+stop+"_"+random.nextInt();
        String framework = ((DstatImporter.framework!=null)? DstatImporter.framework : "unknown");
        String signature = ((DstatImporter.signature!=null)? DstatImporter.signature : "unknown_signature");
        double input_size = DstatImporter.input_size;
        int noOfContainers = DstatImporter.numWorkers;
        int noOfCoresContainer = DstatImporter.coresWorker;
        int memoryContainer = DstatImporter.memoryWorker;
        int id = UUID.randomUUID().hashCode();
        master.setJobID(id);


        //insert job into DB
        String sql = "INSERT INTO "+JobModel.tableName() + " (id, yarn_application_id, framework, signature, input_size, num_workers, " +
                "cores_per_worker, memory_per_worker, start, stop) "+
                "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";

        if(!tableExists())
            createTable();

        log.debug("Attempting to insert the job into DB.");
        try {
            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setInt(1, id);
            pstmt.setString(2, appID);
            pstmt.setString(3, framework);
            pstmt.setString(4, signature);
            pstmt.setDouble(5, input_size);
            pstmt.setInt(6, noOfContainers);
            pstmt.setInt(7, noOfCoresContainer);
            pstmt.setInt(8, memoryContainer);
            pstmt.setLong(9, start);
            pstmt.setLong(10, stop);
            pstmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return master;

    }

    /**
     * Method for checking if the table exists in the database.
     * @return - true if exists or has successfully been created; false if unable to create it (if does not exist).
     */
    private boolean tableExists(){
        String sql = "SELECT name FROM tables WHERE name = '"+JobModel.tableName()+"';";
        try {
            PreparedStatement pstmt = connection.prepareStatement(sql);
            ResultSet resultSet = pstmt.executeQuery();
            //Table found
            if (resultSet.next()) {
                if (resultSet.getString(1).equalsIgnoreCase(JobModel.tableName()))
                    return true;
            }
        }
        catch (SQLException e){
            e.printStackTrace();
        }

        //Table not found
        return false;
    }

    private void createTable(){
        JobModel.createTable(connection);
    }
}
