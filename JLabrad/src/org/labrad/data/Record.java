package org.labrad.data;


public class Record {
    private String name = null;

    private long ID;

    private Data data;

    private boolean needsLookup = false;

    public Record(String name) {
        this(name, Data.EMPTY);
    }

    public Record(String name, Data data) {
        this.name = name;
        this.data = data;
        needsLookup = true;
    }

    public Record(long ID) {
        this(ID, Data.EMPTY);
    }
    
    public Record(long ID, Data data) {
        this.ID = ID;
        this.data = data;
    }

    public Data getData() { return data; }
    public String getName() { return name; }
    public long getID() { return ID; }
    public void setID(long ID) {
    	this.ID = ID;
    	this.needsLookup = false;
    }
    public boolean needsLookup() { return needsLookup; }

    public String toString() {
        if (name != null) {
            return "Record(" + name + ", " + data.pretty() + ")";
        }
        return "Record(" + ID + ", " + data.pretty() + ")";
    }
}
