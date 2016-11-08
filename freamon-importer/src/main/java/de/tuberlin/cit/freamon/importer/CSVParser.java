package de.tuberlin.cit.freamon.importer;


import org.apache.log4j.Logger;

import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

class CSVParser {

    private int firstLine = 0;

    private StringBuilder builder;

    private String currentFile;

    private final static Logger log = Logger.getLogger(CSVParser.class);

    CSVParser(int firstLine, String currentFile){
        log.info("CSVParser started.");
        builder = new StringBuilder();
        this.firstLine = firstLine;
        this.currentFile = currentFile;
    }

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


    List<Entry> parseStringToEntry(){
        this.readFileToStringBuilder();
        List<Entry> results = new ArrayList<>();
        String[] lines = builder.toString().split("\n");
        for (int i=0;i<lines.length;i++){
            log.debug("raw: "+lines[i]);
            if (i>=firstLine) {
                String[] line = lines[i].split(",");
                long epoch = new Date().getTime();
                if (Organiser.epoch != -1){
                    double temp_epoch = Double.parseDouble(line[Organiser.epoch].replaceAll("\"", "")) * 1000;
                    epoch = (long) temp_epoch;
                }
                double usr, sys, idl, wai;
                usr = sys = idl = wai = -1;
                if (Organiser.usr != -1)
                    usr = Double.parseDouble(line[Organiser.usr].replaceAll("\"", ""));
                if (Organiser.sys != -1)
                    sys = Double.parseDouble(line[Organiser.sys].replaceAll("\"", ""));
                if (Organiser.idl != -1)
                    idl = Double.parseDouble(line[Organiser.idl].replaceAll("\"", ""));
                if (Organiser.wai != -1)
                    wai = Double.parseDouble(line[Organiser.wai].replaceAll("\"", ""));
                double hiq, siq, mem_used, mem_buff, mem_cache, mem_free, net_recv, net_send, dsk_read, dsk_writ;
                hiq = siq = mem_used = mem_buff = mem_cache = mem_free = net_recv = net_send = dsk_read = dsk_writ = -1;
                if (Organiser.hiq != -1)
                    hiq = Double.parseDouble(line[Organiser.hiq].replaceAll("\"", ""));
                if (Organiser.siq != -1)
                    siq = Double.parseDouble(line[Organiser.siq].replaceAll("\"", ""));
                if (Organiser.mem_used != -1)
                    mem_used = Double.parseDouble(line[Organiser.mem_used].replaceAll("\"", ""));
                if (Organiser.mem_buff != -1)
                    mem_buff = Double.parseDouble(line[Organiser.mem_buff].replaceAll("\"", ""));
                if (Organiser.mem_cach != -1)
                    mem_cache = Double.parseDouble(line[Organiser.mem_cach].replaceAll("\"", ""));
                if (Organiser.mem_free != -1)
                    mem_free = Double.parseDouble(line[Organiser.mem_free].replaceAll("\"", ""));
                if (Organiser.net_recv != -1)
                    net_recv = Double.parseDouble(line[Organiser.net_recv].replaceAll("\"", ""));
                if (Organiser.net_send != -1)
                    net_send = Double.parseDouble(line[Organiser.net_send].replaceAll("\"", ""));
                if (Organiser.dsk_read != -1)
                    dsk_read = Double.parseDouble(line[Organiser.dsk_read].replaceAll("\"", ""));
                if (Organiser.dsk_writ != -1)
                    dsk_writ = Double.parseDouble(line[Organiser.dsk_writ].replaceAll("\"", ""));
                log.debug("processed: "+epoch + "," + usr + "," + sys + "," + idl + "," + wai + "," + hiq + "," + siq + "," + mem_used + "," + mem_buff + "," + mem_cache + "," + mem_free
                + "," + net_recv + "," + net_send + "," + dsk_read + "," + dsk_writ);
                Entry entry = new Entry(epoch, usr, sys, idl, wai, hiq, siq, mem_used, mem_buff, mem_cache, mem_free, net_recv, net_send,
                        dsk_read, dsk_writ);
                results.add(entry);
            }
        }
        log.debug(results.size() + " items processed.");
        return results;
    }
}
