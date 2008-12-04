package org.labrad;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.labrad.data.Data;
import org.labrad.data.Record;

public class PacketBuilder {
	private SimpleClient sc;
	private ServerProxy server;
	private List<Record> records;
	
	public PacketBuilder(SimpleClient sc, ServerProxy server) {
		this.sc = sc;
		this.server = server;
		records = new ArrayList<Record>();
	}
	
	public List<Data> send() throws IOException {
		List<Record> response = sc.sendRequest(server.getID(), records.toArray(new Record[0]));
		List<Data> answer = new ArrayList<Data>();
		for (Record r : response) {
			answer.add(r.getData());
		}
		return answer;
	}
	
	public PacketBuilder add(long setting) {
		records.add(new Record(setting));
		return this;
	}
	
	public PacketBuilder add(long setting, Data data) {
		records.add(new Record(setting, data));
		return this;
	}
	
	public PacketBuilder add(String setting) {
		records.add(new Record(setting));
		return this;
	}
	
	public PacketBuilder add(String setting, Data data) {
		records.add(new Record(setting, data));
		return this;
	}
	
	public int addIndexed(long setting) {
		records.add(new Record(setting));
		return records.size()-1;
	}
	
	public int addIndexed(long setting, Data data) {
		records.add(new Record(setting, data));
		return records.size()-1;
	}
	
	public int addIndexed(String setting) {
		records.add(new Record(setting));
		return records.size()-1;
	}
	
	public int addIndexed(String setting, Data data) {
		records.add(new Record(setting, data));
		return records.size()-1;
	}
}
