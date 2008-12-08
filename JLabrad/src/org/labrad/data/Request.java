package org.labrad.data;

import java.util.ArrayList;
import java.util.List;

import org.labrad.Constants;

public class Request {
	private long serverID;
	private String serverName;
	private boolean needsServerLookup;
	private Context context;
    private List<Record> records;
     
    
    // constructors
    public Request(String target) { this(target, Constants.DEFAULT_CONTEXT); }
    public Request(long targetID) { this(targetID, Constants.DEFAULT_CONTEXT); }
    
    public Request(String target, Context context) {
    	this.context = context;
    	this.serverName = target;
    	this.needsServerLookup = true;
    	this.records = new ArrayList<Record>();
    }
    
    public Request(long targetID, Context context) {
    	this.context = context;
    	this.serverID = targetID;
    	this.needsServerLookup = false;
    	this.records = new ArrayList<Record>();
    }
    
    public Context getContext() { return context; }
    
    public long getServerID() { return serverID; }
    public void setServerID(long targetID) {
    	this.serverID = targetID;
    	this.needsServerLookup = false;
    }
	public String getServerName() { return serverName; }
	public boolean needsServerLookup() { return needsServerLookup; }
    
	public boolean needsSettingLookup() {
		boolean needsLookup = false;
		for (Record r : records) {
			if (r.needsLookup()) {
				needsLookup = true;
				break;
			}
		}
		return needsLookup;
	}
	
	public boolean needsLookup() {
		return needsServerLookup() || needsSettingLookup();
	}
	
    // adding packets
	/**
	 * Add an empty record for the given setting name, returning the index for later unpacking.
	 * @return an int with the index of this record
	 */
	public int addRecord(String setting) {
		return addRecord(setting, Data.EMPTY);
	}
	
	/**
	 * Add a record for the given setting name, returning the index for later unpacking.
	 * @param setting
	 * @param data
	 * @return
	 */
    public int addRecord(String setting, Data data) {
    	add(setting, data);
    	return records.size() - 1;
    }
    
    public int addRecord(long settingID) {
    	return addRecord(settingID, Data.EMPTY);
    }
    
    public int addRecord(long settingID, Data data) {
    	add(settingID, data);
    	return records.size() - 1;
    }
    
    /**
     * Add an empty record for the given setting name, returning the packet for chaining.
     * @param setting
     * @return
     */
    public Request add(String setting) {
    	return add(setting, Data.EMPTY);
    }
    
    /**
     * Add a record for the given setting name, returning the packet for chaining.
     * @param setting
     * @param data
     * @return
     */
    public Request add(String setting, Data data) {
    	records.add(new Record(setting, data));
    	return this;
    }
    
    public Request add(long settingID) {
    	return add(settingID, Data.EMPTY);
    }
    
    public Request add(long settingID, Data data) {
    	records.add(new Record(settingID, data));
    	return this;
    }
    
    public int size() { return records.size(); }
    public List<Record> getRecords() { return records; }
    public Record getRecord(int index) { return records.get(index); }	
}
