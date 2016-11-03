package de.tuberlin.cit.freamon.importer;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

class JSONParser {

    private final static Logger log = Logger.getLogger(JSONParser.class);

    private StringBuilder builder;
    private JSONObject json;

    JSONParser(String pathToFile){
        log.info("JSONParser started.");
        builder = new StringBuilder();
        this.readFileToStringBuilder(pathToFile);
        json = this.convertStringToJSON();
    }

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

    private JSONObject convertStringToJSON(){
        JSONObject result = null;
        try {
            result =  new JSONObject(builder.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return result;
    }

    String[] extractDataFromJSON(){
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
