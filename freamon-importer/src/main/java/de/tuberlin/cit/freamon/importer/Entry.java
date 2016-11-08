package de.tuberlin.cit.freamon.importer;


public class Entry {

    private double usr, sys, idl, wai, hiq, siq, mem_used, mem_buff, mem_cache, mem_free, net_recv, net_send, dsk_read, dsk_writ;
    private long epoch;

    Entry(long epoch, double usr, double sys, double idl, double wai, double hiq, double siq, double mem_used, double mem_buff, double mem_cache, double mem_free,
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

    long getEpoch() {
        return epoch;
    }

    double getUsr() {
        return usr;
    }

    double getSys() {
        return sys;
    }

    double getIdl() {
        return idl;
    }

    double getWai() {
        return wai;
    }

    public double getHiq() {
        return hiq;
    }

    public double getSiq() {
        return siq;
    }

    public double getMem_used() {
        return mem_used;
    }

    public double getMem_buff() {
        return mem_buff;
    }

    public double getMem_cache() {
        return mem_cache;
    }

    public double getMem_free() {
        return mem_free;
    }

    public double getNet_recv() {
        return net_recv;
    }

    public double getNet_send() {
        return net_send;
    }

    public double getDsk_read() {
        return dsk_read;
    }

    public double getDsk_writ() {
        return dsk_writ;
    }

}
