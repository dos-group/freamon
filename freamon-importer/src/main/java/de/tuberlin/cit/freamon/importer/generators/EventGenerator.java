package de.tuberlin.cit.freamon.importer.generators;

import de.tuberlin.cit.freamon.importer.util.Entry;
import de.tuberlin.cit.freamon.importer.core.DstatImporter;
import de.tuberlin.cit.freamon.importer.core.Master;
import de.tuberlin.cit.freamon.importer.util.CSVParser;
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
public class EventGenerator {

    private Connection connection;
    private final static Logger log = Logger.getLogger(EventGenerator.class);
    private List<String[]> recordsToBeWritten;

    /**
     * Constructor of the object. Connection to the database is established here.
     */
    public EventGenerator(){
        log.debug("EventGenerator started.");
        connection = DB.getConnection("jdbc:monetdb://localhost/freamon", "monetdb", "monetdb");
		ensureTableExists();
        recordsToBeWritten = new ArrayList<>();
    }

    /**
     * Method for creating the event table entries for a given job.
     * @param master - an object specifying the job to be processed.
     */
    public void generateEntries(Master master){
        int jobID = master.getJobID();
        log.debug("jobID = "+jobID);

        //Master
        int masterWorkerID = master.getWorkerID(master.getMasterPath());
        log.debug("masterWorkerID = "+masterWorkerID);
        CSVParser csvParser = new CSVParser(DstatImporter.firstLine, master.getMasterPath());
        List<Entry> masterEntries = csvParser.parseStringToEntry();
        List<String[]> masterRecords = new ArrayList<>();

		for (Entry e : masterEntries){
			long epoch = e.getEpoch();
			double cpuUsr = e.getUsr();
			double cpuSys = e.getSys();
			double cpuIdl = e.getIdl();
			double cpuIOWait = e.getWai();
			double mem = e.getMem_used();
			double netRx = e.getNet_recv();
			double netTx = e.getNet_send();
			double dsk_read = e.getDsk_read();
			double dsk_writ = e.getDsk_writ();
			String[] record_cpuUsr = {String.valueOf(masterWorkerID), String.valueOf(jobID), "cpuUsr", String.valueOf(epoch), String.valueOf(cpuUsr)};
			String[] record_cpuSys = {String.valueOf(masterWorkerID), String.valueOf(jobID), "cpuSys", String.valueOf(epoch), String.valueOf(cpuSys)};
			String[] record_cpuIdl = {String.valueOf(masterWorkerID), String.valueOf(jobID), "cpuIdl", String.valueOf(epoch), String.valueOf(cpuIdl)};
			String[] record_cpuIOWait = {String.valueOf(masterWorkerID), String.valueOf(jobID), "cpuIOWait", String.valueOf(epoch), String.valueOf(cpuIOWait)};
			String[] record_mem = {String.valueOf(masterWorkerID), String.valueOf(jobID), "mem", String.valueOf(epoch), String.valueOf(mem)};
			String[] record_netRx = {String.valueOf(masterWorkerID), String.valueOf(jobID), "netRx", String.valueOf(epoch), String.valueOf(netRx)};
			String[] record_netTx = {String.valueOf(masterWorkerID), String.valueOf(jobID), "netTx", String.valueOf(epoch), String.valueOf(netTx)};
			String[] record_dskRead = {String.valueOf(masterWorkerID), String.valueOf(jobID), "diskRead", String.valueOf(epoch), String.valueOf(dsk_read)};
			String[] record_dskWrit = {String.valueOf(masterWorkerID), String.valueOf(jobID), "diskWrite", String.valueOf(epoch), String.valueOf(dsk_writ)};
			masterRecords.add(record_cpuUsr);
			masterRecords.add(record_cpuSys);
			masterRecords.add(record_cpuIdl);
			masterRecords.add(record_cpuIOWait);
			masterRecords.add(record_mem);
			masterRecords.add(record_netRx);
			masterRecords.add(record_netTx);
			masterRecords.add(record_dskRead);
			masterRecords.add(record_dskWrit);
		}
		log.info("Inserting entries for master worker...");
		masterRecords.forEach(this::insertEventRecord);

        //Slaves
        for (int i=0;i<master.getSlavesPaths().length-1;i++){
            int slaveWorkerID = master.getWorkerID(master.getSlavesPaths()[i]);
            log.debug("slaveWorkerID = "+slaveWorkerID);
            csvParser = new CSVParser(DstatImporter.firstLine, master.getSlavesPaths()[i]);
            List<Entry> slaveEntries = csvParser.parseStringToEntry();
            List<String[]> slaveRecords = new ArrayList<>();

			for (Entry e : slaveEntries){
				long epoch = e.getEpoch();
				double cpuUsr = e.getUsr();
				double cpuSys = e.getSys();
				double cpuIdl = e.getIdl();
				double cpuIOWait = e.getWai();
				double mem = e.getMem_used();
				double netRx = e.getNet_recv();
				double netTx = e.getNet_send();
				double dsk_read = e.getDsk_read();
				double dsk_writ = e.getDsk_writ();
				String[] record_cpuUsr = {String.valueOf(slaveWorkerID), String.valueOf(jobID), "cpuUsr", String.valueOf(epoch), String.valueOf(cpuUsr)};
				String[] record_cpuSys = {String.valueOf(slaveWorkerID), String.valueOf(jobID), "cpuSys", String.valueOf(epoch), String.valueOf(cpuSys)};
				String[] record_cpuIdl = {String.valueOf(slaveWorkerID), String.valueOf(jobID), "cpuIdl", String.valueOf(epoch), String.valueOf(cpuIdl)};
				String[] record_cpuIOWait = {String.valueOf(slaveWorkerID), String.valueOf(jobID), "cpuIOWait", String.valueOf(epoch), String.valueOf(cpuIOWait)};
				String[] record_mem = {String.valueOf(slaveWorkerID), String.valueOf(jobID), "mem", String.valueOf(epoch), String.valueOf(mem)};
				String[] record_netRx = {String.valueOf(slaveWorkerID), String.valueOf(jobID), "netRx", String.valueOf(epoch), String.valueOf(netRx)};
				String[] record_netTx = {String.valueOf(slaveWorkerID), String.valueOf(jobID), "netTx", String.valueOf(epoch), String.valueOf(netTx)};
				String[] record_dskRead = {String.valueOf(slaveWorkerID), String.valueOf(jobID), "diskRead", String.valueOf(epoch), String.valueOf(dsk_read)};
				String[] record_dskWrit = {String.valueOf(slaveWorkerID), String.valueOf(jobID), "diskWrite", String.valueOf(epoch), String.valueOf(dsk_writ)};
				slaveRecords.add(record_cpuUsr);
				slaveRecords.add(record_cpuSys);
				slaveRecords.add(record_cpuIdl);
				slaveRecords.add(record_cpuIOWait);
				slaveRecords.add(record_mem);
				slaveRecords.add(record_netRx);
				slaveRecords.add(record_netTx);
				slaveRecords.add(record_dskRead);
				slaveRecords.add(record_dskWrit);
			}
			log.info("Inserting records of slave "+(i+1)+" of "+(master.getSlavesPaths().length-1));
			slaveRecords.forEach(this::insertEventRecord);

        }

    }

