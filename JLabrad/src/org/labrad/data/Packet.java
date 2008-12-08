package org.labrad.data;

import java.util.Arrays;
import java.util.List;


public class Packet {
    private final Context context;
    private final long target;
    private final int request;
    private final List<Record> records;
        
    public Packet(Context context, long target, int request, Record... records) {
    	this(context, target, request, Arrays.asList(records));
    }
    	
    public Packet(Context context, long target, int request, List<Record> records) {
        this.context = context;
        this.target = target;
        this.request = request;
        this.records = records;
    }
    
    public int size() { return records.size(); }
    public List<Record> getRecords() { return records; }
    public Record getRecord(int index) { return records.get(index); }
	public Context getContext() { return context; }
	public long getTarget() { return target; }
	public int getRequest() { return request; }
	
	public static Packet forRequest(Request request, int requestNum) {
		return new Packet(request.getContext(), request.getServerID(), requestNum, request.getRecords());
	}
	
	public static Packet forMessage(Request request) {
		return Packet.forRequest(request, 0);
	}
}
