package de.tuberlin.cit.freamon.importer.util;


import de.tuberlin.cit.freamon.importer.core.DstatImporter;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CSVParser {

    private int firstLine = 0;

    private StringBuilder builder;

    private String currentFile;

    private final static Logger log = Logger.getLogger(CSVParser.class);

    /**
     * Constructor of the CSV Parser.
     * @param firstLine - first line, where the body of the file begins.
     * @param currentFile - path to the file to be parsed.
     */
    public CSVParser(int firstLine, String currentFile){
        log.debug("CSVParser started.");
        builder = new StringBuilder();
        this.firstLine = firstLine;
        this.currentFile = currentFile;
    }

    /**
     * Method for reading in the file to a {@link StringBuilder} object (to accelerate the reading in operation).
     */
    private void readFileToStringBuilder(){
        BufferedReader br = null;
        try {
            String currentLine;
            br = new BufferedReader(new FileReader(currentFile));
            while ((currentLine = br.readLine()) != null) {
                builder.append(currentLine).append("\n");
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
     * Method for parsing the contents of the {@link StringBuilder} object line by line.
     * @return - a {@link List} of Entry objects containing the read data.
     */
    public List<Entry> parseStringToEntry(){
        this.readFileToStringBuilder();
        List<Entry> results = new ArrayList<>();
        String[] lines = builder.toString().split("\n");
        for (int i=0;i<lines.length;i++){
            if (i>=firstLine) {
                String[] line = lines[i].split(",");
                long epoch = new Date().getTime();
                if (DstatImporter.epoch != -1){
                    double temp_epoch = Double.parseDouble(line[DstatImporter.epoch].replaceAll("\"", "")) * 1000;
                    epoch = (long) temp_epoch;
                }
                double usr, sys, idl, wai;
                usr = sys = idl = wai = -1;
                if (DstatImporter.usr != -1)
                    usr = Double.parseDouble(line[DstatImporter.usr].replaceAll("\"", ""));
                if (DstatImporter.sys != -1)
                    sys = Double.parseDouble(line[DstatImporter.sys].replaceAll("\"", ""));
                if (DstatImporter.idl != -1)
                    idl = Double.parseDouble(line[DstatImporter.idl].replaceAll("\"", ""));
                if (DstatImporter.wai != -1)
                    wai = Double.parseDouble(line[DstatImporter.wai].replaceAll("\"", ""));
                double hiq, siq, mem_used, mem_buff, mem_cache, mem_free, net_recv, net_send, dsk_read, dsk_writ;
                hiq = siq = mem_used = mem_buff = mem_cache = mem_free = net_recv = net_send = dsk_read = dsk_writ = -1;
                if (DstatImporter.hiq != -1)
                    hiq = Double.parseDouble(line[DstatImporter.hiq].replaceAll("\"", ""));
                if (DstatImporter.siq != -1)
                    siq = Double.parseDouble(line[DstatImporter.siq].replaceAll("\"", ""));
                if (DstatImporter.mem_used != -1)
                    mem_used = Double.parseDouble(line[DstatImporter.mem_used].replaceAll("\"", ""));
                if (DstatImporter.mem_buff != -1)
                    mem_buff = Double.parseDouble(line[DstatImporter.mem_buff].replaceAll("\"", ""));
                if (DstatImporter.mem_cach != -1)
                    mem_cache = Double.parseDouble(line[DstatImporter.mem_cach].replaceAll("\"", ""));
                if (DstatImporter.mem_free != -1)
                    mem_free = Double.parseDouble(line[DstatImporter.mem_free].replaceAll("\"", ""));
                if (DstatImporter.net_recv != -1)
                    net_recv = Double.parseDouble(line[DstatImporter.net_recv].replaceAll("\"", ""));
                if (DstatImporter.net_send != -1)
                    net_send = Double.parseDouble(line[DstatImporter.net_send].replaceAll("\"", ""));
                if (DstatImporter.dsk_read != -1)
                    dsk_read = Double.parseDouble(line[DstatImporter.dsk_read].replaceAll("\"", ""));
                if (DstatImporter.dsk_writ != -1)
                    dsk_writ = Double.parseDouble(line[DstatImporter.dsk_writ].replaceAll("\"", ""));
                Entry entry = new Entry(epoch, usr, sys, idl, wai, hiq, siq, mem_used, mem_buff, mem_cache, mem_free, net_recv, net_send,
                        dsk_read, dsk_writ);
                results.add(entry);
            }
        }
        return results;
    }
}