    /**
     * Method for checking if the event table exists and if not creating it.
     * @return - true if table exists (or has just been created); false if does not exist and could not be created.
     */
    private boolean ensureTableExists(){
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
     * Helper method for insertion of a record into the {@link ArrayList} for all records to inserted as a batch.
     * @param record - record as a {@link String[]} object to be inserted.
     */
    private void insertEventRecord(String[] record){
        this.recordsToBeWritten.add(record);
    }

    /**
     * Method for inserting all the records from the recordsToBeWritten {@link ArrayList} into the database.
     * This is conducted in the batch mode.
     */
    public void writeEventRecords(){
        String sql = "INSERT INTO "+EventModel.tableName() + "(execution_unit_id, job_id, kind, millis, value) VALUES (?, ?, ?, ?, ?);";
        PreparedStatement pstmt = null;
        try{
            connection.setAutoCommit(false);
            pstmt = connection.prepareStatement(sql);

            for (int i=0;i<this.recordsToBeWritten.size();i++){
                if (i % 1000 == 0)
                    log.info("Written "+i + " of "+this.recordsToBeWritten.size()+" records to DB.");
                String[] record = this.recordsToBeWritten.get(i);
                pstmt.setInt(1, Integer.parseInt(record[0]));
                pstmt.setInt(2, Integer.parseInt(record[1]));
                pstmt.setString(3, record[2]);
                pstmt.setLong(4, Long.valueOf(record[3]));
                pstmt.setDouble(5, Double.valueOf(record[4]));
                pstmt.addBatch();
            }
            pstmt.executeBatch();
            connection.commit();
        }
        catch (SQLException e){
            e.printStackTrace();
        }
        finally {
            try {
                if (pstmt != null)
                    pstmt.close();
            }
            catch (SQLException e){
                e.printStackTrace();
            }
        }
    }
}
