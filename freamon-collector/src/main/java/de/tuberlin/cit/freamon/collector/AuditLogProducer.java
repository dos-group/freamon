package de.tuberlin.cit.freamon.collector;


import de.tuberlin.cit.freamon.api.AuditLogEntry;
import org.apache.log4j.Logger;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.concurrent.BlockingQueue;

public class AuditLogProducer implements Runnable{
    private final BlockingQueue queue;
    private String path;
    private final Logger log = Logger.getLogger(AuditLogProducer.class);

    public AuditLogProducer(BlockingQueue q, String path){
        queue = q;
        this.path = path;
    }
    public void run(){
        log.debug("Producer started...");
        try {
                startReading();
        }
        catch (Exception ex){
            log.error("Caught an Exception: "+ex);
            ex.printStackTrace();
        }
    }

    public void startReading(){
        log.debug("startReading called");
        String line;
        try{
            LineNumberReader lnr = new LineNumberReader(new FileReader(path));
            while (true){
                line = lnr.readLine();
                log.debug("Line read: "+line);
                if(line == null){
                    log.debug("Waiting 3 seconds");
                    Thread.sleep(3000);
                    continue;
                }
                AuditLogEntry ale = processEntry(line);
                queue.put(ale);
            }
        }
        catch (FileNotFoundException e){
            log.error("The file could not be found: "+e);
            e.printStackTrace();
        }
        catch (IOException e){
            log.error("Caught IO Exception: "+e);
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();

        }
    }

    private AuditLogEntry processEntry(String entry){
        log.debug("processEntry called");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS");

        String date = entry.substring(0,23);
        Long parsedDate = 0L;
        try {
            parsedDate = sdf.parse(date).getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        String report = entry.substring(49);
        String[] splitReport = report.split("\t");
        String allowed = splitReport[0].substring(8);
        boolean boolAllowed = false;
        if (allowed.equalsIgnoreCase("true"))
            boolAllowed = true;
        else if (allowed.equalsIgnoreCase("false"))
            boolAllowed = false;
        String ugi = splitReport[1].substring(4);
        String ip = splitReport[2].substring(3);
        String cmd = splitReport[3].substring(4);
        String src = splitReport[4].substring(4);
        String dst = splitReport[5].substring(4);
        String perm = splitReport[6].substring(5);
        String proto = splitReport[7].substring(6);

        AuditLogEntry ale = new AuditLogEntry(parsedDate, boolAllowed, ugi, ip, cmd, src, dst, perm, proto);
        log.debug("An AuditLogEntry object created with date: "+ale.date());

        return ale;
    }

}
