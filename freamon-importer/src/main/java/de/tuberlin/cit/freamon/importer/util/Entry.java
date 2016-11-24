package de.tuberlin.cit.freamon.importer.util;

/**
 * Object storing the parsed CSV line.
 */
public class Entry {

    private double usr, sys, idl, wai, hiq, siq, mem_used, mem_buff, mem_cache, mem_free, net_recv, net_send, dsk_read, dsk_writ;
    private long epoch;

    /**
     * Default object constructor
     * @param epoch - epoch column in the file
     * @param usr - usr column in the file
     * @param sys - sys column in the file
     * @param idl - idl column in the file
     * @param wai - wai column in the file
     * @param hiq - hiq column in the file
     * @param siq - siq column in the file
     * @param mem_used - mem_used column in the file
     * @param mem_buff - mem_buff column in the file
     * @param mem_cache - mem_cache column in the file
     * @param mem_free - mem_free column in the file
     * @param net_recv - net_recv column in the file
     * @param net_send - net_send column in the file
     * @param dsk_read - dsk_read column in the file
     * @param dsk_writ - dsk_writ column in the file
     */
    public Entry(long epoch, double usr, double sys, double idl, double wai, double hiq, double siq, double mem_used, double mem_buff, double mem_cache, double mem_free,
          double net_recv, double net_send, double dsk_read, double dsk_writ){
        this.epoch = epoch;
        this.usr = usr;
        this.sys = sys;
        this.idl = idl;
        this.wai = wai;
        this.hiq = hiq;
        this.siq = siq;
        this.mem_used = mem_used;
        this.mem_buff = mem_buff;
        this.mem_cache = mem_cache;
        this.mem_free = mem_free;
        this.net_recv = net_recv;
        this.net_send = net_send;
        this.dsk_read = dsk_read;
        this.dsk_writ = dsk_writ;
    }

    /**
     * Getter of the epoch variable
     * @return - long
     */
    public long getEpoch() {
        return epoch;
    }

    /**
     * Getter of the usr variable
     * @return - double
     */
    public double getUsr() {
        return usr;
    }

    /**
     * Getter of the sys variable
     * @return - double
     */
    public double getSys() {
        return sys;
    }

    /**
     * Getter of the idl variable
     * @return - double
     */
    public double getIdl() {
        return idl;
    }

    /**
     * Getter of the wai variable
     * @return - double
     */
    public double getWai() {
        return wai;
    }

    /**
     * Getter of the hiq variable
     * @return - double
     */
    public double getHiq() {
        return hiq;
    }

    /**
     * Getter of the siq variable
     * @return - double
     */
    public double getSiq() {
        return siq;
    }

    /**
     * Getter of the mem_used variable
     * @return - double
     */
    public double getMem_used() {
        return mem_used;
    }

    /**
     * Getter of the mem_buff variable
     * @return - double
     */
    public double getMem_buff() {
        return mem_buff;
    }

    /**
     * Getter of the mem_cache variable
     * @return - double
     */
    public double getMem_cache() {
        return mem_cache;
    }

    /**
     * Getter of the mem_free variable
     * @return - double
     */
    public double getMem_free() {
        return mem_free;
    }

    /**
     * Getter of the net_recv variable
     * @return - double
     */
    public double getNet_recv() {
        return net_recv;
    }

    /**
     * Getter of the net_send variable
     * @return - double
     */
    public double getNet_send() {
        return net_send;
    }

    /**
     * Getter of the dsk_read variable
     * @return - double
     */
    public double getDsk_read() {
        return dsk_read;
    }

    /**
     * Getter of the dsk_writ variable
     * @return - double
     */
    public double getDsk_writ() {
        return dsk_writ;
    }

}
