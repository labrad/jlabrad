package org.labrad.data;

import java.util.Arrays;
import java.util.List;


public class Packet {
    private Context context;
    private long target;
    private String targetString;
    private int request;
    List<Record> records;

    public Packet() {
    	
    }
    
    public Packet(Context context, long target, int request, Record... records) {
    	this(context, target, request, Arrays.asList(records));
    }
    	
    public Packet(Context context, long target, int request, List<Record> records) {
        this.context = context;
        this.target = target;
        this.request = request;
        this.records = records;
    }
    
    public String toString() {
        return "Packet(" + "context=" + context.toString()
                         + ", target=" + Long.toString(target)
                         + ", request=" + Integer.toString(request)
                         + ", records=" + records.toString() + ")";
    }

    public int size() { return records.size(); }
    public List<Record> getRecords() { return records; }
    public Record getRecord(int index) { return records.get(index); }
	public Context getContext() { return context; }
	public long getTarget() { return target; }
	public int getRequest() { return request; }
}
