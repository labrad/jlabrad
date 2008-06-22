package org.labrad.data;


public class Record {
    private String name;

    private long ID;

    private Data data;

    private boolean lookedUp = true;

    public Record(String name) {
        this(name, Data.EMPTY);
    }

    public Record(String name, Data data) {
        this.name = name;
        this.data = data;
        lookedUp = false;
    }

    public Record(long ID) {
        this(ID, Data.EMPTY);
    }
    
    public Record(long ID, Data data) {
        this.ID = ID;
        this.data = data;
    }

    public Data getData() {
    	return data;
    }
    
    public String getName() {
    	return name;
    }
    
    public long getID() {
    	return ID;
    }
    
    public boolean needsLookup() {
        return !lookedUp;
    }

    public String toString() {
        if (!lookedUp) {
            return "Record(" + name + ", " + data.pretty() + ")";
        }
        return "Record(" + Long.toString(ID) + ", " + data.pretty() + ")";
    }
}
