package org.labrad;

import java.io.IOException;

public class ServerProxy {
    private SimpleClient sc;

    private long ID;

    private String name;

    public ServerProxy(SimpleClient sc, long ID, String name) {
        this.sc = sc;
        this.ID = ID;
        this.name = name;
    }

    public Record request(String setting) throws IOException {
        return sc.request(ID, new Record(setting))[0];
    }

    public Record request(String setting, Data data) throws IOException {
        return sc.request(ID, new Record(setting, data))[0];
    }
}
