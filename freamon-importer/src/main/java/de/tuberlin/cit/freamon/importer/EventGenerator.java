package de.tuberlin.cit.freamon.importer;

import de.tuberlin.cit.freamon.results.DB;
import de.tuberlin.cit.freamon.results.EventModel;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class EventGenerator {

    private Connection connection;

    public EventGenerator(){
        connection = DB.getConnection("jdbc:monetdb://localhost/freamon", "monetdb", "monetdb");
    }

    void generateEntries(Map<String, List<Entry>> fileEntries){

    }

    private int getLastJobID(){
        String sql = "SELECT max (\"id\") from "+ EventModel.tableName()+";";
        if (tableExists()){
            try {
                PreparedStatement pstmt = connection.prepareStatement(sql);
                ResultSet resultSet = pstmt.executeQuery();
                return resultSet.getInt(1);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return 0;
    }

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
}
