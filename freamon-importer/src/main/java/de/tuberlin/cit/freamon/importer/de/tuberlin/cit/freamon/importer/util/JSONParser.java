package de.tuberlin.cit.freamon.importer.de.tuberlin.cit.freamon.importer.util;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * Object parsing the state.json files.
 */
public class JSONParser {

    private final static Logger log = Logger.getLogger(JSONParser.class);

    private StringBuilder builder;
    private JSONObject json;

    /**
     * Constructor of the object.
     * @param pathToFile - path to the state.json file.
     */
    public JSONParser(String pathToFile){
        log.debug("JSONParser started.");
        builder = new StringBuilder();
        this.readFileToStringBuilder(pathToFile);
        json = this.convertStringToJSON();
    }

    /**
     * Method reading in the JSON file to the {@link StringBuilder} object.
     * @param pathToFile - path to the JSON file to be read.
     */
    private void readFileToStringBuilder(String pathToFile){
        BufferedReader br = null;
        try {
            String currentLine;
            br = new BufferedReader(new FileReader(pathToFile));
            while ((currentLine = br.readLine()) != null) {
                builder.append(currentLine);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            try {
                if (br != null)br.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * Helper method for conversion of the read in state.json file to a JSONObject.
     * @return - JSON object representing the state.json file.
     */
    private JSONObject convertStringToJSON(){
        JSONObject result = null;
        try {
            result =  new JSONObject(builder.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Method for extracting the data from the JSON file.
     * @return - a {@link String[]} object containing the JSON data.
     */
    public String[] extractDataFromJSON(){
        String[] results = new String[6];

        try {
            results[0] = json.getString("runnerID");
            results[1] = json.getString("runnerName");
            results[2] = json.getString("runnerVersion");
            results[3] = String.valueOf(json.getInt("runExitCode"));
            results[4] = String.valueOf(json.getInt("runTime"));
            results[5] = String.valueOf(json.getInt("plnExitCode"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return results;
    }
}
