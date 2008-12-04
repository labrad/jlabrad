package org.labrad;

import java.io.IOException;

import org.labrad.data.Data;
import org.labrad.data.Record;

public class ServerProxy {
    private SimpleClient sc;

    private long ID;

    private String name;

    public ServerProxy(SimpleClient sc, long ID, String name) {
        this.sc = sc;
        this.ID = ID;
        this.name = name;
    }

    public Data sendRequest(String setting) throws IOException {
        return sc.sendRequest(ID, new Record(setting)).get(0).getData();
    }

    public Data sendRequest(String setting, Data data) throws IOException {
        return sc.sendRequest(ID, new Record(setting, data)).get(0).getData();
    }
    
    public PacketBuilder newPacket() {
    	return new PacketBuilder(sc, this);
    }
    
    public String getName() {
    	return name;
    }
    
    public long getID() {
    	return ID;
    }
}
