package org.labrad;

public class Record {
	String name;
	long ID;
	Data data;
	private boolean needsLookupFlag = false;
	
	public Record(String name) {
		this(name, Data.EMPTY);
	}
	
	public Record(String name, Data data) {
		this.name = name;
		this.data = data;
		needsLookupFlag = true;
	}
	
	public Record(long ID) {
		this(ID, Data.EMPTY);
	}
	
	public Record(long ID, Data data) {
		this.ID = ID;
		this.data = data;
	}
	
	public boolean needsLookup() {
		return needsLookupFlag;
	}
	
	public String toString() {
		if (needsLookupFlag) {
			return "Record(" + name + ", " + data.pretty() + ")";
		}
		return "Record(" + Long.toString(ID) + ", " + data.pretty() + ")";
	}
}
