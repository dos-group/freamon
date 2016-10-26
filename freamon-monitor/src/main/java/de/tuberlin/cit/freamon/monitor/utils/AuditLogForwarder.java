package de.tuberlin.cit.freamon.monitor.utils;

import de.tuberlin.cit.freamon.api.AuditLogEntry;
import de.tuberlin.cit.freamon.results.AuditLogModel;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.util.concurrent.BlockingQueue;

public class AuditLogForwarder implements Runnable {

    private final BlockingQueue<AuditLogEntry> queue;
    private final BlockingQueue outwardQueue;
    private final Connection connection;
    private final Logger log = Logger.getLogger(AuditLogForwarder.class);

    public AuditLogForwarder(BlockingQueue queue, BlockingQueue outwardQueue, Connection connection){
        this.queue = queue;
        this.outwardQueue = outwardQueue;
        this.connection = connection;
    }
    @Override
    public void run() {
        try {
            while (true) {
                if (!queue.isEmpty()) {
                    log.debug("Queue is not empty. Trying to take an entry...");
                    AuditLogEntry ale = queue.take();
                    log.debug("Received an entry with date: " + ale.date());
                    outwardQueue.put(ale);
                    log.debug("Put a new item into outward queue.");
                    log.debug("Contents: allowed=" + ale.allowed() + ", ugi=" + ale.ugi() + ", ip=" + ale.ip() +
                            ", cmd=" + ale.cmd() + ", src=" + ale.src() + ", dst=" + ale.dst() + ", perm=" + ale.perm() + ", proto=" + ale.proto());
                    AuditLogModel.insert(new AuditLogModel(ale.date(), ale.allowed(),
                            ale.ugi(), ale.ip(), ale.cmd(), ale.src(), ale.dst(),
                            ale.perm(), ale.proto()), connection);
                    log.debug("Succeeded!");
                }
                else if (queue.isEmpty()) {
                    log.debug("Currently no entries. Sleeping for a second");
                    Thread.sleep(1000);
                    log.debug("Waked up. Trying again...");
                }

            }
        }

        catch (InterruptedException e){
            log.error("Caught an InterruptedException whilst checking the queue: "+e);
            e.printStackTrace();
        }
    }
}
